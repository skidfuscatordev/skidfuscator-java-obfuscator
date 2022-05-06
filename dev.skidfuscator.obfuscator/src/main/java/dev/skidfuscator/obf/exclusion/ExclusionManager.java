package dev.skidfuscator.obf.exclusion;

import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class ExclusionManager {
    private final List<Exclusion> exclusions;

    public ExclusionManager(List<Exclusion> exclusionList) {
        this.exclusions = exclusionList;
    }

    public ExclusionManager() {
        this(new ArrayList<>());
    }

    public void add(final String exclusion) {
        exclusions.add(ExclusionHelper.renderExclusion(exclusion));
    }

    public boolean check(final ClassNode node) {
        for (Exclusion exclusion : exclusions) {
            try {
                if (exclusion.test(node))
                    return true;
            } catch (AssertionError e) {
                // Do nothing
            }
        }

        return false;
    }

    public boolean check(final MethodNode node) {
        for (Exclusion exclusion : exclusions) {
            try {
                if (exclusion.test(node))
                    return true;
            } catch (AssertionError e) {
                // Do nothing
            }
        }

        return false;
    }


}
