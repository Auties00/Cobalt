package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.response.BlocklistResponse;
import it.auties.whatsapp4j.response.PropsResponse;
import it.auties.whatsapp4j.response.UserInformationResponse;
import org.jetbrains.annotations.NotNull;

public interface WhatsappListener {
    default void onConnected(@NotNull UserInformationResponse info, boolean firstLogin){}
    default void onDisconnected(){}
    default void onInformationUpdate(UserInformationResponse info){}

    default void onPhoneStatusUpdate(){}

    default void onContactsReceived(){ }
    default void onContactReceived(){ }

    default void onChatsReceived(){}
    default void onChatReceived(WhatsappChat chat){}

    default void onNewMessageReceived(@NotNull WhatsappChat chat, @NotNull WhatsappMessage message){

    }

    default void onBlocklistUpdate(@NotNull BlocklistResponse blocklist){}

    default void onPropsUpdate(@NotNull PropsResponse props){}
}
