package dev.skidfuscator.jghost.merge;

import dev.skidfuscator.jghost.GhostMerger;
import dev.skidfuscator.jghost.tree.GhostClassNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;

public class ClassMerger implements GhostMerger<GhostClassNode> {
    @Override
    public GhostClassNode merge(GhostClassNode implementation, GhostClassNode other) {
        final ClassNode classNodeA = implementation.read();
        // TODO: Make hierarchy based superclass resolution
        // TODO: Merge extending interfaces

        /* Merge methods */
        final Set<String> methodHash = new HashSet<>();
        for (MethodNode method : classNodeA.methods) {
            methodHash.add(_hash(method));
        }

        final ClassNode classNodeB = other.read();
        for (MethodNode method : classNodeB.methods) {
            final String hash = _hash(method);

            // Already inside
            if (methodHash.contains(hash)) {
                continue;
            }

            // Add new method hash
            methodHash.add(hash);

            // Add new method to classNodeA
            classNodeA.methods.add(method);
        }

        /* Merge fields */
        final Set<String> fieldHash = new HashSet<>();
        for (FieldNode field : classNodeA.fields) {
            fieldHash.add(_hash(field));
        }

        for (FieldNode field : classNodeB.fields) {
            final String hash = _hash(field);

            // Already inside
            if (fieldHash.contains(hash)) {
                continue;
            }

            // Add new method hash
            fieldHash.add(hash);

            // Add new method to classNodeA
            classNodeA.fields.add(field);
        }



        // Serialize and return
        return GhostClassNode.of(classNodeA);
    }

    private String _hash(final MethodNode methodNode) {
        return methodNode.name + "#" + methodNode.desc;
    }

    private String _hash(final FieldNode methodNode) {
        return methodNode.name + "#" + methodNode.desc;
    }
}
