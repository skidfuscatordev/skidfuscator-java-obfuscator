package dev.skidfuscator.obfuscator.creator;

import com.google.common.collect.Lists;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.FlowEdges;
import org.mapleir.flowgraph.edges.ImmediateEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.collections.graph.*;
import org.mapleir.stdlib.collections.graph.algorithms.SimpleDfs;
import org.mapleir.stdlib.collections.graph.algorithms.TarjanSCC;
import org.mapleir.stdlib.collections.list.IndexedList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.mapleir.asm.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SkidFlowGraphDumper implements BytecodeFrontend {
	private final Skidfuscator skidfuscator;
	private final ControlFlowGraph cfg;
	private final MethodNode m;
	private IndexedList<BasicBlock> order;
	private LabelNode terminalLabel; // synthetic last label for malformed ranges
	private Map<BasicBlock, LabelNode> labels;
	public SkidFlowGraphDumper(Skidfuscator skidfuscator, ControlFlowGraph cfg, MethodNode m) {
		this.skidfuscator = skidfuscator;
		this.cfg = cfg;
		this.m = m;
	}

	public void dump() {
		// Clear methodnode
		m.node.instructions.clear();
		m.node.tryCatchBlocks.clear();
		m.node.visitCode();

		labels = new HashMap<>();
		for (BasicBlock b : cfg.vertices()) {
			labels.put(b, new LabelNode());
		}

		// Linearize
		linearize();

		// Fix edges
		naturalise();

		// Sanity check linearization
		verifyOrdering();

		// Dump code
		for (BasicBlock b : order) {
			m.node.visitLabel(getLabel(b));
			for (Stmt stmt : b) {
				stmt.toCode(m.node, this);
			}
		}
		terminalLabel = new LabelNode();
		m.node.visitLabel(terminalLabel.getLabel());

		// Dump ranges
		for (ExceptionRange<BasicBlock> er : cfg.getRanges()) {
			dumpRange(er);
		}
		
		// Sanity check
		verifyRanges();
		
		m.node.visitEnd();
	}
	
	private void linearize() {
		if (cfg.getEntries().size() != 1)
			throw new IllegalStateException("CFG doesn't have exactly 1 entry");
		BasicBlock entry = cfg.getEntries().iterator().next();
		
		// Build bundle graph
		Map<BasicBlock, BlockBundle> bundles = new HashMap<>();
		Map<BlockBundle, List<BlockBundle>> bunches = new HashMap<>();
		
		// Build bundles
		List<BasicBlock> topoorder = new SimpleDfs<>(cfg, entry, SimpleDfs.TOPO).getTopoOrder();
		for (BasicBlock b : topoorder) {
			if (bundles.containsKey(b)) // Already in a bundle
				continue;
			
			if (b.cfg.getIncomingImmediateEdge(b) != null) // Look for heads of bundles only
				continue;
			
			BlockBundle bundle = new BlockBundle();
			while (b != null) {
				bundle.add(b);
				bundles.put(b, bundle);
				b = b.cfg.getImmediate(b);
			}
			
			List<BlockBundle> bunch = new ArrayList<>();
			bunch.add(bundle);
			bunches.put(bundle, bunch);
		}
		
		// Group bundles by exception ranges
		for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
			BlockBundle prevBundle = null;
			for (BasicBlock b : range.getNodes()) {
				BlockBundle curBundle = bundles.get(b);
				if (prevBundle == null) {
					prevBundle = curBundle;
					continue;
				}
				if (curBundle != prevBundle) {
					List<BlockBundle> bunchA = bunches.get(prevBundle);
					List<BlockBundle> bunchB = bunches.get(curBundle);
					if (bunchA != bunchB) {
						bunchA.addAll(bunchB);
						for (BlockBundle bundle : bunchB) {
							bunches.put(bundle, bunchA);
						}
					}
					prevBundle = curBundle;
				}
			}
		}
		
		// Rebuild bundles
		bundles.clear();
		for (Map.Entry<BlockBundle, List<BlockBundle>> e : bunches.entrySet()) {
			BlockBundle bundle = e.getKey();
			if (bundles.containsKey(bundle.getFirst()))
				continue;
			BlockBundle bunch = new BlockBundle();
			e.getValue().forEach(bunch::addAll);
			for (BasicBlock b : bunch)
				bundles.put(b, bunch);
		}
		
		// Connect bundle graph
		BundleGraph bundleGraph = new BundleGraph();
		BlockBundle entryBundle = bundles.get(entry);
		bundleGraph.addVertex(entryBundle);
		for (BasicBlock b : topoorder) {
			for (FlowEdge<BasicBlock> e : cfg.getEdges(b)) {
				if (e instanceof ImmediateEdge)
					continue;
				BlockBundle src = bundles.get(b);
				bundleGraph.addEdge(new FastGraphEdgeImpl<>(src, bundles.get(e.dst())));
			}
		}
		
		// Linearize & flatten
		order = new IndexedList<>();
		Set<BlockBundle> bundlesSet = new HashSet<>(bundles.values()); // for efficiency
		SkidFlowGraphDumper.linearize(bundlesSet, bundleGraph, entryBundle).forEach(order::addAll);
	}
	
	// Recursively apply Tarjan's SCC algorithm
	private static List<BlockBundle> linearize(Collection<BlockBundle> bundles, BundleGraph fullGraph, BlockBundle entryBundle) {
		BundleGraph subgraph = GraphUtils.inducedSubgraph(fullGraph, bundles, BundleGraph::new);

		// Experimental: kill backedges
		for (FastGraphEdge<BlockBundle> e : new HashSet<>(subgraph.getReverseEdges(entryBundle))) {
			subgraph.removeEdge(e);
		}
		
		// Find SCCs
		TarjanSCC<BlockBundle> sccComputor = new TarjanSCC<>(subgraph);
		sccComputor.search(entryBundle);
		for(BlockBundle b : bundles) {
			if(sccComputor.low(b) == -1) {
				sccComputor.search(b);
			}
		}
		
		// Flatten
		List<BlockBundle> order = new ArrayList<>();
		List<List<BlockBundle>> components = sccComputor.getComponents();
		if (components.size() == 1)
			order.addAll(components.get(0));
		else for (List<BlockBundle> scc : components) // Recurse
			order.addAll(linearize(scc, subgraph, chooseEntry(subgraph, scc)));
		return order;
	}
	
	private static BlockBundle chooseEntry(BundleGraph graph, List<BlockBundle> scc) {
		Set<BlockBundle> sccSet = new HashSet<>(scc);
		Set<BlockBundle> candidates = new HashSet<>(scc);
		candidates.removeIf(bundle -> { // No incoming edges from within the SCC.
			for (FastGraphEdge<BlockBundle> e : graph.getReverseEdges(bundle)) {
				if (sccSet.contains(e.src()))
					return true;
			}
			return false;
		});
		if (candidates.isEmpty())
			return scc.get(0);
		return candidates.iterator().next();
	}

	private void naturalise() {
		for (int i = 0; i < order.size(); i++) {
			BasicBlock b = order.get(i);
			for (FlowEdge<BasicBlock> e : new HashSet<>(cfg.getEdges(b))) {
				BasicBlock dst = e.dst();
				if (e instanceof ImmediateEdge && order.indexOf(dst) != i + 1) {
					// Fix immediates
					final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(b, dst);
					b.add(new UnconditionalJumpStmt(dst, edge));
					cfg.removeEdge(e);
					cfg.addEdge(edge);
				} else if (e instanceof UnconditionalJumpEdge && order.indexOf(dst) == i + 1) {
					// Remove extraneous gotos
					for (ListIterator<Stmt> it = b.listIterator(b.size()); it.hasPrevious(); ) {
						if (it.previous() instanceof UnconditionalJumpStmt) {
							it.remove();
							break;
						}
					}
					cfg.removeEdge(e);
					cfg.addEdge(new ImmediateEdge<>(b, dst));
				}
			}
		}
	}

	private void verifyOrdering() {
		ListIterator<BasicBlock> it = order.listIterator();
		while(it.hasNext()) {
			BasicBlock b = it.next();
			
			for(FlowEdge<BasicBlock> e: cfg.getEdges(b)) {
				if(e.getType() == FlowEdges.IMMEDIATE) {
					if(it.hasNext()) {
						BasicBlock n = it.next();
						it.previous();
						
						if(n != e.dst()) {
							throw new IllegalStateException("Illegal flow " + e + " > " + n);
						}
					} else {
						throw new IllegalStateException("Trailing " + e);
					}
				}
			}
		}
	}

	private void dumpRange(ExceptionRange<BasicBlock> er) {
		// Determine exception type
		Type type;
		Set<Type> typeSet = er.getTypes();
		if (typeSet.size() != 1) {
			// TODO: find base exception
			final Set<ClassNode> classNodes = new HashSet<>();
			for (Type typec : er.getTypes()) {
				final String type1 = typec.getClassName().replace(".", "/");
				ClassNode classNode = skidfuscator
						.getClassSource()
						.findClassNode(type1);

				if (classNode == null) {
					System.err.println("Failed to find class of type " + type1  + "!" );
					try {
						final ClassReader reader = new ClassReader(type1);
						final org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
						reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

						classNode = new SkidClassNode(node, skidfuscator);
						skidfuscator.getClassSource().getClassTree().addVertex(classNode);
					} catch (IOException e) {
						e.printStackTrace();
						continue;
					}
				}

				classNodes.add(classNode);

				/*final List<ClassNode> parents = skidfuscator
						.getClassSource()
						.getClassTree()
						.getAllParents(classNode);

				if (stack.isEmpty()) {

					stack.addAll(Lists.reverse(parents));
				} else {
					final Stack<ClassNode> toIterate = new Stack<>();
					toIterate.add(classNode);
					toIterate.addAll(Lists.reverse(parents));

					runner: {
						while (!stack.isEmpty()) {

							for (ClassNode node : toIterate) {
								if (node.getName().equals(stack.peek().getName()))
									break runner;
							}

							stack.pop();
						}

						throw new IllegalStateException("Could not find common exception type between "
								+ Arrays.toString(er.getTypes().toArray()));
					}
				}*/
			}

			/* Simple DFS naive common ancestor algorithm */
			/*final ClassNode seedClassNodeForIteration = classNodes.iterator().next();
			final Stack<ClassNode> hierarchy = new Stack<>();
			hierarchy.addAll(Lists.reverse(
						skidfuscator
							.getClassSource()
							.getClassTree()
							.getAllParents(seedClassNodeForIteration)
					)
			);
			hierarchy.add(seedClassNodeForIteration);

			while (true) {
				final Set<ClassNode> children = new HashSet<>(
						skidfuscator.getClassSource()
							.getClassTree()
							.getAllChildren(hierarchy.peek())
				);

				if (children.containsAll(classNodes)) {
					break;
				}

				System.err.println("Failed for " + hierarchy.peek().getName());
				System.err.println("Looking for: " + Arrays.toString(classNodes.stream().map(ClassNode::getName).toArray()));
				for (ClassNode child : children) {
					System.err.println("    -> " + child.getName());
				}

				hierarchy.pop();
			}*/

			final Collection<ClassNode> commonAncestors = skidfuscator
					.getClassSource()
					.getClassTree()
					.getCommonAncestor(classNodes);

			assert commonAncestors.size() > 0 : "No common ancestors between exceptions!";
			ClassNode mostIdealAncestor = null;

			if (commonAncestors.size() == 1) {
				mostIdealAncestor = commonAncestors.iterator().next();
			} else {
				iteration: {
					for (ClassNode commonAncestor : commonAncestors) {
						Stack<ClassNode> parents = new Stack<>();
						parents.add(commonAncestor);

						while (!parents.isEmpty()) {
							if (parents.peek()
									.getName()
									.equalsIgnoreCase("java/lang/Throwable")) {
								mostIdealAncestor = commonAncestor;
								break iteration;
							}

							parents.addAll(skidfuscator.getClassSource().getClassTree().getParents(parents.pop()));
						}
					}
				}
			}

			assert mostIdealAncestor != null : "Exception parent not found in dumper!";

			type = Type.getObjectType(mostIdealAncestor.getName());
		} else {
			type = typeSet.iterator().next();
		}
		
		final Label handler = getLabel(er.getHandler());
		List<BasicBlock> range = new ArrayList<>(er.getNodes());
		range.sort(Comparator.comparing(order::indexOf));
		
		Label start;
		int rangeIdx = -1, orderIdx;
		do {
			if (++rangeIdx == range.size()) {
				System.err.println("[warn] range is absent: " + m);
				return;
			}
			BasicBlock b = range.get(rangeIdx);
			orderIdx = order.indexOf(b);
			start = getLabel(b);
		} while (orderIdx == -1);
		
		for (;;) {
			// check for endpoints
			if (orderIdx + 1 == order.size()) { // end of method
				m.node.visitTryCatchBlock(start, terminalLabel.getLabel(), handler, type.getInternalName());
				break;
			} else if (rangeIdx + 1 == range.size()) { // end of range
				Label end = getLabel(order.get(orderIdx + 1));
				m.node.visitTryCatchBlock(start, end, handler, type.getInternalName());
				break;
			}
			
			// check for discontinuity
			BasicBlock nextBlock = range.get(rangeIdx + 1);
			int nextOrderIdx = order.indexOf(nextBlock);
			if (nextOrderIdx - orderIdx > 1) { // blocks in-between, end the handler and begin anew
				System.err.println("[warn] Had to split up a range: " + m);
				Label end = getLabel(order.get(orderIdx + 1));
				m.node.visitTryCatchBlock(start, end, handler, type.getInternalName());
				start = getLabel(nextBlock);
			}

			// next
			rangeIdx++;
			if (nextOrderIdx != -1)
				orderIdx = nextOrderIdx;
		}
	}
	
	private void verifyRanges() {
		for (TryCatchBlockNode tc : m.node.tryCatchBlocks) {
			int start = -1, end = -1, handler = -1;
			for (int i = 0; i < m.node.instructions.size(); i++) {
				AbstractInsnNode ain = m.node.instructions.get(i);
				if (!(ain instanceof LabelNode))
					continue;
				Label l = ((LabelNode) ain).getLabel();
				if (l == tc.start.getLabel())
					start = i;
				if (l == tc.end.getLabel()) {
					if (start == -1)
						throw new IllegalStateException("Try block end before start " + m);
					end = i;
				}
				if (l == tc.handler.getLabel()) {
					handler = i;
				}
			}
			if (start == -1 || end == -1 || handler == -1)
				throw new IllegalStateException("Try/catch endpoints missing: " + start + " " + end + " " + handler + m);
		}
	}

	@Override
	public Label getLabel(BasicBlock b) {
		return labels.get(b).getLabel();
	}

	@Override
	public ControlFlowGraph getGraph() {
		return cfg;
	}

	private static class BundleGraph extends FastDirectedGraph<BlockBundle, FastGraphEdge<BlockBundle>> { }
	@SuppressWarnings("serial")
	private static class BlockBundle extends ArrayList<BasicBlock> implements FastGraphVertex {
		private BasicBlock first = null;
		
		private BasicBlock getFirst() {
			if (first == null)
				first = get(0);
			return first;
		}
		
		@Override
		public String getDisplayName() {
			return getFirst().getDisplayName();
		}
		
		@Override
		public int getNumericId() {
			return getFirst().getNumericId();
		}
		
		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			for (Iterator<BasicBlock> it = this.iterator(); it.hasNext(); ) {
				BasicBlock b = it.next();
				s.append(b.getDisplayName());
				if (it.hasNext())
					s.append("->");
			}
			return s.toString();
		}
		
		@Override
		public int hashCode() {
			return getFirst().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof BlockBundle))
				return false;
			return ((BlockBundle) o).getFirst().equals(getFirst());
		}
	}
}
