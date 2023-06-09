package dev.skidfuscator.obfuscator.hierarchy;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.hierarchy.matching.ClassMethodHash;
import dev.skidfuscator.obfuscator.skidasm.*;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.util.ProgressUtil;
import dev.skidfuscator.obfuscator.util.misc.Parameter;
import dev.skidfuscator.obfuscator.util.progress.ProgressWrapper;
import lukfor.progress.Collection;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.code.expr.invoke.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

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
                for (AnnotationNode annotation : new ArrayList<>(node.node.visibleAnnotations)) {
                    if (checkExclude(annotation)) {
                        skidfuscator.getExemptAnalysis().add(node);
                        node.node.visibleAnnotations.remove(annotation);
                        continue;
                    }
                    getAnnotation(annotation, SkidAnnotation.AnnotationType.VISIBLE);
                }
            }

            if (node.node.invisibleAnnotations != null) {
                for (AnnotationNode annotation : new ArrayList<>(node.node.invisibleAnnotations)) {
                    if (checkExclude(annotation)) {
                        skidfuscator.getExemptAnalysis().add(node);
                        node.node.invisibleAnnotations.remove(annotation);
                        continue;
                    }
                    getAnnotation(annotation, SkidAnnotation.AnnotationType.INVISIBLE);
                }
            }

            if (node.node.visibleTypeAnnotations != null) {
                for (AnnotationNode annotation : new ArrayList<>(node.node.visibleTypeAnnotations)) {
                    if (checkExclude(annotation)) {
                        skidfuscator.getExemptAnalysis().add(node);
                        node.node.visibleAnnotations.remove(annotation);
                        continue;
                    }
                    getAnnotation(annotation, SkidAnnotation.AnnotationType.TYPE_VISIBLE);
                }
            }
            if (node.node.invisibleTypeAnnotations != null) {
                for (AnnotationNode annotation : new ArrayList<>(node.node.invisibleTypeAnnotations)) {
                    if (checkExclude(annotation)) {
                        skidfuscator.getExemptAnalysis().add(node);
                        node.node.visibleAnnotations.remove(annotation);
                        continue;
                    }
                    getAnnotation(annotation, SkidAnnotation.AnnotationType.TYPE_INVISIBLE);
                }
            }

            node.getMethods()
                    .stream()
                    .filter(e -> !skidfuscator.getExemptAnalysis().isExempt(e))
                    .sorted(new Comparator<MethodNode>() {
                @Override
                public int compare(MethodNode o1, MethodNode o2) {
                    final Parameter parameter1 = new Parameter(o1.getDesc());
                    final Parameter parameter2 = new Parameter(o2.getDesc());

                    final List<Type> args1 = parameter1.getArgs();
                    final List<Type> args2 = parameter2.getArgs();

                    return args1.size() - args2.size();
                }
            }).forEach(method -> {
                getGroup(skidfuscator, method);

                if (method.node.visibleAnnotations != null) {
                    for (AnnotationNode annotation : new ArrayList<>(method.node.visibleAnnotations)) {
                        if (checkExclude(annotation)) {
                            skidfuscator.getExemptAnalysis().add(method);
                            method.node.visibleAnnotations.remove(annotation);
                            continue;
                        }
                        getAnnotation(annotation, SkidAnnotation.AnnotationType.VISIBLE);

                    }
                }

                if (method.node.invisibleAnnotations != null) {
                    for (AnnotationNode annotation : new ArrayList<>(method.node.invisibleAnnotations)) {
                        if (checkExclude(annotation)) {
                            skidfuscator.getExemptAnalysis().add(method);
                            method.node.invisibleAnnotations.remove(annotation);
                            continue;
                        }
                        getAnnotation(annotation, SkidAnnotation.AnnotationType.INVISIBLE);
                    }
                }

                if (method.node.visibleTypeAnnotations != null) {
                    for (TypeAnnotationNode annotation : new ArrayList<>(method.node.visibleTypeAnnotations)) {
                        if (checkExclude(annotation)) {
                            skidfuscator.getExemptAnalysis().add(method);
                            method.node.visibleTypeAnnotations.remove(annotation);
                            continue;
                        }
                        getAnnotation(annotation, SkidAnnotation.AnnotationType.TYPE_VISIBLE);

                    }
                }
                if (method.node.invisibleTypeAnnotations != null) {
                    for (TypeAnnotationNode annotation : new ArrayList<>(method.node.invisibleTypeAnnotations)) {
                        if (checkExclude(annotation)) {
                            skidfuscator.getExemptAnalysis().add(method);
                            method.node.invisibleTypeAnnotations.remove(annotation);
                            continue;
                        }
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

        try (ProgressWrapper progressBar = ProgressUtil.progress(skidfuscator.getClassSource().size())){
            nodes = skidfuscator
                    .getClassSource()
                    .getClassTree()
                    .vertices()
                    .stream()
                    .filter(e -> {
                        progressBar.tick();
                        return skidfuscator.getClassSource().isApplicationClass(e.getName());
                    })
                    .filter(e -> !skidfuscator.getExemptAnalysis().isExempt(e))
                    .collect(Collectors.toList());
        }

        Skidfuscator.LOGGER.log("[#]     > Cached over " + nodes.size() + " classes!");
        Skidfuscator.LOGGER.post("[#] Establishing inheritance...");

        try (ProgressWrapper progressBar = ProgressUtil.progress(nodes.size())){
            nodes.forEach(e -> {
                executor.accept(e);
                progressBar.tick();
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
        try (ProgressWrapper invocationBar = ProgressUtil.progress(nodes.size())) {
            nodes.forEach(c -> {
                for (MethodNode method : c.getMethods()) {

                    final SkidControlFlowGraph cfg = skidfuscator.getIrFactory().getFor(method);

                    if (cfg == null) {
                        System.err.println("Failed to compute CFG for method " + method.toString());
                        continue;
                    }

                    cfg.allExprStream()
                            .filter(e -> e instanceof Invokable)
                            .map(e -> (Invocation) e)
                            .forEach(invocation -> {

                                if (invocation.getName().equals("handle")) {
                                    System.out.println("Invoking " + invocation.getOwner() + "#"
                                            + invocation.getName() + invocation.getDesc());
                                }

                                final ClassMethodHash target;

                                if (invocation instanceof DynamicInvocationExpr) {
                                    final DynamicInvocationExpr e = (DynamicInvocationExpr) invocation;

                                    if (!e.getOwner().equals("java/lang/invoke/LambdaMetafactory")
                                            || !e.getName().equals("metafactory")) {
                                        return;
                                        //throw new IllegalStateException("Invalid invoke dynamic!");
                                    }

                                    assert (e.getBootstrapArgs().length == 3 && e.getBootstrapArgs()[1] instanceof Handle);
                                    final Handle boundFunc = (Handle) e.getBootstrapArgs()[1];

                                    if (boundFunc.getName().equals("handle")) {
                                        /*System.out.println("Invoking dynamic " + invocation.getOwner() + "#"
                                                + invocation.getName() + invocation.getDesc());*/
                                    }
                                    target = new ClassMethodHash(boundFunc.getName(), boundFunc.getDesc(), boundFunc.getOwner());

                                } else if (invocation instanceof InvocationExpr) {
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
                                    /*if (method.getName().equals("onEnable")) {
                                        System.out.println("Found target group of name " + targetGroup.getName());
                                    }*/
                                    invocationToGroupMap.put(invocation, targetGroup);

                                    targetGroup.getInvokers().add(skidInvocation);
                                } else {
                                    /*if (method.getName().equals("onEnable")) {
                                        System.out.println("Failed to find target group for " + target.getDisplayName());
                                    }*/
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
                                } else {
                                    if (method.getName().equals("onEnable")) {
                                        //System.out.println("Failed to find target method for " + target.getDisplayName());
                                    }
                                }
                            });
                }
                invocationBar.tick();
            });
        }
    }

    public SkidGroup getGroup(final ClassMethodHash methodNode) {
        return hashToGroupMap.get(methodNode);
    }


    private SkidGroup getGroup(final Skidfuscator session, final MethodNode methodNode) {
        SkidGroup group = methodToGroupMap.get(methodNode);

        if (group == null) {
            if (methodNode.getName().equals("evaluate")) {
                /*System.out.println(
                        skidfuscator.getClassSource().getClassTree().getAllBranches(methodNode.owner).stream()
                        .map(e -> e.getName() + "\n  -->" + e.getMethods()
                                .stream()
                                .map(f -> f.getName() + f.getDesc())
                                .collect(Collectors.joining("\n  -->  ")))
                        .collect(Collectors.joining("\n"))
                );

                /*System.out.println(
                        skidfuscator.getClassSource().getClassTree().getAllParents(methodNode.owner).stream()
                                .map(e -> e.getName() + "\n  -->" + e.getMethods()
                                        .stream()
                                        .map(f -> f.getName() + f.getDesc())
                                        .collect(Collectors.joining("\n  -->  ")))
                                .collect(Collectors.joining("\n"))
                );*/
            }
            final Set<MethodNode> h = methodNode.isStatic()
                    ? new HashSet<>(Collections.singleton(methodNode))

                    : session
                    .getCxt()
                    .getInvocationResolver()
                    .getHierarchyMethodChain(
                            methodNode.owner,
                            methodNode.getName(),
                            methodNode.getDesc(),
                            true // TODO: SUPER SENSITIVE PIECE OF SHIT
                    );

                    /* session
                        .getClassSource()
                        .getClassTree()
                        .getAllBranches(methodNode.owner)
                        .stream()
                        .filter(e -> e.getMethods().stream()
                                .anyMatch(f -> f.getName().equals(methodNode.getName())
                                            && f.getDesc().equals(methodNode.getDesc()))
                        )
                        .map(e -> e.getMethods().stream()
                                .filter(f -> f.getName().equals(methodNode.getName())
                                        && f.getDesc().equals(methodNode.getDesc()))
                                .findFirst()
                                .get()
                        )
                        .collect(Collectors.toSet());*/
            /*final Set<MethodNode> h = session
                    .getCxt()
                    .getInvocationResolver()
                    .getHierarchyMethodChain(
                            methodNode.owner,
                            methodNode.getName(),
                            methodNode.getDesc(),
                            false
                    );*/
            h.add(methodNode);

            final List<MethodNode> methods = new ArrayList<>(h);

            if (false && methodNode.getName().equals("clone")) {
                System.out.println("Creating group of name " + methodNode.getName() + methodNode.getDesc());

                for (MethodNode method : methods) {
                    System.out.println("  --> " + method.getOwner() + "#" + method.getName() + method.getDesc());
                }
            }


            group = new SkidGroup(methods, skidfuscator);
            group.setAnnotation(((SkidClassNode) methodNode.owner).isAnnotation());
            group.setStatical(((SkidMethodNode) methodNode).isStatic());
            group.setName(methodNode.getName());
            group.setDesc(methodNode.getDesc());
            group.setSynthetic(methodNode.isSynthetic());
            //System.out.println(group.getDesc());

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

            for (MethodNode node : h) {
                for (ClassNode allChild : skidfuscator.getClassSource().getClassTree().getAllChildren(node.owner)) {
                    final ClassMethodHash hash = new ClassMethodHash(
                            methodNode.getName(),
                            methodNode.getDesc(),
                            allChild.getName()
                    );
                    hashToGroupMap.put(hash, group);
                }
            }

            groups.add(group);
        }

        return group;
    }

    private boolean checkExclude(final AnnotationNode node) {
        final String filteredNamePre = node.desc.substring(1);
        final String filteredNamePost = filteredNamePre.substring(0, filteredNamePre.length() - 1);

        return filteredNamePost.equals("dev/skidfuscator/annotations/Exclude");
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
    public Set<SkidInvocation> getInvokers(MethodNode methodNode) {
        return getGroup(skidfuscator, methodNode).getInvokers();
    }

    @Override
    public SkidGroup cache(SkidMethodNode methodNode) {
        final SkidGroup group = getGroup(skidfuscator, methodNode);
        final ControlFlowGraph cfg = skidfuscator.getCxt().getIRCache().get(methodNode);

        if (cfg == null)
            return group;

        cfg.allExprStream()
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
    public SkidGroup getGroup(MethodNode methodNode) {
        return getGroup(skidfuscator, methodNode);
    }
}
