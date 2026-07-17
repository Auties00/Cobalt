package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Whether one Click-to-WhatsApp ad entry point may be surfaced to the caller,
 * with the copy to render for it.
 *
 * <p>WhatsApp gates the places where a merchant can start creating a
 * Click-to-WhatsApp ad (the various "promote", "advertise", and ad-management
 * surfaces) behind per-session entitlements. For each entry point or ad
 * experience the server reports whether the client
 * {@linkplain #shouldShow() should show} it. The richer entitlement surface
 * additionally returns the localised {@linkplain #content() primary} and
 * {@linkplain #subContent() secondary} copy strings the client renders on the
 * entry point; the plain surface omits that copy, leaving both empty.
 *
 * <p>This model is one such entry-point entitlement. The
 * {@linkplain #entryPointOrExperience() entry-point identifier} is a
 * server-defined marker exposed as a raw string.
 */
@ProtobufMessage(name = "AdEntryPointEntitlement")
public final class AdEntryPointEntitlement {
    /**
     * Identifier of the entry point or ad experience this entitlement gates,
     * as a server-defined marker. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String entryPointOrExperience;

    /**
     * Whether the client should surface this entry point. Reported by the
     * server as a gate flag; {@code false} when the server reported it hidden
     * or omitted the flag.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean shouldShow;

    /**
     * Primary localised copy to render on the entry point, or {@code null}
     * when the server omitted it (always omitted by the plain entitlement
     * surface).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String content;

    /**
     * Secondary localised copy to render on the entry point, or {@code null}
     * when the server omitted it (always omitted by the plain entitlement
     * surface).
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String subContent;

    /**
     * Constructs a new {@code AdEntryPointEntitlement}. The reference arguments
     * may be {@code null} when the server omitted them; {@code content} and
     * {@code subContent} are always {@code null} for entitlements fetched
     * without copy.
     *
     * @param entryPointOrExperience the gated entry-point or experience marker, or {@code null}
     * @param shouldShow             whether the client should surface the entry point
     * @param content                the primary localised copy, or {@code null}
     * @param subContent             the secondary localised copy, or {@code null}
     */
    AdEntryPointEntitlement(String entryPointOrExperience, boolean shouldShow, String content, String subContent) {
        this.entryPointOrExperience = entryPointOrExperience;
        this.shouldShow = shouldShow;
        this.content = content;
        this.subContent = subContent;
    }

    /**
     * Returns the identifier of the entry point or ad experience this
     * entitlement gates.
     *
     * @return the entry-point or experience marker, or empty when the server
     *         omitted it
     */
    public Optional<String> entryPointOrExperience() {
        return Optional.ofNullable(entryPointOrExperience);
    }

    /**
     * Returns whether the client should surface this entry point.
     *
     * @return {@code true} when the server reported the entry point should be
     *         shown, {@code false} otherwise
     */
    public boolean shouldShow() {
        return shouldShow;
    }

    /**
     * Returns the primary localised copy to render on the entry point.
     *
     * @return the primary copy, or empty when the server omitted it
     */
    public Optional<String> content() {
        return Optional.ofNullable(content);
    }

    /**
     * Returns the secondary localised copy to render on the entry point.
     *
     * @return the secondary copy, or empty when the server omitted it
     */
    public Optional<String> subContent() {
        return Optional.ofNullable(subContent);
    }
}
