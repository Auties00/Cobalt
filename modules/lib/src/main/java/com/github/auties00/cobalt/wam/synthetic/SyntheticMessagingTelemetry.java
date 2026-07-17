package com.github.auties00.cobalt.wam.synthetic;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.DeepLinkClickEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.DeepLinkMsgSentEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.DeepLinkOpenEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.DisappearingMessageChatPickerEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.DisappearingModeSettingChangeEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.DisappearingModeSettingEventsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ForwardActionUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.HfmTextSearchCompleteEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.KeepInChatNotifEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.KeepInChatNuxEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.MessageContextMenuActionsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PinInChatInteractionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PsPhoneNumberHyperlinkEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.QuotedMessageUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ReactionActionsEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ReactionUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SearchTheWebFunnelEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.ShareContentUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SnackbarDeleteUndoEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.SystemMessageClickEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.TextMessageUserJourneyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.UiMessageYourselfActionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.UiRevokeActionEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WaFsSingleEmojiMessageDailyEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WebcLinkPreviewResponseHandleEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ChatbarInitialState;
import com.github.auties00.cobalt.wire.wam.type.DeepLinkAction;
import com.github.auties00.cobalt.wire.wam.type.DeepLinkOpenFrom;
import com.github.auties00.cobalt.wire.wam.type.DeepLinkType;
import com.github.auties00.cobalt.wire.wam.type.DisappearingModeEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.DisappearingModeSettingEventNameType;
import com.github.auties00.cobalt.wire.wam.type.DmChatPickerEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.DmChatPickerEventNameType;
import com.github.auties00.cobalt.wire.wam.type.ForwardActionUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.GroupRoleType;
import com.github.auties00.cobalt.wire.wam.type.GroupTypeClient;
import com.github.auties00.cobalt.wire.wam.type.KicNuxActionNameType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageContextMenuActionType;
import com.github.auties00.cobalt.wire.wam.type.MessageContextMenuOptionType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.OwnerType;
import com.github.auties00.cobalt.wire.wam.type.PhoneNumHyperlinkActionType;
import com.github.auties00.cobalt.wire.wam.type.PinInChatInteractionType;
import com.github.auties00.cobalt.wire.wam.type.PreviousEphemeralityType;
import com.github.auties00.cobalt.wire.wam.type.QuotedMessageUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.QuotedMessageUserJourneyEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.QuotedMessageUserJourneyNavigateResult;
import com.github.auties00.cobalt.wire.wam.type.ReactionActionType;
import com.github.auties00.cobalt.wire.wam.type.ReactionUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.ReactionUserJourneyEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ShareContentUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.ShareContentUserJourneyEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.SnackbarActionType;
import com.github.auties00.cobalt.wire.wam.type.StwEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.StwFormat;
import com.github.auties00.cobalt.wire.wam.type.StwInteraction;
import com.github.auties00.cobalt.wire.wam.type.SystemMessageCategoryType;
import com.github.auties00.cobalt.wire.wam.type.SystemMessageTypeType;
import com.github.auties00.cobalt.wire.wam.type.TextMessageUserJourneyAction;
import com.github.auties00.cobalt.wire.wam.type.TriggerType;
import com.github.auties00.cobalt.wire.wam.type.TsSurface;
import com.github.auties00.cobalt.wire.wam.type.UiMessageYourselfActionType;
import com.github.auties00.cobalt.wire.wam.type.UiMessageYourselfFunnelName;
import com.github.auties00.cobalt.wire.wam.type.UiRevokeActionType;
import com.github.auties00.cobalt.wire.wam.type.UserJourneyChatType;

import java.util.Objects;
import java.util.UUID;

