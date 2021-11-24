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
public class LocationMessage {

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("string")
  private String comment;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("uint32")
  private int degreesClockwiseFromMagneticNorth;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("float")
  private float speedInMps;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("uint32")
  private int accuracyInMeters;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("bool")
  private boolean isLive;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String url;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String address;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String name;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("double")
  private double degreesLongitude;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("double")
  private double degreesLatitude;
}
