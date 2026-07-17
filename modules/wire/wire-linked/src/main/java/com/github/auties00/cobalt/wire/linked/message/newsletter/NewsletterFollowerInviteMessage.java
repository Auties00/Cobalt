package com.github.auties00.cobalt.wire.linked.message.newsletter;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A message that invites a recipient to follow (subscribe to) a newsletter channel.
 *
 * <p>Newsletters are one-to-many broadcast channels on WhatsApp. Followers receive
 * every post published by the channel's administrators but cannot post themselves.
 * This message is sent to a user to suggest that they subscribe to a specific channel
 * and typically appears inline inside a regular chat.
 *
 * <p>A follower invite message carries the target newsletter's identity, its display
 * name and optional thumbnail so the recipient can preview the channel before
 * following, and an optional caption describing why the channel is being recommended.
 */
@ProtobufMessage(name = "Message.NewsletterFollowerInviteMessage")
public final class NewsletterFollowerInviteMessage implements ContextualMessage {
    /**
     * The {@link Jid} of the newsletter channel the recipient is being invited to follow.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid newsletterJid;

    /**
     * The human-readable display name of the newsletter channel at the time the
     * invitation is issued.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String newsletterName;

    /**
     * The raw JPEG bytes of the newsletter channel's thumbnail image, shown as a
     * preview alongside the invitation.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] jpegThumbnail;

    /**
     * An optional free-text caption that accompanies the invitation, typically a
     * short message from the inviter describing the channel.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String caption;

    /**
     * Optional {@link ContextInfo} describing the surrounding conversational context,
     * such as the quoted message this invite is replying to or forwarding metadata.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;


    /**
     * Constructs a new newsletter follower invite message with all of its fields.
     *
     * <p>This constructor is package-private and is intended to be invoked by the
     * generated protobuf builder. Application code should obtain instances via
     * {@code NewsletterFollowerInviteMessageBuilder} rather than calling this
     * constructor directly.
     *
     * @param newsletterJid   the JID of the newsletter channel
     * @param newsletterName  the display name of the newsletter channel
     * @param jpegThumbnail   the raw JPEG bytes of the channel thumbnail
     * @param caption         the optional invitation caption
     * @param contextInfo     the optional context information of the message
     */
    NewsletterFollowerInviteMessage(Jid newsletterJid, String newsletterName, byte[] jpegThumbnail, String caption, ContextInfo contextInfo) {
        this.newsletterJid = newsletterJid;
        this.newsletterName = newsletterName;
        this.jpegThumbnail = jpegThumbnail;
        this.caption = caption;
        this.contextInfo = contextInfo;
    }

    /**
     * Returns the {@link Jid} of the newsletter channel the recipient is invited to follow.
     *
     * @return an {@link Optional} containing the newsletter JID, or an empty {@code Optional}
     *         if none is set
     */
    public Optional<Jid> newsletterJid() {
        return Optional.ofNullable(newsletterJid);
    }

    /**
     * Returns the display name of the newsletter channel at the time the invite was issued.
     *
     * @return an {@link Optional} containing the newsletter name, or an empty {@code Optional}
     *         if none is set
     */
    public Optional<String> newsletterName() {
        return Optional.ofNullable(newsletterName);
    }

    /**
     * Returns the raw JPEG bytes of the newsletter channel's thumbnail image.
     *
     * @return an {@link Optional} containing the thumbnail bytes, or an empty {@code Optional}
     *         if no thumbnail is attached
     */
    public Optional<byte[]> jpegThumbnail() {
        return Optional.ofNullable(jpegThumbnail);
    }

    /**
     * Returns the free-text caption that accompanies this invitation.
     *
     * @return an {@link Optional} containing the caption, or an empty {@code Optional}
     *         if no caption is set
     */
    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    /**
     * Returns the {@link ContextInfo} that describes the conversational context of this message.
     *
     * @return an {@link Optional} containing the context information, or an empty {@code Optional}
     *         if none is set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Sets the {@link Jid} of the newsletter channel this invitation refers to.
     *
     * @param newsletterJid the newsletter JID, or {@code null} to clear the value
     */
    public void setNewsletterJid(Jid newsletterJid) {
        this.newsletterJid = newsletterJid;
    }

    /**
     * Sets the display name of the newsletter channel.
     *
     * @param newsletterName the newsletter name, or {@code null} to clear the value
     */
    public void setNewsletterName(String newsletterName) {
        this.newsletterName = newsletterName;
    }

    /**
     * Sets the raw JPEG bytes of the newsletter channel's thumbnail image.
     *
     * @param jpegThumbnail the thumbnail bytes, or {@code null} to clear the thumbnail
     */
    public void setJpegThumbnail(byte[] jpegThumbnail) {
        this.jpegThumbnail = jpegThumbnail;
    }

    /**
     * Sets the free-text caption that accompanies this invitation.
     *
     * @param caption the caption, or {@code null} to clear the value
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Sets the {@link ContextInfo} that describes the conversational context of this message.
     *
     * @param contextInfo the context information, or {@code null} to clear the value
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }
}
