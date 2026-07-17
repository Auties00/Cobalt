package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Objects;

/**
 * Submits a passkey-signed integrity challenge response and reports whether the relay accepted it.
 *
 * <p>The mutation forwards a WebAuthn assertion that answers an integrity challenge; the relay's
 * verdict is surfaced through {@link IntegrityChallengeResponseMexResponse#success()}. On acceptance,
 * a caller is expected to close its integrity-challenge surface and drop any cached challenge; on
 * rejection, a caller applies its own surface and cache logic.
 *
 * @implNote This implementation base64-encodes the raw JSON-serialised WebAuthn assertion using
 * {@link Base64#getEncoder()}, mirroring WhatsApp Web's {@code btoa(JSON.stringify(e))} call. The
 * {@code prf_available} flag is taken from the caller because Cobalt's codegen has no access to the
 * original assertion object that WhatsApp Web inspects through {@code e.prf_output != null}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexIntegrityChallengeResponse")
public final class IntegrityChallengeResponseMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled GraphQL query identifier for the
     * {@code WAWebMexIntegrityChallengeResponseMutation} document.
     *
     * <p>The relay maps this identifier to a server-side persisted mutation and never sees the GraphQL
     * text on the wire.
     */
    public static final String QUERY_ID = "26230331493320650";

    /**
     * Holds the GraphQL operation name carried by this mutation.
     *
     * <p>WhatsApp Web's MEX perf tracker uses the name to tag the query in latency and error metrics;
     * Cobalt keeps it on the request for embedders mirroring that telemetry surface.
     */
    public static final String OPERATION_NAME = "mexSubmitPasskeyChallengeResponse";

    /**
     * Holds the {@code challenge_type} discriminator emitted on every request.
     *
     * <p>The value is always {@code PASSKEY}; no other challenge type is currently supported.
     */
    String CHALLENGE_TYPE = "PASSKEY";

    /**
     * Holds the raw bytes of the JSON-serialised WebAuthn assertion, base64-encoded inline during
     * dispatch.
     */
    private final byte[] signedChallenge;

    /**
     * Indicates whether the assertion carries a {@code prf_output} field.
     *
     * <p>WhatsApp Web derives this flag inline via {@code e.prf_output != null}; Cobalt callers
     * compute the same predicate against their own assertion object and pass the result.
     */
    private final boolean prfAvailable;

    /**
     * Constructs a new request submitting the given passkey-signed challenge response to the relay.
     *
     * <p>The {@code signedChallenge} must already be the JSON-serialised form of the WebAuthn
     * assertion; WhatsApp Web invokes {@code JSON.stringify(e)} before {@code btoa}, so callers pass
     * the equivalent serialised bytes. The {@code prfAvailable} flag must reflect whether the
     * assertion carries a non-{@code null} {@code prf_output} field, matching the relay's
     * per-assertion telemetry expectation.
     *
     * @param signedChallenge the raw bytes of the JSON-serialised WebAuthn assertion, must not be
     *                        {@code null}
     * @param prfAvailable    whether the assertion carries a {@code prf_output} field
     * @throws NullPointerException if {@code signedChallenge} is {@code null}
     */
    public IntegrityChallengeResponseMexRequest(byte[] signedChallenge, boolean prfAvailable) {
        this.signedChallenge = Objects.requireNonNull(signedChallenge, "signedChallenge cannot be null");
        this.prfAvailable = prfAvailable;
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
     * @implNote This implementation streams the GraphQL variables through fastjson2's
     * {@link JSONWriter}, nests the {@code passkey_response} object under {@code input}, base64-encodes
     * {@link #signedChallenge} via {@link Base64#getEncoder()}, then wraps the payload via
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexIntegrityChallengeResponse", exports = "mexSubmitPasskeyChallengeResponse",
            adaptation = WhatsAppAdaptation.DIRECT)
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

            writer.writeName("challenge_type");
            writer.writeColon();
            writer.writeString(CHALLENGE_TYPE);

            writer.writeName("passkey_response");
            writer.writeColon();
            writer.startObject();
            writer.writeName("signed_challenge");
            writer.writeColon();
            writer.writeString(Base64.getEncoder().encodeToString(signedChallenge));
            writer.writeName("prf_available");
            writer.writeColon();
            writer.writeBool(prfAvailable);
            writer.endObject();

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
