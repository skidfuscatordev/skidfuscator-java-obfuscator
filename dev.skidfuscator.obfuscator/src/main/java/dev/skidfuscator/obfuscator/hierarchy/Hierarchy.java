package dev.skidfuscator.obfuscator.hierarchy;

import dev.skidfuscator.obfuscator.hierarchy.matching.ClassMethodHash;
import dev.skidfuscator.obfuscator.skidasm.*;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

import java.util.List;
import java.util.Set;

public interface Hierarchy {
    void cache();

    SkidGroup getGroup(final ClassMethodHash methodNode);

    SkidGroup getGroup(final MethodNode methodNode);

    List<ClassNode> getClasses();

    List<SkidGroup> getGroups();

    List<SkidMethodNode> getMethods();

    /**
     * @param classNode Annotation class of which you wish to get all references to
     * @return List of all annotations using the Class mentioned as a parameter
     */
    List<SkidAnnotation> getAnnotations(final ClassNode classNode);

    Set<SkidInvocation> getInvokers(final MethodNode methodNode);

    void recacheInvokes();

    SkidGroup cache(final SkidMethodNode methodNode);

    void cache(final SkidClassNode classNode);
}
