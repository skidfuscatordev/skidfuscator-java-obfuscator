package dev.skidfuscator.obfuscator.frame_V2.frame;

import org.mapleir.ir.cfg.BasicBlock;
import dev.skidfuscator.obfuscator.frame_V2.frame.type.TypeHeader;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.objectweb.asm.Type;

import java.util.Set;


public class FrameNode implements FastGraphVertex {
    private final BasicBlock block;
    private TypeHeader pool;

    public FrameNode(BasicBlock block, TypeHeader pool) {
        this.block = block;
        this.pool = pool;
    }

    public void set(final int index, final Type type) {
        pool.set(index, type);
    }

    public Set<Type> compute(final int index) {
        return pool.get(index);
    }
    public void fill(final Type type) {
        pool.fill(type);
    }

    public BasicBlock getBlock() {
        return block;
    }

    public TypeHeader getPool() {
        return pool;
    }

    public void setPool(TypeHeader pool) {
        this.pool = pool;
    }

    @Override
    public int getNumericId() {
        return block.getNumericId();
    }

    @Override
    public String getDisplayName() {
        return block.getDisplayName();
    }
}
