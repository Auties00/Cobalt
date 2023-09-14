package org.example.whatsapp.command;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Set;

public interface Command {
    void onCommand(@NonNull Whatsapp api, @NonNull MessageInfo message);

    String command();

    Set<String> alias();
}
