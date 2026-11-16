package dev.skidfuscator.pureanalysis;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;

public class PurityReport {
    public enum Outcome { PURE, IMPURE, UNSUPPORTED }
    private final boolean pure;
    private final Outcome outcome;
    private final String condition;
    private final String reason;
    private final AbstractInsnNode failedInsn;
    private final List<PurityReport> nested = new ArrayList<>();

    public PurityReport(boolean pure, String condition, String reason, AbstractInsnNode failedInsn) {
        this(pure?Outcome.PURE:Outcome.IMPURE,condition,reason,failedInsn);
    }

    private PurityReport(Outcome outcome,String condition,String reason,AbstractInsnNode failedInsn) {
        this.outcome=outcome;
        this.pure=outcome==Outcome.PURE;
        this.condition = condition;
        this.reason = reason;
        this.failedInsn = failedInsn;
    }

    public void addNested(PurityReport report) {
        nested.add(report);
    }

    public static PurityReport unsupported(String condition,String reason) {
        return new PurityReport(Outcome.UNSUPPORTED,condition,reason,null);
    }
    public Outcome getOutcome() {
        if (outcome==Outcome.IMPURE || nested.stream().anyMatch(r->r.getOutcome()==Outcome.IMPURE)) return Outcome.IMPURE;
        if (outcome==Outcome.UNSUPPORTED || nested.stream().anyMatch(r->r.getOutcome()==Outcome.UNSUPPORTED)) return Outcome.UNSUPPORTED;
        return Outcome.PURE;
    }
    public boolean isPure() { return getOutcome()==Outcome.PURE; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb, 0);
        return sb.toString();
    }

    private void toString(StringBuilder sb, int depth) {
        String indent = "  ".repeat(depth);
        sb.append(indent).append(condition).append(": ").append(getOutcome().name());
        if (!pure && reason != null) {
            sb.append("\n").append(indent).append("Reason: ").append(reason);
            if (failedInsn != null) {
                sb.append("\n").append(indent).append("At instruction: ").append(formatInstruction(failedInsn));
            }
        }
        for (PurityReport nested : this.nested) {
            sb.append("\n");
            nested.toString(sb, depth + 1);
        }
    }

    private String formatInstruction(AbstractInsnNode insn) {
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode min = (MethodInsnNode) insn;
            return String.format("%s.%s%s", min.owner, min.name, min.desc);
        }
        if (insn instanceof FieldInsnNode) {
            FieldInsnNode fin = (FieldInsnNode) insn;
            return String.format("%s.%s:%s", fin.owner, fin.name, fin.desc);
        }
        return insn.toString();
    }
}