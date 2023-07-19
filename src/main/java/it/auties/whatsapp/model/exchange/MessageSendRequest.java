package it.auties.whatsapp.model.exchange;

import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.MessageInfo;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record MessageSendRequest(MessageInfo info, List<ContactJid> recipients, boolean force, boolean peer, Map<String, Object> additionalAttributes) {
    public static MessageSendRequest of(MessageInfo info) {
        return MessageSendRequest.builder()
                .info(info)
                .build();
    }

    public boolean hasRecipientOverride() {
        return recipients != null && !recipients.isEmpty();
    }
}
