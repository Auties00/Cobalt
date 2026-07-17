package com.github.auties00.cobalt.meta.annotation;

import com.github.auties00.cobalt.meta.model.WhatsAppMobilePlatform;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.lang.annotation.*;

/**
 * Declares that the annotated method, constructor, or field implements
 * a specific method or function from a WhatsApp mobile native class.
 *
 * <p>On iOS the method names are Objective-C selectors with a
 * {@code +}/{@code -} prefix (e.g. {@code "-getICDCMeta:"});
 * on Android they are Java/Kotlin method names
 * (e.g. {@code "getICDCMeta"}).
 *
 * <p>Repeatable: a single Cobalt member may map to methods on
 * multiple mobile platforms or classes.
 *
 * <p>Example:
 * <pre>{@code
 * @WhatsAppMobileMethod(className = "WAIdentityIcdcManager", methods = "-getICDCMeta:",
 *                       platform = MobilePlatform.IOS, adaptation = WhatsAppAdaptation.DIRECT)
 * public Optional<IcdcResult> compute(Jid userJid) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Repeatable(WhatsAppMobileMethods.class)
@Documented
public @interface WhatsAppMobileMethod {
    /**
     * Returns the native class name containing the method.
     *
     * @return the class name
     */
    String className();

    /**
     * Returns the method or function name(s) on the target platform.
     *
     * @return one or more method names
     */
    String[] methods();

    /**
     * Returns the mobile platform this method belongs to.
     *
     * @return the mobile platform
     */
    WhatsAppMobilePlatform platform();

    /**
     * Returns the adaptation relationship between the Cobalt
     * implementation and the mobile source.
     *
     * @return the adaptation type
     */
    WhatsAppAdaptation adaptation();
}
