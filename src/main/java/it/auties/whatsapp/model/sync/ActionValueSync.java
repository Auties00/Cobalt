package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufConverter;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.setting.*;
import it.auties.whatsapp.util.Clock;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@Builder
@Jacksonized
@ToString
@Accessors(fluent = true)
@ProtobufName("SyncActionValue")
public class ActionValueSync implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = INT64)
    @Getter
    private long timestamp;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = StarAction.class)
    private StarAction starAction;

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ContactAction.class)
    private ContactAction contactAction;

    @ProtobufProperty(index = 4, type = MESSAGE, implementation = MuteAction.class)
    private MuteAction muteAction;

    @ProtobufProperty(index = 5, type = MESSAGE, implementation = PinAction.class)
    private PinAction pinAction;

    @ProtobufProperty(index = 8, type = MESSAGE, implementation = QuickReplyAction.class)
    private QuickReplyAction quickReplyAction;

    @ProtobufProperty(index = 9, type = MESSAGE, implementation = RecentStickerWeightsAction.class)
    private RecentStickerWeightsAction recentStickerWeightsAction;

    @ProtobufProperty(index = 11, type = MESSAGE, implementation = RecentEmojiWeightsAction.class)
    private RecentEmojiWeightsAction recentEmojiWeightsAction;

    @ProtobufProperty(index = 14, type = MESSAGE, implementation = LabelEditAction.class)
    private LabelEditAction labelEditAction;

    @ProtobufProperty(index = 15, type = MESSAGE, implementation = LabelAssociationAction.class)
    private LabelAssociationAction labelAssociationAction;

    @ProtobufProperty(index = 17, type = MESSAGE, implementation = ArchiveChatAction.class)
    private ArchiveChatAction archiveChatAction;

    @ProtobufProperty(index = 18, type = MESSAGE, implementation = DeleteMessageForMeAction.class)
    private DeleteMessageForMeAction deleteMessageForMeAction;

    @ProtobufProperty(index = 20, type = MESSAGE, implementation = MarkChatAsReadAction.class)
    private MarkChatAsReadAction markChatAsReadAction;

    @ProtobufProperty(index = 21, type = MESSAGE, implementation = ClearChatAction.class)
    private ClearChatAction clearChatAction;

    @ProtobufProperty(index = 22, type = MESSAGE, implementation = DeleteChatAction.class)
    private DeleteChatAction deleteChatAction;

    @ProtobufProperty(index = 25, type = MESSAGE, implementation = FavoriteStickerAction.class)
    private FavoriteStickerAction favoriteStickerAction;

    @ProtobufProperty(index = 26, type = MESSAGE, implementation = AndroidUnsupportedActions.class)
    private AndroidUnsupportedActions androidUnsupportedActions;

    @ProtobufProperty(index = 27, name = "agentAction", type = MESSAGE)
    private AgentAction agentAction;

    @ProtobufProperty(index = 28, name = "subscriptionAction", type = MESSAGE)
    private SubscriptionAction subscriptionAction;

    @ProtobufProperty(index = 29, name = "userStatusMuteAction", type = MESSAGE)
    private UserStatusMuteAction userStatusMuteAction;

    @ProtobufProperty(index = 30, name = "timeFormatAction", type = MESSAGE)
    private TimeFormatAction timeFormatAction;

    @ProtobufProperty(index = 31, name = "nuxAction", type = MESSAGE)
    private NuxAction nuxAction;

    @ProtobufProperty(index = 32, name = "primaryVersionAction", type = MESSAGE)
    private PrimaryVersionAction primaryVersionAction;

    @ProtobufProperty(index = 33, name = "stickerAction", type = MESSAGE)
    private StickerAction stickerAction;

    @ProtobufProperty(index = 34, name = "removeRecentStickerAction", type = MESSAGE)
    private RemoveRecentStickerAction removeRecentStickerAction;

    @ProtobufProperty(index = 35, name = "chatAssignment", type = MESSAGE)
    private ChatAssignmentAction chatAssignmentAction;

    @ProtobufProperty(index = 36, name = "chatAssignmentOpenedStatus", type = MESSAGE)
    private ChatAssignmentOpenedStatusAction chatAssignmentOpenedStatusAction;

    @ProtobufProperty(index = 6, type = MESSAGE, implementation = SecurityNotificationSetting.class)
    private SecurityNotificationSetting securityNotificationSetting;

    @ProtobufProperty(index = 7, type = MESSAGE, implementation = PushNameSetting.class)
    private PushNameSetting pushNameSetting;

    @ProtobufProperty(index = 16, type = MESSAGE, implementation = LocaleSetting.class)
    private LocaleSetting localeSetting;

    @ProtobufProperty(index = 23, type = MESSAGE, implementation = UnarchiveChatsSetting.class)
    private UnarchiveChatsSetting unarchiveChatsSetting;

    @ProtobufProperty(index = 10, type = MESSAGE, implementation = RecentStickerMetadata.class)
    @Getter
    private RecentStickerMetadata recentStickerMetadata;

    @ProtobufProperty(index = 19, type = MESSAGE, implementation = KeyExpiration.class)
    @Getter
    private KeyExpiration keyExpiration;

    @ProtobufProperty(index = 24, type = MESSAGE, implementation = ActionValueSync.PrimaryFeature.class)
    @Getter
    private PrimaryFeature primaryFeature;

    @SuppressWarnings("PatternVariableHidesField")
    private ActionValueSync(@NonNull Action action) {
        this.timestamp = Clock.nowInSeconds();
        switch (action) {
            case StarAction starAction -> this.starAction = starAction;
            case ContactAction contactAction -> this.contactAction = contactAction;
            case MuteAction muteAction -> this.muteAction = muteAction;
            case PinAction pinAction -> this.pinAction = pinAction;
            case QuickReplyAction quickReplyAction -> this.quickReplyAction = quickReplyAction;
            case RecentStickerWeightsAction recentStickerWeightsAction ->
                    this.recentStickerWeightsAction = recentStickerWeightsAction;
            case RecentEmojiWeightsAction recentEmojiWeightsAction ->
                    this.recentEmojiWeightsAction = recentEmojiWeightsAction;
            case LabelEditAction labelEditAction -> this.labelEditAction = labelEditAction;
            case LabelAssociationAction labelAssociationAction -> this.labelAssociationAction = labelAssociationAction;
            case ArchiveChatAction archiveChatAction -> this.archiveChatAction = archiveChatAction;
            case DeleteMessageForMeAction deleteMessageForMeAction ->
                    this.deleteMessageForMeAction = deleteMessageForMeAction;
            case MarkChatAsReadAction markChatAsReadAction -> this.markChatAsReadAction = markChatAsReadAction;
            case ClearChatAction clearChatAction -> this.clearChatAction = clearChatAction;
            case DeleteChatAction deleteChatAction -> this.deleteChatAction = deleteChatAction;
            case FavoriteStickerAction favoriteStickerAction -> this.favoriteStickerAction = favoriteStickerAction;
            case AndroidUnsupportedActions androidUnsupportedActions ->
                    this.androidUnsupportedActions = androidUnsupportedActions;
            case AgentAction agentAction -> this.agentAction = agentAction;
            case ChatAssignmentAction chatAssignmentAction -> this.chatAssignmentAction = chatAssignmentAction;
            case ChatAssignmentOpenedStatusAction chatAssignmentOpenedStatusAction ->
                    this.chatAssignmentOpenedStatusAction = chatAssignmentOpenedStatusAction;
            case NuxAction nuxAction -> this.nuxAction = nuxAction;
            case PrimaryVersionAction primaryVersionAction -> this.primaryVersionAction = primaryVersionAction;
            case RemoveRecentStickerAction removeRecentStickerAction ->
                    this.removeRecentStickerAction = removeRecentStickerAction;
            case StickerAction stickerAction -> this.stickerAction = stickerAction;
            case SubscriptionAction subscriptionAction -> this.subscriptionAction = subscriptionAction;
            case TimeFormatAction timeFormatAction -> this.timeFormatAction = timeFormatAction;
            case UserStatusMuteAction userStatusMuteAction -> this.userStatusMuteAction = userStatusMuteAction;
        }
    }

    @SuppressWarnings("PatternVariableHidesField")
    private ActionValueSync(@NonNull Setting setting) {
        this.timestamp = Clock.nowInSeconds();
        switch (setting) {
            case SecurityNotificationSetting securityNotificationSetting ->
                    this.securityNotificationSetting = securityNotificationSetting;
            case PushNameSetting pushNameSetting -> this.pushNameSetting = pushNameSetting;
            case LocaleSetting localeSetting -> this.localeSetting = localeSetting;
            case UnarchiveChatsSetting unarchiveChatsSetting -> this.unarchiveChatsSetting = unarchiveChatsSetting;
            default ->
                    throw new UnsupportedOperationException("Cannot wrap %s in action value sync".formatted(setting));
        }
    }

    public static ActionValueSync of(@NonNull Action action) {
        return new ActionValueSync(action);
    }

    public static ActionValueSync of(@NonNull Setting setting) {
        return new ActionValueSync(setting);
    }

    public Action action() {
        if (starAction != null) {
            return starAction;
        }
        if (contactAction != null) {
            return contactAction;
        }
        if (muteAction != null) {
            return muteAction;
        }
        if (pinAction != null) {
            return pinAction;
        }
        if (quickReplyAction != null) {
            return quickReplyAction;
        }
        if (recentStickerWeightsAction != null) {
            return recentStickerWeightsAction;
        }
        if (recentEmojiWeightsAction != null) {
            return recentEmojiWeightsAction;
        }
        if (labelEditAction != null) {
            return labelEditAction;
        }
        if (labelAssociationAction != null) {
            return labelAssociationAction;
        }
        if (archiveChatAction != null) {
            return archiveChatAction;
        }
        if (deleteMessageForMeAction != null) {
            return deleteMessageForMeAction;
        }
        if (markChatAsReadAction != null) {
            return markChatAsReadAction;
        }
        if (clearChatAction != null) {
            return clearChatAction;
        }
        if (deleteChatAction != null) {
            return deleteChatAction;
        }
        if (favoriteStickerAction != null) {
            return favoriteStickerAction;
        }
        if (androidUnsupportedActions != null) {
            return androidUnsupportedActions;
        }
        if (agentAction != null) {
            return agentAction;
        }
        if (chatAssignmentAction != null) {
            return chatAssignmentAction;
        }
        if (chatAssignmentOpenedStatusAction != null) {
            return chatAssignmentOpenedStatusAction;
        }
        if (nuxAction != null) {
            return nuxAction;
        }
        if (primaryVersionAction != null) {
            return primaryVersionAction;
        }
        if (removeRecentStickerAction != null) {
            return removeRecentStickerAction;
        }
        if (stickerAction != null) {
            return stickerAction;
        }
        if (subscriptionAction != null) {
            return subscriptionAction;
        }
        if (timeFormatAction != null) {
            return timeFormatAction;
        }
        if (userStatusMuteAction != null) {
            return userStatusMuteAction;
        }
        return null;
    }

    public Setting setting() {
        if (securityNotificationSetting != null) {
            return securityNotificationSetting;
        }
        if (pushNameSetting != null) {
            return pushNameSetting;
        }
        if (localeSetting != null) {
            return localeSetting;
        }
        if (unarchiveChatsSetting != null) {
            return unarchiveChatsSetting;
        }
        return null;
    }

    @ProtobufConverter
    public ReadOnlyActionValueSync toReadOnlyValue(){
        return new ReadOnlyActionValueSync(timestamp, starAction, contactAction, muteAction, pinAction, quickReplyAction,
                recentStickerWeightsAction, recentEmojiWeightsAction, labelEditAction, labelAssociationAction,
                archiveChatAction, deleteMessageForMeAction, markChatAsReadAction, clearChatAction, deleteChatAction,
                favoriteStickerAction, androidUnsupportedActions, agentAction, subscriptionAction, userStatusMuteAction,
                timeFormatAction, nuxAction, primaryVersionAction, stickerAction, removeRecentStickerAction, chatAssignmentAction,
                chatAssignmentOpenedStatusAction, securityNotificationSetting, pushNameSetting, localeSetting, unarchiveChatsSetting,
                recentStickerMetadata, keyExpiration, primaryFeature);
    }

    /**
     * Read only stub used by ModernProtobuf
     */
    @Builder
    public record ReadOnlyActionValueSync(@ProtobufProperty(index = 1, type = INT64) long timestamp,
                                          @ProtobufProperty(index = 2, type = MESSAGE) StarAction starAction,
                                          @ProtobufProperty(index = 3, type = MESSAGE) ContactAction contactAction,
                                          @ProtobufProperty(index = 4, type = MESSAGE) MuteAction muteAction,
                                          @ProtobufProperty(index = 5, type = MESSAGE) PinAction pinAction,
                                          @ProtobufProperty(index = 8, type = MESSAGE) QuickReplyAction quickReplyAction,
                                          @ProtobufProperty(index = 9, type = MESSAGE) RecentStickerWeightsAction recentStickerWeightsAction,
                                          @ProtobufProperty(index = 11, type = MESSAGE) RecentEmojiWeightsAction recentEmojiWeightsAction,
                                          @ProtobufProperty(index = 14, type = MESSAGE) LabelEditAction labelEditAction,
                                          @ProtobufProperty(index = 15, type = MESSAGE) LabelAssociationAction labelAssociationAction,
                                          @ProtobufProperty(index = 17, type = MESSAGE) ArchiveChatAction archiveChatAction,
                                          @ProtobufProperty(index = 18, type = MESSAGE) DeleteMessageForMeAction deleteMessageForMeAction,
                                          @ProtobufProperty(index = 20, type = MESSAGE) MarkChatAsReadAction markChatAsReadAction,
                                          @ProtobufProperty(index = 21, type = MESSAGE) ClearChatAction clearChatAction,
                                          @ProtobufProperty(index = 22, type = MESSAGE) DeleteChatAction deleteChatAction,
                                          @ProtobufProperty(index = 25, type = MESSAGE) FavoriteStickerAction favoriteStickerAction,
                                          @ProtobufProperty(index = 26, type = MESSAGE) AndroidUnsupportedActions androidUnsupportedActions,
                                          @ProtobufProperty(index = 27, type = MESSAGE) AgentAction agentAction,
                                          @ProtobufProperty(index = 28, type = MESSAGE) SubscriptionAction subscriptionAction,
                                          @ProtobufProperty(index = 29, type = MESSAGE) UserStatusMuteAction userStatusMuteAction,
                                          @ProtobufProperty(index = 30, type = MESSAGE) TimeFormatAction timeFormatAction,
                                          @ProtobufProperty(index = 31, type = MESSAGE) NuxAction nuxAction,
                                          @ProtobufProperty(index = 32, type = MESSAGE) PrimaryVersionAction primaryVersionAction,
                                          @ProtobufProperty(index = 33, type = MESSAGE) StickerAction stickerAction,
                                          @ProtobufProperty(index = 34, type = MESSAGE) RemoveRecentStickerAction removeRecentStickerAction,
                                          @ProtobufProperty(index = 35, type = MESSAGE) ChatAssignmentAction chatAssignmentAction,
                                          @ProtobufProperty(index = 36, type = MESSAGE) ChatAssignmentOpenedStatusAction chatAssignmentOpenedStatusAction,
                                          @ProtobufProperty(index = 6, type = MESSAGE) SecurityNotificationSetting securityNotificationSetting,
                                          @ProtobufProperty(index = 7, type = MESSAGE) PushNameSetting pushNameSetting,
                                          @ProtobufProperty(index = 16, type = MESSAGE) LocaleSetting localeSetting,
                                          @ProtobufProperty(index = 23, type = MESSAGE) UnarchiveChatsSetting unarchiveChatsSetting,
                                          @ProtobufProperty(index = 10, type = MESSAGE) RecentStickerMetadata recentStickerMetadata,
                                          @ProtobufProperty(index = 19, type = MESSAGE) KeyExpiration keyExpiration,
                                          @ProtobufProperty(index = 24, type = MESSAGE) PrimaryFeature primaryFeature) implements ProtobufMessage {

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Jacksonized
    @Builder
    @Accessors(fluent = true)
    public static class PrimaryFeature implements ProtobufMessage {
        @ProtobufProperty(index = 1, type = STRING, repeated = true)
        private List<String> flags;
    }
}