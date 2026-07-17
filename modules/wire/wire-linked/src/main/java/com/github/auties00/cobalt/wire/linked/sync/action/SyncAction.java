package com.github.auties00.cobalt.wire.linked.sync.action;

import com.alibaba.fastjson2.JSONArray;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilities;
import com.github.auties00.cobalt.wire.linked.setting.ChatLockSettings;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.AiThreadRenameAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.BotWelcomeRequestAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.MaibaAIFeaturesControlAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.UGCBotAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BroadcastListParticipantAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastAssociationAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastCampaignAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastInsightsAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.BusinessBroadcastListAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CtwaPerCustomerDataSharingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.CustomerDataAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.MarketingMessageAction;
import com.github.auties00.cobalt.wire.linked.sync.action.business.MarketingMessageBroadcastAction;
import com.github.auties00.cobalt.wire.linked.sync.action.call.CallLogAction;
import com.github.auties00.cobalt.wire.linked.sync.action.call.DeleteIndividualCallLogAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.*;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.*;
import com.github.auties00.cobalt.wire.linked.sync.action.device.*;
import com.github.auties00.cobalt.wire.linked.sync.action.media.*;
import com.github.auties00.cobalt.wire.linked.sync.action.payment.*;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingChannelsPersonalisedRecommendationAction;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingDisableLinkPreviewsAction;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivacySettingRelayAllCalls;
import com.github.auties00.cobalt.wire.linked.sync.action.privacy.PrivateProcessingSettingAction;
import com.github.auties00.cobalt.wire.linked.sync.action.setting.*;

import java.util.Collections;

/**
 * Common supertype for every app state sync action that can be encoded into
 * the WhatsApp app state protocol.
 *
 * <p>App state sync is the mechanism WhatsApp uses to replicate user level
 * settings and events (archive, mute, star, contact renames, privacy
 * preferences, and more) across a user's linked devices. Each action type is
 * identified by a canonical name and version and carries a typed payload as
 * well as a deterministic index that acts as the mutation key.
 *
 * <p>Every implementation contributes a canonical action name through
 * {@link #actionName()}, a schema version through {@link #actionVersion()},
 * and provides its index arguments through an implementation of
 * {@link SyncActionArgs}. The default {@link #toIndex(SyncActionArgs)}
 * method assembles the full index array used as the mutation key.
 *
 * @param <A> the arguments type that this action expects when serialising
 *            its index
 */
public sealed interface SyncAction<A extends SyncActionArgs>
        permits SyncActionMessage, SyncActionMessageRange, AiThreadRenameAction, BotWelcomeRequestAction, MaibaAIFeaturesControlAction, UGCBotAction, BroadcastListParticipantAction, BusinessBroadcastAssociationAction, BusinessBroadcastCampaignAction, BusinessBroadcastInsightsAction, BusinessBroadcastListAction, CtwaPerCustomerDataSharingAction, CustomerDataAction, MarketingMessageAction, MarketingMessageBroadcastAction, CallLogAction, DeleteIndividualCallLogAction, ArchiveChatAction, ChatAssignmentAction, ChatAssignmentOpenedStatusAction, ClearChatAction, DeleteChatAction, DeleteMessageForMeAction, InteractiveMessageAction, LockChatAction, MarkChatAsReadAction, MuteAction, PnForLidChatAction, QuickReplyAction, UsernameChatStartModeAction, ContactAction, LabelAssociationAction, LabelEditAction, LabelReorderingAction, LidContactAction, OutContactAction, PinAction, StarAction, UserStatusMuteAction, AgentAction, AndroidUnsupportedActions, ExternalWebBetaAction, KeyExpirationAction, NuxAction, PrimaryFeatureAction, PrimaryVersionAction, StatusPostOptInNotificationPreferencesAction, SubscriptionsSyncV2Action, TimeFormatAction, WaffleAccountLinkStateAction, WamoUserIdentifierAction, AvatarUpdatedAction, FavoritesAction, MusicUserIdAction, NewsletterSavedInterestsAction, NoteEditAction, RecentEmojiWeightsAction, RemoveRecentStickerAction, StatusPrivacyAction, StickerAction, CustomPaymentMethod, CustomPaymentMethodMetadata, CustomPaymentMethodsAction, MerchantPaymentPartnerAction, PaymentInfoAction, PaymentTosAction, PrivacySettingChannelsPersonalisedRecommendationAction, PrivacySettingDisableLinkPreviewsAction, PrivacySettingRelayAllCalls, PrivateProcessingSettingAction, DetectedOutcomesStatusAction, LocaleSetting, NctSaltSyncAction, NotificationActivitySettingAction, PushNameSetting, SettingsSyncAction, UnarchiveChatsSetting, ChatLockSettings, DeviceCapabilities {

    /**
     * Returns the canonical action name used as the first element of the
     * encoded index array.
     *
     * <p>The name identifies the action category on the wire and is matched
     * against the registered handler set on both sides of the sync protocol.
     *
     * @return the canonical action name
     */
    String actionName();

    /**
     * Returns the schema version declared by this action, used by handlers
     * to gate deserialisation and handling of newer payload shapes.
     *
     * @return the action schema version
     */
    int actionVersion();

    /**
     * Serialises this action's full index to a JSON array string built from
     * the action name followed by the trailing arguments supplied by the
     * given {@link SyncActionArgs}.
     *
     * <p>The resulting string is hashed to produce the mutation key used by
     * the app state sync protocol to match mutations across devices and
     * resolve conflicts on the same logical key.
     *
     * @param args the typed index arguments to append after the action name
     * @return the serialised index as a JSON array string
     */
    default String toIndex(A args) {
        var array = new JSONArray();
        array.add(actionName());
        Collections.addAll(array, args.toIndexArgs());
        return array.toJSONString();
    }
}
