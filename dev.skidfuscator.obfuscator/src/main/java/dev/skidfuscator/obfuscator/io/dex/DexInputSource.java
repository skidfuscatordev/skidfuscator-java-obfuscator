package dev.skidfuscator.obfuscator.io.dex;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.io.AbstractInputSource;
import dev.skidfuscator.obfuscator.util.MapleJarUtil;
import org.mapleir.asm.ClassNode;
import org.topdank.byteengineer.commons.data.JarContents;

import java.io.File;

public class DexInputSource extends AbstractInputSource {
    public DexInputSource(Skidfuscator skidfuscator) {
        super(skidfuscator);
    }

    @Override
    public JarContents download(final File input) {
        final DexJarDownloader<ClassNode> downloader = MapleJarUtil.importDex(
                input,
                skidfuscator
        );

        return downloader.getJarContents();
    }
}
