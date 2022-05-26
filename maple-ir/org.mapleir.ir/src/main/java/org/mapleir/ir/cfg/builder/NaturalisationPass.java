package org.mapleir.ir.cfg.builder;

import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.edges.FlowEdge;
import org.mapleir.flowgraph.edges.FlowEdges;
import org.mapleir.flowgraph.edges.TryCatchEdge;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.CaughtExceptionExpr;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.utils.CFGUtils;
import org.mapleir.stdlib.collections.graph.algorithms.SimpleDfs;

import java.util.*;
import java.util.Map.Entry;

public class NaturalisationPass extends ControlFlowGraphBuilder.BuilderPass {

	public NaturalisationPass(ControlFlowGraphBuilder builder) {
		super(builder);
	}

	@Override
	public void run() {
		//mergeImmediates();
		resolveNaturalHandlerFlow();
	}

	// Resolve natural flow into handlers by splitting handler edges up.
	private int resolveNaturalHandlerFlow() {
		int fixed = 0;
		for(BasicBlock b : SimpleDfs.topoorder(builder.graph, builder.head)) {
			boolean hasHandler = false, hasNatural = false;
			for (FlowEdge<BasicBlock> e : builder.graph.getReverseEdges(b)) {
				if (e instanceof TryCatchEdge)
					hasHandler = true;
				else
					hasNatural = true;
				if (hasHandler && hasNatural)
					break;
			}

			if (!(hasHandler && hasNatural)) {
				// control flow is not problematic.
				continue;
			}

			/*
			Before:
			Block A:
			    svar0 = null -- the stack heights should *never* be misaligned; svar0 will always have SOMETHING
			    immediateEdge to H

			Block H:
			    svar0 = catch()
			  	<do exception stuff>
			  	return

			Block B: handler to H
			  	<do stuff>
			  	return

			We will "split" the handler edge by decapitating H and moving the catch() statement into a stub.

			After:
			Block A:
				svar0 = null
				immediateEdge to H

			Block H:
			  	<do exception stuff>
				return

			Block B: handler to H1
				<do stuff>
				return

			Block H1:
				svar0 = catch()
				goto H
			*/

			assert (b.get(0) instanceof CopyVarStmt);
			CopyVarStmt catchCopy = (CopyVarStmt) b.get(0);
			assert (catchCopy.getExpression() instanceof CaughtExceptionExpr);

			// decapitate, doesn't update edges
			BasicBlock newHandlerHead = CFGUtils.splitBlockSimpleFactory(builder.factory, builder.graph, b, 1);
			final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(newHandlerHead, b);
			newHandlerHead.add(
					builder.factory.unconditional_jump_stmt()
							.target(b)
							.edge(edge)
							.build()
			);
			builder.graph.addEdge(edge);
			// update assigns map
			builder.assigns.get(catchCopy.getVariable().getLocal()).add(newHandlerHead);
			search: { // ugly.
				for (Stmt stmt : b) {
					if (stmt instanceof CopyVarStmt) {
						CopyVarStmt cvs = (CopyVarStmt) stmt;
						if (cvs.getVariable().getLocal().equals(catchCopy.getVariable().getLocal())) {
							break search; // there is another copy to svar0
						}
					}
				}
				// We just eliminated the only copy to svar0, remove from the assigns map.
				builder.assigns.get(catchCopy.getVariable().getLocal()).remove(b);
			}

			// update ranges to point to new handler head
			for(ExceptionRange<BasicBlock> er : builder.graph.getRanges()) {
				if (er.getHandler().equals(b)) {
					er.setHandler(newHandlerHead);
				}
			}

			// update handler edges
			for (FlowEdge<BasicBlock> e : builder.graph.getPredecessors(e -> e instanceof TryCatchEdge, b)) {
				// redirect handler to point at new handler head
				TryCatchEdge<BasicBlock> handlerEdge = (TryCatchEdge<BasicBlock>) e;
				builder.graph.addEdge(handlerEdge.clone(handlerEdge.src(), null));
				builder.graph.removeEdge(e);
			}

			fixed++;
		}
		return fixed;
	}

	int mergeImmediates() {
		class MergePair {
			final BasicBlock src;
			final BasicBlock dst;
			MergePair(BasicBlock src, BasicBlock dst)  {
				this.src = src;
				this.dst = dst;
			}
		}
		
		List<MergePair> merges = new ArrayList<>();
		Map<BasicBlock, BasicBlock> remap = new HashMap<>();
		Map<BasicBlock, List<ExceptionRange<BasicBlock>>> ranges = new HashMap<>();

		for(BasicBlock b : SimpleDfs.topoorder(builder.graph, builder.head)) {
			BasicBlock in = b.cfg.getIncomingImmediate(b);
			if(in == null) {
				continue;
			}
			if(in.isFlagSet(BasicBlock.FLAG_NO_MERGE)) {
				continue;
			}
			Set<FlowEdge<BasicBlock>> inSuccs = in.cfg.getSuccessors(e -> !(e instanceof TryCatchEdge), in);
			/*
			 * In a nutshell, to be able to properly merge two blocks, we're looking for this
			 * exact scenario:
			 *
			 *   B.. B..
			 *    \ /      <-- No matters. We don't really care
			 *    BX1 (in)
			 *     |       <-- CRITICAL! Must only be ONE edge which is a successor and
			 *     v           only one reverse edge for the successor
			 *    BX2 (b)
			 *    / \      <-- We don't really care from here on out
			 *   B.. B.;
			 */
			if(inSuccs.size() != 1 || builder.graph.getReverseEdges(b).size() != 1) {
				continue;
			}
			
			List<ExceptionRange<BasicBlock>> range1 = b.cfg.getProtectingRanges(b);
			List<ExceptionRange<BasicBlock>> range2 = in.cfg.getProtectingRanges(in);
			
			if(!range1.equals(range2)) {
				continue;
			}
			
			ranges.put(b, range1);
			ranges.put(in, range2);
			
			merges.add(new MergePair(in, b));
			
			remap.put(in, in);
			remap.put(b, b);
		}
		
		for(MergePair p : merges) {
			BasicBlock src = remap.get(p.src);
			BasicBlock dst = p.dst;
			
			dst.transfer(src);
			
			for(FlowEdge<BasicBlock> e : builder.graph.getEdges(dst)) {
				// since the ranges are the same, we don't need
				// to clone these.
				if(e.getType() != FlowEdges.TRYCATCH) {
					BasicBlock edst = e.dst();
					edst = remap.getOrDefault(edst, edst);
					builder.graph.addEdge(e.clone(src, edst));
				}
			}
			builder.graph.removeVertex(dst);
			
			remap.put(dst, src);
			
			for(ExceptionRange<BasicBlock> r : ranges.get(src)) {
				r.removeVertex(dst);
			}
			for(ExceptionRange<BasicBlock> r : ranges.get(dst)) {
				r.removeVertex(dst);
			}
			
			// System.out.printf("Merged %s into %s.%n", dst.getDisplayName(), src.getDisplayName());
		}
		
		// we need to update the assigns map if we change the cfg.
		for(Entry<Local, Set<BasicBlock>> e : builder.assigns.entrySet()) {
			Set<BasicBlock> set = e.getValue();
			Set<BasicBlock> copy = new HashSet<>(set);
			for(BasicBlock b : copy) {
				BasicBlock r = remap.getOrDefault(b, b);
				if(r != b) {
					set.remove(b);
					set.add(r);
				}
			}
		}
		
		return merges.size();
	}
}
