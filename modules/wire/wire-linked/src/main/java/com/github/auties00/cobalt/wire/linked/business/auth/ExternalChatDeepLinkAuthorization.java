package com.github.auties00.cobalt.wire.linked.business.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * The server's authorisation verdict for a chat opened from an external
 * deep-link.
 *
 * <p>When a third-party app or web surface deep-links into WhatsApp to open a
 * chat with one of its customers, the WhatsApp client must first ask the
 * server whether the chat may be opened on behalf of that partner. This model
 * is the server's response: whether the chat was authorised together with the
 * human-readable name of the authorising partner the UI displays in the
 * deep-link confirmation.
 */
@ProtobufMessage(name = "ExternalChatDeepLinkAuthorization")
public final class ExternalChatDeepLinkAuthorization {
    /**
     * Authorisation verdict. {@code true} when the server authorised the chat.
     * Defaults to {@code false} when the server omitted the field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean authorized;

    /**
     * Human-readable name of the authorising partner, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String partnerName;

    /**
     * Constructs a new {@code ExternalChatDeepLinkAuthorization}.
     *
     * @param authorized  the authorisation verdict
     * @param partnerName the authorising partner name, or {@code null}
     */
    ExternalChatDeepLinkAuthorization(boolean authorized, String partnerName) {
        this.authorized = authorized;
        this.partnerName = partnerName;
    }

    /**
     * Returns whether the server authorised the chat.
     *
     * @return {@code true} when the server authorised the chat, {@code false}
     *         when it did not or omitted the verdict
     */
    public boolean authorized() {
        return authorized;
    }

    /**
     * Returns the human-readable name of the authorising partner.
     *
     * @return the partner name, or empty when the server omitted it
     */
    public Optional<String> partnerName() {
        return Optional.ofNullable(partnerName);
    }
}
