package com.github.auties00.cobalt.wire.cloud;

import java.util.Objects;

/**
 * A version of Meta's Graph API that the WhatsApp Cloud API client can target.
 *
 * <p>Every Cloud API request is addressed to a versioned graph base of the form
 * {@code https://graph.facebook.com/<version>/}. Meta releases a new version roughly twice a year and
 * supports each one for about two years from its release; a request to a retired version is served by
 * the oldest still-supported version. The known versions are exposed as constants on this type, and
 * arbitrary versions can be constructed directly.
 *
 * <p>{@link #DEFAULT} names the version the client targets when none is selected; it tracks the latest
 * stable version. {@link #V23_0} is the version Meta's published OpenAPI specification is generated
 * from and the version Cobalt's request and webhook shapes were validated against; because Graph API
 * versions are backward compatible, those shapes remain valid against newer versions.
 *
 * @param version the URL segment this version maps to, for example {@code v23.0}
 */
public record CloudApiVersion(String version) implements Comparable<CloudApiVersion> {
    /**
     * Graph API {@code v19.0}, released January 2024.
     */
    public static final CloudApiVersion V19_0 = new CloudApiVersion("v19.0");

    /**
     * Graph API {@code v20.0}, released May 2024.
     */
    public static final CloudApiVersion V20_0 = new CloudApiVersion("v20.0");

    /**
     * Graph API {@code v21.0}, released October 2024.
     */
    public static final CloudApiVersion V21_0 = new CloudApiVersion("v21.0");

    /**
     * Graph API {@code v22.0}, released January 2025.
     */
    public static final CloudApiVersion V22_0 = new CloudApiVersion("v22.0");

    /**
     * Graph API {@code v23.0}, released May 2025; the version the published OpenAPI specification is
     * generated from.
     */
    public static final CloudApiVersion V23_0 = new CloudApiVersion("v23.0");

    /**
     * Graph API {@code v24.0}, released October 2025.
     */
    public static final CloudApiVersion V24_0 = new CloudApiVersion("v24.0");

    /**
     * Graph API {@code v25.0}, released February 2026.
     */
    public static final CloudApiVersion V25_0 = new CloudApiVersion("v25.0");

    /**
     * The latest version Cobalt is aware of.
     */
    public static final CloudApiVersion LATEST = V25_0;

    /**
     * The version the client targets when none is selected; tracks {@link #LATEST}.
     */
    public static final CloudApiVersion DEFAULT = LATEST;

    /**
     * Validates the canonical constructor's component.
     *
     * @throws NullPointerException if {@code version} is {@code null}
     */
    public CloudApiVersion {
        Objects.requireNonNull(version, "version must not be null");
    }

    /**
     * Returns the version for a raw version segment, reusing a known constant when the segment matches
     * one and constructing a fresh instance otherwise.
     *
     * @param version the raw version segment, for example {@code v23.0}
     * @return the matching version
     * @throws NullPointerException if {@code version} is {@code null}
     */
    public static CloudApiVersion of(String version) {
        Objects.requireNonNull(version, "version must not be null");
        return switch (version) {
            case "v19.0" -> V19_0;
            case "v20.0" -> V20_0;
            case "v21.0" -> V21_0;
            case "v22.0" -> V22_0;
            case "v23.0" -> V23_0;
            case "v24.0" -> V24_0;
            case "v25.0" -> V25_0;
            default -> new CloudApiVersion(version);
        };
    }

    /**
     * Returns the major component of this version.
     *
     * <p>The major component is the integer between the optional leading {@code v} and the {@code .}
     * separator; for {@code v23.0} it is {@code 23}.
     *
     * @return the major version component
     * @throws NumberFormatException if this version does not follow the {@code vMAJOR.MINOR} form
     */
    public int major() {
        return part(0);
    }

    /**
     * Returns the minor component of this version.
     *
     * <p>The minor component is the integer after the {@code .} separator; for {@code v23.0} it is
     * {@code 0}, and a version with no separator reports {@code 0}.
     *
     * @return the minor version component
     * @throws NumberFormatException if this version does not follow the {@code vMAJOR.MINOR} form
     */
    public int minor() {
        return part(1);
    }

    /**
     * Parses the integer at the given dotted position of the numeric portion of this version.
     *
     * @param index the zero-based dotted position, {@code 0} for the major and {@code 1} for the minor
     * @return the parsed component, or {@code 0} when the position is past the end
     * @throws NumberFormatException if the addressed component is not an integer
     */
    private int part(int index) {
        var raw = version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
        var parts = raw.split("\\.");
        return index < parts.length ? Integer.parseInt(parts[index].trim()) : 0;
    }

    /**
     * Compares this version with another by major component, then by minor component.
     *
     * @param other the version to compare with
     * @return a negative integer, zero, or a positive integer as this version is older than, equal to,
     *         or newer than {@code other}
     * @throws NumberFormatException if either version does not follow the {@code vMAJOR.MINOR} form
     */
    @Override
    public int compareTo(CloudApiVersion other) {
        var byMajor = Integer.compare(major(), other.major());
        return byMajor != 0 ? byMajor : Integer.compare(minor(), other.minor());
    }

    /**
     * Returns whether this version is at least as new as the given minimum.
     *
     * @param minimum the minimum version to test against
     * @return {@code true} if this version is equal to or newer than {@code minimum}
     * @throws NullPointerException if {@code minimum} is {@code null}
     * @throws NumberFormatException if either version does not follow the {@code vMAJOR.MINOR} form
     */
    public boolean isAtLeast(CloudApiVersion minimum) {
        Objects.requireNonNull(minimum, "minimum must not be null");
        return compareTo(minimum) >= 0;
    }
}
