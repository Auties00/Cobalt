package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Inbound parsed response of the {@link FetchGroupIsInternalMexRequest} query, exposing whether the
 * queried group is flagged as internal by the relay.
 *
 * <p>The flag drives the staff-only indicator badge rendered on Meta-internal testing groups. It
 * lives under the {@code properties.internal} scalar of every group inline-fragment variant.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchGroupIsInternalJob")
public final class FetchGroupIsInternalMexResponse implements MexStanza.Response.Json {
    /**
     * Internal-flag scalar projected from
     * {@code data.xwa2_group_query_by_id.properties.internal}.
     */
    private final boolean internal;

    /**
     * Constructs a new response wrapping the parsed boolean internal-flag scalar.
     *
     * <p>Instances are produced by the {@link #of(Stanza)} parser.
     *
     * @param internal whether the relay reports the group as internal
     */
    private FetchGroupIsInternalMexResponse(boolean internal) {
        this.internal = internal;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>This is the entry point for receivers handling the IQ reply of
     * {@link FetchGroupIsInternalMexRequest}. The returned value is {@link Optional#empty()} when the
     * reply lacks a {@code <result>} child or its JSON body cannot be parsed into the expected
     * envelope.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return the parsed response, or {@link Optional#empty()} if the expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupIsInternalJob", exports = "mexFetchGroupIsInternal",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchGroupIsInternalMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchGroupIsInternalMexResponse::of);
    }

    /**
     * Returns whether the queried group is flagged as internal by the relay.
     *
     * <p>The result is {@code true} only when the relay populated {@code properties.internal} with a
     * JSON {@code true} literal; the missing-property and JSON-{@code false} cases both yield
     * {@code false}.
     *
     * @return {@code true} if the relay reports the group as internal, {@code false} otherwise
     */
    public boolean isInternal() {
        return internal;
    }

    /**
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link FetchGroupIsInternalMexResponse}.
     *
     * <p>The missing-envelope, missing-{@code properties} and missing-{@code internal} cases all
     * collapse to a {@code false} verdict; only a JSON {@code true} literal under
     * {@code properties.internal} yields {@code true}.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} containing the parsed response, or empty if the
     *         {@code data.xwa2_group_query_by_id} envelope is absent
     */
    private static Optional<FetchGroupIsInternalMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_group_query_by_id");
        if (root == null) {
            return Optional.empty();
        }

        var properties = root.getJSONObject("properties");
        if (properties == null) {
            return Optional.of(new FetchGroupIsInternalMexResponse(false));
        }

        var internal = Boolean.TRUE.equals(properties.getBoolean("internal"));

        return Optional.of(new FetchGroupIsInternalMexResponse(internal));
    }
}
