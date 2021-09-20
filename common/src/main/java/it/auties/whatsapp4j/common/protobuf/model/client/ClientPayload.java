package it.auties.whatsapp4j.common.protobuf.model.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.common.protobuf.info.WebInfo;
import it.auties.whatsapp4j.common.protobuf.model.companion.CompanionRegData;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ClientPayload {
  @JsonProperty(value = "23")
  private boolean oc;

  @JsonProperty(value = "22")
  private byte[] fbUserAgent;

  @JsonProperty(value = "21")
  private byte[] fbCat;

  @JsonProperty(value = "20")
  private ClientPayloadProduct product;

  @JsonProperty(value = "19")
  private CompanionRegData regData;

  @JsonProperty(value = "18")
  private int device;

  @JsonProperty(value = "17")
  private int agent;

  @JsonProperty(value = "16")
  private int connectAttemptCount;

  @JsonProperty(value = "15")
  private DNSSource dnsSource;

  @JsonProperty(value = "14")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> shards;

  @JsonProperty(value = "13")
  private ClientPayloadConnectReason connectReason;

  @JsonProperty(value = "12")
  private ClientPayloadConnectType connectType;

  @JsonProperty(value = "30")
  private ClientPayloadIOSAppExtension iosAppExtension;

  @JsonProperty(value = "10")
  private boolean shortConnect;

  @JsonProperty(value = "9")
  private int sessionId;

  @JsonProperty(value = "7")
  private String pushName;

  @JsonProperty(value = "6")
  private WebInfo webInfo;

  @JsonProperty(value = "5")
  private UserAgent userAgent;

  @JsonProperty(value = "4")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ClientPayloadClientFeature> clientFeatures;

  @JsonProperty(value = "3")
  private boolean passive;

  @JsonProperty(value = "1")
  private long username;

  @Accessors(fluent = true)
  public enum ClientPayloadClientFeature {
    NONE(0);

    private final @Getter int index;

    ClientPayloadClientFeature(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ClientPayloadClientFeature forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum ClientPayloadIOSAppExtension {
    SHARE_EXTENSION(0),
    SERVICE_EXTENSION(1),
    INTENTS_EXTENSION(2);

    private final @Getter int index;

    ClientPayloadIOSAppExtension(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ClientPayloadIOSAppExtension forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum ClientPayloadConnectType {
    CELLULAR_UNKNOWN(0),
    WIFI_UNKNOWN(1),
    CELLULAR_EDGE(100),
    CELLULAR_IDEN(101),
    CELLULAR_UMTS(102),
    CELLULAR_EVDO(103),
    CELLULAR_GPRS(104),
    CELLULAR_HSDPA(105),
    CELLULAR_HSUPA(106),
    CELLULAR_HSPA(107),
    CELLULAR_CDMA(108),
    CELLULAR_1XRTT(109),
    CELLULAR_EHRPD(110),
    CELLULAR_LTE(111),
    CELLULAR_HSPAP(112);

    private final @Getter int index;

    ClientPayloadConnectType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ClientPayloadConnectType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum ClientPayloadConnectReason {
    PUSH(0),
    USER_ACTIVATED(1),
    SCHEDULED(2),
    ERROR_RECONNECT(3),
    NETWORK_SWITCH(4),
    PING_RECONNECT(5);

    private final @Getter int index;

    ClientPayloadConnectReason(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ClientPayloadConnectReason forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum ClientPayloadProduct {
    WHATSAPP(0),
    MESSENGER(1);

    private final @Getter int index;

    ClientPayloadProduct(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ClientPayloadProduct forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
