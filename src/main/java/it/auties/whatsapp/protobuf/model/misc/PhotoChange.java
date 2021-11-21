package it.auties.whatsapp.protobuf.model.misc;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  private int newPhotoId;

  @JsonProperty(value = "2")
  private byte[] newPhoto;

  @JsonProperty(value = "1")
  private byte[] oldPhoto;
}
