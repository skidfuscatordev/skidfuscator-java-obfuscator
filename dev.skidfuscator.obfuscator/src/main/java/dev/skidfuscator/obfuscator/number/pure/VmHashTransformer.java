package dev.skidfuscator.obfuscator.number.pure;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.number.hash.HashTransformer;
import dev.skidfuscator.obfuscator.number.hash.SkiddedHash;
import dev.skidfuscator.obfuscator.predicate.factory.PredicateFlowGetter;
import dev.skidfuscator.obfuscator.ssvm.JdkBootClassFinder;
import dev.skidfuscator.obfuscator.ssvm.SystemProps$RawNatives;
import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.MethodInvoker;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.classloading.BootClassFinder;
import dev.xdark.ssvm.classloading.RuntimeBootClassFinder;
import dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller;
import dev.xdark.ssvm.execution.PanicException;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.filesystem.FileManager;
import dev.xdark.ssvm.filesystem.HostFileManager;
import dev.xdark.ssvm.filesystem.SimpleFileManager;
import dev.xdark.ssvm.invoke.Argument;
import dev.xdark.ssvm.invoke.InvocationUtil;
import dev.xdark.ssvm.memory.management.MemoryManager;
import dev.xdark.ssvm.mirror.member.JavaMethod;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.natives.SystemPropsNatives;
import jdk.internal.util.SystemProps;
import lombok.Data;
import lombok.SneakyThrows;
import org.mapleir.app.service.CompleteResolvingJarDumper;
import org.mapleir.app.service.LibraryClassSource;
import org.mapleir.app.service.LocateableClassNode;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller.install;
import static dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller.supplyFromRuntime;

public class VmHashTransformer implements HashTransformer {
    private final Skidfuscator skidfuscator;
    private VirtualMachine vm;

    public VmHashTransformer(Skidfuscator skidfuscator) {
        this.skidfuscator = skidfuscator;
        this.init();

        // Post init
        this.selectRandomMethod();

    }

    private JavaMethod selectedMethod;
    private ParameterMatch predicateParam;
    private final Random random = new Random();
    private Object[] randomArgs;  // Store random values for consistency
    private InvocationUtil invocationUtil;
    private JavaMethod printStackTrace;

    private void initializeRandomArgs() {
        Type[] types = Type.getArgumentTypes(selectedMethod.getDesc());
        // Generate random values for all parameters except the predicate parameter
        for (int i = 0; i < types.length; i++) {
            if (i == predicateParam.getIndex()) {
                continue;
            }
            randomArgs[i] = generateRandomValue(types[i]);
        }
    }

