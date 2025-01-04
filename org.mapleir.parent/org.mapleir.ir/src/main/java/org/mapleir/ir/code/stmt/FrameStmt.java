package org.mapleir.ir.code.stmt;

import lombok.Getter;
import lombok.Setter;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

@Getter @Setter
public class FrameStmt extends Stmt {
    // TODO: Add validation
    private int frameType;
    private Object[] frame;
    private Object[] stack;

    public FrameStmt(int frameType, Object[] frame, Object[] stack) {
        super(Opcode.FRAME);
        this.frameType = frameType;
        this.frame = frame;
        this.stack = stack;
    }

    @Override
    @Deprecated
    public void onChildUpdated(int ptr) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public void toString(TabbedStringWriter printer) {
        printer.print("// Frame: locals[" + frame.length + "] " + Arrays.toString(frame)
                + " stack[" + stack.length + "] " + Arrays.toString(stack));
    }

    @Override
    public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
        visitor.visitFrame(frameType, frame.length, frame, stack.length, stack);
    }

    @Override
    public boolean canChangeFlow() {
        return false;
    }

    @Override
    public boolean equivalent(CodeUnit s) {
        if (!(s instanceof FrameStmt))
            return false;

        final FrameStmt other = (FrameStmt) s;

        return Arrays.equals(other.frame, frame) && Arrays.equals(other.stack, stack);
    }

    @Override
    public Stmt copy() {
        return new FrameStmt(frameType, frame, stack);
    }
}
