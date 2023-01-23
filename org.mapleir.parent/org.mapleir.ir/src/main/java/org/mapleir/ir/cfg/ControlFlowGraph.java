package org.mapleir.ir.cfg;

import com.google.common.collect.Streams;
import org.mapleir.asm.MethodNode;
import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.FlowGraph;
import org.mapleir.flowgraph.edges.*;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.PhiExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.CopyPhiStmt;
import org.mapleir.ir.locals.LocalsPool;
import org.mapleir.ir.locals.impl.VersionedLocal;
import org.mapleir.ir.utils.CFGExporterUtils;
import org.mapleir.ir.utils.CFGUtils;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.stdlib.collections.itertools.ChainIterator;
import org.mapleir.stdlib.util.IHasJavaDesc;
import org.mapleir.stdlib.util.JavaDesc;
import org.mapleir.stdlib.util.TabbedStringWriter;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mapleir.ir.code.Opcode.PHI_STORE;

public class ControlFlowGraph extends FlowGraph<BasicBlock, FlowEdge<BasicBlock>> implements IHasJavaDesc {
	
	private final LocalsPool locals;
	private final MethodNode methodNode;
	private final JavaDesc javaDesc;

	// used for assigning unique id's to basicblocks. ugly hack
	// fyi, we start at one arbitrarily.
	private int blockCounter = 1;

	public ControlFlowGraph(LocalsPool locals, MethodNode methodNode) {
		this.locals = locals;
		this.methodNode = methodNode;
		this.javaDesc = methodNode.getJavaDesc();
	}
	
	public ControlFlowGraph(ControlFlowGraph cfg) {
		super(cfg);
		locals = cfg.locals;
		methodNode = cfg.methodNode;
		javaDesc = cfg.javaDesc;
	}

	public int makeBlockId() {
		return blockCounter++;
	}

    public Stream<CodeUnit> allExprStream() {
   		return vertices().stream().flatMap(Collection::stream).map(Stmt::enumerateWithSelf).flatMap(Streams::stream);
   	}

    /**
	 * Properly removes the edge, and cleans up phi uses in fe.dst of phi arguments from fe.src.
	 * @param fe Edge to excise phi uses.
	 */
	public void exciseEdge(FlowEdge<BasicBlock> fe) {
		if (!this.containsEdge(fe))
			throw new IllegalArgumentException("Graph does not contain the specified edge");
		
		removeEdge(fe);
		for (Stmt stmt : fe.dst()) {
			if (stmt.getOpcode() == PHI_STORE) {
				CopyPhiStmt phs = (CopyPhiStmt) stmt;
				PhiExpr phi = phs.getExpression();
				
				BasicBlock pred = fe.src();
				VarExpr arg = (VarExpr) phi.getArgument(pred);
				
				VersionedLocal l = (VersionedLocal) arg.getLocal();
				locals.uses.get(l).remove(arg);
				
				phi.removeArgument(pred);
			} else {
				return;
			}
		}
	}
	
	/**
	 * Excises uses of a removed statement.
	 * @param c Removed statement to update def/use information with respect to.
	 */
	public void exciseStmt(Stmt c) {
		// delete uses
		for(Expr e : c.enumerateOnlyChildren()) {
			if(e.getOpcode() == Opcode.LOCAL_LOAD) {
				VarExpr v = (VarExpr) e;
				
				VersionedLocal l = (VersionedLocal) v.getLocal();
				locals.uses.get(l).remove(v);
			}
		}
		
		c.getBlock().remove(c);
	}
	
	/**
	 * Replaces an expression and updates def/use information accordingly.
	 * @param parent Statement containing expression to be replaced.
	 * @param from Statement to be replaced.
	 * @param to Statement to replace old statement with.
	 */
	public void writeAt(CodeUnit parent, Expr from, Expr to) {
		// remove uses in from
		for(Expr e : from.enumerateWithSelf()) {
			if (e.getOpcode() == Opcode.LOCAL_LOAD) {
				VersionedLocal l = (VersionedLocal) ((VarExpr) e).getLocal();
				locals.uses.get(l).remove(e);
			}
		}
		
		// add uses in to
		for(Expr e : to.enumerateWithSelf()) {
			if (e.getOpcode() == Opcode.LOCAL_LOAD) {
				VarExpr var = (VarExpr) e;
				locals.uses.get((VersionedLocal) var.getLocal()).add(var);
			}
		}
		
		parent.writeAt(to, parent.indexOf(from));
	}

