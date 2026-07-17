package com.github.auties00.cobalt.wire.linked.message.newsletter;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A message that invites a recipient to become an administrator of a newsletter channel.
 *
 * <p>Newsletters are one-to-many broadcast channels on WhatsApp. Each channel can have
 * multiple administrators who are authorized to post content and manage the channel.
 * This message is sent to a user to propose that they accept an administrative role
 * on a specific newsletter. The recipient may accept or decline the invitation before
 * it expires.
 *
 * <p>An admin invite message carries the target newsletter's identity, its display
 * name and optional thumbnail so the recipient can preview the channel, an optional
 * caption describing the invitation, and an expiration timestamp after which the
 * invitation is no longer valid.
 */
@ProtobufMessage(name = "Message.NewsletterAdminInviteMessage")
public final class NewsletterAdminInviteMessage implements ContextualMessage {
    /**
     * The {@link Jid} of the newsletter channel the recipient is being invited to
     * administer.
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
     * short message from the inviter explaining the offer.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String caption;

    /**
     * The Unix timestamp, in seconds, after which the invitation is no longer valid
     * and can no longer be accepted.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64)
    Long inviteExpiration;

    /**
     * Optional {@link ContextInfo} describing the surrounding conversational context,
     * such as the quoted message this invite is replying to or forwarding metadata.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;


    /**
     * Constructs a new newsletter admin invite message with all of its fields.
     *
     * <p>This constructor is package-private and is intended to be invoked by the
     * generated protobuf builder. Application code should obtain instances via
     * {@code NewsletterAdminInviteMessageBuilder} rather than calling this
     * constructor directly.
     *
     * @param newsletterJid     the JID of the newsletter channel
     * @param newsletterName    the display name of the newsletter channel
     * @param jpegThumbnail     the raw JPEG bytes of the channel thumbnail
     * @param caption           the optional invitation caption
     * @param inviteExpiration  the Unix timestamp, in seconds, at which the invite expires
     * @param contextInfo       the optional context information of the message
     */
    NewsletterAdminInviteMessage(Jid newsletterJid, String newsletterName, byte[] jpegThumbnail, String caption, Long inviteExpiration, ContextInfo contextInfo) {
        this.newsletterJid = newsletterJid;
        this.newsletterName = newsletterName;
        this.jpegThumbnail = jpegThumbnail;
        this.caption = caption;
        this.inviteExpiration = inviteExpiration;
        this.contextInfo = contextInfo;
    }

    /**
     * Returns the {@link Jid} of the newsletter channel the recipient is invited to administer.
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
     * Returns the Unix timestamp, in seconds, at which this invitation expires.
     *
     * <p>After the returned instant, the invitation can no longer be accepted.
     *
     * @return an {@link OptionalLong} containing the expiration timestamp, or an empty
     *         {@code OptionalLong} if none is set
     */
    public OptionalLong inviteExpiration() {
        return inviteExpiration == null ? OptionalLong.empty() : OptionalLong.of(inviteExpiration);
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
     * Sets the Unix timestamp, in seconds, at which this invitation expires.
     *
     * @param inviteExpiration the expiration timestamp, or {@code null} to clear the value
     */
    public void setInviteExpiration(Long inviteExpiration) {
        this.inviteExpiration = inviteExpiration;
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
