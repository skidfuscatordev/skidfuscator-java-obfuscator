package dev.skidfuscator.obfuscator.transform;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.config.DefaultTransformerConfig;
import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.util.ConsoleColors;
import dev.skidfuscator.obfuscator.util.MiscUtil;

import java.util.Collections;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

public abstract class AbstractTransformer implements Transformer {
    protected final Skidfuscator skidfuscator;
    protected final String name;
    private final DefaultTransformerConfig config;
    private final List<Transformer> children;
    private int success;
    private int skipped;
    private int failed;

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

    public void success() {
        this.success++;
    }

    public void fail() {
        this.failed++;
    }

    public void skip() {
        this.skipped++;
    }

    public int getSuccess() {
        return success;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getFailed() {
        return failed;
    }

    @Override
    public String getResult() {
        return "Executed " + ConsoleColors.CYAN + this.getName() + ConsoleColors.RESET
                + " ["
                + ConsoleColors.WHITE_BOLD + "Success: " + ConsoleColors.GREEN + success + ConsoleColors.RESET
                + ConsoleColors.WHITE_BOLD + " Skipped: " + ConsoleColors.YELLOW + skipped + ConsoleColors.RESET
                + ConsoleColors.WHITE_BOLD + " Failed: " + ConsoleColors.RED + failed + ConsoleColors.RESET
                + "]";
    }
}
