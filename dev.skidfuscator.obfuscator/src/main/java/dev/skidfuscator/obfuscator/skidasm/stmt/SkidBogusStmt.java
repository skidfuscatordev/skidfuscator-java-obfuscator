package dev.skidfuscator.obfuscator.skidasm.stmt;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;

public class SkidBogusStmt extends Stmt {
    private final BogusType type;
    public SkidBogusStmt(BogusType type) {
        super(Opcode.UNCOND_JUMP);
        this.type = type;
    }

    @Override
    public void onChildUpdated(int ptr) {

    }

    @Override
    public void toString(TabbedStringWriter printer) {

    }

    @Override
    public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {

    }

    @Override
    public boolean canChangeFlow() {
        return false;
    }

    @Override
    public boolean equivalent(CodeUnit s) {
        return false;
    }

    @Override
    public Stmt copy() {
        return new SkidBogusStmt(type);
    }

    public BogusType getType() {
        return type;
    }

    public enum BogusType {
        EXCEPTION,
        IMMEDIATE;
    }
}
