package dev.skidfuscator.obfuscator.skidasm.cfg;

import ch.qos.logback.core.rolling.helper.FileStoreUtil;
import ch.qos.logback.core.util.FileUtil;
import com.google.common.collect.Streams;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.util.IOUtil;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.obfuscator.util.cfg.Blocks;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.CodeUnit;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.Invocation;
import org.mapleir.ir.code.expr.invoke.Invokable;
import org.mapleir.ir.locals.Local;
import org.mapleir.ir.locals.LocalsPool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SkidControlFlowGraph extends ControlFlowGraph {
    private int localTicker;
    private transient Deque<SkidBlock> fuckups = new LinkedList<>();

    public SkidControlFlowGraph(LocalsPool locals, MethodNode methodNode) {
        super(locals, methodNode);

        this.localTicker = locals.getMaxLocals() + 3;
    }

    public SkidControlFlowGraph(ControlFlowGraph cfg) {
        super(cfg);
    }

    public Stream<Invocation> staticInvocationStream() {
        return allExprStream()
                .filter(e -> e instanceof Invokable && !(e instanceof DynamicInvocationExpr))
                .map(e -> (Invocation) e);
    }

    public Stream<DynamicInvocationExpr> dynamicInvocationStream() {
        return allExprStream()
                .filter(e -> e instanceof DynamicInvocationExpr)
                .map(e -> (DynamicInvocationExpr) e);
    }

    public Set<SkidBlock> blocks() {
        return vertices()
                .stream()
                .filter(SkidBlock.class::isInstance)
                .map(SkidBlock.class::cast)
                .collect(Collectors.toSet());
    }

    @Override
    public void verify() {
        try {
            super.verify();
        } catch (Throwable e) {
            if (Skidfuscator.CLOUD) {
                e.printStackTrace();
                Skidfuscator.LOGGER.error("-----------------------------------------------------\n"
                        + "/!\\ Skidfuscator failed to verify an obfuscated method!\n"
                        + "Please use the following debug information and send it to Ghast#0001\n"
                        + "\n"
                        + this
                        + "\n"
                , e);
                return;
            }
            final File output = new File("skidfuscator-error-" + RandomUtil.randomAlphabeticalString(3) + ".txt");
            try {
                Files.write(output.toPath(), this.toString().getBytes(StandardCharsets.UTF_8));
                Skidfuscator.LOGGER.warn( "-----------------------------------------------------\n"
                        + "/!\\ Skidfuscator failed to verify an obfuscated method!\n"
                        + "Please use the following debug information and send it to Ghast#0001\n"
                        + "\n"
                        + "File " + output.getAbsolutePath()
                        + "\n"
                );
            } catch (IOException ex) {
                Skidfuscator.LOGGER.warn( "-----------------------------------------------------\n"
                        + "/!\\ Skidfuscator failed to verify an obfuscated method!\n"
                        + "Please use the following debug information and send it to Ghast#0001\n"
                        + "\n"
                        + this
                        + "\n"
                );
            }
            throw new IllegalStateException(
                    "Failed to verify "
                        + getMethodNode().owner + "#"  + getMethodNode().getName()
                        + getMethodNode().getDesc() + " because of: " +  e.getMessage(),
                    e
            );
        }
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

    public Local getSelfLocal() {
        assert !getMethodNode().isStatic() : "Trying to get instance local on static method";

        return getLocals().get(0);
    }

    public BasicBlock getEntry() {
        return getEntries().iterator().next();
    }

    public SkidBlock getFuckup() {
        if (fuckups.isEmpty()) {
            addFuckup();
        }

        final SkidBlock fuckup = fuckups.removeFirst();
        fuckups.add(fuckup);

        return fuckup;
    }

    public SkidBlock addFuckup() {
        return this.addFuckup(null);
    }

    public SkidBlock addFuckup(final String notice) {
        final SkidBlock block = notice == null
                ? Blocks.exception(this)
                : Blocks.exception(this, notice);
        this.fuckups.add(block);

        return block;
    }
}
