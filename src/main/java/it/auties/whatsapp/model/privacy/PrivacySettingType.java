package it.auties.whatsapp.model.privacy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * The constants of this enumerated type describe the various types of settings that a user can
 * toggle in his account's preferences
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum PrivacySettingType {
    /**
     * Refers to whether your last access on Whatsapp should be visible
     */
    LAST_SEEN("last"),
    /**
     * Refers to whether other people should be able to see when you are online
     */
    ONLINE("online"),
    /**
     * Refers to who should be able to see your profile pic
     */
    PROFILE_PIC("profile"),
    /**
     * Refers to who should be able to see your status
     */
    STATUS("status"),
    /**
     * Refers to who should be able to add you to groups
     */
    ADD_ME_TO_GROUPS("groupadd"),
    /**
     * Refers to whether read receipts should be sent and received for messages
     */
    READ_RECEIPTS("readreceipts");

    @Getter
    private final String data;

    public static Optional<PrivacySettingType> of(String id) {
        return Arrays.stream(values()).filter(entry -> Objects.equals(entry.data(), id)).findFirst();
    }
}
