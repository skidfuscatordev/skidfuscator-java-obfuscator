package dev.skidfuscator.obfuscator.exempt;

public interface ExclusionTester<T> {
    boolean test(T var);

    /**
     * @return Debug output to understand the testing
     */
    String toString();
}