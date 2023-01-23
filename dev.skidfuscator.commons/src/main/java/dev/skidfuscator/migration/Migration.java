package dev.skidfuscator.migration;

import java.io.File;

public interface Migration {
    void migrate(final File old, final File updated);
}
