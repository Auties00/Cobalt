package com.github.auties00.cobalt.meta.annotation;

import com.github.auties00.cobalt.meta.model.WhatsAppWebPlatform;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.lang.annotation.*;

/**
 * Declares that the annotated method, constructor, or field implements
 * a specific export from a WhatsApp Web JavaScript module.
 *
 * <p>Repeatable: a single Cobalt member may map to exports from
 * multiple WA Web modules.
 *
 * <p>Example:
 * <pre>{@code
 * @WhatsAppWebExport(moduleName = "WAWebIdentityIcdcApi", exports = "getICDCMeta",
 *                    adaptation = WhatsAppAdaptation.DIRECT)
 * public Optional<IcdcResult> compute(Jid userJid) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Repeatable(WhatsAppWebExports.class)
@Documented
public @interface WhatsAppWebExport {
    /**
     * Returns the WA Web JavaScript module name containing the export.
     *
     * @return the module name
     */
    String moduleName();

    /**
     * Returns the exported function or variable name(s) from the module.
     *
     * @return one or more export names
     */
    String[] exports();

    /**
     * Returns the web platform scope for this mapping.
     *
     * @return the platform, defaulting to {@link WhatsAppWebPlatform#SHARED}
     */
    WhatsAppWebPlatform platform() default WhatsAppWebPlatform.SHARED;

    /**
     * Returns the adaptation relationship between the Cobalt
     * implementation and the WA Web source.
     *
     * @return the adaptation type
     */
    WhatsAppAdaptation adaptation();
}
