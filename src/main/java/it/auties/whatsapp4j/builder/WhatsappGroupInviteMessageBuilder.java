package it.auties.whatsapp4j.builder;

import it.auties.whatsapp4j.model.WhatsappGroupInviteMessage;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import jakarta.validation.constraints.NotNull;


import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Accessors(fluent = true)
public class WhatsappGroupInviteMessageBuilder extends WhatsappMessageBuilder<WhatsappGroupInviteMessage> {
    /**
     * The jid of the group that this invite regards
     */
    private  @Setter String jid;

    /**
     * The name of the group that this invite regards
     */
    private  @Setter String name;

    /**
     * The caption for this invite
     */
    private  @Setter String caption;

    /**
     * The code of the group that this invite regards
     */
    private  @Setter String code;

    /**
     * The thumbnail of the group that this invite regards
     */
    private  @Setter ByteBuffer thumbnail;

    /**
     * The date when this invitation expires
     */
    private  @Setter ZonedDateTime expiration;

    /**
     * Builds a {@link WhatsappGroupInviteMessage} from the data provided
     *
     * @return a non null WhatsappGroupInviteMessage
     */
    @Override
    public @NotNull WhatsappGroupInviteMessage create() {
        Objects.requireNonNull(chat, "WhatsappAPI: Cannot create a WhatsappGroupInvite with a null chat");
        Objects.requireNonNull(jid, "WhatsappAPI: Cannot create a WhatsappGroupInvite with a null jid");
        Objects.requireNonNull(name, "WhatsappAPI: Cannot create a WhatsappGroupInvite with a null name");
        Objects.requireNonNull(code, "WhatsappAPI: Cannot create a WhatsappGroupInvite with a null code");
        return new WhatsappGroupInviteMessage(ProtobufUtils.createMessageInfo(ProtobufUtils.createGroupInviteMessage(jid, name, caption, code, thumbnail, expiration), chat.jid()));
    }
}
