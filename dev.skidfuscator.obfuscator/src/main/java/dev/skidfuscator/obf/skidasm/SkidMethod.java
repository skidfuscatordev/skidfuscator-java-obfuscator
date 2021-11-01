package dev.skidfuscator.obf.skidasm;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.yggdrasil.caller.CallerType;
import dev.skidfuscator.obf.seed.Seed;
import dev.skidfuscator.obf.utils.Parameter;
import dev.skidfuscator.obf.utils.OpcodeUtil;
import lombok.Data;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class SkidMethod {
    private final List<SkidGraph> methodNodes;
    private final CallerType callerType;
    private final Set<SkidInvocation> invocationModal;
    private Seed<Integer> seed;

    private MethodNode modal;
    private boolean classStatic;
    private Parameter parameter;
    private UUID uuid;

    public SkidMethod(Set<MethodNode> methodNodes, CallerType callerType, Set<SkidInvocation> invocationModal) {
        this.methodNodes = methodNodes.stream().map(e -> new SkidGraph(e, this)).collect(Collectors.toList());
        this.callerType = callerType;
        this.invocationModal = invocationModal;
        this.modal = this.methodNodes.get(0).getNode();
        this.parameter = new Parameter(modal.getDesc());
        this.classStatic = OpcodeUtil.isStatic(modal);
        this.uuid = UUID.randomUUID();
    }

    public void renderPrivate(final SkidSession skidSession) {
        for (SkidGraph methodNode : methodNodes) {
            final ControlFlowGraph cfg = skidSession.getCxt().getIRCache().get(methodNode.getNode());
            if (cfg == null)
                continue;

            seed.renderPrivate(methodNode.getNode(), cfg);

            if (methodNode.getNode().isAbstract())
                continue;
            methodNode.render(cfg);
        }
    }

    public void renderPublic(final SkidSession skidSession) {
        seed.renderPublic(methodNodes);

        for (SkidGraph methodNode : methodNodes) {
            methodNode.getNode().node.desc = parameter.getDesc();
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
