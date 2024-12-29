package dev.skidfuscator.obfuscator.gui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.skidfuscator.obfuscator.util.Observable;
import lombok.Data;

import java.io.*;

@Data
public class SkidfuscatorConfig {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.skidfuscator";
    private static final String CONFIG_FILE = "gui-config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String lastInputPath;
    private String lastOutputPath;
    private String lastLibsPath;
    private String lastRuntimePath;
    private boolean debugEnabled;
    private boolean phantomEnabled;
    private String lastDirectory;

    private transient Observable<Boolean>
            validInput = new Observable.SimpleObservable<>(false),
            validOutput = new Observable.SimpleObservable<>(false);

    public boolean isValid() {
        return validInput.get() && validOutput.get();
    }

    public static class Builder {
        private final SkidfuscatorConfig config;

        public Builder() {
            this.config = new SkidfuscatorConfig();
        }

        public Builder setLastInputPath(String path) {
            config.lastInputPath = path;
            return this;
        }

        public Builder setLastOutputPath(String path) {
            config.lastOutputPath = path;
            return this;
        }

        public Builder setLastLibsPath(String path) {
            config.lastLibsPath = path;
            return this;
        }

        public Builder setLastRuntimePath(String path) {
            config.lastRuntimePath = path;
            return this;
        }

        public Builder setDebugEnabled(boolean enabled) {
            config.debugEnabled = enabled;
            return this;
        }

        public Builder setPhantomEnabled(boolean enabled) {
            config.phantomEnabled = enabled;
            return this;
        }

        public Builder setLastDirectory(String dir) {
            config.lastDirectory = dir;
            return this;
        }

        public SkidfuscatorConfig build() {
            return config;
        }
    }

    public static SkidfuscatorConfig load() {
        File configFile = new File(CONFIG_DIR, CONFIG_FILE);
        if (!configFile.exists()) {
            return new SkidfuscatorConfig();
        }

        try (Reader reader = new FileReader(configFile)) {
            return GSON.fromJson(reader, SkidfuscatorConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new SkidfuscatorConfig();
        }
    }

    public void save() {
        try {
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists() && !configDir.mkdirs()) {
                throw new IOException("Failed to create config directory");
            }

            File configFile = new File(configDir, CONFIG_FILE);
            try (Writer writer = new FileWriter(configFile)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
