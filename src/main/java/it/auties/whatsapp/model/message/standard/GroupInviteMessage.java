package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.util.Clock;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that represents a message holding a whatsapp group invite inside
 */
@AllArgsConstructor(staticName = "newGroupInviteMessage")
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newGroupInviteMessageBuilder")
@Jacksonized
@Accessors(fluent = true)
public final class GroupInviteMessage extends ContextualMessage {
    /**
     * The jid of the group that this invite regards
     */
    @ProtobufProperty(index = 1, type = STRING, concreteType = ContactJid.class, requiresConversion = true)
    private ContactJid group;

    /**
     * The invite code of this message
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String code;

    /**
     * The expiration of this invite in seconds since {@link java.time.Instant#EPOCH}.
     * For example if this invite should expire in three days: {@code ZonedDateTime.now().plusDays(3).toEpochSecond()}
     */
    @ProtobufProperty(index = 3, type = UINT64)
    private long expiration;

    /**
     * The name of the group that this invite regards
     */
    @ProtobufProperty(index = 4, type = STRING)
    private String groupName;

    /**
     * The thumbnail of the group that this invite regards encoded as jpeg in an array of bytes
     */
    @ProtobufProperty(index = 5, type = BYTES)
    private byte[] thumbnail;

    /**
     * The caption of this invite
     */
    @ProtobufProperty(index = 6, type = STRING)
    private String caption;

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 7, type = MESSAGE, concreteType = ContextInfo.class)
    @Default
    private ContextInfo contextInfo = new ContextInfo();  // Overrides ContextualMessage's context info

    /**
     * The type of this invite
     */
    @ProtobufProperty(index = 8, type = MESSAGE, concreteType = Type.class)
    @Default
    private Type groupType = Type.DEFAULT;

    @Override
    public MessageType type() {
        return MessageType.GROUP_INVITE;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    /**
     * Returns the expiration of this invite
     *
     * @return a non-null optional wrapping a zoned date time
     */
    private Optional<ZonedDateTime> expiration() {
        return Clock.parse(expiration);
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum Type implements ProtobufMessage {
        DEFAULT(0),
        PARENT(1);

        @Getter
        private final int index;

        @JsonCreator
        public static Type forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
