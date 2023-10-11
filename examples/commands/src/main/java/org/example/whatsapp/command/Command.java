package org.example.whatsapp.command;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;

import java.util.Set;

public interface Command {
    void onCommand(Whatsapp api, MessageInfo message);

    String command();

    Set<String> alias();
}
