package dev.skidfuscator.obf.transform.context;

import com.google.common.collect.Streams;
import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform.caller.CallerType;
import dev.skidfuscator.obf.transform.flow.FakeJumpFlowPass;
import dev.skidfuscator.obf.transform.flow.FlowPass;
import dev.skidfuscator.obf.transform.flow.gen3.SeedFlowPass;
import dev.skidfuscator.obf.transform.flow.gen3.SkidGraph;
import dev.skidfuscator.obf.transform.seed.IntegerBasedSeed;
import dev.skidfuscator.obf.transform.yggdrasil.SkidInvocation;
import dev.skidfuscator.obf.transform.yggdrasil.SkidMethod;
import dev.skidfuscator.obf.utils.OpcodeUtil;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

import java.util.*;
import java.util.stream.Collectors;

public class MethodRepository {
    private final Set<MethodNode> methodNodes = new HashSet<>();
    private final Map<InvocationExpr, InvocationModal> reader = new HashMap<>();
    private final Map<MethodNode, SkidMethod> skidMethodMap = new HashMap<>();

    public void render(final SkidSession skidSession) {
        final List<ClassNode> nodeList = Streams.stream(skidSession.getClassSource().iterate())
                .parallel()
                .filter(e -> skidSession.getClassSource().isApplicationClass(e.getName()))
                .collect(Collectors.toList());

        for (ClassNode classNode : nodeList) {
            methodNodes.addAll(classNode.getMethods());
        }

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

                            final InvocationModal modal = new InvocationModal(type, codeUnit, targets);
                            reader.put(codeUnit, modal);

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

        final List<SkidMethod> skidMethods = skidMethodMap.values().stream().distinct().collect(Collectors.toList());

        skidMethods.forEach(e -> {
            System.out.println("(Repository) Added group of size " + e.getMethodNodes().size() + " of name " +  e.getModal().getName());
        });

        final Random random = new Random();

        for (SkidMethod skidMethod : skidMethods) {
            final int privateKey = random.nextInt(Integer.MAX_VALUE);
            final int publicKey = random.nextInt(Integer.MAX_VALUE);
            skidMethod.setSeed(new IntegerBasedSeed(skidMethod,
                    privateKey,
                    publicKey)
            );
        }

        final FlowPass[] flowPasses = new FlowPass[] {
                new FakeJumpFlowPass(),
                new SeedFlowPass()
        };

        skidMethods.forEach(e -> e.renderPrivate(skidSession));
        skidMethods.forEach(e -> e.renderPublic(skidSession));
        skidMethods.forEach(e -> {
            for (FlowPass flowPass : flowPasses) {
                flowPass.pass(skidSession, e);
            }
        });

        skidMethods.forEach(e -> {
            for (SkidGraph methodNode : e.getMethodNodes()) {
                methodNode.postlinearize(skidSession.getCxt().getIRCache().get(methodNode.getNode()));
            }
        });
    }
}
