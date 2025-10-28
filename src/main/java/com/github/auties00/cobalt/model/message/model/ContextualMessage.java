package com.github.auties00.cobalt.model.message.model;

import com.github.auties00.cobalt.model.info.ContextInfo;
import com.github.auties00.cobalt.model.core.proto.message.button.*;
import com.github.auties00.cobalt.model.core.proto.message.standard.*;
import com.github.auties00.cobalt.model.message.button.*;
import com.github.auties00.cobalt.model.message.standard.*;
import com.github.auties00.cobalt.model.support.proto.message.button.*;
import com.github.auties00.cobalt.model.message.payment.PaymentOrderMessage;
import com.github.auties00.cobalt.model.support.proto.message.standard.*;

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
