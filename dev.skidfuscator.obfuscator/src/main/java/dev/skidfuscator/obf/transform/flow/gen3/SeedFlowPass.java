package dev.skidfuscator.obf.transform.flow.gen3;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidBlock;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.transform.flow.FlowPass;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import org.mapleir.ir.cfg.BasicBlock;

import java.util.*;

public class SeedFlowPass implements FlowPass {
    private final Map<BasicBlock, SkidBlock> blocks = new HashMap<>();

    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            if (methodNode.getNode().isAbstract())
                continue;

            //run(session.getCxt().getIRCache().get(methodNode));
        }
    }
}
