package org.topdank.eventbus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ java.lang.annotation.ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public abstract @interface EventTarget {

	public abstract EventPriority priority() default EventPriority.NORMAL;
}