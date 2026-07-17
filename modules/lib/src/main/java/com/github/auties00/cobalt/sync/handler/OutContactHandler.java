package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.linked.contact.OutContactBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.OutContactAction;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import java.lang.System.Logger.Level;

/**
 * Applies the {@code outContact} app-state action that synchronises
 * "invite by contact" address-book entries across linked devices.
 *
 * <p>Drives the desktop "invite by contact" flow: each mutation creates,
 * updates, or removes a phone-number-keyed address-book entry that the
 * invite-by-contact surface renders without requiring the contact to be a
 * WhatsApp user. The mutation index keys each entry by the user JID,
 * formatted as {@snippet :
 *     ["outContact", userJid]
 * }
 *
 * <p>The whole batch is gated on
 * {@link ABProp#OUT_CONTACT_INVITES_ENABLED}: when the gate is closed every
 * mutation surfaces as {@link MutationApplicationResult#unsupported()}. A
 * {@link com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation#SET}
 * upserts an {@link com.github.auties00.cobalt.wire.linked.contact.OutContact}
 * record and a
 * {@link com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation#REMOVE}
 * drops it; any other operation is reported as
 * {@link SyncdIndexUtils#malformedActionValue(String)}.
 *
 * @implNote
 * This implementation gates the batch on
 * {@link ABProp#OUT_CONTACT_INVITES_ENABLED} via
 * {@code abPropsService.getInt(...) == 1}. The chat JID is validated via
 * {@link Jid#of(String)}; WA Web's {@code WAJids.interpretAndValidateJid}
 * returns a discriminated union with {@code jidType: "unknown"} which Cobalt
 * collapses into the same malformed outcome. The
 * {@code WAWebBackendApi.frontendFireAndForget} dispatches to the desktop
 * frontend are dropped because Cobalt has no UI consumer.
 */
@WhatsAppWebModule(moduleName = "WAWebOutContactSync")
public final class OutContactHandler implements WebAppStateActionHandler {
    /**
     * The logger for {@link OutContactHandler}.
     *
     * @implNote
     * This implementation logs at {@link Level#DEBUG} for invalid-JID and
     * set/remove tracing; the WA Web per-batch counters and {@code sendLogs}
     * side effect are dropped.
     */
    private static final System.Logger LOGGER = Log.get(OutContactHandler.class);

    /**
     * Holds the AB-props service consulted before applying any mutation.
     */
    private final ABPropsService abPropsService;

    /**
     * Holds the {@link ABProp#OUT_CONTACT_INVITES_ENABLED} value that opts the
     * paired account into the outgoing-contact invite flow.
     *
     * @implNote
     * This implementation matches WA Web's
     * {@code WAWebOutContactInviteGating.isOutContactInviteEnabled} which
     * treats exactly {@code 1} as opted-in; any other value (including
     * {@code 0} and absent) is treated as opt-out.
     */
    private static final int OUT_CONTACT_INVITES_ENABLED_VALUE = 1;

