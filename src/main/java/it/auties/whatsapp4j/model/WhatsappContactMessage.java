package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.utils.ProtobufUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds a list of contacts inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
@ToString
public final class WhatsappContactMessage extends WhatsappUserMessage {
    /**
     * A non null list of Vcards, one for each contact that this message holds
     */
    private final @NotNull List<String> sharedContacts;

    /**
     * Constructs a WhatsappContactsMessage from a raw protobuf object if it holds an array of contacts
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappContactMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, info.getMessage().hasContactMessage() || info.getMessage().hasContactsArrayMessage());
        var contacts = info.getMessage().hasContactMessage() ? List.of(info.getMessage().getContactMessage()) : info.getMessage().getContactsArrayMessage().getContactsList();
        this.sharedContacts = contacts.stream().map(WhatsappProtobuf.ContactMessage::getVcard).toList();
    }

    /**
     * Constructs a new builder to create a WhatsappContactMessage.
     * The result can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @param chat the chat to which the new message should belong
     * @param sharedContacts a list of non parsed vcards, one for each contact that the new message should hold
     */
    @Builder(builderMethodName = "newContactMessage", buildMethodName = "create")
    public WhatsappContactMessage(@NotNull(message = "Cannot create a WhatsappContactMessage with no chat") WhatsappChat chat, @NotNull(message = "Cannot create a WhatsappContactMessage with no shared contacts") @Size(min = 1, message = "Cannot create a WhatsappContactMessage with no shared contacts") List<String> sharedContacts){
        this(ProtobufUtils.createMessageInfo(ProtobufUtils.createContactMessage(sharedContacts), chat.jid()));
    }

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @Override
    public @NotNull Optional<WhatsappProtobuf.ContextInfo> contextInfo() {
        var message = info.getMessage();
        if(message.hasContactMessage()){
            return message.getContactMessage().hasContextInfo() ? Optional.of(message.getContactMessage().getContextInfo()) : Optional.empty();
        }

        return message.getContactsArrayMessage().hasContextInfo() ? Optional.of(message.getContactsArrayMessage().getContextInfo()) : Optional.empty();
    }
}
