package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.socket.Socket;

public interface OnWhatsappSetting extends Listener {
    /**
     * Called when {@link Socket} receives a setting change from Whatsapp.
     *
     * @param whatsapp an instance to the calling api
     * @param setting the setting that was toggled
     */
    @Override
    void onSetting(Whatsapp whatsapp, Setting setting);
}