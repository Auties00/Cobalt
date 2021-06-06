package it.auties.whatsapp4j.protobuf.model;

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
  @JsonProperty(value = "34")
  private WebFeaturesFlag recentStickersV2;

  @JsonProperty(value = "33")
  private WebFeaturesFlag e2ENotificationSync;

  @JsonProperty(value = "32")
  private WebFeaturesFlag ephemeralMessages;

  @JsonProperty(value = "31")
  private WebFeaturesFlag templateMessageInteractivity;

  @JsonProperty(value = "30")
  private WebFeaturesFlag templateMessage;

  @JsonProperty(value = "29")
  private WebFeaturesFlag voipGroupCall;

  @JsonProperty(value = "28")
  private WebFeaturesFlag starredStickers;

  @JsonProperty(value = "27")
  private WebFeaturesFlag catalog;

  @JsonProperty(value = "26")
  private WebFeaturesFlag recentStickers;

  @JsonProperty(value = "25")
  private WebFeaturesFlag groupsV4JoinPermission;

  @JsonProperty(value = "24")
  private WebFeaturesFlag frequentlyForwardedSetting;

  @JsonProperty(value = "23")
  private WebFeaturesFlag thirdPartyStickers;

  @JsonProperty(value = "22")
  private WebFeaturesFlag voipIndividualVideo;

  @JsonProperty(value = "21")
  private WebFeaturesFlag statusRanking;

  @JsonProperty(value = "20")
  private WebFeaturesFlag videoPlaybackUrl;

  @JsonProperty(value = "19")
  private WebFeaturesFlag vnameV2;

  @JsonProperty(value = "18")
  private WebFeaturesFlag mediaUploadRichQuickReplies;

  @JsonProperty(value = "15")
  private WebFeaturesFlag mediaUpload;

  @JsonProperty(value = "14")
  private WebFeaturesFlag labelsEdit;

  @JsonProperty(value = "13")
  private WebFeaturesFlag liveLocationsFinal;

  @JsonProperty(value = "12")
  private WebFeaturesFlag stickerPackQuery;

  @JsonProperty(value = "11")
  private WebFeaturesFlag payments;

  @JsonProperty(value = "10")
  private WebFeaturesFlag quickRepliesQuery;

  @JsonProperty(value = "9")
  private WebFeaturesFlag voipIndividualIncoming;

  @JsonProperty(value = "8")
  private WebFeaturesFlag queryVname;

  @JsonProperty(value = "7")
  private WebFeaturesFlag liveLocations;

  @JsonProperty(value = "6")
  private WebFeaturesFlag queryStatusV3Thumbnail;

  @JsonProperty(value = "5")
  private WebFeaturesFlag changeNumberV2;

  @JsonProperty(value = "4")
  private WebFeaturesFlag groupsV3Create;

  @JsonProperty(value = "3")
  private WebFeaturesFlag groupsV3;

  @JsonProperty(value = "2")
  private WebFeaturesFlag voipIndividualOutgoing;

  @JsonProperty(value = "1")
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
