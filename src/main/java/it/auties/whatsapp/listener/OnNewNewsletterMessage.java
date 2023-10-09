package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.info.NewsletterMessageInfo;

public interface OnNewNewsletterMessage extends Listener {
    /**
     * Called when a new message is received in a newsletter
     *
     * @param info the message that was sent
     */
    @Override
    void onNewMessage(NewsletterMessageInfo info);
}