package it.auties.whatsapp.protobuf.message.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

/**
 * A model class that describes an interactive annotation linked to a message
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class InteractiveAnnotation {
  /**
   * Polygon vertices
   */
  @JsonProperty("1")
  @JsonPropertyDescription("Point")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Point> polygonVertices;

  /**
   * Location
   */
  @JsonProperty("2")
  @JsonPropertyDescription("Location")
  private Location location;

  /**
   * Returns the type of action
   *
   * @return a non-null Action
   */
  public Action type() {
    return location != null ? Action.LOCATION : Action.UNKNOWN;
  }

  /**
   * The constants of this enumrated type describe the various types of action that an interactive annotation can provide
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum Action {
    /**
     * Unknown
     */
    UNKNOWN(0),

    /**
     * Location
     */
    LOCATION(2);

    @Getter
    private final int index;

    @JsonCreator
    public static Action forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(Action.UNKNOWN);
    }
  }

  /**
   * This model class describes a Point in space
   */
  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Builder
  @Accessors(fluent = true)
  public static class Point {
    /**
     * X coordinate, deprecated
     */
    @JsonProperty("1")
    @JsonPropertyDescription("int32")
    @Deprecated
    private int xDeprecated;

    /**
     * Y coordinate, deprecated
     */
    @JsonProperty("2")
    @JsonPropertyDescription("int32")
    @Deprecated
    private int yDeprecated;

    /**
     * X coordinate
     */
    @JsonProperty("3")
    @JsonPropertyDescription("double")
    private double x;

    /**
     * Y coordinate
     */
    @JsonProperty("4")
    @JsonPropertyDescription("double")
    private double y;
  }

  /**
   * This model class describes a Location
   */
  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Builder
  @Accessors(fluent = true)
  public static class Location {
    /**
     * The latitude of this location, in degrees
     */
    @JsonProperty("1")
    @JsonPropertyDescription("double")
    private double latitude;

    /**
     * The longitude of this location, in degrees
     */
    @JsonProperty("2")
    @JsonPropertyDescription("double")
    private double longitude;

    /**
     * The name of this location
     */
    @JsonProperty("3")
    @JsonPropertyDescription("string")
    private String name;
  }
}
