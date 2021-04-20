package it.auties.whatsapp4j.model;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.io.chain.ChainingTextStringParser;
import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.builder.WhatsappContactMessageBuilder;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Collection;
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
     * A non null list of parsed VCards, one for each contact that the raw protobuf message used to build this object holds
     */
    private final @NotNull List<VCard> sharedContacts;

    /**
     * Constructs a WhatsappContactsMessage from a raw protobuf object if it holds an array of contacts
     *
     * @param info the raw protobuf to wrap
     */
    public WhatsappContactMessage(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        super(info, info.getMessage().hasContactMessage() || info.getMessage().hasContactsArrayMessage());
        var contacts = info.getMessage().hasContactMessage() ? List.of(info.getMessage().getContactMessage()) : info.getMessage().getContactsArrayMessage().getContactsList();
        this.sharedContacts = contacts.stream()
                .map(WhatsappProtobuf.ContactMessage::getVcard)
                .map(Ezvcard::parse)
                .map(ChainingTextStringParser::all)
                .flatMap(Collection::stream)
                .toList();
    }


    /**
     * Constructs a new {@link WhatsappContactMessageBuilder} to build a new message that can be later sent using {@link WhatsappAPI#sendMessage(WhatsappUserMessage)}
     *
     * @return a non null WhatsappContactMessageBuilder
     */
    public static @NotNull WhatsappContactMessageBuilder newContactMessage(){
        return new WhatsappContactMessageBuilder();
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
