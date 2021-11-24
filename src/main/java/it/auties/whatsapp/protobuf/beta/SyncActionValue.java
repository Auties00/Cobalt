package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncActionValue {

  @JsonProperty(value = "23", required = false)
  @JsonPropertyDescription("UnarchiveChatsSetting")
  private UnarchiveChatsSetting unarchiveChatsSetting;

  @JsonProperty(value = "22", required = false)
  @JsonPropertyDescription("DeleteChatAction")
  private DeleteChatAction deleteChatAction;

  @JsonProperty(value = "21", required = false)
  @JsonPropertyDescription("ClearChatAction")
  private ClearChatAction clearChatAction;

  @JsonProperty(value = "20", required = false)
  @JsonPropertyDescription("MarkChatAsReadAction")
  private MarkChatAsReadAction markChatAsReadAction;

  @JsonProperty(value = "19", required = false)
  @JsonPropertyDescription("KeyExpiration")
  private KeyExpiration keyExpiration;

  @JsonProperty(value = "18", required = false)
  @JsonPropertyDescription("DeleteMessageForMeAction")
  private DeleteMessageForMeAction deleteMessageForMeAction;

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ArchiveChatAction")
  private ArchiveChatAction archiveChatAction;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("LocaleSetting")
  private LocaleSetting localeSetting;

  @JsonProperty(value = "15", required = false)
  @JsonPropertyDescription("LabelAssociationAction")
  private LabelAssociationAction labelAssociationAction;

  @JsonProperty(value = "14", required = false)
  @JsonPropertyDescription("LabelEditAction")
  private LabelEditAction labelEditAction;

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("RecentEmojiWeightsAction")
  private RecentEmojiWeightsAction recentEmojiWeightsAction;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("RecentStickerMetadata")
  private RecentStickerMetadata recentStickerMetadata;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("RecentStickerWeightsAction")
  private RecentStickerWeightsAction recentStickerWeightsAction;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("QuickReplyAction")
  private QuickReplyAction quickReplyAction;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("PushNameSetting")
  private PushNameSetting pushNameSetting;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("SecurityNotificationSetting")
  private SecurityNotificationSetting securityNotificationSetting;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("PinAction")
  private PinAction pinAction;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("MuteAction")
  private MuteAction muteAction;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("ContactAction")
  private ContactAction contactAction;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("StarAction")
  private StarAction starAction;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("int64")
  private long timestamp;
}
