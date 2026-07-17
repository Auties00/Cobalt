package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Outbound MEX mutation that rotates a group's invite code by asking the relay to mint a fresh
 * opaque code string.
 *
 * <p>Issuing this mutation generates a new invite code for the given receiver and invalidates any
 * previously distributed {@code chat.whatsapp.com/<code>} link bound to that receiver. Callers that
 * only need the current code without rotating it use {@link FetchGroupInviteCodeMexRequest} instead.
 *
 * @implNote This implementation emits the {@code input} object with the three fields WA Web always
 * sends: {@code receiver}, {@code entry_point} and {@code server_send_sms}. The
 * {@code server_send_sms} flag is a declared field that WA Web always populates, so it is written
 * unconditionally as {@code false} rather than omitted.
 */
@WhatsAppWebModule(moduleName = "WAWebMexCreateInviteCodeJob")
public final class CreateInviteCodeMexRequest implements MexStanza.Request.Json {
    /**
     * Compiled GraphQL query identifier for the {@code WAWebMexCreateInviteCodeJobMutation}
     * document.
     *
     * <p>The relay maps this id to its persisted operation; the GraphQL text is never sent on the
     * wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCreateInviteCodeJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "26155584267463745";

    /**
     * GraphQL operation name reported alongside this mutation when it is dispatched.
     *
     * <p>Tags the query in latency and error metrics; kept on the request for embedders mirroring
     * WhatsApp's telemetry surface.
     */
    public static final String OPERATION_NAME = "mexCreateInviteCode";

    /**
     * Receiver identifier bound to the {@code input.receiver} GraphQL input field.
     */
    private final String receiver;

    /**
     * Originating UI surface tag bound to the {@code input.entry_point} GraphQL input field.
     */
    private final String entryPoint;

    /**
     * Constructs a new request with the two GraphQL input fields.
     *
     * <p>Both arguments are required and forwarded verbatim to the relay. The {@code entryPoint} is
     * the telemetry attribution tag (for example {@code "CHAT_INFO_INVITE_BUTTON"}) the relay
     * associates with the rotation event.
     *
     * @param receiver   the receiver identifier the code is minted for, never {@code null}
     * @param entryPoint the originating UI surface tag, never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public CreateInviteCodeMexRequest(String receiver, String entryPoint) {
        this.receiver = Objects.requireNonNull(receiver, "receiver cannot be null");
        this.entryPoint = Objects.requireNonNull(entryPoint, "entryPoint cannot be null");
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
     * @implNote This implementation streams the {@code input.receiver}, {@code input.entry_point}
     * and {@code input.server_send_sms} GraphQL fields through fastjson2's {@link JSONWriter} and
     * wraps them in the standard MEX IQ envelope built through
     * {@link MexStanza.Request.Json#createMexNode(String, String)}. The {@code server_send_sms} flag
     * is always emitted as {@code false}, matching WA Web which always populates it.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCreateInviteCodeJob", exports = "mexCreateInviteCode",
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
            writer.writeName("receiver");
            writer.writeColon();
            writer.writeString(receiver);
            writer.writeName("entry_point");
            writer.writeColon();
            writer.writeString(entryPoint);
            writer.writeName("server_send_sms");
            writer.writeColon();
            writer.writeBool(false);
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
