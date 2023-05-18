package it.auties.whatsapp.model.sync;

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
import java.util.Optional;

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

    @ProtobufProperty(index = 10, type = MESSAGE, implementation = StickerMetadata.class)
    private StickerMetadata stickerMetadata;

    @ProtobufProperty(index = 19, type = MESSAGE, implementation = KeyExpiration.class)
    private KeyExpiration keyExpiration;

    @ProtobufProperty(index = 24, type = MESSAGE, implementation = ActionValueSync.PrimaryFeature.class)
    private PrimaryFeature primaryFeature;

    @SuppressWarnings("PatternVariableHidesField")
    private ActionValueSync(@NonNull Action action) {
        this.timestamp = Clock.nowSeconds();
        if (action instanceof StarAction starAction) {
            this.starAction = starAction;
        } else if (action instanceof ContactAction contactAction) {
            this.contactAction = contactAction;
        } else if (action instanceof MuteAction muteAction) {
            this.muteAction = muteAction;
        } else if (action instanceof PinAction pinAction) {
            this.pinAction = pinAction;
        } else if (action instanceof QuickReplyAction quickReplyAction) {
            this.quickReplyAction = quickReplyAction;
        } else if (action instanceof RecentStickerWeightsAction recentStickerWeightsAction) {
            this.recentStickerWeightsAction = recentStickerWeightsAction;
        } else if (action instanceof RecentEmojiWeightsAction recentEmojiWeightsAction) {
            this.recentEmojiWeightsAction = recentEmojiWeightsAction;
        } else if (action instanceof LabelEditAction labelEditAction) {
            this.labelEditAction = labelEditAction;
        } else if (action instanceof LabelAssociationAction labelAssociationAction) {
            this.labelAssociationAction = labelAssociationAction;
        } else if (action instanceof ArchiveChatAction archiveChatAction) {
            this.archiveChatAction = archiveChatAction;
        } else if (action instanceof DeleteMessageForMeAction deleteMessageForMeAction) {
            this.deleteMessageForMeAction = deleteMessageForMeAction;
        } else if (action instanceof MarkChatAsReadAction markChatAsReadAction) {
            this.markChatAsReadAction = markChatAsReadAction;
        } else if (action instanceof ClearChatAction clearChatAction) {
            this.clearChatAction = clearChatAction;
        } else if (action instanceof DeleteChatAction deleteChatAction) {
            this.deleteChatAction = deleteChatAction;
        } else if (action instanceof FavoriteStickerAction favoriteStickerAction) {
            this.favoriteStickerAction = favoriteStickerAction;
        } else if (action instanceof AndroidUnsupportedActions androidUnsupportedActions) {
            this.androidUnsupportedActions = androidUnsupportedActions;
        } else if (action instanceof AgentAction agentAction) {
            this.agentAction = agentAction;
        } else if (action instanceof ChatAssignmentAction chatAssignmentAction) {
            this.chatAssignmentAction = chatAssignmentAction;
        } else if (action instanceof ChatAssignmentOpenedStatusAction chatAssignmentOpenedStatusAction) {
            this.chatAssignmentOpenedStatusAction = chatAssignmentOpenedStatusAction;
        } else if (action instanceof NuxAction nuxAction) {
            this.nuxAction = nuxAction;
        } else if (action instanceof PrimaryVersionAction primaryVersionAction) {
            this.primaryVersionAction = primaryVersionAction;
        } else if (action instanceof RemoveRecentStickerAction removeRecentStickerAction) {
            this.removeRecentStickerAction = removeRecentStickerAction;
        } else if (action instanceof StickerAction stickerAction) {
            this.stickerAction = stickerAction;
        } else if (action instanceof SubscriptionAction subscriptionAction) {
            this.subscriptionAction = subscriptionAction;
        } else if (action instanceof TimeFormatAction timeFormatAction) {
            this.timeFormatAction = timeFormatAction;
        } else if (action instanceof UserStatusMuteAction userStatusMuteAction) {
            this.userStatusMuteAction = userStatusMuteAction;
        }
    }

    @SuppressWarnings("PatternVariableHidesField")
    private ActionValueSync(@NonNull Setting setting) {
        this.timestamp = Clock.nowSeconds();
        if (setting instanceof SecurityNotificationSetting securityNotificationSetting) {
            this.securityNotificationSetting = securityNotificationSetting;
        } else if (setting instanceof PushNameSetting pushNameSetting) {
            this.pushNameSetting = pushNameSetting;
        } else if (setting instanceof LocaleSetting localeSetting) {
            this.localeSetting = localeSetting;
        } else if (setting instanceof UnarchiveChatsSetting unarchiveChatsSetting) {
            this.unarchiveChatsSetting = unarchiveChatsSetting;
        } else {
            throw new UnsupportedOperationException("Cannot wrap %s in action value sync".formatted(setting));
        }
    }

    public static ActionValueSync of(@NonNull Action action) {
        return new ActionValueSync(action);
    }

    public static ActionValueSync of(@NonNull Setting setting) {
        return new ActionValueSync(setting);
    }

    @SuppressWarnings({"DuplicatedCode"}) // IntelliJ bug
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

    public Optional<StarAction> starAction() {
        return Optional.ofNullable(starAction);
    }

    public Optional<ContactAction> contactAction() {
        return Optional.ofNullable(contactAction);
    }

    public Optional<MuteAction> muteAction() {
        return Optional.ofNullable(muteAction);
    }

    public Optional<PinAction> pinAction() {
        return Optional.ofNullable(pinAction);
    }

    public Optional<QuickReplyAction> quickReplyAction() {
        return Optional.ofNullable(quickReplyAction);
    }

    public Optional<RecentStickerWeightsAction> recentStickerWeightsAction() {
        return Optional.ofNullable(recentStickerWeightsAction);
    }

    public Optional<RecentEmojiWeightsAction> recentEmojiWeightsAction() {
        return Optional.ofNullable(recentEmojiWeightsAction);
    }

    public Optional<LabelEditAction> labelEditAction() {
        return Optional.ofNullable(labelEditAction);
    }

    public Optional<LabelAssociationAction> labelAssociationAction() {
        return Optional.ofNullable(labelAssociationAction);
    }

    public Optional<ArchiveChatAction> archiveChatAction() {
        return Optional.ofNullable(archiveChatAction);
    }

    public Optional<DeleteMessageForMeAction> deleteMessageForMeAction() {
        return Optional.ofNullable(deleteMessageForMeAction);
    }

    public Optional<MarkChatAsReadAction> markChatAsReadAction() {
        return Optional.ofNullable(markChatAsReadAction);
    }

    public Optional<ClearChatAction> clearChatAction() {
        return Optional.ofNullable(clearChatAction);
    }

    public Optional<DeleteChatAction> deleteChatAction() {
        return Optional.ofNullable(deleteChatAction);
    }

    public Optional<FavoriteStickerAction> favoriteStickerAction() {
        return Optional.ofNullable(favoriteStickerAction);
    }

    public Optional<AndroidUnsupportedActions> androidUnsupportedActions() {
        return Optional.ofNullable(androidUnsupportedActions);
    }

    public Optional<AgentAction> agentAction() {
        return Optional.ofNullable(agentAction);
    }

    public Optional<SubscriptionAction> subscriptionAction() {
        return Optional.ofNullable(subscriptionAction);
    }

    public Optional<UserStatusMuteAction> userStatusMuteAction() {
        return Optional.ofNullable(userStatusMuteAction);
    }

    public Optional<TimeFormatAction> timeFormatAction() {
        return Optional.ofNullable(timeFormatAction);
    }

    public Optional<NuxAction> nuxAction() {
        return Optional.ofNullable(nuxAction);
    }

    public Optional<PrimaryVersionAction> primaryVersionAction() {
        return Optional.ofNullable(primaryVersionAction);
    }

    public Optional<StickerAction> stickerAction() {
        return Optional.ofNullable(stickerAction);
    }

    public Optional<RemoveRecentStickerAction> removeRecentStickerAction() {
        return Optional.ofNullable(removeRecentStickerAction);
    }

    public Optional<ChatAssignmentAction> chatAssignmentAction() {
        return Optional.ofNullable(chatAssignmentAction);
    }

    public Optional<ChatAssignmentOpenedStatusAction> chatAssignmentOpenedStatusAction() {
        return Optional.ofNullable(chatAssignmentOpenedStatusAction);
    }

    public Optional<SecurityNotificationSetting> securityNotificationSetting() {
        return Optional.ofNullable(securityNotificationSetting);
    }

    public Optional<PushNameSetting> pushNameSetting() {
        return Optional.ofNullable(pushNameSetting);
    }

    public Optional<LocaleSetting> localeSetting() {
        return Optional.ofNullable(localeSetting);
    }

    public Optional<UnarchiveChatsSetting> unarchiveChatsSetting() {
        return Optional.ofNullable(unarchiveChatsSetting);
    }

    public Optional<StickerMetadata> stickerMetadata() {
        return Optional.ofNullable(stickerMetadata);
    }

    public Optional<KeyExpiration> keyExpiration() {
        return Optional.ofNullable(keyExpiration);
    }

    public Optional<PrimaryFeature> primaryFeature() {
        return Optional.ofNullable(primaryFeature);
    }

    @AllArgsConstructor
    @Jacksonized
    @Builder
    @Accessors(fluent = true)
    public static class PrimaryFeature implements ProtobufMessage {
        @ProtobufProperty(index = 1, type = STRING, repeated = true)
        @Getter
        private List<String> flags;
    }
}