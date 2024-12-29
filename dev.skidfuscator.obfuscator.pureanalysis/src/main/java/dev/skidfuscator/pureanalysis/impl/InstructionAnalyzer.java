package dev.skidfuscator.pureanalysis.impl;

import dev.skidfuscator.pureanalysis.Analyzer;
import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.PurityContext;
import dev.skidfuscator.pureanalysis.PurityReport;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;

public abstract class InstructionAnalyzer extends Analyzer {
    public InstructionAnalyzer(String name, PurityContext context, PurityAnalyzer analyzer, Class<? extends AbstractInsnNode>... instructionType) {
        super(name, context, analyzer);
        this.instructionType = instructionType;
    }

    private final Class<? extends AbstractInsnNode>[] instructionType;

    @Override
    public PurityReport analyze(Analyzer.Context ctx) {
        PurityReport report = new PurityReport(true, "Method Analysis", null, null);

        for (AbstractInsnNode insn : ctx.method().instructions) {
            if (Arrays.stream(instructionType).noneMatch(type -> type.isAssignableFrom(insn.getClass()))) {
                continue;
            }

            PurityReport insnReport = this.analyze(new InstructionAnalyzer.Context(
                    insn, ctx.method(), ctx.parent()
            ));

            if (!insnReport.isPure()) {
                report.addNested(insnReport);
                return report;
            }
        }

        return report;
    }

    public abstract PurityReport analyze(Context ctx);

    public static class Context {
        final AbstractInsnNode insn;
        final MethodNode method;
        final ClassNode classNode;

        public Context(AbstractInsnNode insn, MethodNode method, ClassNode classNode) {
            this.insn = insn;
            this.method = method;
            this.classNode = classNode;
        }

        public AbstractInsnNode insn() {
            return insn;
        }

        public MethodNode method() {
            return method;
        }

        public ClassNode parent() {
            return classNode;
        }
    }
}