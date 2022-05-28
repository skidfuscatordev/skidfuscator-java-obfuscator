package dev.skidfuscator.obfuscator.phantom.jghost.tree;

import com.google.gson.annotations.SerializedName;
import dev.skidfuscator.obfuscator.phantom.jghost.Ghost;
import dev.skidfuscator.obfuscator.phantom.jghost.GhostReader;
import org.objectweb.asm.tree.FieldNode;

public class GhostFieldNode implements GhostReader<FieldNode> {
    @SerializedName("access")
    private int access;
    @SerializedName("name")
    private String name;
    @SerializedName("desc")
    private String desc;
    @SerializedName("signature")
    private String signature;
    @SerializedName("value")
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
