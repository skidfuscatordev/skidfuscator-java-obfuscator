package dev.skidfuscator.obfuscator.event.annotation;

import dev.skidfuscator.obfuscator.event.EventPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Listen {
    int value() default EventPriority.STANDARD;
}
