package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;
import com.github.auties00.cobalt.exception.WhatsAppException;
import com.github.auties00.cobalt.exception.linked.WhatsAppSessionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers {@link WhatsAppSocketClient#classifyReaderFailure(Exception)}, the mapping the reader loop
 * uses to turn a caught fault into the exception surfaced via
 * {@link WhatsAppSocketListener#onError(WhatsAppException)} (or {@code null} for an orderly close).
 * The reader thread runs only on an established session, so the central guarantee asserted here is
 * that any unsolicited drop classifies as a reconnect, never as a terminal disconnect.
 */
class WhatsAppSocketReaderFailureTest {
    @Test
    @DisplayName("an EOFException is an orderly close with no surfaced error")
    void endOfStream() {
        assertNull(WhatsAppSocketClient.classifyReaderFailure(new EOFException()));
    }

    @Test
    @DisplayName("a BadMac is passed through unchanged")
    void badMac() {
        var badMac = new WhatsAppSessionException.BadMac();
        assertSame(badMac, WhatsAppSocketClient.classifyReaderFailure(badMac));
    }

    @ParameterizedTest(name = "{0} maps to a RECONNECT-classified session close")
    @MethodSource("reconnectWorthyFaults")
    @DisplayName("any other unsolicited drop maps to a RECONNECT-classified session close")
    void reconnectWorthyFault(Exception failure) {
        var surfaced = WhatsAppSocketClient.classifyReaderFailure(failure);
        assertInstanceOf(WhatsAppSessionException.Closed.class, surfaced);
        assertEquals(WhatsAppLinkedClientErrorResult.RECONNECT, surfaced.toErrorResult());
    }

    private static Stream<Arguments> reconnectWorthyFaults() {
        return Stream.of(
                Arguments.of(new SocketException("Connection reset")),
                Arguments.of(new IOException("boom")),
                Arguments.of(new IllegalStateException("decode bug"))
        );
    }
}
