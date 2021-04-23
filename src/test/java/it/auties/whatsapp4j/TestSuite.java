package it.auties.whatsapp4j;


import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.api.WhatsappMessageRequest;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappContact;
import it.auties.whatsapp4j.model.WhatsappContactStatus;
import it.auties.whatsapp4j.model.WhatsappMessage;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.response.impl.MessageResponse;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class TestSuite {

  @Test
  public void testConnectionFlowBasic() {

    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    // Connect to the client
    api.connect();

    // Disconnect the client
    api.disconnect();
  }


  @Test
  public void testConnectionFlowConfiguration() {

    // Instantiate configuration object
    final var configuration = WhatsappConfiguration.builder()
        .whatsappUrl("wss://web.whatsapp.com/ws") // WhatsappWeb's WebSocket URL
        .whatsappTag("W4J") // The tag used for log in requests
        .requestTag("requestTag") // The tag used for post log in requests
        .description("Whatsapp4j") // The description provided to Whatsapp during the authentication process
        .shortDescription("W4J") // An acronym for the description
        .reconnectWhenDisconnected((reason) -> true) // Determines whether the connection should be reclaimed
        .async(true) // Determines whether requests sent to whatsapp should be asyncronous or not
        .build(); // Builds an instance of WhatsappConfiguration

    // Create a configured instance of the API
    final var api = new WhatsappAPI(configuration);

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    // Connect to the client
    api.connect();

    // Disconnect the client
    api.disconnect();
  }

  @Test
  public void testDataAccess() {

    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    // Acquire the in memory data manager
    final var dataManager = api.manager(); // Get an instance of WhatsappDataManager

    // Access the in memory data
    final var chats = dataManager.chats(); // Get all the chats in memory
    final var contacts = dataManager.contacts(); // Get all the contacts in memory
    final var number = dataManager.phoneNumber(); // Get your phone number as a jid

    // Assert is it empty
    assertTrue(chats.isEmpty());
    assertTrue(contacts.isEmpty());
    assertTrue(number.isEmpty());

    // Querying in memory data through extended methods

    final String jid = "";
    final String name = "";

    assertTrue(dataManager.findContactByJid(jid).isEmpty());

    assertTrue(dataManager.findContactByName(name).isEmpty());
    assertTrue(dataManager.findContactsByName(name).isEmpty());

    assertTrue(dataManager.findChatByJid(jid).isEmpty());
    assertTrue(dataManager.findChatByName(name).isEmpty());
    assertTrue(dataManager.findChatsByName(name).isEmpty());

    final WhatsappProtobuf.WebMessageInfo messageInfo = WhatsappProtobuf.WebMessageInfo.getDefaultInstance();
    final WhatsappMessage message = new WhatsappMessage(messageInfo);

    assertTrue(dataManager.findChatByMessage(message).isEmpty());

    final WhatsappChat chat = WhatsappChat.builder().build();

    final String msgId = "";
    assertTrue(dataManager.findMessageById(chat, msgId).isEmpty());

    final WhatsappProtobuf.ContextInfo contextInfo = WhatsappProtobuf.ContextInfo.getDefaultInstance();
    assertTrue(dataManager.findQuotedMessageInChatByContext(chat, contextInfo).isEmpty());

    api.disconnect();
  }

  @Test
  public void testSendWhatsappMessageRequest() throws ExecutionException, InterruptedException {

    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    final String recipient = "";
    final String text = "";

    final WhatsappProtobuf.WebMessageInfo messageInfo = WhatsappProtobuf.WebMessageInfo.getDefaultInstance();
    final WhatsappMessage quotedMessage = new WhatsappMessage(messageInfo);

    final var textRequest = WhatsappMessageRequest
        .builder()
        .recipient(recipient) // The recepient of this message
        .text(text) // The text of this message
        .quotedMessage(quotedMessage) // The message to quote
        .forwarded(false) // Whether this message is forwarded or not
        .build(); // Builds an instance of WhatsappMessageRequest

    // Send message returns a completable future
    api.sendMessage(textRequest).join();

    final var quotedTextRequest = WhatsappMessageRequest.ofQuotedText(recipient, text, quotedMessage);

    // Send message returns a completable future
    assertEquals(0,
        api.sendMessage(quotedTextRequest)
            .thenApplyAsync(MessageResponse::status).get());

  }

  @Test
  public void testSendWebMessageInfo() throws ExecutionException, InterruptedException {

    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    final String recipient = "";
    final String text = "";

    var key = WhatsappProtobuf.MessageKey.newBuilder()
        .setFromMe(true)
        .setRemoteJid(recipient)
        .setId(WhatsappUtils.randomId())
        .build();

    var conversation = WhatsappProtobuf.Message.newBuilder()
        .setConversation(text)
        .build();

    var webMessageInfo = WhatsappProtobuf.WebMessageInfo.newBuilder()
        .setMessage(conversation)
        .setKey(key)
        .setMessageTimestamp(Instant.now().getEpochSecond())
        .setStatus(WhatsappProtobuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.PENDING)
        .build();

    // Send message returns a completable future
    assertEquals(0,
        api.sendMessage(webMessageInfo)
            .thenApplyAsync(MessageResponse::status).get());


    final String quotedMessageSenderJid = "";
    final String quotedMessageId = "";
    final String quotedMessageRemoteJid = "";
    boolean forwarded = false;
    var contextInfo = WhatsappProtobuf.ContextInfo.newBuilder()
        .setQuotedMessage(WhatsappProtobuf.Message.getDefaultInstance())
        .setParticipant(quotedMessageSenderJid)
        .setStanzaId(quotedMessageId)
        .setRemoteJid(quotedMessageRemoteJid)
        .setIsForwarded(forwarded)
        .build();

    var extendedTextMessage = WhatsappProtobuf.Message.newBuilder()
        .setExtendedTextMessage(WhatsappProtobuf.ExtendedTextMessage.newBuilder()
            .setText(text)
            .setContextInfo(contextInfo));

    var quotedText = WhatsappProtobuf.WebMessageInfo.newBuilder()
        .setMessage(extendedTextMessage)
        .setKey(key)
        .setMessageTimestamp(Instant.now().getEpochSecond())
        .setStatus(WhatsappProtobuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.PENDING)
        .build();

    assertEquals(0,
        api.sendMessage(quotedText)
            .thenApplyAsync(MessageResponse::status).get());

    api.disconnect();
  }

  @Test
  public void testOnlineStatus() {

    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    @NotNull WhatsappContactStatus presence = WhatsappContactStatus.AVAILABLE;
    api.changePresence(presence).join();

    @NotNull WhatsappChat chat = WhatsappChat.builder().build();
    api.changePresence(presence, chat).join();

    api.disconnect();
  }

  @Test
  public void testLastKnownStatus() {

    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    // Acquire the in memory data manager
    final var dataManager = api.manager(); // Get an instance of WhatsappDataManager

    @NotNull String jid = "";
    var contact = dataManager.findContactByJid(jid);

    assertTrue(contact.map(WhatsappContact::lastKnownPresence).isEmpty());

    api.disconnect();
  }

  @Test
  public void testPresenceSubscription() {
    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    // Acquire the in memory data manager
    final var dataManager = api.manager(); // Get an instance of WhatsappDataManager

    @NotNull String jid = "";
    var contact = dataManager.findContactByJid(jid);

    contact.map(api::subscribeToUserPresence)
        .map(CompletableFuture::join);

    assertTrue(contact
        .flatMap(WhatsappContact::lastKnownPresence)
        .map(WhatsappContactStatus::name)
        .isEmpty());

    api.disconnect();
  }

  @Test
  public void testTextStatus() throws ExecutionException, InterruptedException {

    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    final var dataManager = api.manager(); // Get an instance of WhatsappDataManager

    @NotNull String jid = "";
    dataManager
        .findContactByJid(jid)
        .map(api::queryUserStatus)
        .ifPresent(future -> assertTrue(future.join()
            .status()
            .isPresent()));

    api.disconnect();
  }

  @Test
  public void testFetchPicture() {

    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    @NotNull WhatsappChat chat = WhatsappChat.builder().build();

    assertNotNull(api.queryChatPicture(chat)
        .join()
        .url());

    api.disconnect();
  }

  @Test
  public void testFetchGroupMetaData() {

    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    @NotNull WhatsappChat group = WhatsappChat.builder().build();

    assertNotNull(api.queryChatPicture(group)
        .join());

    api.disconnect();
  }

  @Test
  public void fetchCommonGroupsWithContact() {

    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    @NotNull WhatsappContact contact = WhatsappContact.builder().build();

    assertTrue(api.queryGroupsInCommon(contact)
        .join()
        .groups()
        .isEmpty());

    api.disconnect();
  }

  @Test
  public void testLoadChat() {
    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    @NotNull WhatsappContact contact = WhatsappContact.builder().build();
    assertNotNull(api.queryChat(contact).join()); // Loads the chat associated with the contact

    @NotNull WhatsappContact chatJid = WhatsappContact.builder().build();
    assertNotNull(api.queryChat(chatJid).join());

    assertNotNull(api.queryChat(chatJid)
        .join()
        .data()
        .map(api::loadConversation)
        .map(CompletableFuture::join)
        .map(WhatsappChat::presences));

    api.disconnect();
  }

  @Test
  public void testSearchMessages() {


    // Create an instance of the API
    final var api = new WhatsappAPI();

    // Register your listener
    api.registerListener(new WhatsappTestListener());

    api.connect();

    // Local memory search
    @NotNull WhatsappContact chatJid = WhatsappContact.builder().build();
    assertNotNull(api.queryChat(chatJid)
        .join()
        .data()
        .map(api::loadConversation)
        .map(CompletableFuture::join)
        .map(WhatsappChat::messages));

    // Server search
    @NotNull String searchStr = "";
    int numOfMessages = 10;
    int page = 0;

    api.search(searchStr, numOfMessages, page)
        .join()
        .data()
        .ifPresentOrElse(
            messages -> assertFalse(messages.isEmpty()),
            () -> fail("No mesages found"));

    api.disconnect();
  }


}
