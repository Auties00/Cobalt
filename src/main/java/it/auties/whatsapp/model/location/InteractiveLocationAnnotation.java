package it.auties.whatsapp.model.location;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that describes an interactive annotation linked to a message
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("InteractiveAnnotation")
public class InteractiveLocationAnnotation
    implements ProtobufMessage {

  /**
   * Polygon vertices
   */
  @ProtobufProperty(index = 1, type = MESSAGE, implementation = Point.class, repeated = true)
  private List<Point> polygonVertices;

  /**
   * Location
   */
  @ProtobufProperty(index = 2, type = MESSAGE, implementation = Location.class)
  private Location location;

  /**
   * Returns the type of sync
   *
   * @return a non-null Action
   */
  public Action type() {
    return location != null ?
        Action.LOCATION :
        Action.UNKNOWN;
  }

  /**
   * The constants of this enumrated type describe the various types of sync that an interactive
   * annotation can provide
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  @ProtobufName("ActionType")
  public enum Action
      implements ProtobufMessage {

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
    public static Action of(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(Action.UNKNOWN);
    }
  }

  public static class InteractiveLocationAnnotationBuilder {

    public InteractiveLocationAnnotationBuilder polygonVertices(List<Point> polygonVertices) {
      if (this.polygonVertices == null) {
        this.polygonVertices = new ArrayList<>();
      }
      this.polygonVertices.addAll(polygonVertices);
      return this;
    }
  }
}