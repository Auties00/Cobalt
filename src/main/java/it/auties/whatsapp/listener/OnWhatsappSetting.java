package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.setting.Setting;

public interface OnWhatsappSetting extends QrDiscardingListener {
    /**
     * Called when {@link Socket} receives a setting change from Whatsapp.
     *
     * @param setting the setting that was toggled
     */
    void onSetting(Whatsapp whatsapp, Setting setting);
}