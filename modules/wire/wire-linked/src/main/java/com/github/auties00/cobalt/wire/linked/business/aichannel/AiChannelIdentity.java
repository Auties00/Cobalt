package com.github.auties00.cobalt.wire.linked.business.aichannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Public identity of a WhatsApp GenAI agent channel.
 *
 * <p>A GenAI agent connected to a WhatsApp channel exposes a display name
 * and an avatar image that followers see in the channel header. This model
 * is that public identity: the {@linkplain #displayName() display name} and
 * the {@linkplain #avatarImageUrl() avatar image URL}.
 */
@ProtobufMessage(name = "AiChannelIdentity")
public final class AiChannelIdentity {
    /**
     * Agent display name shown in the channel header, or {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String displayName;

    /**
     * Resolved URL of the agent's avatar image, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String avatarImageUrl;

    /**
     * Constructs a new {@code AiChannelIdentity}. Any reference argument may
     * be {@code null} when the server omitted the corresponding field.
     *
     * @param displayName    the agent display name, or {@code null}
     * @param avatarImageUrl the resolved avatar image URL, or {@code null}
     */
    AiChannelIdentity(String displayName, String avatarImageUrl) {
        this.displayName = displayName;
        this.avatarImageUrl = avatarImageUrl;
    }

    /**
     * Returns the agent display name shown in the channel header.
     *
     * @return the display name, or empty when the server omitted it
     */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    /**
     * Returns the resolved URL of the agent's avatar image.
     *
     * @return the avatar image URL, or empty when the server omitted it
     */
    public Optional<String> avatarImageUrl() {
        return Optional.ofNullable(avatarImageUrl);
    }
}
