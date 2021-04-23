package it.auties.whatsapp4j;

import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappContact;
import it.auties.whatsapp4j.model.WhatsappMessage;
import it.auties.whatsapp4j.response.impl.BlocklistResponse;
import it.auties.whatsapp4j.response.impl.PhoneBatteryResponse;
import it.auties.whatsapp4j.response.impl.PropsResponse;
import it.auties.whatsapp4j.response.impl.UserInformationResponse;
import it.auties.whatsapp4j.response.model.JsonListResponse;

public class WhatsappTestListener implements WhatsappListener {

  public void onLoggedIn(UserInformationResponse info, boolean firstLogin) { }

  public void onDisconnected() { }

  public void onInformationUpdate(UserInformationResponse info) { }

  public void onListResponse(JsonListResponse response) { }

  public void onContactsReceived() { }

  public void onContactUpdate(WhatsappContact contact) { }

  public void onContactReceived(WhatsappContact contact) { }

  public void onContactPresenceUpdate(WhatsappChat chat, WhatsappContact contact) { }

  public void onChatsReceived() { }

  public void onChatReceived(WhatsappChat chat) { }

  public void onChatArchived(WhatsappChat chat) { }

  public void onChatUnarchived(WhatsappChat chat) { }

  public void onChatMuteChange(WhatsappChat chat) { }

  public void onChatReadStatusChange(WhatsappChat chat) { }

  public void onChatEphemeralStatusChange(WhatsappChat chat) { }

  public void onNewMessageReceived(WhatsappChat chat, WhatsappMessage message) { }

  public void onMessageReadStatusUpdate(WhatsappChat chat, WhatsappContact contact, WhatsappMessage message) { }

  public void onMessageUpdate(WhatsappChat chat, WhatsappMessage message) { }

  public void onMessageDeleted(WhatsappChat chat, WhatsappMessage message, boolean everyone) { }

  public void onMessageStarred(WhatsappChat chat, WhatsappMessage message) { }

  public void onMessageUnstarred(WhatsappChat chat, WhatsappMessage message) { }

  public void onMessageGlobalReadStatusUpdate(WhatsappChat chat, WhatsappMessage message) { }

  public void onBlocklistUpdate(BlocklistResponse blocklist) { }

  public void onPropsUpdate(PropsResponse props) { }

  public void onPhoneBatteryStatusUpdate(PhoneBatteryResponse battery) { }

}
