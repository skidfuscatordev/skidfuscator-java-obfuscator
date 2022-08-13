package dev.skidfuscator.testclasses.evaluator.test.impl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface TestAnnotation {
    String string();

    int intValue();

    double doubleValue();
}