/**
 * Emits the block of messaging-surface WhatsApp Metrics events that a genuine WhatsApp Web session
 * logs as the user browses and acts inside chats, but for which Cobalt exposes no corresponding
 * feature to trigger a real emission.
 *
 * <p>WhatsApp Web instruments a large family of user-interface journeys around the chat list and the
 * message thread: deep-link opens, disappearing-mode toggles, reaction trays, keep-in-chat prompts,
 * pin-in-chat banners, forward and share sheets, quoted-reply navigation, and the single-emoji daily
 * roll-up. These are all client-side interaction telemetry with no server round-trip, so a headless
 * library such as Cobalt has no organic call site to raise them. A Cobalt session whose telemetry
 * stream never carried any of these interaction events would be trivially separable from a real Web
 * client by the server-side WAM consumers, so this service synthesises the block once per connect,
 * populating each event with plausible, real-looking interaction data (a stable per-session app
 * session id, per-interaction funnel and session ids, host-clock timestamps, and realistic enum,
 * boolean, and count fabrications) sampled from live store state wherever Cobalt can supply it.
 *
 * <p>The single public entry point {@link #emitSessionTelemetry()} fires the whole block on the
 * calling thread. It is intended to run once per successful socket bring-up, mirroring the burst of
 * interaction telemetry a real client accumulates in the minutes after page load. One member of the
 * block, {@link #emitWaFsSingleEmojiMessageDaily()}, is a rolling-day roll-up on a real client;
 * Cobalt folds it into the per-connect burst rather than running a separate daily timer, because the
 * per-connect cadence already keeps the stream populated and the roll-up carries no state that would
 * be double-counted.
 *
 * @implNote
 * This implementation fabricates every interaction detail that has no live Cobalt source and reads
 * the small subset that does: the default disappearing-message duration from the settings sub-store
 * and the resident chat count from the chat sub-store. Ids are minted per emission exactly as WA
 * mints them at the head of each funnel, while the app-scoped session ids are minted once at
 * construction and shared across the journey events, mirroring WA's single per-app session id.
 */
@WhatsAppWebModule(moduleName = "WAWebDeepLinkClickWamEvent")
@WhatsAppWebModule(moduleName = "WAWebDeepLinkOpenWamEvent")
@WhatsAppWebModule(moduleName = "WAWebDeepLinkMsgSentWamEvent")
@WhatsAppWebModule(moduleName = "WAWebHfmTextSearchCompleteWamEvent")
@WhatsAppWebModule(moduleName = "WAWebDisappearingModeSettingChangeWamEvent")
@WhatsAppWebModule(moduleName = "WAWebDisappearingModeSettingEventsWamEvent")
@WhatsAppWebModule(moduleName = "WAWebDisappearingMessageChatPickerWamEvent")
@WhatsAppWebModule(moduleName = "WAWebReactionActionsWamEvent")
@WhatsAppWebModule(moduleName = "WAWebReactionUserJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPsPhoneNumberHyperlinkWamEvent")
@WhatsAppWebModule(moduleName = "WAWebUiRevokeActionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebKeepInChatNotifWamEvent")
@WhatsAppWebModule(moduleName = "WAWebKeepInChatNuxWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSnackbarDeleteUndoWamEvent")
@WhatsAppWebModule(moduleName = "WAWebMessageContextMenuActionsWamEvent")
@WhatsAppWebModule(moduleName = "WAWebUiMessageYourselfActionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebWebcLinkPreviewResponseHandleWamEvent")
@WhatsAppWebModule(moduleName = "WAWebPinInChatInteractionWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSystemMessageClickWamEvent")
@WhatsAppWebModule(moduleName = "WAWebTextMessageUserJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebSearchTheWebFunnelWamEvent")
@WhatsAppWebModule(moduleName = "WAWebShareContentUserJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebQuotedMessageUserJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebForwardActionUserJourneyWamEvent")
@WhatsAppWebModule(moduleName = "WAWebWaFsSingleEmojiMessageDailyWamEvent")
public final class SyntheticMessagingTelemetry {
    /**
     * The default disappearing-message duration reported, in seconds, when the settings sub-store
     * carries no configured default.
     *
     * <p>Set to seven days, the middle option WhatsApp offers in the default-timer picker, so the
     * fabricated disappearing-mode events look like an ordinary account that has enabled the feature.
     */
    private static final int DEFAULT_DISAPPEARING_SECONDS = 604_800;

