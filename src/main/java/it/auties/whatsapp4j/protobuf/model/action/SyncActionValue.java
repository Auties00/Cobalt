package it.auties.whatsapp4j.protobuf.model.action;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.protobuf.model.key.KeyExpiration;
import it.auties.whatsapp4j.protobuf.model.recent.RecentStickerMetadata;
import it.auties.whatsapp4j.protobuf.model.setting.LocaleSetting;
import it.auties.whatsapp4j.protobuf.model.setting.PushNameSetting;
import it.auties.whatsapp4j.protobuf.model.setting.SecurityNotificationSetting;
import it.auties.whatsapp4j.protobuf.model.setting.UnarchiveChatsSetting;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncActionValue {
  @JsonProperty(value = "23")
  private UnarchiveChatsSetting unarchiveChatsSetting;

  @JsonProperty(value = "22")
  private DeleteChatAction deleteChatAction;

  @JsonProperty(value = "21")
  private ClearChatAction clearChatAction;

  @JsonProperty(value = "20")
  private MarkChatAsReadAction markChatAsReadAction;

  @JsonProperty(value = "19")
  private KeyExpiration keyExpiration;

  @JsonProperty(value = "18")
  private DeleteMessageForMeAction deleteMessageForMeAction;

  @JsonProperty(value = "17")
  private ArchiveChatAction archiveChatAction;

  @JsonProperty(value = "16")
  private LocaleSetting localeSetting;

  @JsonProperty(value = "15")
  private LabelAssociationAction labelAssociationAction;

  @JsonProperty(value = "14")
  private LabelEditAction labelEditAction;

  @JsonProperty(value = "11")
  private RecentEmojiWeightsAction recentEmojiWeightsAction;

  @JsonProperty(value = "10")
  private RecentStickerMetadata recentStickerMetadata;

  @JsonProperty(value = "9")
  private RecentStickerWeightsAction recentStickerWeightsAction;

  @JsonProperty(value = "8")
  private QuickReplyAction quickReplyAction;

  @JsonProperty(value = "7")
  private PushNameSetting pushNameSetting;

  @JsonProperty(value = "6")
  private SecurityNotificationSetting securityNotificationSetting;

  @JsonProperty(value = "5")
  private PinAction pinAction;

  @JsonProperty(value = "4")
  private MuteAction muteAction;

  @JsonProperty(value = "3")
  private ContactAction contactAction;

  @JsonProperty(value = "2")
  private StarAction starAction;

  @JsonProperty(value = "1")
  private long timestamp;
}
