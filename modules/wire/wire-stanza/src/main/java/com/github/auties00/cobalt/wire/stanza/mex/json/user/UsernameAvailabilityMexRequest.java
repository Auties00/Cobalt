package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the MEX IQ stanza that checks whether a candidate username can be claimed.
 *
 * <p>This query backs the live-validation indicator on the username picker. The candidate name is
 * sent as the {@code input} variable; the relay validates length, charset, and reservation status
 * server-side. The {@code source} variable records which surface triggered the check and defaults to
 * {@code "USER_INPUT"}, and the {@code session_id} variable correlates a sequence of checks within
 * one editing session. The reply is consumed through {@link UsernameAvailabilityMexResponse}.
 *
 * @see UsernameAvailabilityMexResponse
 */
@WhatsAppWebModule(moduleName = "WAWebMexUsernameAvailability")
public final class UsernameAvailabilityMexRequest implements MexStanza.Request.Json {
    /**
     * The compiled-document id the relay maps to the persisted query.
     *
     * <p>Emitted as the {@code query_id} attribute of the outbound {@code <query>} stanza.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUsernameAvailabilityQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "26122779627399568";

    /**
     * The GraphQL operation name reported alongside this request.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUsernameAvailabilityQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "mexCheckUsernameAvailabilityQueryJob";

    /**
     * The {@code input} GraphQL variable carrying the candidate username; a {@code null} value is
     * emitted as JSON {@code null}.
     */
    private final String input;

    /**
     * The {@code source} GraphQL variable recording the surface that triggered the check; a
     * {@code null} value is emitted as the default {@code "USER_INPUT"}.
     */
    private final String source;

    /**
     * The {@code session_id} GraphQL variable correlating a sequence of checks within one editing
     * session; a {@code null} value is emitted as JSON {@code null}.
     */
    private final String sessionId;

    /**
     * Constructs a username-availability check request carrying only the candidate name.
     *
     * <p>The candidate name is forwarded verbatim as the {@code input} variable; the relay validates
     * length, charset, and reservation status server-side. The {@code source} variable defaults to
     * {@code "USER_INPUT"} and the {@code session_id} variable is emitted as JSON {@code null}.
     *
     * @param input the candidate username; a {@code null} value is emitted as JSON {@code null}
     */
    public UsernameAvailabilityMexRequest(String input) {
        this(input, null, null);
    }

    /**
     * Constructs a username-availability check request carrying the candidate name along with the
     * triggering surface and editing-session correlation id.
     *
     * <p>The candidate name is forwarded verbatim as the {@code input} variable; the relay validates
     * length, charset, and reservation status server-side. The {@code source} variable records which
     * surface triggered the check and the {@code session_id} variable correlates a sequence of checks
     * within one editing session. All three variables are always materialised on the wire:
     * {@code input} and {@code session_id} fall back to JSON {@code null} when unset, and
     * {@code source} falls back to {@code "USER_INPUT"}.
     *
     * @param input     the candidate username; a {@code null} value is emitted as JSON {@code null}
     * @param source    the triggering surface; a {@code null} value is emitted as {@code "USER_INPUT"}
     * @param sessionId the editing-session correlation id; a {@code null} value is emitted as JSON
     *                  {@code null}
     */
    public UsernameAvailabilityMexRequest(String input, String source, String sessionId) {
        this.input = input;
        this.source = source;
        this.sessionId = sessionId;
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
     * @implNote This implementation always materialises every declared top-level variable, mirroring
     * the relay's compiled query: {@code input} and {@code session_id} are emitted with their value
     * or as JSON {@code null}, and {@code source} defaults to the string {@code "USER_INPUT"} that
     * the dispatcher hard-codes when unset; envelope construction is delegated to
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUsernameAvailability", exports = "mexCheckUsernameAvailabilityQueryJob",
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
            if (input != null) {
                writer.writeString(input);
            } else {
                writer.writeNull();
            }

            writer.writeName("session_id");
            writer.writeColon();
            if (sessionId != null) {
                writer.writeString(sessionId);
            } else {
                writer.writeNull();
            }

            writer.writeName("source");
            writer.writeColon();
            writer.writeString(source != null ? source : "USER_INPUT");
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
