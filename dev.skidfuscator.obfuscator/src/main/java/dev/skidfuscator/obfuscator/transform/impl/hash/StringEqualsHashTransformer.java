package dev.skidfuscator.obfuscator.transform.impl.hash;

import dev.skidfuscator.obfuscator.Skidfuscator;

public class StringEqualsHashTransformer extends StringComparisonHashTransformer {
    public StringEqualsHashTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "String Equals Hash", false);
    }
}
