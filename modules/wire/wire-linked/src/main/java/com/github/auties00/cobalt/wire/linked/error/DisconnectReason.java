package com.github.auties00.cobalt.wire.linked.error;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A terminal failure marker that the WhatsApp server embeds in an app-state
 * synchronization patch.
 *
 * <p>When the server cannot produce a usable patch for a collection such as
 * chats, contacts, or starred messages it attaches an instance of this
 * message instead of, or in addition to, the normal mutation payload. The
 * client is expected to treat any patch that carries a disconnect reason as
 * fatal, log the numeric {@link #code()} and the optional {@link #text()},
 * and trigger a full re-synchronization of the affected collection.
 *
 * <p>The {@link #code()} field classifies the nature of the failure using
 * the {@link DisconnectCode} enumeration. The {@link #text()} field carries
 * an optional human-readable description that is useful for diagnostic logs
 * and error reports but is not intended to be shown to end users.
 *
 * @see DisconnectCode
 */
@ProtobufMessage(name = "ExitCode")
public final class DisconnectReason {
    /**
     * The numeric code that classifies the terminal condition, or
     * {@code null} if the server did not supply one.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
    DisconnectCode code;

    /**
     * An optional human-readable description of the terminal condition,
     * supplied by the server for diagnostic purposes.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String text;

    /**
     * Constructs a new {@code DisconnectReason} with the given code and
     * optional descriptive text.
     *
     * @param code the numeric code that classifies the terminal condition,
     *        or {@code null} if unknown
     * @param text an optional human-readable description of the terminal
     *        condition, or {@code null} if not provided
     */
    DisconnectReason(DisconnectCode code, String text) {
        this.code = code;
        this.text = text;
    }

    /**
     * Returns the numeric code that classifies the terminal condition.
     *
     * @return an {@link Optional} containing the {@link DisconnectCode}, or
     *         an empty {@code Optional} if no code was supplied by the server
     */
    public Optional<DisconnectCode> code() {
        return Optional.ofNullable(code);
    }

    /**
     * Returns the human-readable description of the terminal condition.
     *
     * @return an {@link Optional} containing the descriptive text, or an
     *         empty {@code Optional} if no text was supplied by the server
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Replaces the numeric code that classifies the terminal condition.
     *
     * @param code the new code, or {@code null} to clear
     */
    public void setCode(DisconnectCode code) {
        this.code = code;
    }

    /**
     * Replaces the human-readable description of the terminal condition.
     *
     * @param text the new descriptive text, or {@code null} to clear
     */
    public void setText(String text) {
        this.text = text;
    }
}
