package it.auties.whatsapp.model.privacy;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * The constants of this enumerated type describe the various types of preferences that can be
 * toggled for a corresponding setting
 */
public enum PrivacySettingValue {
    /**
     * Everyone
     */
    EVERYONE("all"),
    /**
     * All the contacts saved on your Whatsapp's user
     */
    CONTACTS("contacts"),
    /**
     * All the contacts saved on your Whatsapp's user except some
     */
    CONTACTS_EXCEPT("contact_blacklist"),
    /**
     * Nobody
     */
    NOBODY("none"),
    /**
     * Match last seen
     */
    MATCH_LAST_SEEN("match_last_seen");

    private final String data;
    PrivacySettingValue(String data) {
        this.data = data;
    }

    public static Optional<PrivacySettingValue> of(String id) {
        return Arrays.stream(values())
                .filter(entry -> Objects.equals(entry.data(), id))
                .findFirst();
    }

    public String data() {
        return this.data;
    }
}
