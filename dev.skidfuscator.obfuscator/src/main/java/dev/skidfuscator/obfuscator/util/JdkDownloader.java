package dev.skidfuscator.obfuscator.util;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Locale;

public final class JdkDownloader {
    public static final Path CACHE_DIR=Paths.get(System.getProperty("user.home"),".ssvm","jdk");
    private static volatile boolean jdkDownloaded;
    private static final String VERSION="17.0.13.11.1";
    private record Image(String file,String sha,String root,String home) {}
    // Checksums: https://github.com/corretto/corretto-17/releases/tag/17.0.13.11.1
    private static Image image() {
        String os=System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch=System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        boolean arm=arch.equals("aarch64")||arch.equals("arm64");
        if (!arm && !arch.equals("amd64") && !arch.equals("x86_64")) throw new IllegalStateException("Unsupported JDK architecture: "+arch);
        if (os.equals("linux")) return arm
            ?new Image("amazon-corretto-17.0.13.11.1-linux-aarch64.tar.gz","89ab3c3d0335243033c850153047399b5c028c2a61e842c3a5d1ca7809d46d1a","amazon-corretto-17.0.13.11.1-linux-aarch64","")
            :new Image("amazon-corretto-17.0.13.11.1-linux-x64.tar.gz","31ccb0791940add3fa49b82fff5f87a65dbea27d294f58a753b9775f9752ecc7","amazon-corretto-17.0.13.11.1-linux-x64","");
        if (os.startsWith("mac")) return arm
            ?new Image("amazon-corretto-17.0.13.11.1-macosx-aarch64.tar.gz","33b40aee3ae3db7d298bea0bd34574e755beb2f017ce4eb323f8e28960f0324d","amazon-corretto-17.jdk","Contents/Home")
            :new Image("amazon-corretto-17.0.13.11.1-macosx-x64.tar.gz","17fb370f6b6e4f79e6fb6ee720631d2c3fd39d90f37d079352327aef4d834689","amazon-corretto-17.jdk","Contents/Home");
        if (os.startsWith("windows")&&!arm) return new Image("amazon-corretto-17.0.13.11.1-windows-x64-jdk.zip","eaacdc37c6f23753ebfae341985f11baacfaf84ca972e600803cb409c94ee8ca","jdk17.0.13_11","");
        throw new IllegalStateException("Unsupported JDK platform: "+os+"/"+arch);
    }
    private static Path installed(Image image) { return CACHE_DIR.resolve(image.root()+"-"+image.sha().substring(0,12)); }
    public static Path getCachedJdk() throws IOException {
        Image image=image(); Path root=installed(image), home=root.resolve(image.home());
        if (Files.isRegularFile(root.resolve(".verified-sha256")) && Files.readString(root.resolve(".verified-sha256")).trim().equals(image.sha())
                && Files.isRegularFile(home.resolve("jmods/java.base.jmod"))) { jdkDownloaded=true; return home; }
        return null;
    }
    public static synchronized Path getJdkHome() throws IOException {
        Path existing=getCachedJdk(); if (existing!=null) return existing;
        Files.createDirectories(CACHE_DIR);
        try (FileChannel channel=FileChannel.open(CACHE_DIR.resolve(".install.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
             var lock=channel.lock()) {
            existing=getCachedJdk(); if (existing!=null) return existing;
            Image image=image(); Path staging=Files.createTempDirectory(CACHE_DIR,".install-");
            try {
                Path archive=staging.resolve(image.file()), expanded=staging.resolve("expanded");
                VerifiedDownloads.download(URI.create("https://corretto.aws/downloads/resources/"+VERSION+"/"+image.file()),archive,image.sha(),512L*1024*1024);
                VerifiedDownloads.extract(archive,expanded,image.file().endsWith(".zip"),VerifiedDownloads.MAX_EXPANDED);
                Path root=expanded.resolve(image.root());
                if (!Files.isRegularFile(root.resolve(image.home()).resolve("jmods/java.base.jmod"))) throw new IOException("Incomplete JDK image");
                Files.writeString(root.resolve(".verified-sha256"),image.sha());
                // A corrupt cache is never reused or merged with a new extraction.
                if (Files.exists(installed(image))) throw new IOException("Invalid verified JDK cache; remove "+installed(image)+" and retry");
                Files.move(root,installed(image),StandardCopyOption.ATOMIC_MOVE);
            } finally { VerifiedDownloads.deleteTree(staging); }
        }
        return getCachedJdk();
    }
    public static String getJmodPath() throws IOException { return getJdkHome().resolve("jmods").toString(); }
    public static String getCachedJmodPath() throws IOException { Path home=getCachedJdk(); return home==null?null:home.resolve("jmods").toString(); }
    public static boolean isJdkDownloaded() { return jdkDownloaded; }
}
