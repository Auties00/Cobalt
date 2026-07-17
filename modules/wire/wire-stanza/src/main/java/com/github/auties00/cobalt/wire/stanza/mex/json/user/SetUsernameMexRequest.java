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
 * Builds the MEX IQ stanza that claims, reserves, or updates a WhatsApp username.
 *
 * <p>This request backs the username picker on the Settings screen and the username step during
 * onboarding. The candidate name is sent as the {@code input} variable, with {@code reserved},
 * {@code session_id}, and {@code source} as optional companion variables; setting
 * {@code reserved=true} pre-claims the username during onboarding without finalising it, and the
 * standard settings flow tags {@code source} with {@code "USER_INPUT"}. The reply is consumed
 * through {@link SetUsernameMexResponse}.
 *
 * @see SetUsernameMexResponse
 */
@WhatsAppWebModule(moduleName = "WAWebMexSetUsernameJob")
public final class SetUsernameMexRequest implements MexStanza.Request.Json {
    /**
     * The compiled-document id the relay maps to the persisted mutation.
     *
     * <p>Emitted as the {@code query_id} attribute of the outbound {@code <query>} stanza.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexSetUsernameJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "25757341163897635";

    /**
     * The GraphQL operation name reported alongside this request.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexSetUsernameJobMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "mexSetUsernameQueryJob";

    /**
     * The {@code input} GraphQL variable carrying the candidate username.
     */
    private final String input;

    /**
     * The {@code reserved} flag, possibly {@code null}.
     */
    private final Boolean reserved;

    /**
     * The {@code session_id} field, possibly {@code null}.
     */
    private final String sessionId;

    /**
     * The {@code source} entry-point tag, possibly {@code null}.
     */
    private final String source;

    /**
     * Constructs a set-username mutation request.
     *
     * <p>Passing {@code reserved=true} pre-claims the username during onboarding without finalising
     * it; the typical post-onboarding flow later re-dispatches with {@code reserved=false}.
     * {@code sessionId} ties the reservation to a registration flow, and {@code source} tags the
     * entry point ({@code "USER_INPUT"} is the value the settings UI emits).
     *
     * @param input the candidate username to claim or update; {@code null} is emitted as JSON
     *              {@code null}
     * @param reserved whether the username should be reserved; {@code null} is emitted as JSON
     *                 {@code null}
     * @param sessionId the registration-flow session identifier; {@code null} is emitted as JSON
     *                  {@code null}
     * @param source the entry-point tag; {@code null} is emitted as JSON {@code null}
     */
    public SetUsernameMexRequest(String input, Boolean reserved, String sessionId, String source) {
        this.input = input;
        this.reserved = reserved;
        this.sessionId = sessionId;
        this.source = source;
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
     * the relay's compiled mutation: {@code input}, {@code reserved}, {@code session_id}, and
     * {@code source} are each emitted with their supplied value or as JSON {@code null} when unset;
     * envelope construction is delegated to
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexSetUsernameJob", exports = "mexSetUsernameQueryJob",
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
            writer.writeName("reserved");
            writer.writeColon();
            if (reserved != null) {
                writer.writeBool(reserved);
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
            if (source != null) {
                writer.writeString(source);
            } else {
                writer.writeNull();
            }
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
