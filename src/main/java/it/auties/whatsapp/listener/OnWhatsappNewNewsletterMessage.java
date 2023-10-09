package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;

public interface OnWhatsappNewNewsletterMessage extends Listener {
    /**
     * Called when a new message is received in a newsletter
     *
     * @param whatsapp an instance to the calling api
     * @param info     the message that was sent
     */
    @Override
    void onNewMessage(Whatsapp whatsapp, NewsletterMessageInfo info);
}