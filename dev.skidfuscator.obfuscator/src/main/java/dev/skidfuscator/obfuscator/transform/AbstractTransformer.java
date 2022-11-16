package dev.skidfuscator.obfuscator.transform;

import dev.skidfuscator.obfuscator.Skidfuscator;

import java.util.Collections;
import java.util.List;

public abstract class AbstractTransformer implements Transformer {
    protected final Skidfuscator skidfuscator;
    private final String name;
    private final List<Transformer> children;

    public AbstractTransformer(Skidfuscator skidfuscator, String name) {
        this(skidfuscator, name, Collections.emptyList());
    }

    public AbstractTransformer(Skidfuscator skidfuscator, String name, List<Transformer> children) {
        this.skidfuscator = skidfuscator;
        this.name = name;
        this.children = children;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Transformer> getChildren() {
        return children;
    }
}
