package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.payment.PaymentOrderMessage;
import it.auties.whatsapp.model.message.standard.*;

import java.util.Optional;

/**
 * A model interface that represents a message sent by a contact that provides a context. Classes
 * that implement this interface must provide an accessor named contextInfo to access said
 * property.
 */
public sealed interface ContextualMessage extends Message
        permits ButtonsMessage, InteractiveMessage, InteractiveResponseMessage, ListMessage,
        TemplateMessage, ButtonReplyMessage, MediaMessage, PaymentOrderMessage, ContactMessage, ContactsMessage,
        GroupInviteMessage, LiveLocationMessage, LocationMessage, PollCreationMessage, ProductMessage, RequestPhoneNumberMessage, TextMessage {
    Optional<ContextInfo> contextInfo();
    void setContextInfo(ContextInfo contextInfo);
}
