package it.auties.whatsapp4j.model;

import org.jetbrains.annotations.NotNull;

public abstract class WhatsappListener {
    public void onConnected(@NotNull WhatsappUserInformation info, boolean firstLogin){}
    public void onDisconnected(){}
    public void onInformationUpdate(WhatsappUserInformation info){}

    public void onPhoneStatusUpdate(){}

    public void onContactsReceived(){ }
    public void onContactReceived(){ }

    public void onChatsReceived(){}
    public void onChatReceived(WhatsappChat chat){}

    public void onNewMessageReceived(@NotNull WhatsappChat chat, @NotNull WhatsappMessage message, boolean sentByMe){}

    public void onBlocklistUpdate(@NotNull WhatsappBlocklist blocklist){}

    public void onPropsReceived(@NotNull WhatsappProps props){}
}
