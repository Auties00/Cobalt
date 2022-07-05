package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.socket.Socket;

public interface OnSetting extends Listener {
    /**
     * Called when {@link Socket} receives a setting change from Whatsapp.
     *
     * @param setting the setting that was toggled
     */
    @Override
    void onSetting(Setting setting);
}