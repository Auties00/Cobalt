package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.setting.*;
import it.auties.whatsapp.util.Clock;

import java.util.Optional;

@ProtobufMessageName("SyncActionValue")
public record ActionValueSync(
        @ProtobufProperty(index = 1, type = ProtobufType.INT64)
        long timestamp,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<StarAction> starAction,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<ContactAction> contactAction,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<MuteAction> muteAction,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        Optional<PinAction> pinAction,
        @ProtobufProperty(index = 8, type = ProtobufType.OBJECT)
        Optional<QuickReplyAction> quickReplyAction,
        @ProtobufProperty(index = 11, type = ProtobufType.OBJECT)
        Optional<RecentEmojiWeightsAction> recentEmojiWeightsAction,
        @ProtobufProperty(index = 14, type = ProtobufType.OBJECT)
        Optional<LabelEditAction> labelEditAction,
        @ProtobufProperty(index = 15, type = ProtobufType.OBJECT)
        Optional<LabelAssociationAction> labelAssociationAction,
        @ProtobufProperty(index = 17, type = ProtobufType.OBJECT)
        Optional<ArchiveChatAction> archiveChatAction,
        @ProtobufProperty(index = 18, type = ProtobufType.OBJECT)
        Optional<DeleteMessageForMeAction> deleteMessageForMeAction,
        @ProtobufProperty(index = 20, type = ProtobufType.OBJECT)
        Optional<MarkChatAsReadAction> markChatAsReadAction,
        @ProtobufProperty(index = 21, type = ProtobufType.OBJECT)
        Optional<ClearChatAction> clearChatAction,
        @ProtobufProperty(index = 22, type = ProtobufType.OBJECT)
        Optional<DeleteChatAction> deleteChatAction,
        @ProtobufProperty(index = 25, type = ProtobufType.OBJECT)
        Optional<StickerAction> favoriteStickerAction,
        @ProtobufProperty(index = 26, type = ProtobufType.OBJECT)
        Optional<AndroidUnsupportedActions> androidUnsupportedActions,
        @ProtobufProperty(index = 27, type = ProtobufType.OBJECT)
        Optional<AgentAction> agentAction,
        @ProtobufProperty(index = 28, type = ProtobufType.OBJECT)
        Optional<SubscriptionAction> subscriptionAction,
        @ProtobufProperty(index = 29, type = ProtobufType.OBJECT)
        Optional<UserStatusMuteAction> userStatusMuteAction,
        @ProtobufProperty(index = 30, type = ProtobufType.OBJECT)
        Optional<TimeFormatAction> timeFormatAction,
        @ProtobufProperty(index = 31, type = ProtobufType.OBJECT)
        Optional<NuxAction> nuxAction,
        @ProtobufProperty(index = 32, type = ProtobufType.OBJECT)
        Optional<PrimaryVersionAction> primaryVersionAction,
        @ProtobufProperty(index = 33, type = ProtobufType.OBJECT)
        Optional<StickerAction> stickerAction,
        @ProtobufProperty(index = 34, type = ProtobufType.OBJECT)
        Optional<RemoveRecentStickerAction> removeRecentStickerAction,
        @ProtobufProperty(index = 35, type = ProtobufType.OBJECT)
        Optional<ChatAssignmentAction> chatAssignmentAction,
        @ProtobufProperty(index = 36, type = ProtobufType.OBJECT)
        Optional<ChatAssignmentOpenedStatusAction> chatAssignmentOpenedStatusAction,
        @ProtobufProperty(index = 6, type = ProtobufType.OBJECT)
        Optional<SecurityNotificationSettings> securityNotificationSetting,
        @ProtobufProperty(index = 7, type = ProtobufType.OBJECT)
        Optional<PushNameSettings> pushNameSetting,
        @ProtobufProperty(index = 16, type = ProtobufType.OBJECT)
        Optional<LocaleSettings> localeSetting,
        @ProtobufProperty(index = 23, type = ProtobufType.OBJECT)
        Optional<UnarchiveChatsSettings> unarchiveChatsSetting,
        @ProtobufProperty(index = 10, type = ProtobufType.OBJECT)
        Optional<StickerMetadata> stickerMetadata,
        @ProtobufProperty(index = 19, type = ProtobufType.OBJECT)
        Optional<KeyExpiration> keyExpiration,
        @ProtobufProperty(index = 24, type = ProtobufType.OBJECT)
        Optional<PrimaryFeature> primaryFeature
) implements ProtobufMessage {
    public static ActionValueSync of(Action action) {
        var builder = new ActionValueSyncBuilder().timestamp(Clock.nowSeconds());
        switch (action) {
            case StarAction starAction -> builder.starAction(starAction);
            case ContactAction contactAction -> builder.contactAction(contactAction);
            case MuteAction muteAction -> builder.muteAction(muteAction);
            case PinAction pinAction -> builder.pinAction(pinAction);
            case QuickReplyAction quickReplyAction -> builder.quickReplyAction(quickReplyAction);
            case RecentEmojiWeightsAction recentEmojiWeightsAction ->
                    builder.recentEmojiWeightsAction(recentEmojiWeightsAction);
            case LabelEditAction labelEditAction -> builder.labelEditAction(labelEditAction);
            case LabelAssociationAction labelAssociationAction ->
                    builder.labelAssociationAction(labelAssociationAction);
            case ArchiveChatAction archiveChatAction -> builder.archiveChatAction(archiveChatAction);
            case DeleteMessageForMeAction deleteMessageForMeAction ->
                    builder.deleteMessageForMeAction(deleteMessageForMeAction);
            case MarkChatAsReadAction markChatAsReadAction -> builder.markChatAsReadAction(markChatAsReadAction);
            case ClearChatAction clearChatAction -> builder.clearChatAction(clearChatAction);
            case DeleteChatAction deleteChatAction -> builder.deleteChatAction(deleteChatAction);
            case AndroidUnsupportedActions androidUnsupportedActions ->
                    builder.androidUnsupportedActions(androidUnsupportedActions);
            case AgentAction agentAction -> builder.agentAction(agentAction);
            case ChatAssignmentAction chatAssignmentAction -> builder.chatAssignmentAction(chatAssignmentAction);
            case ChatAssignmentOpenedStatusAction chatAssignmentOpenedStatusAction ->
                    builder.chatAssignmentOpenedStatusAction(chatAssignmentOpenedStatusAction);
            case NuxAction nuxAction -> builder.nuxAction(nuxAction);
            case PrimaryVersionAction primaryVersionAction -> builder.primaryVersionAction(primaryVersionAction);
            case RemoveRecentStickerAction removeRecentStickerAction ->
                    builder.removeRecentStickerAction(removeRecentStickerAction);
            case StickerAction stickerAction -> builder.stickerAction(stickerAction);
            case SubscriptionAction subscriptionAction -> builder.subscriptionAction(subscriptionAction);
            case TimeFormatAction timeFormatAction -> builder.timeFormatAction(timeFormatAction);
            case UserStatusMuteAction userStatusMuteAction -> builder.userStatusMuteAction(userStatusMuteAction);
            default -> throw new UnsupportedOperationException("Cannot wrap %s in action value sync".formatted(action));
        }
        return builder.build();
    }

    public static ActionValueSync of(Setting setting) {
        var builder = new ActionValueSyncBuilder().timestamp(Clock.nowSeconds());
        switch (setting) {
            case SecurityNotificationSettings securityNotificationSettings ->
                    builder.securityNotificationSetting(securityNotificationSettings);
            case PushNameSettings pushNameSettings -> builder.pushNameSetting(pushNameSettings);
            case LocaleSettings localeSettings -> builder.localeSetting(localeSettings);
            case UnarchiveChatsSettings unarchiveChatsSettings -> builder.unarchiveChatsSetting(unarchiveChatsSettings);
            default ->
                    throw new UnsupportedOperationException("Cannot wrap %s in action value sync".formatted(setting));
        }
        return builder.build();
    }

    public Optional<? extends Action> action() {
        if (starAction.isPresent()) {
            return starAction;
        }
        if (contactAction.isPresent()) {
            return contactAction;
        }
        if (muteAction.isPresent()) {
            return muteAction;
        }
        if (pinAction.isPresent()) {
            return pinAction;
        }
        if (quickReplyAction.isPresent()) {
            return quickReplyAction;
        }
        if (recentEmojiWeightsAction.isPresent()) {
            return recentEmojiWeightsAction;
        }
        if (labelEditAction.isPresent()) {
            return labelEditAction;
        }
        if (labelAssociationAction.isPresent()) {
            return labelAssociationAction;
        }
        if (archiveChatAction.isPresent()) {
            return archiveChatAction;
        }
        if (deleteMessageForMeAction.isPresent()) {
            return deleteMessageForMeAction;
        }
        if (markChatAsReadAction.isPresent()) {
            return markChatAsReadAction;
        }
        if (clearChatAction.isPresent()) {
            return clearChatAction;
        }
        if (deleteChatAction.isPresent()) {
            return deleteChatAction;
        }
        if (favoriteStickerAction.isPresent()) {
            return favoriteStickerAction;
        }
        if (androidUnsupportedActions.isPresent()) {
            return androidUnsupportedActions;
        }
        if (agentAction.isPresent()) {
            return agentAction;
        }
        if (chatAssignmentAction.isPresent()) {
            return chatAssignmentAction;
        }
        if (chatAssignmentOpenedStatusAction.isPresent()) {
            return chatAssignmentOpenedStatusAction;
        }
        if (nuxAction.isPresent()) {
            return nuxAction;
        }
        if (primaryVersionAction.isPresent()) {
            return primaryVersionAction;
        }
        if (removeRecentStickerAction.isPresent()) {
            return removeRecentStickerAction;
        }
        if (stickerAction.isPresent()) {
            return stickerAction;
        }
        if (subscriptionAction.isPresent()) {
            return subscriptionAction;
        }
        if (timeFormatAction.isPresent()) {
            return timeFormatAction;
        }
        return userStatusMuteAction;
    }

    public Optional<? extends Setting> setting() {
        if (securityNotificationSetting.isPresent()) {
            return securityNotificationSetting;
        }
        if (pushNameSetting.isPresent()) {
            return pushNameSetting;
        }
        if (localeSetting.isPresent()) {
            return localeSetting;
        }
        return unarchiveChatsSetting;
    }
}