package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.setting.Setting;

public interface OnSetting
        extends Listener {
    /**
     * Called when the socket receives a setting change from Whatsapp.
     *
     * @param setting the setting that was toggled
     */
    @Override
    void onSetting(Setting setting);
}