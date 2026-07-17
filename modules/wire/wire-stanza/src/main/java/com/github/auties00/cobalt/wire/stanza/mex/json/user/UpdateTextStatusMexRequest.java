package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the MEX IQ stanza that publishes or clears the user's ephemeral text status entry.
 *
 * <p>This request backs the Status tab "Add to my status" composer. The status body, optional emoji
 * decoration, and ephemeral duration are sent as the {@code input} variable; publishing an empty
 * status clears any existing entry. The reply is consumed through
 * {@link UpdateTextStatusMexResponse}.
 *
 * @see UpdateTextStatusMexResponse
 */
@WhatsAppWebModule(moduleName = "WAWebMexUpdateTextStatusJob")
public final class UpdateTextStatusMexRequest implements MexStanza.Request.Json {
    /**
     * The compiled-document id the relay maps to the persisted mutation.
     *
     * <p>Emitted as the {@code query_id} attribute of the outbound {@code <query>} stanza.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUpdateTextStatusJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "9152604461510864";

    /**
     * The GraphQL operation name reported alongside this request.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUpdateTextStatusJobMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "mexUpdateTextStatus";

    /**
     * The body text of the status, possibly {@code null}.
     */
    private final String text;

    /**
     * The optional emoji decoration, possibly {@code null}.
     */
    private final String emoji;

    /**
     * The ephemeral duration in seconds.
     */
    private final long ephemeralDurationSec;

    /**
     * Constructs an update-text-status mutation request.
     *
     * <p>An empty {@code text} string is coerced to {@code null}, a {@code null} {@code emoji} is
     * omitted from the variables payload entirely, and when both {@code text} and {@code emoji} are
     * absent the duration is reset to {@code 0}, publishing the empty status that clears any
     * existing entry. These normalisation rules are applied at serialisation time.
     *
     * @param text the text body of the status, or {@code null} or empty to clear it
     * @param emoji the optional emoji decoration, or {@code null} to omit
     * @param ephemeralDurationSec the ephemeral duration in seconds, or {@code 0} for no expiry
     */
    public UpdateTextStatusMexRequest(String text, String emoji, long ephemeralDurationSec) {
        this.text = text;
        this.emoji = emoji;
        this.ephemeralDurationSec = ephemeralDurationSec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits
     * {@code {"variables": {"input": {"text": ..., "emoji"?: {"content": ...}, "ephemeral_duration_sec": ...}}}}
     * after applying the normalisation rules described on the constructor; the {@code emoji}
     * sub-object is emitted only when an emoji decoration is supplied, and {@code text} is
     * serialised as JSON {@code null} when the normalised value is empty; envelope construction is
     * delegated to {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUpdateTextStatusJob", exports = "mexUpdateTextStatus",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        var normalisedText = (text == null || text.isEmpty()) ? null : text;
        var normalisedDuration = ephemeralDurationSec;
        if (normalisedText == null && emoji == null && normalisedDuration != 0L) {
            normalisedDuration = 0L;
        }
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            writer.writeName("text");
            writer.writeColon();
            if (normalisedText == null) {
                writer.writeNull();
            } else {
                writer.writeString(normalisedText);
            }
            if (emoji != null) {
                writer.writeName("emoji");
                writer.writeColon();
                writer.startObject();
                writer.writeName("content");
                writer.writeColon();
                writer.writeString(emoji);
                writer.endObject();
            }
            writer.writeName("ephemeral_duration_sec");
            writer.writeColon();
            writer.writeInt64(normalisedDuration);
            writer.endObject();
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return Json.createMexNode(QUERY_ID, output.toString());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
