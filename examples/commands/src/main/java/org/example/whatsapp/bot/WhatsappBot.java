package org.example.whatsapp.bot;

import it.auties.whatsapp4j.api.WhatsappAPI;
import org.example.whatsapp.command.CommandManager;
import org.example.whatsapp.command.HelloCommand;

public class WhatsappBot {
    public static void main(String... args) {
        var manager = new CommandManager();
        manager.addCommand(new HelloCommand());

        var api = new WhatsappAPI();
        api.registerListener(new WhatsappBotListener(api, manager));
        api.connect();
    }
}
