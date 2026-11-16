package dev.skidfuscator.obfuscator.util;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/** Bounded HTTPS downloads and extraction into private staging directories. */
public final class VerifiedDownloads {
    private VerifiedDownloads() {}
    private static final ThreadLocal<Boolean> OFFLINE=ThreadLocal.withInitial(()->false);
    public static void offline(boolean value) { OFFLINE.set(value); }
    public static void clearContext() { OFFLINE.remove(); }
    public static final long MAX_EXPANDED = 2L * 1024 * 1024 * 1024;
    public static void download(URI uri, Path target, String sha256, long maxBytes) throws IOException {
        if (OFFLINE.get()) throw new IOException("Network download blocked by offline mode");
        if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) throw new IOException("A pinned SHA-256 is required");
        Path staged = Files.createTempFile(target.toAbsolutePath().getParent(),".download-",".part");
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(5);
        try {
            URI current = uri;
            for (int redirects=0; redirects<=5; redirects++) {
                if (!"https".equalsIgnoreCase(current.getScheme()) || current.getUserInfo()!=null)
                    throw new IOException("Only credential-free HTTPS artifact URLs are allowed");
                URLConnection raw = open(current);
                raw.setConnectTimeout(10_000); raw.setReadTimeout(30_000);
                HttpURLConnection connection = (HttpURLConnection)raw;
                connection.setInstanceFollowRedirects(false);
                try {
                    int status = connection.getResponseCode();
                    if (status>=300 && status<400) {
                        String location = connection.getHeaderField("Location");
                        if (location==null) throw new IOException("Artifact redirect has no destination");
                        current = current.resolve(location); continue;
                    }
                    if (status!=200) throw new IOException("Artifact server returned HTTP " + status);
                    if (connection.getContentLengthLong()>maxBytes) throw new IOException("Artifact exceeds download budget");
                    try (InputStream input=connection.getInputStream(); OutputStream output=Files.newOutputStream(staged)) {
                        copy(input,output,maxBytes,deadline);
                    }
                    verify(staged,sha256);
                    Files.move(staged,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                    return;
                } finally { connection.disconnect(); }
            }
            throw new IOException("Too many artifact redirects");
        } finally { Files.deleteIfExists(staged); }
    }
    private static URLConnection open(URI uri) throws IOException {
        String noProxy = System.getenv().getOrDefault("NO_PROXY",System.getenv().getOrDefault("no_proxy",""));
        for (String entry : noProxy.split(",")) {
            String host=entry.trim();
            if (!host.isEmpty() && (host.equals("*") || uri.getHost().equals(host)
                    || host.startsWith(".") && uri.getHost().endsWith(host))) return uri.toURL().openConnection(Proxy.NO_PROXY);
        }
        String configured = System.getenv("HTTPS_PROXY");
        if (configured==null) configured=System.getenv("https_proxy");
        if (configured==null || configured.isEmpty()) return uri.toURL().openConnection();
        URI proxy=URI.create(configured);
        if (proxy.getUserInfo()!=null) throw new IOException("Use platform proxy authentication rather than credentials in proxy URLs");
        return uri.toURL().openConnection(new Proxy(Proxy.Type.HTTP,new InetSocketAddress(proxy.getHost(),proxy.getPort()>0?proxy.getPort():80)));
    }
    public static void verify(Path file,String expected) throws IOException {
        String actual = dev.skidfuscator.obfuscator.reliability.RunReport.hash(file.toFile());
        if (!actual.equals(expected)) throw new IOException("Artifact SHA-256 mismatch");
    }
    private static long copy(InputStream input,OutputStream output,long limit,long deadline) throws IOException {
        byte[] buffer=new byte[8192]; long total=0; int length;
        while ((length=input.read(buffer))!=-1) {
            total+=length;
            if (total>limit || System.nanoTime()>deadline || Thread.currentThread().isInterrupted())
                throw new IOException("Artifact size/time budget exceeded");
            output.write(buffer,0,length);
        }
        return total;
    }
    public static void extract(Path archive,Path directory,boolean zip,long maxExpanded) throws IOException {
        Path root=directory.toAbsolutePath().normalize();
        Files.createDirectories(root);
        long remaining=maxExpanded, deadline=System.nanoTime()+java.util.concurrent.TimeUnit.MINUTES.toNanos(5);
        int count=0;
        if (zip) {
            try (ZipInputStream input=new ZipInputStream(Files.newInputStream(archive))) {
                ZipEntry entry;
                while ((entry=input.getNextEntry())!=null) {
                    if (++count>100_000) throw new IOException("Too many archive entries");
                    Path path=safePath(root,entry.getName());
                    if (entry.isDirectory()) Files.createDirectories(path);
                    else {
                        Files.createDirectories(path.getParent());
                        try (OutputStream out=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)) {
                            remaining-=copy(input,out,remaining,deadline);
                        }
                    }
                }
            }
        } else {
            try (TarArchiveInputStream input=new TarArchiveInputStream(new GzipCompressorInputStream(Files.newInputStream(archive)))) {
                TarArchiveEntry entry;
                while ((entry=input.getNextTarEntry())!=null) {
                    if (++count>100_000) throw new IOException("Too many archive entries");
                    if (entry.isSymbolicLink() || entry.isLink() || !entry.isFile() && !entry.isDirectory()) throw new IOException("Archive links and special files are not allowed");
                    Path path=safePath(root,entry.getName());
                    if (entry.isDirectory()) Files.createDirectories(path);
                    else {
                        Files.createDirectories(path.getParent());
                        try (OutputStream out=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)) {
                            remaining-=copy(input,out,remaining,deadline);
                        }
                    }
                }
            }
        }
    }
    private static Path safePath(Path root,String name) throws IOException {
        String normalized=name.replace('\\','/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) throw new IOException("Absolute archive path rejected");
        Path target=root.resolve(normalized).normalize();
        if (!target.startsWith(root) || target.equals(root) && !name.endsWith("/")) throw new IOException("Archive path traversal rejected");
        for (Path p=target;p!=null && p.startsWith(root);p=p.getParent())
            if (Files.isSymbolicLink(p)) throw new IOException("Archive symlink traversal rejected");
        return target;
    }
    public static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root,LinkOption.NOFOLLOW_LINKS)) return;
        try (var paths=Files.walk(root)) {
            for (Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
