package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * One selectable Meta AI experience offered in the AI mode picker.
 *
 * <p>WhatsApp can offer several distinct Meta AI experiences (for example a
 * general assistant and more specialised modes) that the user switches between
 * in an AI mode selector. The set of modes on offer is fetched dynamically, so
 * the client can introduce or retire modes without a client update. Each mode
 * carries a numeric {@linkplain #modeId() identifier}, a server-defined
 * {@linkplain #type() type} discriminator, an {@linkplain #experimental()
 * experimental} flag, and the localised {@linkplain #title() title} and
 * {@linkplain #subtitle() subtitle} the picker renders.
 *
 * <p>This model is one such mode as the server reports it. The type
 * discriminator is exposed as a raw string because its value set is not a
 * closed set in the client.
 */
@ProtobufMessage(name = "MetaAiMode")
public final class MetaAiMode {
    /**
     * Numeric identifier of the mode, or {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64)
    final Long modeId;

    /**
     * Server-defined type discriminator of the mode, as a raw marker.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String type;

    /**
     * Whether the mode is experimental. Reported by the server as a flag;
     * {@code false} when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean experimental;

    /**
     * Localised title of the mode shown in the picker. {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String title;

    /**
     * Localised subtitle of the mode shown in the picker. {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String subtitle;

    /**
     * Constructs a new {@code MetaAiMode}. The reference arguments may be
     * {@code null} when the server omitted them.
     *
     * @param modeId       the numeric mode identifier, or {@code null}
     * @param type         the type discriminator marker, or {@code null}
     * @param experimental whether the mode is experimental
     * @param title        the localised title, or {@code null}
     * @param subtitle     the localised subtitle, or {@code null}
     */
    MetaAiMode(Long modeId, String type, boolean experimental, String title, String subtitle) {
        this.modeId = modeId;
        this.type = type;
        this.experimental = experimental;
        this.title = title;
        this.subtitle = subtitle;
    }

    /**
     * Returns the numeric identifier of the mode.
     *
     * @return the mode id, or empty when the server omitted it
     */
    public OptionalLong modeId() {
        return modeId == null ? OptionalLong.empty() : OptionalLong.of(modeId);
    }

    /**
     * Returns the server-defined type discriminator of the mode.
     *
     * @return the type marker, or empty when the server omitted it
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns whether the mode is experimental.
     *
     * @return {@code true} when the server flagged the mode experimental,
     *         {@code false} otherwise
     */
    public boolean experimental() {
        return experimental;
    }

    /**
     * Returns the localised title of the mode.
     *
     * @return the title, or empty when the server omitted it
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the localised subtitle of the mode.
     *
     * @return the subtitle, or empty when the server omitted it
     */
    public Optional<String> subtitle() {
        return Optional.ofNullable(subtitle);
    }
}