    /**
     * The number of random bytes hashed into a fabricated hex thread identifier.
     *
     * <p>WhatsApp reports a thread id as an opaque HMAC digest; thirty-two bytes reproduce the
     * sixty-four-hex-character shape of that digest.
     */
    private static final int THREAD_ID_BYTES = 32;

    /**
     * The bound WhatsApp client whose store supplies the live settings and chat state sampled while
     * fabricating the interaction events.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service through which every synthesised interaction event is committed for batched
     * upload.
     */
    private final WamService wamService;

    /**
     * The app-scoped session identifier shared across the user-journey events emitted in a single
     * connect.
     *
     * <p>WhatsApp threads one app session id through every journey logged during an app run; this is
     * minted once per service instance to reproduce that single stable value.
     */
    private final String appSessionId;

    /**
     * The app-scoped unified session identifier shared across the user-journey events emitted in a
     * single connect.
     *
     * <p>WhatsApp carries a second, unified session id alongside the app session id on its newer
     * journey events; this is minted once per service instance to reproduce that stable value.
     */
    private final String unifiedSessionId;

    /**
     * Constructs a new {@code SyntheticMessagingTelemetry} bound to the given client and WAM service.
     *
     * @param client     the WhatsApp client whose store is sampled, must not be {@code null}
     * @param wamService the WAM service used to commit the synthesised events, must not be
     *                   {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SyntheticMessagingTelemetry(LinkedWhatsAppClient client, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.appSessionId = UUID.randomUUID().toString();
        this.unifiedSessionId = UUID.randomUUID().toString();
    }

    /**
     * Emits the full block of synthetic messaging-interaction events once.
     *
     * <p>This is the sole public entry point and is intended to run once per successful socket
     * bring-up. It mints one shared deep-link session id for the linked deep-link click, open, and
     * message-sent sequence (mirroring WhatsApp, which threads a single id through that funnel) and
     * then commits every event in the block through {@link WamService#commit}. The
     * {@link #emitWaFsSingleEmojiMessageDaily()} roll-up is a rolling-day event on a real client and
     * is folded into this per-connect burst rather than driven by a separate daily timer.
     */
    public void emitSessionTelemetry() {
        var deepLinkSessionId = SyntheticTelemetryUtils.newSessionId();
        emitDeepLinkClick(deepLinkSessionId);
        emitDeepLinkOpen(deepLinkSessionId);
        emitDeepLinkMsgSent(deepLinkSessionId);
        emitHfmTextSearchComplete();
        emitDisappearingModeSettingChange();
        emitDisappearingModeSettingEvents();
        emitDisappearingMessageChatPicker();
        emitReactionActions();
        emitReactionUserJourney();
        emitPsPhoneNumberHyperlink();
        emitUiRevokeAction();
        emitKeepInChatNotif();
        emitKeepInChatNux();
        emitSnackbarDeleteUndo();
        emitMessageContextMenuActions();
        emitUiMessageYourselfAction();
        emitWebcLinkPreviewResponseHandle();
        emitPinInChatInteraction();
        emitSystemMessageClick();
        emitTextMessageUserJourney();
        emitSearchTheWebFunnel();
        emitShareContentUserJourney();
        emitQuotedMessageUserJourney();
        emitForwardActionUserJourney();
        emitWaFsSingleEmojiMessageDaily();
    }

    /**
     * Commits a synthetic {@code DeepLinkClick} event (id 1156) describing the shape of a tapped
     * {@code wa.me} chat link.
     *
     * <p>The fabricated link carries a phone number and prefilled text but no username or PIN gate,
     * the most common consumer chat-link shape.
     *
     * @param deepLinkSessionId the shared identifier of the deep-link funnel this click opens
     */
    private void emitDeepLinkClick(String deepLinkSessionId) {
        wamService.commit(new DeepLinkClickEventBuilder()
                .deepLinkHasPhoneNumber(true)
                .deepLinkHasText(true)
                .deepLinkHasUsername(false)
                .deepLinkHasUsernamePin(false)
                .deepLinkRequirePinEntry(false)
                .deepLinkSessionId(deepLinkSessionId)
                .build());
    }

