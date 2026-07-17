package com.github.auties00.cobalt.sync.factory;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValue;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.ContactAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.ContactActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.SyncPendingMutation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;

/**
 * Builds outgoing app-state mutations that sync an addressbook contact to or from the primary device.
 *
 * <p>Adding, editing, or removing a contact in the Web UI emits a contact-sync
 * mutation that the primary device replays against its native addressbook so
 * the contact stays consistent across linked devices. This factory builds the
 * outgoing mutation; the inbound counterpart is
 * {@link com.github.auties00.cobalt.sync.handler.ContactActionHandler}.
 *
 * @implNote
 * This implementation uses {@link Jid#toString()} for the index segment. WA
 * Web produces a legacy {@code @c.us}-suffixed encoding; Cobalt normalises JIDs
 * to a single canonical form upstream so the legacy form is not reproduced.
 * Receive-side indexing in WA Web also accepts the canonical form, so the wire
 * is compatible.
 */
public final class ContactActionMutationFactory {
    /**
     * The logger for {@link ContactActionMutationFactory}.
     */
    private static final System.Logger LOGGER = Log.get(ContactActionMutationFactory.class);

    /**
     * Creates a stateless factory with no collaborators.
     *
     * <p>A single instance may be shared across the lifetime of the client.
     */
    public ContactActionMutationFactory() {

    }

    /**
     * Returns a SET or REMOVE mutation that syncs the given contact, timestamped at the moment of the call.
     *
     * <p>This is the production code path; it delegates to
     * {@link #getContactSyncMutation(Jid, String, String, boolean, Jid, Boolean, String, Instant)}
     * with {@link Instant#now()}. Tests that need byte-equality against a
     * captured WA Web oracle should call the eight-arg overload directly with a
     * pinned timestamp.
     *
     * @implNote
     * This implementation forwards {@code null} for any optional field (first
     * name, full name, LID, username); the builder writes them as absent on the
     * wire so a deletion ({@code isDelete == true}) still carries a valid empty
     * {@link ContactAction} body.
     *
     * @param contactJid        the contact's primary {@link Jid} (PN form)
     * @param firstName         the user's first name, or {@code null} when unset
     * @param fullName          the user's full display name, or {@code null} when unset
     * @param isDelete          {@code true} to emit a {@link SyncdOperation#REMOVE}, {@code false} for {@link SyncdOperation#SET}
     * @param lid               the LID-form {@link Jid} for the same contact, or {@code null}
     * @param syncToAddressbook whether the primary device must sync the contact to its native addressbook, or {@code null} to leave it unchanged
     * @param username          the contact's WhatsApp username (without leading {@code @}), or {@code null}
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    @WhatsAppWebExport(moduleName = "WAWebContactSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public SyncPendingMutation getContactSyncMutation(
            Jid contactJid,
            String firstName,
            String fullName,
            boolean isDelete,
            Jid lid,
            Boolean syncToAddressbook,
            String username
    ) {
        return getContactSyncMutation(contactJid, firstName, fullName, isDelete, lid, syncToAddressbook, username, Instant.now());
    }

    /**
     * Returns a SET or REMOVE mutation that syncs the given contact at a caller-supplied timestamp.
     *
     * <p>The pinned-timestamp seam exists so byte-parity tests can re-encode the
     * same {@link SyncActionValue} that a
     * WA Web capture pinned at a known time; callers that just want the
     * production path should prefer the seven-arg overload.
     *
     * @implNote
     * This implementation does not log or report when the contact is a LID; WA
     * Web warns and sends a Falco event in that case, but Cobalt does not run
     * Falco and leaves the choice up to the caller.
     *
     * @param contactJid        the contact's primary {@link Jid} (PN form)
     * @param firstName         the user's first name, or {@code null} when unset
     * @param fullName          the user's full display name, or {@code null} when unset
     * @param isDelete          {@code true} to emit a {@link SyncdOperation#REMOVE}, {@code false} for {@link SyncdOperation#SET}
     * @param lid               the LID-form {@link Jid} for the same contact, or {@code null}
     * @param syncToAddressbook whether the primary device must sync the contact to its native addressbook, or {@code null} to leave it unchanged
     * @param username          the contact's WhatsApp username (without leading {@code @}), or {@code null}
     * @param timestamp         the mutation timestamp seeded into both the action value and the pending mutation
     * @return the pending mutation ready to be queued for outbound app-state sync
     */
    public SyncPendingMutation getContactSyncMutation(
            Jid contactJid,
            String firstName,
            String fullName,
            boolean isDelete,
            Jid lid,
            Boolean syncToAddressbook,
            String username,
            Instant timestamp
    ) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building contact sync mutation contact={0} delete={1}", contactJid, isDelete);
        var action = new ContactActionBuilder()
                .fullName(fullName)
                .firstName(firstName)
                .lidJid(lid)
                .saveOnPrimaryAddressbook(syncToAddressbook)
                .username(username)
                .build();
        var value = new SyncActionValueBuilder()
                .timestamp(timestamp)
                .contactAction(action)
                .build();
        var operation = isDelete
                ? SyncdOperation.REMOVE
                : SyncdOperation.SET;
        var index = JSON.toJSONString(List.of(ContactAction.ACTION_NAME, contactJid.toString()));
        var mutation = new DecryptedMutation.Trusted(
                index,
                value,
                operation,
                timestamp,
                ContactAction.ACTION_VERSION
        );
        return new SyncPendingMutation(mutation, 0);
    }
}
