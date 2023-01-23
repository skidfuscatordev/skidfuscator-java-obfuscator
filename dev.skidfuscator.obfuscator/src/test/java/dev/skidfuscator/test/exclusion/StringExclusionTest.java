package dev.skidfuscator.test.exclusion;

import dev.skidfuscator.obfuscator.exempt.ExemptAnalysis;
import dev.skidfuscator.obfuscator.exempt.SimpleExemptAnalysis;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StringExclusionTest {
    private ExemptAnalysis analysis;

    @BeforeEach
    public void init() {
        //this.analysis = new SimpleExemptAnalysis();
    }

    @Test
    public void testJda() {
        final String jdaExempt = "^jda\\/";
        final String[] jdaClasses = {};
    }

}
