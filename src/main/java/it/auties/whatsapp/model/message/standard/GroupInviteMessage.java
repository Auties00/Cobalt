package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.util.Clock;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * A model class that represents a message holding a whatsapp group invite inside
 */
@ProtobufMessageName("Message.GroupInviteMessage")
public record GroupInviteMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        Jid group,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String code,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
        long expirationSeconds,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        @NonNull
        String groupName,
        @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
        Optional<byte[]> thumbnail,
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        Optional<String> caption,
        @ProtobufProperty(index = 7, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo,
        @ProtobufProperty(index = 8, type = ProtobufType.OBJECT)
        Type groupType
) implements ContextualMessage {
    @Override
    public MessageType type() {
        return MessageType.GROUP_INVITE;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    public Optional<ZonedDateTime> expiration() {
        return Clock.parseSeconds(expirationSeconds);
    }

    @ProtobufMessageName("Message.GroupInviteMessage.GroupType")
    public enum Type implements ProtobufEnum {
        DEFAULT(0),
        PARENT(1);

        final int index;

        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }
    }
}