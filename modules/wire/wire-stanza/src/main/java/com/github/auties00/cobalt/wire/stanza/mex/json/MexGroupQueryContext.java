package com.github.auties00.cobalt.wire.stanza.mex.json;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Enumerates the {@code query_context} GraphQL variable values accepted by the MEX group and
 * community queries.
 *
 * <p>WhatsApp Web always supplies this variable on every group-info, subgroup and invite-code MEX
 * query; the value tags the purpose of the query for the relay. {@link #INTERACTIVE},
 * {@link #UNKNOWN} and {@link #INVITE_CODE} are context hints that do not change the returned data,
 * because the projected fields are fixed by the persisted GraphQL document. By contrast
 * {@link #MISSING_PARTICIPANT_IDENTIFICATION} asks the relay to fully resolve participant LID and
 * phone-number identification and therefore changes the participant edges carried by the reply.
 *
 * @implNote This implementation mirrors the normalizer in {@code WAWebMexFetchGroupInfoJob}: WA Web
 * maps the internal caller tags {@code "interactive"} and {@code "enter_group_info"} to
 * {@link #INTERACTIVE}, {@code "missing_participant_identification"} to
 * {@link #MISSING_PARTICIPANT_IDENTIFICATION}, and both an absent tag and {@code "out_of_sync_update"}
 * to {@link #UNKNOWN}; the subgroup and invite-code jobs bypass the normalizer and pin
 * {@link #INTERACTIVE} and {@link #INVITE_CODE} respectively. The wire string is the uppercase
 * constant name for every value.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchGroupInfoJob")
@WhatsAppWebModule(moduleName = "WAWebMexFetchGroupInfoIncludBotsJob")
public enum MexGroupQueryContext {
    /**
     * Foreground user-driven group query such as opening group info or listing and refreshing the
     * subgroups of a community.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupInfoJob", exports = "mexGetGroupInfo",
            adaptation = WhatsAppAdaptation.DIRECT)
    INTERACTIVE("INTERACTIVE"),

    /**
     * Re-query that asks the relay to fully resolve participant LID and phone-number identification
     * when the locally cached group metadata is incomplete.
     *
     * <p>Unlike the other constants this value changes the reply: the relay returns the fully
     * identified participant edge list rather than the cached projection.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupInfoJob", exports = "mexGetGroupInfo",
            adaptation = WhatsAppAdaptation.DIRECT)
    MISSING_PARTICIPANT_IDENTIFICATION("MISSING_PARTICIPANT_IDENTIFICATION"),

    /**
     * Unspecified or background group query; the normalizer fallback WA Web emits when it has no
     * caller tag or is performing a background re-sync.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupInfoJob", exports = "mexGetGroupInfo",
            adaptation = WhatsAppAdaptation.DIRECT)
    UNKNOWN("UNKNOWN"),

    /**
     * Query whose purpose is reading a group's invite code.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupInviteCodeJob", exports = "fetchMexGroupInviteCode",
            adaptation = WhatsAppAdaptation.DIRECT)
    INVITE_CODE("INVITE_CODE");

    /**
     * The literal value emitted on the {@code query_context} GraphQL variable.
     */
    private final String wireValue;

    /**
     * Binds a new constant to its wire literal.
     *
     * @param wireValue the literal the relay expects on the {@code query_context} variable
     */
    MexGroupQueryContext(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the literal emitted on the {@code query_context} GraphQL variable.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }
}
