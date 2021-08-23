package dev.skidfuscator.obf.transform_legacy.parameter.impl;

import dev.skidfuscator.obf.asm.MethodGroup;
import dev.skidfuscator.obf.init.SkidSession;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class ZelixMethodGroup extends MethodGroup {
    private static final Logger LOGGER = Logger.getLogger(ZelixMethodGroup.class);
    private long privateKey;
    private long publicKey;
    private boolean root;

    public ZelixMethodGroup(String name, String desc, long privateKey, long publicKey) {
        super(name, desc);
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public Set<ZelixInvocation> getZelixCallers() {
        return callers.stream().map(e -> (ZelixInvocation) e).collect(Collectors.toSet());
    }

    public List<ZelixMethodWrapper> getZelixWrappers() {
        return this.getWrappers().stream().map(e -> (ZelixMethodWrapper) e).collect(Collectors.toList());
    }

    public void renderKey(final SkidSession session) {
        for (ZelixMethodWrapper wrapper : this.getZelixWrappers()) {
            LOGGER.debug("Rendering key of wrapper " + wrapper.getDisplayName() + "...");
            wrapper.renderKey(session);
            LOGGER.debug("Finished rendering key of wrapper " + wrapper.getDisplayName() + "!");
        }
    }
}
