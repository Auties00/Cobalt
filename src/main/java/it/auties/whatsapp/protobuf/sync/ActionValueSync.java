package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.action.*;
import it.auties.whatsapp.protobuf.setting.LocaleSetting;
import it.auties.whatsapp.protobuf.setting.PushNameSetting;
import it.auties.whatsapp.protobuf.setting.SecurityNotificationSetting;
import it.auties.whatsapp.protobuf.setting.UnarchiveChatsSetting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ActionValueSync implements GenericSync {
    @JsonProperty("1")
    @JsonPropertyDescription("int64")
    private long timestamp;

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

    @JsonProperty("6")
    @JsonPropertyDescription("SecurityNotificationSetting")
    private SecurityNotificationSetting securityNotificationSetting;

    @JsonProperty("7")
    @JsonPropertyDescription("PushNameSetting")
    private PushNameSetting pushNameSetting;

    @JsonProperty("8")
    @JsonPropertyDescription("QuickReplyAction")
    private QuickReplyAction quickReplyAction;

    @JsonProperty("9")
    @JsonPropertyDescription("RecentStickerWeightsAction")
    private RecentStickerWeightsAction recentStickerWeightsAction;

    @JsonProperty("10")
    @JsonPropertyDescription("RecentStickerMetadata")
    private RecentStickerMetadata recentStickerMetadata;

    @JsonProperty("11")
    @JsonPropertyDescription("RecentEmojiWeightsAction")
    private RecentEmojiWeightsAction recentEmojiWeightsAction;

    @JsonProperty("14")
    @JsonPropertyDescription("LabelEditAction")
    private LabelEditAction labelEditAction;

    @JsonProperty("15")
    @JsonPropertyDescription("LabelAssociationAction")
    private LabelAssociationAction labelAssociationAction;

    @JsonProperty("16")
    @JsonPropertyDescription("LocaleSetting")
    private LocaleSetting localeSetting;

    @JsonProperty("17")
    @JsonPropertyDescription("ArchiveChatAction")
    private ArchiveChatAction archiveChatAction;

    @JsonProperty("18")
    @JsonPropertyDescription("DeleteMessageForMeAction")
    private DeleteMessageForMeAction deleteMessageForMeAction;

    @JsonProperty("19")
    @JsonPropertyDescription("KeyExpiration")
    private KeyExpiration keyExpiration;

    @JsonProperty("20")
    @JsonPropertyDescription("MarkChatAsReadAction")
    private MarkChatAsReadAction markChatAsReadAction;

    @JsonProperty("21")
    @JsonPropertyDescription("ClearChatAction")
    private ClearChatAction clearChatAction;

    @JsonProperty("22")
    @JsonPropertyDescription("DeleteChatAction")
    private DeleteChatAction deleteChatAction;

    @JsonProperty("23")
    @JsonPropertyDescription("UnarchiveChatsSetting")
    private UnarchiveChatsSetting unarchiveChatsSetting;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @Accessors(fluent = true)
    public static class RecentEmojiWeightsAction {
        @JsonProperty("1")
        @JsonPropertyDescription("RecentEmojiWeight")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<RecentEmojiWeight> weights;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @Accessors(fluent = true)
    public static class RecentStickerWeightsAction {
        @JsonProperty("1")
        @JsonPropertyDescription("RecentStickerWeight")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<RecentStickerWeight> weights;
    }
}
