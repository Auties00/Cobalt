package it.auties.whatsapp.api;

import it.auties.whatsapp.api.ErrorHandler.Location;
import it.auties.whatsapp.util.Exceptions;
import it.auties.whatsapp.util.HmacValidationException;

import java.lang.System.Logger.Level;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

/**
 * This interface allows to handle a socket error and provides a default way to do so
 */
@SuppressWarnings("unused")
public interface ErrorHandler extends BiFunction<Location, Throwable, ErrorHandler.Result> {
    /**
     * Default error handler. Prints the exception on the terminal.
     *
     * @return a non-null error handler
     */
    static ErrorHandler toTerminal() {
        return toTerminal(null, null);
    }

    /**
     * Default error handler. Prints the exception on the terminal.
     *
     * @param onRestore action to execute if the session is restored, can be null
     * @param onIgnored action to execute if the session is not restored, can be null
     * @return a non-null error handler
     */
    static ErrorHandler toTerminal(BiConsumer<Location, Throwable> onRestore, BiConsumer<Location, Throwable> onIgnored) {
        return defaultErrorHandler(Throwable::printStackTrace, onRestore, onIgnored, ERROR);
    }

    /**
     * Default error handler
     *
     * @param exceptionPrinter a consumer that handles the printing of the throwable, can be null
     * @param onRestore        action to execute if the session is restored, can be null
     * @param onIgnored        action to execute if the session is not restored, can be null
     * @param loggingLevel     the level used to log messages about the error, can be null if no
     *                         logging should be done
     * @return a non-null error handler
     */
    static ErrorHandler defaultErrorHandler(Consumer<Throwable> exceptionPrinter, BiConsumer<Location, Throwable> onRestore, BiConsumer<Location, Throwable> onIgnored, Level loggingLevel) {
        return (location, throwable) -> {
            var logger = System.getLogger("ErrorHandler");
            if (location == SOCKET) {
                return Result.RECONNECT;
            }
            if (loggingLevel != null) {
                logger.log(loggingLevel, "Socket failure at %s".formatted(location));
            }
            if (throwable != null && exceptionPrinter != null) {
                exceptionPrinter.accept(throwable);
            }
            if (location != INITIAL_APP_STATE_SYNC && location != CRYPTOGRAPHY && !(location == MESSAGE && throwable instanceof HmacValidationException)) {
                if (loggingLevel != null) {
                    logger.log(loggingLevel, "Ignored failure");
                }
                if (onIgnored == null) {
                    return Result.DISCARD;
                }
                onIgnored.accept(location, throwable);
                return Result.DISCARD;
            }
            if (loggingLevel != null) {
                logger.log(loggingLevel, "Restoring session");
            }
            if (onRestore == null) {
                return Result.RESTORE;
            }
            onRestore.accept(location, throwable);
            return Result.RESTORE;
        };
    }

    /**
     * Default error handler. Saves the exception locally.
     *
     * @return a non-null error handler
     */
    static ErrorHandler toFile() {
        return toFile(null, null);
    }

    /**
     * Default error handler. Saves the exception locally.
     *
     * @param onRestore action to execute if the session is restored, can be null
     * @param onIgnored action to execute if the session is not restored, can be null
     * @return a non-null error handler
     */
    static ErrorHandler toFile(BiConsumer<Location, Throwable> onRestore, BiConsumer<Location, Throwable> onIgnored) {
        var logger = System.getLogger("ErrorHandler");
        return defaultErrorHandler(throwable -> logger.log(INFO, "Saved stacktrace at: %s".formatted(Exceptions.save(throwable))), onRestore, onIgnored, ERROR);
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
    static ErrorHandler defaultErrorHandler(BiConsumer<Location, Throwable> onRestore, BiConsumer<Location, Throwable> onIgnored) {
        return defaultErrorHandler(null, onRestore, onIgnored, ERROR);
    }

    /**
     * The constants of this enumerated type describe the various locations where an error can occur
     * in the socket
     */
    enum Location {
        /**
         * Unknown
         */
        UNKNOWN,
        /**
         * Cryptographic error
         */
        CRYPTOGRAPHY,
        /**
         * Called when a malformed node is sent
         */
        ERRONEOUS_NODE,
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
         * Called when an error is thrown while pulling app data
         */
        PULL_APP_STATE,
        /**
         * Called when an error is thrown while pushing app data
         */
        PUSH_APP_STATE,
        /**
         * Called when an error is thrown while pulling initial app data
         */
        INITIAL_APP_STATE_SYNC,
        /**
         * Called when an error occurs when serializing or deserializing a Whatsapp message
         */
        MESSAGE,
        /**
         * Called when an error occurs when deciphering a poll message
         */
        POLL,
        /**
         * Called when sending or receiving a socket message
         */
        SOCKET
    }

    /**
     * The constants of this enumerated type describe the various types of actions that can be
     * performed by an error handler in response to a throwable
     */
    enum Result {
        /**
         * Ignores an error that was thrown by the socket
         */
        DISCARD,
        /**
         * Deletes the current session and creates a new one instantly
         */
        RESTORE,
        /**
         * Disconnects from the current session without deleting it
         */
        DISCONNECT,
        /**
         * Disconnects from the current session without deleting it and reconnects to it
         */
        RECONNECT,
        /**
         * Deletes the current session
         */
        LOG_OUT
    }
}
