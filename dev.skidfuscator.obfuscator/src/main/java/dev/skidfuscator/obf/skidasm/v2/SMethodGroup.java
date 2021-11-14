package dev.skidfuscator.obf.skidasm.v2;

import dev.skidfuscator.obf.init.SkidSession;

import java.util.List;

public class SMethodGroup implements Renderable {
    private final List<SMethod> methods;

    public SMethodGroup(List<SMethod> methods) {
        this.methods = methods;
    }

    @Override
    public void render(SkidSession session) {
        for (SMethod method : methods) {
            method.render(session);
        }
    }
}
