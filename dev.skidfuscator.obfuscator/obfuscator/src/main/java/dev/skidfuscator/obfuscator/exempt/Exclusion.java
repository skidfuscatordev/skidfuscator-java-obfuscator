package dev.skidfuscator.obfuscator.exempt;

import lombok.Builder;
import lombok.Data;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.FieldNode;
import org.mapleir.asm.MethodNode;

/**
 * Exclusion object to allow for quick and easy exclusion testing
 * without having to do too much annoying shit.
 */
@Data
@Builder
public class Exclusion {
    private ExclusionMap testers;

    /**
     * Test if a class is to be excluded
     *
     * @param classNode the class node
     * @return the boolean
     */
    public boolean test(final ClassNode classNode) {
        assert testers.containsKey(ExclusionType.CLASS) : "Trying to test with null class tester";

        return testers.poll(ExclusionType.CLASS).test(classNode);
    }

    /**
     * Test if a method is to be excluded
     *
     * @param methodNode the method node
     * @return the boolean
     */
    public boolean test(final MethodNode methodNode) {
        assert testers.containsKey(ExclusionType.METHOD) : "Trying to test with null method tester";

        return testers.poll(ExclusionType.METHOD).test(methodNode);
    }

    /**
     * Test if a boolean is to be excluded
     *
     * @param fieldNode the class node
     * @return the boolean
     */
    public boolean test(final FieldNode fieldNode) {
        assert testers.containsKey(ExclusionType.FIELD) : "Trying to test with null field tester";

        return testers.poll(ExclusionType.FIELD).test(fieldNode);
    }

    @Override
    public String toString() {
        return "Exclusion{" +
                "testers=" + testers +
                '}';
    }
}