package dev.skidfuscator.obf.skidasm.v2;

import dev.skidfuscator.obf.init.SkidSession;
import lombok.Data;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;

import java.util.List;

@Data
public class SMethod implements Renderable{
    private final MethodNode parent;
    private final List<SBlock> blocks;
    private final ControlFlowGraph cfg;

    private SMethodGroup group;

    public SMethod(MethodNode parent, List<SBlock> blocks, ControlFlowGraph cfg) {
        this.parent = parent;
        this.blocks = blocks;
        this.cfg = cfg;
    }

    @Override
    public void render(SkidSession session) {
        for (SBlock block : blocks) {
            block.render(session);
        }
    }
}
