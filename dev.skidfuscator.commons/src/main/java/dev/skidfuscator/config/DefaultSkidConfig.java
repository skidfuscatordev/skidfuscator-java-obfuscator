package dev.skidfuscator.config;

import com.typesafe.config.Config;

import java.io.File;
import java.util.Collections;

public class DefaultSkidConfig extends DefaultConfig {
    public DefaultSkidConfig(Config config, String path) {
        super(config, path);
    }

    public boolean isDriver() {
        return this.getBoolean("driver.enabled", true);
    }

    public File[] getLibs() {
        return this.getStringList("libraries", Collections.emptyList())
                .stream()
                .map(File::new)
                .distinct()
                .toArray(File[]::new);
    }
}
