package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;


/**
 * A model class that represents a message holding a whatsapp group invite inside
 */
@ProtobufMessage(name = "Message.GroupInviteMessage")
public final class GroupInviteMessage implements ContextualMessage<GroupInviteMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final Jid group;
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final String code;
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    private final long expirationSeconds;
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    private final String groupName;
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    private final byte[] thumbnail;
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    private final String caption;
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;
    @ProtobufProperty(index = 8, type = ProtobufType.ENUM)
    private final Type groupType;

    public GroupInviteMessage(Jid group, String code, long expirationSeconds, String groupName, byte[] thumbnail, String caption, ContextInfo contextInfo, Type groupType) {
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
    public GroupInviteMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
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