package it.auties.whatsapp.exception;

import it.auties.whatsapp.model.request.Node;

/**
 * An unchecked exception that is thrown when an erroneous node is received by Whatsapp
 */
public final class ErroneousNodeRequestException
    extends ErroneousRequestException {

  private final Node error;

  public ErroneousNodeRequestException(String message, Node error) {
    super(message);
    this.error = error;
  }

  public ErroneousNodeRequestException(String message, Node error, Throwable cause) {
    super(message, cause);
    this.error = error;
  }

  /**
   * Returns the erroneous body that was sent to Whatsapp
   *
   * @return a nullable erroneous body
   */
  public Node error() {
    return error;
  }
}
