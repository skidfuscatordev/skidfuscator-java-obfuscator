package dev.skidfuscator.obfuscator.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.zip.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class VerifiedDownloadsTest {
    @TempDir Path root;
    Path zip(String name,int size) throws Exception {
        Path archive=root.resolve("archive.zip");
        try (var out=new ZipOutputStream(Files.newOutputStream(archive))) {
            out.putNextEntry(new ZipEntry(name));out.write(new byte[size]);out.closeEntry();
        }
        return archive;
    }
    @Test void rejectsTraversalAndWindowsPaths() throws Exception {
        for (String name:new String[]{"../escape","/absolute","C:/escape","..\\escape"}) {
            Path archive=zip(name,5);
            assertThrows(IOException.class,()->VerifiedDownloads.extract(archive,root.resolve("out"),true,100));
        }
        assertFalse(Files.exists(root.getParent().resolve("escape")));
    }
    @Test void enforcesExpandedBudget() throws Exception {
        Path archive=zip("data",1024);
        assertThrows(IOException.class,()->VerifiedDownloads.extract(archive,root.resolve("out"),true,16));
    }
    @Test void extractsOrdinaryFilesAndRejectsDuplicateOverwrite() throws Exception {
        Path archive=zip("nested/file",5),output=root.resolve("out");
        VerifiedDownloads.extract(archive,output,true,100);
        assertEquals(5,Files.size(output.resolve("nested/file")));
        assertThrows(IOException.class,()->VerifiedDownloads.extract(archive,output,true,100));
    }
    @Test void rejectsTarLinks() throws Exception {
        Path archive=root.resolve("archive.tar.gz");
        try (var out=new TarArchiveOutputStream(new GzipCompressorOutputStream(Files.newOutputStream(archive)))) {
            TarArchiveEntry entry=new TarArchiveEntry("link",TarConstants.LF_SYMLINK);
            entry.setLinkName("../../escape");out.putArchiveEntry(entry);out.closeArchiveEntry();
        }
        assertThrows(IOException.class,()->VerifiedDownloads.extract(archive,root.resolve("out"),false,100));
    }
    @Test void verifiesKnownDigestAndRejectsWrongDigest() throws Exception {
        Path file=root.resolve("data");Files.writeString(file,"abc");
        VerifiedDownloads.verify(file,"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThrows(IOException.class,()->VerifiedDownloads.verify(file,"0".repeat(64)));
    }
    @Test void refusesInsecureTransportAndPreservesDestination() throws Exception {
        Path file=root.resolve("artifact");Files.writeString(file,"previous");
        assertThrows(IOException.class,()->VerifiedDownloads.download(URI.create("http://localhost/artifact"),file,"0".repeat(64),100));
        assertEquals("previous",Files.readString(file));
        try (var files=Files.list(root)) { assertFalse(files.anyMatch(p->p.toString().endsWith(".part"))); }
    }
    @Test void seededRandomIsResettableAndThreadLocal() throws Exception {
        RandomUtil.begin(42);long first=RandomUtil.nextLong();RandomUtil.begin(42);assertEquals(first,RandomUtil.nextLong());
        long[] other=new long[1];Thread thread=new Thread(()->{RandomUtil.begin(42);other[0]=RandomUtil.nextLong();RandomUtil.end();});
        thread.start();thread.join();assertEquals(first,other[0]);RandomUtil.end();
    }
}
