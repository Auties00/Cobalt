package it.auties.whatsapp.model.contact;

import it.auties.whatsapp.model.chat.Chat;
import lombok.NonNull;

/**
 * Utility interface to make providing a jid easier
 */
public sealed interface ContactJidProvider
    permits Chat, Contact, ContactJid {

  /**
   * Returns this object as a jid
   *
   * @return a non-null jid
   */
  @NonNull ContactJid toJid();
}
