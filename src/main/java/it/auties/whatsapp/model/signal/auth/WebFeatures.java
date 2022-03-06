package it.auties.whatsapp.model.signal.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class WebFeatures {
  @JsonProperty("46")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag mdForceUpgrade;

  @JsonProperty("45")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag ephemeral24HDuration;

  @JsonProperty("44")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag ephemeralAllowGroupMembers;

  @JsonProperty("43")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag archiveV2;

  @JsonProperty("42")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag settingsSync;

  @JsonProperty("41")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupDogfoodingInternalOnly;

  @JsonProperty("40")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupUiiCleanup;

  @JsonProperty("39")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag support;

  @JsonProperty("37")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag userNotice;

  @JsonProperty("36")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag recentStickersV3;

  @JsonProperty("34")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag recentStickersV2;

  @JsonProperty("33")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag e2ENotificationSync;

  @JsonProperty("32")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag ephemeralMessages;

  @JsonProperty("31")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag templateMessageInteractivity;

  @JsonProperty("30")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag templateMessage;

  @JsonProperty("29")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipGroupCall;

  @JsonProperty("28")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag starredStickers;

  @JsonProperty("27")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag catalog;

  @JsonProperty("26")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag recentStickers;

  @JsonProperty("25")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupsV4JoinPermission;

  @JsonProperty("24")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag frequentlyForwardedSetting;

  @JsonProperty("23")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag thirdPartyStickers;

  @JsonProperty("22")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipIndividualVideo;

  @JsonProperty("21")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag statusRanking;

  @JsonProperty("20")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag videoPlaybackUrl;

  @JsonProperty("19")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag vnameV2;

  @JsonProperty("18")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag mediaUploadRichQuickReplies;

  @JsonProperty("15")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag mediaUpload;

  @JsonProperty("14")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag labelsEdit;

  @JsonProperty("13")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag liveLocationsFinal;

  @JsonProperty("12")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag stickerPackQuery;

  @JsonProperty("11")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag payments;

  @JsonProperty("10")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag quickRepliesQuery;

  @JsonProperty("9")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipIndividualIncoming;

  @JsonProperty("8")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag queryVname;

  @JsonProperty("7")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag liveLocations;

  @JsonProperty("6")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag queryStatusV3Thumbnail;

  @JsonProperty("5")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag changeNumberV2;

  @JsonProperty("4")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupsV3Create;

  @JsonProperty("3")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag groupsV3;

  @JsonProperty("2")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag voipIndividualOutgoing;

  @JsonProperty("1")
  @JsonPropertyDescription("WebFeaturesFlag")
  private WebFeaturesFlag labelsDisplay;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum WebFeaturesFlag {
    NOT_STARTED(0),
    FORCE_UPGRADE(1),
    DEVELOPMENT(2),
    PRODUCTION(3);

    @Getter
    private final int index;

    @JsonCreator
    public static WebFeaturesFlag forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
