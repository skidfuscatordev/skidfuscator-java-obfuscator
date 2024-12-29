package dev.skidfuscator.obfuscator.io;

import java.util.Set;

public interface Workspace {
    Set<InputSource> inputSources();

    Set<InputSource> librarySources();


}
