package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.creator.*;
import dev.skidfuscator.obfuscator.hierarchy.SkidHierarchy;
import dev.skidfuscator.obfuscator.hierarchy.matching.ClassMethodHash;
import dev.skidfuscator.obfuscator.skidasm.*;
import org.mapleir.app.service.ClassTree;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.MethodNode;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HierarchyInvocationScanTest {
    @SuppressWarnings("unchecked")
    @Test void scansEachPreservedCallerOnceAndSkipsUnrelatedBootstrapInstructions() throws Exception {
        var raw = new org.objectweb.asm.tree.ClassNode(); raw.name="Caller"; raw.superName="java/lang/Object";
        for (int i=0;i<2;i++) {
            var method = new org.objectweb.asm.tree.MethodNode(Opcodes.ACC_STATIC,"call"+i,"()V",null,null);
            method.instructions.add(new InvokeDynamicInsnNode("concat","()Ljava/lang/String;",
                    new Handle(Opcodes.H_INVOKESTATIC,"java/lang/invoke/StringConcatFactory","makeConcatWithConstants","()V",false)));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"Target","run","()V",false));
            raw.methods.add(method);
        }
        var caller = ClassHelper.create(raw);
        Skidfuscator owner = mock(Skidfuscator.class);
        SkidApplicationClassSource source = mock(SkidApplicationClassSource.class);
        ClassTree tree = mock(ClassTree.class);
        when(owner.getClassSource()).thenReturn(source); when(owner.getIrFactory()).thenReturn(mock(SkidCache.class));
        when(source.getClassTree()).thenReturn(tree); when(source.isApplicationClass("Caller")).thenReturn(true);
        when(tree.vertices()).thenReturn(Collections.singleton(caller));
        var hierarchy = new SkidHierarchy(owner);
        var group = mock(SkidGroup.class); Set<SkidInvocation> invocations = new LinkedHashSet<>();
        when(group.getInvokers()).thenReturn(invocations);
        var groups = SkidHierarchy.class.getDeclaredField("hashToGroupMap"); groups.setAccessible(true);
        ((Map<ClassMethodHash,SkidGroup>)groups.get(hierarchy)).put(new ClassMethodHash("run","()V","Target"),group);
        var scan = SkidHierarchy.class.getDeclaredMethod("setupInvoke"); scan.setAccessible(true); scan.invoke(hierarchy);
        assertEquals(2,invocations.size());
        Set<MethodNode> callers = new HashSet<>();
        invocations.forEach(invocation -> { assertTrue(invocation.isExempt()); callers.add(invocation.getOwner()); });
        assertEquals(new HashSet<>(caller.getMethods()),callers);
    }
}
