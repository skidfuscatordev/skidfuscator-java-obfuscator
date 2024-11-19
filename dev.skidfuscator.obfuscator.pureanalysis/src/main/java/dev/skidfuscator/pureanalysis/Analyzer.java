package dev.skidfuscator.pureanalysis;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class Analyzer {
    protected final PurityContext context;
    protected final PurityAnalyzer analyzer;
    protected final String name;

    protected Analyzer(String name, PurityContext context, PurityAnalyzer analyzer) {
        this.name = name;
        this.context = context;
        this.analyzer = analyzer;
    }

    public abstract PurityReport analyze(Context ctx);

    protected PurityReport pure() {
        return new PurityReport(true, name, null, null);
    }

    protected PurityReport impure(String reason, AbstractInsnNode insn) {
        return new PurityReport(false, name, reason, insn);
    }

    protected PurityReport impure(String reason) {
        return new PurityReport(false, name, reason, null);
    }

    public static class Context {
        MethodNode method;
        ClassNode classNode;

        public Context(MethodNode method, ClassNode classNode) {
            this.method = method;
            this.classNode = classNode;
        }

        public MethodNode method() {
            return method;
        }

        public ClassNode parent() {
            return classNode;
        }
    }
}