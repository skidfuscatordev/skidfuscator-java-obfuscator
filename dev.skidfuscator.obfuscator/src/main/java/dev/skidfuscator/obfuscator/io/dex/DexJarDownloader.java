package dev.skidfuscator.obfuscator.io.dex;

import com.google.common.io.ByteStreams;
import com.googlecode.d2j.dex.ClassVisitorFactory;
import com.googlecode.d2j.dex.Dex2Asm;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.reader.DexFileReader;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.asm.ASMFactory;
import org.topdank.byteengineer.commons.data.*;
import org.topdank.byteio.in.AbstractJarDownloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DexJarDownloader<C extends ClassNode> extends AbstractJarDownloader<C> {

    private final Skidfuscator skidfuscator;
    protected final JarInfo jarInfo;

    public DexJarDownloader(Skidfuscator skidfuscator, JarInfo jarInfo) {
        this.skidfuscator = skidfuscator;
        this.jarInfo = jarInfo;
    }

    public DexJarDownloader(Skidfuscator skidfuscator, ASMFactory<C> factory, JarInfo jarInfo) {
        super(factory);
        this.skidfuscator = skidfuscator;
        this.jarInfo = jarInfo;
    }

    @Override
    public void download() throws IOException {
        // STEP 1: Download the DEX raw
        URL url = null;
        JarURLConnection connection = (JarURLConnection) (url = new URL(jarInfo.formattedURL())).openConnection();
        JarFile jarFile = connection.getJarFile();
        Enumeration<JarEntry> entries = jarFile.entries();
        contents = new LocateableJarContents(url);

        Map<String, byte[]> data = new HashMap<>();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            byte[] bytes = read(jarFile.getInputStream(entry));
            if (entry.getName().endsWith(".class")) {
                data.put(entry.getName(), bytes);
                //System.out.println("[+] " + entry.getName());
            } else {
                JarResource resource = new JarResource(entry.getName(), bytes);
                contents.getResourceContents().add(resource);
            }
        }

        JarContents.ClassNodeContainer classes = new JarContents.ClassNodeContainer();
        ClassVisitorFactory factory = classInternalName -> {
            org.objectweb.asm.tree.ClassNode asmNode = new org.objectweb.asm.tree.ClassNode();
            asmNode.name = classInternalName;

            contents.getClassContents().add(new JarClassData(
                    classInternalName, data.get(classInternalName),
                    new SkidClassNode(asmNode, skidfuscator)
            ));

            return asmNode;
        };

        DexFileReader reader = new DexFileReader(connection.getInputStream());
        DexFileNode node = new DexFileNode();
        reader.accept(node);


        new Dex2Asm().convertDex(
                node,
                factory
        );
    }

    private byte[] read(InputStream inputStream) throws IOException {
        return ByteStreams.toByteArray(inputStream);
    }
}
