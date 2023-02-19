package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.privacy.PrivacySettingValue;

import java.util.Map;

public interface OnWhatsappPrivacySettings extends Listener {
    /**
     * Called when the socket receives the privacy settings from Whatsapp
     *
     * @param whatsapp        an instance to the calling api
     * @param privacySettings the settings
     */
    @Override
    void onPrivacySettings(Whatsapp whatsapp, Map<PrivacySettingType, PrivacySettingValue> privacySettings);
}