package it.auties.whatsapp.model.signal.auth;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class UserAgent implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = MESSAGE, implementation = UserAgent.UserAgentPlatform.class)
  private UserAgentPlatform platform;

  @ProtobufProperty(index = 2, type = MESSAGE, implementation = Version.class)
  private Version appVersion;

  @ProtobufProperty(index = 3, type = STRING)
  private String mcc;

  @ProtobufProperty(index = 4, type = STRING)
  private String mnc;

  @ProtobufProperty(index = 5, type = STRING)
  private String osVersion;

  @ProtobufProperty(index = 6, type = STRING)
  private String manufacturer;

  @ProtobufProperty(index = 7, type = STRING)
  private String device;

  @ProtobufProperty(index = 8, type = STRING)
  private String osBuildNumber;

  @ProtobufProperty(index = 9, type = STRING)
  private String phoneId;

  @ProtobufProperty(index = 10, type = MESSAGE, implementation = UserAgent.UserAgentReleaseChannel.class)
  private UserAgentReleaseChannel releaseChannel;

  @ProtobufProperty(index = 11, type = STRING)
  private String localeLanguageIso6391;

  @ProtobufProperty(index = 12, type = STRING)
  private String localeCountryIso31661Alpha2;

  @ProtobufProperty(index = 13, type = STRING)
  private String deviceBoard;

  @AllArgsConstructor
  @Accessors(fluent = true)
  @ProtobufName("Platform")
  public enum UserAgentPlatform implements ProtobufMessage {

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
    VR(25),
    OCULUS_CALL(26),
    MILAN(27),
    CAPI(28),
    WEAROS(29),
    ARDEVICE(30),
    VRDEVICE(31);
    @Getter
    private final int index;

    @JsonCreator
    public static UserAgentPlatform of(int index) {
      return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst()
          .orElse(null);
    }
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  @ProtobufName("ReleaseChannel")
  public enum UserAgentReleaseChannel implements ProtobufMessage {

    RELEASE(0),
    BETA(1),
    ALPHA(2),
    DEBUG(3);
    @Getter
    private final int index;

    public static UserAgentReleaseChannel of(int index) {
      return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst()
          .orElse(null);
    }
  }
}