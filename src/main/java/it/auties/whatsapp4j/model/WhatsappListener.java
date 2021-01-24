package it.auties.whatsapp4j.model;

public abstract class WhatsappListener {
    public void onConnected(WhatsappUserInformation info, boolean firstLogin){}
    public void onDisconnected(){}
    public void onInformationUpdate(WhatsappUserInformation info){}

    public void onPhoneStatusUpdate(){}

    public void onContactsReceived(){ }
    public void onContactReceived(){ }

    public void onChatsReceived(){}
    public void onChatReceived(WhatsappChat chat){}

    public void onNewMessageReceived(WhatsappChat chat, WhatsappMessage message, boolean sentByMe){}

    public void onBlocklistUpdate(WhatsappBlocklist blocklist){}
}
