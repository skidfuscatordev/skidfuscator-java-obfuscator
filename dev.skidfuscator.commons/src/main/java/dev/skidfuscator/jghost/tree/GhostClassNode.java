package dev.skidfuscator.jghost.tree;

import com.google.gson.annotations.SerializedName;
import dev.skidfuscator.builder.ClassNodeBuilder;
import dev.skidfuscator.jghost.GhostReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GhostClassNode implements GhostReader<ClassNode> {
    /* Base class data */
    @SerializedName("ver")
    private int version;
    @SerializedName("acc")
    private int access;
    @SerializedName("nme")
    private String name;
    @SerializedName("sig")
    private String signature;
    @SerializedName("super")
    private String superName;
    @SerializedName("itfs")
    private String[] interfaces;
    @SerializedName("mthds")
    private List<GhostMethodNode> methods;
    @SerializedName("flds")
    private List<GhostFieldNode> fields;
    @SerializedName("vanns")
    private List<GhostAnnotationNode> visibleAnnotations;
    @SerializedName("invanns")
    private List<GhostAnnotationNode> invisibleAnnotations;

    public GhostClassNode() {
        super();
    }

    private GhostClassNode(final ClassNode classNode) {
        this.version = classNode.version;
        this.access = classNode.access;
        this.name = classNode.name;
        this.superName = classNode.superName;

        this.fields = classNode.fields
                .stream()
                .map(GhostFieldNode::of)
                .collect(Collectors.toList());

        this.methods = classNode.methods
                .stream()
                .map(GhostMethodNode::of)
                .collect(Collectors.toList());

        if (classNode.visibleAnnotations != null) {
            this.visibleAnnotations = classNode.visibleAnnotations
                    .stream()
                    .map(GhostAnnotationNode::of)
                    .collect(Collectors.toList());
        }

        if (classNode.invisibleAnnotations != null) {
            this.invisibleAnnotations = classNode.invisibleAnnotations
                    .stream()
                    .map(GhostAnnotationNode::of)
                    .collect(Collectors.toList());
        }
    }


    @Override
    public ClassNode read() {
        final ClassNode node = new ClassNodeBuilder()
                .name(name)
                .access(access)
                .signature(signature)
                .superName(superName)
                .interfaces(interfaces)
                .build();

        node.version = version;
        node.methods = new ArrayList<>();
        node.superName = superName;

        for (GhostMethodNode methodNode : methods) {
            node.methods.add(methodNode.read());
        }

        for (GhostFieldNode field : fields) {
            node.fields.add(field.read());
        }

        if (visibleAnnotations != null) {
            node.visibleAnnotations = new ArrayList<>();
            for (GhostAnnotationNode visibleAnnotation : visibleAnnotations) {
                node.visibleAnnotations.add(visibleAnnotation.read());
            }
        }

        if (invisibleAnnotations != null) {
            node.invisibleAnnotations = new ArrayList<>();

            for (GhostAnnotationNode invisibleAnnotation : invisibleAnnotations) {
                node.invisibleAnnotations.add(invisibleAnnotation.read());
            }
        }
        return node;
    }

    public static GhostClassNode of(final ClassNode classNode) {
        return new GhostClassNode(classNode);
    }
}
