package com.github.auties00.cobalt.registration.push.apns.plist.value;

/**
 * Holds a plist boolean leaf, the binary plist {@code 0x08} (false) and {@code 0x09} (true)
 * markers.
 *
 * <p>An instance represents a true/false flag carried by an APNS plist payload, such as the sandbox
 * indicator on the connect handshake. The primitive {@code boolean} is stored directly.
 *
 * @param value the boolean value
 */
public record PlistBooleanValue(boolean value) implements PlistValue {
}
