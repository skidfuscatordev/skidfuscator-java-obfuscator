package dev.skidfuscator.obfuscator.skidasm;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.attribute.Attribute;
import dev.skidfuscator.obfuscator.attribute.AttributeKey;
import dev.skidfuscator.obfuscator.attribute.AttributeMap;
import dev.skidfuscator.obfuscator.attribute.StandardAttribute;
import dev.skidfuscator.obfuscator.predicate.opaque.ClassOpaquePredicate;
import dev.skidfuscator.obfuscator.skidasm.builder.SkidFieldNodeBuilder;
import dev.skidfuscator.obfuscator.skidasm.builder.SkidMethodNodeBuilder;
import dev.skidfuscator.obfuscator.util.RandomUtil;
import lombok.Getter;
import org.mapleir.asm.ClassNode;
import org.mapleir.ir.code.stmt.ReturnStmt;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;

@Getter // TODO: Deprecate the getter and document the stuff
public class SkidClassNode extends ClassNode {
    private final Skidfuscator skidfuscator;
    private final ClassOpaquePredicate classPredicate;
    private final ClassOpaquePredicate staticPredicate;
    private transient SkidMethodNode clinitNode;
    private transient Boolean mixin;
    private final AttributeMap attributes;

    private transient Integer randomInt;

    /**
     * @param node MapleIR class node object
     */
    public SkidClassNode(org.objectweb.asm.tree.ClassNode node, Skidfuscator session) {
        super(node, false);
        this.skidfuscator = session;
        this.classPredicate = skidfuscator
                .getPredicateAnalysis()
                .getClassPredicate(this);
        this.staticPredicate = skidfuscator
                .getPredicateAnalysis()
                .getClassStaticPredicate(this);

        for (MethodNode method : node.methods) {
            super.getMethods().add(new SkidMethodNode(method, this, session));
        }

        for (FieldNode field : node.fields) {
            super.getFields().add(new SkidFieldNode(field, this, session));
        }

        this.attributes = new AttributeMap();
    }

    /**
     * @return Returns a Method node builder to allow users to properly build methods
     *         without conflicting with other stuff
     */
    public SkidMethodNodeBuilder createMethod() {
        return new SkidMethodNodeBuilder(skidfuscator,this);
    }

    /**
     * @return Returns a Field node builder to allow users to properly build fields
     *         without conflicting with other stuff
     */
    public SkidFieldNodeBuilder createField() {
        return new SkidFieldNodeBuilder(skidfuscator,this);
    }

    public boolean hasAttribute(AttributeKey attributeKey) {
        return attributes.containsKey(attributeKey);
    }

    public <T> T getAttribute(AttributeKey attributeKey) {
        return attributes.poll(attributeKey);
    }

    public <T> void setAttribute(AttributeKey attributeKey, T value) {
        attributes.get(attributeKey).set(value);
    }

    public <T> void addAttribute(AttributeKey attributeKey, T value) {
        attributes.put(attributeKey, new StandardAttribute<>(value));
    }

    /**
     * @return Returns the class init method (<clinit> method)
     */
    public SkidMethodNode getClassInit() {
        if (clinitNode != null)
            return clinitNode;

        /*
         * Try to find an existing one
         */
        for (org.mapleir.asm.MethodNode method : new ArrayList<>(getMethods())) {
            if (method.getName().equals("<clinit>") && method.getDesc().equals("()V")) {
                if (method instanceof SkidMethodNode) {
                    return clinitNode = (SkidMethodNode) method;
                } else {
                    this.getMethods().remove(method);

                    final SkidMethodNode clinit = new SkidMethodNode(method.node, this, skidfuscator);
                    this.getMethods().add(clinit);

                    return clinitNode = clinit;
                }
            }
        }

        /*
         * Build a new one if absent
         */
        final SkidMethodNode clinit = this.createMethod()
                .access(Opcodes.ACC_STATIC)
                .desc("()V")
                .name("<clinit>")
                .phantom(false)
                .build();

        clinit.getEntryBlock().add(new ReturnStmt());

        return clinitNode = clinit;
    }

    public Type getType() {
        return Type.getType("L" + this.getName() + ";");
    }

    /**
     * @return Returns whether the class node is an interface
     */
    public boolean isInterface() {
        return (node.access & Opcodes.ACC_INTERFACE) != 0;
    }

    /**
     * @return Returns whether the class is an annotation or not
     */
    public boolean isAnnotation() {
        return (node.access & Opcodes.ACC_ANNOTATION) != 0;
    }

    /**
     * @return Returns whether the class is static or not (important for subclasses!)
     */
    public boolean isStatic() {
        return (node.access & Opcodes.ACC_STATIC) != 0;
    }

    /**
     * @return Returns whether the class is abstract or not
     */
    public boolean isAbstract() {
        return (node.access & Opcodes.ACC_ABSTRACT) != 0;
    }

    /**
     * @return Returns whether the class is final or not (useful for optimizing group stuff!)
     */
    public boolean isFinal() {
        return (node.access & Opcodes.ACC_FINAL) != 0;
    }

    /**
     * @return Returns whether the class is public or not (important?)
     */
    public boolean isPublic() {
        return (node.access & Opcodes.ACC_PUBLIC) != 0;
    }

    /**
     * @return Returns whether the class has the protected attribute
     */
    public boolean isProtected() {
        return (node.access & Opcodes.ACC_PROTECTED) != 0;
    }

    /**
     * @return Returns whether the class has the private attribute
     */
    public boolean isPrivate() {
        return (node.access & Opcodes.ACC_PRIVATE) != 0;
    }

    /**
     * @return Returns whether the class is super (idk what this is supposed to mean)
     */
    public boolean isSuper() {
        return (node.access & Opcodes.ACC_SUPER) != 0;
    }

    public boolean isMixin() {
        if (mixin != null)
            return mixin;

        if (node.invisibleAnnotations == null) {
            return mixin = false;
        }

        for (AnnotationNode invisibleAnnotation : node.invisibleAnnotations) {
            if (invisibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
                return mixin = true;
            }
        }

        return mixin = false;
    }

    public int getRandomInt() {
        if (randomInt == null) {
            randomInt = RandomUtil.nextInt();
        }

        return randomInt;
    }

    public byte[] toByteArray() {
        final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        this.node.accept(classWriter);
        return classWriter.toByteArray();
    }
}
