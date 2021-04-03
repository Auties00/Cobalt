package it.auties.whatsapp4j.builder;

import ezvcard.VCard;
import it.auties.whatsapp4j.model.WhatsappContactMessage;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Accessors(fluent = true)
public class WhatsappContactMessageBuilder extends WhatsappMessageBuilder<WhatsappContactMessage> {
    /**
     * A non null list of parsed VCards, one for each contact that the raw protobuf message used to build this object holds
     */
    private @Nullable List<VCard> sharedContacts;

    @Override
    public @NotNull WhatsappContactMessage create() {
        Objects.requireNonNull(chat, "WhatsappAPI: Cannot create a WhatsappContactMessage with a null chat");    
        Objects.requireNonNull(sharedContacts, "WhatsappAPI: Cannot create a WhatsappContactMessage with null sharedContacts");
        return new WhatsappContactMessage(ProtobufUtils.createMessageInfo(ProtobufUtils.createContactMessage(sharedContacts), chat.jid()));
    }
}
