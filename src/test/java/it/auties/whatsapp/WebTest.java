package it.auties.whatsapp;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.util.Json;

// Just used for testing locally
public class WebTest {
    private static final String messageJson = """
            {
             "templateMessage" : {
                  "contextInfo" : { },
                  "content" : {
                    "templateId" : "123662662752370",
                    "body" : "Hi Alessandro Autiero \uD83D\uDC4B,\\n\\nThank you for your message.\\n\\nHow can I help you today?",
                    "footer" : "WATI's Chatbot",
                    "buttons" : [ {
                      "quickReplyButton" : {
                        "text" : "Know the Pricing",
                        "id" : "642030ee5832ed3cc3901a5f"
                      }
                    }, {
                      "quickReplyButton" : {
                        "text" : "Know how WATI works?",
                        "id" : "642030ee5832ed3cc3901a5f"
                      }
                    }, {
                      "quickReplyButton" : {
                        "text" : "Get Started",
                        "id" : "642030ee5832ed3cc3901a5f"
                      }
                    } ]
                  },
                  "fourRowTemplateFormat" : {
                    "content" : {
                      "namespace" : "e61db83b_dde9_4afb_b21c_fcb64502af0e|en_US",
                      "elementName" : "welcome_wati",
                      "fallbackLg" : "en",
                      "fallbackLc" : "US",
                      "localizableParameters" : [ {
                        "defaultValue" : "Alessandro Autiero"
                      } ],
                      "templateMessage" : {
                        "content" : { }
                      }
                    },
                    "buttons" : [ {
                      "quickReplyButton" : {
                        "id" : "642030ee5832ed3cc3901a5f"
                      }
                    }, {
                      "index" : 1,
                      "quickReplyButton" : {
                        "id" : "642030ee5832ed3cc3901a5f"
                      }
                    }, {
                      "index" : 2,
                      "quickReplyButton" : {
                        "id" : "642030ee5832ed3cc3901a5f"
                      }
                    } ]
                  }
                }
                }""";
    public static void main(String[] args) {

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> e.printStackTrace());
        var whatsapp = Whatsapp.lastConnection()
                .addLoggedInListener(api -> {
                    System.out.printf("Connected: %s%n", api.store().privacySettings());
                    var result = Json.readValue(messageJson, MessageContainer.class);
                    result.deviceInfo().deviceListMetadata().senderKeyHash(Bytes.ofBase64("v/n3vzFajeyg1Q==").toByteArray());
                    result.deviceInfo().deviceListMetadata().senderTimestamp(1679846963L);
                    api.sendMessage(ContactJid.of("14798024855"), result);
                })
                .addNewMessageListener(message -> System.out.println(message.toJson()))
                .addContactsListener((api, contacts) -> System.out.printf("Contacts: %s%n", contacts.size()))
                .addChatsListener(chats -> System.out.printf("Chats: %s%n", chats.size()))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addActionListener((action, info) -> System.out.printf("New action: %s, info: %s%n", action, info))
                .addSettingListener(setting -> System.out.printf("New setting: %s%n", setting))
                .addContactPresenceListener((chat, contact, status) -> System.out.printf("Status of %s changed in %s to %s%n", contact.name(), chat.name(), status.name()))
                .addAnyMessageStatusListener((chat, contact, info, status) -> System.out.printf("Message %s in chat %s now has status %s for %s %n", info.id(), info.chatName(), status, contact == null ? null : contact.name()))
                .addChatMessagesSyncListener((chat, last) -> System.out.printf("%s now has %s messages: %s%n", chat.name(), chat.messages().size(), !last ? "waiting for more" : "done"))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .connect()
                .join();
        System.out.println("Connected");
        whatsapp.awaitDisconnection();
        System.out.println("Disconnected");
    }
}
