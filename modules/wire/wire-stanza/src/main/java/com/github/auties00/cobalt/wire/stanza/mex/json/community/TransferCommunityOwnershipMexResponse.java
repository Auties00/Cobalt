package com.github.auties00.cobalt.wire.stanza.mex.json.community;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parsed response of the {@code mexTransferCommunityOwnershipJob} MEX mutation.
 *
 * <p>This response carries the affected community group identifier together
 * with the LID migration state projected from
 * {@code data.xwa2_group_update_users_role}. It is the inbound counterpart of
 * {@link TransferCommunityOwnershipMexRequest}; the
 * {@link LidMigrationState#addressingMode()} field reports whether the group
 * switched LID-addressing modes, which WA Web uses to decide whether to refresh
 * the group metadata after the transfer.
 *
 * @implNote This implementation models only the response shape; the downstream
 * metadata-refresh side-effect WA Web triggers on an addressing-mode change is
 * left to the caller. The {@code group_id} field returned by the relay is a
 * stringified WhatsApp WID; WA Web wraps it through a WID factory, whereas
 * Cobalt keeps it as a {@link String} and lets callers wrap it on demand.
 */
@WhatsAppWebModule(moduleName = "WAWebMexTransferCommunityOwnershipJob")
public final class TransferCommunityOwnershipMexResponse implements MexStanza.Response.Json {
    /**
     * Affected community group identifier, or {@code null} when the relay
     * omitted it.
     *
     * <p>Matches the {@code community_id} sent in the mutation input.
     */
    private final String groupId;

    /**
     * LID migration state record reported alongside the mutation result, or
     * {@code null} when the relay omitted it.
     */
    private final LidMigrationState lidMigrationState;

    /**
     * Constructs a new response with the given fields.
     *
     * @param groupId           the affected community group identifier
     * @param lidMigrationState the LID migration state record
     */
    private TransferCommunityOwnershipMexResponse(String groupId, LidMigrationState lidMigrationState) {
        this.groupId = groupId;
        this.lidMigrationState = lidMigrationState;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>This is the entry point for receivers handling
     * {@code <iq xmlns="w:mex">} replies tagged with
     * {@link TransferCommunityOwnershipMexRequest#QUERY_ID}. It unwraps the
     * {@code <result>} child, reads its content bytes and decodes the GraphQL
     * JSON envelope.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return the parsed response, or {@link Optional#empty()} if the expected
     *         JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexTransferCommunityOwnershipJob", exports = "mexTransferCommunityOwnershipJob",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<TransferCommunityOwnershipMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(TransferCommunityOwnershipMexResponse::of);
    }

    /**
     * Returns the affected community group identifier.
     *
     * <p>The value is empty when the relay omitted the {@code group_id} field.
     * Callers that need a typed WID should pass the value through their own
     * factory.
     *
     * @return an {@link Optional} containing the identifier, or empty if absent
     */
    public Optional<String> groupId() {
        return Optional.ofNullable(groupId);
    }

    /**
     * Returns the LID migration state record reported alongside the mutation
     * result.
     *
     * <p>Compare {@link LidMigrationState#addressingMode()} against the local
     * addressing-mode flag to decide whether to refresh the group metadata
     * after the transfer; WA Web only triggers the refresh when the two
     * disagree.
     *
     * @return an {@link Optional} containing the record, or empty if absent
     */
    public Optional<LidMigrationState> lidMigrationState() {
        return Optional.ofNullable(lidMigrationState);
    }

    /**
     * LID migration state record describing the group's post-mutation
     * addressing mode.
     *
     * <p>The addressing-mode tag describes whether the group is in
     * LID-addressed mode or has fallen back to legacy phone-number addressing.
     * The WA Web caller compares this tag against its locally cached
     * {@code isLidAddressingMode} flag to detect a mode change.
     */
    public static final class LidMigrationState {
        /**
         * Addressing mode tag for the affected group, or {@code null} when the
         * relay omitted it.
         *
         * <p>WA Web treats any non-null value as truthy in its addressing-mode
         * boolean coercion.
         */
        private final String addressingMode;

        /**
         * Constructs a new record with the given addressing mode tag.
         *
         * @param addressingMode the addressing mode tag
         */
        private LidMigrationState(String addressingMode) {
            this.addressingMode = addressingMode;
        }

        /**
         * Returns the addressing mode tag for the affected group.
         *
         * <p>The value is empty when the relay omitted the
         * {@code addressing_mode} field, which WA Web interprets as the group
         * having no LID addressing (legacy phone-number mode).
         *
         * @return an {@link Optional} containing the tag, or empty if absent
         */
        public Optional<String> addressingMode() {
            return Optional.ofNullable(addressingMode);
        }

        /**
         * Parses a LID migration state record from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return an {@link Optional} containing the parsed record, or empty if
         *         {@code obj} is {@code null}
         */
        static Optional<LidMigrationState> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var addressingMode = obj.getString("addressing_mode");
            return Optional.of(new LidMigrationState(addressingMode));
        }

        /**
         * Parses a list of LID migration state records from the given JSON
         * array, skipping {@code null} entries.
         *
         * <p>This is a symmetry helper for array-shaped state data; it is
         * currently unused at the call sites of this response.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed records, empty if {@code arr} is
         *         {@code null}
         */
        static List<LidMigrationState> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<LidMigrationState>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Parses the response from the raw JSON payload bytes.
     *
     * <p>This is only invoked via the {@link #of(Stanza)} entry point after the
     * IQ stanza has been unwrapped.
     *
     * @implNote This implementation requires the {@code data} and
     * {@code data.xwa2_group_update_users_role} envelopes to be present; the
     * inner {@code lid_migration_state} object is allowed to be absent and
     * collapses to {@code null} on the response, mirroring WA Web's
     * optional-chaining check {@code u?.lid_migration_state}.
     *
     * @param json the raw JSON bytes from the {@code <result>} child
     * @return an {@link Optional} containing the parsed response, or empty if
     *         the envelope is missing
     */
    private static Optional<TransferCommunityOwnershipMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_group_update_users_role");
        if (root == null) {
            return Optional.empty();
        }

        var groupId = root.getString("group_id");
        var lidMigrationState = LidMigrationState.of(root.getJSONObject("lid_migration_state")).orElse(null);

        return Optional.of(new TransferCommunityOwnershipMexResponse(groupId, lidMigrationState));
    }
}
