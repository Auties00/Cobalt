package it.auties.whatsapp4j.api;

import org.jetbrains.annotations.NotNull;


/**
 * The constants of this enumerated type describe the various actions that can be executed on a {@code WhatsappContact} in a {@code WhatsappChat}
 * Said chat should be a group
 * Said actions can be executed using various methods in {@code WhatsappAPI}
 */
public enum GroupAction {
    ADD, REMOVE, PROMOTE, DEMOTE;

    public @NotNull String data(){
        return name().toLowerCase();
    }
}
