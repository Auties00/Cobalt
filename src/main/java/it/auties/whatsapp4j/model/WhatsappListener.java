package it.auties.whatsapp4j.model;

public abstract class WhatsappListener {
    public void onConnected(String name, boolean firstLogin){}
    public void onDisconnected(){}

    public void onPhoneStatusUpdate(){}

    public void onContactsReceived(){ }
    public void onContactReceived(){ }

    public void onChatsReceived(){}
    public void onChatReceived(WhatsappChat chat){}

    public void onNewMessageReceived(WhatsappChat chat, WhatsappMessage message, boolean sentByMe){}

    public void onBlacklistReceived(){}
    public void onBlacklistUpdate(){}
}
