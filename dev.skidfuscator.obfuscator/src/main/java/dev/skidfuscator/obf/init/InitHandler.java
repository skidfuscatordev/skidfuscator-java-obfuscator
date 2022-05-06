package dev.skidfuscator.obf.init;

import dev.skidfuscator.obf.SkidInstance;

import java.io.File;

public interface InitHandler {
    SkidSession init(final SkidInstance instance);
}
