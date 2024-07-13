package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Optional;

@ProtobufMessage
public record NewsletterAdminInviteMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid newsletterJid,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String newsletterName,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] jpegThumbnail,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String caption,
        @ProtobufProperty(index = 5, type = ProtobufType.INT64)
        long inviteExpirationTimestampSeconds
) implements Message {
    public Optional<ZonedDateTime> inviteExpirationTimestamp() {
        return Clock.parseSeconds(inviteExpirationTimestampSeconds);
    }

    @Override
    public MessageType type() {
        return MessageType.NEWSLETTER_ADMIN_INVITE;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}
