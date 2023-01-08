package it.auties.whatsapp.model.signal.auth;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.sync.HistorySyncConfig;
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
@ProtobufName("DeviceProps")
public class Companion
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, type = STRING)
  private String os;

  @ProtobufProperty(index = 2, type = MESSAGE, implementation = Version.class)
  private Version version;

  @ProtobufProperty(index = 3, type = MESSAGE, implementation = Companion.CompanionPropsPlatformType.class)
  private CompanionPropsPlatformType platformType;

  @ProtobufProperty(index = 4, type = BOOL)
  private boolean requireFullSync;

  @ProtobufProperty(index = 5, name = "historySyncConfig", type = ProtobufType.MESSAGE)
  private HistorySyncConfig historySyncConfig;

  @AllArgsConstructor
  @Accessors(fluent = true)
  @ProtobufName("PlatformType")
  public enum CompanionPropsPlatformType {

    UNKNOWN(0),
    CHROME(1),
    FIREFOX(2),
    INTERNET_EXPLORER(3),
    OPERA(4),
    SAFARI(5),
    EDGE(6),
    DESKTOP(7),
    IPAD(8),
    ANDROID_TABLET(9),
    PORTAL(10),
    PORTAL_PLUS(11),
    PORTAL_TV(12),
    TCL_TV(13);
    @Getter
    private final int index;

    @JsonCreator
    public static CompanionPropsPlatformType of(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}