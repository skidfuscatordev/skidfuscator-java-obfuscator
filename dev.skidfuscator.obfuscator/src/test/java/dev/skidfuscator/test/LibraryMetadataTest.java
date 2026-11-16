package dev.skidfuscator.test;

import dev.skidfuscator.jghost.GhostHelper;
import dev.skidfuscator.jghost.tree.GhostClassNode;
import dev.skidfuscator.logger.Logger;
import java.io.*;
import java.nio.file.*;
import java.util.jar.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.topdank.byteengineer.commons.data.JarInfo;
import org.topdank.byteio.in.SingleJarDownloader;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class LibraryMetadataTest {
    @TempDir Path root;
    final Logger logger = mock(Logger.class);

    void library(Path path, String name) throws IOException {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, name,
                "<T:Ljava/lang/Object;>Ljava/lang/Object;Ljava/io/Serializable;", "java/lang/Object", new String[]{"java/io/Serializable"});
        writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,"VALUE","Ljava/lang/String;",null,
                "unicode:á:"+Character.toString((char)0xd800)+":😀").visitEnd();
        writer.visitEnd();
        try (var out = new JarOutputStream(Files.newOutputStream(path))) {
            out.putNextEntry(new JarEntry(name+".class")); out.write(writer.toByteArray()); out.closeEntry();
        }
    }

    @Test void cachedMetadataRetainsInterfacesAndGenericSignature() throws Exception {
        Path input=root.resolve("library.jar"); library(input,"First");
        var cache=GhostHelper.getLibrary(logger,input.toFile(),root.resolve("cache").toFile(),false);
        var node=cache.getContents().getClasses().values().iterator().next().read();
        assertEquals("java/io/Serializable",node.interfaces.get(0));
        assertEquals("<T:Ljava/lang/Object;>Ljava/lang/Object;Ljava/io/Serializable;",node.signature);
        assertEquals(node.interfaces,GhostClassNode.of(node).read().interfaces);
        var reloaded=GhostHelper.getLibrary(logger,input.toFile(),root.resolve("cache").toFile(),false)
                .getContents().getClasses().values().iterator().next().read();
        assertEquals(node.fields.get(0).value,reloaded.fields.get(0).value);
    }

    @Test void replacingSameNamedLibraryInvalidatesTheCache() throws Exception {
        Path input=root.resolve("library.jar"),folder=root.resolve("cache"); library(input,"First");
        String original=GhostHelper.getLibrary(logger,input.toFile(),folder.toFile(),false).getSha256();
        library(input,"Second");
        var changed=GhostHelper.getLibrary(logger,input.toFile(),folder.toFile(),false);
        assertNotEquals(original,changed.getSha256());
        assertEquals("Second",changed.getContents().getClasses().values().iterator().next().read().name);
        try (var files=Files.list(folder.resolve("v2"))) { assertEquals(2,files.count()); }
    }

    @Test void corruptedMetadataAndUnreadableLibrariesFailExplicitly() throws Exception {
        Path input=root.resolve("library.jar"),folder=root.resolve("cache"); library(input,"First");
        GhostHelper.getLibrary(logger,input.toFile(),folder.toFile(),false);
        try (var files=Files.list(folder.resolve("v2"))) { Files.writeString(files.findFirst().orElseThrow(),"{}"); }
        assertThrows(IllegalStateException.class,()->GhostHelper.getLibrary(logger,input.toFile(),folder.toFile(),false));
        Files.writeString(input,"not a ZIP");
        assertThrows(UncheckedIOException.class,()->GhostHelper.createFromLibraryFile(logger,input.toFile()));
    }

    @Test void selectsMultiReleaseLibraryVariantForTheDeploymentJdk() throws Exception {
        Path input=root.resolve("multi.jar");
        Manifest manifest=new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version","1.0");
        manifest.getMainAttributes().putValue("Multi-Release","true");
        try (var out=new JarOutputStream(Files.newOutputStream(input),manifest)) {
            for (int release:new int[]{8,11,21}) {
                ClassWriter writer=new ClassWriter(0);
                writer.visit(release+44,Opcodes.ACC_PUBLIC,"Variant",null,"java/lang/Object",null);
                writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,"RELEASE","I",null,release).visitEnd();
                writer.visitEnd();
                String entry=release==8?"Variant.class":"META-INF/versions/"+release+"/Variant.class";
                out.putNextEntry(new JarEntry(entry)); out.write(writer.toByteArray()); out.closeEntry();
            }
        }
        for (int target:new int[]{8,17,25}) {
            var downloader=new SingleJarDownloader<>(new JarInfo(input.toFile()),target); downloader.download();
            var classes=downloader.getJarContents().getClassContents();
            assertEquals(1,classes.size());
            int expected=target==8?8:target==17?11:21;
            assertEquals(expected,classes.iterator().next().getClassNode().node.fields.get(0).value);
            var library=GhostHelper.getLibrary(logger,input.toFile(),root.resolve("cache").toFile(),false,target);
            assertEquals(expected,((Number)library.getContents().getClasses().values().iterator().next().read().fields.get(0).value).intValue());
        }
        try (var files=Files.list(root.resolve("cache/v2"))) { assertEquals(3,files.count()); }
    }
}
