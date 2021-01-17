//TODO: Remove this class when development is complete
package it.auties.whatsapp4j.utils.debug;

import it.auties.whatsapp4j.model.WhatsappChat;
import lombok.experimental.UtilityClass;

import java.util.stream.Collectors;

@UtilityClass
public class DebugUtils {
    public String chatMessagesToString(WhatsappChat chat){
        return chat
                .messages()
                .stream()
                .map(webMessageInfoContent ->  {
                    var message = webMessageInfoContent.getMessage();
                    var to = chat.isGroup() ? webMessageInfoContent.getKey().getFromMe() ? "me" : webMessageInfoContent.getParticipant() : webMessageInfoContent.getKey().getFromMe() ? "me" : chat.name() == null ? chat.jid() : chat.name();
                    if (message.hasConversation()) {
                        return "%s: %s".formatted(to, message.getConversation());
                    }

                    if (message.hasExtendedTextMessage()) {
                        return "%s: %s".formatted(to, message.getExtendedTextMessage().getText());
                    }

                    if(message.hasDocumentMessage()){
                        return "%s: %s".formatted(to, message.getDocumentMessage().getFileName());
                    }

                    if(message.hasStickerMessage()){
                        return "%s: %s".formatted(to, message.getStickerMessage().getMimetype());
                    }

                    if(message.hasImageMessage()) {
                        return "%s: Image, %s".formatted(to, message.getImageMessage().getCaption());
                    }

                    var msg = message.hasChat() + " " + message.hasVideoMessage() + " " + message.hasAudioMessage() + " " + message.hasCall() + " " + message.hasCancelPaymentRequestMessage() + " " + message.hasContactMessage() + " " + message.hasContactsArrayMessage() + " " + message.hasDeclinePaymentRequestMessage() + " " + message.hasDeviceSentMessage() + " " + message.hasDeviceSyncMessage() + " " + message.hasFastRatchetKeySenderKeyDistributionMessage() + " " + message.hasGroupInviteMessage() + " " + message.hasHighlyStructuredMessage() + " " + message.hasLiveLocationMessage() + " " + message.hasLocationMessage() + " " + message.hasProductMessage() + " " + message.hasProtocolMessage() + " " + message.hasRequestPaymentMessage() + " " + message.hasSenderKeyDistributionMessage() + " " + message.hasSendPaymentMessage() + " " + message.hasTemplateButtonReplyMessage() + " " + message.hasTemplateMessage();
                    return "%s: unknown(%s)".formatted(to, msg);
                })
                .collect(Collectors.joining("\n"));
    }
}
