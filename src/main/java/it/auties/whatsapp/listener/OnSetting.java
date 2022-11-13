package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.socket.SocketHandler;

public interface OnSetting extends Listener {
    /**
     * Called when {@link SocketHandler} receives a setting change from Whatsapp.
     *
     * @param setting the setting that was toggled
     */
    @Override
    void onSetting(Setting setting);
}