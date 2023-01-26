package dev.skidfuscator.obfuscator.transform;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.config.DefaultTransformerConfig;
import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.util.MiscUtil;

import java.util.Collections;
import java.util.List;

public abstract class AbstractTransformer implements Transformer {
    protected final Skidfuscator skidfuscator;
    protected final String name;
    private final DefaultTransformerConfig config;
    private final List<Transformer> children;

    public AbstractTransformer(Skidfuscator skidfuscator, String name) {
        this(skidfuscator, name, Collections.emptyList());
    }

    public AbstractTransformer(Skidfuscator skidfuscator, String name, List<Transformer> children) {
        this.skidfuscator = skidfuscator;
        this.name = name;
        this.children = children;
        this.config = this.createConfig();
    }

    protected <T extends DefaultTransformerConfig> T createConfig() {
        return (T) new DefaultTransformerConfig(skidfuscator.getTsConfig(), MiscUtil.toCamelCase(name));
    }

    public DefaultTransformerConfig getConfig() {
        return config;
    }

    public void register() {
        EventBus.register(this);

        for (Transformer child : children) {
            child.register();
        }
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
