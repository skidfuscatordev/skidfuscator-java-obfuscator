package dev.skidfuscator.obfuscator.protection;

import dev.skidfuscator.obfuscator.event.Listener;

public interface ProtectionProvider extends Listener {
    boolean shouldWarn();

    String getWarning();
}
