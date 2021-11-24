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
public class GroupInviteMessage {

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String groupName;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("int64")
  private long inviteExpiration;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String inviteCode;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String groupJid;
}
