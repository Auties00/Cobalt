package com.github.auties00.cobalt.meta.annotation;

import java.lang.annotation.*;

/**
 * Container for repeatable {@link WhatsAppMobileClass} annotations.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface WhatsAppMobileClasses {
    /**
     * Returns the repeated {@link WhatsAppMobileClass} annotations.
     *
     * @return the annotations
     */
    WhatsAppMobileClass[] value();
}
