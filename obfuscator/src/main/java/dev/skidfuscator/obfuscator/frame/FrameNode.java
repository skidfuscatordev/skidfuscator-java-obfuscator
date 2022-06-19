package dev.skidfuscator.obfuscator.frame;

import dev.skidfuscator.obfuscator.skidasm.SkidExpressionPool;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.ExpressionPool;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;
import java.util.Arrays;


public class FrameNode implements FastGraphVertex {
    private final BasicBlock block;
    private SkidExpressionPool pool;

    public FrameNode(BasicBlock block, SkidExpressionPool pool) {
        this.block = block;
        this.pool = pool;
    }

    public void set(final int index, final Type type) {
        pool.set(index, type);
    }

    public Type compute(final int index) {
        return pool.get(index);
    }

    @Deprecated
    public Type get(final int index) {
        return pool.getTypes()[index];
    }

    public void fill(final Type type) {
        Arrays.fill(pool.getTypes(), type);
    }

    public BasicBlock getBlock() {
        return block;
    }

    public SkidExpressionPool getPool() {
        return pool;
    }

    public void setPool(SkidExpressionPool pool) {
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