    /**
     * Commits a synthetic {@code DeepLinkOpen} event (id 2136) describing the resolution of a tapped
     * {@code wa.me} chat link into an open chat.
     *
     * <p>The fabricated open resolves an existing consumer contact reached through a web link click.
     *
     * @param deepLinkSessionId the shared identifier of the deep-link funnel this open belongs to
     */
    private void emitDeepLinkOpen(String deepLinkSessionId) {
        wamService.commit(new DeepLinkOpenEventBuilder()
                .campaign("wa_link")
                .deepLinkOpenFrom(DeepLinkOpenFrom.DEEP_LINK_WA_LINK_CLICK)
                .deepLinkSessionId(deepLinkSessionId)
                .deepLinkType(DeepLinkType.DEEP_LINK_CHAT)
                .isContact(true)
                .linkOwnerType(OwnerType.CONSUMER)
                .sourceSurface(1)
                .build());
    }

    /**
     * Commits a synthetic {@code DeepLinkMsgSent} event (id 3198) recording that the deep-link funnel
     * concluded with a sent message.
     *
     * @param deepLinkSessionId the shared identifier of the deep-link funnel that produced the send
     */
    private void emitDeepLinkMsgSent(String deepLinkSessionId) {
        wamService.commit(new DeepLinkMsgSentEventBuilder()
                .deepLinkAction(DeepLinkAction.MSG_SENT)
                .deepLinkSessionId(deepLinkSessionId)
                .build());
    }

    /**
     * Commits a synthetic {@code HfmTextSearchComplete} event (id 2186) marking completion of a
     * held-for-media text search pass.
     *
     * <p>The event carries no fields; its presence on the wire is the sole signal, so the bare
     * builder is committed.
     */
    private void emitHfmTextSearchComplete() {
        wamService.commit(new HfmTextSearchCompleteEventBuilder().build());
    }

    /**
     * Commits a synthetic {@code DisappearingModeSettingChange} event (id 3056) describing a
     * successful account-level default-timer change from off to the configured default.
     *
     * <p>The previous duration reads zero (feature off) and the new duration is sourced from the live
     * settings default; the after-read and error fields are left unset because WhatsApp populates
     * them only on the after-read and failure paths respectively.
     */
    private void emitDisappearingModeSettingChange() {
        wamService.commit(new DisappearingModeSettingChangeEventBuilder()
                .previousEphemeralityDuration(0)
                .newEphemeralityDuration(defaultDisappearingSeconds())
                .lastToggleTimestamp(nowMillis())
                .disappearingModeEntryPoint(DisappearingModeEntryPointType.ACCOUNT_SETTINGS)
                .isAfterRead(false)
                .isSuccess(true)
                .previousEphemeralityType(PreviousEphemeralityType.DISAPPEARING_MESSAGE)
                .build());
    }

    /**
     * Commits a synthetic {@code DisappearingModeSettingEvents} event (id 3446) recording that the
     * account default-timer was set from the settings surface.
     *
     * <p>The previous duration reads zero (feature off) and the new duration is sourced from the live
     * settings default.
     */
    private void emitDisappearingModeSettingEvents() {
        wamService.commit(new DisappearingModeSettingEventsEventBuilder()
                .disappearingModeSettingEventName(DisappearingModeSettingEventNameType.DEFAULT_MESSAGE_TIMER_SET)
                .lastToggleTimestamp(nowMillis())
                .newEphemeralityDuration(defaultDisappearingSeconds())
                .previousEphemeralityDuration(0)
                .disappearingModeEntryPoint(DisappearingModeEntryPointType.ACCOUNT_SETTINGS)
                .isAfterRead(false)
                .build());
    }

