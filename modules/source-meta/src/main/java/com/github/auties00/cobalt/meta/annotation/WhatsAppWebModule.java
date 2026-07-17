package com.github.auties00.cobalt.meta.annotation;

import com.github.auties00.cobalt.meta.model.WhatsAppWebPlatform;

import java.lang.annotation.*;

/**
 * Declares that the annotated type adapts code from a WhatsApp Web
 * JavaScript module.
 *
 * <p>Repeatable: a single Cobalt class may map to multiple WA Web modules.
 *
 * <p>Example:
 * <pre>{@code
 * @WhatsAppWebModule("WAWebIdentityIcdcApi")
 * @WhatsAppWebModule(moduleName = "WAWebBizCoexGatingUtils", platform = WebPlatform.WINDOWS)
 * public final class IcdcComputer { ... }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
@Repeatable(WhatsAppWebModules.class)
@Documented
public @interface WhatsAppWebModule {
    /**
     * Returns the WA Web JavaScript module name (e.g. {@code "WAWebIdentityIcdcApi"}).
     *
     * @return the module name
     */
    String moduleName();

    /**
     * Returns the web platform scope for this mapping.
     *
     * @return the platform, defaulting to {@link WhatsAppWebPlatform#SHARED}
     */
    WhatsAppWebPlatform platform() default WhatsAppWebPlatform.SHARED;
}
