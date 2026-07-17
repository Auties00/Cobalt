package com.github.auties00.cobalt.wire.linked.device.pairing;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Objects;
import java.util.Optional;

/**
 * The platform of the mobile WhatsApp client that a companion (WhatsApp Web, Windows, macOS)
 * is linked to.
 *
 * <p>Carried by the {@code <platform name="..."/>} child of the {@code <pair-success>} IQ stanza
 * the server returns at the end of the linked-device pairing handshake. The five tokens are the
 * complete set the WhatsApp mobile apps register: {@code "android"} (consumer WhatsApp on Android),
 * {@code "iphone"} (consumer WhatsApp on iPhone), {@code "ipad"} (consumer WhatsApp on iPad),
 * {@code "smba"} (WhatsApp Business on Android), {@code "smbi"} (WhatsApp Business on iOS).
 *
 * @apiNote
 * Use {@link #isBusiness()} to discriminate the WhatsApp Business variants from the consumer
 * variants. This mirrors WhatsApp Web's {@code WAWebMobilePlatforms.isSMB()} check, which routes
 * Business-only features (Click-To-WhatsApp Ads, the Meta Business Suite bridge, the SMB metered
 * messaging surface) on companion clients.
 */
@ProtobufEnum(name = "LinkedPrimaryPlatform")
public enum LinkedPrimaryPlatform {
    /**
     * Consumer WhatsApp on Android, wire token {@code "android"}.
     */
    ANDROID(0, "android"),

    /**
     * Consumer WhatsApp on iPhone, wire token {@code "iphone"}.
     */
    IOS(1, "iphone"),

    /**
     * Consumer WhatsApp on iPad, wire token {@code "ipad"}.
     */
    IPAD(2, "ipad"),

    /**
     * WhatsApp Business on Android, wire token {@code "smba"} (Small/Medium Business Android).
     */
    ANDROID_BUSINESS(3, "smba"),

    /**
     * WhatsApp Business on iOS, wire token {@code "smbi"} (Small/Medium Business iOS).
     */
    IOS_BUSINESS(4, "smbi");

    /**
     * The protobuf-encoded index used to persist this enum.
     */
    final int index;

    /**
     * The wire token emitted under the {@code name} attribute of the pair-success
     * {@code <platform>} child.
     */
    private final String wireValue;

    /**
     * Constructs a primary-platform variant bound to its protobuf index and wire token.
     *
     * @param index     the protobuf index
     * @param wireValue the wire token
     */
    LinkedPrimaryPlatform(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf-encoded index.
     *
     * @return the index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the wire token emitted under the pair-success {@code <platform name="..."/>}
     * attribute.
     *
     * @return the wire token, never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Returns whether this variant identifies a WhatsApp Business primary
     * ({@link #ANDROID_BUSINESS} or {@link #IOS_BUSINESS}).
     *
     * <p>Mirrors WhatsApp Web's {@code WAWebMobilePlatforms.isSMB()} check.
     *
     * @return {@code true} when the primary is WhatsApp Business; {@code false} otherwise
     */
    public boolean isBusiness() {
        return this == ANDROID_BUSINESS || this == IOS_BUSINESS;
    }

    /**
     * Returns whether this variant identifies a personal (consumer) WhatsApp primary
     * ({@link #ANDROID}, {@link #IOS} or {@link #IPAD}).
     *
     * @return {@code true} when the primary is consumer WhatsApp; {@code false} otherwise
     */
    public boolean isPersonal() {
        return !isBusiness();
    }

    /**
     * Resolves a {@link LinkedPrimaryPlatform} from its wire token.
     *
     * <p>The lookup is lenient: a token outside the documented closed set, or a {@code null}
     * token, resolves to {@link Optional#empty()} rather than raising.
     *
     * @param wireValue the wire token to resolve, may be {@code null}
     * @return the matching variant, or empty when {@code wireValue} is {@code null} or unrecognised
     */
    public static Optional<LinkedPrimaryPlatform> ofWireValue(String wireValue) {
        if (wireValue == null) {
            return Optional.empty();
        }
        for (var value : values()) {
            if (Objects.equals(value.wireValue, wireValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
