package dev.skidfuscator.obfuscator;

import lombok.Builder;
import lombok.Getter;

import java.io.File;

@Builder
@Getter
public class SkidfuscatorSession {
    private File input;
    private File output;
    private File libs;
    private File exempt;
    private File runtime;
    private boolean phantom;
}
