package it.auties.whatsapp.protobuf.message.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.message.button.*;
import it.auties.whatsapp.protobuf.message.standard.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.experimental.SuperBuilder;

/**
 * A model interface that represents a WhatsappMessage sent by a contact that provides a context.
 * Classes that implement this interface must provide an accessor named contextInfo to access said property.
 */
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(buildMethodName = "create")
@Data
@Accessors(fluent = true)
public sealed abstract class ContextualMessage implements Message
        permits ButtonsMessage, ButtonsResponseMessage, ListMessage,
        TemplateButtonReplyMessage, TemplateMessage, MediaMessage,
        ContactMessage, ContactsArrayMessage, GroupInviteMessage,
        LiveLocationMessage, LocationMessage, ProductMessage, TextMessage {
    /**
     * The context info of this message
     */
    @JsonProperty("17")
    @JsonPropertyDescription("context")
    private ContextInfo contextInfo;
}
