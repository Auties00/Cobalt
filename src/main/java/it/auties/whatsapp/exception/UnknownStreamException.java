package it.auties.whatsapp.exception;

import lombok.NonNull;

/**
 * An unchecked exception that is thrown when an unknown error occurs in the WebSocket stream
 */
public class UnknownStreamException
    extends RuntimeException {

  public UnknownStreamException(@NonNull String reason) {
    super(reason);
  }
}
