package dev.skidfuscator.obf.transform.impl.fixer;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import dev.skidfuscator.obf.transform.impl.flow.FlowPass;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.ir.TypeUtils;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.util.*;

public class ExceptionFixerPass implements FlowPass {
    @Override
    public void pass(SkidSession session, SkidMethod method) {
        for (SkidGraph methodNode : method.getMethodNodes()) {
            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(methodNode.getNode());

            if (cfg == null)
                continue;

            for (ExceptionRange<BasicBlock> range : cfg.getRanges()) {
                if (range.getTypes().size() <= 1)
                    continue;

                ClassNode superType = null;
                for (Type type : range.getTypes()) {
                    ClassNode classNode = session.getCxt()
                            .getApplication()
                            .findClassNode(type.getClassName());

                    if (classNode == null) {
                        try {
                            classNode = ClassHelper.create(type.getClassName());
                        } catch (IOException e) {
                            continue;
                        }
                    }

                    if (superType == null) {
                        superType = classNode;
                    } else {
                        superType = session.getCxt()
                                .getApplication()
                                .getClassTree()
                                .getCommonSuperType(superType.getName(), classNode.getName());
                    }
                }

                final Set<Type> types = new HashSet<>(Collections.singleton(
                        superType == null
                                ? TypeUtils.OBJECT_TYPE
                                : Type.getObjectType(superType.getName())
                ));
                range.setTypes(types);

                session.count();
            }
        }
    }

    @Override
    public String getName() {
        return "Exception Fixer";
    }
}