    /**
     * Constructs the out-contact sync handler bound to the given AB-props
     * service.
     *
     * @param abPropsService the AB-props service consulted on every mutation
     */
    @WhatsAppWebExport(moduleName = "WAWebOutContactSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public OutContactHandler(ABPropsService abPropsService) {
        this.abPropsService = abPropsService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebOutContactSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public String actionName() {
        return OutContactAction.ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebOutContactSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public SyncPatchType collectionName() {
        return OutContactAction.COLLECTION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebOutContactSync", exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return OutContactAction.ACTION_VERSION;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation walks the per-mutation arms of WA Web's
     * {@code WAWebOutContactSync.applyMutations}: the
     * {@link ABProp#OUT_CONTACT_INVITES_ENABLED} gate must be {@code 1};
     * {@code indexParts[1]} must be a non-empty string parseable as a
     * {@link Jid} via {@link Jid#of(String)}; the JID must be a phone-user
     * JID ({@link Jid#hasUserServer()}); a
     * {@link com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation#SET}
     * upserts an {@link com.github.auties00.cobalt.wire.linked.contact.OutContact}
     * built from {@code (fullName, firstName)} normalised via
     * {@link #coalesceEmpty(String)} and {@link #deriveFirstWord(String)},
     * while a
     * {@link com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation#REMOVE}
     * drops the record. The frontend IPC dispatches and the {@code sendLogs}
     * debug-flush are dropped because Cobalt has no desktop bridge.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebOutContactSync", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationApplicationResult applyMutation(LinkedWhatsAppClient client, DecryptedMutation.Trusted mutation) {
        var gateValue = abPropsService.getInt(ABProp.OUT_CONTACT_INVITES_ENABLED);
        if (gateValue != OUT_CONTACT_INVITES_ENABLED_VALUE) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "out contact: unsupported, invites disabled");
            return MutationApplicationResult.unsupported();
        }

        var indexArray = JSON.parseArray(mutation.index());
        if (indexArray.size() <= 1) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "out contact mutation malformed: missing jid index");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }
        var userJidString = indexArray.getString(1);
        if (userJidString == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "out contact mutation malformed: null jid index");
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        Jid userJid;
        try {
            userJid = Jid.of(userJidString);
        } catch (Exception e) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "out contact: malformed jid {0}", new LogRedactable.User(userJidString));
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        if (!userJid.hasUserServer()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "out contact: jid missing expected domain {0}", userJid);
            return SyncdIndexUtils.malformedActionValue(collectionName().name());
        }

        return switch (mutation.operation()) {
            case SET -> {
                if (!(mutation.value().flatMap(sav -> sav.action()).orElse(null) instanceof OutContactAction action)) {
                    if (Log.WARNING)
                        LOGGER.log(Level.WARNING, "out contact mutation malformed: missing action value");
                    yield SyncdIndexUtils.malformedActionValue(collectionName().name());
                }

                var fullName = coalesceEmpty(action.fullName().orElse(null));
                var explicitFirstName = coalesceEmpty(action.firstName().orElse(null));
                var firstName = explicitFirstName != null ? explicitFirstName : deriveFirstWord(fullName);

                var outContact = new OutContactBuilder()
                        .jid(userJid)
                        .fullName(fullName)
                        .firstName(firstName)
                        .build();
                client.store().contactStore().addOutContact(outContact);

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "out contact: set {0}", userJid);

                yield MutationApplicationResult.success();
            }
            case REMOVE -> {
                client.store().contactStore().removeOutContact(userJid);

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "out contact: remove {0}", userJid);

                yield MutationApplicationResult.success();
            }
            default -> {
                if (Log.DEBUG)
                    LOGGER.log(Level.DEBUG, "out contact: unsupported operation {0}", mutation.operation());
                yield SyncdIndexUtils.malformedActionValue(collectionName().name());
            }
        };
    }

    /**
     * Coalesces a {@code null} or empty string into {@code null}.
     *
     * <p>Normalises {@link OutContactAction#fullName()} and
     * {@link OutContactAction#firstName()} before deciding whether to derive a
     * fallback or skip the field entirely.
     *
     * @implNote
     * This implementation mirrors the WA Web inline helper {@code m(e)}:
     * {@code return e == null || e === "" ? null : e}.
     *
     * @param value the value to normalise; may be {@code null}
     * @return {@code null} when {@code value} is {@code null} or empty;
     *         otherwise the original value unchanged
     */
    private static String coalesceEmpty(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }

    /**
     * Returns the first whitespace-separated token of the given string,
     * trimmed.
     *
     * <p>Derives a {@code firstName} fallback from a {@code fullName} when the
     * action does not carry an explicit first name.
     *
     * @implNote
     * This implementation mirrors the WA Web inline helper {@code p(e)}:
     * {@code var t = e.trim().split(" ")[0]; return t || null;}. WA Web splits
     * on the literal ASCII space character (rather than any Unicode
     * whitespace), so this implementation uses the same {@code ' '} delimiter.
     *
     * @param value the value to extract the first word from; may be
     *              {@code null}
     * @return the first whitespace-delimited token of the trimmed value;
     *         {@code null} when {@code value} is {@code null} or empty after
     *         trimming
     */
    private static String deriveFirstWord(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        var spaceIndex = trimmed.indexOf(' ');
        var firstToken = spaceIndex == -1 ? trimmed : trimmed.substring(0, spaceIndex);
        return firstToken.isEmpty() ? null : firstToken;
    }
}
