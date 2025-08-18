package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;


/**
 * A model class that represents a message holding a whatsapp group invite inside
 */
@ProtobufMessage(name = "Message.GroupInviteMessage")
public final class GroupInviteMessage implements ContextualMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid group;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String code;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    final long expirationSeconds;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String groupName;

    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    final byte[] thumbnail;

    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String caption;

    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    @ProtobufProperty(index = 8, type = ProtobufType.ENUM)
    final Type groupType;

    GroupInviteMessage(Jid group, String code, long expirationSeconds, String groupName, byte[] thumbnail, String caption, ContextInfo contextInfo, Type groupType) {
        this.group = group;
        this.code = code;
        this.expirationSeconds = expirationSeconds;
        this.groupName = groupName;
        this.thumbnail = thumbnail;
        this.caption = caption;
        this.contextInfo = contextInfo;
        this.groupType = groupType;
    }

    @Override
    public Message.Type type() {
        return Message.Type.GROUP_INVITE;
    }

    @Override
    public Category category() {
        return Category.STANDARD;
    }

    public Optional<ZonedDateTime> expiration() {
        return Clock.parseSeconds(expirationSeconds);
    }

    public Jid group() {
        return group;
    }

    public String code() {
        return code;
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }

    public String groupName() {
        return groupName;
    }

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    public Type groupType() {
        return groupType;
    }

    @Override
    public String toString() {
        return "GroupInviteMessage[" +
                "group=" + group + ", " +
                "code=" + code + ", " +
                "expirationSeconds=" + expirationSeconds + ", " +
                "groupName=" + groupName + ", " +
                "thumbnail=" + Arrays.toString(thumbnail) + ", " +
                "caption=" + caption + ", " +
                "contextInfo=" + contextInfo + ", " +
                "groupType=" + groupType + ']';
    }


    @ProtobufEnum(name = "Message.GroupInviteMessage.GroupType")
    public enum Type {
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