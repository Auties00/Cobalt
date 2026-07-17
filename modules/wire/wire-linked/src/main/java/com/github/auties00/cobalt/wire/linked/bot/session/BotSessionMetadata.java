package com.github.auties00.cobalt.wire.linked.bot.session;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Metadata identifying and tracking a single bot conversation session on
 * WhatsApp.
 *
 * <p>A session groups a sequence of user messages and bot responses into a
 * logical conversation turn. The server assigns a unique
 * {@link #sessionId() sessionId} (typically a UUID) and the client reports a
 * {@link #sessionSource() sessionSource} that describes how the user
 * initiated the interaction (for example, via text input, voice, or a
 * type-ahead suggestion).
 *
 * <p>This metadata is carried inside
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata#sessionMetadata()}.
 *
 * @see BotSessionSource
 */
@ProtobufMessage(name = "BotSessionMetadata")
public final class BotSessionMetadata {
    /**
     * The server-assigned unique identifier for this session, for example
     * {@code "a1b2c3d4-e5f6-7890-abcd-ef1234567890"}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String sessionId;

    /**
     * The origin that triggered the creation of this session.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    BotSessionSource sessionSource;


    /**
     * Constructs a new {@code BotSessionMetadata} with the specified values.
     *
     * @param sessionId     the session identifier, or {@code null}
     * @param sessionSource the session source, or {@code null}
     */
    BotSessionMetadata(String sessionId, BotSessionSource sessionSource) {
        this.sessionId = sessionId;
        this.sessionSource = sessionSource;
    }

    /**
     * Returns the server-assigned unique identifier for this session.
     *
     * @return an {@code Optional} describing the session identifier, or an
     *         empty {@code Optional} if not set
     */
    public Optional<String> sessionId() {
        return Optional.ofNullable(sessionId);
    }

    /**
     * Returns the origin that triggered the creation of this session.
     *
     * @return an {@code Optional} describing the session source, or an empty
     *         {@code Optional} if not set
     */
    public Optional<BotSessionSource> sessionSource() {
        return Optional.ofNullable(sessionSource);
    }

    /**
     * Sets the server-assigned unique identifier for this session.
     *
     * @param sessionId the new session identifier, or {@code null}
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Sets the origin that triggered the creation of this session.
     *
     * @param sessionSource the new session source, or {@code null}
     */
    public void setSessionSource(BotSessionSource sessionSource) {
        this.sessionSource = sessionSource;
    }
}
