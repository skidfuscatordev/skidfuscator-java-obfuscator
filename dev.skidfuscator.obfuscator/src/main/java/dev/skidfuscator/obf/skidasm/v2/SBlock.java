package dev.skidfuscator.obf.skidasm.v2;

import dev.skidfuscator.obf.init.SkidSession;
import lombok.Data;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;

@Data
public class SBlock implements Renderable {
    private final BasicBlock block;
    private final int seed;

    public SBlock(BasicBlock block, int seed) {
        this.block = block;
        this.seed = seed;
    }

    @Override
    public void render(SkidSession session) {
        
    }
}
