package com.github.auties00.cobalt.registration.push.apns.plist.value;

import java.util.Objects;

/**
 * Holds a plist {@code <string>} leaf, the binary plist {@code 0x50..0x5F} (ASCII),
 * {@code 0x60..0x6F} (UTF-16BE), and {@code 0x70..0x7F} (UTF-8) marker families.
 *
 * <p>An instance represents text carried by an APNS plist payload. Every on-wire encoding decodes
 * into the same Java {@link String}, so consumers see a uniform value regardless of the source
 * form.
 *
 * @implNote This implementation stores the already-decoded {@link String} rather than the raw bytes
 *           plus an encoding marker, since the parser is the only call site that sees the original
 *           encoding and downstream consumers always want the textual value.
 * @param value the decoded string
 */
public record PlistStringValue(String value) implements PlistValue {
    /**
     * Constructs a string leaf, rejecting a {@code null} value.
     *
     * @param value the decoded string
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public PlistStringValue {
        Objects.requireNonNull(value, "value");
    }
}
