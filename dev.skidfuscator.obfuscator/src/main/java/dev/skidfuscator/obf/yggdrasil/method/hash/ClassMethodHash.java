package dev.skidfuscator.obf.yggdrasil.method.hash;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

/**
 * @author Ghast
 * @since 08/03/2021
 * SkidfuscatorV2 © 2021
 */

@EqualsAndHashCode(callSuper = true)
@Getter
public class ClassMethodHash extends MethodHash {
    private final String clazz;

    public ClassMethodHash(String name, String desc, String clazz) {
        super(name, desc);
        this.clazz = clazz;
    }

    public ClassMethodHash(MethodNode clazz) {
        super(clazz.getName(), clazz.getDesc());
        this.clazz = clazz.owner.getName();
    }

    public String getDisplayName() {
        return clazz + "#" + getName() + getDesc();
    }
}
