package dev.skidfuscator.obf;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.util.List;

@Data
@Builder
public class SkidInstance {
    private File input;
    private File runtime;
    private File libs;
    private File output;
    private boolean preventDump;
    private List<String> exclusions;

    public SkidInstance input(File input) {
        this.input = input;
        return this;
    }

    public SkidInstance runtime(File runtime) {
        this.runtime = runtime;
        return this;
    }

    public SkidInstance libs(File libs) {
        this.libs = libs;
        return this;
    }

    public SkidInstance output(File output) {
        this.output = output;
        return this;
    }

    public SkidInstance preventDump(boolean preventDump) {
        this.preventDump = preventDump;
        return this;
    }

    public SkidInstance exclusions(List<String> exclusions) {
        this.exclusions = exclusions;
        return this;
    }
}
