package com.github.auties00.cobalt.wire.linked.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.protobuf.model.ProtobufString;

import java.util.ArrayList;
import java.util.List;

/**
 * The audience or enablement state selected for a {@link PrivacySettingType}.
 *
 * <p>This is the root of a closed, two-level hierarchy: every constant in
 * {@link PrivacySettingType} declares the concrete sub-interface of
 * {@code PrivacySettingValue} that enumerates the values it accepts (for example
 * {@link LastSeenPrivacyValue} for {@link PrivacySettingType#LAST_SEEN}), and every
 * such sub-interface is sealed over the {@code record} variants that represent its
 * legal values. Modelling the values as records rather than enum constants lets a
 * value carry the data it needs: the {@code ContactsExcept} variant of a visibility
 * setting holds the blocklist of {@link Jid} values inline through {@link #excluded()},
 * so the audience and its refinement list travel together as one value.
 *
 * <p>A value is fully self-describing: {@link #type()} identifies the setting it
 * configures and {@link #token()} is the wire token the server uses for it. Those two
 * pieces, together with {@link #excluded()}, are encoded into a single
 * {@code type:token[:jid,jid,...]} string for protobuf persistence by
 * {@link #toEncodedValue()} and reconstructed by {@link #of(ProtobufString)}.
 */
public sealed interface PrivacySettingValue
        permits LastSeenPrivacyValue, OnlinePrivacyValue, ProfilePicturePrivacyValue, AboutPrivacyValue,
        ReadReceiptsPrivacyValue, GroupAddPrivacyValue, CallAddPrivacyValue, MessagesPrivacyValue,
        DefenseModePrivacyValue {
    /**
     * Returns the setting that this value configures.
     *
     * @return the owning {@link PrivacySettingType}, never {@code null}
     */
    PrivacySettingType<?> type();

    /**
     * Returns the wire token the WhatsApp servers use for this value.
     *
     * <p>Examples include {@code "all"} for an everyone audience and
     * {@code "contact_blacklist"} for a contacts-except audience.
     *
     * @return the server-side token, never {@code null}
     */
    String token();

    /**
     * Returns the contacts that refine this value.
     *
     * <p>This list is non-empty only for the contacts-except variants, where it is the
     * blocklist of contacts denied access; every other variant returns an empty list.
     *
     * @return an unmodifiable list of refining contacts, never {@code null}
     */
    List<Jid> excluded();

    /**
     * Encodes this value into the {@code type:token[:jid,jid,...]} string used for
     * protobuf persistence.
     *
     * <p>The leading {@link PrivacySettingType#wire()} makes the encoded form
     * self-describing so {@link #of(ProtobufString)} can reconstruct the concrete
     * variant without external context. The trailing comma-separated {@link Jid} list
     * is appended only when {@link #excluded()} is non-empty.
     *
     * @return the encoded representation, never {@code null}
     */
    @ProtobufSerializer
    default String toEncodedValue() {
        var encoded = new StringBuilder(type().wire()).append(':').append(token());
        var excluded = excluded();
        for (var i = 0; i < excluded.size(); i++) {
            encoded.append(i == 0 ? ':' : ',').append(excluded.get(i));
        }
        return encoded.toString();
    }

    /**
     * Reconstructs a value from the {@code type:token[:jid,jid,...]} string produced by
     * {@link #toEncodedValue()}.
     *
     * <p>The wire prefix resolves the {@link PrivacySettingType}, which in turn resolves
     * the token (and the parsed refinement list) to the concrete variant. An input that
     * names an unknown setting or an unsupported token resolves to {@code null} so a
     * stale persisted value never aborts deserialization.
     *
     * @param encoded the encoded representation, or {@code null}
     * @return the reconstructed value, or {@code null} if the input is {@code null} or
     *         does not resolve
     */
    @ProtobufDeserializer
    static PrivacySettingValue of(ProtobufString encoded) {
        if (encoded == null) {
            return null;
        }
        var raw = encoded.toString();
        var firstColon = raw.indexOf(':');
        if (firstColon < 0) {
            return null;
        }
        var wire = raw.substring(0, firstColon);
        var rest = raw.substring(firstColon + 1);
        var secondColon = rest.indexOf(':');
        String token;
        List<Jid> excluded;
        if (secondColon < 0) {
            token = rest;
            excluded = List.of();
        } else {
            token = rest.substring(0, secondColon);
            excluded = new ArrayList<>();
            for (var part : rest.substring(secondColon + 1).split(",")) {
                if (!part.isEmpty()) {
                    excluded.add(Jid.of(part));
                }
            }
        }
        return PrivacySettingType.of(wire)
                .flatMap(type -> type.parse(token, excluded))
                .map(PrivacySettingValue.class::cast)
                .orElse(null);
    }
}
