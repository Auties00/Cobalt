package com.github.auties00.cobalt.wire.linked.sync.action;

import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilities;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.setting.ChatLockSettings;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.AiThreadRenameAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.BotWelcomeRequestAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.MaibaAIFeaturesControlAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.UGCBotAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastCampaignAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastInsightsAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastListAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastAssociationAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CustomerDataAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CtwaPerCustomerDataSharingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.MarketingMessageAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.MarketingMessageBroadcastAction;
import com.github.auties00.cobalt.wire.linked.sync.action.call.*;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.*;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.*;
import com.github.auties00.cobalt.wire.linked.sync.action.device.*;
import com.github.auties00.cobalt.wire.linked.sync.action.media.*;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.CustomPaymentMethodsAction;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.MerchantPaymentPartnerAction;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentInfoAction;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.PaymentTosAction;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingChannelsPersonalisedRecommendationAction;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingDisableLinkPreviewsAction;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingRelayAllCalls;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivateProcessingSettingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.*;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Tagged union payload carried by every app state sync mutation.
 *
 * <p>At most one of the action fields is populated in any given instance,
 * identifying the concrete action that the mutation represents. The
 * timestamp field is shared by every action and records when the mutation
 * was produced. The {@link #action()} accessor inspects the populated field
 * and exposes the contained action through the common {@link SyncAction}
 * supertype, letting callers dispatch on the action without having to test
 * every field individually.
 */
@ProtobufMessage(name = "SyncActionValue")
public final class SyncActionValue {
    /**
     * Wall clock time at which the mutation was produced on the originating
     * device, encoded on the wire in whole seconds.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant timestamp;

    /**
     * Concrete action when the mutation represents a message star toggle.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final StarAction starAction;

    /**
     * Concrete action when the mutation represents a contact name edit.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final ContactAction contactAction;

    /**
     * Concrete action when the mutation represents a chat mute.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final MuteAction muteAction;

    /**
     * Concrete action when the mutation represents a chat pin toggle.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final PinAction pinAction;

    /**
     * Concrete action when the mutation represents a push name update.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final PushNameSetting pushNameSetting;

    /**
     * Concrete action when the mutation represents a quick reply template
     * edit.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final QuickReplyAction quickReplyAction;

    /**
     * Concrete action when the mutation represents a recent emoji weight
     * update.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.MESSAGE)
    final RecentEmojiWeightsAction recentEmojiWeightsAction;

    /**
     * Concrete action when the mutation represents a label create, rename,
     * or delete.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.MESSAGE)
    final LabelEditAction labelEditAction;

    /**
     * Concrete action when the mutation represents a label association
     * change.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    final LabelAssociationAction labelAssociationAction;

    /**
     * Concrete action when the mutation represents a locale update.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    final LocaleSetting localeSetting;

    /**
     * Concrete action when the mutation represents a chat archive toggle.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    final ArchiveChatAction archiveChatAction;

    /**
     * Concrete action when the mutation represents a delete for me action
     * on a message.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.MESSAGE)
    final DeleteMessageForMeAction deleteMessageForMeAction;

    /**
     * Concrete action when the mutation represents a disappearing messages
     * key expiration update.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.MESSAGE)
    final KeyExpirationAction keyExpirationAction;

    /**
     * Concrete action when the mutation represents a mark chat read or
     * unread toggle.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.MESSAGE)
    final MarkChatAsReadAction markChatAsReadAction;

    /**
     * Concrete action when the mutation represents a clear chat messages
     * action.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.MESSAGE)
    final ClearChatAction clearChatAction;

    /**
     * Concrete action when the mutation represents a delete chat action.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.MESSAGE)
    final DeleteChatAction deleteChatAction;

    /**
     * Concrete action when the mutation represents a bulk unarchive chats
     * setting change.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.MESSAGE)
    final UnarchiveChatsSetting unarchiveChatsSetting;

    /**
     * Concrete action when the mutation represents a primary device feature
     * toggle.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.MESSAGE)
    final PrimaryFeatureAction primaryFeatureAction;

    /**
     * Concrete action when the mutation represents an Android unsupported
     * actions marker.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.MESSAGE)
    final AndroidUnsupportedActions androidUnsupportedActions;

    /**
     * Concrete action when the mutation represents a business agent change.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.MESSAGE)
    final AgentAction agentAction;

    /**
     * Concrete action when the mutation represents a user status mute
     * change.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.MESSAGE)
    final UserStatusMuteAction userStatusMuteAction;

    /**
     * Concrete action when the mutation represents a time format preference
     * change.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.MESSAGE)
    final TimeFormatAction timeFormatAction;

    /**
     * Concrete action when the mutation represents a New User Experience
     * flag update.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.MESSAGE)
    final NuxAction nuxAction;

    /**
     * Concrete action when the mutation represents a primary device version
     * report.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.MESSAGE)
    final PrimaryVersionAction primaryVersionAction;

    /**
     * Concrete action when the mutation represents a sticker pack
     * installation change.
     */
    @ProtobufProperty(index = 33, type = ProtobufType.MESSAGE)
    final StickerAction stickerAction;

    /**
     * Concrete action when the mutation represents a recent sticker
     * removal.
     */
    @ProtobufProperty(index = 34, type = ProtobufType.MESSAGE)
    final RemoveRecentStickerAction removeRecentStickerAction;

    /**
     * Concrete action when the mutation represents a chat assignment to an
     * agent.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.MESSAGE)
    final ChatAssignmentAction chatAssignment;

    /**
     * Concrete action when the mutation represents the opened status of a
     * chat assignment.
     */
    @ProtobufProperty(index = 36, type = ProtobufType.MESSAGE)
    final ChatAssignmentOpenedStatusAction chatAssignmentOpenedStatus;

    /**
     * Concrete action when the mutation associates a phone number based
     * chat with its lid based counterpart.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.MESSAGE)
    final PnForLidChatAction pnForLidChatAction;

    /**
     * Concrete action when the mutation represents a marketing message
     * template update.
     */
    @ProtobufProperty(index = 38, type = ProtobufType.MESSAGE)
    final MarketingMessageAction marketingMessageAction;

    /**
     * Concrete action when the mutation represents a marketing message
     * broadcast update.
     */
    @ProtobufProperty(index = 39, type = ProtobufType.MESSAGE)
    final MarketingMessageBroadcastAction marketingMessageBroadcastAction;

    /**
     * Concrete action when the mutation represents an external web beta
     * toggle.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.MESSAGE)
    final ExternalWebBetaAction externalWebBetaAction;

    /**
     * Concrete action when the mutation represents the relay all calls
     * privacy setting.
     */
    @ProtobufProperty(index = 41, type = ProtobufType.MESSAGE)
    final PrivacySettingRelayAllCalls privacySettingRelayAllCalls;

    /**
     * Concrete action when the mutation represents a call log entry.
     */
    @ProtobufProperty(index = 42, type = ProtobufType.MESSAGE)
    final CallLogAction callLogAction;

    /**
     * Concrete action when the mutation represents a user generated content
     * bot update.
     */
    @ProtobufProperty(index = 43, type = ProtobufType.MESSAGE)
    final UGCBotAction ugcBotAction;

    /**
     * Concrete action when the mutation represents a status privacy setting
     * change.
     */
    @ProtobufProperty(index = 44, type = ProtobufType.MESSAGE)
    final StatusPrivacyAction statusPrivacy;

    /**
     * Concrete action when the mutation represents a bot welcome request
     * update.
     */
    @ProtobufProperty(index = 45, type = ProtobufType.MESSAGE)
    final BotWelcomeRequestAction botWelcomeRequestAction;

    /**
     * Concrete action when the mutation deletes a single call log entry.
     */
    @ProtobufProperty(index = 46, type = ProtobufType.MESSAGE)
    final DeleteIndividualCallLogAction deleteIndividualCallLog;

    /**
     * Concrete action when the mutation represents a label reordering.
     */
    @ProtobufProperty(index = 47, type = ProtobufType.MESSAGE)
    final LabelReorderingAction labelReorderingAction;

    /**
     * Concrete action when the mutation represents a payment info update.
     */
    @ProtobufProperty(index = 48, type = ProtobufType.MESSAGE)
    final PaymentInfoAction paymentInfoAction;

    /**
     * Concrete action when the mutation represents a custom payment methods
     * list update.
     */
    @ProtobufProperty(index = 49, type = ProtobufType.MESSAGE)
    final CustomPaymentMethodsAction customPaymentMethodsAction;

    /**
     * Concrete action when the mutation represents a chat lock toggle.
     */
    @ProtobufProperty(index = 50, type = ProtobufType.MESSAGE)
    final LockChatAction lockChatAction;

    /**
     * Concrete action when the mutation represents global chat lock
     * settings.
     */
    @ProtobufProperty(index = 51, type = ProtobufType.MESSAGE)
    ChatLockSettings chatLockSettings;

    /**
     * Concrete action when the mutation represents a WAMO user identifier
     * update.
     */
    @ProtobufProperty(index = 52, type = ProtobufType.MESSAGE)
    final WamoUserIdentifierAction wamoUserIdentifierAction;

    /**
     * Concrete action when the mutation represents a disable link previews
     * privacy setting change.
     */
    @ProtobufProperty(index = 53, type = ProtobufType.MESSAGE)
    final PrivacySettingDisableLinkPreviewsAction privacySettingDisableLinkPreviewsAction;

    /**
     * Concrete action when the mutation represents device capability
     * changes for the account.
     */
    @ProtobufProperty(index = 54, type = ProtobufType.MESSAGE)
    DeviceCapabilities deviceCapabilities;

    /**
     * Concrete action when the mutation represents a personal note edit.
     */
    @ProtobufProperty(index = 55, type = ProtobufType.MESSAGE)
    final NoteEditAction noteEditAction;

    /**
     * Concrete action when the mutation represents a favorites list update.
     */
    @ProtobufProperty(index = 56, type = ProtobufType.MESSAGE)
    final FavoritesAction favoritesAction;

    /**
     * Concrete action when the mutation represents a merchant payment
     * partner update.
     */
    @ProtobufProperty(index = 57, type = ProtobufType.MESSAGE)
    final MerchantPaymentPartnerAction merchantPaymentPartnerAction;

    /**
     * Concrete action when the mutation represents a Waffle account link
     * state change.
     */
    @ProtobufProperty(index = 58, type = ProtobufType.MESSAGE)
    final WaffleAccountLinkStateAction waffleAccountLinkStateAction;

    /**
     * Concrete action when the mutation represents a username chat start
     * mode toggle.
     */
    @ProtobufProperty(index = 59, type = ProtobufType.MESSAGE)
    final UsernameChatStartModeAction usernameChatStartMode;

    /**
     * Concrete action when the mutation represents a notification activity
     * setting change.
     */
    @ProtobufProperty(index = 60, type = ProtobufType.MESSAGE)
    final NotificationActivitySettingAction notificationActivitySettingAction;

    /**
     * Concrete action when the mutation represents a lid based contact
     * update.
     */
    @ProtobufProperty(index = 61, type = ProtobufType.MESSAGE)
    final LidContactAction lidContactAction;

    /**
     * Concrete action when the mutation represents per customer data
     * sharing for click to WhatsApp ads.
     */
    @ProtobufProperty(index = 62, type = ProtobufType.MESSAGE)
    final CtwaPerCustomerDataSharingAction ctwaPerCustomerDataSharingAction;

    /**
     * Concrete action when the mutation represents a payment terms of
     * service acceptance.
     */
    @ProtobufProperty(index = 63, type = ProtobufType.MESSAGE)
    final PaymentTosAction paymentTosAction;

    /**
     * Concrete action when the mutation represents the channels
     * personalised recommendation privacy setting.
     */
    @ProtobufProperty(index = 64, type = ProtobufType.MESSAGE)
    final PrivacySettingChannelsPersonalisedRecommendationAction privacySettingChannelsPersonalisedRecommendationAction;

    /**
     * Concrete action when the mutation represents a business broadcast
     * association update.
     */
    @ProtobufProperty(index = 65, type = ProtobufType.MESSAGE)
    final BusinessBroadcastAssociationAction businessBroadcastAssociationAction;

    /**
     * Concrete action when the mutation represents a detected outcomes
     * status change.
     */
    @ProtobufProperty(index = 66, type = ProtobufType.MESSAGE)
    final DetectedOutcomesStatusAction detectedOutcomesStatusAction;

    /**
     * Concrete action when the mutation represents a Maiba AI features
     * control toggle.
     */
    @ProtobufProperty(index = 68, type = ProtobufType.MESSAGE)
    final MaibaAIFeaturesControlAction maibaAiFeaturesControlAction;

    /**
     * Concrete action when the mutation represents a business broadcast
     * list update.
     */
    @ProtobufProperty(index = 69, type = ProtobufType.MESSAGE)
    final BusinessBroadcastListAction businessBroadcastListAction;

    /**
     * Concrete action when the mutation represents a music service user id
     * update.
     */
    @ProtobufProperty(index = 70, type = ProtobufType.MESSAGE)
    final MusicUserIdAction musicUserIdAction;

    /**
     * Concrete action when the mutation represents a status post opt in
     * notification preferences change.
     */
    @ProtobufProperty(index = 71, type = ProtobufType.MESSAGE)
    final StatusPostOptInNotificationPreferencesAction statusPostOptInNotificationPreferencesAction;

    /**
     * Concrete action when the mutation represents an account avatar
     * update.
     */
    @ProtobufProperty(index = 72, type = ProtobufType.MESSAGE)
    final AvatarUpdatedAction avatarUpdatedAction;

    /**
     * Concrete action when the mutation represents a private processing
     * setting change.
     */
    @ProtobufProperty(index = 74, type = ProtobufType.MESSAGE)
    final PrivateProcessingSettingAction privateProcessingSettingAction;

    /**
     * Concrete action when the mutation represents a newsletter saved
     * interests update.
     */
    @ProtobufProperty(index = 75, type = ProtobufType.MESSAGE)
    final NewsletterSavedInterestsAction newsletterSavedInterestsAction;

    /**
     * Concrete action when the mutation renames an AI thread.
     */
    @ProtobufProperty(index = 76, type = ProtobufType.MESSAGE)
    final AiThreadRenameAction aiThreadRenameAction;

    /**
     * Concrete action when the mutation represents an interactive message
     * action.
     */
    @ProtobufProperty(index = 77, type = ProtobufType.MESSAGE)
    final InteractiveMessageAction interactiveMessageAction;

    /**
     * Concrete action when the mutation represents a generic settings sync
     * update.
     */
    @ProtobufProperty(index = 78, type = ProtobufType.MESSAGE)
    final SettingsSyncAction settingsSyncAction;

    /**
     * Concrete action when the mutation represents an outbound contact
     * sync.
     */
    @ProtobufProperty(index = 79, type = ProtobufType.MESSAGE)
    final OutContactAction outContactAction;

    /**
     * Concrete action when the mutation represents a Noise chain token salt
     * sync update.
     */
    @ProtobufProperty(index = 80, type = ProtobufType.MESSAGE)
    final NctSaltSyncAction nctSaltSyncAction;

    /**
     * Concrete action when the mutation represents a business broadcast
     * campaign update.
     */
    @ProtobufProperty(index = 81, type = ProtobufType.MESSAGE)
    final BusinessBroadcastCampaignAction businessBroadcastCampaignAction;

    /**
     * Concrete action when the mutation represents a business broadcast
     * insights update.
     */
    @ProtobufProperty(index = 82, type = ProtobufType.MESSAGE)
    final BusinessBroadcastInsightsAction businessBroadcastInsightsAction;

    /**
     * Concrete action when the mutation represents a business customer data
     * update.
     */
    @ProtobufProperty(index = 83, type = ProtobufType.MESSAGE)
    final CustomerDataAction customerDataAction;

    /**
     * Concrete action when the mutation represents a subscriptions sync v2
     * update.
     */
    @ProtobufProperty(index = 84, type = ProtobufType.MESSAGE)
    final SubscriptionsSyncV2Action subscriptionsSyncV2Action;

    /**
     * Constructs a new sync action value populated with the given fields.
     *
     * <p>At most one of the action fields is expected to be non {@code null}
     * on any given instance; the {@code timestamp} is always set to the
     * mutation's wall clock time.
     *
     * @param timestamp the wall clock time of the mutation
     * @param starAction the star action payload, or {@code null}
     * @param contactAction the contact action payload, or {@code null}
     * @param muteAction the mute action payload, or {@code null}
     * @param pinAction the pin action payload, or {@code null}
     * @param pushNameSetting the push name setting payload, or {@code null}
     * @param quickReplyAction the quick reply action payload, or {@code null}
     * @param recentEmojiWeightsAction the recent emoji weights payload, or {@code null}
     * @param labelEditAction the label edit action payload, or {@code null}
     * @param labelAssociationAction the label association payload, or {@code null}
     * @param localeSetting the locale setting payload, or {@code null}
     * @param archiveChatAction the archive chat action payload, or {@code null}
     * @param deleteMessageForMeAction the delete for me action payload, or {@code null}
     * @param keyExpirationAction the key expiration action payload, or {@code null}
     * @param markChatAsReadAction the mark chat as read payload, or {@code null}
     * @param clearChatAction the clear chat action payload, or {@code null}
     * @param deleteChatAction the delete chat action payload, or {@code null}
     * @param unarchiveChatsSetting the unarchive chats setting payload, or {@code null}
     * @param primaryFeatureAction the primary feature action payload, or {@code null}
     * @param androidUnsupportedActions the Android unsupported actions payload, or {@code null}
     * @param agentAction the agent action payload, or {@code null}
     * @param userStatusMuteAction the user status mute payload, or {@code null}
     * @param timeFormatAction the time format action payload, or {@code null}
     * @param nuxAction the NUX flag action payload, or {@code null}
     * @param primaryVersionAction the primary version action payload, or {@code null}
     * @param stickerAction the sticker action payload, or {@code null}
     * @param removeRecentStickerAction the remove recent sticker payload, or {@code null}
     * @param chatAssignment the chat assignment action payload, or {@code null}
     * @param chatAssignmentOpenedStatus the chat assignment opened status payload, or {@code null}
     * @param pnForLidChatAction the phone number for lid chat payload, or {@code null}
     * @param marketingMessageAction the marketing message action payload, or {@code null}
     * @param marketingMessageBroadcastAction the marketing message broadcast payload, or {@code null}
     * @param externalWebBetaAction the external web beta action payload, or {@code null}
     * @param privacySettingRelayAllCalls the relay all calls privacy payload, or {@code null}
     * @param callLogAction the call log action payload, or {@code null}
     * @param ugcBotAction the user generated content bot payload, or {@code null}
     * @param statusPrivacy the status privacy action payload, or {@code null}
     * @param botWelcomeRequestAction the bot welcome request payload, or {@code null}
     * @param deleteIndividualCallLog the delete call log entry payload, or {@code null}
     * @param labelReorderingAction the label reordering action payload, or {@code null}
     * @param paymentInfoAction the payment info action payload, or {@code null}
     * @param customPaymentMethodsAction the custom payment methods payload, or {@code null}
     * @param lockChatAction the chat lock action payload, or {@code null}
     * @param chatLockSettings the chat lock settings payload, or {@code null}
     * @param wamoUserIdentifierAction the WAMO user identifier payload, or {@code null}
     * @param privacySettingDisableLinkPreviewsAction the disable link previews privacy payload, or {@code null}
     * @param deviceCapabilities the device capabilities payload, or {@code null}
     * @param noteEditAction the note edit action payload, or {@code null}
     * @param favoritesAction the favorites action payload, or {@code null}
     * @param merchantPaymentPartnerAction the merchant payment partner payload, or {@code null}
     * @param waffleAccountLinkStateAction the Waffle account link state payload, or {@code null}
     * @param usernameChatStartMode the username chat start mode payload, or {@code null}
     * @param notificationActivitySettingAction the notification activity setting payload, or {@code null}
     * @param lidContactAction the lid contact action payload, or {@code null}
     * @param ctwaPerCustomerDataSharingAction the CTWA per customer data sharing payload, or {@code null}
     * @param paymentTosAction the payment terms of service payload, or {@code null}
     * @param privacySettingChannelsPersonalisedRecommendationAction the channels personalised recommendation payload, or {@code null}
     * @param businessBroadcastAssociationAction the business broadcast association payload, or {@code null}
     * @param detectedOutcomesStatusAction the detected outcomes status payload, or {@code null}
     * @param maibaAiFeaturesControlAction the Maiba AI features control payload, or {@code null}
     * @param businessBroadcastListAction the business broadcast list payload, or {@code null}
     * @param musicUserIdAction the music user identifier payload, or {@code null}
     * @param statusPostOptInNotificationPreferencesAction the status post opt in preferences payload, or {@code null}
     * @param avatarUpdatedAction the avatar updated action payload, or {@code null}
     * @param privateProcessingSettingAction the private processing setting payload, or {@code null}
     * @param newsletterSavedInterestsAction the newsletter saved interests payload, or {@code null}
     * @param aiThreadRenameAction the AI thread rename payload, or {@code null}
     * @param interactiveMessageAction the interactive message action payload, or {@code null}
     * @param settingsSyncAction the settings sync action payload, or {@code null}
     * @param outContactAction the outbound contact action payload, or {@code null}
     * @param nctSaltSyncAction the Noise chain token salt sync payload, or {@code null}
     * @param businessBroadcastCampaignAction the business broadcast campaign payload, or {@code null}
     * @param businessBroadcastInsightsAction the business broadcast insights payload, or {@code null}
     * @param customerDataAction the customer data action payload, or {@code null}
     * @param subscriptionsSyncV2Action the subscriptions sync v2 payload, or {@code null}
     */
    SyncActionValue(Instant timestamp, StarAction starAction, ContactAction contactAction, MuteAction muteAction, PinAction pinAction, PushNameSetting pushNameSetting, QuickReplyAction quickReplyAction, RecentEmojiWeightsAction recentEmojiWeightsAction, LabelEditAction labelEditAction, LabelAssociationAction labelAssociationAction, LocaleSetting localeSetting, ArchiveChatAction archiveChatAction, DeleteMessageForMeAction deleteMessageForMeAction, KeyExpirationAction keyExpirationAction, MarkChatAsReadAction markChatAsReadAction, ClearChatAction clearChatAction, DeleteChatAction deleteChatAction, UnarchiveChatsSetting unarchiveChatsSetting, PrimaryFeatureAction primaryFeatureAction, AndroidUnsupportedActions androidUnsupportedActions, AgentAction agentAction, UserStatusMuteAction userStatusMuteAction, TimeFormatAction timeFormatAction, NuxAction nuxAction, PrimaryVersionAction primaryVersionAction, StickerAction stickerAction, RemoveRecentStickerAction removeRecentStickerAction, ChatAssignmentAction chatAssignment, ChatAssignmentOpenedStatusAction chatAssignmentOpenedStatus, PnForLidChatAction pnForLidChatAction, MarketingMessageAction marketingMessageAction, MarketingMessageBroadcastAction marketingMessageBroadcastAction, ExternalWebBetaAction externalWebBetaAction, PrivacySettingRelayAllCalls privacySettingRelayAllCalls, CallLogAction callLogAction, UGCBotAction ugcBotAction, StatusPrivacyAction statusPrivacy, BotWelcomeRequestAction botWelcomeRequestAction, DeleteIndividualCallLogAction deleteIndividualCallLog, LabelReorderingAction labelReorderingAction, PaymentInfoAction paymentInfoAction, CustomPaymentMethodsAction customPaymentMethodsAction, LockChatAction lockChatAction, ChatLockSettings chatLockSettings, WamoUserIdentifierAction wamoUserIdentifierAction, PrivacySettingDisableLinkPreviewsAction privacySettingDisableLinkPreviewsAction, DeviceCapabilities deviceCapabilities, NoteEditAction noteEditAction, FavoritesAction favoritesAction, MerchantPaymentPartnerAction merchantPaymentPartnerAction, WaffleAccountLinkStateAction waffleAccountLinkStateAction, UsernameChatStartModeAction usernameChatStartMode, NotificationActivitySettingAction notificationActivitySettingAction, LidContactAction lidContactAction, CtwaPerCustomerDataSharingAction ctwaPerCustomerDataSharingAction, PaymentTosAction paymentTosAction, PrivacySettingChannelsPersonalisedRecommendationAction privacySettingChannelsPersonalisedRecommendationAction, BusinessBroadcastAssociationAction businessBroadcastAssociationAction, DetectedOutcomesStatusAction detectedOutcomesStatusAction, MaibaAIFeaturesControlAction maibaAiFeaturesControlAction, BusinessBroadcastListAction businessBroadcastListAction, MusicUserIdAction musicUserIdAction, StatusPostOptInNotificationPreferencesAction statusPostOptInNotificationPreferencesAction, AvatarUpdatedAction avatarUpdatedAction, PrivateProcessingSettingAction privateProcessingSettingAction, NewsletterSavedInterestsAction newsletterSavedInterestsAction, AiThreadRenameAction aiThreadRenameAction, InteractiveMessageAction interactiveMessageAction, SettingsSyncAction settingsSyncAction, OutContactAction outContactAction, NctSaltSyncAction nctSaltSyncAction, BusinessBroadcastCampaignAction businessBroadcastCampaignAction, BusinessBroadcastInsightsAction businessBroadcastInsightsAction, CustomerDataAction customerDataAction, SubscriptionsSyncV2Action subscriptionsSyncV2Action) {
        this.timestamp = timestamp;
        this.starAction = starAction;
        this.contactAction = contactAction;
        this.muteAction = muteAction;
        this.pinAction = pinAction;
        this.pushNameSetting = pushNameSetting;
        this.quickReplyAction = quickReplyAction;
        this.recentEmojiWeightsAction = recentEmojiWeightsAction;
        this.labelEditAction = labelEditAction;
        this.labelAssociationAction = labelAssociationAction;
        this.localeSetting = localeSetting;
        this.archiveChatAction = archiveChatAction;
        this.deleteMessageForMeAction = deleteMessageForMeAction;
        this.keyExpirationAction = keyExpirationAction;
        this.markChatAsReadAction = markChatAsReadAction;
        this.clearChatAction = clearChatAction;
        this.deleteChatAction = deleteChatAction;
        this.unarchiveChatsSetting = unarchiveChatsSetting;
        this.primaryFeatureAction = primaryFeatureAction;
        this.androidUnsupportedActions = androidUnsupportedActions;
        this.agentAction = agentAction;
        this.userStatusMuteAction = userStatusMuteAction;
        this.timeFormatAction = timeFormatAction;
        this.nuxAction = nuxAction;
        this.primaryVersionAction = primaryVersionAction;
        this.stickerAction = stickerAction;
        this.removeRecentStickerAction = removeRecentStickerAction;
        this.chatAssignment = chatAssignment;
        this.chatAssignmentOpenedStatus = chatAssignmentOpenedStatus;
        this.pnForLidChatAction = pnForLidChatAction;
        this.marketingMessageAction = marketingMessageAction;
        this.marketingMessageBroadcastAction = marketingMessageBroadcastAction;
        this.externalWebBetaAction = externalWebBetaAction;
        this.privacySettingRelayAllCalls = privacySettingRelayAllCalls;
        this.callLogAction = callLogAction;
        this.ugcBotAction = ugcBotAction;
        this.statusPrivacy = statusPrivacy;
        this.botWelcomeRequestAction = botWelcomeRequestAction;
        this.deleteIndividualCallLog = deleteIndividualCallLog;
        this.labelReorderingAction = labelReorderingAction;
        this.paymentInfoAction = paymentInfoAction;
        this.customPaymentMethodsAction = customPaymentMethodsAction;
        this.lockChatAction = lockChatAction;
        this.chatLockSettings = chatLockSettings;
        this.wamoUserIdentifierAction = wamoUserIdentifierAction;
        this.privacySettingDisableLinkPreviewsAction = privacySettingDisableLinkPreviewsAction;
        this.deviceCapabilities = deviceCapabilities;
        this.noteEditAction = noteEditAction;
        this.favoritesAction = favoritesAction;
        this.merchantPaymentPartnerAction = merchantPaymentPartnerAction;
        this.waffleAccountLinkStateAction = waffleAccountLinkStateAction;
        this.usernameChatStartMode = usernameChatStartMode;
        this.notificationActivitySettingAction = notificationActivitySettingAction;
        this.lidContactAction = lidContactAction;
        this.ctwaPerCustomerDataSharingAction = ctwaPerCustomerDataSharingAction;
        this.paymentTosAction = paymentTosAction;
        this.privacySettingChannelsPersonalisedRecommendationAction = privacySettingChannelsPersonalisedRecommendationAction;
        this.businessBroadcastAssociationAction = businessBroadcastAssociationAction;
        this.detectedOutcomesStatusAction = detectedOutcomesStatusAction;
        this.maibaAiFeaturesControlAction = maibaAiFeaturesControlAction;
        this.businessBroadcastListAction = businessBroadcastListAction;
        this.musicUserIdAction = musicUserIdAction;
        this.statusPostOptInNotificationPreferencesAction = statusPostOptInNotificationPreferencesAction;
        this.avatarUpdatedAction = avatarUpdatedAction;
        this.privateProcessingSettingAction = privateProcessingSettingAction;
        this.newsletterSavedInterestsAction = newsletterSavedInterestsAction;
        this.aiThreadRenameAction = aiThreadRenameAction;
        this.interactiveMessageAction = interactiveMessageAction;
        this.settingsSyncAction = settingsSyncAction;
        this.outContactAction = outContactAction;
        this.nctSaltSyncAction = nctSaltSyncAction;
        this.businessBroadcastCampaignAction = businessBroadcastCampaignAction;
        this.businessBroadcastInsightsAction = businessBroadcastInsightsAction;
        this.customerDataAction = customerDataAction;
        this.subscriptionsSyncV2Action = subscriptionsSyncV2Action;
    }

    /**
     * Returns the wall clock time at which the mutation was produced.
     *
     * @return an optional containing the timestamp, empty if absent
     */
    public Optional<Instant> timestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Returns the concrete action carried by this value, exposed through
     * the common {@link SyncAction} supertype.
     *
     * <p>Inspects the action fields in declaration order and returns the
     * first populated one so that callers can dispatch on the action
     * without having to test each field individually.
     *
     * @return an optional containing the concrete action, empty if no
     *         action field is populated
     */
    public Optional<? extends SyncAction<?>> action() {
        if (starAction != null) return Optional.of(starAction);
        if (contactAction != null) return Optional.of(contactAction);
        if (muteAction != null) return Optional.of(muteAction);
        if (pinAction != null) return Optional.of(pinAction);
        if (pushNameSetting != null) return Optional.of(pushNameSetting);
        if (quickReplyAction != null) return Optional.of(quickReplyAction);
        if (recentEmojiWeightsAction != null) return Optional.of(recentEmojiWeightsAction);
        if (labelEditAction != null) return Optional.of(labelEditAction);
        if (labelAssociationAction != null) return Optional.of(labelAssociationAction);
        if (localeSetting != null) return Optional.of(localeSetting);
        if (archiveChatAction != null) return Optional.of(archiveChatAction);
        if (deleteMessageForMeAction != null) return Optional.of(deleteMessageForMeAction);
        if (keyExpirationAction != null) return Optional.of(keyExpirationAction);
        if (markChatAsReadAction != null) return Optional.of(markChatAsReadAction);
        if (clearChatAction != null) return Optional.of(clearChatAction);
        if (deleteChatAction != null) return Optional.of(deleteChatAction);
        if (unarchiveChatsSetting != null) return Optional.of(unarchiveChatsSetting);
        if (primaryFeatureAction != null) return Optional.of(primaryFeatureAction);
        if (androidUnsupportedActions != null) return Optional.of(androidUnsupportedActions);
        if (agentAction != null) return Optional.of(agentAction);
        if (userStatusMuteAction != null) return Optional.of(userStatusMuteAction);
        if (timeFormatAction != null) return Optional.of(timeFormatAction);
        if (nuxAction != null) return Optional.of(nuxAction);
        if (primaryVersionAction != null) return Optional.of(primaryVersionAction);
        if (stickerAction != null) return Optional.of(stickerAction);
        if (removeRecentStickerAction != null) return Optional.of(removeRecentStickerAction);
        if (chatAssignment != null) return Optional.of(chatAssignment);
        if (chatAssignmentOpenedStatus != null) return Optional.of(chatAssignmentOpenedStatus);
        if (pnForLidChatAction != null) return Optional.of(pnForLidChatAction);
        if (marketingMessageAction != null) return Optional.of(marketingMessageAction);
        if (marketingMessageBroadcastAction != null) return Optional.of(marketingMessageBroadcastAction);
        if (externalWebBetaAction != null) return Optional.of(externalWebBetaAction);
        if (privacySettingRelayAllCalls != null) return Optional.of(privacySettingRelayAllCalls);
        if (callLogAction != null) return Optional.of(callLogAction);
        if (ugcBotAction != null) return Optional.of(ugcBotAction);
        if (statusPrivacy != null) return Optional.of(statusPrivacy);
        if (botWelcomeRequestAction != null) return Optional.of(botWelcomeRequestAction);
        if (deleteIndividualCallLog != null) return Optional.of(deleteIndividualCallLog);
        if (labelReorderingAction != null) return Optional.of(labelReorderingAction);
        if (paymentInfoAction != null) return Optional.of(paymentInfoAction);
        if (customPaymentMethodsAction != null) return Optional.of(customPaymentMethodsAction);
        if (lockChatAction != null) return Optional.of(lockChatAction);
        if (chatLockSettings != null) return Optional.of(chatLockSettings);
        if (wamoUserIdentifierAction != null) return Optional.of(wamoUserIdentifierAction);
        if (privacySettingDisableLinkPreviewsAction != null) return Optional.of(privacySettingDisableLinkPreviewsAction);
        if (deviceCapabilities != null) return Optional.of(deviceCapabilities);
        if (noteEditAction != null) return Optional.of(noteEditAction);
        if (favoritesAction != null) return Optional.of(favoritesAction);
        if (merchantPaymentPartnerAction != null) return Optional.of(merchantPaymentPartnerAction);
        if (waffleAccountLinkStateAction != null) return Optional.of(waffleAccountLinkStateAction);
        if (usernameChatStartMode != null) return Optional.of(usernameChatStartMode);
        if (notificationActivitySettingAction != null) return Optional.of(notificationActivitySettingAction);
        if (lidContactAction != null) return Optional.of(lidContactAction);
        if (ctwaPerCustomerDataSharingAction != null) return Optional.of(ctwaPerCustomerDataSharingAction);
        if (paymentTosAction != null) return Optional.of(paymentTosAction);
        if (privacySettingChannelsPersonalisedRecommendationAction != null) return Optional.of(privacySettingChannelsPersonalisedRecommendationAction);
        if (businessBroadcastAssociationAction != null) return Optional.of(businessBroadcastAssociationAction);
        if (detectedOutcomesStatusAction != null) return Optional.of(detectedOutcomesStatusAction);
        if (maibaAiFeaturesControlAction != null) return Optional.of(maibaAiFeaturesControlAction);
        if (businessBroadcastListAction != null) return Optional.of(businessBroadcastListAction);
        if (musicUserIdAction != null) return Optional.of(musicUserIdAction);
        if (statusPostOptInNotificationPreferencesAction != null) return Optional.of(statusPostOptInNotificationPreferencesAction);
        if (avatarUpdatedAction != null) return Optional.of(avatarUpdatedAction);
        if (privateProcessingSettingAction != null) return Optional.of(privateProcessingSettingAction);
        if (newsletterSavedInterestsAction != null) return Optional.of(newsletterSavedInterestsAction);
        if (aiThreadRenameAction != null) return Optional.of(aiThreadRenameAction);
        if (interactiveMessageAction != null) return Optional.of(interactiveMessageAction);
        if (settingsSyncAction != null) return Optional.of(settingsSyncAction);
        if (outContactAction != null) return Optional.of(outContactAction);
        if (nctSaltSyncAction != null) return Optional.of(nctSaltSyncAction);
        if (businessBroadcastCampaignAction != null) return Optional.of(businessBroadcastCampaignAction);
        if (businessBroadcastInsightsAction != null) return Optional.of(businessBroadcastInsightsAction);
        if (customerDataAction != null) return Optional.of(customerDataAction);
        if (subscriptionsSyncV2Action != null) return Optional.of(subscriptionsSyncV2Action);
        return Optional.empty();
    }

    /**
     * Sets the wall clock time at which the mutation was produced.
     *
     * @param timestamp the timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the chat lock settings payload carried by this value.
     *
     * @param chatLockSettings the chat lock settings payload
     */
    public void setChatLockSettings(ChatLockSettings chatLockSettings) {
        this.chatLockSettings = chatLockSettings;
    }

    /**
     * Sets the device capabilities payload carried by this value.
     *
     * @param deviceCapabilities the device capabilities payload
     */
    public void setDeviceCapabilities(DeviceCapabilities deviceCapabilities) {
        this.deviceCapabilities = deviceCapabilities;
    }
}
