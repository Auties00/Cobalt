package com.github.auties00.cobalt.meta.model;

/**
 * Distinguishes shared WhatsApp Web logic from desktop-specific code.
 *
 * <p>WhatsApp Web and Desktop share the same JavaScript codebase.
 * Most modules are identical across all three targets; the desktop
 * variants are used only when a module diverges from the shared bundle.
 */
public enum WhatsAppWebPlatform {
    /**
     * Core logic shared by the browser client and all desktop apps.
     */
    SHARED,

    /**
     * Windows Desktop-specific.
     */
    WINDOWS,

    /**
     * macOS Desktop-specific.
     */
    MAC_OS
}
