package dev.skidfuscator.obf;

import com.google.common.collect.Streams;
import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform.impl.fixer.ExceptionFixerPass;
import dev.skidfuscator.obf.transform.impl.flow.*;
import dev.skidfuscator.obf.utils.TimedLogger;
import dev.skidfuscator.obf.yggdrasil.caller.CallerType;
import dev.skidfuscator.obf.transform.impl.flow.gen3.SeedFlowPass;
import dev.skidfuscator.obf.skidasm.SkidGraph;
import dev.skidfuscator.obf.seed.IntegerBasedSeed;
import dev.skidfuscator.obf.skidasm.SkidInvocation;
import dev.skidfuscator.obf.skidasm.SkidMethod;
import dev.skidfuscator.obf.utils.OpcodeUtil;
import org.apache.log4j.LogManager;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

import java.util.*;
import java.util.stream.Collectors;

public class SkidMethodRenderer {
    private final Set<MethodNode> methodNodes = new HashSet<>();
    private final Map<MethodNode, SkidMethod> skidMethodMap = new HashMap<>();
    private final TimedLogger logger = new TimedLogger(LogManager.getLogger(this.getClass()));

    public void render(final SkidSession skidSession) {
        logger.log("Beginning Skidfuscator 1.0.1...");

        final List<ClassNode> nodeList = Streams.stream(skidSession.getClassSource().iterate())
                .parallel()
                .filter(e -> skidSession.getClassSource().isApplicationClass(e.getName()))
                .collect(Collectors.toList());

        for (ClassNode classNode : nodeList) {
            methodNodes.addAll(classNode.getMethods());
        }
        logger.log("Finished initial load");
        logger.post("Beginning method load...");
        for (MethodNode methodNode : methodNodes) {
            final Set<MethodNode> hierarchy = skidSession.getCxt().getInvocationResolver()
                    .getHierarchyMethodChain(methodNode.owner, methodNode.getName(), methodNode.getDesc(), true);

            hierarchy.add(methodNode);

            if (hierarchy.isEmpty()) {
                System.out.println("/!\\ Method " + methodNode.getDisplayName() + " is empty!");
                continue;
            }

            SkidMethod method = null;

            for (MethodNode node : hierarchy) {
                if (skidMethodMap.containsKey(node)) {
                    method = skidMethodMap.get(node);
                    break;
                }
            }

            if (method == null) {
                final boolean contains = methodNodes.containsAll(hierarchy);
                final CallerType type;
                if (!contains || OpcodeUtil.isSynthetic(methodNode)) {
                    type = CallerType.LIBRARY;
                } else {
                    final boolean entry = hierarchy.stream().anyMatch(e -> skidSession.getEntryPoints().contains(e));

                    type = entry ? CallerType.ENTRY : CallerType.APPLICATION;
                }

                method = new SkidMethod(new HashSet<>(hierarchy), type, new HashSet<>());
            }

            skidMethodMap.put(methodNode, method);
        }

        logger.log("Finished loading " + skidMethodMap.size() + " methods");
        logger.post("Beginning method mapping...");
        for (MethodNode method : methodNodes) {
            final ControlFlowGraph cfg = skidSession.getCxt().getIRCache().getFor(method);
            cfg.allExprStream()
                    .filter(e -> e instanceof InvocationExpr)
                    .filter(e -> !(e instanceof DynamicInvocationExpr))
                    .map(e -> (InvocationExpr) e)
                    .forEach(codeUnit -> {
                        try {
                            final Set<MethodNode> targets = codeUnit.resolveTargets(skidSession.getCxt().getInvocationResolver());

                            final CallerType type = methodNodes.containsAll(targets)
                                    ? CallerType.APPLICATION
                                    : CallerType.LIBRARY;

                            final SkidMethod skidMethod = skidMethodMap.get(method);

                            if (skidMethod == null)
                                return;

                            for (MethodNode target : targets) {
                                if (skidMethodMap.containsKey(target)) {
                                    skidMethodMap.get(target).getInvocationModal().add(new SkidInvocation(skidMethod, codeUnit));
                                    break;
                                }
                            }
                        } catch (Throwable e) {

                        }

                    });
        }

        logger.log("Finished mapping " + skidMethodMap.size() + " methods");
        logger.post("[*] Gen3 bootstrapping... Beginning seeding...");
        final List<SkidMethod> skidMethods = skidMethodMap.values().stream().distinct().collect(Collectors.toList());

        /*skidMethods.forEach(e -> {
            System.out.println("(Repository) Added group of size " + e.getMethodNodes().size() + " of name " +  e.getModal().getName());
        });*/

        final Random random = new Random();

        for (SkidMethod skidMethod : skidMethods) {
            final int privateKey = random.nextInt(Integer.MAX_VALUE);
            final int publicKey = random.nextInt(Integer.MAX_VALUE);
            skidMethod.setSeed(new IntegerBasedSeed(skidMethod,
                    privateKey,
                    publicKey)
            );
        }

        logger.log("[*] Finished initial seed of " + skidMethods.size() + " methods");
        logger.post("[*] Gen3 Flow... Beginning obfuscation...");
        final FlowPass[] flowPasses = new FlowPass[] {
                new NumberMutatorPass(),
                new SwitchMutatorPass(),
                //new ConditionMutatorPass(),
                new FakeExceptionJumpFlowPass(),
                new FakeJumpFlowPass(),
                new SeedFlowPass(),
        };

        final FlowPass[] fixers = new FlowPass[] {
                new ExceptionFixerPass()
        };

        // Fix retarded exceptions

        skidMethods.forEach(e -> e.renderPrivate(skidSession));

        for (FlowPass flowPass : flowPasses) {
            skidMethods.forEach(e -> flowPass.pass(skidSession, e));
            logger.log("     [@G3#flow] Finished running "
                    + flowPass.getName()
                    + " [Changed: " + skidSession.popCount()
                    + "]");
        }

        skidMethods.forEach(e -> {
            for (SkidGraph methodNode : e.getMethodNodes()) {
                methodNode.postlinearize(skidSession.getCxt().getIRCache().get(methodNode.getNode()));
            }
        });

        skidMethods.forEach(e -> {
            for (FlowPass flowPass : fixers) {
                flowPass.pass(skidSession, e);
            }
        });

        skidMethods.forEach(e -> e.renderPublic(skidSession));
        logger.log("[*] Finished Gen3 flow obfuscation");

    }

}
