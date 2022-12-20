package it.auties.whatsapp.exception;

import lombok.experimental.StandardException;

/**
 * An unchecked exception that is thrown when {@link it.auties.whatsapp.crypto.AesGmc} fails to decode or encode data
 */
@StandardException
public class AesException
        extends RuntimeException {

}