	@Override
	public String toString() {
		TabbedStringWriter sw = new TabbedStringWriter();
		
		for(ExceptionRange<BasicBlock> r : getRanges()) {
			sw.print(r.toString() + "\n");
		}
		
		int insn = 0;
		
		for(BasicBlock b : verticesInOrder()) {
			CFGUtils.blockToString(sw, this, b, insn);
		}
		return sw.toString();
	}

	public LocalsPool getLocals() {
		return locals;
	}

	@Override
	public JavaDesc getJavaDesc() {
		return javaDesc;
	}

	@Override
	public String getOwner() {
		return javaDesc.owner;
	}

	@Override
	public String getName() {
		return javaDesc.name;
	}

	@Override
	public String getDesc() {
		return javaDesc.desc;
	}

	@Override
	public JavaDesc.DescType getDescType() {
		return JavaDesc.DescType.METHOD;
	}

	public MethodNode getMethodNode() {
		return methodNode;
	}

	@Override
	public ControlFlowGraph copy() {
		return new ControlFlowGraph(this);
	}

	@Override
	public FlowEdge<BasicBlock> clone(FlowEdge<BasicBlock> edge, BasicBlock src, BasicBlock dst) {
		return edge.clone(src, dst);
	}
	
	public Iterable<Stmt> stmts() {
		return () -> new ChainIterator.CollectionChainIterator<>(vertices());
	}
	
	public void relabel(List<BasicBlock> order) {
		if (order.size() != size())
			throw new IllegalArgumentException("order is wrong length");
		// copy edge sets
		Map<BasicBlock, Set<FlowEdge<BasicBlock>>> edges = new HashMap<>();
		for(BasicBlock b : order) {
			if (!containsVertex(b))
				throw new IllegalArgumentException("order has missing vertex " + b);
			edges.put(b, getEdges(b));
		}
		// clean graph
		clear();
		
		// rename and add blocks
		blockCounter = 1;
		for(BasicBlock b : order) {
			b.setId(makeBlockId());
			addVertex(b);
		}
		
		for(Entry<BasicBlock, Set<FlowEdge<BasicBlock>>> e : edges.entrySet()) {
			BasicBlock b = e.getKey();
			for(FlowEdge<BasicBlock> fe : e.getValue()) {
				addEdge(fe);
			}
		}
	}

