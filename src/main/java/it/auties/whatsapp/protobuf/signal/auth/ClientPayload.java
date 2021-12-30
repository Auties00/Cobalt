package it.auties.whatsapp.protobuf.signal.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.util.BytesDeserializer;
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
  @JsonProperty("1")
  @JsonPropertyDescription("uint64")
  private long username;

  @JsonProperty("3")
  @JsonPropertyDescription("bool")
  private boolean passive;

  @JsonProperty("4")
  @JsonPropertyDescription("ClientPayloadClientFeature")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ClientPayloadClientFeature> clientFeatures;

  @JsonProperty("5")
  @JsonPropertyDescription("UserAgent")
  private UserAgent userAgent;

  @JsonProperty("6")
  @JsonPropertyDescription("WebInfo")
  private WebInfo webInfo;

  @JsonProperty("7")
  @JsonPropertyDescription("string")
  private String pushName;

  @JsonProperty("9")
  @JsonPropertyDescription("sfixed32")
  private int sessionId;

  @JsonProperty("10")
  @JsonPropertyDescription("bool")
  private boolean shortConnect;

  @JsonProperty("12")
  @JsonPropertyDescription("ClientPayloadConnectType")
  private ClientPayloadConnectType connectType;

  @JsonProperty("13")
  @JsonPropertyDescription("ClientPayloadConnectReason")
  private ClientPayloadConnectReason connectReason;

  @JsonProperty("14")
  @JsonPropertyDescription("int32")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> shards;

  @JsonProperty("15")
  @JsonPropertyDescription("DNSSource")
  private DNSSource dnsSource;

  @JsonProperty("16")
  @JsonPropertyDescription("uint32")
  private int connectAttemptCount;

  @JsonProperty("17")
  @JsonPropertyDescription("uint32")
  private int agent;

  @JsonProperty("18")
  @JsonPropertyDescription("uint32")
  private int device;

  @JsonProperty("19")
  @JsonPropertyDescription("CompanionRegData")
  private CompanionData regData;

  @JsonProperty("20")
  @JsonPropertyDescription("ClientPayloadProduct")
  private ClientPayloadProduct product;

  @JsonProperty("21")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] fbCat;

  @JsonProperty("22")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] fbUserAgent;

  @JsonProperty("23")
  @JsonPropertyDescription("bool")
  private boolean oc;

  @JsonProperty("30")
  @JsonPropertyDescription("ClientPayloadIOSAppExtension")
  private ClientPayloadIOSAppExtension iosAppExtension;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum ClientPayloadClientFeature {
    NONE(0);

    @Getter
    private final int index;

    @JsonCreator
    public static ClientPayloadClientFeature forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum ClientPayloadIOSAppExtension {
    SHARE_EXTENSION(0),
    SERVICE_EXTENSION(1),
    INTENTS_EXTENSION(2);

    @Getter
    private final int index;

    @JsonCreator
    public static ClientPayloadIOSAppExtension forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @AllArgsConstructor
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

    @Getter
    private final int index;

    @JsonCreator
    public static ClientPayloadConnectType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum ClientPayloadConnectReason {
    PUSH(0),
    USER_ACTIVATED(1),
    SCHEDULED(2),
    ERROR_RECONNECT(3),
    NETWORK_SWITCH(4),
    PING_RECONNECT(5);

    @Getter
    private final int index;

    @JsonCreator
    public static ClientPayloadConnectReason forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum ClientPayloadProduct {
    WHATSAPP(0),
    MESSENGER(1);

    @Getter
    private final int index;

    @JsonCreator
    public static ClientPayloadProduct forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
