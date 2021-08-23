package dev.skidfuscator.obf.transform.yggdrasil;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform.caller.CallerType;
import dev.skidfuscator.obf.transform.context.InvocationModal;
import dev.skidfuscator.obf.transform.seed.Seed;
import dev.skidfuscator.obf.transform_legacy.parameter.Parameter;
import dev.skidfuscator.obf.utils.OpcodeUtil;
import lombok.Data;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class SkidMethod {
    private final List<MethodNode> methodNodes;
    private final CallerType callerType;
    private final Set<SkidInvocation> invocationModal;
    private Seed<?> seed;

    private MethodNode modal;
    private boolean classStatic;
    private Parameter parameter;
    private UUID uuid;

    public SkidMethod(Set<MethodNode> methodNodes, CallerType callerType, Set<SkidInvocation> invocationModal) {
        this.methodNodes = new ArrayList<>(methodNodes);
        this.callerType = callerType;
        this.invocationModal = invocationModal;
        this.modal = this.methodNodes.get(0);
        this.parameter = new Parameter(modal.getDesc());
        this.classStatic = OpcodeUtil.isStatic(modal);
        this.uuid = UUID.randomUUID();
    }

    public void renderPrivate(final SkidSession skidSession) {
        for (MethodNode methodNode : methodNodes) {
            final ControlFlowGraph cfg = skidSession.getCxt().getIRCache().get(methodNode);
            if (cfg == null)
                continue;

            seed.renderPrivate(methodNode, cfg);
        }
    }

    public void renderPublic(final SkidSession skidSession) {
        seed.renderPublic(methodNodes);

        for (MethodNode methodNode : methodNodes) {
            methodNode.node.desc = parameter.getDesc();
        }
    }

    public boolean isStatic() {
        return classStatic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SkidMethod that = (SkidMethod) o;

        return uuid != null ? uuid.equals(that.uuid) : that.uuid == null;
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }
}