	/**
	 * Runs sanity checking on this graph, useful for debugging purposes.
	 */
	public void verify() {
		if (getEntries().size() != 1)
			throw new IllegalStateException("Wrong number of entries: " + getEntries());

		int maxId = 0;
		Set<Integer> usedIds = new HashSet<>();
		for (BasicBlock b : vertices()) {
			if (!usedIds.add(b.getNumericId()))
				throw new IllegalStateException("Id collision: " + b);
			if (b.getNumericId() > maxId)
				maxId = b.getNumericId();

			if (getReverseEdges(b).size() == 0 && !getEntries().contains(b)) {
				throw new IllegalStateException("dead incoming: " + CFGUtils.printBlock(b));
			}

			for (FlowEdge<BasicBlock> fe : getEdges(b)) {
				if (fe.src() != b) {
					throw new RuntimeException(fe + " from " + b);
				}

				BasicBlock dst = fe.dst();

				if (!containsVertex(dst) || !containsReverseVertex(dst)) {
					throw new RuntimeException(
							fe + "; dst invalid: " + containsVertex(dst) + " : " + containsReverseVertex(dst));
				}

				boolean found = getReverseEdges(dst).contains(fe);

				if (!found) {
					throw new RuntimeException("no reverse: " + fe);
				}

				if (fe.getType() == FlowEdges.TRYCATCH) {
					TryCatchEdge<BasicBlock> tce = (TryCatchEdge<BasicBlock>) fe;
					if (!tce.erange.containsVertex(b)) {
						throw new RuntimeException("no contains: " + b + " in " + tce.erange + " for " + tce);
					}
				}
			}

			b.checkConsistency();
		}
		if (maxId != size())
			throw new IllegalStateException("Bad id numbering: " + size() + " vertices total, but max id is " + maxId);

		for (ExceptionRange<BasicBlock> er : getRanges()) {
			if (er.getNodes().size() == 0) {
				throw new RuntimeException("empty range: " + er);
			}

			if (!containsVertex(er.getHandler()) || !containsReverseVertex(er.getHandler())) {
				throw new RuntimeException("invalid handler: " + er.getHandler() + " in " + er);
			}

			for (BasicBlock b : er.getNodes()) {
				if (!containsVertex(b) || !containsReverseVertex(b)) {
					throw new RuntimeException("invalid b: " + b + " to " + er);
				}

				boolean found = false;

				for (FlowEdge<BasicBlock> fe : getEdges(b)) {
					if (fe.getType() == FlowEdges.TRYCATCH) {
						TryCatchEdge<BasicBlock> tce = (TryCatchEdge<BasicBlock>) fe;

						if (tce.erange == er) {
							if (tce.dst() != er.getHandler()) {
								throw new RuntimeException("false tce: " + tce + ", er: " + er);
							} else {
								found = true;
							}
						}
					}
				}

				if (!found) {
					throw new RuntimeException("mismatch: " + b + " to " + er + " ; " + getEdges(b));
				}
			}
		}
	}

	public void recomputeEdges() {
		final List<ImmediateEdge<BasicBlock>> immediateEdges = new ArrayList<>();

		for (BasicBlock vertex : vertices()) {
			final ImmediateEdge<BasicBlock> immediate = getImmediateEdge(vertex);

			if (immediate == null)
				continue;

			immediateEdges.add(immediate);
		}

		for (BasicBlock vertex : vertices()) {
			final ImmediateEdge<BasicBlock> immediate = getIncomingImmediateEdge(vertex);

			if (immediate == null)
				continue;

			assert immediateEdges.contains(immediate) : "Failed to find incoming immediate!";
		}

		// Clear all edges
		for (Set<FlowEdge<BasicBlock>> value : map.values()) {
			value.clear();
		}
		for (Set<FlowEdge<BasicBlock>> value : reverseMap.values()) {
			value.clear();
		}

		// Add all immediates
		for (ImmediateEdge<BasicBlock> immediateEdge : immediateEdges) {
			addEdge(immediateEdge);
		}

		// Add all call based edges
		for (BasicBlock vertex : vertices()) {
			vertex.forEach(stmt -> {
				if (stmt instanceof UnconditionalJumpStmt) {
					final UnconditionalJumpStmt jmp = (UnconditionalJumpStmt) stmt;

					final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(
							vertex,
							jmp.getTarget()
					);

					jmp.setEdge(edge);
					addEdge(edge);
				} else if (stmt instanceof ConditionalJumpStmt) {
					final ConditionalJumpStmt jmp = (ConditionalJumpStmt) stmt;

					final ConditionalJumpEdge<BasicBlock> edge = new ConditionalJumpEdge<>(
							vertex,
							jmp.getTrueSuccessor(),
							jmp.toOpcode()
					);

					jmp.setEdge(edge);
					addEdge(edge);
				} else if (stmt instanceof SwitchStmt) {
					final SwitchStmt jmp = (SwitchStmt) stmt;

					final DefaultSwitchEdge<BasicBlock> edge = new DefaultSwitchEdge<>(
							vertex,
							jmp.getDefaultTarget()
					);
					addEdge(edge);

					jmp.getTargets().entrySet().forEach(e -> {
						addEdge(new SwitchEdge<>(
								vertex,
								e.getValue(),
								e.getKey()
						));
					});
				}
			});
		}

		for (ExceptionRange<BasicBlock> range : ranges) {
			for (BasicBlock node : range.getNodes()) {
				addEdge(new TryCatchEdge<>(
						node,
						range
				));
			}
		}
	}

