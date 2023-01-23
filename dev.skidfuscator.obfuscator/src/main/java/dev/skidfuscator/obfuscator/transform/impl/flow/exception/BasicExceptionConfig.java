package dev.skidfuscator.obfuscator.transform.impl.flow.exception;

import com.typesafe.config.Config;
import dev.skidfuscator.config.DefaultTransformerConfig;

public class BasicExceptionConfig extends DefaultTransformerConfig {
    public BasicExceptionConfig(Config config, String path) {
        super(config, path);
    }

    public BasicExceptionStrength getStrength() {
        return getEnum("strength", BasicExceptionStrength.GOOD);
    }
}
