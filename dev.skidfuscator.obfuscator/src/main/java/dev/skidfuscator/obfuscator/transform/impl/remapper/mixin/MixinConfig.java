package dev.skidfuscator.obfuscator.transform.impl.remapper.mixin;

import com.typesafe.config.Config;
import dev.skidfuscator.config.DefaultTransformerConfig;

public class MixinConfig extends DefaultTransformerConfig {
    public MixinConfig(Config config, String path) {
        super(config, path);
    }

    @Override
    public boolean isEnabled() {
        return this.getBoolean("enabled", false);
    }

    public String getRefmapPath() {
        return this.getString("refmap", "not_found");
    }

    public String getMixinPath() {
        return this.getString("config", "not_found");
    }
}
