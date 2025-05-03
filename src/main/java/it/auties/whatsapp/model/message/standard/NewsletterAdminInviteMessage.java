package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@ProtobufMessage
public final class NewsletterAdminInviteMessage implements Message {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid newsletterJid;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String newsletterName;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final byte[] jpegThumbnail;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String caption;

    @ProtobufProperty(index = 5, type = ProtobufType.INT64)
    final long inviteExpirationTimestampSeconds;

    NewsletterAdminInviteMessage(Jid newsletterJid, String newsletterName, byte[] jpegThumbnail, String caption, long inviteExpirationTimestampSeconds) {
        this.newsletterJid = Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
        this.newsletterName = Objects.requireNonNull(newsletterName, "newsletterName cannot be null");
        this.jpegThumbnail = Objects.requireNonNull(jpegThumbnail, "jpegThumbnail cannot be null");
        this.caption = Objects.requireNonNull(caption, "caption cannot be null");
        this.inviteExpirationTimestampSeconds = inviteExpirationTimestampSeconds;
    }

    public Jid newsletterJid() {
        return newsletterJid;
    }

    public String newsletterName() {
        return newsletterName;
    }

    public byte[] jpegThumbnail() {
        return jpegThumbnail;
    }

    public String caption() {
        return caption;
    }

    public long inviteExpirationTimestampSeconds() {
        return inviteExpirationTimestampSeconds;
    }

    public Optional<ZonedDateTime> inviteExpirationTimestamp() {
        return Clock.parseSeconds(inviteExpirationTimestampSeconds);
    }

    @Override
    public Type type() {
        return Type.NEWSLETTER_ADMIN_INVITE;
    }

    @Override
    public Category category() {
        return Category.STANDARD;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewsletterAdminInviteMessage that
                && Objects.equals(newsletterJid, that.newsletterJid)
                && Objects.equals(newsletterName, that.newsletterName)
                && Arrays.equals(jpegThumbnail, that.jpegThumbnail)
                && Objects.equals(caption, that.caption)
                && inviteExpirationTimestampSeconds == that.inviteExpirationTimestampSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(newsletterJid, newsletterName, Arrays.hashCode(jpegThumbnail), caption, inviteExpirationTimestampSeconds);
    }

    @Override
    public String toString() {
        return "NewsletterAdminInviteMessage[" +
                "newsletterJid=" + newsletterJid + ", " +
                "newsletterName=" + newsletterName + ", " +
                "jpegThumbnail=" + Arrays.toString(jpegThumbnail) + ", " +
                "caption=" + caption + ", " +
                "inviteExpirationTimestampSeconds=" + inviteExpirationTimestampSeconds +
                ']';
    }
}