package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.model.WhatsappChat;
import org.jetbrains.annotations.NotNull;

/**
 * The constants of this enumerated type describe the various policies that can be enforced for a {@link GroupSetting} in a {@link it.auties.whatsapp4j.model.WhatsappChat}.
 * Said chat should be a group: {@link WhatsappChat#isGroup()}.
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
     * Returns the name of this enumerated constant
     *
     * @return a lowercase non null String
     */
    public @NotNull String data() {
        return String.valueOf(this == ADMINS);
    }
}