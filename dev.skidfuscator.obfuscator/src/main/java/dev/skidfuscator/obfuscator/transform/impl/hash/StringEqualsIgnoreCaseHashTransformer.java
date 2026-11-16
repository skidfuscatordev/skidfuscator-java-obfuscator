package dev.skidfuscator.obfuscator.transform.impl.hash;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.config.DefaultTransformerConfig;

public class StringEqualsIgnoreCaseHashTransformer extends StringComparisonHashTransformer {
    public StringEqualsIgnoreCaseHashTransformer(final Skidfuscator skidfuscator) {
        super(skidfuscator, "String EqIgCase Hash", true);
    }

    @Override
    protected DefaultTransformerConfig createConfig() {
        String path = skidfuscator.getTsConfig().hasPath("stringEqualsIgnoreCaseHash")
                ? "stringEqualsIgnoreCaseHash" : "stringEqigcaseHash";
        return new DefaultTransformerConfig(skidfuscator.getTsConfig(), path);
    }
}
