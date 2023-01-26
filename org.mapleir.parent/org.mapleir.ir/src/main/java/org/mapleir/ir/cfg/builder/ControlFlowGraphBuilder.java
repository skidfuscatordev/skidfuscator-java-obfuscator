package org.mapleir.ir.cfg.builder;

import org.apache.log4j.Logger;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.SSAFactory;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.DefaultBlockFactory;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.impl.StaticMethodLocalsPool;
import org.mapleir.ir.locals.impl.VirtualMethodLocalsPool;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;
import org.mapleir.asm.MethodNode;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class ControlFlowGraphBuilder {

	private static final Logger LOGGER = Logger.getLogger(ControlFlowGraph.class);

	public final MethodNode method;
	protected final SSAFactory factory;
	protected final ControlFlowGraph graph;
	protected final Set<Local> locals;
	protected final NullPermeableHashMap<Local, Set<BasicBlock>> assigns;
	protected BasicBlock head;
	protected final boolean optimise;

	public ControlFlowGraphBuilder(MethodNode method) {
		this(method, DefaultBlockFactory.INSTANCE, true);
	}

	public ControlFlowGraphBuilder(MethodNode method, SSAFactory factory) {
		this(method, factory, true);
	}

	public ControlFlowGraphBuilder(MethodNode method, SSAFactory factory, boolean optimise) {
		this.optimise = optimise;
		this.method = method;
		this.factory = factory;
		if(Modifier.isStatic(method.node.access)) {
			graph = factory.cfg()
					.localsPool(new StaticMethodLocalsPool())
					.method(method)
					.build();
		} else {
			graph = factory.cfg()
					.localsPool(new VirtualMethodLocalsPool())
					.method(method)
					.build();
		}
		locals = new HashSet<>();

		// Reserved self reference local
		if (!method.isStatic()) {
			locals.add(graph.getLocals().get(0, false));
		}

		assigns = new NullPermeableHashMap<>(HashSet::new);
	}
	
	public static abstract class BuilderPass {
		protected final ControlFlowGraphBuilder builder;
		
		public BuilderPass(ControlFlowGraphBuilder builder) {
			this.builder = builder;
		}
		
		public abstract void run();
	}
	
	protected BuilderPass[] resolvePasses() {
		return new BuilderPass[] {
				new GenerationPassV2(this),
				new DeadBlocksPass(this),
				new NaturalisationPass(this),
				new SSAGenPass(this, optimise),
		};
	}
	
	public ControlFlowGraph buildImpl() {
		for(BuilderPass p : resolvePasses()) {
			p.run();
			// CFGUtils.easyDumpCFG(graph, "post-" + p.getClass().getSimpleName());
		}
		return graph;
	}

	public static ControlFlowGraph build(MethodNode method) {
		return build(method, DefaultBlockFactory.INSTANCE);
	}

	public static ControlFlowGraph build(MethodNode method, SSAFactory factory) {
		ControlFlowGraphBuilder builder = new ControlFlowGraphBuilder(method, factory);
		return builder.buildImpl();
	}
}
