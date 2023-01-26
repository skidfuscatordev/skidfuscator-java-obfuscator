package dev.skidfuscator.obfuscator.creator;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.creator.pass.SkidLocalsReallocator;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlockFactory;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.algorithms.BoissinotDestructor;
import org.mapleir.ir.algorithms.LocalsReallocator;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.SSAFactory;
import org.mapleir.ir.cfg.builder.*;

public class SkidFlowGraphBuilder extends ControlFlowGraphBuilder {
    private final Skidfuscator skidfuscator;

    public SkidFlowGraphBuilder(MethodNode method, Skidfuscator skidfuscator) {
        super(method);
        this.skidfuscator = skidfuscator;
    }

    public SkidFlowGraphBuilder(MethodNode method, SSAFactory factory, Skidfuscator skidfuscator) {
        super(method, factory);
        this.skidfuscator = skidfuscator;
    }

    public SkidFlowGraphBuilder(MethodNode method, SSAFactory factory, boolean optimise, Skidfuscator skidfuscator) {
        super(method, factory, optimise);
        this.skidfuscator = skidfuscator;
    }

    @Override
    protected BuilderPass[] resolvePasses() {
        return new BuilderPass[] {
                new SkidGenerationPass(this, skidfuscator),
                new DeadBlocksPass(this),
                //new LocalFixerPass(this),
                //new NaturalisationPass(this),
                new SSAGenPass(this, false)
                //new CreationFixer(this)
        };
    }

    public static ControlFlowGraph build(final Skidfuscator skidfuscator, final MethodNode method) {
        ControlFlowGraphBuilder builder = new SkidFlowGraphBuilder(method, SkidBlockFactory.v(skidfuscator), skidfuscator);
        final ControlFlowGraph cfg = builder.buildImpl();
        BoissinotDestructor.leaveSSA(cfg);
        SkidLocalsReallocator.realloc(skidfuscator, cfg);

        return cfg;
    }
}
