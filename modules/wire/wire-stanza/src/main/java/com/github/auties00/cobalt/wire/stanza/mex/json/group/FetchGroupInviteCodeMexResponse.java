package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Inbound parsed response of the {@link FetchGroupInviteCodeMexRequest} query, exposing the current
 * invite code scalar and the echoed group identifier.
 *
 * <p>The invite code is the opaque suffix of the shareable {@code chat.whatsapp.com/<code>} URL.
 *
 * @implNote This implementation surfaces a {@code null} invite code as an empty {@link Optional} on
 * {@link #inviteCode()} rather than throwing, in contrast to WA Web which raises a MEX error when the
 * relay omits the field; the caller decides how to react to the missing-code case, in line with
 * Cobalt's configurable error model.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchGroupInviteCodeJob")
public final class FetchGroupInviteCodeMexResponse implements MexStanza.Response.Json {
    /**
     * Current invite code scalar projected from
     * {@code data.xwa2_group_query_by_id.invite_code}.
     */
    private final String inviteCode;

    /**
     * Group identifier scalar projected from {@code data.xwa2_group_query_by_id.id}, echoed back by
     * the relay.
     */
    private final String id;

    /**
     * Constructs a new response wrapping the parsed scalar fields.
     *
     * <p>Instances are produced by the {@link #of(Stanza)} parser.
     *
     * @param inviteCode the current invite code, or {@code null} if absent
     * @param id         the echoed group identifier, or {@code null} if absent
     */
    private FetchGroupInviteCodeMexResponse(String inviteCode, String id) {
        this.inviteCode = inviteCode;
        this.id = id;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>This is the entry point for receivers handling the IQ reply of
     * {@link FetchGroupInviteCodeMexRequest}. The returned value is {@link Optional#empty()} when the
     * reply lacks a {@code <result>} child or its JSON body cannot be parsed into the expected
     * envelope.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return the parsed response, or {@link Optional#empty()} if the expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupInviteCodeJob", exports = "fetchMexGroupInviteCode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchGroupInviteCodeMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchGroupInviteCodeMexResponse::of);
    }

    /**
     * Returns the current invite code scalar.
     *
     * <p>The value is the opaque suffix of the shareable {@code chat.whatsapp.com/<code>} URL; it is
     * absent when the relay omitted the field, for example because the group has not yet been issued
     * an invite code.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<String> inviteCode() {
        return Optional.ofNullable(inviteCode);
    }

    /**
     * Returns the echoed group identifier scalar.
     *
     * <p>This mirrors the {@code id} variable sent in {@link FetchGroupInviteCodeMexRequest}; it is
     * useful when correlating the reply against a batched dispatch.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link FetchGroupInviteCodeMexResponse}.
     *
     * <p>The {@code data.xwa2_group_query_by_id} envelope is walked, returning
     * {@link Optional#empty()} when any intermediate object is missing.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} containing the parsed response, or empty if the
     *         {@code data.xwa2_group_query_by_id} envelope is absent
     */
    private static Optional<FetchGroupInviteCodeMexResponse> of(byte[] json) {
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

        var inviteCode = root.getString("invite_code");
        var id = root.getString("id");

        return Optional.of(new FetchGroupInviteCodeMexResponse(inviteCode, id));
    }
}
