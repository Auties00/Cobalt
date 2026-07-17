package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.mutation.OrphanMutationEntry;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.ContactAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.UserStatusMuteAction;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Reconciles the local address-book contact roster with {@code contact} sync mutations.
 *
 * <p>This handler drives the address-book surface (the Contacts list, the
 * new-message picker, the LID-PN learning index). When the user adds, edits,
 * renames, or deletes an address-book contact on another device, the server
 * replays the change here as a {@link ContactAction}, and the result becomes
 * observable through
 * {@link LinkedWhatsAppContactStore#findContactByJid(com.github.auties00.cobalt.wire.core.jid.JidProvider)}.
 *
 * @implNote
 * This implementation drops several WA Web batch-level side effects
 * because Cobalt has no equivalent surface: the
 * {@code WAWebSyncContactsJob} debounced background refresh, the
 * {@code clearStatusForRemovedContact} frontend send-and-receive
 * call, the per-batch {@code writeSyncdLog} markers, and the
 * batched {@code bulkGet} that filters out username-only contacts
 * before clearing address-book fields. The username-contact filter is
 * implemented per-mutation against the local
 * {@link com.github.auties00.cobalt.wire.linked.contact.Contact#isAddedByUsername()}
 * flag instead. LID-PN learning is performed inline via
 * {@link LinkedWhatsAppContactStore#registerLidMapping(Jid, Jid)}
 * rather than batched and committed via WA Web's
 * {@code createLidPnMappings(flushImmediately:true, learningSource:"other")}.
 */
@WhatsAppWebModule(moduleName = "WAWebContactSync")
public final class ContactActionHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link ContactActionHandler}.
     */
    private static final System.Logger LOGGER = Log.get(ContactActionHandler.class);

    /**
     * The {@link ABPropsService} consulted before writing the username field.
     *
     * <p>Reads the {@link ABProp#USERNAME_CONTACT_SYNCD_SUPPORT_ENABLE} gate;
     * when off the username field on a SET mutation is ignored and the
     * username-contact filter on a REMOVE mutation is bypassed.
     */
    private final ABPropsService abPropsService;

    /**
     * The {@link UserStatusMuteHandler} delegated to when retrying orphan user-status-mute mutations.
     *
     * <p>Re-processes any orphan {@code user_status_mute} mutations unblocked by
     * the appearance of a fresh contact, after the contact upsert.
     */
    private final UserStatusMuteHandler userStatusMuteHandler;

    /**
     * The compiled {@link Pattern} matching any single Unicode whitespace character.
     *
     * <p>Splits a full name so {@link #deriveShortName(String)} can take its
     * first whitespace-delimited token.
     */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    /**
     * Constructs the contact-action handler with its dependencies.
     *
     * <p>The sync handler registry instantiates this with the shared
     * {@link ABPropsService} and the dependent {@link UserStatusMuteHandler}.
     *
     * @param abPropsService the {@link ABPropsService} consulted for the username gate
     * @param userStatusMuteHandler the {@link UserStatusMuteHandler} used to re-process orphan mutations
     */
    @WhatsAppWebExport(moduleName = "WAWebContactSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public ContactActionHandler(ABPropsService abPropsService, UserStatusMuteHandler userStatusMuteHandler) {
        this.abPropsService = abPropsService;
        this.userStatusMuteHandler = userStatusMuteHandler;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebContactSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return ContactAction.ACTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebContactSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return ContactAction.COLLECTION_NAME;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebContactSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return ContactAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For SET mutations, validates the JSON index
     * {@code ["contact", contactJid]}, skips LID JIDs, upserts the
     * {@link com.github.auties00.cobalt.wire.linked.contact.Contact} with its full
     * name, derived short name, optional username, and LID mapping, and retries
     * any pending orphan {@code user_status_mute} mutations for the same JID.
     * For REMOVE mutations, skips LID and bot JIDs and clears the contact's
     * address-book fields (name, short name, username).
     *
     * @implNote
     * This implementation derives the short name via
     * {@link #deriveShortName(String)} when the action does not carry
     * one, mirroring WA Web's
     * {@code WAWebContactShortName.getShortName} fallback. The
     * username field is written only when
     * {@link ABProp#USERNAME_CONTACT_SYNCD_SUPPORT_ENABLE} is set,
     * matching WA Web's {@code usernameContactSyncdEnabled()} gate.
     * Username-only contacts (those flagged
     * {@link com.github.auties00.cobalt.wire.linked.contact.Contact#isAddedByUsername()})
     * survive a REMOVE when the gate is on, mirroring WA Web's
     * {@code bulkGet} filter that exempts {@code isUsernameContact === true}
     * entries from address-book clearing.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebContactSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() <= 1) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "contact mutation malformed: index size={0}", indexArray.size());
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }
        var contactJidString = indexArray.getString(1);
        if (contactJidString == null || contactJidString.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "contact mutation malformed: missing contact jid");
            return SyncdIndexUtils.malformedActionIndex(collectionName().name(), actionName());
        }

        var contactJid = Jid.of(contactJidString);
        var usernameEnabled = abPropsService
                .getBool(ABProp.USERNAME_CONTACT_SYNCD_SUPPORT_ENABLE);

        switch (mutation.operation()) {
            case SET -> {
                if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof ContactAction action)) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "contact mutation malformed: missing action value");
                    return SyncdIndexUtils.malformedActionValue(collectionName().name());
                }

                if (contactJid.hasLidServer()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "contact mutation skipped: lid jid {0}", contactJid);
                    return MutationApplicationResult.skipped();
                }

                var contact = client.store().contactStore().findContactByJid(contactJid)
                        .orElseGet(() -> client.store().contactStore().addNewContact(contactJid));
                var fullName = action.fullName().orElse("");
                contact.setFullName(fullName);
                var shortName = action.firstName()
                        .orElseGet(() -> deriveShortName(fullName));
                contact.setShortName(shortName);

                if (usernameEnabled) {
                    action.username()
                            .filter(u -> !u.isEmpty())
                            .map(u -> u.startsWith("@") ? u.substring(1) : u)
                            .ifPresent(contact::setUsername);
                }

                action.lidJid().ifPresent(lid -> {
                    contact.setLid(lid);
                    if (contactJid.hasUserServer()) {
                        client.store().contactStore().registerLidMapping(contactJid, lid);
                    }
                });

                retryOrphanStatusMutes(client, contactJidString);

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "contact upserted: jid={0}", contactJid);
                return MutationApplicationResult.success();
            }
            case REMOVE -> {
                if (contactJid.hasLidServer() || contactJid.hasBotServer()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "contact remove mutation skipped: lid or bot jid {0}", contactJid);
                    return MutationApplicationResult.skipped();
                }

                var contact = client.store().contactStore().findContactByJid(contactJid);
                if (contact.isPresent()) {
                    if (usernameEnabled && contact.get().isAddedByUsername()) {
                        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "contact remove mutation skipped: username contact {0}", contactJid);
                        return MutationApplicationResult.success();
                    }
                    contact.get().setFullName(null);
                    contact.get().setShortName(null);
                    contact.get().setUsername(null);
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "contact address-book fields cleared: jid={0}", contactJid);
                }
                return MutationApplicationResult.success();
            }
            default -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "contact mutation unsupported: operation={0}", mutation.operation());
                return MutationApplicationResult.unsupported();
            }
        }
    }

    /**
     * Re-processes any orphan {@code user_status_mute} mutations whose target contact JID is the one just upserted.
     *
     * <p>Invoked from the SET branch of
     * {@link #applyMutation(LinkedWhatsAppClient, DecryptedMutation.Trusted)} once the
     * contact upsert lands. Successfully reapplied orphans are deleted from the
     * orphan store; failures are left in place for a future retry.
     *
     * @implNote
     * This implementation walks
     * {@link LinkedWhatsAppSyncStore#findOrphanMutationsByModel(SyncPatchType, String)}
     * and dispatches each entry through {@link UserStatusMuteHandler}.
     * Any thrown exception is caught and logged at {@link Level#WARNING},
     * replacing WA Web's {@code WALogger.ERROR(...).sendLogs(...)} pair
     * (Cobalt has no server-side log-uploading channel here).
     *
     * @param client the {@link LinkedWhatsAppClient} whose store hosts the orphan entries
     * @param contactJidString the contact JID string used to look up the orphan entries
     */
    @WhatsAppWebExport(moduleName = "WAWebContactSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    private void retryOrphanStatusMutes(LinkedWhatsAppClient client, String contactJidString) {
        try {
            var entries = client.store().syncStore().findOrphanMutationsByModel(UserStatusMuteAction.COLLECTION_NAME, contactJidString);
            if (entries.isEmpty()) {
                return;
            }

            var applied = new ArrayList<OrphanMutationEntry>();
            for (var entry : entries) {
                var orphanMutation = new DecryptedMutation.Trusted(
                        entry.index(),
                        entry.value(),
                        entry.operation(),
                        entry.timestamp(),
                        entry.actionVersion()
                );
                var result = userStatusMuteHandler.applyMutation(client, orphanMutation);
                if (result.actionState() == SyncActionState.SUCCESS) {
                    applied.add(entry);
                }
            }

            if (!applied.isEmpty()) {
                client.store().syncStore().removeOrphanMutations(UserStatusMuteAction.COLLECTION_NAME, applied);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "orphan status mutes reapplied: count={0}", applied.size());
            }
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "orphan status mutes retry failed for contact " + new LogRedactable.User(contactJidString), e);
        }
    }

    /**
     * Returns the first whitespace-delimited word of {@code fullName} when it contains a Unicode letter.
     *
     * <p>Serves as the fallback for {@link ContactAction#firstName()} when the
     * wire payload omits the short name. An empty or letter-free first token
     * returns the empty string rather than {@code null}; callers in this module
     * coalesce the two uniformly.
     *
     * @implNote
     * This implementation splits on the {@link #WHITESPACE_PATTERN}
     * (Unicode whitespace), takes the first token, and confirms via
     * {@link #containsLetter(String)} that the token has at least one
     * Unicode letter character. WA Web uses an explicit
     * {@code WAWebAlphaRegex} character class; Java's
     * {@link Character#isLetter(int)} covers the same Unicode L*
     * categories.
     *
     * @param fullName the contact full name to derive the short name from
     * @return the first whitespace-delimited token containing a letter, or the empty string if none
     */
    @WhatsAppWebExport(moduleName = "WAWebContactShortName", exports = "getShortName", adaptation = WhatsAppAdaptation.ADAPTED)
    static String deriveShortName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return "";
        }
        var tokens = WHITESPACE_PATTERN.split(fullName, 2);
        var firstToken = tokens[0];
        if (firstToken.isEmpty()) {
            return "";
        }
        if (!containsLetter(firstToken)) {
            return "";
        }
        return firstToken;
    }

    /**
     * Returns whether the given string contains at least one Unicode letter character.
     *
     * <p>Lets {@link #deriveShortName(String)} reject tokens that are pure
     * punctuation, digits, or symbols.
     *
     * @implNote
     * This implementation streams the string's code points and tests
     * each via {@link Character#isLetter(int)}; the L* general
     * category coverage matches the Unicode letter character class WA
     * Web's regex compiles to.
     *
     * @param s the string to inspect
     * @return {@code true} when at least one code point is a Unicode letter; {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebAlphaRegex", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean containsLetter(String s) {
        return s.codePoints().anyMatch(Character::isLetter);
    }
}
