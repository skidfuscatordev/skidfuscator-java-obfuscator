package dev.skidfuscator.obf.skidasm.v2;

import dev.skidfuscator.obf.init.SkidSession;
import lombok.Data;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SClass implements Renderable {
    private final ClassNode parent;
    private final List<SClass> classes;
    private final List<SMethodGroup> methodGroups;

    public SClass(ClassNode parent, List<SClass> classes, List<SMethodGroup> methodGroups) {
        this.parent = parent;
        this.classes = classes;
        this.methodGroups = methodGroups;
    }

    @Override
    public void render(SkidSession session) {
        for (SMethodGroup methodGroup : methodGroups) {
            methodGroup.render(session);
        }
    }
    
    public static void create(final SkidSession session, final ClassNode node) {
        final List<ClassNode> heredity = new ArrayList<>(session.getClassSource().getClassTree().getAllBranches(node));
        final Map<MethodNode, List<MethodNode>> methodHeredity = new HashMap<>();

        for (MethodNode method : node.getMethods()) {
            final List<MethodNode> h = new ArrayList<>(session.getCxt().getInvocationResolver()
                    .getHierarchyMethodChain(node, method.getName(), method.getDesc(), true));
            
            methodHeredity.put(method, h);
        }
    }
    
    
}
