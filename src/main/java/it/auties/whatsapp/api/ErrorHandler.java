package it.auties.whatsapp.api;

import it.auties.whatsapp.api.ErrorHandler.Location;
import it.auties.whatsapp.util.Exceptions;
import it.auties.whatsapp.util.HmacValidationException;

import java.lang.System.Logger.Level;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static it.auties.whatsapp.api.ErrorHandler.Location.DISCONNECTED;
import static it.auties.whatsapp.api.ErrorHandler.Location.MESSAGE;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

/**
 * This interface allows to handle a socket error and provides a default way to do so
 */
@SuppressWarnings("unused")
public interface ErrorHandler extends BiFunction<Location, Throwable, Boolean> {
    /**
     * System logger.
     * A nice feature from Java 9.
     */
    System.Logger LOGGER = System.getLogger(ErrorHandler.class.getName());

    /**
     * Default error handler.
     * Prints the exception on the terminal.
     *
     * @return a non-null error handler
     */
    static ErrorHandler toTerminal() {
        return toTerminal(null, null);
    }

    /**
     * Default error handler.
     * Prints the exception on the terminal.
     *
     * @param onRestore action to execute if the session is restored, can be null
     * @param onIgnored action to execute if the session is not restored, can be null
     * @return a non-null error handler
     */
    static ErrorHandler toTerminal(BiConsumer<Location, Throwable> onRestore,
                                   BiConsumer<Location, Throwable> onIgnored) {
        return defaultErrorHandler(Throwable::printStackTrace, onRestore, onIgnored, ERROR);
    }

    /**
     * Default error handler.
     * Saves the exception locally.
     *
     * @return a non-null error handler
     */
    static ErrorHandler toFile() {
        return toFile(null, null);
    }


    /**
     * Default error handler.
     * Saves the exception locally.
     *
     * @param onRestore action to execute if the session is restored, can be null
     * @param onIgnored action to execute if the session is not restored, can be null
     * @return a non-null error handler
     */
    static ErrorHandler toFile(BiConsumer<Location, Throwable> onRestore, BiConsumer<Location, Throwable> onIgnored) {
        return defaultErrorHandler(
                throwable -> LOGGER.log(INFO, "Saved stacktrace at: %s".formatted(Exceptions.save(throwable))),
                onRestore, onIgnored, ERROR);
    }

    /**
     * Default error handler
     *
     * @param exceptionPrinter a consumer that handles the printing of the throwable, can be null
     * @return a non-null error handler
     */
    static ErrorHandler defaultErrorHandler(Consumer<Throwable> exceptionPrinter) {
        return defaultErrorHandler(exceptionPrinter, null, null, ERROR);
    }

    /**
     * Default error handler
     *
     * @param onRestore action to execute if the session is restored, can be null
     * @param onIgnored action to execute if the session is not restored, can be null
     * @return a non-null error handler
     */
    static ErrorHandler defaultErrorHandler(BiConsumer<Location, Throwable> onRestore,
                                            BiConsumer<Location, Throwable> onIgnored) {
        return defaultErrorHandler(null, onRestore, onIgnored, ERROR);
    }

    /**
     * Default error handler
     *
     * @param exceptionPrinter a consumer that handles the printing of the throwable, can be null
     * @param onRestore        action to execute if the session is restored, can be null
     * @param onIgnored        action to execute if the session is not restored, can be null
     * @param loggingLevel     the level used to log messages about the error, can be null if no logging should be done
     * @return a non-null error handler
     */
    static ErrorHandler defaultErrorHandler(Consumer<Throwable> exceptionPrinter,
                                            BiConsumer<Location, Throwable> onRestore,
                                            BiConsumer<Location, Throwable> onIgnored, Level loggingLevel) {
        return (location, throwable) -> {
            if (loggingLevel != null) {
                LOGGER.log(loggingLevel, "Socket failure at %s".formatted(location));
            }

            if (exceptionPrinter != null) {
                exceptionPrinter.accept(throwable);
            }

            if (isIgnorable(location, throwable)) {
                if (loggingLevel != null) {
                    LOGGER.log(loggingLevel, "Ignored failure");
                }

                if (onIgnored == null) {
                    return false;
                }

                onIgnored.accept(location, throwable);
                return false;
            }

            if (loggingLevel != null) {
                LOGGER.log(loggingLevel, "Restoring session");
            }

            if (onRestore == null) {
                return true;
            }

            onRestore.accept(location, throwable);
            return true;
        };
    }

    private static boolean isIgnorable(Location location, Throwable throwable) {
        return location != DISCONNECTED && (location != MESSAGE || !(throwable instanceof HmacValidationException));
    }

    /**
     * The constants of this enumerated type describe the various locations where an error can occur in the socket
     */
    enum Location {
        /**
         * Unknown
         */
        UNKNOWN,

        /**
         * Called when a malformed node is sent
         */
        ERRONEOUS_NODE,

        /**
         * The client was manually disconnected from whatsapp
         */
        DISCONNECTED,

        /**
         * Called when the media connection cannot be renewed
         */
        MEDIA_CONNECTION,

        /**
         * Called when an error arrives from the stream
         */
        STREAM,

        /**
         * Called when an error is thrown while logging in
         */
        LOGIN,

        /**
         * Called when an error is thrown while pulling or pushing app data
         */
        APP_STATE_SYNC,

        /**
         * Called when an error occurs when serializing or deserializing a Whatsapp message
         */
        MESSAGE
    }
}
