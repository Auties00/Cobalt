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
public class ClientPayload {

  @JsonProperty(value = "23", required = false)
  @JsonPropertyDescription("bool")
  private boolean oc;

  @JsonProperty(value = "22", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fbUserAgent;

  @JsonProperty(value = "21", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fbCat;

  @JsonProperty(value = "20", required = false)
  @JsonPropertyDescription("ClientPayloadProduct")
  private ClientPayloadProduct product;

  @JsonProperty(value = "19", required = false)
  @JsonPropertyDescription("CompanionRegData")
  private CompanionRegData regData;

  @JsonProperty(value = "18", required = false)
  @JsonPropertyDescription("uint32")
  private int device;

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("uint32")
  private int agent;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("uint32")
  private int connectAttemptCount;

  @JsonProperty(value = "15", required = false)
  @JsonPropertyDescription("DNSSource")
  private DNSSource dnsSource;

  @JsonProperty(value = "14", required = false)
  @JsonPropertyDescription("int32")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> shards;

  @JsonProperty(value = "13", required = false)
  @JsonPropertyDescription("ClientPayloadConnectReason")
  private ClientPayloadConnectReason connectReason;

  @JsonProperty(value = "12", required = false)
  @JsonPropertyDescription("ClientPayloadConnectType")
  private ClientPayloadConnectType connectType;

  @JsonProperty(value = "30", required = false)
  @JsonPropertyDescription("ClientPayloadIOSAppExtension")
  private ClientPayloadIOSAppExtension iosAppExtension;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("bool")
  private boolean shortConnect;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("sfixed32")
  private int sessionId;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("string")
  private String pushName;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("WebInfo")
  private WebInfo webInfo;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("UserAgent")
  private UserAgent userAgent;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("ClientPayloadClientFeature")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ClientPayloadClientFeature> clientFeatures;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("bool")
  private boolean passive;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("uint64")
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
