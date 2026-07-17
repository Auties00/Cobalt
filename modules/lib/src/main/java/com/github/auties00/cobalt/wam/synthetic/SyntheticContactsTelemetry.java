package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.CompanionInviteContactEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.CompanionsContactEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ContactSearchExperienceEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.NativeContactsNuxEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebContactListStartNewChatEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.*;

import java.util.Objects;
import java.util.UUID;

/**
 * Emits the block of WhatsApp Web contact-management WAM beacons that a real Web
 * or Desktop session produces from its contacts surfaces (the start-new-chat
 * contact list, the companion add/save-contact drawer, the native-contacts
 * onboarding NUX, the username/contact search bar, and the invite-non-WhatsApp
 * contact flow) but that a headless Cobalt client has no rendered surface to
 * generate.
 *
 * <p>None of the five events wired here map to a Cobalt feature: each measures a
 * tap, impression, save, search, or invite performed against a graphical
 * contacts UI. Cobalt caches server-pushed contacts in its
 * {@link com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore} but
 * exposes no user-facing save-contact drawer, onboarding NUX, contact-search
 * field, or invite-to-WhatsApp action, so a Cobalt session that never emitted
 * any of them would carry a telemetry fingerprint trivially distinguishable from
 * a genuine WhatsApp Web session. This service synthesises one plausible,
 * self-consistent occurrence of each and commits them through
 * {@link WamService#commit(com.github.auties00.cobalt.wam.model.WamEventSpec)}.
 * Every event is populated with real store-derived values wherever the datum
 * exists (the WhatsApp-known contact count sampled from the live store) and with
 * realistic fabricated constants for the purely presentational fields a headless
 * client cannot source (surface enums, edited-field flags, save results, and
 * session identifiers).
 *
 * <p>The single public entrypoint is {@link #emitSessionTelemetry()}. Each event
 * is committed exactly once per invocation; the intended cadence is once per
 * successful connection (a real UI produces these throughout a session as the
 * user browses contacts, but a single representative sample per connect is
 * sufficient to keep the stream shaped like a genuine client without
 * over-reporting).
 *
 * @see WamService
 * @see com.github.auties00.cobalt.wire.wam.event.CompanionsContactEventEvent
 */
public final class SyntheticContactsTelemetry {
    /**
     * The fabricated number of address-book-only (non-WhatsApp) contacts added to
     * the live WhatsApp-known contact count when reporting the invite flow's
     * address-book total.
     *
     * <p>A real phone address book carries more entries than are resolved to
     * WhatsApp; this floor keeps the synthesised address-book total strictly
     * larger than the WhatsApp count even when the live store holds few contacts,
     * mirroring the {@code OutContactCollection.length} term WA Web adds on top of
     * {@code ContactCollection.length}.
     */
    private static final long FABRICATED_OUT_CONTACTS = 12L;

    /**
     * The bound WhatsApp client whose store supplies the live contact count
     * sampled when populating the synthetic events.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every synthesised contacts event is committed
     * for batched upload.
     */
    private final WamService wamService;

    /**
     * The stable per-instance session identifier reused across the emitted events
     * as their string and numeric session fields.
     *
     * <p>It is minted once at construction and shared so the synthesised beacons
     * correlate to a single session the way a real client's contact-drawer and
     * invite events would, mirroring WA Web's per-flow {@code cryptoRandomUUID}
     * and {@code OutContactInviteSessionId}.
     */
    private final UUID sessionUuid;

    /**
     * Constructs a new {@code SyntheticContactsTelemetry} bound to the given
     * client and WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be
     *                   {@code null}
     * @param wamService the WAM service used to commit the emitted events, must
     *                   not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticContactsTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.sessionUuid = UUID.randomUUID();
    }

    /**
     * Emits one representative occurrence of every synthetic contacts beacon
     * carried by this service.
     *
     * <p>This is the single entrypoint the client drives once per successful
     * connection. It commits the start-new-chat, add-contact, native-contacts
     * NUX, contact-search, and invite-contact events in turn; each helper
     * fabricates one self-consistent occurrence and commits it through
     * {@link WamService}. There is no natural recurring cadence because a headless
     * client raises no contacts-UI events, so a single per-connect sample stands
     * in for the stream a rendered client would produce as the user works through
     * their contacts.
     */
    public void emitSessionTelemetry() {
        emitWebContactListStartNewChat();
        emitCompanionsContactEvent();
        emitNativeContactsNuxEvent();
        emitContactSearchExperience();
        emitCompanionInviteContact();
    }

