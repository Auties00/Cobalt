package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.privacy.PrivacySettingEntry;

public interface OnWhatsappPrivacySettingChanged extends Listener {
    /**
     * Called when a privacy setting is modified
     *
     * @param whatsapp an instance to the calling api
     * @param oldPrivacyEntry the old entry
     * @param newPrivacyEntry the new entry
     */
    @Override
    void onPrivacySettingChanged(Whatsapp whatsapp, PrivacySettingEntry oldPrivacyEntry, PrivacySettingEntry newPrivacyEntry);
}
