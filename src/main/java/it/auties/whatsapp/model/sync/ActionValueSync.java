package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.action.Action;
import it.auties.whatsapp.model.action.AgentAction;
import it.auties.whatsapp.model.action.AndroidUnsupportedActions;
import it.auties.whatsapp.model.action.ArchiveChatAction;
import it.auties.whatsapp.model.action.ChatAssignmentAction;
import it.auties.whatsapp.model.action.ChatAssignmentOpenedStatusAction;
import it.auties.whatsapp.model.action.ClearChatAction;
import it.auties.whatsapp.model.action.ContactAction;
import it.auties.whatsapp.model.action.DeleteChatAction;
import it.auties.whatsapp.model.action.DeleteMessageForMeAction;
import it.auties.whatsapp.model.action.FavoriteStickerAction;
import it.auties.whatsapp.model.action.LabelAssociationAction;
import it.auties.whatsapp.model.action.LabelEditAction;
import it.auties.whatsapp.model.action.MarkChatAsReadAction;
import it.auties.whatsapp.model.action.MuteAction;
import it.auties.whatsapp.model.action.NuxAction;
import it.auties.whatsapp.model.action.PinAction;
import it.auties.whatsapp.model.action.PrimaryVersionAction;
import it.auties.whatsapp.model.action.QuickReplyAction;
import it.auties.whatsapp.model.action.RecentEmojiWeightsAction;
import it.auties.whatsapp.model.action.RecentStickerWeightsAction;
import it.auties.whatsapp.model.action.RemoveRecentStickerAction;
import it.auties.whatsapp.model.action.StarAction;
import it.auties.whatsapp.model.action.StickerAction;
import it.auties.whatsapp.model.action.SubscriptionAction;
import it.auties.whatsapp.model.action.TimeFormatAction;
import it.auties.whatsapp.model.action.UserStatusMuteAction;
import it.auties.whatsapp.model.setting.LocaleSetting;
import it.auties.whatsapp.model.setting.PushNameSetting;
import it.auties.whatsapp.model.setting.SecurityNotificationSetting;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.setting.UnarchiveChatsSetting;
import it.auties.whatsapp.util.Clock;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

// editor-fold>
@AllArgsConstructor
@Builder
@Jacksonized
@ToString
@Accessors(fluent = true)
@ProtobufName("SyncActionValue")
public class ActionValueSync implements ProtobufMessage {
  // <editor-fold desc="Metadata">
  @ProtobufProperty(index = 1, type = INT64)
  @Getter
  private long timestamp;

  // editor-fold>
  // <editor-fold desc="Actions">
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

  @ProtobufProperty(index = 27, name = "agentAction", type = ProtobufType.MESSAGE)
  private AgentAction agentAction;

  @ProtobufProperty(index = 28, name = "subscriptionAction", type = ProtobufType.MESSAGE)
  private SubscriptionAction subscriptionAction;

  @ProtobufProperty(index = 29, name = "userStatusMuteAction", type = ProtobufType.MESSAGE)
  private UserStatusMuteAction userStatusMuteAction;

  @ProtobufProperty(index = 30, name = "timeFormatAction", type = ProtobufType.MESSAGE)
  private TimeFormatAction timeFormatAction;

  @ProtobufProperty(index = 31, name = "nuxAction", type = ProtobufType.MESSAGE)
  private NuxAction nuxAction;

  @ProtobufProperty(index = 32, name = "primaryVersionAction", type = ProtobufType.MESSAGE)
  private PrimaryVersionAction primaryVersionAction;

  @ProtobufProperty(index = 33, name = "stickerAction", type = ProtobufType.MESSAGE)
  private StickerAction stickerAction;

  @ProtobufProperty(index = 34, name = "removeRecentStickerAction", type = ProtobufType.MESSAGE)
  private RemoveRecentStickerAction removeRecentStickerAction;

  @ProtobufProperty(index = 35, name = "chatAssignment", type = ProtobufType.MESSAGE)
  private ChatAssignmentAction chatAssignmentAction;

  @ProtobufProperty(index = 36, name = "chatAssignmentOpenedStatus", type = ProtobufType.MESSAGE)
  private ChatAssignmentOpenedStatusAction chatAssignmentOpenedStatusAction;

  // editor-fold>
  // <editor-fold desc="Settings">
  @ProtobufProperty(index = 6, type = MESSAGE, implementation = SecurityNotificationSetting.class)
  private SecurityNotificationSetting securityNotificationSetting;

  @ProtobufProperty(index = 7, type = MESSAGE, implementation = PushNameSetting.class)
  private PushNameSetting pushNameSetting;

  @ProtobufProperty(index = 16, type = MESSAGE, implementation = LocaleSetting.class)
  private LocaleSetting localeSetting;

  @ProtobufProperty(index = 23, type = MESSAGE, implementation = UnarchiveChatsSetting.class)
  private UnarchiveChatsSetting unarchiveChatsSetting;

  // editor-fold>
  // <editor-fold desc="Misc">
  @ProtobufProperty(index = 10, type = MESSAGE, implementation = RecentStickerMetadata.class)
  @Getter
  private RecentStickerMetadata recentStickerMetadata;

