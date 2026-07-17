package com.github.auties00.cobalt.meta.annotation;

import com.github.auties00.cobalt.meta.model.WhatsAppMobilePlatform;

import java.lang.annotation.*;

/**
 * Declares that the annotated type adapts code from a WhatsApp mobile
 * native class.
 *
 * <p>On iOS the class name is an Objective-C class
 * (e.g. {@code "WAIdentityIcdcManager"}); on Android it is a
 * fully-qualified Java/Kotlin class
 * (e.g. {@code "com.whatsapp.identity.IcdcManager"}).
 *
 * <p>Repeatable: a single Cobalt class may map to classes on
 * multiple mobile platforms.
 *
 * <p>Example:
 * <pre>{@code
 * @WhatsAppMobileClass(className = "WAIdentityIcdcManager", platform = MobilePlatform.IOS)
 * @WhatsAppMobileClass(className = "com.whatsapp.identity.IcdcManager", platform = MobilePlatform.ANDROID)
 * public final class IcdcComputer { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(WhatsAppMobileClasses.class)
@Documented
public @interface WhatsAppMobileClass {
    /**
     * Returns the native class name on the target mobile platform.
     *
     * @return the class name
     */
    String className();

    /**
     * Returns the mobile platform this class belongs to.
     *
     * @return the mobile platform
     */
    WhatsAppMobilePlatform platform();
}
