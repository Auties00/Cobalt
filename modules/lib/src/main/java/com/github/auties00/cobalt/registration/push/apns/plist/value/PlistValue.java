package com.github.auties00.cobalt.registration.push.apns.plist.value;

/**
 * Models one stanza of an Apple property list as a sealed value tree.
 *
 * <p>An instance represents a single decoded plist value used by the APNS connect handshake. The
 * permitted implementations mirror the eight concrete types defined by Apple's
 * {@code PropertyList-1.0.dtd}: {@link PlistDictionaryValue}, {@link PlistArrayValue},
 * {@link PlistStringValue}, {@link PlistDataValue}, {@link PlistIntegerValue},
 * {@link PlistFloatingPointValue}, {@link PlistBooleanValue}, and {@link PlistDateValue}. Consumers
 * pattern-match exhaustively over these variants when walking a decoded value; because the
 * hierarchy is closed at compile time, the binary and XML parsers and writers can switch on the
 * concrete type without a default branch. Every variant is an immutable {@code record}.
 */
public sealed interface PlistValue permits
        PlistDictionaryValue,
        PlistArrayValue,
        PlistStringValue,
        PlistDataValue,
        PlistIntegerValue,
        PlistFloatingPointValue,
        PlistBooleanValue,
        PlistDateValue {
}
