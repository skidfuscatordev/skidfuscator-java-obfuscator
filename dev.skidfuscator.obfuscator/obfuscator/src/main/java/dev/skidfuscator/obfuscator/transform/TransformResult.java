package dev.skidfuscator.obfuscator.transform;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class TransformResult {
    private final int changed;
    private final List<Exception> exceptions;

    public TransformResult(int changed, List<Exception> exceptions) {
        this.changed = changed;
        this.exceptions = exceptions;
    }

    public TransformResult exception(Exception... exceptions) {
        this.exceptions.addAll(Arrays.asList(exceptions));
        return this;
    }

    public static TransformResult of(final int changed) {
        return new TransformResult(changed, new ArrayList<>());
    }
}
