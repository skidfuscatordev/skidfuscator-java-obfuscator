package dev.skidfuscator.obfuscator.transform.impl.string.generator.v3;

import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidFieldNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.builder.SkidMethodNodeBuilder;
import dev.skidfuscator.obfuscator.transform.impl.string.generator.EncryptionGeneratorV3;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import dev.skidfuscator.obfuscator.util.misc.Pair;
import lombok.Getter;
import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.FieldNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.FieldLoadExpr;
import org.mapleir.ir.code.expr.NewArrayExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.expr.invoke.StaticInvocationExpr;
import org.mapleir.ir.code.stmt.FieldStoreStmt;
import org.mapleir.ir.code.stmt.ReturnStmt;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public abstract class AbstractEncryptionGeneratorV3 implements EncryptionGeneratorV3 {
    @Getter
    private final String name;

    public AbstractEncryptionGeneratorV3(String name) {
        this.name = name;
    }

    private final InjectMapping methodMapping = new InjectMapping();
    private final InjectMapping fieldMapping = new InjectMapping();

    @Override
    public void visitPre(SkidClassNode node) {
        final Map<String, Pair<String, String>> fieldRemapMap = new HashMap<>();

        /*
         * Inject fields into the class node
         */
        for (Field declaredField : this.getClass().getDeclaredFields()) {
            declaredField.setAccessible(true);

            /*
             * Skip fields that are not annotated with @Inject
             */
            if (!declaredField.isAnnotationPresent(InjectField.class)) {
                continue;
            }

            final InjectField injectField = declaredField.getAnnotation(InjectField.class);

            final ClassNode classNode = ClassHelper.create(this.getClass());
            final FieldNode fieldNode = classNode.getFields()
                    .stream()
                    .filter(f -> f.node.name.equals(declaredField.getName())
                            && f.node.desc.equals(Type.getDescriptor(declaredField.getType()))
                    ).findFirst().orElse(null);

            // Illegal scenario
            if (fieldNode == null) {
                throw new IllegalStateException("Field node not found for " + declaredField.getName());
            }

            /*
             * 1. Random name tag handling
             * ----------------------------------------------------
             * If the field is tagged with InjectTags.RANDOM_NAME,
             * then we will generate a random name for the field.
             */
            final Set<InjectFieldTag> tags = new HashSet<>(Arrays.asList(injectField.tags())
            );
            if (tags.contains(InjectFieldTag.RANDOM_NAME)) {
                final String fieldDesc = declaredField.getName() + Type.getType(declaredField.getType()).getDescriptor();
                final String randomName = RandomUtil.randomAlphabeticalString(10);
                fieldNode.node.name = randomName;

                fieldRemapMap.put(
                        fieldDesc,
                        new Pair<>(
                                randomName,
                                Type.getDescriptor(declaredField.getType())
                        )
                );
            }

            /*
             * 2. Final field handling
             * ----------------------------------------------------
             * If the field is tagged with InjectTags.FINAL, then we will
             * set the field to be final.
             */
            if (tags.contains(InjectFieldTag.FINAL)) {
                fieldNode.node.access |= ACC_FINAL;
            }

            /*
             * 3. Interface compatibility handling
             * ----------------------------------------------------
             * If the field is tagged with InjectTags.INTERFACE_COMPAT, then we will
             * set the field to be public, static and final
             */
            if (!tags.contains(InjectFieldTag.NO_INTERFACE_COMPAT) && node.isInterface()) {
                fieldNode.node.access = ACC_PUBLIC | ACC_STATIC | ACC_FINAL;
            }

            fieldMapping.mapInject(injectField.value(), fieldNode.getName());

            /*
             * Final field construction, add the field to the class node
             * and remap any incorrect field references.
             */
            final SkidFieldNode skidFieldNode = node.createField()
                    .access(fieldNode.node.access)
                    .name(fieldNode.getName())
                    .desc(fieldNode.getDesc())
                    .signature(fieldNode.node.signature)
                    .value(fieldNode.node.value)
                    .phantom(true)
                    .build();
        }

        /*
         * Inject methods into the class node
         */
        for (Method declaredMethod : this.getClass().getDeclaredMethods()) {
            declaredMethod.setAccessible(true);

            //System.out.println(declaredMethod.getName());
            //System.out.println(declaredMethod.getAnnotation(InjectMethod.class));

            /*
             * Skip methods that are not annotated with @Inject
             */
            if (!declaredMethod.isAnnotationPresent(InjectMethod.class)) {
                continue;
            }

            //System.out.println("Injecting method " + declaredMethod.getName());

            /*
             * Get the method node from the class node
             */
            final ClassNode classNode = ClassHelper.create(this.getClass());
            final Type declaredMethodType = Type.getType(declaredMethod);
            final MethodNode methodNode = classNode.getMethods()
                    .stream()
                    .filter(m -> m.getName().equals(declaredMethod.getName())
                            && m.getDesc().equals(declaredMethodType.getDescriptor())
                    ).findFirst().orElse(null);

            // Illegal scenario
            if (methodNode == null) {
                throw new IllegalStateException("Method node not found for " + declaredMethod.getName());
            }

            final InjectMethod injectMethod = declaredMethod.getAnnotation(InjectMethod.class);

            /*
             * 1. Random name tag handling
             * ----------------------------------------------------
             * If the method is tagged with InjectTags.RANDOM_NAME,
             * then we will generate a random name for the method.
             */
            final Set<InjectMethodTag> tags = new HashSet<>(Arrays.asList(injectMethod.tags())
            );
            if (tags.contains(InjectMethodTag.RANDOM_NAME)) {
                final String randomName = RandomUtil.randomAlphabeticalString(10);
                methodNode.node.name = randomName;
            }

            methodMapping.mapInject(injectMethod.value(), methodNode.node.name);

            /*
             * Final construction, assemble the method node into a live
             * SkidMethodNode and recompute the control flow graph.
             */
            final SkidMethodNode skidMethodNode = node
                    .createMethod()
                    .access(ACC_PUBLIC | ACC_STATIC)
                    .name(methodNode.getName())
                    .desc(methodNode.getDesc())
                    .signature(methodNode.node.signature)
                    .exceptions(methodNode.node.exceptions.toArray(new String[0]))
                    .phantom(true)
                    .build();
            skidMethodNode.node.instructions = methodNode.node.instructions;

            /*
             * Remap any incorrect field references
             */
            skidMethodNode.node.instructions.forEach(insn -> {
                if (insn instanceof org.objectweb.asm.tree.FieldInsnNode) {
                    final org.objectweb.asm.tree.FieldInsnNode fieldInsnNode = (org.objectweb.asm.tree.FieldInsnNode) insn;

                    /*
                     * If the field owner is the same as the class node, then we need to remap it
                     */
                    if (fieldInsnNode.owner.equals(classNode.getName())) {
                        fieldInsnNode.owner = node.getName();
                    }

                    /*
                     * Remap the field name and description to their new values
                     */
                    final Pair<String, String> remappedFieldName = fieldRemapMap.get(
                            fieldInsnNode.name + fieldInsnNode.desc
                    );

                    if (remappedFieldName != null) {
                        fieldInsnNode.name = remappedFieldName.getA();
                        fieldInsnNode.desc = remappedFieldName.getB();
                    } else {
                        if (fieldInsnNode.owner.equals(classNode.getName())) {
                            throw new IllegalStateException("Field remap not found for " + fieldInsnNode.name + fieldInsnNode.desc);
                        }
                    }
                } else if (insn instanceof org.objectweb.asm.tree.MethodInsnNode) {
                    final org.objectweb.asm.tree.MethodInsnNode methodInsnNode = (org.objectweb.asm.tree.MethodInsnNode) insn;

                    /*
                     * If the method owner is the same as the class node, then we need to remap it
                     */
                    if (methodInsnNode.owner.equals(classNode.getName())) {
                        methodInsnNode.owner = node.getName();
                    }

                    /*
                     * Remap the method name and description to their new values
                     */
                    final String remappedMethodName = methodMapping.getMapping(methodInsnNode.name);

                    if (remappedMethodName != null) {
                        methodInsnNode.name = remappedMethodName;
                    } else {
                        if (methodInsnNode.owner.equals(classNode.getName())) {
                            throw new IllegalStateException("Method remap not found for " + methodInsnNode.name);
                        }
                    }
                }
            });

            skidMethodNode.recomputeCfg();
        }
    }

    protected Expr callInjectMethod(final SkidClassNode parent,
                                    final String injectName,
                                    final String desc,
                                    final Expr... args) {
        final String method = methodMapping.getMapping(injectName);

        if (method == null) {
            throw new IllegalStateException("Method mapping not found for " + injectName);
        }

        return new StaticInvocationExpr(
                parent.isInterface() ? InvocationExpr.CallType.INTERFACE : InvocationExpr.CallType.STATIC,
                args,
                parent.getName(),
                method,
                desc
        );
    }

    protected Expr fetchInjectField(final SkidClassNode parent,
                                    final String injectName,
                                    final String desc) {
        final String field = fieldMapping.getMapping(injectName);

        if (field == null) {
            throw new IllegalStateException("Field mapping not found for " + injectName);
        }

        return new FieldLoadExpr(
                null,
                parent.getName(),
                field,
                desc,
                true
        );
    }

    protected Stmt storeInjectField(final SkidClassNode parent,
                                    final String injectName,
                                    final String desc,
                                    final Expr value) {
        final String field = fieldMapping.getMapping(injectName);

        if (field == null) {
            throw new IllegalStateException("Field mapping not found for " + injectName);
        }

        return new FieldStoreStmt(
                null,
                value,
                parent.getName(),
                field,
                desc,
                true
        );
    }

    protected static <T> Expr generateArrayGenerator(final SkidClassNode node, final T[] array, final Type elementType) {
        final SkidMethodNode injector = new SkidMethodNodeBuilder(node.getSkidfuscator(), node)
                .access(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)
                .name(RandomUtil.randomAlphabeticalString(15))
                .desc("()[" + elementType.getDescriptor())
                .phantom(true)
                .build();

        final Expr[] constants = new Expr[array.length];
        for (int i = 0; i < array.length; i++) {
            constants[i] = new ConstantExpr(array[i], elementType);
        }

        final NewArrayExpr arrayExpr = new NewArrayExpr(
                new Expr[]{new ConstantExpr(array.length, Type.INT_TYPE)},
                Type.getType(elementType.getDescriptor() + "[]"),
                constants
        );

        injector.getCfg()
                .getEntry()
                .add(new ReturnStmt(Type.getType(elementType.getDescriptor() + "[]"), arrayExpr));

        return new StaticInvocationExpr(
                node.isInterface() ? InvocationExpr.CallType.INTERFACE : InvocationExpr.CallType.STATIC,
                new Expr[0],
                node.getName(),
                injector.getName(),
                injector.getDesc()
        );
    }

    protected static Expr generateByteArrayGenerator(final SkidClassNode node, final byte[] array) {
        return generatePrimitiveArrayGenerator(node, array, Type.BYTE_TYPE);
    }

    protected static Expr generateIntArrayGenerator(final SkidClassNode node, final int[] array) {
        return generatePrimitiveArrayGenerator(node, array, Type.INT_TYPE);
    }

    private static Expr generatePrimitiveArrayGenerator(final SkidClassNode node, final Object array, final Type elementType) {
        // Validation
        if (!elementType.equals(Type.BYTE_TYPE) && !elementType.equals(Type.INT_TYPE)) {
            throw new IllegalArgumentException("Unsupported primitive type: " + elementType);
        }

        // Check array
        if (array == null) {
            throw new IllegalArgumentException("Array cannot be null");
        }

        // Check array type
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Object is not an array");
        }

        // Check array length
        if (java.lang.reflect.Array.getLength(array) == 0) {
            //throw new IllegalArgumentException("Array length is 0");
        }

        // Check array element type
        if (!elementType.equals(Type.getType(array.getClass().getComponentType()))) {
            throw new IllegalArgumentException("Array element type does not match");
        }

        final SkidMethodNode injector = new SkidMethodNodeBuilder(node.getSkidfuscator(), node)
                .access(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)
                .name(RandomUtil.randomAlphabeticalString(15))
                .desc("()[" + elementType.getDescriptor())
                .phantom(true)
                .build();

        final int length = java.lang.reflect.Array.getLength(array);
        final Expr[] constants = new Expr[length];

        for (int i = 0; i < length; i++) {
            Object value = java.lang.reflect.Array.get(array, i);
            constants[i] = new ConstantExpr(value, elementType);
        }

        final NewArrayExpr arrayExpr = new NewArrayExpr(
                new Expr[]{new ConstantExpr(length, Type.INT_TYPE)},
                Type.getType("[" + elementType.getDescriptor()),
                constants
        );

        injector.getCfg()
                .getEntry()
                .add(new ReturnStmt(Type.getType("[" + elementType.getDescriptor()), arrayExpr));

        return new StaticInvocationExpr(
                node.isInterface() ? InvocationExpr.CallType.INTERFACE : InvocationExpr.CallType.STATIC,
                new Expr[0],
                node.getName(),
                injector.getName(),
                injector.getDesc()
        );
    }

    // Convenience methods for Map conversion
    protected static Expr generateIntArrayFromMap(final SkidClassNode node, Map<Integer, Integer> map) {
        int[] array = new int[map.size() * 2];
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            array[i++] = entry.getKey();
            array[i++] = entry.getValue();
        }
        return generateIntArrayGenerator(node, array);
    }

    protected static Expr generateByteArrayFromInts(final SkidClassNode node, final int[] array) {
        byte[] bytes = new byte[array.length * 4];
        for (int i = 0; i < array.length; i++) {
            int value = array[i];
            bytes[i * 4] = (byte) (value >>> 24);
            bytes[i * 4 + 1] = (byte) (value >>> 16);
            bytes[i * 4 + 2] = (byte) (value >>> 8);
            bytes[i * 4 + 3] = (byte) value;
        }
        return generateByteArrayGenerator(node, bytes);
    }

    // Helper method to handle arrays of different types
    protected static Expr generateArray(final SkidClassNode node, final Object array) {
        if (array instanceof byte[]) {
            return generateByteArrayGenerator(node, (byte[]) array);
        } else if (array instanceof int[]) {
            return generateIntArrayGenerator(node, (int[]) array);
        } else if (array instanceof Map) {
            return generateIntArrayFromMap(node, (Map<Integer, Integer>) array);
        }
        throw new IllegalArgumentException("Unsupported array type: " + array.getClass());
    }

    static class InjectMapping {
        private final Map<String, String> fieldMap = new HashMap<>();

        public void mapInject(String original, String mapped) {
            fieldMap.put(original, mapped);
        }

        public String getMapping(String original) {
            return fieldMap.get(original);
        }
    }
}