    private Object generateRandomValue(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return random.nextBoolean();
            case Type.BYTE:
                return (byte) (random.nextInt(256) - 128);
            case Type.CHAR:
                return (char) random.nextInt(65536);
            case Type.SHORT:
                return (short) (random.nextInt(65536) - 32768);
            case Type.INT:
                return random.nextInt(Integer.MAX_VALUE - 1) + 1;
            case Type.FLOAT:
                return random.nextFloat() * Float.MAX_VALUE;
            case Type.LONG:
                return random.nextLong();
            case Type.DOUBLE:
                return random.nextDouble() * Double.MAX_VALUE;
            default:
                throw new IllegalArgumentException("Unsupported parameter type: " + type);
        }
    }

    public void selectRandomMethod() {
        if (methodMatches.isEmpty()) {
            throw new IllegalStateException("No valid methods found for hash transformation");
        }

        List<JavaMethod> methods = new ArrayList<>(methodMatches.keySet());
        this.selectedMethod = methods.get(random.nextInt(methods.size()));

        Set<ParameterMatch> matches = methodMatches.get(selectedMethod);
        List<ParameterMatch> paramList = new ArrayList<>(matches);
        this.predicateParam = paramList.get(random.nextInt(paramList.size()));

        /*System.out.println(String.format(
                "Selecting method %s", selectedMethod.getOwner().getName() + "." + selectedMethod.getName() + selectedMethod.getDesc()
        ));*/

        // Initialize random arguments for each parameter (except predicate param)
        this.randomArgs = new Object[Type.getArgumentTypes(selectedMethod.getDesc()).length];
        initializeRandomArgs();
    }

    @Override
    public SkiddedHash hash(int starting, BasicBlock vertex, PredicateFlowGetter caller) {
        if (selectedMethod == null) {
            throw new IllegalStateException("No method selected for hashing");
        }

        try {
            int hashed = hash(starting);
            Expr hashExpr = hash(vertex, caller);

            this.selectRandomMethod();

            return new SkiddedHash(hashExpr, hashed);
        } catch (VMException | PanicException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    private Expr createConstantExpr(Type type, Object value) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return new ConstantExpr((Boolean) value ? 1 : 0, Type.INT_TYPE);
            case Type.BYTE:
                return new ConstantExpr((Byte) value, type);
            case Type.CHAR:
                return new ConstantExpr((Character) value, type);
            case Type.SHORT:
                return new ConstantExpr((Short) value, type);
            case Type.INT:
                return new ConstantExpr((Integer) value, type);
            case Type.FLOAT:
                return new ConstantExpr((Float) value, type);
            case Type.LONG:
                return new ConstantExpr((Long) value, type);
            case Type.DOUBLE:
                return new ConstantExpr((Double) value, type);
            default:
                throw new IllegalArgumentException("Unsupported parameter type: " + type);
        }
    }

    @Override
    public int hash(int starting) {
        if (selectedMethod == null) {
            throw new IllegalStateException("No method selected for hashing");
        }

        final Argument[] args = new Argument[randomArgs.length];

        for (int i = 0; i < args.length; i++) {
            if (i == predicateParam.getIndex()) {
                args[i] = Argument.int32(skidfuscator.getLegacyHasher().hash(starting));
            } else {
                args[i] = VmUtil.getArgument(vm, randomArgs[i], Type.getArgumentTypes(selectedMethod.getDesc())[i]);
            }
        }

        try {
            return (int) invocationUtil.invokeInt(selectedMethod, args);
        } catch (VMException e) {
            //invocationUtil.invokeVoid(printStackTrace, Argument.reference(e.getOop()));
            methodMatches.remove(selectedMethod);
            selectRandomMethod();
            //System.out.println("Reflushing... found " + selectedMethod.getName() + selectedMethod.getDesc() + " instead");
            return hash(starting);
        } catch (Exception e) {
            methodMatches.remove(selectedMethod);
            selectRandomMethod();
            //System.out.println("Reflushing... found " + selectedMethod.getName() + selectedMethod.getDesc() + " instead");
            return hash(starting);
        }
    }

    @Override
    public Expr hash(BasicBlock vertex, PredicateFlowGetter caller) {
        if (selectedMethod == null) {
            throw new IllegalStateException("No method selected for hashing");
        }

        Type[] types = Type.getArgumentTypes(selectedMethod.getDesc());
        Expr[] invokeArgs = new Expr[types.length];
        for (int i = 0; i < invokeArgs.length; i++) {
            if (i == predicateParam.getIndex()) {
                invokeArgs[i] = skidfuscator.getLegacyHasher().hash(vertex, caller);
            } else {
                invokeArgs[i] = createConstantExpr(types[i], randomArgs[i]);
            }
        }

        final Expr invoke = new StaticInvocationExpr(
                invokeArgs,
                selectedMethod.getOwner().getName().replace(".", "/"),
                selectedMethod.getName(),
                selectedMethod.getDesc()
        );

        //System.out.println(String.format("Invoking with %s", invoke));

        return invoke;
    }

    private static final String[] ILLEGAL_PATTERNS = {
            // sun/
            "sun/",
            "com/sun/",
            // jdk specific
            "jdk/internal/",
            "jdk/swing/interop/DragSourceContextWrapper",
            "com/oracle/",
            "com/ibm/",
    };

    @SneakyThrows
    private void init() {
        ImagineBreaker.openBootModules();

        // -- SENSITIVE FUCKERY DO NOT TOUCH

        this.vm = new VirtualMachine() {
            @Override
            protected BootClassFinder createBootClassFinder() {
                try {
                    return new JdkBootClassFinder(RuntimeBootClassFinder.create());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create JDK class definer", e);
                }
            }

            @Override
            protected FileManager createFileManager() {
                return new SimpleFileManager();
            }
        };
        //vm.getProperties().put("java.class.path", "");

        final VMInterface vmi = vm.getInterface();
        final MemoryManager memoryManager = vm.getMemoryManager();
        vm.initialize();

        // [fix] fuck modules
        InstanceClass jdk_internal_module_ModuleBootstrap = (InstanceClass) vm.findBootstrapClass("jdk/internal/module/ModuleBootstrap");
        vmi.setInvoker(
                vm.getSymbols().java_lang_System(), "initPhase2", "(ZZ)I",
                ctx -> {
                    ctx.setResult(0);
                    return Result.ABORT;
                }
        );
        vmi.setInvoker(
                jdk_internal_module_ModuleBootstrap, "boot", "()V",
                MethodInvoker.noop()
        );

        // [fix] fuck JDK9+ raw natives on Liberica
        SystemProps$RawNatives.init(vm);

        // [fix] jdk8+ memory fuckery
        if (vm.getJvmVersion() > 8) {
            // Bug in SSVM makes it think there are overlapping sleeps, so until that gets fixed we stub out sleeping.
            InstanceClass thread = vm.getSymbols().java_lang_Thread();
            vmi.setInvoker(thread.getMethod("sleep", "(J)V"), MethodInvoker.noop());

            // SSVM manages its own memory, and this conflicts with it. Stubbing it out keeps everyone happy.
            InstanceClass bits = (InstanceClass) vm.findBootstrapClass("java/nio/Bits");
            if (bits != null) vmi.setInvoker(bits.getMethod("reserveMemory", "(JJ)V"), MethodInvoker.noop());
        }

        // ==== FINALLY ====
        vm.bootstrap();
        // =================

        // Create a loader pulling classes and files from a root directory
        final CompleteResolvingJarDumper dumper = new CompleteResolvingJarDumper(
                null,
                skidfuscator.getClassSource()
        );
        final SupplyingClassLoaderInstaller.DataSupplier runtime = supplyFromRuntime();
        final SupplyingClassLoaderInstaller.Helper helper = install(vm, new SupplyingClassLoaderInstaller.DataSupplier() {
                    @Override
                    public byte[] getClass(String className) {
                        // Give priority to active runtime classes
                        try {
                            final byte[] data = runtime.getClass(className);
                            if (data.length > 0) {
                                return data;
                            }
                            throw new IllegalStateException(String.format(
                                    "Could not find %s in classpath",
                                    className
                            ));
                        } catch (Exception e) {
                            final LocateableClassNode locateableClassNode = skidfuscator.getClassSource().findClass(className.replace(".", "/"));
                            if (locateableClassNode == null) {
                                throw new IllegalStateException(String.format(
                                        "Could not find %s in classpath",
                                        className
                                ));
                            }

                            final ClassWriter writer = dumper.buildClassWriter(
                                    skidfuscator.getClassSource().getClassTree(),
                                    ClassWriter.COMPUTE_FRAMES
                            );

                            locateableClassNode.node.node.accept(writer);
                            return writer.toByteArray();
                        }
                    }

                    @Override
                    public byte[] getResource(String resourcePath) {
                        return new byte[0];
                    }
                }
        );
        invocationUtil = InvocationUtil.create(vm);


        final Map<MethodNode, Set<ParameterMatch>> potentialCandidates = new HashMap<>();

        //System.out.println("Class sources: ");

        for (LibraryClassSource library : skidfuscator.getClassSource().getLibraries()) {
            //System.out.println(String.format("-  Source: %s [x%d]", library.getClass().getName(), library.size()));
        }

        // Phase 1: Collect all of the compatible methods statically
        for (ClassNode vertex : skidfuscator.getClassSource().iterateWithLibraries()) {
            if (vertex.isInterface() || vertex.isAnnotation() || vertex.isEnum())
                continue;

            // [fix] Sun classes are not compatible and weird and whacky. Probably
            //       more to come
            if (Arrays.stream(ILLEGAL_PATTERNS).anyMatch(vertex.getName()::startsWith)) {
                /*System.out.println(String.format(
                        "Skipping %s due to illegal pattern",
                        vertex.getName()
                ));*/
                continue;
            }

            for (org.mapleir.asm.MethodNode method : vertex.getMethods()) {
                //System.out.println("Checking method " + method.getName() + " " + method.getDesc());
                if (!isCandidate(method))
                    continue;

                //System.out.println("Candidate");
                final Set<ParameterMatch> matches = getCandidate(method);
                potentialCandidates.put(method, matches);
            }
        }

        // Phase 2: Filter candidates based on loadable classes
        final Map<MethodNode, Set<ParameterMatch>> filteredCandidates = new HashMap<>();
        final Map<ClassNode, InstanceClass> classes = new HashMap<>();

        for (Map.Entry<MethodNode, Set<ParameterMatch>> entry : potentialCandidates.entrySet()) {
            MethodNode method = entry.getKey();
            String owner = method.getOwner();

            try {
                // Try to load the class if we haven't already
                if (!classes.containsKey(method.getOwnerClass())) {
                    InstanceClass klass = helper.loadClass(owner.replace("/", "."));
                    classes.put(method.getOwnerClass(), klass);
                }

                // If we reach here, class loaded successfully, so add the method to filtered candidates
                filteredCandidates.put(method, entry.getValue());
            } catch (VMException | PanicException | ClassNotFoundException e) {
                System.out.println("Failed to load class for method " + owner + "." + method.getName());
                //e.printStackTrace();
                // Skip this method since its class couldn't be loaded
                continue;
            }
        }

        // Replace the original potentialCandidates with the filtered ones
        potentialCandidates.clear();
        potentialCandidates.putAll(filteredCandidates);


        // Phase 3:
        potentialCandidates.forEach((method, matches) -> {
            final InstanceClass klass = classes.get(method.getOwnerClass());
            final JavaMethod javaMethod = klass.getMethod(method.getName(), method.getDesc());

            methodMatches.put(javaMethod, matches);
        });

        //System.out.println("Method matches: " + methodMatches.size());

        final InstanceClass vmExceptionKlass = helper.loadClass("java.lang.Throwable");
        printStackTrace = vmExceptionKlass.getMethod("printStackTrace", "()V");
    }

    private final Map<JavaMethod, Set<ParameterMatch>> methodMatches = new HashMap<>();

    private boolean isCandidate(final org.mapleir.asm.MethodNode method) {
        if (!method.isStatic() || method.isClinit() || method.isInit() || method.isAbstract() || method.isNative() || !method.isPublic())
            return false;

        final Type[] types = Type.getArgumentTypes(method.getDesc());

        // TODO: Add long support
        if (types.length == 0 || Arrays.stream(types).noneMatch(type -> type.getSort() == Type.INT/* || type.getSort() == Type.LONG)*/))
            return false;

        if (Arrays.stream(types).anyMatch(type -> type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY))
            return false;

        return Type.getReturnType(method.getDesc()) == Type.INT_TYPE;
    }

    private Set<ParameterMatch> getCandidate(final org.mapleir.asm.MethodNode method) {
        final Type[] types = Type.getArgumentTypes(method.getDesc());
        final Set<ParameterMatch> matches = new HashSet<>();

        int index = 0;
        for (Type type : types) {
            if (type.getSort() == Type.INT) {
                matches.add(new ParameterMatch(index, type));
            }

            index += type.getSize();
        }

        return matches;
    }

    @Data static class ParameterMatch {
        private final int index;
        private final Type type;
    }
}