    /**
     * Commits a synthetic {@code DisappearingMessageChatPicker} event (id 3398) describing a
     * multi-chat apply of the default timer from the chat picker.
     *
     * <p>The picker's total-chats field is sourced from the live resident chat count; the selected
     * and newly-ephemeral counts are fabricated to a small, self-consistent multi-select.
     */
    private void emitDisappearingMessageChatPicker() {
        wamService.commit(new DisappearingMessageChatPickerEventBuilder()
                .chatsSelected(3)
                .dmChatPickerEntryPoint(DmChatPickerEntryPointType.DEFAULT_MODE_SETTING)
                .dmChatPickerEventName(DmChatPickerEventNameType.CHAT_PICKER_CHATS_SELECTED)
                .ephemeralityDuration(defaultDisappearingSeconds())
                .groupChatsSelected(1)
                .groupSizeDistributionJson("{\"2\":1}")
                .newlyEphemeralChats(3)
                .totalChatsInChatPicker(plausibleChatTotal())
                .build());
    }

    /**
     * Commits a synthetic {@code ReactionActions} event (id 3184) recording an update of an own
     * reaction on an individual text message.
     */
    private void emitReactionActions() {
        wamService.commit(new ReactionActionsEventBuilder()
                .messageType(MessageType.INDIVIDUAL)
                .reactionAction(ReactionActionType.UPDATE)
                .mediaType(MediaType.NONE)
                .build());
    }

