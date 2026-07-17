package com.github.auties00.cobalt.wire.linked.preference;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Distribution ring of the Windows desktop WhatsApp client driving the
 * Windows Push Notification Service (WNS) subscription.
 *
 * <p>Each distribution ring receives a separate notification stream so that
 * pre-release builds of the WhatsApp Universal Windows Platform (UWP) client
 * can roll new push surfaces independently of the production build. The ring
 * is communicated to the relay through the {@code version} attribute of the
 * outbound push-config IQ; the production ring is encoded as the absence of
 * the attribute and therefore returns a {@code null}
 * {@linkplain #wireValue() wire value}.
 */
@ProtobufEnum
public enum WindowsPushChannel {
    /**
     * The internal hybrid-dogfooding ring distributed to Meta employees on
     * pre-release Windows builds. Encoded on the wire as
     * {@code "uwp_hybrid_dogfooding"}.
     */
    HYBRID_DOGFOODING(0, "uwp_hybrid_dogfooding"),

    /**
     * The pre-release alpha ring open to early-access testers. Encoded on
     * the wire as {@code "uwp_alpha"}.
     */
    ALPHA(1, "uwp_alpha"),

    /**
     * The public beta ring open to opted-in users; this is also the relay
     * fallback that the WhatsApp Web routing helper substitutes when an
     * unrecognised ring name is supplied. Encoded on the wire as
     * {@code "uwp_beta"}.
     */
    BETA(2, "uwp_beta"),

    /**
     * The production-release ring distributed through the Microsoft Store.
     * The relay treats the missing {@code version} attribute as the public
     * ring, so {@link #wireValue()} returns {@code null} for this case.
     */
    PUBLIC(3, null);

    /**
     * The protobuf wire-format index associated with this distribution ring.
     */
    final int index;

    /**
     * The literal value emitted on the {@code version} attribute of the
     * push-config stanza, or {@code null} when the attribute should be
     * omitted entirely (production ring).
     */
    final String wireValue;

    /**
     * Constructs a new {@code WindowsPushChannel} with the supplied protobuf
     * index and wire-side string.
     *
     * @param index     the protobuf wire-format index
     * @param wireValue the wire-side string, or {@code null} when the
     *                  attribute should be omitted
     */
    WindowsPushChannel(@ProtobufEnumIndex int index, String wireValue) {
        this.index = index;
        this.wireValue = wireValue;
    }

    /**
     * Returns the protobuf wire-format index associated with this ring.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the wire-side string emitted on the {@code version} attribute,
     * or {@code null} when the ring is encoded as the missing-attribute
     * default.
     *
     * @return the wire string, or {@code null} for the production ring
     */
    public String wireValue() {
        return wireValue;
    }
}
