package dev.skidfuscator.obf.skidasm.v2;

import dev.skidfuscator.obf.init.SkidSession;
import org.mapleir.asm.ClassNode;

import java.util.List;

public class SClassGroup implements Renderable {
    private final List<SClass> classes;

    public SClassGroup(List<SClass> methods) {
        this.classes = methods;
    }

    @Override
    public void render(SkidSession session) {
        for (SClass method : classes) {
            method.render(session);
        }
    }
    
    public static void create(final SkidSession session, final ClassNode sample) {}
}