    /**
     * Commits a synthetic {@code ReactionUserJourney} event (id 5752) describing the selection of a
     * reaction from the message-hold tray on an individual chat.
     */
    private void emitReactionUserJourney() {
        wamService.commit(new ReactionUserJourneyEventBuilder()
                .appSessionId(appSessionId)
                .messageHasOwnReaction(false)
                .messageHasReaction(true)
                .messageMediaType(MediaType.NONE)
                .messageType(MessageType.INDIVIDUAL)
                .reactionUserJourneyAction(ReactionUserJourneyAction.REACTION_SELECT)
                .reactionUserJourneyEntryPoint(ReactionUserJourneyEntryPoint.MESSAGE_HOLD)
                .uiSurface(TsSurface.CHAT_THREAD)
                .unifiedSessionId(unifiedSessionId)
                .userJourneyChatType(UserJourneyChatType.INDIVIDUAL)
                .userJourneyEventMs(nowMillis())
                .userJourneyFunnelId(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Commits a synthetic {@code PsPhoneNumberHyperlink} event (id 3266) recording a click on an
     * in-message phone-number hyperlink whose owner is on WhatsApp.
     *
     * <p>This event is declared on the private telemetry channel; {@link WamService#commit} routes it
     * there and mints its private-stats id automatically.
     */
    private void emitPsPhoneNumberHyperlink() {
        wamService.commit(new PsPhoneNumberHyperlinkEventBuilder()
                .isPhoneNumHyperlinkOwner(false)
                .phoneNumHyperlinkAction(PhoneNumHyperlinkActionType.CLICK_PHONE_NUM_HYPERLINK)
                .phoneNumberStatusOnWa(true)
                .sequenceNumber(1)
                .build());
    }

    /**
     * Commits a synthetic {@code UiRevokeAction} event (id 3298) recording a delete-for-everyone
     * confirmation from the message context menu.
     */
    private void emitUiRevokeAction() {
        wamService.commit(new UiRevokeActionEventBuilder()
                .messageAction(UiRevokeActionType.DELETE_FOR_EVERYONE_SELECTED)
                .uiRevokeActionDuration(1_200)
                .uiRevokeActionSessionId(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Commits a synthetic {@code KeepInChatNotif} event (id 3484) reporting the rolling counters for
     * keep-in-chat notification impressions and taps.
     */
    private void emitKeepInChatNotif() {
        wamService.commit(new KeepInChatNotifEventBuilder()
                .kicGroupNotificationTaps(2)
                .kicGroupNotifications(5)
                .kicNotificationTaps(3)
                .kicNotifications(9)
                .build());
    }

    /**
     * Commits a synthetic {@code KeepInChatNux} event (id 3486) recording an impression of the
     * keep-in-chat first-run education prompt on chat entry.
     *
     * <p>The chat's ephemerality duration is sourced from the live settings default.
     */
    private void emitKeepInChatNux() {
        wamService.commit(new KeepInChatNuxEventBuilder()
                .chatEphemeralityDuration(defaultDisappearingSeconds())
                .kicNuxActionName(KicNuxActionNameType.KIC_NUX_IMPRESSION)
                .threadId(SyntheticTelemetryUtils.randomHexLower(THREAD_ID_BYTES))
                .trigger(TriggerType.CHAT_ENTRY)
                .build());
    }

    /**
     * Commits a synthetic {@code SnackbarDeleteUndo} event (id 3628) recording the display of the
     * delete-undo snackbar after a single message deletion in an individual chat.
     */
    private void emitSnackbarDeleteUndo() {
        wamService.commit(new SnackbarDeleteUndoEventBuilder()
                .isAGroup(false)
                .mediaType(MediaType.NONE)
                .messagesUndeleted(1)
                .snackbarActionType(SnackbarActionType.SNACKBAR_SHOWN)
                .threadId(SyntheticTelemetryUtils.randomHexLower(THREAD_ID_BYTES))
                .build());
    }

    /**
     * Commits a synthetic {@code MessageContextMenuActions} event (id 3694) recording a reply option
     * chosen from the message context menu on an own individual-chat message.
     */
    private void emitMessageContextMenuActions() {
        wamService.commit(new MessageContextMenuActionsEventBuilder()
                .isAGroup(false)
                .isMultiAction(false)
                .isOriginalSender(true)
                .messageContextMenuAction(MessageContextMenuActionType.CLICK)
                .messageContextMenuOption(MessageContextMenuOptionType.REPLY)
                .build());
    }

    /**
     * Commits a synthetic {@code UiMessageYourselfAction} event (id 3780) recording the reopening of
     * the message-yourself note-to-self chat from the new-chat funnel.
     */
    private void emitUiMessageYourselfAction() {
        wamService.commit(new UiMessageYourselfActionEventBuilder()
                .uiMessageYourselfActionSessionId(SyntheticTelemetryUtils.newSessionId())
                .uiMessageYourselfActionType(UiMessageYourselfActionType.EXISTING_NTS_OPENED)
                .uiMessageYourselfFunnelName(UiMessageYourselfFunnelName.NEW_CHAT)
                .build());
    }

    /**
     * Commits a synthetic {@code WebcLinkPreviewResponseHandle} event (id 3860) recording a
     * successful high-quality link-preview fetch.
     */
    private void emitWebcLinkPreviewResponseHandle() {
        wamService.commit(new WebcLinkPreviewResponseHandleEventBuilder()
                .didRespondHqPreview(true)
                .isPreviewSuccess(true)
                .previewDurationMs(240)
                .previewSessionId(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Commits a synthetic {@code PinInChatInteraction} event (id 4436) recording a self pin created
     * by an admin from the pin banner in a regular group.
     */
    private void emitPinInChatInteraction() {
        wamService.commit(new PinInChatInteractionEventBuilder()
                .groupRole(GroupRoleType.ADMIN)
                .groupSize(12)
                .groupTypeClient(GroupTypeClient.REGULAR_GROUP)
                .isAGroup(true)
                .isSelfPin(true)
                .mediaType(MediaType.NONE)
                .pinCount(1)
                .pinInChatInteractionType(PinInChatInteractionType.TAP_ON_BANNER)
                .pinIndex(0)
                .build());
    }

    /**
     * Commits a synthetic {@code SystemMessageClick} event (id 5082) recording a tap on the
     * end-to-end encryption privacy system message in an individual chat.
     */
    private void emitSystemMessageClick() {
        wamService.commit(new SystemMessageClickEventBuilder()
                .isAGroup(false)
                .isANewThread(false)
                .systemMessageCategory(SystemMessageCategoryType.PRIVACY)
                .systemMessageType(SystemMessageTypeType.E2E_ENCRYPTED_MESSAGES)
                .build());
    }

    /**
     * Commits a synthetic {@code TextMessageUserJourney} event (id 5404) recording a text message
     * sent from an empty composer in an individual chat thread.
     */
    private void emitTextMessageUserJourney() {
        wamService.commit(new TextMessageUserJourneyEventBuilder()
                .appSessionId(appSessionId)
                .chatbarInitialState(ChatbarInitialState.EMPTY)
                .textMessageUserJourneyAction(TextMessageUserJourneyAction.SENT)
                .textMessageUserJourneyContainsQuotedItem(false)
                .uiSurface(TsSurface.CHAT_THREAD)
                .unifiedSessionId(unifiedSessionId)
                .userJourneyChatType(UserJourneyChatType.INDIVIDUAL)
                .userJourneyEventMs(nowMillis())
                .userJourneyFunnelId(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Commits a synthetic {@code SearchTheWebFunnel} event (id 5702) recording that the search-the-web
     * entry point was surfaced on a long-pressed URL in an individual chat.
     *
     * <p>The image-search failure field is left unset because WhatsApp populates it only on the
     * image-search failure path.
     */
    private void emitSearchTheWebFunnel() {
        wamService.commit(new SearchTheWebFunnelEventBuilder()
                .messageType(MessageType.INDIVIDUAL)
                .stwEntryPoint(StwEntryPoint.URL_LONG_PRESS)
                .stwFormat(StwFormat.SINGLE_LINK)
                .stwInteraction(StwInteraction.ENTRY_POINT_SURFACED)
                .build());
    }

    /**
     * Commits a synthetic {@code ShareContentUserJourney} event (id 5734) recording a single image
     * shared to two recipients from the message context menu.
     */
    private void emitShareContentUserJourney() {
        wamService.commit(new ShareContentUserJourneyEventBuilder()
                .appSessionId(appSessionId)
                .captionAdded(false)
                .forwardUserJourneyFunnelId(SyntheticTelemetryUtils.newSessionId())
                .hasBotImagineImages(false)
                .hasCaptionPrefilled(false)
                .hasFiles(false)
                .hasImages(true)
                .hasLinks(false)
                .hasMusic(false)
                .hasStatusRecipient(false)
                .hasVideo(false)
                .isForwardFlow(false)
                .mediaCount(1)
                .messageSelectedCount(1)
                .numberOfRecipients(2)
                .prefilledCaptionRemoved(false)
                .shareContentUserJourneyAction(ShareContentUserJourneyAction.CONTENT_SHARED)
                .shareContentUserJourneyEntryPoint(ShareContentUserJourneyEntryPoint.CONTEXT_MENU)
                .shareContentUserJourneySurfaceEntryPoint(TsSurface.CHAT_THREAD)
                .uiSurface(TsSurface.CHAT_THREAD)
                .unifiedSessionId(unifiedSessionId)
                .userJourneyEventMs(nowMillis())
                .userJourneyFunnelId(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Commits a synthetic {@code QuotedMessageUserJourney} event (id 6444) recording a quoted reply
     * sent from a swipe-to-reply gesture in an individual chat.
     */
    private void emitQuotedMessageUserJourney() {
        wamService.commit(new QuotedMessageUserJourneyEventBuilder()
                .appSessionId(appSessionId)
                .chatbarInitialState(ChatbarInitialState.EMPTY)
                .messageMediaType(MediaType.NONE)
                .messageType(MessageType.INDIVIDUAL)
                .quotedMediaType(MediaType.NONE)
                .quotedMessageTypeEnum(MessageType.INDIVIDUAL)
                .quotedMessageUserJourneyAction(QuotedMessageUserJourneyAction.QUOTED_MESSAGE_SENT)
                .quotedMessageUserJourneyEntryPoint(QuotedMessageUserJourneyEntryPoint.SWIPED_TO_REPLY)
                .quotedMessageUserJourneyNavigateResult(QuotedMessageUserJourneyNavigateResult.NAVIGATE_SUCCESS_SAME_CHAT)
                .uiSurface(TsSurface.CHAT_THREAD)
                .unifiedSessionId(unifiedSessionId)
                .userJourneyChatType(UserJourneyChatType.INDIVIDUAL)
                .userJourneyFunnelId(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Commits a synthetic {@code ForwardActionUserJourney} event (id 6506) recording a forward chosen
     * from the message context menu on an own individual-chat message.
     */
    private void emitForwardActionUserJourney() {
        wamService.commit(new ForwardActionUserJourneyEventBuilder()
                .appSessionId(appSessionId)
                .forwardActionUserJourneyAction(ForwardActionUserJourneyAction.FORWARD_TAPPED_IN_CONTEXT_MENU)
                .forwardUserJourneyFunnelId(SyntheticTelemetryUtils.newSessionId())
                .messageIsFromMe(true)
                .messageMediaType(MediaType.NONE)
                .messageType(MessageType.INDIVIDUAL)
                .uiSurface(TsSurface.CHAT_THREAD)
                .unifiedSessionId(unifiedSessionId)
                .userJourneyChatType(UserJourneyChatType.INDIVIDUAL)
                .userJourneyEventMs(nowMillis())
                .userJourneyFunnelId(SyntheticTelemetryUtils.newSessionId())
                .build());
    }

    /**
     * Commits a synthetic {@code WaFsSingleEmojiMessageDaily} event (id 5602) reporting the rolling
     * daily counters for single-emoji and animated-emoji message activity.
     *
     * <p>This is a rolling-day roll-up on a real client; it is folded into the per-connect burst
     * driven by {@link #emitSessionTelemetry()} rather than run on a separate daily timer, and every
     * counter is fabricated to a plausible day of light emoji usage.
     */
    private void emitWaFsSingleEmojiMessageDaily() {
        wamService.commit(new WaFsSingleEmojiMessageDailyEventBuilder()
                .animatedEmojiEnabled(true)
                .animatedEmojiReceiveCnt(4)
                .animatedEmojiSendCnt(2)
                .emojiClickCnt(11)
                .emojiReplyCount(3)
                .pauseAnimationCnt(1)
                .replayAnimationCnt(2)
                .singleEmojiReceiveCnt(7)
                .singleEmojiSendCnt(5)
                .build());
    }



    /**
     * Returns the current host wall-clock time in milliseconds since the epoch.
     *
     * <p>This is the source for the millisecond timestamp fields WhatsApp populates with
     * {@code Date.now()} (the journey event clocks and the disappearing-mode toggle timestamps).
     *
     * @return the current epoch time in milliseconds
     */
    private long nowMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Resolves the default disappearing-message duration reported by the disappearing-mode events.
     *
     * <p>The value is read from the live settings sub-store when a default timer is configured, and
     * falls back to {@link #DEFAULT_DISAPPEARING_SECONDS} otherwise so the fabricated events always
     * carry a plausible enabled duration.
     *
     * @return the default disappearing duration in seconds
     */
    private int defaultDisappearingSeconds() {
        return client.store()
                .settingsStore()
                .disappearingMode()
                .map(mode -> (int) mode.duration().toSeconds())
                .filter(seconds -> seconds > 0)
                .orElse(DEFAULT_DISAPPEARING_SECONDS);
    }

    /**
     * Estimates a plausible total chat count for the disappearing-message chat picker.
     *
     * <p>The value is the live resident chat count from the chat sub-store, floored at the fabricated
     * multi-select size so the reported total is never smaller than the number of chats the synthetic
     * picker interaction claims to have selected.
     *
     * @return the reported total chat count for the chat picker
     */
    private long plausibleChatTotal() {
        var resident = client.store().chatStore().chats().size();
        return Math.max(resident, 24);
    }
}
