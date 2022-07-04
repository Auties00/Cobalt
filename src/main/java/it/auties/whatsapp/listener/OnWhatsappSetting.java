package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.socket.Socket;

public interface OnWhatsappSetting extends Listener {
    /**
     * Called when {@link Socket} receives a setting change from Whatsapp.
     *
     * @param setting the setting that was toggled
     */
    void onSetting(Whatsapp whatsapp, Setting setting);
}