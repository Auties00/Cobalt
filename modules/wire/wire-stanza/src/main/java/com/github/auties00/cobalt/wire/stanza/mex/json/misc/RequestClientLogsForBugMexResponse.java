package com.github.auties00.cobalt.wire.stanza.mex.json.misc;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Parses the MEX response of the request-client-logs-for-bug mutation built by
 * {@link RequestClientLogsForBugMexRequest}.
 *
 * <p>Projects the single {@code data.xwa2_request_client_logs_for_bug} boolean scalar reporting
 * whether the relay accepted the log-collection request. A parsed instance always carries a defined
 * acceptance flag; the envelope itself being absent yields {@link Optional#empty()} from the parser.
 *
 * @see RequestClientLogsForBugMexRequest
 *
 * @deprecated not wired: bug-report peer-log solicitation has no headless surface.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WAWebMexRequestClientLogsForBugJob")
public final class RequestClientLogsForBugMexResponse implements MexStanza.Response.Json {
    /**
     * Holds whether the relay accepted the log-collection request.
     */
    private final boolean accepted;

    /**
     * Constructs a response wrapping the relay's acceptance flag.
     *
     * <p>Instances are produced only by the {@link #of(Stanza)} parser.
     *
     * @param accepted whether the relay accepted the log-collection request
     */
    private RequestClientLogsForBugMexResponse(boolean accepted) {
        this.accepted = accepted;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_request_client_logs_for_bug} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result payload
     */
    @WhatsAppWebExport(moduleName = "WAWebMexRequestClientLogsForBugJob", exports = "requestClientLogsForBugJob",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<RequestClientLogsForBugMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(RequestClientLogsForBugMexResponse::of);
    }

    /**
     * Returns whether the relay accepted the log-collection request.
     *
     * @return {@code true} when the relay reported acceptance, {@code false} when it reported
     *         refusal or omitted the scalar
     */
    public boolean accepted() {
        return accepted;
    }

    /**
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * <p>Reserved for the public {@link #of(Stanza)} overload.
     *
     * @implNote This implementation guards every nested lookup so a malformed envelope produces
     * {@link Optional#empty()} rather than a parser exception, and coerces a missing or non-boolean
     * scalar to {@code false} to mirror the {@code f !== true} acceptance check in the WhatsApp Web
     * dispatcher.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_request_client_logs_for_bug} root
     */
    private static Optional<RequestClientLogsForBugMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        if (!data.containsKey("xwa2_request_client_logs_for_bug")) {
            return Optional.empty();
        }

        var accepted = data.getBooleanValue("xwa2_request_client_logs_for_bug");
        return Optional.of(new RequestClientLogsForBugMexResponse(accepted));
    }
}
