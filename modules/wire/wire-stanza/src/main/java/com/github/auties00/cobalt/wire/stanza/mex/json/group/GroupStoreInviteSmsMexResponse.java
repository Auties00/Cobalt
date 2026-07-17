package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Parses the MEX response of the group-store-invite-SMS mutation built by
 * {@link GroupStoreInviteSmsMexRequest}.
 *
 * <p>Projects the {@code data.xwa2_group_store_invites_sms} envelope, exposing the echoed
 * {@code group_jid} and the per-participant {@link ParticipantResponse} list. Each participant
 * response carries an {@code error_code} that is absent (or zero) on success and set to a
 * relay-defined failure code otherwise; the list is positionally aligned with the participant list
 * sent in the request.
 *
 * @see GroupStoreInviteSmsMexRequest
 */
@WhatsAppWebModule(moduleName = "WAWebMexGroupStoreInviteSmsJob")
public final class GroupStoreInviteSmsMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the echoed {@code group_jid} scalar identifying the group the invites targeted.
     */
    private final String groupJid;

    /**
     * Holds the per-participant responses projected from {@code participant_responses}.
     */
    private final List<ParticipantResponse> participantResponses;

    /**
     * Constructs a response wrapping the echoed group Jid and the per-participant responses.
     *
     * <p>Instances are produced only by the {@link #of(Stanza)} parser.
     *
     * @param groupJid             the echoed group Jid, may be {@code null}
     * @param participantResponses the per-participant responses, never {@code null}
     */
    private GroupStoreInviteSmsMexResponse(String groupJid, List<ParticipantResponse> participantResponses) {
        this.groupJid = groupJid;
        this.participantResponses = participantResponses;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_group_store_invites_sms} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result payload
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGroupStoreInviteSmsJob", exports = "mexGroupStoreInviteSms",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<GroupStoreInviteSmsMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(GroupStoreInviteSmsMexResponse::of);
    }

    /**
     * Returns the echoed group Jid the invites targeted.
     *
     * @return the group Jid, or empty when the relay omitted the field
     */
    public Optional<String> groupJid() {
        return Optional.ofNullable(groupJid);
    }

    /**
     * Returns the per-participant responses, positionally aligned with the request's participant
     * list.
     *
     * @return the parsed responses, never {@code null}; empty when the relay returned none
     */
    public List<ParticipantResponse> participantResponses() {
        return participantResponses;
    }

    /**
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * <p>Reserved for the public {@link #of(Stanza)} overload.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_group_store_invites_sms} root
     */
    private static Optional<GroupStoreInviteSmsMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_group_store_invites_sms");
        if (root == null) {
            return Optional.empty();
        }

        var groupJid = root.getString("group_jid");
        var participantResponses = ParticipantResponse.ofArray(root.getJSONArray("participant_responses"));
        return Optional.of(new GroupStoreInviteSmsMexResponse(groupJid, participantResponses));
    }

    /**
     * Wraps one {@code participant_responses} entry: the per-participant outcome of the SMS invite.
     *
     * <p>Carries a single {@code error_code} scalar that is absent (or zero) on success and set to a
     * relay-defined failure code otherwise.
     */
    public static final class ParticipantResponse {
        /**
         * Holds the {@code error_code} scalar: the relay-defined failure code for this participant,
         * or {@code null} when the invite succeeded.
         */
        private final Long errorCode;

        /**
         * Constructs a participant response from the parsed error code.
         *
         * <p>Reserved for the static parser.
         *
         * @param errorCode the relay-defined failure code, may be {@code null}
         */
        private ParticipantResponse(Long errorCode) {
            this.errorCode = errorCode;
        }

        /**
         * Returns the relay-defined failure code for this participant.
         *
         * @return the error code, or empty when the relay omitted the field (typically on success)
         */
        public OptionalLong errorCode() {
            return errorCode != null ? OptionalLong.of(errorCode) : OptionalLong.empty();
        }

        /**
         * Parses a {@link ParticipantResponse} from the given JSON object.
         *
         * <p>Used by {@link #ofArray(JSONArray)} when walking the {@code participant_responses}
         * array.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link ParticipantResponse}, or empty when {@code obj} is {@code null}
         */
        static Optional<ParticipantResponse> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var errorCode = obj.getLong("error_code");
            return Optional.of(new ParticipantResponse(errorCode));
        }

        /**
         * Parses a list of {@link ParticipantResponse} entries from the given JSON array.
         *
         * <p>Walks every element through {@link #of(JSONObject)}; {@code null} entries inside the
         * array are skipped. A {@code null} array collapses to {@link List#of()}.
         *
         * @param arr the JSON array to parse
         * @return an unmodifiable list of parsed entries, empty when {@code arr} is {@code null}
         */
        static List<ParticipantResponse> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<ParticipantResponse>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }
}
