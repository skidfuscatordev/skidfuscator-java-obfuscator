package org.mapleir.ir.code.stmt;

import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.codegen.BytecodeFrontend;
import org.mapleir.stdlib.util.TabbedStringWriter;
import org.objectweb.asm.MethodVisitor;

public class LineNumberStmt extends Stmt {
    private int line;

    public LineNumberStmt(int line) {
        super(Opcode.LINE_NO);
        this.line = line;
    }

    @Override
    public void onChildUpdated(int ptr) {

    }

    @Override
    public void toString(TabbedStringWriter printer) {

    }

    @Override
    public void toCode(MethodVisitor visitor, BytecodeFrontend assembler) {
        visitor.visitLineNumber(
                line,
                assembler.getLabel(this.getBlock())
        );
    }

    @Override
    public boolean canChangeFlow() {
        return false;
    }

    @Override
    public boolean equivalent(CodeUnit s) {
        return s instanceof LineNumberStmt && ((LineNumberStmt) s).line == line;
    }

    @Override
    public Stmt copy() {
        return new LineNumberStmt(line);
    }
}
