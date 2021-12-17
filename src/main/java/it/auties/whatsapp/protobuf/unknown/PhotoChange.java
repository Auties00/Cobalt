package it.auties.whatsapp.protobuf.unknown;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class PhotoChange {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("uint32")
  private int newPhotoId;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] newPhoto;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("bytes")
  private byte[] oldPhoto;
}
