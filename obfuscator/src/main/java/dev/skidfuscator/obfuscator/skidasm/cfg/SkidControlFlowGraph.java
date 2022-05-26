package dev.skidfuscator.obfuscator.skidasm.cfg;

import com.google.common.collect.Streams;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.locals.LocalsPool;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SkidControlFlowGraph extends ControlFlowGraph {
    public SkidControlFlowGraph(LocalsPool locals, MethodNode methodNode) {
        super(locals, methodNode);
    }

    public SkidControlFlowGraph(ControlFlowGraph cfg) {
        super(cfg);
    }

    @Override
    public Stream<CodeUnit> allExprStream() {
        return vertices()
                .stream()
                .filter(e -> !e.isFlagSet(SkidBlock.FLAG_NO_OPAQUE))
                .flatMap(Collection::stream)
                .map(Stmt::enumerateWithSelf)
                .flatMap(Streams::stream);
    }
}
