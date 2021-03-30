package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

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
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @Override
    public @NotNull Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        return info.getMessage().getGroupInviteMessage().hasContextInfo() ? Optional.of(info.getMessage().getGroupInviteMessage().getContextInfo()) : Optional.empty();
    }
}
