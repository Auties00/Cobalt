package it.auties.whatsapp.exception;

import lombok.experimental.StandardException;

/**
 * This exception is thrown when a phone number cannot be registered by the Whatsapp API
 */
@StandardException
public class RegistrationException extends RuntimeException{
}