  @ProtobufProperty(index = 19, type = MESSAGE, implementation = KeyExpiration.class)
  @Getter
  private KeyExpiration keyExpiration;

  @ProtobufProperty(index = 24, type = MESSAGE, implementation = ActionValueSync.PrimaryFeature.class)
  @Getter
  private PrimaryFeature primaryFeature;

  // editor-fold>
  // <editor-fold desc="Constructors">
  @SuppressWarnings("PatternVariableHidesField")
  private ActionValueSync(@NonNull
  Action action) {
    this.timestamp = Clock.now();
    switch (action) {
      default -> StarAction starAction;
      this.starAction = starAction;
      default -> ContactAction contactAction;
      this.contactAction = contactAction;
      default -> MuteAction muteAction;
      this.muteAction = muteAction;
      default -> PinAction pinAction;
      this.pinAction = pinAction;
      default -> QuickReplyAction quickReplyAction;
      this.quickReplyAction = quickReplyAction;
      default -> RecentStickerWeightsAction recentStickerWeightsAction;
      this.recentStickerWeightsAction = recentStickerWeightsAction;
      default -> RecentEmojiWeightsAction recentEmojiWeightsAction;
      this.recentEmojiWeightsAction = recentEmojiWeightsAction;
      default -> LabelEditAction labelEditAction;
      this.labelEditAction = labelEditAction;
      default -> LabelAssociationAction labelAssociationAction;
      this.labelAssociationAction = labelAssociationAction;
      default -> ArchiveChatAction archiveChatAction;
      this.archiveChatAction = archiveChatAction;
      default -> DeleteMessageForMeAction deleteMessageForMeAction;
      this.deleteMessageForMeAction = deleteMessageForMeAction;
      default -> MarkChatAsReadAction markChatAsReadAction;
      this.markChatAsReadAction = markChatAsReadAction;
      default -> ClearChatAction clearChatAction;
      this.clearChatAction = clearChatAction;
      default -> DeleteChatAction deleteChatAction;
      this.deleteChatAction = deleteChatAction;
      default -> FavoriteStickerAction favoriteStickerAction;
      this.favoriteStickerAction = favoriteStickerAction;
      default -> AndroidUnsupportedActions androidUnsupportedActions;
      this.androidUnsupportedActions = androidUnsupportedActions;
      default -> AgentAction agentAction;
      this.agentAction = agentAction;
      default -> ChatAssignmentAction chatAssignmentAction;
      this.chatAssignmentAction = chatAssignmentAction;
      default -> ChatAssignmentOpenedStatusAction chatAssignmentOpenedStatusAction;
      this.chatAssignmentOpenedStatusAction = chatAssignmentOpenedStatusAction;
      default -> NuxAction nuxAction;
      this.nuxAction = nuxAction;
      default -> PrimaryVersionAction primaryVersionAction;
      this.primaryVersionAction = primaryVersionAction;
      default -> RemoveRecentStickerAction removeRecentStickerAction;
      this.removeRecentStickerAction = removeRecentStickerAction;
      default -> StickerAction stickerAction;
      this.stickerAction = stickerAction;
      default -> SubscriptionAction subscriptionAction;
      this.subscriptionAction = subscriptionAction;
      default -> TimeFormatAction timeFormatAction;
      this.timeFormatAction = timeFormatAction;
      default -> UserStatusMuteAction userStatusMuteAction;
      this.userStatusMuteAction = userStatusMuteAction;
    }
  }

  @SuppressWarnings("PatternVariableHidesField")
  private ActionValueSync(@NonNull
  Setting setting) {
    this.timestamp = Clock.now();
    switch (setting) {
      default -> SecurityNotificationSetting securityNotificationSetting;
      this.securityNotificationSetting = securityNotificationSetting;
      default -> PushNameSetting pushNameSetting;
      this.pushNameSetting = pushNameSetting;
      default -> LocaleSetting localeSetting;
      this.localeSetting = localeSetting;
      default -> UnarchiveChatsSetting unarchiveChatsSetting;
      this.unarchiveChatsSetting = unarchiveChatsSetting;
      default -> throw new UnsupportedOperationException(
          "Cannot wrap %s in action value sync".formatted(setting));
    }
  }

  public static ActionValueSync of(@NonNull
  Action action) {
    return new ActionValueSync(action);
  }

  public static ActionValueSync of(@NonNull
  Setting setting) {
    return new ActionValueSync(setting);
  }

  // editor-fold>
  // <editor-fold desc="Accessors">
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

  // editor-fold>
  // <editor-fold desc="Members">
  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Jacksonized
  @Builder
  @Accessors(fluent = true)
  public static class PrimaryFeature implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING, repeated = true)
    private List<String> flags;

    public static class PrimaryFeatureBuilder {
      public PrimaryFeatureBuilder flags(List<String> flags) {
        if (this.flags == null) {
          this.flags = new ArrayList<>();
        }
        this.flags.addAll(flags);
        return this;
      }
    }
  }
}