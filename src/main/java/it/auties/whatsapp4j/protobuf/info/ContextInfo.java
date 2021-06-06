package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.message.MessageContainer;
import it.auties.whatsapp4j.protobuf.message.MessageKey;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ContextInfo {
  @JsonProperty(value = "26")
  private long ephemeralSettingTimestamp;

  @JsonProperty(value = "25")
  private int expiration;

  @JsonProperty(value = "24")
  private MessageKey placeholderKey;

  @JsonProperty(value = "23")
  private AdReplyInfo quotedAd;

  @JsonProperty(value = "22")
  private boolean isForwarded;

  @JsonProperty(value = "21")
  private int forwardingScore;

  @JsonProperty(value = "20")
  private int conversionDelaySeconds;

  @JsonProperty(value = "19")
  private byte[] conversionData;

  @JsonProperty(value = "18")
  private String conversionSource;

  @JsonProperty(value = "15")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> mentionedJid;

  @JsonProperty(value = "4")
  private String remoteJid;

  @JsonProperty(value = "3")
  private MessageContainer quotedMessage;

  @JsonProperty(value = "2")
  private String participant;

  @JsonProperty(value = "1")
  private String stanzaId;
}
