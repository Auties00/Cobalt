package it.auties.whatsapp.protobuf.signal.auth;

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
public class Version {
  @JsonProperty("1")
  @JsonPropertyDescription("uint32")
  private int primary;

  @JsonProperty("2")
  @JsonPropertyDescription("uint32")
  private int secondary;

  @JsonProperty("3")
  @JsonPropertyDescription("uint32")
  private int tertiary;

  @JsonProperty("4")
  @JsonPropertyDescription("uint32")
  private int quaternary;

  @JsonProperty("5")
  @JsonPropertyDescription("uint32")
  private int quinary;

  public Version(int primary){
    this.primary = primary;
  }

  public Version(int primary, int secondary, int tertiary) {
    this.primary = primary;
    this.secondary = secondary;
    this.tertiary = tertiary;
  }
}
