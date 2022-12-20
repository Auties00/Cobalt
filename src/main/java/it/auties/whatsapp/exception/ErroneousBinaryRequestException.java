package it.auties.whatsapp.exception;

/**
 * An unchecked exception that is thrown when an erroneous binary is received by Whatsapp
 */
public final class ErroneousBinaryRequestException
        extends ErroneousRequestException {
    private final Object error;

    @SuppressWarnings("unused")
    public ErroneousBinaryRequestException(String message, Object error) {
        super(message);
        this.error = error;
    }

    public ErroneousBinaryRequestException(String message, Object error, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    /**
     * Returns the erroneous body that was sent to Whatsapp
     *
     * @return a nullable erroneous body
     */
    public Object error() {
        return error;
    }
}
