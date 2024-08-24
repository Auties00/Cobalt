package it.auties.whatsapp.model.privacy;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * The constants of this enumerated type describe the various types of preferences that can be
 * toggled for a corresponding setting
 */
@ProtobufEnum
public enum PrivacySettingValue {
    /**
     * Everyone
     */
    EVERYONE(0, "all"),
    /**
     * All the contacts saved on your Whatsapp's user
     */
    CONTACTS(1, "contacts"),
    /**
     * All the contacts saved on your Whatsapp's user except some
     */
    CONTACTS_EXCEPT(2, "contact_blacklist"),
    /**
     * Nobody
     */
    NOBODY(3, "none"),
    /**
     * Match last seen
     */
    MATCH_LAST_SEEN(4, "match_last_seen");

    final int index;
    private final String data;

    PrivacySettingValue(@ProtobufEnumIndex int index, String data) {
        this.index = index;
        this.data = data;
    }

    public static Optional<PrivacySettingValue> of(String id) {
        return Arrays.stream(values())
                .filter(entry -> Objects.equals(entry.data(), id))
                .findFirst();
    }

    public int index() {
        return index;
    }

    public String data() {
        return this.data;
    }
}
