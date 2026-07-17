package com.github.auties00.cobalt.stream.presence;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.presence.SmaxServerUpdateResponse;
import com.github.auties00.cobalt.store.linked.protobuf.ProtobufWhatsAppStore;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.linked.LinkedContactPresenceListener;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.contact.Contact;
import com.github.auties00.cobalt.wire.linked.contact.ContactStatus;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stream.NodeStreamService;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Set;

/**
 * Updates the {@link Contact#lastKnownPresence()} and {@link Contact#lastSeen()} of the contact identified by a
 * {@code <presence>} stanza.
 *
 * <p>This handler drives the "online" and "last seen" surface shown next to a contact's name. It runs whenever the
 * server pushes a presence update for any subscribed contact; presence subscription itself is requested elsewhere. The
 * inbound stanza is resolved into a {@link SmaxServerUpdateResponse} variant that classifies it as
 * {@link ContactStatus#AVAILABLE} or {@link ContactStatus#UNAVAILABLE} and carries the last-seen value resolved by
 * {@link #resolveLastSeen(String, ContactStatus)}. The privacy sentinels {@code "deny"}, {@code "none"} and
 * {@code "error"} mean the peer's privacy settings hide their last-seen timestamp; in that case the existing timestamp
 * is left untouched.
 *
 * @implNote
 * This implementation delegates stanza classification to {@link SmaxServerUpdateResponse} and then mutates the presence
 * collection from the resolved variant. WhatsApp Web distinguishes a group-available and group-unavailable presence
 * variant that updates the per-group viewer count; that surface is not implemented here, so those variants flow through
 * to the per-contact path. WhatsApp Web also surfaces the {@code deny} sentinel as a {@code forceDisplay} UI hint on its
 * presence model; Cobalt is headless and has no such hint, so the {@code deny} sentinel is consumed by
 * {@link #resolveLastSeen(String, ContactStatus)} and discarded.
 */
@WhatsAppWebModule(moduleName = "WAWebHandlePresence")
@WhatsAppWebModule(moduleName = "WAWebChangePresenceHandlerAction")
public final class PresenceStreamHandler extends SocketStreamHandler.Concurrent {
    /**
     * The logger for {@link PresenceStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(PresenceStreamHandler.class);

    /**
     * Holds the {@code last}-attribute sentinel strings that mean the peer has hidden their last-seen timestamp via
     * privacy settings.
     *
     * <p>When the {@code last} value matches one of these strings, the contact's existing {@link Contact#lastSeen()} is
     * left untouched rather than overwritten.
     *
     * @implNote
     * This implementation mirrors the WhatsApp Web module-local constant {@code ["deny","none","error"]}.
     */
    private static final Set<String> HIDDEN_LAST_VALUES = Set.of("deny", "none", "error");

