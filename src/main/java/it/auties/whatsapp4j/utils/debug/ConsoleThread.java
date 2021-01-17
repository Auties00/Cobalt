//TODO: Remove this class when development is complete
package it.auties.whatsapp4j.utils.debug;

import it.auties.whatsapp4j.model.WhatsappManager;
import lombok.AllArgsConstructor;

import java.util.Scanner;

@AllArgsConstructor
public class ConsoleThread extends Thread{
    private final WhatsappManager manager;
    @Override
    public void run() {
        try {
            final var scanner = new Scanner(System.in);
            String s = scanner.nextLine();
            if (s.equals("contacts")) {
                manager.printContacts();
            } else if (s.equals("chats")) {
                manager.printChats();
            } else if(s.equals("clear")){
                System.out.flush();
                System.out.println("Cleaned!");
            }else if (s.equals("data")) {
                manager.printData();
            } else if(s.contains("chat")){
                var chatJid = s.split(" ", 2)[1];
                var chat = manager.findChatByJid(chatJid).orElseThrow();
                System.out.printf("Chat: %s Group: %s%n", chat.name(), chat.isGroup());
                System.out.println(DebugUtils.chatMessagesToString(chat));
            }else if(s.contains("contact")){
                var contactJid = s.split(" ", 2)[1];
                System.out.println(manager.findContactByJid(contactJid).orElseThrow());
            }else {
                System.out.println("Unknown command!");
            }

        }finally {
            run();
        }
    }
}
