package it.auties.whatsapp.model.message.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.payment.PaymentOrderMessage;
import it.auties.whatsapp.model.message.standard.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

/**
 * A model interface that represents a message sent by a contact that provides a context. Classes
 * that implement this interface must provide an accessor named contextInfo to access said
 * property.
 */
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Data
@Accessors(fluent = true)
public sealed abstract class ContextualMessage implements Message permits ButtonsMessage, InteractiveMessage, InteractiveResponseMessage, ListMessage, TemplateMessage, ButtonReplyMessage, MediaMessage, PaymentOrderMessage, ContactMessage, ContactsArrayMessage, GroupInviteMessage, LiveLocationMessage, LocationMessage, PollCreationMessage, ProductMessage, RequestPhoneNumberMessage, TextMessage {
    /**
     * An empty instance of the context
     */
    private static final ContextInfo EMPTY_CONTEXT = new ContextInfo();

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 17, type = MESSAGE, implementation = ContextInfo.class)
    private ContextInfo contextInfo;

    /**
     * Returns the context info of this message or an empty instance
     *
     * @return a non-null context info
     */
    public ContextInfo contextInfo(){
        return Objects.requireNonNullElse(contextInfo, EMPTY_CONTEXT);
    }

    /**
     * Returns whether this message has a non-empty context info
     *
     * @return a boolean
     */
    public boolean hasContextInfo(){
        return contextInfo != null;
    }

    @JsonGetter
    private ContextInfo contextInfoJson(){
        return contextInfo;
    }
}
