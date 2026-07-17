package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Holds the parsed response of the {@link IntegrityChallengeResponseMexRequest} mutation.
 *
 * <p>The response carries the {@code success} and {@code error_message} scalars from the
 * {@code xwa2_submit_integrity_challenge_response} envelope, exposed through {@link #success()} and
 * {@link #errorMessage()}. A truthy {@link #success()} means the relay accepted the submitted
 * challenge, allowing a caller to close its integrity-challenge surface and drop the cached challenge;
 * otherwise the {@link #errorMessage()} explains the rejection.
 *
 * @implNote This implementation surfaces both scalars as {@link Optional} containers so absence stays
 * observable, letting callers distinguish a relay outage from an explicit rejection, whereas WhatsApp
 * Web reads {@code n.success} directly and treats a missing value as falsy.
 */
@WhatsAppWebModule(moduleName = "WAWebMexIntegrityChallengeResponse")
public final class IntegrityChallengeResponseMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the {@code success} scalar reflecting the relay's verdict on the submitted challenge, or
     * {@code null} when the relay omitted it.
     */
    private final Boolean success;

    /**
     * Holds the {@code error_message} scalar populated when the challenge was rejected, or
     * {@code null} when the relay omitted it.
     */
    private final String errorMessage;

    /**
     * Constructs a new response wrapping the parsed scalar fields of the
     * {@code xwa2_submit_integrity_challenge_response} envelope.
     *
     * <p>Instances are produced exclusively by the {@link #of(Stanza)} parser.
     *
     * @param success      the {@code success} scalar, may be {@code null}
     * @param errorMessage the {@code error_message} scalar, may be {@code null}
     */
    private IntegrityChallengeResponseMexResponse(Boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>Reads the {@code <result>} child's byte content through {@link Stanza#getChild(String)} and
     * {@link Stanza#toContentBytes()}, then routes it through the private byte-level parser. The result
     * is {@link Optional#empty()} when the stanza carries no result or when the
     * {@code data.xwa2_submit_integrity_challenge_response} envelope is absent.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexIntegrityChallengeResponse", exports = "mexSubmitPasskeyChallengeResponse",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Optional<IntegrityChallengeResponseMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(IntegrityChallengeResponseMexResponse::of);
    }

    /**
     * Returns the relay's verdict on the submitted challenge.
     *
     * @return an {@link Optional} containing the success flag, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<Boolean> success() {
        return Optional.ofNullable(success);
    }

    /**
     * Returns the error message reported by the relay when the challenge was rejected.
     *
     * @return an {@link Optional} containing the error message, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    /**
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link IntegrityChallengeResponseMexResponse}.
     *
     * <p>Routed through {@link #of(Stanza)} after the byte content of the {@code <result>} child is
     * extracted. The result is {@link Optional#empty()} when the envelope, the {@code data} branch, or
     * the {@code xwa2_submit_integrity_challenge_response} child is absent.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         {@code data.xwa2_submit_integrity_challenge_response} envelope is absent
     */
    private static Optional<IntegrityChallengeResponseMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_submit_integrity_challenge_response");
        if (root == null) {
            return Optional.empty();
        }

        var success = root.getBoolean("success");
        var errorMessage = root.getString("error_message");

        return Optional.of(new IntegrityChallengeResponseMexResponse(success, errorMessage));
    }
}
