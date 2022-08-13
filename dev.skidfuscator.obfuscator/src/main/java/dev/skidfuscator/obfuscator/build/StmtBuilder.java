package dev.skidfuscator.obfuscator.build;

import org.mapleir.ir.cfg.SSAFactory;
import org.mapleir.ir.code.Stmt;

public class StmtBuilder {
    private final SSAFactory factory;
    private final Stmt stmt;

    public StmtBuilder(SSAFactory factory, Stmt stmt) {
        this.factory = factory;
        this.stmt = stmt;
    }

    public Stmt build() {
        return stmt;
    }
}
