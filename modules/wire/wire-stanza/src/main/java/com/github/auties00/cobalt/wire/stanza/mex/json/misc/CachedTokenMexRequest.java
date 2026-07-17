package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

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
 * Builds the MEX mutation that trades a freshly minted RSA public key for a batch of
 * canonical-nonce-encrypted access tokens.
 *
 * <p>This mutation backs the cached-nonce leg of the account-linking canonical-token pipeline. The
 * caller generates an RSA key pair, encodes the PEM-armoured public key as base64 and forwards it as
 * the {@code client_pub_key} field alongside a fresh {@code request_id}; the relay encrypts the
 * access-token payload against that public key and returns it as the {@code encrypted_access_tokens}
 * RSA-with-AES bundle parsed by {@link CachedTokenMexResponse}. Both fields are nested under the
 * single {@code input} GraphQL variable.
 *
 * @see CachedTokenMexResponse
 */
@WhatsAppWebModule(moduleName = "WAWebMexCachedTokenJob")
public final class CachedTokenMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of the {@code WAWebMexCachedTokenJobMutation}
     * document.
     *
     * <p>The relay maps this identifier to its persisted operation; it is emitted as the
     * {@code query_id} attribute of the outgoing {@code <query>} child and the GraphQL text is never
     * sent on the wire.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCachedTokenJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "27013462064904056";

    /**
     * Holds the GraphQL operation name reported alongside this mutation when it is dispatched.
     *
     * <p>The name tags the operation in latency and error metrics; it is kept on the request so
     * embedders mirroring that telemetry surface can emit the same tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCachedTokenJobMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebMexCachedTokenJobMutation";

    /**
     * Holds the {@code client_pub_key} field of the {@code input} object: the PEM-armoured RSA
     * public key, base64-encoded, against which the relay encrypts the access-token payload.
     */
    private final String clientPubKey;

    /**
     * Holds the {@code request_id} field of the {@code input} object: a fresh per-request identifier
     * the relay echoes for correlation.
     */
    private final String requestId;

    /**
     * Constructs a cached-token mutation request carrying the client public key and request id.
     *
     * <p>The {@code clientPubKey} is the base64 encoding of the caller's PEM-armoured RSA public key;
     * the relay encrypts the access-token payload against it so only the matching private key can
     * decrypt the reply. The {@code requestId} is a fresh per-request correlation identifier. Either
     * field whose value is {@code null} is omitted from the nested {@code input} object.
     *
     * @param clientPubKey the base64-encoded PEM RSA public key, or {@code null} to omit the field
     * @param requestId    the per-request correlation identifier, or {@code null} to omit the field
     */
    public CachedTokenMexRequest(String clientPubKey, String requestId) {
        this.clientPubKey = clientPubKey;
        this.requestId = requestId;
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
     * {@link JSONWriter}, nesting the {@code client_pub_key} and {@code request_id} fields under a
     * single {@code input} object and emitting each field only when its corresponding constructor
     * argument is non-null. The wrapped envelope is built through
     * {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCachedTokenJob", exports = "fetchCachedNonceToken",
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
            if (clientPubKey != null) {
                writer.writeName("client_pub_key");
                writer.writeColon();
                writer.writeString(clientPubKey);
            }
            if (requestId != null) {
                writer.writeName("request_id");
                writer.writeColon();
                writer.writeString(requestId);
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
