package dev.skidfuscator.obfuscator.io.jar;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.io.AbstractInputSource;
import dev.skidfuscator.obfuscator.phantom.jphantom.PhantomJarDownloader;
import dev.skidfuscator.obfuscator.util.MapleJarUtil;
import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.data.JarContents;

import java.io.File;

public class JarInputSource extends AbstractInputSource {
    public JarInputSource(Skidfuscator skidfuscator) {
        super(skidfuscator);
    }

    @Override
    public JarContents download(File input) {
        final PhantomJarDownloader<ClassNode> downloader = MapleJarUtil.importPhantomJar(
                input,
                skidfuscator
        );

        return downloader.getJarContents();
    }
}
