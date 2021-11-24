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
public class LiveLocationMessage {

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("uint32")
  private int timeOffset;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("int64")
  private long sequenceNumber;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("uint32")
  private int degreesClockwiseFromMagneticNorth;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("float")
  private float speedInMps;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint32")
  private int accuracyInMeters;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("double")
  private double degreesLongitude;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("double")
  private double degreesLatitude;
}
