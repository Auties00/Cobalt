package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.constant.ProtoBuf;
import it.auties.whatsapp4j.event.WhatsappListener;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record WhatsappManager(List<WhatsappChat> chats, List<WhatsappContact> contacts) {
    public static WhatsappManager buildInstance() {
        return new WhatsappManager(new ArrayList<>(), new ArrayList<>());
    }

    public Optional<WhatsappContact> findContactByJid(@NotNull String jid) {
        return contacts.stream().filter(e -> Objects.equals(e.jid(), jid)).findFirst();
    }

    public Optional<WhatsappChat> findChatByJid(@NotNull String jid) {
        return chats.stream().filter(e -> Objects.equals(e.jid(), jid)).findFirst();
    }

    public void addContact(@NotNull WhatsappContact contact) {
        contacts.add(contact);
    }

    public void addChat(@NotNull WhatsappChat chat) {
        chats.add(chat);
    }

    public void clear(){
        chats.clear();
        contacts.clear();
    }

    @SuppressWarnings("unchecked")
    public void digestWhatsappNode(@NotNull WhatsappNode node, @NotNull WhatsappListener listener) {
        var duplicate = Boolean.parseBoolean(node.attrs().getOrDefault("duplicate", "false"));
        if (duplicate) {
            return;
        }

        if (node.description().equals("response")) {
            var type = node.attrs().getOrDefault("type", "");
            if(type == null){
                return;
            }

            var nodes = (List<WhatsappNode>) node.content();
            if(nodes == null) {
                return;
            }

            if (type.equals("contacts")) {
                nodes.stream()
                        .map(WhatsappNode::attrs)
                        .filter(attrs -> !attrs.isEmpty())
                        .map(contactAttributes -> WhatsappContact
                                .builder()
                                .jid(contactAttributes.get("jid").split("@")[0])
                                .name(contactAttributes.get("name"))
                                .chosenName(contactAttributes.get("notify"))
                                .shortName(contactAttributes.get("short"))
                                .build())
                        .forEach(this::addContact);
                listener.onContactsReceived();
            } else if (type.equals("chat")) {
                nodes.stream()
                        .map(WhatsappNode::attrs)
                        .forEach(chatAttributes -> {
                            var chatName = chatAttributes.get("name");
                            var jid = chatAttributes.get("jid").split("@")[0];
                            var matchingContact = findContactByJid(jid);
                            var chat = WhatsappChat.builder()
                                    .jid(jid)
                                    .name(matchingContact.map(contact -> contact.bestName(chatName)).orElse(chatName))
                                    .unreadMessages(Integer.parseInt(chatAttributes.get("count")))
                                    .mute(chatAttributes.get("mute"))
                                    .spam(Boolean.parseBoolean(chatAttributes.get("spam")))
                                    .messages(new WhatsappMessages())
                                    .build();
                            addChat(chat);
                        });
                listener.onChatsReceived();
            }
        } else if (node.description().equals("action")) {
            var action = node.attrs().get("add");
            if(action == null){
                return;
            }

            var nodes = (List<WhatsappNode>) node.content();
            if(nodes == null) {
                return;
            }

            nodes.stream()
                    .map(WhatsappNode::content)
                    .map(ProtoBuf.WebMessageInfo.class::cast)
                    .filter(Objects::nonNull)
                    .forEach(message -> {
                        var jid = message.getKey().getRemoteJid().split("@")[0];
                        var chat = findChatByJid(jid).orElseGet(() -> {
                            var chatTemp = WhatsappChat.builder()
                                    .jid(jid)
                                    .name(message.hasMessage() && message.getMessage().hasExtendedTextMessage() && message.getMessage().getExtendedTextMessage().hasContextInfo() && message.getMessage().getExtendedTextMessage().getContextInfo().hasParticipant() ? message.getMessage().getExtendedTextMessage().getContextInfo().getParticipant() : message.hasParticipant() ? message.getParticipant() : jid)
                                    .messages(new WhatsappMessages())
                                    .build();
                            addChat(chatTemp);
                            listener.onChatReceived(chatTemp);
                            return chatTemp;
                        });

                        if (action.equals("last")) {
                            chat.messages().addLast(message);
                        } else {
                            chat.messages().addBefore(message);
                        }
                    });
        }
    }

    public void printData() {
        printContacts();
        printChats();
    }

    public void printContacts() {
        System.out.println(contacts);
    }

    public void printChats() {
        chats.stream().sorted(Comparator.comparingInt(whatsappChat -> whatsappChat.messages().size())).forEach(System.out::println);
    }
}
