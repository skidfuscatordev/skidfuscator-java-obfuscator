package dev.skidfuscator.jghost.merge;

import dev.skidfuscator.jghost.GhostMerger;
import dev.skidfuscator.jghost.tree.GhostClassNode;
import dev.skidfuscator.jghost.tree.GhostContents;
import dev.skidfuscator.jghost.tree.GhostLibrary;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LibraryMerger implements GhostMerger<GhostLibrary> {
    private final ClassMerger classMerger = new ClassMerger();

    @Override
    public GhostLibrary merge(GhostLibrary implementation, GhostLibrary other) {
        final GhostContents contents = implementation.getContents();
        final Map<String, GhostClassNode> classes = contents.getClasses();

        other.getContents().getClasses().forEach((name, data) -> {
            GhostClassNode classNode = classes.get(name);

            if (classNode != null) {
                classNode = classMerger.merge(classNode, data);
            } else {
                classNode = data;
            }

            classes.put(name, classNode);
        });

        return implementation;
    }
}
