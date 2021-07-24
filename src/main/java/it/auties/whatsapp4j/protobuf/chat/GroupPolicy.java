package it.auties.whatsapp4j.protobuf.chat;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.NonNull;

/**
 * The constants of this enumerated type describe the various policies that can be enforced for a {@link GroupSetting} in a {@link Chat}.
 * Said chat should be a group: {@link Chat#isGroup()}.
 * Said actions can be executed using various methods in {@link WhatsappAPI}.
 */
public enum GroupPolicy {
    /**
     * Allows both admins and users
     */
    ANYONE,

    /**
     * Allows only admins
     */
    ADMINS;

    /**
     * Returns a GroupPolicy based on a boolean value obtained from Whatsapp
     *
     * @param input the boolean value obtained from Whatsapp
     * @return a lowercase non null String
     */
    public static @NonNull GroupPolicy forData(boolean input) {
        return input ? ADMINS : ANYONE;
    }

    /**
     * Returns a boolean parsed as a string for Whatsapp
     *
     * @return a lowercase non null String
     */
    public @NonNull String data() {
        return String.valueOf(this == ADMINS);
    }
}