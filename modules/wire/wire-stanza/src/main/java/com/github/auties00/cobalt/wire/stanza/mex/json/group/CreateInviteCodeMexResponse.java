package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Optional;

/**
 * Inbound parsed response of the {@link CreateInviteCodeMexRequest} mutation, exposing the freshly
 * minted invite code scalar.
 *
 * <p>The relay returns this reply when an invite-code rotation succeeds. The code is the opaque
 * suffix of the resulting {@code chat.whatsapp.com/<code>} share link.
 */
@WhatsAppWebModule(moduleName = "WAWebMexCreateInviteCodeJob")
public final class CreateInviteCodeMexResponse implements MexStanza.Response.Json {
    /**
     * Freshly minted invite code scalar projected from
     * {@code data.xwa2_growth_create_invite_code.code}.
     */
    private final String code;

    /**
     * Constructs a new response wrapping the parsed {@code code} scalar.
     *
     * <p>Instances are produced by the {@link #of(Stanza)} parser.
     *
     * @param code the freshly minted invite code, or {@code null} if absent
     */
    private CreateInviteCodeMexResponse(String code) {
        this.code = code;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>This is the entry point for receivers handling the IQ reply of
     * {@link CreateInviteCodeMexRequest}. The returned value is {@link Optional#empty()} when the
     * reply lacks a {@code <result>} child or its JSON body cannot be parsed into the expected
     * envelope.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return the parsed response, or {@link Optional#empty()} if the expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexCreateInviteCodeJob", exports = "mexCreateInviteCode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<CreateInviteCodeMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(CreateInviteCodeMexResponse::of);
    }

    /**
     * Returns the freshly minted invite code scalar.
     *
     * <p>The value is the opaque suffix of the resulting {@code chat.whatsapp.com/<code>} share
     * link; it is absent when the relay omitted the field.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<String> code() {
        return Optional.ofNullable(code);
    }

    /**
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link CreateInviteCodeMexResponse}.
     *
     * <p>The {@code data.xwa2_growth_create_invite_code} envelope is walked, returning
     * {@link Optional#empty()} when any intermediate object is missing.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} containing the parsed response, or empty if the
     *         {@code data.xwa2_growth_create_invite_code} envelope is absent
     */
    private static Optional<CreateInviteCodeMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_growth_create_invite_code");
        if (root == null) {
            return Optional.empty();
        }

        var code = root.getString("code");

        return Optional.of(new CreateInviteCodeMexResponse(code));
    }
}
