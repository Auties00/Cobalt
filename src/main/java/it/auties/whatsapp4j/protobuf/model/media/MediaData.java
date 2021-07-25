package it.auties.whatsapp4j.protobuf.model.media;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class MediaData {
  @JsonProperty(value = "1")
  private String localPath;
}
