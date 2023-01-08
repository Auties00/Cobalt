package it.auties.whatsapp.model.request;

import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.MessageInfo;
import java.util.Map;
import lombok.Builder;

@Builder
public record MessageSendRequest(MessageInfo info, ContactJid overrideSender, boolean force,
                                 Map<String, Object> additionalAttributes) {
  public static MessageSendRequest of(MessageInfo info) {
    return MessageSendRequest.builder()
        .info(info)
        .build();
  }

  public boolean hasSenderOverride() {
    return overrideSender != null;
  }
}
