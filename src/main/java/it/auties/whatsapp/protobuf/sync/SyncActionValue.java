package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.action.*;
import it.auties.whatsapp.protobuf.action.ArchiveChatAction;
import it.auties.whatsapp.protobuf.action.ClearChatAction;
import it.auties.whatsapp.protobuf.action.ContactAction;
import it.auties.whatsapp.protobuf.action.DeleteChatAction;
import it.auties.whatsapp.protobuf.action.DeleteMessageForMeAction;
import it.auties.whatsapp.protobuf.action.LabelAssociationAction;
import it.auties.whatsapp.protobuf.action.LabelEditAction;
import it.auties.whatsapp.protobuf.action.MarkChatAsReadAction;
import it.auties.whatsapp.protobuf.action.MuteAction;
import it.auties.whatsapp.protobuf.action.PinAction;
import it.auties.whatsapp.protobuf.action.QuickReplyAction;
import it.auties.whatsapp.protobuf.action.RecentEmojiWeightsAction;
import it.auties.whatsapp.protobuf.action.RecentStickerWeightsAction;
import it.auties.whatsapp.protobuf.action.StarAction;
import it.auties.whatsapp.protobuf.unknown.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncActionValue {
  @JsonProperty(value = "23")
  @JsonPropertyDescription("UnarchiveChatsSetting")
  private UnarchiveChatsSetting unarchiveChatsSetting;

  @JsonProperty(value = "22")
  @JsonPropertyDescription("DeleteChatAction")
  private DeleteChatAction deleteChatAction;

  @JsonProperty(value = "21")
  @JsonPropertyDescription("ClearChatAction")
  private ClearChatAction clearChatAction;

  @JsonProperty(value = "20")
  @JsonPropertyDescription("MarkChatAsReadAction")
  private MarkChatAsReadAction markChatAsReadAction;

  @JsonProperty(value = "19")
  @JsonPropertyDescription("KeyExpiration")
  private KeyExpiration keyExpiration;

  @JsonProperty(value = "18")
  @JsonPropertyDescription("DeleteMessageForMeAction")
  private DeleteMessageForMeAction deleteMessageForMeAction;

  @JsonProperty(value = "17")
  @JsonPropertyDescription("ArchiveChatAction")
  private ArchiveChatAction archiveChatAction;

  @JsonProperty(value = "16")
  @JsonPropertyDescription("LocaleSetting")
  private LocaleSetting localeSetting;

  @JsonProperty(value = "15")
  @JsonPropertyDescription("LabelAssociationAction")
  private LabelAssociationAction labelAssociationAction;

  @JsonProperty(value = "14")
  @JsonPropertyDescription("LabelEditAction")
  private LabelEditAction labelEditAction;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("RecentEmojiWeightsAction")
  private RecentEmojiWeightsAction recentEmojiWeightsAction;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("RecentStickerMetadata")
  private RecentStickerMetadata recentStickerMetadata;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("RecentStickerWeightsAction")
  private RecentStickerWeightsAction recentStickerWeightsAction;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("QuickReplyAction")
  private QuickReplyAction quickReplyAction;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("PushNameSetting")
  private PushNameSetting pushNameSetting;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("SecurityNotificationSetting")
  private SecurityNotificationSetting securityNotificationSetting;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("PinAction")
  private PinAction pinAction;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("MuteAction")
  private MuteAction muteAction;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("ContactAction")
  private ContactAction contactAction;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("StarAction")
  private StarAction starAction;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("int64")
  private long timestamp;
}
