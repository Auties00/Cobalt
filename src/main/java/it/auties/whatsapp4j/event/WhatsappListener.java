package it.auties.whatsapp4j.event;

import it.auties.whatsapp4j.model.WhatsappChat;

public abstract class WhatsappListener {
    public void onConnecting(){

    }
    public void onOpen(){

    }
    public void onClose(){

    }

    public void onPhoneStatusUpdateReceived(){

    }

    public void onContactsReceived(){

    }
    public void onContactUpdate(){

    }

    public void onChatReceived(WhatsappChat chat){

    }
    public void onChatsReceived(){

    }
    public void onChatUpdate(){

    }

    public void onBlacklistReceived(){

    }
    public void onBlacklistUpdate(){

    }
}
