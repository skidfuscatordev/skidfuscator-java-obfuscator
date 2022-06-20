package org.mapleir.ir.utils;

import java.util.Iterator;

import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.builtin.Colour;
import org.mapleir.dot4j.attr.builtin.ComplexLabel;
import org.mapleir.dot4j.attr.builtin.ComplexLabel.Justification;
import org.mapleir.dot4j.attr.builtin.Rank;
import org.mapleir.dot4j.attr.builtin.Shape;
import org.mapleir.dot4j.attr.builtin.Style;
import org.mapleir.dot4j.model.Context;
import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.dot4j.model.Factory;
import org.mapleir.flowgraph.edges.TryCatchEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.Stmt;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.util.PropertyHelper;
import org.mapleir.stdlib.collections.graph.GraphUtils;
import org.mapleir.stdlib.util.TabbedStringWriter;

public class CFGExporterUtils {

	public static final String OPT_EDGES = "decorate_edges";
	public static final String OPT_HIDE_HANDLER_EDGES = "hide_handler_edges";
	public static final String OPT_STMTS = "show_stmts";
	public static final String OPT_SIMPLE_EDGES = "simple_edges";
	
	public static DotGraph makeDotGraph(ControlFlowGraph cfg, IPropertyDictionary properties) {
		boolean showStmts = PropertyHelper.isSet(properties, OPT_STMTS);
		boolean labelEdges = PropertyHelper.isSet(properties, OPT_EDGES);
		boolean simplifyEdges = PropertyHelper.isSet(properties, OPT_SIMPLE_EDGES);
		boolean hideHandlerEdges = PropertyHelper.isSet(properties, OPT_HIDE_HANDLER_EDGES);

		DotGraph sourceGraph = Factory.graph().getGraphAttr().with(Rank.SOURCE);
		DotGraph sinkGraph = Factory.graph().getGraphAttr().with(Rank.SINK);
		
		DotGraph g = GraphUtils.makeDotSkeleton(cfg, (block, node) -> {
			StringBuilder sb = new StringBuilder();
			sb.append("<table>");
			{
				sb.append("<tr><td><font point-size=\"12\">").append(block.getDisplayName()).append("</font></td></tr>");
			}
			{
				sb.append("<tr><td>");
				if (showStmts) {
					StringBuilder stmtsStr = new StringBuilder();
					outputBlock(block, stmtsStr);
					sb.append(stmtsStr.toString());
				}
				sb.append("</td></tr>");
			}
			sb.append("</table>");
			
			Attrs colour;
			DotGraph rankGraph;
			if(cfg.getEntries().contains(block)) {
				colour = Colour.RED.fill();
				rankGraph = sourceGraph;
			} else if(cfg.getEdges(block).isEmpty()) {
				colour = Colour.GREEN.fill();
				rankGraph = sinkGraph;
			} else {
				colour = Attrs.attrs();
				rankGraph = null;
			}
			
			if(rankGraph != null) {
				Context.use(rankGraph, (ctx) -> {
					return rankGraph.addSource(Factory.node(node.getName()));
				});
			}
			
			node.with(Shape.BOX, Style.FILLED, colour,
					ComplexLabel.html(sb.toString()).justify(Justification.LEFT));
			return true;
		}, (e, dotE) -> {
			if (hideHandlerEdges && e instanceof TryCatchEdge) {
				// dotE.with(Style.INVIS);
				return false;
			}
			if (labelEdges) {
				dotE.with(ComplexLabel
						.of(simplifyEdges ? e.getClass().getSimpleName().replace("Edge", "") : e.toGraphString()));
			}
			return true;
		}).setDirected(true);
		
		g.addSource(sourceGraph);
		g.addSource(sinkGraph);
		
		return g;
	}

	private static void outputBlock(BasicBlock block, StringBuilder outStr) {
		Iterator<Stmt> it = block.iterator();
		HtmlTabbedStringWriter sw = new HtmlTabbedStringWriter();
		int insn = 0;
		while(it.hasNext()) {
			Stmt stmt = it.next();
			sw.print(insn++ + ". ");
			sw.sanitise = true;
			stmt.toString(sw);
			sw.sanitise = false;
			sw.print("\n");
		}
		outStr.append(sw.toString());
	}
	
	static class HtmlTabbedStringWriter extends TabbedStringWriter {
		boolean sanitise;
		
		private void printUnchecked(CharSequence str) {
			for (int i = 0; i < str.length(); i++) {
				super.print(str.charAt(i), true);
			}
		}
		
		@Override
		public TabbedStringWriter print(char c, boolean indent) {
			if(c == '\n') {
				printUnchecked("<br/>");
				return this;
			}
			
			if(sanitise) {
				if(Character.isAlphabetic(c) || Character.isDigit(c)) {
					super.print(c, indent);
				} else {
					printUnchecked("&#" + (int) c + ";");
				}
			} else {
				super.print(c, indent);
			}
			return this;
		}
	}
}
