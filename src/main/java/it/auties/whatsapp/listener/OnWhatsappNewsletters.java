package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.newsletter.Newsletter;

import java.util.Collection;

public interface OnWhatsappNewsletters extends Listener {
    /**
     * Called when the socket receives all the newsletters from WhatsappWeb's Socket
     *
     * @param whatsapp    an instance to the calling api
     * @param newsletters the newsletters
     */
    @Override
    void onNewsletters(Whatsapp whatsapp, Collection<Newsletter> newsletters);
}