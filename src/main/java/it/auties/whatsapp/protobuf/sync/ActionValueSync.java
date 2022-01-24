package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.action.*;
import it.auties.whatsapp.protobuf.setting.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(fluent = true)
public final class ActionValueSync implements GenericSync {
    @JsonProperty("1")
    @JsonPropertyDescription("int64")
    @Getter
    private long timestamp;

    //<editor-fold desc="Actions">
    @JsonProperty("2")
    @JsonPropertyDescription("StarAction")
    private StarAction starAction;

    @JsonProperty("3")
    @JsonPropertyDescription("ContactAction")
    private ContactAction contactAction;

    @JsonProperty("4")
    @JsonPropertyDescription("MuteAction")
    private MuteAction muteAction;

    @JsonProperty("5")
    @JsonPropertyDescription("PinAction")
    private PinAction pinAction;

    @JsonProperty("8")
    @JsonPropertyDescription("QuickReplyAction")
    private QuickReplyAction quickReplyAction;

    @JsonProperty("9")
    @JsonPropertyDescription("RecentStickerWeightsAction")
    private RecentStickerWeightsAction recentStickerWeightsAction;

    @JsonProperty("11")
    @JsonPropertyDescription("RecentEmojiWeightsAction")
    private RecentEmojiWeightsAction recentEmojiWeightsAction;

    @JsonProperty("14")
    @JsonPropertyDescription("LabelEditAction")
    private LabelEditAction labelEditAction;

    @JsonProperty("15")
    @JsonPropertyDescription("LabelAssociationAction")
    private LabelAssociationAction labelAssociationAction;


    @JsonProperty("17")
    @JsonPropertyDescription("ArchiveChatAction")
    private ArchiveChatAction archiveChatAction;

    @JsonProperty("18")
    @JsonPropertyDescription("DeleteMessageForMeAction")
    private DeleteMessageForMeAction deleteMessageForMeAction;

    @JsonProperty("20")
    @JsonPropertyDescription("MarkChatAsReadAction")
    private MarkChatAsReadAction markChatAsReadAction;

    @JsonProperty("21")
    @JsonPropertyDescription("ClearChatAction")
    private ClearChatAction clearChatAction;

    @JsonProperty("22")
    @JsonPropertyDescription("DeleteChatAction")
    private DeleteChatAction deleteChatAction;

    @JsonProperty("26")
    @JsonPropertyDescription("FavoriteStickerAction")
    private FavoriteStickerAction favoriteStickerAction;

    @JsonProperty("25")
    @JsonPropertyDescription("AndroidUnsupportedActions")
    private AndroidUnsupportedActions androidUnsupportedActions;
    //</editor-fold>

    //<editor-fold desc="Settings">
    @JsonProperty("6")
    @JsonPropertyDescription("SecurityNotificationSetting")
    private SecurityNotificationSetting securityNotificationSetting;

    @JsonProperty("7")
    @JsonPropertyDescription("PushNameSetting")
    private PushNameSetting pushNameSetting;

    @JsonProperty("16")
    @JsonPropertyDescription("LocaleSetting")
    private LocaleSetting localeSetting;


    @JsonProperty("23")
    @JsonPropertyDescription("UnarchiveChatsSetting")
    private UnarchiveChatsSetting unarchiveChatsSetting;
    //</editor-fold>

    //<editor-fold desc="Misc">
    @JsonProperty("10")
    @JsonPropertyDescription("RecentStickerMetadata")
    @Getter
    private RecentStickerMetadata recentStickerMetadata;

    @JsonProperty("19")
    @JsonPropertyDescription("KeyExpiration")
    @Getter
    private KeyExpiration keyExpiration;

    @JsonProperty("24")
    @JsonPropertyDescription("PrimaryFeature")
    @Getter
    private PrimaryFeature primaryFeature;
    //</editor-fold>

    public Action action(){
        if(starAction != null) return starAction;
        if(contactAction != null) return contactAction;
        if(muteAction != null) return muteAction;
        if(pinAction != null) return pinAction;
        if(quickReplyAction != null) return quickReplyAction;
        if(recentStickerWeightsAction != null) return recentStickerWeightsAction;
        if(recentEmojiWeightsAction != null) return recentEmojiWeightsAction;
        if(labelEditAction != null) return labelEditAction;
        if(labelAssociationAction != null) return labelAssociationAction;
        if(archiveChatAction != null) return archiveChatAction;
        if(deleteMessageForMeAction != null) return deleteMessageForMeAction;
        if(markChatAsReadAction != null) return markChatAsReadAction;
        if(clearChatAction != null) return clearChatAction;
        if(deleteChatAction != null) return deleteChatAction;
        if(favoriteStickerAction != null) return favoriteStickerAction;
        if(androidUnsupportedActions != null) return androidUnsupportedActions;
        return null;
    }

    public Setting setting(){
        if(securityNotificationSetting != null) return securityNotificationSetting;
        if(pushNameSetting != null) return pushNameSetting;
        if(localeSetting != null) return localeSetting;
        if(unarchiveChatsSetting != null) return unarchiveChatsSetting;
        return null;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @Accessors(fluent = true)
    public static class PrimaryFeature {
        @JsonProperty("1")
        @JsonPropertyDescription("string")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<String> flags;
    }
}
