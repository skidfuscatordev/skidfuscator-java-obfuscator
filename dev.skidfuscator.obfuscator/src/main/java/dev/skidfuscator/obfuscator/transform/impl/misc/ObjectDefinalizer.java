package dev.skidfuscator.obfuscator.transform.impl.misc;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.transform.method.RunMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * The concept behind this transformer is simply the issue with
 * final fields not being set in the clinit if they are primitive types (obviously)
 * so what we do, is we remove the value and set it in the clinit
 *
 * @author ryan
 */
public class ObjectDefinalizer extends AbstractTransformer {
    private static final Set<Type> TYPES = new HashSet<>(Arrays.asList(
            Type.INT_TYPE,
            Type.SHORT_TYPE,
            Type.BYTE_TYPE,
            Type.CHAR_TYPE
    ));

    public ObjectDefinalizer(Skidfuscator skidfuscator) {
        this(skidfuscator, Collections.emptyList());
    }

    public ObjectDefinalizer(Skidfuscator skidfuscator, List<Transformer> children) {
        super(skidfuscator,"Object Definalizer", children);
    }

    @Listen
    void handle(final RunMethodTransformEvent event) {
        final SkidMethodNode methodNode = event.getMethodNode();

        if (methodNode.isAbstract() || methodNode.isInit())
            return;

        if (methodNode.node.instructions.size() > 10000)
            return;

        final ControlFlowGraph cfg = methodNode.getCfg();

        if (cfg == null)
            return;

        final MethodNode clinit = methodNode.getParent().getClinitNode();

        final InsnList insnList = new InsnList();
        // TODO: Don't forget if you use ASM, compute the CFG again with
        //       MethodNode#recomputeCfg
    }
}