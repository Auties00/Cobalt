package com.github.auties00.cobalt.wire.linked.business.aichannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Pairing state between the caller's session and a WhatsApp GenAI agent
 * channel.
 *
 * <p>Before the WhatsApp client renders the agent-channel controls it checks
 * whether the session is bound to such a channel, whether the pairing is
 * complete, the channel's lifecycle status, and the Facebook-side channel
 * identifier (so the client can deep-link into the agent's management
 * surface). This model is that pairing state: the
 * {@linkplain #hasChannel() channel-presence flag}, the
 * {@linkplain #status() lifecycle status token}, the
 * {@linkplain #paired() pairing flag}, and the
 * {@linkplain #facebookChannelId() Facebook channel identifier}.
 */
@ProtobufMessage(name = "AiChannelLinkedStatus")
public final class AiChannelLinkedStatus {
    /**
     * Channel-presence flag. {@code true} when the session is bound to an
     * agent channel. Defaults to {@code false} when the server omitted the
     * field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean hasChannel;

    /**
     * Server-defined lifecycle status token, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String status;

    /**
     * Pairing flag. {@code true} when the session-to-channel pairing is
     * complete. Defaults to {@code false} when the server omitted the field.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean paired;

    /**
     * Facebook-side identifier of the linked agent channel, or {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String facebookChannelId;

    /**
     * Constructs a new {@code AiChannelLinkedStatus}.
     *
     * @param hasChannel        whether the session is bound to an agent
     *                          channel
     * @param status            the lifecycle status token, or {@code null}
     * @param paired            whether the session-to-channel pairing is
     *                          complete
     * @param facebookChannelId the Facebook channel identifier, or
     *                          {@code null}
     */
    AiChannelLinkedStatus(boolean hasChannel, String status, boolean paired, String facebookChannelId) {
        this.hasChannel = hasChannel;
        this.status = status;
        this.paired = paired;
        this.facebookChannelId = facebookChannelId;
    }

    /**
     * Returns whether the session is bound to an agent channel.
     *
     * @return {@code true} when bound, {@code false} when not or when the
     *         server omitted the flag
     */
    public boolean hasChannel() {
        return hasChannel;
    }

    /**
     * Returns the server-defined lifecycle status token.
     *
     * @return the status token, or empty when the server omitted it
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns whether the session-to-channel pairing is complete.
     *
     * @return {@code true} when paired, {@code false} when not or when the
     *         server omitted the flag
     */
    public boolean paired() {
        return paired;
    }

    /**
     * Returns the Facebook-side identifier of the linked agent channel.
     *
     * @return the Facebook channel identifier, or empty when the server
     *         omitted it
     */
    public Optional<String> facebookChannelId() {
        return Optional.ofNullable(facebookChannelId);
    }
}
