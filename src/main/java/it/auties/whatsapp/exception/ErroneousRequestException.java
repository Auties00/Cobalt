package it.auties.whatsapp.exception;

import lombok.experimental.StandardException;

/**
 * An unchecked exception that is thrown when an erroneous body, either a binary or a node, is
 * received by Whatsapp
 */
@StandardException
public abstract sealed class ErroneousRequestException
    extends RuntimeException
    permits ErroneousBinaryRequestException, ErroneousNodeRequestException {

  /**
   * Returns the erroneous body that was sent to Whatsapp
   *
   * @return a nullable erroneous body
   */
  public abstract Object error();
}
