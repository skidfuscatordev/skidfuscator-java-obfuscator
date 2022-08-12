package dev.skidfuscator.jghost.tree;

import com.google.gson.annotations.SerializedName;
import dev.skidfuscator.jghost.GhostReader;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.List;

public class GhostAnnotationNode implements GhostReader<AnnotationNode> {
    @SerializedName("dsc")
    public String desc;

    @SerializedName("vals")
    public List<Object> values;

    public GhostAnnotationNode() {
        super();
    }

    private GhostAnnotationNode(final AnnotationNode node) {
        this.desc = node.desc;
        this.values = node.values;
    }

    @Override
    public AnnotationNode read() {
        final AnnotationNode node = new AnnotationNode(desc);
        node.values = values;

        return node;
    }

    public static GhostAnnotationNode of(final AnnotationNode node) {
        return new GhostAnnotationNode(node);
    }
}
