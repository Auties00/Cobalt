package it.auties.whatsapp.protobuf.temp;

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
public class LocationMessage {
  @JsonProperty(value = "17")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("string")
  private String comment;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("uint32")
  private int degreesClockwiseFromMagneticNorth;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("float")
  private float speedInMps;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("uint32")
  private int accuracyInMeters;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("bool")
  private boolean isLive;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("string")
  private String url;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("string")
  private String address;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("string")
  private String name;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("double")
  private double degreesLongitude;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("double")
  private double degreesLatitude;
}
