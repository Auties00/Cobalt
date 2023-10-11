package it.auties.whatsapp.model.privacy;

import it.auties.whatsapp.model.jid.Jid;

import java.util.List;

/**
 * A model that represents a privacy setting entry
 *
 * @param type the non-null type
 * @param value the non-null value
 * @param excluded the non-null list of excluded contacts if {@link PrivacySettingEntry#value} == {@link PrivacySettingValue#CONTACTS_EXCEPT}
 */
public record PrivacySettingEntry(PrivacySettingType type, PrivacySettingValue value, List<Jid> excluded) {
    /**
     * Canonical constructor
     */
    public PrivacySettingEntry(PrivacySettingType type, PrivacySettingValue value, List<Jid> excluded) {
        this.type = type;
        this.value = value;
        this.excluded = excluded == null ? List.of() : excluded;
    }

    /**
     * Checks if {@link PrivacySettingEntry#value} == {@link PrivacySettingValue#CONTACTS_EXCEPT}
     *
     * @return a boolean
     */
    private boolean hasExcluded(){
        return value == PrivacySettingValue.CONTACTS_EXCEPT;
    }
}
