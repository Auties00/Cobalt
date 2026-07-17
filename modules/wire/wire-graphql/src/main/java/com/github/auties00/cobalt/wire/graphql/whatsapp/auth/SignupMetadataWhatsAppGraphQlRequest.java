package com.github.auties00.cobalt.wire.graphql.whatsapp.auth;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the graph.whatsapp.com GraphQL query that fetches the onboarding metadata for a WhatsApp Business signup flow.
 *
 * <p>The query takes two top-level scalar GraphQL variables: a {@code signup_id} identifying the
 * in-progress signup and a {@code phone_number} the signup is being attached to. WhatsApp Web's
 * {@code WAWebSignupMetadataFetcher.fetchSignupMetadata(signup_id, phone_number)} forwards both
 * straight through to the graph.whatsapp.com endpoint, then projects the reply into the consent copy shown during signup.
 * The graph.whatsapp.com endpoint returns the signup metadata under the linked {@code wa_signup_metadata} field; the reply
 * is consumed through {@link SignupMetadataWhatsAppGraphQlResponse}.
 *
 * @see SignupMetadataWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebSignupMetadataQuery")
public final class SignupMetadataWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebSignupMetadataQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26378108788468347";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebSignupMetadataQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebSignupMetadataQuery";

    /**
     * The {@code signup_id} GraphQL variable identifying the in-progress signup, or {@code null} to
     * omit it.
     */
    private final String signupId;

    /**
     * The {@code phone_number} GraphQL variable the signup is being attached to, or {@code null} to
     * omit it.
     *
     * <p>This is the raw signup phone-number string the server keys the metadata by, not a WhatsApp
     * address, so it is carried as a plain {@link String} rather than a
     * {@link com.github.auties00.cobalt.wire.core.jid.Jid}.
     */
    private final String phoneNumber;

    /**
     * Constructs a signup-metadata query request carrying the signup id and phone number.
     *
     * <p>Both values populate the GraphQL variables object; each value that is {@code null} is
     * omitted from the serialized object.
     *
     * @param signupId    the in-progress signup identifier, or {@code null} to omit the variable
     * @param phoneNumber the phone number the signup is attached to, or {@code null} to omit the
     *                    variable
     */
    public SignupMetadataWhatsAppGraphQlRequest(String signupId, String phoneNumber) {
        this.signupId = signupId;
        this.phoneNumber = phoneNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
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
     * @implNote This implementation emits {@code {"signup_id": <signupId>, "phone_number":
     * <phoneNumber>}}, writing each variable only when its value is non-null and emitting {@code "{}"}
     * when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSignupMetadataFetcher", exports = "fetchSignupMetadata",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (signupId != null) {
                writer.writeName("signup_id");
                writer.writeColon();
                writer.writeString(signupId);
            }

            if (phoneNumber != null) {
                writer.writeName("phone_number");
                writer.writeColon();
                writer.writeString(phoneNumber);
            }
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WhatsAppGraphQlEnvironment environment() {
        return WhatsAppGraphQlEnvironment.WWW;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}
