package dev.skidfuscator.obf.transform.impl.kappa;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import dev.skidfuscator.obf.transform.impl.ProjectPass;
import dev.skidfuscator.obf.transform.impl.flow.FlowPass;
import dev.skidfuscator.obf.utils.RandomUtil;
import lombok.SneakyThrows;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.algorithms.BoissinotDestructor;
import org.mapleir.ir.algorithms.LocalsReallocator;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.builder.GenerationPass;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.stmt.PopStmt;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import sun.misc.Unsafe;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SuperDuperAgentPass implements ProjectPass {

    public void kill() {
        final List<String> bad = Arrays.asList(
                "-javaagent",
                "-agentlib"
        );
        for (String inputArgument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            for (String s : bad) {
                if (inputArgument.contains(s))
                    return;
            }
        }
        Unsafe unsafe;

        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");

            unsafeField.setAccessible(true);

            unsafe = (Unsafe) unsafeField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return;
        }

        final byte[] EMPTY_CLASS_BYTES =
                {
                        -54, -2, -70, -66, 0, 0, 0, 49, 0, 5, 1, 0, 34, 115, 117, 110,
                        47, 105, 110, 115, 116, 114, 117, 109, 101, 110, 116, 47, 73,
                        110, 115, 116, 114, 117, 109, 101, 110, 116, 97, 116, 105, 111,
                        110, 73, 109, 112, 108, 7, 0, 1, 1, 0, 16, 106, 97, 118, 97, 47,
                        108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 7, 0, 3, 0, 1,
                        0, 2, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0
                };


        // this is for testing purposes to make sure it's actually loaded
        try {
            java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
            m.setAccessible(true);
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            Object test1 = m.invoke(cl, "sun.instrument.InstrumentationImpl");

            if (test1 != null) {
                //System.err.println("Warning! The instrumentation API is already pre-loaded!");
                return;
            }

            unsafe.defineClass("sun.instrument.InstrumentationImpl", EMPTY_CLASS_BYTES, 0, EMPTY_CLASS_BYTES.length, null, null);
            Class.forName("sun.instrument.InstrumentationImpl");
        } catch (Throwable e) {
        }
    }

    @SneakyThrows
    private MethodNode getMethodNode(final org.mapleir.asm.ClassNode extra) {
        final ClassReader classReader = new ClassReader(SuperDuperAgentPass.class.getResourceAsStream("/" + this.getClass().getName().replace(".", "/") + ".class"));
        final org.objectweb.asm.tree.ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        final Method method = SuperDuperAgentPass.class.getDeclaredMethod("kill");

        final org.objectweb.asm.tree.MethodNode methodNode = classNode.methods.stream()
                .filter(e -> e.name.equals(method.getName()))
                .filter(e -> e.desc.equals("()V"))
                .findFirst()
                .orElse(null);


        final org.objectweb.asm.tree.MethodNode copied =  new org.objectweb.asm.tree.MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_DEPRECATED | Opcodes.ACC_STATIC,
                "skid",
                "()V",
                "",
                null
        );

        copied.instructions.add(methodNode.instructions);
        final MethodNode methodNode1 = new MethodNode(
                copied,
                extra
        );

        extra.node.methods.add(copied);
        extra.getMethods().add(methodNode1);

        return methodNode1;
    }


    @Override
    public void pass(SkidSession session) {
        final List<MethodNode> methodNodeList = new ArrayList<>(session.getCxt().getApplicationContext().getEntryPoints());

        final List<MethodNode> randomSelect = methodNodeList.stream()
                .filter(e -> !e.owner.isEnum())
                .filter(e -> e.owner.node.outerClass == null && !e.owner.getName().contains("$"))
                .collect(Collectors.toList());
        final MethodNode random = randomSelect.get(RandomUtil.nextInt(randomSelect.size()));

        final MethodNode methodNode = getMethodNode(random.owner);
        final ControlFlowGraph vcfg = session.getCxt().getIRCache().getFor(methodNode);
        BoissinotDestructor.leaveSSA(vcfg);
        LocalsReallocator.realloc(vcfg);

        for (MethodNode node : methodNodeList) {
            if (node.isAbstract() || node.isNative())
                continue;

            final ControlFlowGraph cfg = session.getCxt().getIRCache().get(node);

            if (cfg == null) {
                continue;
            }

            final BasicBlock entry = cfg.getEntries().iterator().next();
            final StaticInvocationExpr staticInvocationExpr = new StaticInvocationExpr(
                    new Expr[0],
                    methodNode.owner.getName(),
                    methodNode.getName(),
                    methodNode.getDesc()
            );

            final PopStmt stmt = new PopStmt(staticInvocationExpr);

            entry.add(0, stmt);
        }
    }

    @Override
    public String getName() {
        return "Super Anti-Agent Pass";
    }
}
