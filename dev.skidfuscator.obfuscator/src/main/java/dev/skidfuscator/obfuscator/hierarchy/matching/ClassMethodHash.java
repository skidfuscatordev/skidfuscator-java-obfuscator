package dev.skidfuscator.obfuscator.hierarchy.matching;

import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

/**
 * @author Ghast
 * @since 08/03/2021
 * SkidfuscatorV2 © 2021
 */

@Getter
public class ClassMethodHash extends MethodHash {
    private final String clazz;

    public ClassMethodHash(String name, String desc, String clazz) {
        super(name, desc);
        this.clazz = clazz;
    }

    public ClassMethodHash(SkidMethodNode clazz) {
        super(clazz.getName(), clazz.getDesc());
        this.clazz = clazz.owner.getName();
    }

    public String getDisplayName() {
        return clazz + "#" + getName() + getDesc();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), clazz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassMethodHash)) return false;
        if (!super.equals(o)) return false;

        ClassMethodHash that = (ClassMethodHash) o;

        return clazz.equals(that.clazz);
    }
}