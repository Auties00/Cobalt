package com.github.auties00.cobalt.wire.stanza.mex.json.bot;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.time.Instant;
import java.util.Optional;

/**
 * Inbound parsed response of the {@link FetchBotCertificateRevocationListMexRequest} query, exposing
 * the {@code xwa2_fetch_feature_pki_crl} envelope returned by the relay.
 *
 * <p>The envelope carries the base64-encoded DER certificate revocation list for the
 * {@code whatsapp_simple_signal} PKI together with its {@code next_update} watermark. The
 * bot-certificate revocation service decodes the CRL into its revoked serial numbers and uses the
 * watermark to decide when the list is stale and must be refetched.
 *
 * @implNote This implementation projects the two relay scalars verbatim and adapts the
 * seconds-since-epoch {@code next_update} into a JDK {@link Instant}, leaving the base64 decode and the
 * DER parse to the consuming service. The relay nests the payload under a {@code data} envelope on the
 * GraphQL transport but delivers it at the root on some paths, so the parser accepts both shapes.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchBotCertificateRevocationList")
public final class FetchBotCertificateRevocationListMexResponse implements MexStanza.Response.Json {
    /**
     * The relay key wrapping the CRL payload.
     */
    private static final String CRL_ROOT = "xwa2_fetch_feature_pki_crl";

    /**
     * Base64-encoded DER CRL scalar projected from {@code xwa2_fetch_feature_pki_crl.crl}, or
     * {@code null} when absent.
     */
    private final String crl;

    /**
     * The {@code next_update} watermark in seconds since epoch projected from
     * {@code xwa2_fetch_feature_pki_crl.next_update}, or {@code null} when the relay omits it.
     */
    private final Long nextUpdate;

    /**
     * Constructs a new response wrapping the parsed CRL payload and next-update watermark.
     *
     * <p>Instances are produced by the {@link #of(Stanza)} parser.
     *
     * @param crl        the base64-encoded DER CRL scalar
     * @param nextUpdate the {@code next_update} watermark in seconds since epoch
     */
    private FetchBotCertificateRevocationListMexResponse(String crl, Long nextUpdate) {
        this.crl = crl;
        this.nextUpdate = nextUpdate;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>This is the entry point for the revocation service handling the IQ reply of
     * {@link FetchBotCertificateRevocationListMexRequest}. The returned value is
     * {@link Optional#empty()} when the reply lacks a {@code <result>} child or its JSON body does not
     * carry the {@code xwa2_fetch_feature_pki_crl} envelope.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return the parsed response, or {@link Optional#empty()} if the expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchBotCertificateRevocationList", exports = "mexFetchBotCertificateRevocationList",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchBotCertificateRevocationListMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchBotCertificateRevocationListMexResponse::of);
    }

    /**
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link FetchBotCertificateRevocationListMexResponse}.
     *
     * <p>The {@code xwa2_fetch_feature_pki_crl} envelope is read from under the GraphQL {@code data}
     * wrapper when present and from the root otherwise, returning {@link Optional#empty()} when the
     * envelope is absent.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} containing the parsed response, or empty if the
     *         {@code xwa2_fetch_feature_pki_crl} envelope is absent
     */
    private static Optional<FetchBotCertificateRevocationListMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        var root = (data != null ? data : jsonObject).getJSONObject(CRL_ROOT);
        if (root == null) {
            return Optional.empty();
        }

        var crl = root.getString("crl");
        var nextUpdate = root.getLong("next_update");
        return Optional.of(new FetchBotCertificateRevocationListMexResponse(crl, nextUpdate));
    }

    /**
     * Returns the base64-encoded DER certificate revocation list scalar.
     *
     * <p>The value is the raw relay scalar; the consuming service base64-decodes it and parses the
     * resulting DER with the platform {@link java.security.cert.CertificateFactory}.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<String> crl() {
        return Optional.ofNullable(crl);
    }

    /**
     * Returns the next-update watermark.
     *
     * <p>The relay's seconds-since-epoch value is adapted into a JDK {@link Instant}; an absent
     * watermark leaves the consuming service to apply its own default horizon.
     *
     * @return an {@link Optional} containing the value as an {@link Instant}, or empty if absent
     */
    public Optional<Instant> nextUpdate() {
        return Optional.ofNullable(nextUpdate).map(Instant::ofEpochSecond);
    }
}
