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
public class ContextInfo {

  @JsonProperty(value = "31", required = false)
  @JsonPropertyDescription("uint32")
  private int entryPointConversionDelaySeconds;

  @JsonProperty(value = "30", required = false)
  @JsonPropertyDescription("string")
  private String entryPointConversionApp;

  @JsonProperty(value = "29", required = false)
  @JsonPropertyDescription("string")
  private String entryPointConversionSource;

  @JsonProperty(value = "28", required = false)
  @JsonPropertyDescription("ExternalAdReplyInfo")
  private ExternalAdReplyInfo externalAdReply;

  @JsonProperty(value = "27", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] ephemeralSharedSecret;

  @JsonProperty(value = "26", required = false)
  @JsonPropertyDescription("int64")
  private long ephemeralSettingTimestamp;

  @JsonProperty(value = "25", required = false)
  @JsonPropertyDescription("uint32")
  private int expiration;

  @JsonProperty(value = "24", required = false)
  @JsonPropertyDescription("MessageKey")
  private MessageKey placeholderKey;

  @JsonProperty(value = "23", required = false)
  @JsonPropertyDescription("AdReplyInfo")
  private AdReplyInfo quotedAd;

  @JsonProperty(value = "22", required = false)
  @JsonPropertyDescription("bool")
  private boolean isForwarded;

  @JsonProperty(value = "21", required = false)
  @JsonPropertyDescription("uint32")
  private int forwardingScore;

  @JsonProperty(value = "20", required = false)
  @JsonPropertyDescription("uint32")
  private int conversionDelaySeconds;

  @JsonProperty(value = "19", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] conversionData;

  @JsonProperty(value = "18", required = false)
  @JsonPropertyDescription("string")
  private String conversionSource;

  @JsonProperty(value = "15", required = false)
  @JsonPropertyDescription("string")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> mentionedJid;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String remoteJid;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("Message")
  private Message quotedMessage;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String participant;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String stanzaId;
}
