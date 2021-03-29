# WhatsappWeb4j

### What is WhatsappWeb4j

WhatsappWeb##### is a standalone library built to interact with [WhatsappWeb](https://web.whatsapp.com/). This means that no browser, application or
any additional software is necessary to use this library. This library was built for [Java 15](https://openjdk.java.net/projects/jdk/15/) and [JakartaEE 9](https://jakarta.ee/release/9/). 
Support for Java 1##### the latest LTS as of this date, will come soon. Any help to this library is welcomed as long as the coding style of the project is respected. 

### How to install 

#### Maven
Add this dependency to your dependencies in the pom:
```xml
<dependency>
    <groupId>com.github.auties00</groupId>
    <artifactId>whatsappweb4j</artifactId>
    <version>1.1</version>
</dependency>
```

#### Gradle
Add this dependency to your build.gradle:
```groovy
implementation 'com.github.auties00:whatsappweb4j:1.1'
```

### Javadocs
Javadocs for WhatsappWeb##### are available [here](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/index.html), all contributions are welcomed!

### How to configure WhatsappWeb4j
To use this library, start by initializing an instance of WhatsappAPI:
```java
var api = new WhatsappAPI();
```
Alternatively, you can provide a custom [WhatsappConfiguration](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/api/WhatsappConfiguration.html):
```java
var configuration = WhatsappConfiguration.builder()
        .whatsappUrl("wss://web.whatsapp.com/ws") // WhatsappWeb's WebSocket URL
        .whatsappTag("W4J") // The tag used for log in requests
        .requestTag("requestTag") // The tag used for post log in requests
        .description("Whatsapp4j") // The description provided to Whatsapp during the authentication process
        .shortDescription("W4J") // An acronym for the description
        .reconnectWhenDisconnected((reason) -> true) // Determines whether the connection should be reclaimed
        .async(true) // Determines whether requests sent to whatsapp should be asyncronous or not
        .build(); // Builds an instance of WhatsappConfiguration

var api = new WhatsappAPI(configuration);
```

Now create a [WhatsappListener](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/listener/WhatsappListener.html), rember to implement only the methods that you need:
```java
public class YourAwesomeListener implements WhatsappListener {
    public void onLoggedIn(UserInformationResponse info, boolean firstLogin) { }
    
    public void onDisconnected() { }

    public void onInformationUpdate(UserInformationResponse info) { }

    public void onListResponse(JsonListResponse response) { }

    public void onContactsReceived() { }

    public void onContactUpdate(WhatsappContact contact) { }

    public void onContactReceived(WhatsappContact contact) { }

    public void onContactPresenceUpdate(WhatsappChat chat, WhatsappContact contact) { }

    public void onChatsReceived() { }

    public void onChatReceived(WhatsappChat chat) { }

    public void onChatArchived(WhatsappChat chat) { }

    public void onChatUnarchived(WhatsappChat chat) { }

    public void onChatMuteChange(WhatsappChat chat) { }

    public void onChatReadStatusChange(WhatsappChat chat) { }

    public void onChatEphemeralStatusChange(WhatsappChat chat) { }

    public void onNewMessageReceived(WhatsappChat chat, WhatsappMessage message) { }

    public void onMessageReadStatusUpdate(WhatsappChat chat, WhatsappContact contact, WhatsappMessage message) { }

    public void onMessageUpdate(WhatsappChat chat, WhatsappMessage message) { }

    public void onMessageDeleted(WhatsappChat chat, WhatsappMessage message, boolean everyone) { }

    public void onMessageStarred(WhatsappChat chat, WhatsappMessage message) { }

    public void onMessageUnstarred(WhatsappChat chat, WhatsappMessage message) { }

    public void onMessageGlobalReadStatusUpdate(WhatsappChat chat, WhatsappMessage message) { }

    public void onBlocklistUpdate(BlocklistResponse blocklist) { }

    public void onPropsUpdate(PropsResponse props) { }

    public void onPhoneBatteryStatusUpdate(PhoneBatteryResponse battery) { }
}
```

There are two ways to register listeners:
1.  Manually

    ```java
    api.registerListener(new YourAwesomeListener());
    ```

2. Automatically
    > **_IMPORTANT:_**  Only listeners that provide a no arguments' constructor can be discovered automatically

    Annotate your listener using [@RegisterListener](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/listener/RegisterListener.html):
    ```java
    import it.auties.whatsapp4j.listener.RegisterListener;
    import it.auties.whatsapp4j.listener.WhatsappListener;

    @RegisterListener
    public class YourAwesomeListener implements WhatsappListener { }
    ```

    then enable auto-detection:
    ```java
    api.autodetectListeners();
    ```

Now open a connection with WhatsappWeb:
```java
api.connect();
```

When your program is done, disconnect from WhatsappWeb:
```java
api.disconnect();
```

Or logout:
```java
api.logout();
```
### In memory data

All the messages, chats and contacts stored in memory can be accessed using the singleton [WhatsappDataManager](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/manager/WhatsappDataManager.html):
```java
var manager = api.manager(); // Get an instance of WhatsappDataManager
var chats = api.chats(); // Get all the chats in memory
var contacts = api.contacts(); // Get all the contacts in memory
var number = api.phoneNumber(); // Get your phone number as a jid
```
> **_IMPORTANT:_** When your program first starts up, these fields will be empty. To be notified when they are populated, implement the corresponding method in a WhatsappListener

This class also exposes various methods to query data as explained in the [javadocs](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/manager/WhatsappDataManager.html):
```java
Optional<WhatsappContact> findContactByJid(String jid);
Optional<WhatsappContact> findContactByName(String name);
Set<WhatsappContact> findContactsByName(String name);

Optional<WhatsappChat> findChatByJid(String jid);
Optional<WhatsappChat> findChatByName(String name);
Set<WhatsappChat> findChatsByName(String name);
Optional<WhatsappChat> findChatByMessage(WhatsappMessage message);

Optional<WhatsappMessage> findMessageById(WhatsappChat chat, String id);
Optional<WhatsappMessage> findQuotedMessageInChatByContext(WhatsappChat chat, ContextInfo context);        
```

The keys linked to an active session can be accessed using [WhatsappKeysManager](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/manager/WhatsappKeysManager.html).
### Sending messages

> **_IMPORTANT:_** Support for non text messages is not currently available, though, it's planned

Create an instance of [WhatsappMessageRequest](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/api/WhatsappMessageRequest.html):
```java
var textBuilder = WhatsappMessageRequest
        .builder()
        .recipient(recipient) // The receipent of this message
        .text(text) // The text of this message
        .quotedMessage(quotedMessage) // The message to quote
        .forwarded(false) // Whether this message is forwarded or not
        .build(); // Builds an instance of WhatsappMessageRequest

var text = WhatsappMessageRequest.ofText(recipient, text);
var quotedText = WhatsappMessageRequest.ofQuotedText(recipient, text, quotedMessage);
```

Alternatively, you can use the [WebMessageInfo](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/model/WhatsappProtobuf.WebMessageInfo.html), the raw Protobuf object, even though it's not recommended as it's not very developer friendly:
```java
var key = WhatsappProtobuf.MessageKey.newBuilder()
        .setFromMe(true)
        .setRemoteJid(recipient)
        .setId(WhatsappUtils.randomId())
        .build();

var conversation = WhatsappProtobuf.Message.newBuilder().setConversation(text);

var text = WhatsappProtobuf.WebMessageInfo.newBuilder()
        .setMessage(conversation)
        .setKey(key)
        .setMessageTimestamp(Instant.now().getEpochSecond())
        .setStatus(WhatsappProtobuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.PENDING)
        .build();

var context = WhatsappProtobuf.ContextInfo.newBuilder()
        .setQuotedMessage(quotedMessage)
        .setParticipant(quotedMessageSenderJid)
        .setStanzaId(quotedMessageId)
        .setRemoteJid(quotedMessageRemoteJid)
        .setIsForwarded(forwarded)
        .build();

var extendedTextMessage = WhatsappProtobuf.Message.newBuilder()
        .setExtendedTextMessage(WhatsappProtobuf.ExtendedTextMessage.newBuilder().setText(text).setContextInfo(context));

var quotedText = WhatsappProtobuf.WebMessageInfo.newBuilder()
        .setMessage(extendedTextMessage)
        .setKey(key)
        .setMessageTimestamp(Instant.now().getEpochSecond())
        .setStatus(WhatsappProtobuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.PENDING)
        .build();
```

### Online status

To change your global [WhatsappContactStatus](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/model/WhatsappContactStatus.html):
``` java
api.changePresence(status);
```

To change your [WhatsappContactStatus](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/model/WhatsappContactStatus.html) for a specific [WhatsappChat](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/model/WhatsappChat.html):
``` java
api.changePresence(status, chat);
```

To query the last known status of a [WhatsappContact](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/model/WhatsappContact.html)::
``` java
var lastKnownPresenceOptional = contact.lastKnownPresence();
```
If the returned value is an empty Optional, the last status of the contact is unknown.
As a matter of fact, Whatsapp sends updates regarding the presence of a contact only when:

- A message was recently exchanged between you and said contact
- A new message arrives from said contact
- You send a message to said contact

To force Whatsapp to send these updates use:
``` java
api.subscribeToUserPresence(contact);
```

Then, after the subscribeToUserPresence's future is completed, query again the presence of said contact. 

### Query data about a group, or a contact

##### Text status
   ``` java
   var statusFuture = api.queryUserStatus(contact); // A completable future
   var statusResponse = statusFuture.get(); // Wait for the future to complete
   var textStatus = statusResponse.status().orElse("No status found"); // The contact's status
   ```
   
##### Profile picture or chat picture
   ``` java
   var pictureFuture = api.queryChatPicture(chat); // A completable future
   var pictureResponse = pictureFuture.get(); // Wait for the future to complete
   var pictureUrl = pictureResponse.url(); // The picture for this chat
   ```
   
##### Group's Metadata
   ``` java
   var metadataFuture = api.queryChatPicture(group); // A completable future
   var metadata = metadataFuture.get(); // The group's metadata
   ```

##### Groups in common with a contact
   ``` java
   var groupsFuture = api.queryGroupsInCommon(contact); // A completable future
   var groupsResponse = metadataFuture.get(); // Wait for the future to complete
   var groups = groupsResponse.groups(); // A list of common groups
   ```
   
### Load a chat

To query a chat that is not in memory:
``` java
var contactChat = api.queryChat(contact); // Loads the chat assiosiated with the contact
var jidChat = api.queryChat(chatJid); // Loads a chat assiosiated with a jid
```
> **_IMPORTANT:_**  This method does not save the queried chat in memory


If the chat is already in memory, to load more messages:
``` java
api.loadConversation(chat); // Loads the twenty messages that came chronologically before the oldest one
api.loadConversation(chat, numOfMessages, message); // Loads the numOfMessages that came chronologically before the specified message
```

### Search messages

To access messages in memory:
``` java
var messages = chat.messages();
```

To search messages globally on Whatsapp's servers:
``` java
var future = api.search(stringToSearch, numOfMessages, page);  // A future for the request
var response = future.get(); // Wait for the future to complete
var messages = response.data().orElseThrow(); // The requested messages
```

To search messages for a specific chat on Whatsapp's servers:
``` java
var future = api.search(stringToSearch, chat, numOfMessages, page);  // A future for the request
var response = future.get(); // Wait for the future to complete
var messages = response.data().orElseThrow(); // The requested messages
```

### Miscellaneous chat related methods

##### Mute a chat
``` java
var future = api.mute(chat);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

##### Unmute a chat
``` java
var future = api.mute(chat);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

##### Archive a chat
``` java
var future = api.archive(chat);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

##### Unrchive a chat
``` java
var future = api.unarchive(chat);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

##### Enable ephemeral messages in a chat
``` java
var future = api.enableEphemeralMessages(chat);  // A future for the request
var response = future.get(); // Wait for the future to complete
```   

##### Mark a chat as read
``` java
var future = api.markAsRead(chat);  // A future for the request
var response = future.get(); // Wait for the future to complete
```   

##### Mark a chat as unread
``` java
var future = api.markAsUnread(chat);  // A future for the request
var response = future.get(); // Wait for the future to complete
```   

##### Pin a chat
``` java
var future = api.pin(chat);  // A future for the request
var response = future.get(); // Wait for the future to complete
``` 

##### Unpin a chat
``` java
var future = api.unpin(chat);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

### Execute an action on contact for a group

##### Add a contact to a group
``` java
var future = api.add(group, contact);  // A future for the request
var response = future.get(); // Wait for the future to complete
// A list of modifications made by the request
// Each entry contains the jid of the affected contact and the status of said moification  
var success = response.modifications();
```

##### Remove a contact from a group
``` java
var future = api.remove(group, contact);  // A future for the request
var response = future.get(); // Wait for the future to complete
// A list of modifications made by the request
// Each entry contains the jid of the affected contact and the status of said moification  
var success = response.modifications();
```

##### Promote a contact to admin in a group
``` java
var future = api.promote(group, contact);  // A future for the request
var response = future.get(); // Wait for the future to complete
// A list of modifications made by the request
// Each entry contains the jid of the affected contact and the status of said moification  
var success = response.modifications();
```

##### Demote a contact to user in a group
``` java
var future = api.demote(group, contact);  // A future for the request
var response = future.get(); // Wait for the future to complete
// A list of modifications made by the request
// Each entry contains the jid of the affected contact and the status of said moification  
var success = response.modifications();
```

### Change the metadata/settings of a group

##### Change the group's name/subject
``` java
var future = api.changeGroupName(group, newName);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

##### Change the group's description
``` java
var future = api.changeGroupDescription(group, newDescription);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

##### Change who can send messages in the group
``` java
var future = api.changeWhoCanSendMessagesInGroup(group, policy);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

##### Change who can edit the metadata/settings in the group
``` java
var future = api.changeWhoCanEditGroupInfo(group, policy);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

##### Change the icon/picture of a group

> **_IMPORTANT:_**  This method is in the API marked as Beta but is not yet implemented

##### Remove the icon/picture of a group 
``` java
var future = api.removeGroupPicture(group);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

### Miscellaneous group related methods
##### Create a group
``` java
var future = api.createGroup(group, friend, friend##### friend2);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

##### Leave a group
``` java
var future = api.leave(group);  // A future for the request
var response = future.get(); // Wait for the future to complete
```