	public Set<FlowEdge<BasicBlock>> getPredecessors(Predicate<? super FlowEdge<BasicBlock>> e, BasicBlock b) {
		Stream<FlowEdge<BasicBlock>> s = getReverseEdges(b).stream();
		s = s.filter(e);
		return s.collect(Collectors.toSet());
	}

	public Set<FlowEdge<BasicBlock>> getSuccessors(Predicate<? super FlowEdge<BasicBlock>> e, BasicBlock b) {
		Stream<FlowEdge<BasicBlock>> s = getEdges(b).stream();
		s = s.filter(e);
		return s.collect(Collectors.toSet());
	}

	Set<FlowEdge<BasicBlock>> findImmediatesImpl(Set<FlowEdge<BasicBlock>> set) {
		Set<FlowEdge<BasicBlock>> iset = new HashSet<>();
		for(FlowEdge<BasicBlock> e : set) {
			if(e instanceof ImmediateEdge) {
				iset.add(e);
			}
		}
		return iset;
	}

	FlowEdge<BasicBlock> findSingleImmediateImpl(Set<FlowEdge<BasicBlock>> _set) {
		Set<FlowEdge<BasicBlock>> set = findImmediatesImpl(_set);
		int size = set.size();
		if(size == 0) {
			return null;
		} else if(size > 1) {
			throw new IllegalStateException(set.toString());
		} else {
			return set.iterator().next();
		}
	}

	public ImmediateEdge<BasicBlock> getImmediateEdge(BasicBlock b) {
		return (ImmediateEdge<BasicBlock>) findSingleImmediateImpl(getEdges(b));
	}

	public BasicBlock getImmediate(BasicBlock b) {
		FlowEdge<BasicBlock> e =  findSingleImmediateImpl(getEdges(b));
		if(e != null) {
			return e.dst();
		} else {
			return null;
		}
	}

	public BasicBlock getIncomingImmediate(BasicBlock b) {
		FlowEdge<BasicBlock> e =  findSingleImmediateImpl(getReverseEdges(b));
		if(e != null) {
			return e.src();
		} else {
			return null;
		}
	}

	public ImmediateEdge<BasicBlock> getIncomingImmediateEdge(BasicBlock b) {
		return (ImmediateEdge<BasicBlock>) findSingleImmediateImpl(getReverseEdges(b));
	}

	public List<BasicBlock> getJumpEdges(BasicBlock b) {
		List<BasicBlock> jes = new ArrayList<>();
		for (FlowEdge<BasicBlock> e : getEdges(b)) {
			if (!(e instanceof ImmediateEdge)) {
				jes.add(e.dst());
			}
		}
		return jes;
	}

	public List<BasicBlock> getJumpReverseEdges(BasicBlock b) {
		List<BasicBlock> jes = new ArrayList<>();
		for (FlowEdge<BasicBlock> e : getReverseEdges(b)) {
			if (!(e instanceof ImmediateEdge)) {
				jes.add(e.dst());
			}
		}
		return jes;
	}

	public boolean isHandler(BasicBlock b) {
		for(FlowEdge<BasicBlock> e : getReverseEdges(b)) {
			if(e instanceof TryCatchEdge) {
				if(e.dst() == b) {
					return true;
				} else {
					throw new IllegalStateException("incoming throw edge for " + b.getDisplayName() + " with dst " + e.dst().getDisplayName());
				}
			}
		}
		return false;
	}

	public List<ExceptionRange<BasicBlock>> getProtectingRanges(BasicBlock b) {
		List<ExceptionRange<BasicBlock>> ranges = new ArrayList<>();
		for(ExceptionRange<BasicBlock> er : getRanges()) {
			if(er.containsVertex(b)) {
				ranges.add(er);
			}
		}
		return ranges;
	}
	
	@Override
	public DotGraph makeDotGraph(IPropertyDictionary properties) {
		return CFGExporterUtils.makeDotGraph(this, properties);
	}
}
