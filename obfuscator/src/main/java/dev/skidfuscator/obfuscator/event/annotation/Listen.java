package dev.skidfuscator.obfuscator.event.annotation;

import dev.skidfuscator.obfuscator.event.EventPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class is the annotation which allows for methods to be registered
 * in the EventBus as listeners of a particular event.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Listen {
    /**
     * @return Value which represents the priority of the event listener
     */
    int value() default EventPriority.STANDARD;
}
