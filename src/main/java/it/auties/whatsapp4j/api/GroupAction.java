package it.auties.whatsapp4j.api;

import org.jetbrains.annotations.NotNull;

public enum GroupAction {
    ADD, REMOVE, PROMOTE, DEMOTE;

    public @NotNull String data(){
        return name().toLowerCase();
    }
}
