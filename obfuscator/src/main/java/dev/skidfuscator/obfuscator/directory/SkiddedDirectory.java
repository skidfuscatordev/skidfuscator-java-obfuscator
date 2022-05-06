package dev.skidfuscator.obfuscator.directory;

import dev.dirs.BaseDirectories;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@UtilityClass
@Getter
public class SkiddedDirectory {
    private Path directory;

    public File getCache() {
        final File file = directory.resolve("cache").toFile();

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.mkdir();
        }

        return file;
    }

    public void init(final String path) {
        if (path == null) {
            try {
                directory = Paths.get(BaseDirectories.get().configDir)
                        .resolve("Skidfuscator");
            } catch (Throwable e) {

                /*
                 * All credit to Recaf by ColeE for this bit. It seems Powershell is
                 * not very nice with us fella path users
                 */
                if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                    directory = Paths.get(System.getenv("APPDATA"), "Skidfuscator");
                } else {
                    throw new IllegalStateException("Failed to initialize Skidded directory", e);
                }
            }
        } else {
            try {
                directory = Paths.get(new URI(path.toLowerCase(Locale.ROOT)));
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to initialize Skidded directory", e);
            }
        }
    }
}
