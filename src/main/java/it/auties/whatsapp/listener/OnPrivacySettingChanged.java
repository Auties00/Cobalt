package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.privacy.PrivacySettingEntry;

public interface OnPrivacySettingChanged extends Listener {
    /**
     * Called when a privacy setting is modified
     *
     * @param oldPrivacyEntry the old entry
     * @param newPrivacyEntry the new entry
     */
    @Override
    void onPrivacySettingChanged(PrivacySettingEntry oldPrivacyEntry, PrivacySettingEntry newPrivacyEntry);
}
