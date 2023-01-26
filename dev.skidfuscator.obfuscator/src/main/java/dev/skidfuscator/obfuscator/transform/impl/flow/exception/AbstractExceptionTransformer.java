package dev.skidfuscator.obfuscator.transform.impl.flow.exception;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.transform.AbstractTransformer;
import dev.skidfuscator.obfuscator.transform.Transformer;

import java.util.List;

public abstract class AbstractExceptionTransformer extends AbstractTransformer {
    public AbstractExceptionTransformer(Skidfuscator skidfuscator) {
        super(skidfuscator, "Flow Exception");
    }

    public AbstractExceptionTransformer(Skidfuscator skidfuscator, String name, List<Transformer> children) {
        super(skidfuscator, name, children);
    }
}
