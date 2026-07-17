package com.github.auties00.cobalt.meta.annotation;

import java.lang.annotation.*;

/**
 * Container for repeatable {@link WhatsAppWebModule} annotations.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface WhatsAppWebModules {
    /**
     * Returns the repeated {@link WhatsAppWebModule} annotations.
     *
     * @return the annotations
     */
    WhatsAppWebModule[] value();
}
