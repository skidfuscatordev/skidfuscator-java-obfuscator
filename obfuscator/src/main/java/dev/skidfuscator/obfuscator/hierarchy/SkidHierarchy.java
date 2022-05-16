package dev.skidfuscator.obfuscator.hierarchy;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.hierarchy.matching.ClassMethodHash;
import dev.skidfuscator.obfuscator.skidasm.*;
import dev.skidfuscator.obfuscator.util.ProgressUtil;
import me.tongfei.progressbar.ProgressBar;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.invoke.*;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SkidHierarchy implements Hierarchy {
    private final Map<MethodNode, SkidGroup> methodToGroupMap = new ConcurrentHashMap<>();
    private final Map<ClassMethodHash, SkidGroup> hashToGroupMap = new ConcurrentHashMap<>();
    private final Map<ClassMethodHash, MethodNode> hashToMethodMap = new ConcurrentHashMap<>();
    private final Map<Invocation, SkidGroup> invocationToGroupMap = new ConcurrentHashMap<>();
    private final Map<Invocation, MethodNode> invocationToMethodMap = new ConcurrentHashMap<>();
    private final Map<MethodNode, List<SkidInvocation>> methodToInvocationsMap = new ConcurrentHashMap<>();

    private final Skidfuscator skidfuscator;

    private List<SkidGroup> groups;
    private List<SkidMethodNode> methods;
    private List<ClassNode> nodes;
    private Map<ClassNode, List<SkidAnnotation>> annotations;
    private final Consumer<ClassNode> executor = new Consumer<ClassNode>() {
        @Override
        public void accept(ClassNode node) {
            if (node.node.visibleAnnotations != null) {
                for (AnnotationNode annotation : node.node.visibleAnnotations) {
                    getAnnotation(annotation, SkidAnnotation.AnnotationType.VISIBLE);
                }
            }

            if (node.node.invisibleAnnotations != null) {
                for (AnnotationNode annotation : node.node.invisibleAnnotations) {
                    getAnnotation(annotation, SkidAnnotation.AnnotationType.INVISIBLE);
                }
            }

            if (node.node.visibleTypeAnnotations != null) {
                for (AnnotationNode annotation : node.node.visibleTypeAnnotations) {
                    getAnnotation(annotation, SkidAnnotation.AnnotationType.TYPE_VISIBLE);
                }
            }
            if (node.node.invisibleTypeAnnotations != null) {
                for (AnnotationNode annotation : node.node.invisibleTypeAnnotations) {
                    getAnnotation(annotation, SkidAnnotation.AnnotationType.TYPE_INVISIBLE);
                }
            }

            node.getMethods().forEach(method -> {
                getGroup(skidfuscator, method);

                if (method.node.visibleAnnotations != null) {
                    for (AnnotationNode annotation : method.node.visibleAnnotations) {
                        getAnnotation(annotation, SkidAnnotation.AnnotationType.VISIBLE);

                    }
                }

                if (method.node.invisibleAnnotations != null) {
                    for (AnnotationNode annotation : method.node.invisibleAnnotations) {
                        getAnnotation(annotation, SkidAnnotation.AnnotationType.INVISIBLE);
                    }
                }

                if (method.node.visibleTypeAnnotations != null) {
                    for (AnnotationNode annotation : method.node.visibleTypeAnnotations) {
                        getAnnotation(annotation, SkidAnnotation.AnnotationType.TYPE_VISIBLE);

                    }
                }
                if (method.node.invisibleTypeAnnotations != null) {
                    for (AnnotationNode annotation : method.node.invisibleTypeAnnotations) {
                        getAnnotation(annotation, SkidAnnotation.AnnotationType.TYPE_INVISIBLE);

                    }
                }
            });

            node.getFields().forEach(field -> {
                if (field.node.visibleAnnotations != null) {
                    for (AnnotationNode annotation : field.node.visibleAnnotations) {
                        getAnnotation(annotation, SkidAnnotation.AnnotationType.VISIBLE);

                    }
                }

                if (field.node.invisibleAnnotations != null) {
                    for (AnnotationNode annotation : field.node.invisibleAnnotations) {
                        getAnnotation(annotation, SkidAnnotation.AnnotationType.INVISIBLE);

                    }
                }

                if (field.node.visibleTypeAnnotations != null) {
                    for (AnnotationNode annotation : field.node.visibleTypeAnnotations) {
                        getAnnotation(annotation, SkidAnnotation.AnnotationType.TYPE_VISIBLE);

                    }
                }
                if (field.node.invisibleTypeAnnotations != null) {
                    for (AnnotationNode annotation : field.node.invisibleTypeAnnotations) {
                        getAnnotation(annotation, SkidAnnotation.AnnotationType.TYPE_INVISIBLE);
                    }
                }
            });

        }
    };

    public SkidHierarchy(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
    }

    public void cache() {
        Skidfuscator.LOGGER.log("[#] Beginning cache...");
        this.methods = new ArrayList<>();
        this.groups = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.annotations = new HashMap<>();

        try (ProgressBar progressBar = ProgressUtil.progress(skidfuscator.getClassSource().size())){
            nodes = skidfuscator.getClassSource().getClassTree().vertices().parallelStream()
                    .filter(e -> {
                        progressBar.step();
                        return skidfuscator.getClassSource().isApplicationClass(e.getName());
                    })
                    .collect(Collectors.toList());
        }

        Skidfuscator.LOGGER.log("[#]     > Cached over " + nodes.size() + " classes!");
        Skidfuscator.LOGGER.post("[#] Establishing inheritance...");

        try (ProgressBar progressBar = ProgressUtil.progress(nodes.size())){
            nodes.forEach(e -> {
                executor.accept(e);
                progressBar.step();
            });
        }
        Skidfuscator.LOGGER.log("[#]     > Cached over " + methodToGroupMap.size() + " method groups!");
        Skidfuscator.LOGGER.post("[#] Establishing dynamic call evaluation...");
        this.setupInvoke();
        Skidfuscator.LOGGER.log("[#] Logged over " + invocationToGroupMap.size() + " invocations!");
    }

    @Override
    public void recacheInvokes() {
        this.invocationToGroupMap.clear();
        this.invocationToMethodMap.clear();
        this.methodToInvocationsMap.clear();

        this.setupInvoke();
    }

    private void setupInvoke() {
        try (ProgressBar invocationBar = ProgressUtil.progress(nodes.size())) {
            nodes.forEach(c -> {
                for (MethodNode method : c.getMethods()) {
                    final ControlFlowGraph cfg = skidfuscator.getCxt().getIRCache().get(method);

                    if (cfg == null)
                        continue;

                    cfg.allExprStream()
                            .parallel()
                            .filter(e -> e instanceof Invokable && !(e instanceof DynamicInvocationExpr))
                            .map(e -> (Invocation) e)
                            .forEach(invocation -> {
                                final ClassMethodHash target;

                                if (invocation instanceof InvocationExpr) {
                                    final InvocationExpr e = (InvocationExpr) invocation;
                                    target = new ClassMethodHash(e.getName(), e.getDesc(), e.getOwner());
                                } else if (invocation instanceof InitialisedObjectExpr) {
                                    final InitialisedObjectExpr e = (InitialisedObjectExpr) invocation;
                                    target = new ClassMethodHash(e.getName(), e.getDesc(), e.getOwner());
                                } else {
                                    return;
                                }

                                final SkidInvocation skidInvocation = new SkidInvocation(
                                        method,
                                        invocation
                                );

                                final SkidGroup targetGroup = hashToGroupMap.get(target);
                                if (targetGroup != null) {
                                    invocationToGroupMap.put(invocation, targetGroup);

                                    targetGroup.getInvokers().add(skidInvocation);
                                }

                                final MethodNode targetMethod = hashToMethodMap.get(target);
                                if (targetMethod != null) {
                                    invocationToMethodMap.put(invocation, targetMethod);

                                    if (targetMethod instanceof SkidMethodNode) {
                                        ((SkidMethodNode) targetMethod).addInvocation(skidInvocation);
                                    }

                                    methodToInvocationsMap.computeIfAbsent(targetMethod, e -> {
                                        return new ArrayList<>(Collections.singleton(skidInvocation));
                                    });
                                }



                            });
                }
                invocationBar.step();
            });
        }
    }

    private SkidGroup getGroup(final Skidfuscator session, final MethodNode methodNode) {
        SkidGroup group = methodToGroupMap.get(methodNode);

        if (group == null) {
            final Set<MethodNode> h = session.getCxt()
                    .getInvocationResolver()
                    .getHierarchyMethodChain(methodNode.owner, methodNode.getName(), methodNode.getDesc(), true);
            h.add(methodNode);

            final List<MethodNode> methods = new ArrayList<>(h);

            group = new SkidGroup(methods, skidfuscator);
            group.setAnnotation(((SkidClassNode) methodNode.owner).isAnnotation());
            group.setStatical(((SkidMethodNode) methodNode).isStatic());
            group.setName(methodNode.getName());
            group.setDesc(methodNode.getDesc());

            for (MethodNode method : methods) {
                if (method instanceof SkidMethodNode) {
                    this.methods.add((SkidMethodNode) method);
                    ((SkidMethodNode) method).setGroup(group);
                }
                methodToGroupMap.put(method, group);

                final ClassMethodHash hash = new ClassMethodHash(
                        method.getName(),
                        method.getDesc(),
                        method.owner.getName()
                );

                hashToGroupMap.put(hash, group);
                hashToMethodMap.put(hash, method);
            }

            groups.add(group);
        }

        return group;
    }

    private void getAnnotation(final AnnotationNode node, final SkidAnnotation.AnnotationType type) {
        final String filteredNamePre = node.desc.substring(1);
        final String filteredNamePost = filteredNamePre.substring(0, filteredNamePre.length() - 1);
        final ClassNode parent = skidfuscator
                .getClassSource()
                .findClassNode(filteredNamePost);
        List<SkidAnnotation> nodes = annotations.computeIfAbsent(parent, k -> new ArrayList<>());
        try {
            nodes.add(new SkidAnnotation(
                    node,
                    type,
                    skidfuscator,
                    parent
            ));
        } catch (Exception e) {
            // Just skip for now
        }
    }

    @Override
    public List<SkidGroup> getGroups() {
        return groups;
    }

    @Override
    public List<ClassNode> getClasses() {
        return nodes;
    }

    @Override
    public List<SkidMethodNode> getMethods() {
        return methods;
    }

    @Override
    public List<SkidAnnotation> getAnnotations(final ClassNode classNode) {
        return annotations.get(classNode);
    }

    @Override
    public List<SkidInvocation> getInvokers(MethodNode methodNode) {
        return methodToInvocationsMap.get(methodNode);
    }

    @Override
    public SkidGroup cache(SkidMethodNode methodNode) {
        final SkidGroup group = getGroup(skidfuscator, methodNode);
        final ControlFlowGraph cfg = skidfuscator.getCxt().getIRCache().get(methodNode);

        if (cfg == null)
            return group;

        cfg.allExprStream()
                .parallel()
                .filter(e -> e instanceof Invokable && !(e instanceof DynamicInvocationExpr))
                .map(e -> (Invocation) e)
                .forEach(invocation -> {
                    final ClassMethodHash target;

                    if (invocation instanceof InvocationExpr) {
                        final InvocationExpr e = (InvocationExpr) invocation;
                        target = new ClassMethodHash(e.getName(), e.getDesc(), e.getOwner());
                    } else if (invocation instanceof InitialisedObjectExpr) {
                        final InitialisedObjectExpr e = (InitialisedObjectExpr) invocation;
                        target = new ClassMethodHash(e.getName(), e.getDesc(), e.getOwner());
                    } else {
                        return;
                    }

                    final SkidInvocation skidInvocation = new SkidInvocation(
                            methodNode,
                            invocation
                    );

                    final SkidGroup targetGroup = hashToGroupMap.get(target);
                    if (targetGroup != null) {
                        invocationToGroupMap.put(invocation, targetGroup);

                        targetGroup.getInvokers().add(skidInvocation);
                    }

                    final MethodNode targetMethod = hashToMethodMap.get(target);
                    if (targetMethod != null) {
                        invocationToMethodMap.put(invocation, targetMethod);

                        if (targetMethod instanceof SkidMethodNode) {
                            ((SkidMethodNode) targetMethod).addInvocation(skidInvocation);
                        }

                        methodToInvocationsMap.computeIfAbsent(targetMethod, e -> new ArrayList<>(Collections.singleton(skidInvocation)));
                    }

                });
        return group;
    }


    @Override
    public void cache(SkidClassNode classNode) {
        executor.accept(classNode);
    }

    @Override
    public SkidGroup getGroup(SkidMethodNode methodNode) {
        return getGroup(skidfuscator, methodNode);
    }
}
