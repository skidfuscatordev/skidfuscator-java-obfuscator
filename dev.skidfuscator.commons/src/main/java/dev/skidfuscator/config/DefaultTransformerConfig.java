package dev.skidfuscator.config;

import com.typesafe.config.Config;

public class DefaultTransformerConfig extends DefaultConfig {
    public DefaultTransformerConfig(Config config, String path) {
        super(config, path);
    }

    public boolean isEnabled() {
        return this.getBoolean("enabled", true);
    }
}
