package it.auties.whatsapp.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify that a field or a class has no explicit support from the library
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface Unsupported {
    String warning() default "This property is not supported by WhatsappWeb4j. " +
            "These fields are only intended to develop new features";
}
