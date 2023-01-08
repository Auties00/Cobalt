package it.auties.whatsapp.controller;

import it.auties.whatsapp.util.JacksonProvider;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * This interface represents is implemented by all WhatsappWeb4J's controllers. It provides an easy
 * way to store IDs and serialize said class.
 */
public sealed interface Controller<T extends Controller<T>>
    extends JacksonProvider
    permits Store, Keys {

  /**
   * Returns the id of this controller
   *
   * @return an id
   */
  int id();

  /**
   * Clears some or all fields of this object
   */
  void clear();

  /**
   * Disposes this object
   */
  void dispose();

  /**
   * Whether the default serializer should be used
   *
   * @return a boolean
   */
  boolean useDefaultSerializer();

  /**
   * Serializes this object
   *
   * @param async whether the operation should be executed asynchronously
   */
  void serialize(boolean async);

  /**
   * Set whether the default serializer should be used
   *
   * @return the same instance
   */
  T useDefaultSerializer(boolean useDefaultSerializer);

  /**
   * Converts this controller to a json. Useful when debugging.
   *
   * @return a non-null string
   */
  @SuppressWarnings("unused")
  default String toJson() {
    try {
      return JSON.writerWithDefaultPrettyPrinter()
          .writeValueAsString(this);
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot convert controller to json", exception);
    }
  }
}
