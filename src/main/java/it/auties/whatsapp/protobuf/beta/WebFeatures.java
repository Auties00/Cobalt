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
public class WebFeatures {

  @JsonProperty(value = "46", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag mdForceUpgrade;

  @JsonProperty(value = "45", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag ephemeral24HDuration;

  @JsonProperty(value = "44", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag ephemeralAllowGroupMembers;

  @JsonProperty(value = "43", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag archiveV2;

  @JsonProperty(value = "42", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag settingsSync;

  @JsonProperty(value = "41", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupDogfoodingInternalOnly;

  @JsonProperty(value = "40", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupUiiCleanup;

  @JsonProperty(value = "39", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag support;

  @JsonProperty(value = "37", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag userNotice;

  @JsonProperty(value = "36", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag recentStickersV3;

  @JsonProperty(value = "34", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag recentStickersV2;

  @JsonProperty(value = "33", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag e2ENotificationSync;

  @JsonProperty(value = "32", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag ephemeralMessages;

  @JsonProperty(value = "31", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag templateMessageInteractivity;

  @JsonProperty(value = "30", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag templateMessage;

  @JsonProperty(value = "29", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipGroupCall;

  @JsonProperty(value = "28", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag starredStickers;

  @JsonProperty(value = "27", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag catalog;

  @JsonProperty(value = "26", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag recentStickers;

  @JsonProperty(value = "25", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupsV4JoinPermission;

  @JsonProperty(value = "24", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag frequentlyForwardedSetting;

  @JsonProperty(value = "23", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag thirdPartyStickers;

  @JsonProperty(value = "22", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipIndividualVideo;

  @JsonProperty(value = "21", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag statusRanking;

  @JsonProperty(value = "20", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag videoPlaybackUrl;

  @JsonProperty(value = "19", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag vnameV2;

  @JsonProperty(value = "18", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag mediaUploadRichQuickReplies;

  @JsonProperty(value = "15", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag mediaUpload;

  @JsonProperty(value = "14", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag labelsEdit;

  @JsonProperty(value = "13", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag liveLocationsFinal;

  @JsonProperty(value = "12", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag stickerPackQuery;

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag payments;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag quickRepliesQuery;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipIndividualIncoming;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag queryVname;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag liveLocations;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag queryStatusV3Thumbnail;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag changeNumberV2;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupsV3Create;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupsV3;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipIndividualOutgoing;

  @JsonProperty(value = "1", required = false)
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
