package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.response.impl.json.PhoneBatteryResponse;
import it.auties.whatsapp4j.response.impl.json.BlocklistResponse;
import it.auties.whatsapp4j.response.impl.json.PropsResponse;
import it.auties.whatsapp4j.response.impl.json.UserInformationResponse;
import it.auties.whatsapp4j.response.model.json.JsonListResponse;
import org.jetbrains.annotations.NotNull;

public interface WhatsappListener {
    default void onLoggedIn(@NotNull UserInformationResponse info, boolean firstLogin){}
    default void onDisconnected(){}
    default void onInformationUpdate(@NotNull UserInformationResponse info){}
    default void onListResponse(@NotNull JsonListResponse response){}

    default void onContactsReceived(){ }
    default void onContactUpdate(@NotNull WhatsappContact contact){ }
    default void onContactReceived(@NotNull WhatsappContact contact){ }
    default void onContactPresenceUpdate(@NotNull WhatsappChat chat, @NotNull WhatsappContact contact){}

    default void onChatsReceived(){}
    default void onChatReceived(@NotNull WhatsappChat chat){}
    default void onChatArchived(@NotNull WhatsappChat chat){}
    default void onChatUnarchived(@NotNull WhatsappChat chat){}
    default void onChatMuteChange(@NotNull WhatsappChat chat){}
    default void onChatReadStatusChange(@NotNull WhatsappChat chat){}
    default void onChatEphemeralStatusChange(@NotNull WhatsappChat chat){}

    default void onNewMessageReceived(@NotNull WhatsappChat chat, @NotNull WhatsappMessage message){ }
    default void onMessageReadStatusUpdate(@NotNull WhatsappChat chat, @NotNull WhatsappContact contact, @NotNull WhatsappMessage message){}
    default void onMessageUpdate(@NotNull WhatsappChat chat, @NotNull WhatsappMessage message){}
    default void onMessageDeleted(@NotNull WhatsappChat chat, @NotNull WhatsappMessage message, boolean everyone){}
    default void onMessageStarred(@NotNull WhatsappChat chat, @NotNull WhatsappMessage message){}
    default void onMessageStatusUpdate(@NotNull WhatsappChat chat, @NotNull WhatsappMessage message){}
    default void onMessageUnstarred(@NotNull WhatsappChat chat, @NotNull WhatsappMessage message){}

    default void onBlocklistUpdate(@NotNull BlocklistResponse blocklist){}

    default void onPropsUpdate(@NotNull PropsResponse props){}

    default void onPhoneBatteryStatusUpdate(@NotNull PhoneBatteryResponse battery){}
}
