package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Decoded reply to the update-text-status mutation.
 *
 * <p>Consumed after dispatching {@link UpdateTextStatusMexRequest}. The reply wraps the
 * {@code xwa2_update_text_status.result} status token; the success signal is the boolean derived
 * from {@code result == "SUCCESS"}, exposed through {@link #isSuccess()}, while the raw token is
 * kept on {@link #result()} so callers may distinguish among the relay's error tokens.
 *
 * @see UpdateTextStatusMexRequest
 */
@WhatsAppWebModule(moduleName = "WAWebMexUpdateTextStatusJob")
public final class UpdateTextStatusMexResponse implements MexStanza.Response.Json {
    /**
     * The {@code result} field carrying the relay's status token, possibly {@code null}.
     */
    private final String result;

    /**
     * Wraps the decoded relay status token.
     *
     * @param result the {@code result} field
     */
    private UpdateTextStatusMexResponse(String result) {
        this.result = result;
    }

    /**
     * Decodes the {@code <result>} child of an inbound MEX IQ.
     *
     * <p>The argument is the IQ stanza received in reply to a stanza dispatched with
     * {@link UpdateTextStatusMexRequest#toStanza()}.
     *
     * @param stanza the IQ reply stanza
     * @return the decoded reply, or {@link Optional#empty()} when the payload is missing or
     *         malformed
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUpdateTextStatusJob", exports = "mexUpdateTextStatus",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<UpdateTextStatusMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(UpdateTextStatusMexResponse::of);
    }

    /**
     * Returns the raw status token.
     *
     * <p>The token is preserved so callers may distinguish among the relay's error tokens;
     * {@link #isSuccess()} exposes the success/failure boolean.
     *
     * @return the token wrapped in an {@link Optional}, or {@link Optional#empty()} when the relay
     *         omitted the field
     */
    public Optional<String> result() {
        return Optional.ofNullable(result);
    }

    /**
     * Returns whether the mutation succeeded.
     *
     * <p>The result is {@code true} only when the relay's {@code xwa2_update_text_status.result}
     * token equals {@code "SUCCESS"}.
     *
     * @return {@code true} when {@link #result()} equals {@code "SUCCESS"}, {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUpdateTextStatusJob", exports = "mexUpdateTextStatus",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean isSuccess() {
        return Objects.equals(result, "SUCCESS");
    }

    /**
     * Decodes the {@code <result>} payload bytes into a {@link UpdateTextStatusMexResponse}.
     *
     * @implNote This implementation projects {@code data.xwa2_update_text_status.result}; missing
     * intermediate envelopes yield {@link Optional#empty()}.
     *
     * @param json the raw {@code <result>} payload bytes
     * @return the decoded reply, or {@link Optional#empty()} when the payload does not parse or
     *         lacks the required envelope
     */
    private static Optional<UpdateTextStatusMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_update_text_status");
        if (root == null) {
            return Optional.empty();
        }

        var result = root.getString("result");

        return Optional.of(new UpdateTextStatusMexResponse(result));
    }
}
