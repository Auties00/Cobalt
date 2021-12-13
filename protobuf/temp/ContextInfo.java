package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ContextInfo {
  @JsonProperty(value = "31")
  @JsonPropertyDescription("uint32")
  private int entryPointConversionDelaySeconds;

  @JsonProperty(value = "30")
  @JsonPropertyDescription("string")
  private String entryPointConversionApp;

  @JsonProperty(value = "29")
  @JsonPropertyDescription("string")
  private String entryPointConversionSource;

  @JsonProperty(value = "28")
  @JsonPropertyDescription("ExternalAdReplyInfo")
  private ExternalAdReplyInfo externalAdReply;

  @JsonProperty(value = "27")
  @JsonPropertyDescription("bytes")
  private byte[] ephemeralSharedSecret;

  @JsonProperty(value = "26")
  @JsonPropertyDescription("int64")
  private long ephemeralSettingTimestamp;

  @JsonProperty(value = "25")
  @JsonPropertyDescription("uint32")
  private int expiration;

  @JsonProperty(value = "24")
  @JsonPropertyDescription("MessageKey")
  private MessageKey placeholderKey;

  @JsonProperty(value = "23")
  @JsonPropertyDescription("AdReplyInfo")
  private AdReplyInfo quotedAd;

  @JsonProperty(value = "22")
  @JsonPropertyDescription("bool")
  private boolean isForwarded;

  @JsonProperty(value = "21")
  @JsonPropertyDescription("uint32")
  private int forwardingScore;

  @JsonProperty(value = "20")
  @JsonPropertyDescription("uint32")
  private int conversionDelaySeconds;

  @JsonProperty(value = "19")
  @JsonPropertyDescription("bytes")
  private byte[] conversionData;

  @JsonProperty(value = "18")
  @JsonPropertyDescription("string")
  private String conversionSource;

  @JsonProperty(value = "15")
  @JsonPropertyDescription("string")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> mentionedJid;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("string")
  private String remoteJid;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("Message")
  private Message quotedMessage;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String participant;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String stanzaId;
}
