package it.auties.whatsapp.protobuf.sync;

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
  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  private byte[] oldPhoto;

  @JsonProperty("2")
  @JsonPropertyDescription("bytes")
  private byte[] newPhoto;

  @JsonProperty("3")
  @JsonPropertyDescription("uint32")
  private int newPhotoId;
}
