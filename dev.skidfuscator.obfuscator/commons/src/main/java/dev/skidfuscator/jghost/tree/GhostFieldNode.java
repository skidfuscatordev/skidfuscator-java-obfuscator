package dev.skidfuscator.jghost.tree;

import com.google.gson.annotations.SerializedName;
import dev.skidfuscator.jghost.GhostReader;
import org.objectweb.asm.tree.FieldNode;

public class GhostFieldNode implements GhostReader<FieldNode> {
    @SerializedName("acc")
    private int access;
    @SerializedName("nme")
    private String name;
    @SerializedName("dsc")
    private String desc;
    @SerializedName("sig")
    private String signature;
    @SerializedName("val")
    private Object value;

    public GhostFieldNode() {
        super();
    }

    private GhostFieldNode(final FieldNode node) {
        this.access = node.access;
        this.name = node.name;
        this.desc = node.desc;
        this.signature = node.signature;
        this.value = node.value;
    }

    @Override
    public FieldNode read() {
        final FieldNode node = new FieldNode(access, name, desc, signature, value);
        return node;
    }

    public static GhostFieldNode of(final FieldNode fieldNode) {
        return new GhostFieldNode(fieldNode);
    }
}
