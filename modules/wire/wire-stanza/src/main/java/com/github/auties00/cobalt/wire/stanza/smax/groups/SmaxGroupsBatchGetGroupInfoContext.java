package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Enumerates the {@code context} attribute values WhatsApp Web emits on the bulk
 * {@code <query context="...">} of a batch group-info request.
 *
 * <p>The tag names the client maintenance path that issued the bulk fetch; the relay logs it but
 * does not act on it, so the returned group metadata is identical regardless of the value. The
 * attribute is optional and omitted entirely when no value is supplied.
 *
 * @implNote This implementation enumerates the three tokens WA Web's callers pass verbatim through
 * {@code WASmaxOutGroupsBatchGetGroupInfoRequest.makeBatchGetGroupInfoRequest}. The wire field is a
 * free-form string the relay accepts as-is; modelling it as an enum constrains Cobalt callers to the
 * known-good values rather than an arbitrary string.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBatchGetGroupInfoRequest")
public enum SmaxGroupsBatchGetGroupInfoContext {
    /**
     * Bulk group-metadata refresh issued during per-group dirty-bit recovery over the truncatable
     * batch path.
     */
    @WhatsAppWebExport(moduleName = "WAWebQueryAndUpdateGroupsMetadataByJidsJob",
            exports = "queryAndUpdateAllGroupMetadata", adaptation = WhatsAppAdaptation.DIRECT)
    PER_GROUP_DIRTY_RECOVERY_TRUNCATABLE("per_group_dirty_recovery_truncatable"),

    /**
     * Bulk group-metadata refresh issued while migrating inactive phone-number-addressed groups to
     * LID addressing.
     */
    @WhatsAppWebExport(moduleName = "WAWebInactiveGroupLidMigrationJob",
            exports = "queryAndUpdateAllGroupMetadata", adaptation = WhatsAppAdaptation.DIRECT)
    INACTIVE_GROUP_MIGRATION("inactive_group_migration"),

    /**
     * Default token substituted when re-fetching truncated group-info responses in size-bounded
     * paginated batches.
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupQueryJob",
            exports = "queryGroupsById_DO_NOT_USE_DIRECTLY", adaptation = WhatsAppAdaptation.DIRECT)
    GET_PARTICIPATING_GROUPS_PAGINATED("get_participating_groups_paginated");

    /**
     * The literal value emitted on the {@code context} attribute.
     */
    private final String wireValue;

    /**
     * Binds a new constant to its wire literal.
     *
     * @param wireValue the literal the relay expects on the {@code context} attribute
     */
    SmaxGroupsBatchGetGroupInfoContext(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the literal emitted on the {@code context} attribute of the batch group-info
     * {@code <query>}.
     *
     * @return the wire literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }
}
