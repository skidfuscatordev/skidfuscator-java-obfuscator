package dev.skidfuscator.obfuscator.transform.impl.flow.driver;

import com.typesafe.config.Config;
import dev.skidfuscator.config.DefaultTransformerConfig;

public class DriverConfig extends DefaultTransformerConfig {
    public DriverConfig(Config config, String path) {
        super(config, path);
    }

    @Override
    public boolean isEnabled() {
        return this.getBoolean("enabled", false);
    }

    public String getName() {
        return this.getString("path", "skid/Driver");
    }
}