package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the MEX request that flips a per-user newsletter setting.
 *
 * <p>Each invocation carries the target newsletter Jid, a setting-type identifier such as
 * {@code "MUTE_ADMIN_ACTIVITY"} or {@code "MUTE_FOLLOWER_ACTIVITY"}, and a toggle value such as
 * {@code "ON"} or {@code "OFF"}; the relay applies the change and echoes the result parsed by
 * {@link UpdateNewsletterUserSettingMexResponse#of(Stanza)}. The instance is created through the
 * public constructor and dispatched as a {@link MexStanza.Request.Json}.
 *
 * @implNote This implementation expects the caller to own both the retry policy and the merge of
 * the result into the local newsletter cache; WhatsApp Web instead drives the mutation through its
 * own backoff wrapper and response-conversion helper.
 */
@WhatsAppWebModule(moduleName = "WAWebMexUpdateNewsletterUserSetting")
public final class UpdateNewsletterUserSettingMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of this mutation on the WhatsApp relay.
     *
     * <p>The value is emitted as the {@code query_id} attribute of the outgoing {@code <query>}
     * child; the relay rejects requests whose persisted-query identifier is unknown.
     */
    public static final String QUERY_ID = "31938993655691868";

    /**
     * Holds the GraphQL operation name reported for this mutation.
     *
     * <p>The value tags latency and error telemetry on observability sinks that key metrics on the
     * operation name.
     */
    public static final String OPERATION_NAME = "mexUpdateNewsletterUserSetting";

    /**
     * Holds the Jid string of the newsletter whose per-user setting is being changed.
     */
    private final String newsletterId;

    /**
     * Holds the setting-type identifier, for example {@code "MUTE_ADMIN_ACTIVITY"}.
     */
    private final String type;

    /**
     * Holds the new setting value, for example {@code "ON"} or {@code "OFF"}.
     */
    private final String value;

    /**
     * Constructs a request that flips a per-user setting on a newsletter.
     *
     * <p>The {@code type} string is one of the newsletter setting-type identifiers
     * ({@code "MUTE_ADMIN_ACTIVITY"} for admin notifications, {@code "MUTE_FOLLOWER_ACTIVITY"} for
     * follower notifications); the {@code value} string is the relay-defined toggle state
     * ({@code "ON"} for muted, {@code "OFF"} for unmuted in the mute surface).
     *
     * @param newsletterId the newsletter Jid whose setting is being changed
     * @param type         the setting-type identifier
     * @param value        the new setting value
     */
    public UpdateNewsletterUserSettingMexRequest(String newsletterId, String type, String value) {
        this.newsletterId = newsletterId;
        this.type = type;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #QUERY_ID}, the persisted-query identifier of this mutation.
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link #OPERATION_NAME}, the operation name reported for this mutation.
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * Serialises this request into a MEX IQ {@link StanzaBuilder} ready to be dispatched through the
     * WhatsApp relay.
     *
     * <p>Produces the {@code {variables: {input: {newsletter_id?, type?, value?}}}} payload consumed
     * by the persisted query identified by {@link #QUERY_ID}; each entry is omitted when its field
     * is {@code null} so the GraphQL schema never receives an explicit {@code null} variable.
     *
     * @implNote This implementation writes the GraphQL variables directly through a
     * {@link JSONWriter} and delegates IQ envelope construction to
     * {@link MexStanza.Request.Json#createMexNode(String, String)}; any {@link IOException}
     * raised by the in-memory writer is wrapped in an {@link UncheckedIOException} since neither
     * sink can fail in practice.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUpdateNewsletterUserSetting", exports = "mexUpdateNewsletterUserSetting",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (newsletterId != null) {
                writer.writeName("newsletter_id");
                writer.writeColon();
                writer.writeString(newsletterId);
            }
            if (type != null) {
                writer.writeName("type");
                writer.writeColon();
                writer.writeString(type);
            }
            if (value != null) {
                writer.writeName("value");
                writer.writeColon();
                writer.writeString(value);
            }
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