    /**
     * Synthesises a start-new-chat action from the contact-list panel (event id
     * 4560).
     *
     * <p>The fabricated event models the user starting a one-to-one chat by
     * searching for and picking a contact in the contact-list surface, matching
     * WA Web's {@code logContactListStartNewChatAction} which reports the
     * search-result flag and the chat type.
     */
    @WhatsAppWebExport(moduleName = "WAWebNewChatMetricUtils", exports = "logContactListStartNewChatAction", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitWebContactListStartNewChat() {
        wamService.commit(new WebContactListStartNewChatEventBuilder()
                .webContactListStartNewChatSearch(true)
                .webContactListStartNewChatType(WebContactListStartNewChatType.CONTACT)
                .build());
    }

    /**
     * Synthesises a companion add-contact drawer save (event id 5718).
     *
     * <p>The fabricated event models a successful save of a new WhatsApp contact
     * opened from the New chat drawer: the first name and phone number were
     * edited, address-book sync stayed off, and the saved number resolves to a
     * WhatsApp user. It mirrors WA Web's {@code AddContactEvent.logSave}, which
     * emits a per-drawer session id, the save source and result, and the
     * edited/autofilled field flags.
     */
    @WhatsAppWebExport(moduleName = "WAWebContactLogging", exports = "AddContactEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitCompanionsContactEvent() {
        wamService.commit(new CompanionsContactEventEventBuilder()
                .companionAddContactActionType(AddContactActionType.SAVE)
                .companionAddContactEventType(CompanionAddContactEventType.CREATE_NEW)
                .companionAddContactSessionId(SyntheticTelemetryUtils.newSessionId())
                .companionAddContactSource(CompanionAddContactSource.NEW_CHAT)
                .companionContactSaveResult(CompanionContactSaveResult.SUCCESS)
                .companionFnameEdited(true)
                .companionHasPhoneNumber(true)
                .companionHasUsername(false)
                .companionIsContactSyncToOs(false)
                .companionLnameEdited(false)
                .companionPhNumberAutofilled(false)
                .companionPhNumberEdited(true)
                .companionSyncSettingChanged(false)
                .companionUsernameAutofilled(false)
                .companionUsernameEdited(false)
                .companionWhatsappContactStatus(CompanionWhatsappContactStatus.IN_NETWORK)
                .build());
    }

    /**
     * Synthesises a native-contacts onboarding NUX impression (event id 5788).
     *
     * <p>The fabricated event models the first-time native-contacts new-user
     * experience being viewed from the new-contact entry point, mirroring WA Web's
     * {@code logViewNativeContactNux} which reports the entry point and the NUX
     * event type.
     */
    @WhatsAppWebExport(moduleName = "WAWebContactLogging", exports = "logViewNativeContactNux", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitNativeContactsNuxEvent() {
        wamService.commit(new NativeContactsNuxEventEventBuilder()
                .nativeContactsNuxEntryPoint(NativeContactsNuxEntryPoint.NEW_CONTACT)
                .nativeContactsNuxEventType(NativeContactsNuxEventType.VIEW_NATIVE_CONTACTS_NUX)
                .build());
    }

    /**
     * Synthesises a contact/username search action (event id 6574).
     *
     * <p>The fabricated event models the user beginning a username search whose
     * query starts with an {@code '@'} from the New chat entry point, mirroring WA
     * Web's {@code UsernameSearchLogger.log} which reports whether the search is a
     * username search, whether it starts with {@code '@'}, the search action name,
     * and the entry point.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsernameSearchLogger", exports = "UsernameSearchLogger", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitContactSearchExperience() {
        wamService.commit(new ContactSearchExperienceEventBuilder()
                .contactSearchEntrypoint(ContactSearchEntrypoint.NEW_CHAT)
                .isUsernameSearch(true)
                .searchActionName(SearchActionName.SEARCH_START)
                .searchStartsWithAt(true)
                .build());
    }

    /**
     * Synthesises an invite-non-WhatsApp-contact send (event id 8230).
     *
     * <p>The fabricated event models sending a native-SMS invite to an
     * address-book contact who is not on WhatsApp, selected from the contact
     * picker list. The WhatsApp-contact count is sampled from the live store and
     * the address-book total is derived as that count plus a fabricated
     * non-WhatsApp remainder, mirroring WA Web's {@code logOneToOneInviteContact}
     * which reports the invite method, origin, session id, validity, and the two
     * contact counts ({@code ContactCollection.length} versus that plus
     * {@code OutContactCollection.length}). The optional invite-code error field
     * is left unset, matching WA which omits it on a successful send.
     */
    @WhatsAppWebExport(moduleName = "WAWebOutContactLoggingUtils", exports = "logOneToOneInviteContact", adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitCompanionInviteContact() {
        var whatsAppContacts = contactCount();
        var addressBookContacts = whatsAppContacts + FABRICATED_OUT_CONTACTS;
        wamService.commit(new CompanionInviteContactEventBuilder()
                .companionInviteAction(CompanionInviteActionType.INVITE_SEND)
                .companionInviteMethod(CompanionInviteMethodType.NATIVE_SMS)
                .companionInviteNumContactsAddressBook(addressBookContacts)
                .companionInviteNumContactsWa(whatsAppContacts)
                .companionInviteOrigin(CompanionInviteOriginType.CONTACT_PICKER_LIST)
                .companionInviteSessionId(numericSessionId())
                .companionValidInviteCode(true)
                .build());
    }


    /**
     * Returns a positive numeric session identifier derived from the shared
     * per-instance session UUID.
     *
     * <p>It masks off the sign bit of the UUID's most-significant longword so the
     * integer-typed invite-session field receives a stable, non-negative value
     * standing in for WA Web's numeric {@code OutContactInviteSessionId}.
     *
     * @return the non-negative numeric session identifier
     */
    private long numericSessionId() {
        return sessionUuid.getMostSignificantBits() & Long.MAX_VALUE;
    }

    /**
     * Returns the number of WhatsApp-known contacts held in the live store.
     *
     * <p>This is the real datum reported as the invite flow's WhatsApp-contact
     * count and used as the base for the fabricated address-book total.
     *
     * @return the contact count
     */
    private long contactCount() {
        return client.store().contactStore().contacts().size();
    }
}
