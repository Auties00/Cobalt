package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.response.impl.json.ModificationForParticipantStatus;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a whatsapp group invite inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
@ToString
public final class WhatsappGroupInviteMessage extends WhatsappUserMessage {
    /**
     * The jid of the group that this invite regards
     */
    private final @NotNull String jid;

    /**
     * The name of the group that this invite regards
     */
    private final @NotNull String name;

    /**
     * The caption for this invite
     */
    private final @NotNull String caption;

    /**
     * The code of the group that this invite regards
     */
    private final @NotNull String code;

    /**
     * The thumbnail of the group that this invite regards
     */
    private final @NotNull ByteBuffer thumbnail;

    /**
     * The date when this invitation expires
     */
    private final @NotNull ZonedDateTime expiration;
    
    /**
     * Constructs a WhatsappUserMessage from a raw protobuf object if it holds an invitation for a whatsapp group
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappGroupInviteMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, info.getMessage().hasGroupInviteMessage());
        var invite = info.getMessage().getGroupInviteMessage();
        this.jid = invite.getGroupJid();
        this.name = invite.getGroupName();
        this.caption = invite.getCaption();
        this.code = invite.getInviteCode();
        this.thumbnail = invite.getJpegThumbnail().asReadOnlyByteBuffer();
        this.expiration = ZonedDateTime.ofInstant(Instant.ofEpochSecond(invite.getInviteExpiration()), ZoneId.systemDefault());
    }

    /**
     * Constructs a new builder to create a WhatsappGroupInviteMessage.
     * The result can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @param chat             the non null chat to which the new message should belong
     * @param groupJid         the non null jid of the group that this invite regards
     * @param groupName        the non null name of the group that this invite regards
     * @param inviteCode       the non null code of this invite, can be obtained from {@link WhatsappAPI#queryGroupInviteCode(WhatsappChat)} or from {@link ModificationForParticipantStatus#inviteCode()}
     * @param inviteExpiration the expiration for this invite, by default three days after its creation
     * @param inviteCaption    the caption for this invite, by default empty
     * @param inviteThumbnail  the thumbnail for this invite, by default empty
     * @param quotedMessage    the message that the new message should quote, by default empty
     * @param forwarded        whether this message is forwarded or not, by default false
     */
    @Builder(builderMethodName = "newGroupInviteMessage", buildMethodName = "create")
    public WhatsappGroupInviteMessage(@NotNull(message = "Cannot create a WhatsappGroupInviteMessage with no chat") WhatsappChat chat, @NotNull(message = "Cannot create a WhatsappGroupInviteMessage with no group jid")  String groupJid, @NotNull(message = "Cannot create a WhatsappGroupInviteMessage with no group name")  String groupName, @NotNull(message = "Cannot create a WhatsappGroupInviteMessage with no invite code, please check WhatsappAPI#queryGroupInviteCode")  String inviteCode, ZonedDateTime inviteExpiration, String inviteCaption, byte[] inviteThumbnail, WhatsappUserMessage quotedMessage, boolean forwarded) {
        this(ProtobufUtils.createMessageInfo(ProtobufUtils.createGroupInviteMessage(groupJid, groupName, inviteCode, inviteExpiration, inviteCaption, inviteThumbnail, quotedMessage, forwarded), chat.jid()));
    }

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @Override
    public @NotNull Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        return info.getMessage().getGroupInviteMessage().hasContextInfo() ? Optional.of(info.getMessage().getGroupInviteMessage().getContextInfo()) : Optional.empty();
    }
}
