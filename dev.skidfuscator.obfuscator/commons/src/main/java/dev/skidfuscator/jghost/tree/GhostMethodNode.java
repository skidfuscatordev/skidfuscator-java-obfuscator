package dev.skidfuscator.jghost.tree;

import com.google.gson.annotations.SerializedName;
import dev.skidfuscator.builder.MethodNodeBuilder;
import dev.skidfuscator.jghost.GhostReader;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GhostMethodNode implements GhostReader<MethodNode> {
    @SerializedName("nme")
    private String name;

    @SerializedName("acc")
    private int access;

    @SerializedName("dsc")
    private String desc;

    @SerializedName("sig")
    private String signature;

    @SerializedName("exs")
    private List<String> exceptions;

    @SerializedName("vanns")
    private List<GhostAnnotationNode> visibleAnnotations;

    @SerializedName("invanns")
    private List<GhostAnnotationNode> invisibleAnnotations;

    public GhostMethodNode() {
        super();
    }

    private GhostMethodNode(final MethodNode node) {
        this.access = node.access;
        this.name = node.name;
        this.desc = node.desc;
        this.signature = node.signature;
        this.exceptions = node.exceptions.isEmpty() ? null : node.exceptions;

        if (node.visibleAnnotations != null) {
            this.visibleAnnotations = node.visibleAnnotations
                    .stream()
                    .map(GhostAnnotationNode::of)
                    .collect(Collectors.toList());
        }

        if (node.invisibleAnnotations != null) {
            this.invisibleAnnotations = node.invisibleAnnotations
                    .stream()
                    .map(GhostAnnotationNode::of)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public MethodNode read() {
        final MethodNode methodNode = new MethodNodeBuilder()
                .access(access)
                .name(name)
                .desc(desc)
                .signature(signature)
                .exceptions(exceptions == null ? new String[0] : exceptions.toArray(new String[0]))
                .build();

        if (visibleAnnotations != null) {
            methodNode.visibleAnnotations = new ArrayList<>();
            for (GhostAnnotationNode visibleAnnotation : visibleAnnotations) {
                methodNode.visibleAnnotations.add(visibleAnnotation.read());
            }
        }

        if (invisibleAnnotations != null) {
            methodNode.invisibleAnnotations = new ArrayList<>();

            for (GhostAnnotationNode invisibleAnnotation : invisibleAnnotations) {
                methodNode.invisibleAnnotations.add(invisibleAnnotation.read());
            }
        }

        return methodNode;
    }

    public static GhostMethodNode of(final MethodNode methodNode) {
        return new GhostMethodNode(methodNode);
    }
}
