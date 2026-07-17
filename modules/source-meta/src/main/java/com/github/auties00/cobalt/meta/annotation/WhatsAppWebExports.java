package com.github.auties00.cobalt.meta.annotation;

import java.lang.annotation.*;

/**
 * Container for repeatable {@link WhatsAppWebExport} annotations.
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface WhatsAppWebExports {
    /**
     * Returns the repeated {@link WhatsAppWebExport} annotations.
     *
     * @return the annotations
     */
    WhatsAppWebExport[] value();
}
