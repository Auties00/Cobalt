package com.github.auties00.cobalt.meta.annotation;

import java.lang.annotation.*;

/**
 * Container for repeatable {@link WhatsAppMobileMethod} annotations.
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface WhatsAppMobileMethods {
    /**
     * Returns the repeated {@link WhatsAppMobileMethod} annotations.
     *
     * @return the annotations
     */
    WhatsAppMobileMethod[] value();
}
