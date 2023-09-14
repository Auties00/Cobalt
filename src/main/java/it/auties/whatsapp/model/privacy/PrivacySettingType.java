package it.auties.whatsapp.model.privacy;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;

/**
 * The constants of this enumerated type describe the various types of settings that a user can
 * toggle in his account's preferences
 */
public enum PrivacySettingType {
    /**
     * Refers to whether your last access on Whatsapp should be visible
     */
    LAST_SEEN("last", Set.of(PrivacySettingValue.EVERYONE, PrivacySettingValue.CONTACTS, PrivacySettingValue.CONTACTS_EXCEPT, PrivacySettingValue.NOBODY)),
    /**
     * Refers to whether other people should be able to see when you are online
     */
    ONLINE("online", Set.of(PrivacySettingValue.EVERYONE, PrivacySettingValue.MATCH_LAST_SEEN)),
    /**
     * Refers to who should be able to see your profile pic
     */
    PROFILE_PIC("profile", Set.of(PrivacySettingValue.EVERYONE, PrivacySettingValue.CONTACTS, PrivacySettingValue.CONTACTS_EXCEPT, PrivacySettingValue.NOBODY)),
    /**
     * Refers to who should be able to see your status
     */
    STATUS("status", Set.of(PrivacySettingValue.EVERYONE, PrivacySettingValue.CONTACTS, PrivacySettingValue.CONTACTS_EXCEPT, PrivacySettingValue.NOBODY)),
    /**
     * Refers to who should be able to add you to groups
     */
    ADD_ME_TO_GROUPS("groupadd", Set.of(PrivacySettingValue.EVERYONE, PrivacySettingValue.CONTACTS, PrivacySettingValue.CONTACTS_EXCEPT)),
    /**
     * Refers to whether read receipts should be sent and received for messages
     */
    READ_RECEIPTS("readreceipts", Set.of(PrivacySettingValue.EVERYONE, PrivacySettingValue.NOBODY)),
    /**
     * Refers to who can add you to a call
     */
    CALL_ADD("calladd", Set.of(PrivacySettingValue.EVERYONE));

    private final String data;
    private final Set<PrivacySettingValue> values;

    PrivacySettingType(String data, Set<PrivacySettingValue> values) {
        this.data = data;
        this.values = values;
    }

    public static Optional<PrivacySettingType> of(String id) {
        return Arrays.stream(values())
                .filter(entry -> Objects.equals(entry.data(), id))
                .findFirst();
    }

    public Set<PrivacySettingValue> supportedValues() {
        return Collections.unmodifiableSet(values);
    }

    public boolean isSupported(@NonNull PrivacySettingValue value) {
        return values.contains(value);
    }

    public String data() {
        return this.data;
    }
}
