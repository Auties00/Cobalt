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
public class LiveLocationMessage {
  @JsonProperty(value = "17")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("uint32")
  private int timeOffset;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("int64")
  private long sequenceNumber;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("uint32")
  private int degreesClockwiseFromMagneticNorth;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("float")
  private float speedInMps;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("uint32")
  private int accuracyInMeters;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("double")
  private double degreesLongitude;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("double")
  private double degreesLatitude;
}
