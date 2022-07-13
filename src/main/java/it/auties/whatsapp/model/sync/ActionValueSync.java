package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.setting.*;
import it.auties.whatsapp.util.Clock;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Builder
@Jacksonized
@ToString
@Accessors(fluent = true)
public class ActionValueSync implements ProtobufMessage {
    //<editor-fold desc="Metadata">
    @ProtobufProperty(index = 1, type = INT64)
    @Getter
    private long timestamp;
    //</editor-fold>

    //<editor-fold desc="Actions">
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = StarAction.class)
    private StarAction starAction;

    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ContactAction.class)
    private ContactAction contactAction;

    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = MuteAction.class)
    private MuteAction muteAction;

    @ProtobufProperty(index = 5, type = MESSAGE, concreteType = PinAction.class)
    private PinAction pinAction;

    @ProtobufProperty(index = 8, type = MESSAGE, concreteType = QuickReplyAction.class)
    private QuickReplyAction quickReplyAction;

    @ProtobufProperty(index = 9, type = MESSAGE, concreteType = RecentStickerWeightsAction.class)
    private RecentStickerWeightsAction recentStickerWeightsAction;

    @ProtobufProperty(index = 11, type = MESSAGE, concreteType = RecentEmojiWeightsAction.class)
    private RecentEmojiWeightsAction recentEmojiWeightsAction;

    @ProtobufProperty(index = 14, type = MESSAGE, concreteType = LabelEditAction.class)
    private LabelEditAction labelEditAction;

    @ProtobufProperty(index = 15, type = MESSAGE, concreteType = LabelAssociationAction.class)
    private LabelAssociationAction labelAssociationAction;

    @ProtobufProperty(index = 17, type = MESSAGE, concreteType = ArchiveChatAction.class)
    private ArchiveChatAction archiveChatAction;

    @ProtobufProperty(index = 18, type = MESSAGE, concreteType = DeleteMessageForMeAction.class)
    private DeleteMessageForMeAction deleteMessageForMeAction;

    @ProtobufProperty(index = 20, type = MESSAGE, concreteType = MarkChatAsReadAction.class)
    private MarkChatAsReadAction markChatAsReadAction;

    @ProtobufProperty(index = 21, type = MESSAGE, concreteType = ClearChatAction.class)
    private ClearChatAction clearChatAction;

    @ProtobufProperty(index = 22, type = MESSAGE, concreteType = DeleteChatAction.class)
    private DeleteChatAction deleteChatAction;

    @ProtobufProperty(index = 25, type = MESSAGE, concreteType = FavoriteStickerAction.class)
    private FavoriteStickerAction favoriteStickerAction;

    @ProtobufProperty(index = 26, type = MESSAGE, concreteType = AndroidUnsupportedActions.class)
    private AndroidUnsupportedActions androidUnsupportedActions;
    //</editor-fold>

    //<editor-fold desc="Settings">
    @ProtobufProperty(index = 6, type = MESSAGE, concreteType = SecurityNotificationSetting.class)
    private SecurityNotificationSetting securityNotificationSetting;

    @ProtobufProperty(index = 7, type = MESSAGE, concreteType = PushNameSetting.class)
    private PushNameSetting pushNameSetting;

    @ProtobufProperty(index = 16, type = MESSAGE, concreteType = LocaleSetting.class)
    private LocaleSetting localeSetting;

    @ProtobufProperty(index = 23, type = MESSAGE, concreteType = UnarchiveChatsSetting.class)
    private UnarchiveChatsSetting unarchiveChatsSetting;
    //</editor-fold>

    //<editor-fold desc="Misc">
    @ProtobufProperty(index = 10, type = MESSAGE, concreteType = RecentStickerMetadata.class)
    @Getter
    private RecentStickerMetadata recentStickerMetadata;

    @ProtobufProperty(index = 19, type = MESSAGE, concreteType = KeyExpiration.class)
    @Getter
    private KeyExpiration keyExpiration;

    @ProtobufProperty(index = 24, type = MESSAGE, concreteType = PrimaryFeature.class)
    @Getter
    private PrimaryFeature primaryFeature;

    //</editor-fold>

    //<editor-fold desc="Constructors">
    @SuppressWarnings("PatternVariableHidesField")
    private ActionValueSync(@NonNull Action action) {
        this.timestamp = Clock.now();
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
        }
    }

    @SuppressWarnings("PatternVariableHidesField")
    private ActionValueSync(@NonNull Setting setting) {
        this.timestamp = Clock.now();
        switch (setting) {
            case SecurityNotificationSetting securityNotificationSetting ->
                    this.securityNotificationSetting = securityNotificationSetting;
            case PushNameSetting pushNameSetting -> this.pushNameSetting = pushNameSetting;
            case LocaleSetting localeSetting -> this.localeSetting = localeSetting;
            case UnarchiveChatsSetting unarchiveChatsSetting -> this.unarchiveChatsSetting = unarchiveChatsSetting;
            case EphemeralSetting ephemeralSetting -> throw new UnsupportedOperationException(
                    "Cannot wrap %s in action value sync".formatted(ephemeralSetting));
        }
    }

    public static ActionValueSync of(@NonNull Action action) {
        return new ActionValueSync(action);
    }

    public static ActionValueSync of(@NonNull Setting setting) {
        return new ActionValueSync(setting);
    }

    //</editor-fold>

    //<editor-fold desc="Accessors">
    public Action action() {
        if (starAction != null)
            return starAction;
        if (contactAction != null)
            return contactAction;
        if (muteAction != null)
            return muteAction;
        if (pinAction != null)
            return pinAction;
        if (quickReplyAction != null)
            return quickReplyAction;
        if (recentStickerWeightsAction != null)
            return recentStickerWeightsAction;
        if (recentEmojiWeightsAction != null)
            return recentEmojiWeightsAction;
        if (labelEditAction != null)
            return labelEditAction;
        if (labelAssociationAction != null)
            return labelAssociationAction;
        if (archiveChatAction != null)
            return archiveChatAction;
        if (deleteMessageForMeAction != null)
            return deleteMessageForMeAction;
        if (markChatAsReadAction != null)
            return markChatAsReadAction;
        if (clearChatAction != null)
            return clearChatAction;
        if (deleteChatAction != null)
            return deleteChatAction;
        if (favoriteStickerAction != null)
            return favoriteStickerAction;
        if (androidUnsupportedActions != null)
            return androidUnsupportedActions;
        return null;
    }

    public Setting setting() {
        if (securityNotificationSetting != null)
            return securityNotificationSetting;
        if (pushNameSetting != null)
            return pushNameSetting;
        if (localeSetting != null)
            return localeSetting;
        if (unarchiveChatsSetting != null)
            return unarchiveChatsSetting;
        return null;
    }

    //</editor-fold>

    //<editor-fold desc="Members">
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
                if (this.flags == null)
                    this.flags = new ArrayList<>();
                this.flags.addAll(flags);
                return this;
            }
        }
    }

    //</editor-fold>
}
