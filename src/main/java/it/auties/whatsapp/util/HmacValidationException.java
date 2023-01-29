package it.auties.whatsapp.util;

import lombok.NonNull;

/**
 * An unchecked exception that is thrown when a hmac signature cannot be validated
 */
public class HmacValidationException
    extends SecurityException {

  public HmacValidationException(@NonNull String location) {
    super(location);
  }
}
