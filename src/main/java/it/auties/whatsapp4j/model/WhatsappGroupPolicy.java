package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import jakarta.validation.constraints.NotNull;

/**
 * The constants of this enumerated type describe the various policies that can be enforced for a {@link WhatsappGroupSetting} in a {@link it.auties.whatsapp4j.model.WhatsappChat}.
 * Said chat should be a group: {@link WhatsappChat#isGroup()}.
 * Said actions can be executed using various methods in {@link WhatsappAPI}.
 */
public enum WhatsappGroupPolicy {
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
    public static @NotNull WhatsappGroupPolicy forData(boolean input) {
        return input ? ADMINS : ANYONE;
    }

    /**
     * Returns a boolean parsed as a string for Whatsapp
     *
     * @return a lowercase non null String
     */
    public @NotNull String data() {
        return String.valueOf(this == ADMINS);
    }
}