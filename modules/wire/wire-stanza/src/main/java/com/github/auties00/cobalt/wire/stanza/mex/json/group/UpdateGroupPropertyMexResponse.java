package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Inbound parsed response of the {@link UpdateGroupPropertyMexRequest} mutation, exposing the
 * affected group id and the resulting group state echoed back by the relay.
 *
 * <p>The {@code state} scalar is the post-mutation lifecycle status; any value other than
 * {@code "ACTIVE"} indicates the relay rejected the mutation because the group is no longer in a
 * writable state.
 *
 * @implNote This implementation does not reject non-{@code ACTIVE} states inside the parser, in
 * contrast to WA Web which raises a server-status error of 405; the choice is deliberate per
 * Cobalt's configurable error model, and callers may compare {@link #state()} against
 * {@code "ACTIVE"} explicitly.
 */
@WhatsAppWebModule(moduleName = "WAWebMexUpdateGroupPropertyJob")
public final class UpdateGroupPropertyMexResponse implements MexStanza.Response.Json {
    /**
     * Group id scalar projected from {@code data.xwa2_group_update_property.id}, echoed back by the
     * relay.
     */
    private final String id;

    /**
     * Post-mutation group state scalar projected from {@code data.xwa2_group_update_property.state};
     * expected to be {@code "ACTIVE"} on success.
     */
    private final String state;

    /**
     * Constructs a new response wrapping the parsed scalar fields.
     *
     * <p>Instances are produced by the {@link #of(Stanza)} parser.
     *
     * @param id    the echoed group id, or {@code null} if absent
     * @param state the post-mutation group state, or {@code null} if absent
     */
    private UpdateGroupPropertyMexResponse(String id, String state) {
        this.id = id;
        this.state = state;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>This is the entry point for receivers handling the IQ reply of
     * {@link UpdateGroupPropertyMexRequest}. The returned value is {@link Optional#empty()} when the
     * reply lacks a {@code <result>} child or its JSON body cannot be parsed into the expected
     * envelope.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return the parsed response, or {@link Optional#empty()} if the expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUpdateGroupPropertyJob", exports = "mexUpdateGroupPropertyJob",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<UpdateGroupPropertyMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(UpdateGroupPropertyMexResponse::of);
    }

    /**
     * Returns the group id scalar echoed back by the relay.
     *
     * <p>This mirrors the {@code group_id} variable sent in {@link UpdateGroupPropertyMexRequest}; it
     * is useful when correlating the reply against a batched dispatch.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the post-mutation group state scalar.
     *
     * <p>The value is expected to be {@code "ACTIVE"} on success; values such as
     * {@code "NON_EXISTENT"} or {@code "SUSPENDED"} indicate the relay rejected the mutation because
     * the group is no longer in a writable state.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<String> state() {
        return Optional.ofNullable(state);
    }

    /**
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link UpdateGroupPropertyMexResponse}.
     *
     * <p>The {@code data.xwa2_group_update_property} envelope is walked, returning
     * {@link Optional#empty()} when any intermediate object is missing.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} containing the parsed response, or empty if the
     *         {@code data.xwa2_group_update_property} envelope is absent
     */
    private static Optional<UpdateGroupPropertyMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_group_update_property");
        if (root == null) {
            return Optional.empty();
        }

        var id = root.getString("id");
        var state = root.getString("state");

        return Optional.of(new UpdateGroupPropertyMexResponse(id, state));
    }
}
