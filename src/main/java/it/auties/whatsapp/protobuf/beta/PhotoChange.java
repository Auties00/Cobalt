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
public class PhotoChange {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint32")
  private int newPhotoId;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] newPhoto;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] oldPhoto;
}
