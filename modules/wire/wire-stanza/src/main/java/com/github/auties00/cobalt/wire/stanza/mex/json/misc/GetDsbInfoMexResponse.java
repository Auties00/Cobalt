package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Holds the parsed response of the {@link GetDsbInfoMexRequest} mutation.
 *
 * <p>The response carries the relay-generated reference number from the {@code xwa2_get_dsb_info}
 * envelope, exposed through {@link #referenceNumber()}. That number is the user-visible token shown in
 * the data-subject-request confirmation surface, which the user can quote when checking on the
 * request's processing state.
 *
 * @implNote This implementation surfaces the lone {@code reference_number} scalar as an
 * {@link Optional} so absence stays observable, whereas WhatsApp Web reads
 * {@code n.xwa2_get_dsb_info.reference_number} directly and throws when the envelope is {@code null}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexGetDsbInfoJob")
public final class GetDsbInfoMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the relay-generated {@code reference_number} scalar the user can quote to track the
     * data-subject request, or {@code null} when the relay omitted it.
     */
    private final String referenceNumber;

    /**
     * Constructs a new response wrapping the {@code reference_number} scalar.
     *
     * <p>Instances are produced exclusively by the {@link #of(Stanza)} parser.
     *
     * @param referenceNumber the relay-generated reference number, may be {@code null}
     */
    private GetDsbInfoMexResponse(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>Reads the {@code <result>} child's byte content through {@link Stanza#getChild(String)} and
     * {@link Stanza#toContentBytes()}, then routes it through the private byte-level parser. The result
     * is {@link Optional#empty()} when the stanza carries no result or when the
     * {@code data.xwa2_get_dsb_info} envelope is absent; WhatsApp Web's wrapper raises a
     * {@code ServerStatusCodeError(500)} in the same situation.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetDsbInfoJob", exports = "mexGetDsbInfo",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<GetDsbInfoMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(GetDsbInfoMexResponse::of);
    }

    /**
     * Returns the relay-generated reference number.
     *
     * @return an {@link Optional} containing the reference number, or {@link Optional#empty()} if the
     *         relay omitted the scalar
     */
    public Optional<String> referenceNumber() {
        return Optional.ofNullable(referenceNumber);
    }

    /**
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link GetDsbInfoMexResponse}.
     *
     * <p>Routed through {@link #of(Stanza)} after the byte content of the {@code <result>} child is
     * extracted. The result is {@link Optional#empty()} when the envelope, the {@code data} branch, or
     * the {@code xwa2_get_dsb_info} child is absent.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} wrapping the parsed response, or {@link Optional#empty()} if the
     *         {@code data.xwa2_get_dsb_info} envelope is absent
     */
    private static Optional<GetDsbInfoMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_get_dsb_info");
        if (root == null) {
            return Optional.empty();
        }

        var referenceNumber = root.getString("reference_number");

        return Optional.of(new GetDsbInfoMexResponse(referenceNumber));
    }
}