    /**
     * Owns the store this handler mutates and the listeners that receive the resulting presence notifications.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Constructs a new {@link PresenceStreamHandler} bound to the given {@link LinkedWhatsAppClient}.
     *
     * <p>The handler is constructed by the {@link NodeStreamService} wiring; embedders do not call this directly. The
     * supplied client must have an initialized
     * {@link ProtobufWhatsAppStore store}.
     *
     * @param whatsapp the non-{@code null} client whose store is updated and whose listeners are notified
     */
    @WhatsAppWebExport(moduleName = "WAWebHandlePresence", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public PresenceStreamHandler(LinkedWhatsAppClient whatsapp) {
        this.whatsapp = whatsapp;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Consumes one {@code <presence>} stanza for a non-self contact by resolving it into a
     * {@link SmaxServerUpdateResponse} variant, sets {@link ContactStatus#AVAILABLE} or
     * {@link ContactStatus#UNAVAILABLE} on the matching {@link Contact}, updates {@link Contact#lastSeen()} via
     * {@link #resolveLastSeen(String, ContactStatus)}, persists the contact and dispatches the presence notification to
     * every registered listener on a fresh virtual thread. Stanzas that do not resolve to a documented presence variant,
     * and stanzas whose {@code from} resolves to the client's own account, are dropped.
     *
     * @implNote
     * This implementation diverges from WhatsApp Web in three places.
     * <ul>
     * <li>WhatsApp Web dispatches group-available and group-unavailable variants to a separate group-presence action
     * handler; Cobalt does not implement that group-viewer-count surface, so those variants flow through to the
     * per-contact path here.</li>
     * <li>WhatsApp Web gates the entire path behind a LID-migration check and logs an error if a migrated client
     * receives a phone-number presence; Cobalt does not maintain that gate and accepts both LID and phone-number
     * {@code from} attributes through the {@link SmaxServerUpdateResponse} admit-set.</li>
     * <li>The {@code deny} flag that WhatsApp Web propagates as a {@code forceDisplay} UI hint is dropped, because
     * Cobalt is headless and has no equivalent display state.</li>
     * </ul>
     *
     * @param stanza {@inheritDoc}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandlePresence", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebChangePresenceHandlerAction", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public void handle(Stanza stanza) {
        var presence = SmaxServerUpdateResponse.of(stanza).orElse(null);
        if (presence == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring unparsable presence stanza {0}", stanza);
            return;
        }

        Jid from;
        ContactStatus status;
        String lastValue;
        switch (presence) {
            case SmaxServerUpdateResponse.GroupAvailable groupAvailable -> {
                from = groupAvailable.from();
                status = ContactStatus.AVAILABLE;
                lastValue = null;
            }
            case SmaxServerUpdateResponse.GroupUnavailable groupUnavailable -> {
                from = groupUnavailable.from();
                status = ContactStatus.UNAVAILABLE;
                lastValue = null;
            }
            case SmaxServerUpdateResponse.LastSeenWithOtherValue withOtherValue -> {
                from = withOtherValue.from();
                status = ContactStatus.UNAVAILABLE;
                lastValue = withOtherValue.last().orElse(null);
            }
            case SmaxServerUpdateResponse.UserUnavailable userUnavailable -> {
                from = userUnavailable.from();
                status = ContactStatus.UNAVAILABLE;
                lastValue = userUnavailable.last().orElse(null);
            }
            case SmaxServerUpdateResponse.Available available -> {
                from = available.from();
                status = ContactStatus.AVAILABLE;
                lastValue = available.last().orElse(null);
            }
        }

        var meJid = whatsapp.store().accountStore().jid().orElse(null);
        if (meJid != null && isSelfPresence(from, meJid)) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "ignoring self presence update from {0}", from);
            return;
        }

        var contact = getOrCreateContact(from);
        if (contact == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring presence update with unresolved from jid");
            return;
        }

        contact.setLastKnownPresence(status);

        var lastSeen = resolveLastSeen(lastValue, status);
        if (lastSeen != null) {
            contact.setLastSeen(lastSeen);
        }
        whatsapp.store().contactStore().addContact(contact);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "updated presence for {0} to {1}", contact.toJid(), status);
        notifyPresence(contact.toJid(), contact.toJid());
    }

    /**
     * Returns the {@link Instant} to store on {@link Contact#lastSeen()} given the raw {@code last} attribute value and
     * the resolved presence {@code status}, or {@code null} when the existing timestamp should be preserved.
     *
     * <p>This is the four-way classifier {@link #handle(Stanza)} uses to drive {@link Contact#setLastSeen(Instant)}. It
     * returns {@code null} for an online contact (last-seen is only relevant to offline contacts), {@link Instant#now()}
     * when an offline contact has no {@code last} attribute at all (the contact just went offline), {@code null} for
     * any of the {@link #HIDDEN_LAST_VALUES} (privacy settings hide the timestamp), and otherwise the unix epoch second
     * parsed from the attribute. A malformed numeric attribute is logged at debug level and also yields {@code null}.
     *
     * @implNote
     * This implementation is the equivalent of WhatsApp Web's helper that casts a numeric {@code last} value to a unix
     * time and falls back to the current unix time when the attribute is absent. Cobalt additionally swallows
     * {@link NumberFormatException} on malformed timestamps, where WhatsApp Web's numeric coercion would produce
     * {@code NaN} and propagate; returning {@code null} instead is consistent with the "leave existing timestamp
     * untouched" semantic in {@link #handle(Stanza)}.
     *
     * @param lastValue the raw {@code last} attribute, or {@code null} when the stanza did not carry one
     * @param status    the {@link ContactStatus} just derived from the {@code type} attribute
     * @return the timestamp to store, or {@code null} to leave the existing {@link Contact#lastSeen()} untouched
     */
    private Instant resolveLastSeen(String lastValue, ContactStatus status) {
        if (status != ContactStatus.UNAVAILABLE) {
            return null;
        }

        if (lastValue == null) {
            return Instant.now();
        }

        if (HIDDEN_LAST_VALUES.contains(lastValue)) {
            return null;
        }

        try {
            return Instant.ofEpochSecond(Long.parseLong(lastValue));
        } catch (NumberFormatException exception) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ignoring malformed presence last value {0}", lastValue);
            return null;
        }
    }

    /**
     * Returns {@code true} when the given {@code from} JID refers to the client's own account.
     *
     * <p>This check suppresses self-echoes by filtering presence updates for the local user out of the
     * presence-driven surfaces.
     *
     * @implNote
     * This implementation accepts a {@link Jid} on either the phone-number server or the LID server. The LID-server
     * branch consults the store's LID-to-phone-number cache via {@code findPhoneByLid}; WhatsApp Web instead carries
     * both ids on its current-user record and compares them directly without a cache lookup.
     *
     * @param from  the {@code from} attribute of the {@code <presence>} stanza
     * @param meJid the client's own JID from {@code store().jid()}
     * @return {@code true} when {@code from} and {@code meJid} resolve to the same user; {@code false} otherwise
     */
    private boolean isSelfPresence(Jid from, Jid meJid) {
        var fromUser = from.toUserJid();
        var meUser = meJid.toUserJid();
        if (fromUser.user().equals(meUser.user())) {
            return true;
        }
        if (fromUser.hasLidServer()) {
            var phoneJid = whatsapp.store().contactStore().findPhoneByLid(fromUser).orElse(null);
            return phoneJid != null && phoneJid.user().equals(meUser.user());
        }
        return false;
    }

    /**
     * Returns the {@link Contact} stored under the canonical phone-number form of {@code jid}, creating a fresh entry
     * when none exists.
     *
     * <p>This method centralizes the LID-to-phone-number normalization every presence-driven contact lookup needs.
     * Embedders that consume the presence notification can therefore assume the {@link Contact#toJid()} they receive is
     * on the phone-number server even when the wire stanza carried a LID-server {@code from}. A LID-server input that
     * has no cached phone-number mapping falls back to the LID form.
     *
     * @implNote
     * This implementation collapses WhatsApp Web's two-step JID-to-WID and phone-number lookup into one store call
     * because the LID-to-phone-number cache is the only source of truth for the mapping in Cobalt.
     *
     * @param jid the {@link Jid} read off the stanza, possibly {@code null}
     * @return the resolved {@link Contact}, or {@code null} when {@code jid} is {@code null}
     */
    private Contact getOrCreateContact(Jid jid) {
        if (jid == null) {
            return null;
        }

        var canonical = jid.toUserJid().hasLidServer()
                ? whatsapp.store().contactStore().findPhoneByLid(jid.toUserJid()).orElse(jid.toUserJid())
                : jid.toUserJid();
        return whatsapp.store().contactStore().findContactByJid(canonical)
                .orElseGet(() -> whatsapp.store().contactStore().addNewContact(canonical));
    }

    /**
     * Fans out a presence notification to every registered
     * {@link LinkedWhatsAppClientListener}.
     *
     * <p>The {@code conversation} and {@code participant} arguments are equal for {@code <presence>} stanzas (presence
     * is always per-contact, never per-group); they are kept distinct in the listener signature so the same callback
     * can also be invoked from {@code <chatstate>} handlers where the conversation is a group JID.
     *
     * @implNote
     * This implementation starts one virtual thread per listener so that a blocking listener cannot stall the
     * {@link NodeStreamService} dispatch loop.
     *
     * @param conversation the JID of the conversation that experienced the presence change
     * @param participant  the JID of the participant whose presence changed
     */
    private void notifyPresence(Jid conversation, Jid participant) {
        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof LinkedContactPresenceListener typed) {
                Thread.startVirtualThread(() -> typed.onContactPresence(whatsapp, conversation, participant));
            }
        }
    }
}
