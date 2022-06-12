package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that represents a message holding a whatsapp group invite inside
 */
@AllArgsConstructor(staticName = "newGroupInviteMessage")
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newGroupInviteMessage", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class GroupInviteMessage extends ContextualMessage {
    /**
     * The jid of the group that this invite regards
     */
    @ProtobufProperty(index = 1, type = STRING, concreteType = ContactJid.class, requiresConversion = true)
    private ContactJid groupId;

    /**
     * The invite code of this message
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String code;

    /**
     * The expiration of this invite in milliseconds since {@link java.time.Instant#EPOCH}
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
    private ContextInfo contextInfo; // Overrides ContextualMessage's context info
}
