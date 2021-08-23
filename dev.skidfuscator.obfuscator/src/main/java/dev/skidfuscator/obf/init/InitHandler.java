package dev.skidfuscator.obf.init;

import java.io.File;

public interface InitHandler {
    SkidSession init(final File jar, final File output);
}
