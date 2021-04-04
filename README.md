# WhatsappWeb4j

### What is WhatsappWeb4j

WhatsappWeb4j is a standalone library built to interact with [WhatsappWeb](https://web.whatsapp.com/). This means that no browser, application or
any additional software is necessary to use this library. This library was built for [Java 16](https://openjdk.java.net/projects/jdk/16/) and [JakartaEE 9](https://jakarta.ee/release/9/). 
Support for Java 11, the latest LTS as of this date, will come soon. Any help to this library is welcomed as long as the coding style of the project is respected. 

### How to install 

#### Maven
Add this dependency to your dependencies in the pom:
```xml
<dependency>
    <groupId>com.github.auties00</groupId>
    <artifactId>whatsappweb4j</artifactId>
    <version>1.2</version>
</dependency>
```

#### Gradle
Add this dependency to your build.gradle:
```groovy
implementation 'com.github.auties00:whatsappweb4j:1.2'
```

### Javadocs
Javadocs for WhatsappWeb4j are available [here](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/index.html), all contributions are welcomed!

### How to configure WhatsappWeb4j
To use this library, start by initializing an instance of WhatsappAPI:
```java
var api = new WhatsappAPI();
```
Alternatively, you can provide a custom [WhatsappConfiguration](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/api/WhatsappConfiguration.html):
```java
var configuration = WhatsappConfiguration.builder()
        .whatsappUrl("wss://web.whatsapp.com/ws") // WhatsappWeb's WebSocket URL
        .requestTag("requestTag") // The tag used for requests made to WhatsappWeb's WebSocket
        .description("Whatsapp4j") // The description provided to Whatsapp during the authentication process
        .shortDescription("W4J") // An acronym for the description
        .reconnectWhenDisconnected((reason) -> true) // Determines whether the connection should be reclaimed
        .async(true) // Determines whether requests sent to whatsapp should be asyncronous or not
        .build(); // Builds an instance of WhatsappConfiguration

var api = new WhatsappAPI(configuration);
```

Now create a [WhatsappListener](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/listener/WhatsappListener.html), remember to implement only the methods that you need:
```java
public class YourAwesomeListener implements WhatsappListener {
    public void onLoggedIn(UserInformationResponse info, boolean firstLogin) {
       System.out.println("Connected :)");
    }
    
    public void onDisconnected() {
       System.out.println("Disconnected :(");
    }
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
var number = api.phoneNumberJid(); // Get your phone number as a jid
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
### Create and send a message

##### Create a Text Message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name
        
var text = WhatsappTextMessage.newTextMessage(chat, "Hello my friend :)"); // Create a new text message
var quotedText = WhatsappTextMessage.newTextMessage(chat, "Hello my friend ;)", anotherMessage); // Create a new text message that quotes another message
        
var textWithBuilder = WhatsappTextMessage.newTextMessage() // Create a new WhatsappTextMessageBuilder
        .chat(chat) // Set the chat for this message
        .text(text) // Set the text for this message
        .forwarded(true) // Set whether this message is forwarded or not
        .quotedMessage(someMessage) // Set the message that this message quotes
        .create();
```

##### Create a Media Message
```java
// Read the file you want to send as an array of bytes, here are two common examples
var fileMedia = Files.readAllBytes(file.toPath());
var urlMedia = url.openStream().readAllBytes();

var media = WhatsappMediaMessage.newMediaMessage() // Create a new media message
        .caption("Look at this!") // Set the caption of the message, that is the text below the file. Only available for images and videos
        .media(file) // Set the media as an array of bytes
        .type(WhatsappMediaMessageType.IMAGE) // Set the type of media you want to send
        .create();
```

##### Create a Location Message
```java
var location = WhatsappLocationMessage.newLocationMessage() // Create a new location message
        .caption("Look at this!") // Set the caption of the message, that is the text below the file. Not available if this message is live
        .coordinates(new WhatsappCoordinates(138.9193, 1183.1389, 10)) // Set the coordinates of the location to share
        .accuracy(10) // Set the accuracy in meters of the coordinates to share
        .live(false) // Set whether this message is live or not
        .speed(19) // Set the speed of the device in meters per second
        .thumbnail(thumbnail) // Set the thumbnail of this message
        .create();
```

##### Create a Group Invite Message
```java
var invite = WhatsappGroupInviteMessage.newGroupInviteMessage()
        .caption("Come join my group of fellow programmers") // Set the caption of this message
        .name("Fellow Programmers 1.0") // Set the name of the group
        .thumbnail(thumbnail) // Set the thumbnail of the group
        .expiration(ZonedDateTime.now().plusDays(90)) // Set the date of expiration for this invite
        .jid("jid@g.us") // Set the jid of the group
        .code("1931130") // Set the code of the group
        .create();
```

##### Create a Contact(s) Message
```java
var contacts = WhatsappContactMessage.newContactMessage()
        .sharedContacts(List.of(contactVCard, anotherVCard))
        .create();
```

##### Create a raw message
If the options above don't satisfy your needs, open an issue and request the feature you need. In the meanwhile though, you can use you can create your own [WebMessageInfo](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/it/auties/whatsapp4j/model/WhatsappProtobuf.WebMessageInfo.html), the raw Protobuf object for a message, even though it's not recommended as it's not very developer friendly.
Here is an example on how to create a raw text message:
```java
var key = WhatsappProtobuf.MessageKey.newBuilder()
        .setFromMe(true)
        .setRemoteJid(recipient)
        .setId(WhatsappUtils.randomId())
        .build();

var conversation = WhatsappProtobuf.Message.newBuilder()
        .setConversation(text)
        .build();

var text = WhatsappProtobuf.WebMessageInfo.newBuilder()
        .setMessage(conversation)
        .setKey(key)
        .setMessageTimestamp(Instant.now().getEpochSecond())
        .setStatus(WhatsappProtobuf.WebMessageInfo.WebMessageInfoStatus.PENDING)
        .build();

var context = WhatsappProtobuf.ContextInfo.newBuilder()
        .setQuotedMessage(quotedMessage)
        .setParticipant(quotedMessageSenderJid)
        .setStanzaId(quotedMessageId)
        .setRemoteJid(quotedMessageRemoteJid)
        .setIsForwarded(forwarded)
        .build();

var extendedTextMessage = WhatsappProtobuf.Message.newBuilder()
        .setExtendedTextMessage(WhatsappProtobuf.ExtendedTextMessage.newBuilder()
            .setText(text)
            .setContextInfo(context)
            .build())
        .build();

var quotedText = WhatsappProtobuf.WebMessageInfo.newBuilder()
        .setMessage(extendedTextMessage)
        .setKey(key)
        .setMessageTimestamp(Instant.now().getEpochSecond())
        .setStatus(WhatsappProtobuf.WebMessageInfo.WebMessageInfoStatus.PENDING)
        .build();
```

##### Send a message
```java
api.sendMessage(message);
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

## How to contribute

The current release of lombok does not work with java16 which is required to
compile the projects code.

### Building and installing Lombok

This project can be built quite easily, the only tricky part is that the delombok maven plugin needs to point to lombok 1.18.20 instead of 1.18.18. To fix this:

1. Execute these commands:
    ```
    cd ${HOME}/.m2/repository/org/projectlombok/lombok-maven/1.18.18.0/
    vi lombok-maven-1.18.18.0.pom
    ```
2. Replace the lombok.version property so that it reads: `<lombok.version>1.18.20</lombok.version>`

You can now run `mvn clean install` to build and test your version of the library.
