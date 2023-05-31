package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;

public interface OnWhatsappRegistrationCode extends Listener {
    /**
     * Called when an OTP is requested from a new device
     * Only works on the mobile API
     *
     * @param whatsapp an instance to the calling api
     * @param code the registration code
     */
    @Override
    void onRegistrationCode(Whatsapp whatsapp, long code);
}