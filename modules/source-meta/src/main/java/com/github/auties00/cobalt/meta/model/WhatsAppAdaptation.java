package com.github.auties00.cobalt.meta.model;

/**
 * Describes the relationship between a Cobalt implementation and its
 * WhatsApp source counterpart.
 */
public enum WhatsAppAdaptation {
    /**
     * Direct port: same logic, translated to Java.
     */
    DIRECT,

    /**
     * Architecturally adapted: same purpose, different structure
     * (e.g. constructor DI instead of module imports, executor instead
     * of setTimeout).
     */
    ADAPTED
}
