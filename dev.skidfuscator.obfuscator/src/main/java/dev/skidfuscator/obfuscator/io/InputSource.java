package dev.skidfuscator.obfuscator.io;

import org.topdank.byteengineer.commons.data.JarContents;

import java.io.File;

public interface InputSource {
    JarContents download(final File input);
}
