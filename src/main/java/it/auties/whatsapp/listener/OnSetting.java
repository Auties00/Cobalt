package it.auties.whatsapp.listener;

import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.setting.Setting;

public interface OnSetting extends Listener {
    /**
     * Called when {@link Socket} receives a setting change from Whatsapp.
     *
     * @param setting the setting that was toggled
     */
    void onSetting(Setting setting);
}