package framework;
import java.lang.annotation.*;
/** The mathematical model may be pure, but this analyzer deliberately does not prove it. */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
public @interface Unsupported { String value(); }
