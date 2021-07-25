package it.auties.whatsapp4j.protobuf.model.misc;

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
  @JsonProperty(value = "3")
  private int newPhotoId;

  @JsonProperty(value = "2")
  private byte[] newPhoto;

  @JsonProperty(value = "1")
  private byte[] oldPhoto;
}
