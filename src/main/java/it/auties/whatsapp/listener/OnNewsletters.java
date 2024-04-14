package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.newsletter.Newsletter;

import java.util.Collection;

public interface OnNewsletters extends Listener {
    /**
     * Called when the socket receives all the newsletters from WhatsappWeb's Socket
     *
     * @param newsletters the newsletters
     */
    @Override
    void onNewsletters(Collection<Newsletter> newsletters);
}