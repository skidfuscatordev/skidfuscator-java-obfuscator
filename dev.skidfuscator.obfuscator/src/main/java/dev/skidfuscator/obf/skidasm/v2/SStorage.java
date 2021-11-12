package dev.skidfuscator.obf.skidasm.v2;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.skidasm.SkidInvocation;
import dev.skidfuscator.obf.utils.ProgressUtil;
import dev.skidfuscator.obf.utils.RandomUtil;
import dev.skidfuscator.obf.utils.TimedLogger;
import dev.skidfuscator.obf.yggdrasil.method.hash.ClassMethodHash;
import me.tongfei.progressbar.ProgressBar;
import org.apache.log4j.LogManager;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SStorage {
    private final Map<MethodNode, SMethod> methodMap = new ConcurrentHashMap<>();
    private final Map<MethodNode, SMethodGroup> methodGroupMap = new ConcurrentHashMap<>();
    private final Map<ClassMethodHash, SMethodGroup> classMethodHashGroup = new ConcurrentHashMap<>();
    private final Map<InvocationExpr, SMethodGroup> invocationSMethodGroupMap = new HashMap<>();
    private final TimedLogger logger = new TimedLogger(LogManager.getLogger(this.getClass()));

    public void cache(final SkidSession session) {
        logger.log("[#] Beginning cache...");
        final List<ClassNode> nodes;

        try (ProgressBar progressBar = ProgressUtil.progress(session.getClassSource().size())){
            nodes = session.getClassSource().getClassTree().vertices().parallelStream()
                    .filter(e -> {
                        progressBar.step();
                        return session.getClassSource().isApplicationClass(e.getName());
                    })
                    .collect(Collectors.toList());
        }

        logger.log("[#]     > Cached over " + nodes.size() + " classes!");
        logger.post("[#] Establishing inheritance...");

        final ForkJoinPool customThreadPool = new ForkJoinPool(4);

        try (ProgressBar progressBar = ProgressUtil.progress(nodes.size())){
            final Consumer<ClassNode> executor = new Consumer<ClassNode>() {
                @Override
                public void accept(ClassNode node) {
                    node.getMethods().forEach(method -> {
                        getGroup(session, method);
                    });
                    progressBar.step();
                }
            };

            customThreadPool.submit(() -> {
                nodes.parallelStream().forEach(executor);
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            customThreadPool.shutdown();
        }
        logger.log("[#]     > Cached over " + methodGroupMap.size() + " method groups!");
        logger.post("[#] Establishing dynamic call evaluation...");
        try (ProgressBar invocationBar = ProgressUtil.progress(nodes.size())) {
            nodes.parallelStream().forEach(c -> {
                for (MethodNode method : c.getMethods()) {
                    final ControlFlowGraph cfg = session.getCxt().getIRCache().get(method);

                    if (cfg == null)
                        continue;

                    cfg.allExprStream()
                            .parallel()
                            .filter(e -> e instanceof InvocationExpr && !(e instanceof DynamicInvocationExpr))
                            .map(e -> (InvocationExpr) e)
                            .forEach(e -> {
                                final ClassMethodHash target = new ClassMethodHash(e.getName(), e.getDesc(), e.getOwner());
                                final SMethodGroup targetGroup = classMethodHashGroup.get(target);

                                if (targetGroup != null) {
                                    invocationSMethodGroupMap.put(e, targetGroup);
                                }
                            });
                }
                invocationBar.step();
            });
        }

    }

    public SMethodGroup getGroup(final SkidSession session, final MethodNode methodNode) {
        SMethodGroup group = methodGroupMap.get(methodNode);

        if (group == null) {
            final Set<MethodNode> h = session.getCxt().getInvocationResolver()
                    .getHierarchyMethodChain(methodNode.owner, methodNode.getName(), methodNode.getDesc(), true);

            final List<SMethod> methods = new ArrayList<>();
            for (MethodNode node : h) {
                final ControlFlowGraph cfg = session.getCxt().getIRCache().get(node);
                final List<SBlock> blocks = new ArrayList<>();

                if (cfg != null) {
                    for (BasicBlock vertex : cfg.vertices()) {
                        blocks.add(new SBlock(vertex, RandomUtil.nextInt()));
                    }
                }

                final SMethod method = new SMethod(node, blocks, cfg);
                methods.add(method);
            }

            group = new SMethodGroup(methods);

            for (SMethod method : methods) {
                method.setGroup(group);
                methodGroupMap.put(method.getParent(), group);
                methodMap.put(method.getParent(), method);

                final ClassMethodHash hash = new ClassMethodHash(
                        method.getParent().getName(),
                        method.getParent().getDesc(),
                        method.getParent().owner.getName()
                );

                classMethodHashGroup.put(hash, group);
            }
        }

        return group;
    }
}
