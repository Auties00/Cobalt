package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.exception.WhatsAppException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.BiConsumer;

/**
 * A pluggable strategy for reacting to failures raised by a
 * {@link LinkedWhatsAppClient}.
 *
 * <p>Cobalt deliberately avoids hardcoding recovery behaviour: whenever the
 * socket, stream, Signal pipeline, media pipeline, or synchronisation
 * subsystem throws a subtype of {@link WhatsAppException}, the client
 * delegates to an error handler which decides whether to swallow the error,
 * tear the session down, force a reconnect, or treat the account as banned
 * or logged out. This is a core divergence from WhatsApp Web, which inlines
 * recovery logic at every throw site.
 *
 * <p>The interface is a functional interface so a single
 * {@link java.util.function.BiFunction}-style lambda can be provided as the
 * handler. Static factory methods
 * ({@link #toTerminal()}, {@link #toFile()}, {@link #toFile(Path)}) return
 * sensible default implementations that log the exception and return its
 * {@link WhatsAppException#toErrorResult()}.
 *
 * @see WhatsAppException
 * @see LinkedWhatsAppClient#handleFailure(WhatsAppException)
 */
@SuppressWarnings("unused")
@FunctionalInterface
public interface WhatsAppLinkedClientErrorHandler {
    /**
     * Processes an error raised by the WhatsApp client pipeline and returns
     * the recovery action that should follow.
     *
     * <p>Implementations are expected to be non-blocking: they are invoked
     * from the thread that caught the failure and their return value drives
     * immediate session-control decisions (disconnect, reconnect, ban, log
     * out). If a handler needs to perform I/O (for example, writing the
     * stack trace to disk) it should dispatch that work to another thread
     * as the provided defaults do.
     *
     * @param whatsapp  the client where the error originated; its
     *                  {@link LinkedWhatsAppClient#store()} can be consulted for
     *                  context such as the session JID
     * @param exception the exception that was raised
     * @return the recovery action to apply
     */
    WhatsAppLinkedClientErrorResult handleError(LinkedWhatsAppClient whatsapp, WhatsAppException exception);

    /**
     * Returns an error handler that writes full stack traces to the
     * terminal's standard error stream.
     *
     * <p>This is the default handler used by the builder when no custom
     * handler is supplied. It is well suited to development and debugging
     * sessions where seeing the exception inline is helpful; production
     * deployments should prefer {@link #toFile()}.
     *
     * @return a handler that prints exceptions to {@code System.err}
     */
    @SuppressWarnings("CallToPrintStackTrace")
    static WhatsAppLinkedClientErrorHandler toTerminal() {
        return defaultErrorHandler((api, error) -> error.printStackTrace());
    }

    /**
     * Returns an error handler that persists stack traces to
     * {@code $HOME/.cobalt/errors}.
     *
     * <p>Each exception is written to a new file named with the current
     * timestamp in milliseconds, on a dedicated virtual thread so the
     * session-control decision is not delayed by the write.
     *
     * @return a handler that persists exceptions under the user home
     *         directory
     */
    static WhatsAppLinkedClientErrorHandler toFile() {
        return toFile(Path.of(System.getProperty("user.home"), ".cobalt", "errors"));
    }

    /**
     * Returns an error handler that persists stack traces to the supplied
     * directory.
     *
     * <p>Behaviour matches {@link #toFile()} except that files are written
     * under {@code directory}. The directory must exist or be creatable by
     * the running process; failure to write raises an
     * {@link UncheckedIOException} on the background thread.
     *
     * @param directory the directory where stack traces are written
     * @return a handler that persists exceptions under the given directory
     */
    static WhatsAppLinkedClientErrorHandler toFile(Path directory) {
        return defaultErrorHandler((api, throwable) -> Thread.startVirtualThread(() -> {
            var stackTraceWriter = new StringWriter();
            try(var stackTracePrinter = new PrintWriter(stackTraceWriter)) {
                var path = directory.resolve(System.currentTimeMillis() + ".txt");
                throwable.printStackTrace(stackTracePrinter);
                Files.writeString(path, stackTraceWriter.toString(), StandardOpenOption.CREATE);
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot serialize exception", exception);
            }
        }));
    }

    /**
     * Builds the shared error-handling policy used by the
     * {@link #toTerminal()} and {@link #toFile(Path)} factories, delegating
     * the stack-trace rendering to the supplied {@code printer}.
     *
     * <p>The returned handler logs the exception, renders it through the
     * {@code printer} when one is supplied, and returns the exception's own
     * {@link WhatsAppException#toErrorResult()}. Each {@link WhatsAppException}
     * subtype carries the {@link WhatsAppLinkedClientErrorResult} that matches
     * WhatsApp Web's native reaction to its failure mode, so the handler
     * does not classify the exception itself.
     *
     * @param printer consumer that renders the exception (for example to
     *                stderr or to a file); may be {@code null} to skip
     *                rendering entirely
     * @return a handler that logs, renders, and maps exceptions to a
     *         {@link WhatsAppLinkedClientErrorResult}
     */
    private static WhatsAppLinkedClientErrorHandler defaultErrorHandler(BiConsumer<LinkedWhatsAppClient, WhatsAppException> printer) {
        return (whatsapp, exception) -> {
            if (printer != null) {
                printer.accept(whatsapp, exception);
            }
            return exception.toErrorResult();
        };
    }

}
