package it.auties.whatsapp4j.api;

import org.jetbrains.annotations.NotNull;

/**
 * The constants of this enumerated type describe the various policies that can be enforced for a {@code GroupSetting} in a {@code WhatsappChat}
 * Said chat should be a group
 * Said actions can be executed using various methods in {@code WhatsappAPI}
 */
public enum GroupPolicy {
    ANYONE,
    ADMINS;

    public @NotNull String data() {
        return String.valueOf(this == ADMINS);
    }
}