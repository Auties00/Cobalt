package it.auties.whatsapp4j.common.protobuf.model.app;

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
public class AppVersion {
  @JsonProperty(value = "5")
  private int quinary;

  @JsonProperty(value = "4")
  private int quaternary;

  @JsonProperty(value = "3")
  private int tertiary;

  @JsonProperty(value = "2")
  private int secondary;

  @JsonProperty(value = "1")
  private int primary;

  public AppVersion(int primary){
    this.primary = primary;
  }
}
