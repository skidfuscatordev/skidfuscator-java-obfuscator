package dev.skidfuscator.obfuscator.dependency;

import dev.skidfuscator.obfuscator.util.VerifiedDownloads;
import lombok.SneakyThrows;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Locale;

public class DependencyDownloader {
    @SneakyThrows
    public Path download(CommonDependency dependency) {
        Path cache=Paths.get("mappings-cloud"); Files.createDirectories(cache);
        String name=dependency.name().toLowerCase(Locale.ROOT);
        Path destination=cache.resolve(name+"-"+dependency.getSha256().substring(0,12));
        try (FileChannel channel=FileChannel.open(cache.resolve("."+name+".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
             var lock=channel.lock()) {
            if (Files.isRegularFile(destination.resolve(".verified-sha256"))
                    && Files.readString(destination.resolve(".verified-sha256")).equals(dependency.getSha256())) return destination;
            Path stage=Files.createTempDirectory(cache,".download-");
            try {
                Path archive=stage.resolve("artifact");
                VerifiedDownloads.download(URI.create(dependency.getUrl()),archive,dependency.getSha256(),256L*1024*1024);
                Path expanded=stage.resolve("content"); Files.createDirectories(expanded);
                if (dependency.getUrl().endsWith(".zip")) VerifiedDownloads.extract(archive,expanded,true,512L*1024*1024);
                else Files.move(archive,expanded.resolve(name+".json"));
                Files.writeString(expanded.resolve(".verified-sha256"),dependency.getSha256());
                if (Files.exists(destination)) throw new java.io.IOException("Invalid dependency cache: "+destination);
                Files.move(expanded,destination,StandardCopyOption.ATOMIC_MOVE);
            } finally { VerifiedDownloads.deleteTree(stage); }
            return destination;
        }
    }
}
