package dev.skidfuscator.obf.asm;

import dev.skidfuscator.obf.init.SkidSession;
import dev.skidfuscator.obf.transform_legacy.parameter.Parameter;
import dev.skidfuscator.obf.transform_legacy.parameter.ParameterResolver;
import lombok.Getter;
import org.apache.log4j.Logger;

import java.util.*;

@Getter
public class MethodGroup {
    private static final Logger LOGGER = Logger.getLogger(MethodGroup.class);
    private final List<MethodWrapper> wrappers = new ArrayList<>();
    protected final Set<MethodInvocation> callers = new HashSet<>();
    private final String name;
    private final Parameter desc;

    public MethodGroup(String name, String desc) {
        this.name = name;
        this.desc = new Parameter(desc);
    }

    public void renderCallers(SkidSession skidSession, final ParameterResolver resolver) {
        for (MethodWrapper wrapper : wrappers) {
            wrapper.renderCallers(skidSession, resolver);
        }

    }

    public void render() {
        for (MethodWrapper wrapper : wrappers) {
            LOGGER.info("[FinalRender] Rendering " + wrapper.getDisplayName() + "'s signature");
            wrapper.render();
            LOGGER.info("[FinalRender] Finished rendering " + wrapper.getDisplayName() + "'s signature");
        }
    }
}
