package com.github.auties00.cobalt.meta.model;

/**
 * A version of Meta's Graph API that a WhatsApp Cloud API operation can require.
 *
 * <p>This enum mirrors the runtime Cloud API version type as a fixed set of constants so that it can be
 * used as an annotation element value, which the runtime type cannot because annotation elements must be
 * enums. The constants are declared from oldest to newest, so their natural enum ordering reflects
 * version ordering and {@link #compareTo} can be used to test whether one version is at least as new as
 * another.
 */
public enum WhatsAppCloudApiVersion {
    /**
     * Graph API {@code v19.0}, released January 2024.
     */
    V19_0("v19.0"),

    /**
     * Graph API {@code v20.0}, released May 2024.
     */
    V20_0("v20.0"),

    /**
     * Graph API {@code v21.0}, released October 2024.
     */
    V21_0("v21.0"),

    /**
     * Graph API {@code v22.0}, released January 2025.
     */
    V22_0("v22.0"),

    /**
     * Graph API {@code v23.0}, released May 2025; the version the published OpenAPI specification is
     * generated from.
     */
    V23_0("v23.0"),

    /**
     * Graph API {@code v24.0}, released October 2025.
     */
    V24_0("v24.0"),

    /**
     * Graph API {@code v25.0}, released February 2026.
     */
    V25_0("v25.0");

    /**
     * The URL segment this version maps to, for example {@code v23.0}.
     */
    private final String version;

    /**
     * Creates a version backed by the given URL segment.
     *
     * @param version the URL segment this version maps to, for example {@code v23.0}
     */
    WhatsAppCloudApiVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the URL segment this version maps to.
     *
     * @return the version segment, for example {@code v23.0}
     */
    public String version() {
        return version;
    }
}
