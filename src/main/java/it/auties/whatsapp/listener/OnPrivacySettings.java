package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.privacy.PrivacySettingValue;
import it.auties.whatsapp.socket.SocketHandler;

import java.util.Map;

public interface OnPrivacySettings extends Listener {
    /**
     * Called when {@link SocketHandler} receives the privacy settings from Whatsapp
     *
     * @param privacySettings the settings
     */
    void onPrivacySettings(Map<PrivacySettingType, PrivacySettingValue> privacySettings);
}