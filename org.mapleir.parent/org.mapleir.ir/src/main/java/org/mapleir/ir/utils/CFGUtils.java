package org.mapleir.ir.utils;

import org.mapleir.dot4j.Exporter;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.*;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.SSAFactory;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.stmt.ConditionalJumpStmt;
import org.mapleir.ir.code.stmt.SwitchStmt;
import org.mapleir.ir.code.stmt.ThrowStmt;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.impl.BooleanProperty;
import org.mapleir.propertyframework.util.PropertyHelper;
import org.mapleir.stdlib.collections.bitset.GenericBitSet;
import org.mapleir.stdlib.collections.graph.algorithms.SimpleDfs;
import org.mapleir.stdlib.util.TabbedStringWriter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CFGUtils {
	/**
	 * Split the block upto statement index `to`, exclusively, into a new block.
	 * Updates edges and ranges nicely.
	 * @return the newly created block containing the instructions before `to`
	 */
	public static BasicBlock splitBlock(SSAFactory factory, ControlFlowGraph cfg, BasicBlock b, int to) {
		return splitBlock(factory, cfg, b, to, false);
	}

	// Please don't call me, call splitBlock(ControlFlowGraph, BasicBlock, int) instead.
	@Deprecated
	public static BasicBlock splitBlock(SSAFactory factory, ControlFlowGraph cfg, BasicBlock b, int to, boolean ssagencheck) {
		/* eg. split the block as follows:
		 *
		 *  NAME:
		 *    stmt1
		 *    stmt2
		 *    stmt3
		 *    stmt4
		 *    stmt5
		 *    jump L1, L2
		 *   [jump edge to L1]
		 *   [jump edge to L2]
		 *   [exception edges]
		 *
		 * split at 3, create a new block (incoming
		 * immediate), transfer instruction from 0
		 * to index into new block, create immediate
		 * edge to old block, clone exception edges,
		 * redirect pred edges.
		 *
		 * 1/9/16: we also need to modify the last
		 *         statement of the pred blocks to
		 *         point to NAME'.
		 *
		 *  NAME':
		 *    stmt1
		 *    stmt2
		 *    stmt3
		 *   [immediate to NAME]
		 *  NAME:
		 *    stmt4
		 *    stmt5
		 *    jump L1, L2
		 *   [jump edge to L1]
		 *   [jump edge to L2]
		 *   [exception edges]
		 */

		// split block
		BasicBlock newBlock = splitBlockSimpleFactory(factory, cfg, b, to);

		// redo ranges
		for(ExceptionRange<BasicBlock> er : cfg.getRanges()) {
			if (er.containsVertex(b))
				er.addVertexBefore(b, newBlock);
		}

		// redirect b preds into newBlock and remove them.
		Set<FlowEdge<BasicBlock>> oldEdges = new HashSet<>(cfg.getReverseEdges(b));
		for (FlowEdge<BasicBlock> e : oldEdges) {
			BasicBlock p = e.src();
			FlowEdge<BasicBlock> c;
			if (e instanceof TryCatchEdge) { // b is ehandler
				TryCatchEdge<BasicBlock> tce = (TryCatchEdge<BasicBlock>) e;
				if (tce.dst() != tce.erange.getHandler()) {
					if ((tce.dst() == newBlock || tce.erange.getHandler() != newBlock)) {
						System.err.println("Offending postsplit block: " + b);
						System.err.println("Offending newblock: " + newBlock);
						System.err.println("Offending edge: " + tce);
						System.err.println("Offending erange: " + tce.erange);
						System.err.println(cfg);
						throw new AssertionError("Very odd split case. please investigate");
					}
					cfg.addEdge(tce.clone(tce.src(), null)); // null = use ehandler for dst
					cfg.removeEdge(tce);
				}
				if (tce.erange.getHandler() != newBlock) {
					tce.erange.setHandler(newBlock);
					cfg.addEdge(tce.clone(tce.src(), null));
					cfg.removeEdge(tce);
				}
			} else {
				c = e.clone(p, newBlock);
				cfg.addEdge(c);
				cfg.removeEdge(e);
			}

			// Fix flow instruction targets
			if (!p.isEmpty()) {
				Stmt last = p.get(p.size() - 1);
				int op = last.getOpcode();
				if (e instanceof ConditionalJumpEdge) {
					if (op != Opcode.COND_JUMP)
						throw new IllegalArgumentException("wrong flow instruction: Got " + Opcode.opname(op) + " Expected: COND_JUMP");
					ConditionalJumpStmt j = (ConditionalJumpStmt) last;
//					assertTarget(last, j.getTrueSuccessor(), b);
					if (j.getTrueSuccessor() == b)
						j.setTrueSuccessor(newBlock);
				} else if (e instanceof UnconditionalJumpEdge) {
					if (op != Opcode.UNCOND_JUMP)
						throw new IllegalArgumentException("wrong flow instruction, got " + last.getOpname() + " instead");
					UnconditionalJumpStmt j = (UnconditionalJumpStmt) last;
					BasicBlock t = j.getTarget();
					if(t != b) {
						System.err.println(cfg);
						System.err.println(j.getBlock());
						throw new IllegalStateException(j + ", "+ t.getDisplayName() + " != " + b.getDisplayName());
					}
					j.setTarget(newBlock);
				} else if (e instanceof SwitchEdge) {
					if (op != Opcode.SWITCH_JUMP)
						throw new IllegalArgumentException("wrong flow instruction.");
					SwitchStmt s = (SwitchStmt) last;
					for (Map.Entry<Integer, BasicBlock> en : s.getTargets().entrySet()) {
						BasicBlock t = en.getValue();
						if (t == b) {
							en.setValue(newBlock);
						}
					}
				}
			}
		}


		if (ssagencheck && !checkCloneHandler(newBlock)) {
			System.err.println(cfg);
			System.err.println(newBlock.getDisplayName());
			System.err.println(b.getDisplayName());
			throw new IllegalStateException("the new block should always need a handler..?");
		}

		// clone exception edges
		for (FlowEdge<BasicBlock> e : cfg.getEdges(b)) {
			if (e.getType() == FlowEdges.TRYCATCH) {
				TryCatchEdge<BasicBlock> c = ((TryCatchEdge<BasicBlock>) e).clone(newBlock, null); // second param is discarded (?)
				cfg.addEdge(c);
			}
		}

		// create immediate to newBlock
		cfg.addEdge(new ImmediateEdge<>(newBlock, b));

		return newBlock;
	}

	private static boolean checkCloneHandler(BasicBlock b) {
		if (b.isEmpty())
			throw new IllegalArgumentException("empty block after split?");
		// backwards iteration is faster
		for (ListIterator<Stmt> it = b.listIterator(b.size()); it.hasPrevious(); ) {
			if (checkCloneHandler(it.previous()))
				return true;
		}
		return false;
	}

	private static boolean checkCloneHandler(Stmt stmt) {
		if (stmt instanceof CopyVarStmt) {
			CopyVarStmt copy = (CopyVarStmt) stmt;
			int opc = copy.getExpression().getOpcode();
			if (!copy.isSynthetic() && opc != Opcode.LOCAL_LOAD && opc != Opcode.CATCH)
				return true;
		} else if (stmt.canChangeFlow()) {
			if (stmt instanceof ThrowStmt)
				return true;
			// for ssagenpass, no need to check child exprs as no complex subexprs can occur before propagation.
			// otherwise, we do need to check.
		} else {
			return true;
		}
		return false;
	}

	/**
	 * Splits block up to index `to`, exclusively. Doesn't update edges, etc.
	 * @return The new block, containing the split-off instructions
	 */
	public static BasicBlock splitBlockSimple(ControlFlowGraph cfg, BasicBlock b, int to) {
		BasicBlock newBlock = new BasicBlock(cfg);
		cfg.addVertex(newBlock);
		b.transferUpto(newBlock, to);
		return newBlock;
	}

	/**
	 * Splits block up to index `to`, exclusively. Doesn't update edges, etc.
	 * @return The new block, containing the split-off instructions
	 */
	public static BasicBlock splitBlockSimpleFactory(SSAFactory factory, ControlFlowGraph cfg, BasicBlock b, int to) {
		BasicBlock newBlock = factory.block().cfg(cfg).build();
		cfg.addVertex(newBlock);
		b.transferUpto(newBlock, to);
		return newBlock;
	}

	/**
	 * Splits block up to index `to`, exclusively. Doesn't update edges, etc.
	 * @return The new block, containing the split-off instructions
	 */
	public static BasicBlock splitBlockReverseFactory(SSAFactory factory, ControlFlowGraph cfg, BasicBlock b, int to) {
		//System.out.println("Old block: \n" + printBlock(b));
		BasicBlock newBlock = factory.block().cfg(cfg).build();
		cfg.addVertex(newBlock);

		final int size = b.size() - to;

		for (int i = 0; i < size; i++) {
			Stmt s = b.remove(b.size() - 1);

			newBlock.add(0, s);
			assert (s.getBlock() == newBlock);
		}


		if (cfg.getImmediateEdge(b) != null) {
			final ImmediateEdge<BasicBlock> dest = cfg.getImmediateEdge(b);
			cfg.removeEdge(dest);

			cfg.addEdge(new ImmediateEdge<>(
					newBlock,
					dest.dst()
			));
		}

		cfg.addEdge(new ImmediateEdge<>(
				b,
				newBlock
		));

		for (ExceptionRange<BasicBlock> protectingRange : cfg.getProtectingRanges(b)) {
			protectingRange.addVertex(newBlock);
			cfg.addEdge(new TryCatchEdge<>(
					newBlock,
					protectingRange
			));
		}

		//System.out.println("New old block: \n" + printBlock(b));
		//System.out.println("New block: \n" + printBlock(newBlock));

		//cfg.recomputeEdges();

		return newBlock;
	}

	/**
	 * Renders the cfg as an image using graphviz.
	 * This should be for debugging purposes only.
	 * @param cfg cfg to dump.
	 * @param filename output name without file extension.
	 */
	public static void easyDumpCFG(ControlFlowGraph cfg, String filename) {
		IPropertyDictionary dict = PropertyHelper.createDictionary();
		dict.put(new BooleanProperty(CFGExporterUtils.OPT_EDGES, true));
		dict.put(new BooleanProperty(CFGExporterUtils.OPT_STMTS, true));
		try {
			Exporter.fromGraph(CFGExporterUtils.makeDotGraph(cfg, dict)).export(new File("cfg testing", filename + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String printBlocks(Collection<BasicBlock> bbs) {
		TabbedStringWriter sw = new TabbedStringWriter();
		int insn = 1;
		for(BasicBlock bb : bbs) {
			blockToString(sw, bb.getGraph(), bb, insn);
			insn += bb.size();
		}
		return sw.toString();
	}

	public static String printBlock(BasicBlock b) {
		TabbedStringWriter sw = new TabbedStringWriter();
		blockToString(sw, b.getGraph(), b, 1);
		return sw.toString();
	}

	public static void blockToString(TabbedStringWriter sw, ControlFlowGraph cfg, BasicBlock b, int insn) {
		// sw.print("===#Block " + b.getId() + "(size=" + (b.size()) + ")===");
		sw.print(String.format("===#Block %s(size=%d, flags=%s)===", b.getDisplayName(), b.size(), Integer.toBinaryString(b.getFlags())));
		sw.tab();
		
		Iterator<Stmt> it = b.iterator();
		if(!it.hasNext()) {
			sw.untab();
		} else {
			sw.print("\n");
		}
		while(it.hasNext()) {
			Stmt stmt = it.next();
//			sw.print(stmt.getId() + ". ");
			sw.print(insn++ + ". ");
			stmt.toString(sw);
			
			if(!it.hasNext()) {
				sw.untab();
			} else {
				sw.print("\n");
			}
		}

		sw.tab();
		sw.tab();
		
		if(cfg.containsVertex(b)) {
			for(FlowEdge<BasicBlock> e : cfg.getEdges(b)) {
//				if(e.getType() != FlowEdges.TRYCATCH) {
					sw.print("\n-> " + e.toString());
//				}
			}

			for(FlowEdge<BasicBlock> p : cfg.getReverseEdges(b)) {
//				if(p.getType() != FlowEdges.TRYCATCH) {
					sw.print("\n<- " + p.toString());
//				}
			}
		}

		sw.untab();
		sw.untab();
		
		sw.print("\n");
	}

	public static BasicBlock deleteUnreachableBlocks(ControlFlowGraph cfg) {
		if (cfg.getEntries().size() != 1)
			throw new IllegalArgumentException();
		BasicBlock head = cfg.getEntries().iterator().next();
		GenericBitSet<BasicBlock> reachable = cfg.createBitSet(SimpleDfs.preorder(cfg, head));
		GenericBitSet<BasicBlock> unreachable = cfg.createBitSet(cfg.vertices());
		unreachable.removeAll(reachable);
		for (BasicBlock b : unreachable) {
			cfg.removeVertex(b);
		}
		return head;
	}
}
