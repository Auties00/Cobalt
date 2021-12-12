package it.auties.whatsapp.protobuf.signal.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class WebFeatures {
  @JsonProperty(value = "46")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag mdForceUpgrade;

  @JsonProperty(value = "45")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag ephemeral24HDuration;

  @JsonProperty(value = "44")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag ephemeralAllowGroupMembers;

  @JsonProperty(value = "43")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag archiveV2;

  @JsonProperty(value = "42")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag settingsSync;

  @JsonProperty(value = "41")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupDogfoodingInternalOnly;

  @JsonProperty(value = "40")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupUiiCleanup;

  @JsonProperty(value = "39")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag support;

  @JsonProperty(value = "37")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag userNotice;

  @JsonProperty(value = "36")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag recentStickersV3;

  @JsonProperty(value = "34")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag recentStickersV2;

  @JsonProperty(value = "33")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag e2ENotificationSync;

  @JsonProperty(value = "32")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag ephemeralMessages;

  @JsonProperty(value = "31")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag templateMessageInteractivity;

  @JsonProperty(value = "30")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag templateMessage;

  @JsonProperty(value = "29")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipGroupCall;

  @JsonProperty(value = "28")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag starredStickers;

  @JsonProperty(value = "27")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag catalog;

  @JsonProperty(value = "26")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag recentStickers;

  @JsonProperty(value = "25")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupsV4JoinPermission;

  @JsonProperty(value = "24")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag frequentlyForwardedSetting;

  @JsonProperty(value = "23")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag thirdPartyStickers;

  @JsonProperty(value = "22")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipIndividualVideo;

  @JsonProperty(value = "21")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag statusRanking;

  @JsonProperty(value = "20")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag videoPlaybackUrl;

  @JsonProperty(value = "19")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag vnameV2;

  @JsonProperty(value = "18")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag mediaUploadRichQuickReplies;

  @JsonProperty(value = "15")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag mediaUpload;

  @JsonProperty(value = "14")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag labelsEdit;

  @JsonProperty(value = "13")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag liveLocationsFinal;

  @JsonProperty(value = "12")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag stickerPackQuery;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag payments;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag quickRepliesQuery;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipIndividualIncoming;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag queryVname;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag liveLocations;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag queryStatusV3Thumbnail;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag changeNumberV2;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupsV3Create;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupsV3;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipIndividualOutgoing;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag labelsDisplay;

  @Accessors(fluent = true)
  public enum WebFeaturesFlag {
    NOT_STARTED(0),
    FORCE_UPGRADE(1),
    DEVELOPMENT(2),
    PRODUCTION(3);

    private final @Getter int index;

    WebFeaturesFlag(int index) {
      this.index = index;
    }

    @JsonCreator
    public static WebFeaturesFlag forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
