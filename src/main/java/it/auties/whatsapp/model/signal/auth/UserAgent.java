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
public class UserAgent {
  @JsonProperty("1")
  @JsonPropertyDescription("UserAgentPlatform")
  private UserAgentPlatform platform;

  @JsonProperty("2")
  @JsonPropertyDescription("AppVersion")
  private Version appVersion;

  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String mcc;

  @JsonProperty("4")
  @JsonPropertyDescription("string")
  private String mnc;

  @JsonProperty("5")
  @JsonPropertyDescription("string")
  private String osVersion;

  @JsonProperty("6")
  @JsonPropertyDescription("string")
  private String manufacturer;

  @JsonProperty("7")
  @JsonPropertyDescription("string")
  private String device;

  @JsonProperty("8")
  @JsonPropertyDescription("string")
  private String osBuildNumber;

  @JsonProperty("9")
  @JsonPropertyDescription("string")
  private String phoneId;

  @JsonProperty("10")
  @JsonPropertyDescription("UserAgentReleaseChannel")
  private UserAgentReleaseChannel releaseChannel;

  @JsonProperty("11")
  @JsonPropertyDescription("string")
  private String localeLanguageIso6391;

  @JsonProperty("12")
  @JsonPropertyDescription("string")
  private String localeCountryIso31661Alpha2;

  @JsonProperty("13")
  @JsonPropertyDescription("string")
  private String deviceBoard;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum UserAgentPlatform {
    ANDROID(0),
    IOS(1),
    WINDOWS_PHONE(2),
    BLACKBERRY(3),
    BLACK_BERRY_X(4),
    S40(5),
    S60(6),
    PYTHON_CLIENT(7),
    TIZEN(8),
    ENTERPRISE(9),
    SMB_ANDROID(10),
    KAIOS(11),
    SMB_IOS(12),
    WINDOWS(13),
    WEB(14),
    PORTAL(15),
    GREEN_ANDROID(16),
    GREEN_IPHONE(17),
    BLUE_ANDROID(18),
    BLUE_IPHONE(19),
    FB_LITE_ANDROID(20),
    M_LITE_ANDROID(21),
    IG_LITE_ANDROID(22),
    PAGE(23),
    MACOS(24),
    VR(25);

    @Getter
    private final int index;

    @JsonCreator
    public static UserAgentPlatform forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum UserAgentReleaseChannel {
    RELEASE(0),
    BETA(1),
    ALPHA(2),
    DEBUG(3);

    @Getter
    private final int index;

    @JsonCreator
    public static UserAgentReleaseChannel forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
