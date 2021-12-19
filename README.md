# WhatsappWeb4j

### What is WhatsappWeb4j

WhatsappWeb4j is a standalone library built to interact with [WhatsappWeb](https://web.whatsapp.com/). This means that no browser, application or
any additional software is necessary to use this library. This library was built for [Java 17](https://openjdk.java.net/projects/jdk/17/). 

### How to install 

#### Maven
Add this dependency to your dependencies in the pom:
```xml
<dependency>
    <groupId>com.github.auties00</groupId>
    <artifactId>whatsappweb4j</artifactId>
    <version>2.2.6</version>
</dependency>
```

#### Gradle
Add this dependency to your build.gradle:
```groovy
implementation 'com.github.auties00:whatsappweb4j:2.2.6'
```

### Examples

If you need some examples to get started, check the [examples' directory](https://github.com/Auties00/WhatsappWeb4j/tree/master/examples) in this project. There are several easy
and documented projects and more will come.

### Javadocs
Javadocs for WhatsappWeb4j are available [here](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/index.html), all contributions are welcomed!

### How to contribute

As of today, no additional configuration is needed to edit this project. I recommend using the latest version of IntelliJ.

1. [Fork this project](https://docs.github.com/en/github/getting-started-with-github/fork-a-repo)
2. [Clone the new repo](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/cloning-a-repository)
3. [Create a new branch](https://docs.github.com/en/desktop/contributing-and-collaborating-using-github-desktop/managing-branches#creating-a-branch)
3. Once you have implemented the new feature, [create a new merge request](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request)

If you are trying to implement a feature that is present on WhatsappWeb's WebClient, for example audio or video calls, consider using [WhatsappWeb4jRequestAnalyzer](https://github.com/Auties00/whatsappweb4j-request-analyzer),
a tool I built for this exact purpose.

### How to configure WhatsappWeb4j
To use this library, start by initializing an instance of WhatsappAPI:
```java
var api = new WhatsappAPI();
```
Alternatively, you can provide a custom [WhatsappConfiguration](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/api/WhatsappConfiguration.html):
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

Now create a [WhatsappListener](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/listener/WhatsappListener.html), remember to implement only the methods that you need:

```java
public class YourAwesomeListener implements WhatsappListener {
   public void onLoggedIn(@NonNull UserInformationResponse info) {
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

    Annotate your listener using [@RegisterListener](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/listener/RegisterListener.html):
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

All the messages, chats and contacts stored in memory can be accessed using the singleton [WhatsappDataManager](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/manager/WhatsappDataManager.html):
```java
var manager = api.manager(); // Get an instance of WhatsappDataManager
var chats = manager.chats(); // Get all the chats in memory
var contacts = manager.contacts(); // Get all the contacts in memory
var number = manager.phoneNumberJid(); // Get your phone number as a jid
```
> **_IMPORTANT:_** When your program first starts up, these fields will be empty. To be notified when they are populated, implement the corresponding method in a WhatsappListener

This class also exposes various methods to query data as explained in the [javadocs](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/manager/WhatsappDataManager.html):
```java
Optional<Contact> findContactByJid(String jid);
Optional<Contact> findContactByName(String name);
Set<Contact> findContactsByName(String name);

Optional<Chat> findChatByJid(String jid);
Optional<Chat> findChatByName(String name);
Set<Chat> findChatsByName(String name);
Optional<Chat> findChatByMessage(MessageInfo message);

Optional<MessageInfo> findMessageById(Chat chat, String id);
``` 

The keys linked to an active session can be accessed using [WhatsappKeysManager](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/manager/WhatsappKeysManager.html).

### Send a message

##### Simple text message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name
api.sendMessage(chat, "Hello my friend :)"); // Send the text message
```

##### Rich text message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name
var message = TextMessage.newTextMessage() // Create a new text message builder
        .text("Check this video out: https://www.youtube.com/watch?v=dQw4w9WgXcQ") // Set the text to "A nice and complex message"
        .canonicalUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ") // Set the url
        .matchedText("https://www.youtube.com/watch?v=dQw4w9WgXcQ") // Set the matched text for the url in the message
        .title("A nice suprise") // Set the title of the url
        .description("Check me out") // Set the description of the url
        .create(); // Create the message
api.sendMessage(chat, message); // Send the rich text message
```

##### Image message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name

// Read the file you want to send as an array of bytes, here are two common examples
var fileMedia = Files.readAllBytes(file.toPath()); // Read a media from a file 
var urlMedia = new URL(url).openStream().readAllBytes(); // Read a media from an url 

var image = ImageMessage.newImageMessage() // Create a new image message builder
        .media(urlMedia) // Set the image of this message
        .caption("A nice image") // Set the caption of this message
        .create(); // Create the message
api.sendMessage(chat, image); // Send the image message
```

##### Audio message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name

// Read the file you want to send as an array of bytes, here are two common examples
var fileMedia = Files.readAllBytes(file.toPath()); // Read a media from a file 
var urlMedia = new URL(url).openStream().readAllBytes(); // Read a media from an url 

var audio = AudioMessage.newAudioMessage() // Create a new audio message builder
        .media(urlMedia) // Set the audio of this message
        .voiceMessage(false) // Set whether this message is a voice message or a standard audio message
        .create(); // Create the message
api.sendMessage(chat, audio); // Send the audio message
```

##### Video message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name

// Read the file you want to send as an array of bytes, here are two common examples
var fileMedia = Files.readAllBytes(file.toPath()); // Read a media from a file 
var urlMedia = new URL(url).openStream().readAllBytes(); // Read a media from an url 

var video = VideoMessage.newVideoMessage() // Create a new video message builder
        .media(urlMedia) // Set the video of this message
        .caption("A nice video") // Set the caption of this message
        .width(100) // Set the width of the video
        .height(100) // Set the height of the video
        .create(); // Create the message
api.sendMessage(chat, video); // Send the video message
```

##### Gif message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name

// Read the file you want to send as an array of bytes, here are two common examples
var fileMedia = Files.readAllBytes(file.toPath()); // Read a media from a file 
var urlMedia = new URL(url).openStream().readAllBytes(); // Read a media from an url 

var gif = VideoMessage.newGifMessage() // Create a new gif message builder
        .media(urlMedia) // Set the gif of this message
        .caption("A nice video") // Set the caption of this message
        .gifAttribution(VideoMessageAttribution.TENOR) // Set the source of the gif
        .create(); // Create the message
api.sendMessage(chat, gif); // Send the gif message
```

> **_IMPORTANT:_** Whatsapp doesn't support conventional gifs. Instead, videos can be played as gifs if particular attributes are set. This is the reason why the gif builder is under the VideoMessage class. Sending a conventional gif will result in an exception if detected or in undefined behaviour.

##### Document message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name

// Read the file you want to send as an array of bytes, here are two common examples
var fileMedia = Files.readAllBytes(file.toPath()); // Read a media from a file 
var urlMedia = new URL(url).openStream().readAllBytes(); // Read a media from an url 

var document = DocumentMessage.newDocumentMessage() // Create a new document message builder
        .media(urlMedia) // Set the document of this message
        .title("A nice pdf") // Set the title of the document
        .fileName("pdf-test.pdf") // Set the name of the document
        .pageCount(1) // Set the number of pages of the document
        .create(); // Create the message
api.sendMessage(chat, document); // Send the docuemnt message
```

##### Location message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name
var location = LocationMessage.newLocationMessage() // Create a new location message
        .caption("Look at this!") // Set the caption of the message, that is the text below the file. Not available if this message is live
        .degreesLatitude(38.9193) // Set the longitude of the location to share
        .degreesLongitude(1183.1389) // Set the latitude of the location to share
        .live(false) // Set whether this location is live or not
        .create(); // Create the message
api.sendMessage(chat, location); // Send the location message
```

##### Live location message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name
var location = LiveLocationMessage.newLiveLocationMessage() // Create a new live location message
        .caption("Look at this!") // Set the caption of the message, that is the text below the file. Not available if this message is live
        .degreesLatitude(38.9193) // Set the longitude of the location to share
        .degreesLongitude(1183.1389) // Set the latitude of the location to share
        .accuracyInMeters(10) // Set the accuracy of the location in meters
        .speedInMps(12) // Set the speed of the device sharing the location in meter per seconds
        .create(); // Create the message
api.sendMessage(chat, location); // Send the location message
```
> **_IMPORTANT:_** Updating the position of a live location message is not supported as of now out of the box. The tools to do so are in the API though so, if you'd like to write a developer friendly method to do so, know that all contributions are welcomed!

##### Group invite message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name
var group = api.findChatByName("Fellow Programmers 1.0").orElseThrow(); // Query a group

var groupCode = api.queryGroupInviteCode(group).get().code(); // Query the invitation code of the group
var groupInvite = GroupInviteMessage.newGroupInviteMessage() // Create a new group invite message
        .caption("Come join my group of fellow programmers") // Set the caption of this message
        .groupName(group.displayName()) // Set the name of the group
        .groupJid(group.jid())) // Set the jid of the group
        .inviteExpiration(ZonedDateTime.now().plusDays(3).toInstant().toEpochMilli()) // Set the expiration of this invite
        .inviteCode(code) // Set the code of the group, this can be obtained also when a contact cannot be added as a member to a group
        .create(); // Create the message
api.sendMessage(chat, groupInvite); // Send the invite message
```

##### Contact message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name
var contactMessage = ContactMessage.newContactMessage()  // Create a new contact message
        .displayName("A nice friend") // Set the display name of the contact
        .vcard(vcard) // Set the vcard(https://en.wikipedia.org/wiki/VCard) of the contact
        .create(); // Create the message
api.sendMessage(chat, groupInvite); // Send the contact message
```

##### Contacts array message
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name
var contactsMessage = ContactsArrayMessage.newContactsArrayMessage()  // Create a new contacts array message
        .displayName("A nice friend") // Set the display name of the first contact that this message contains
        .contacts(contactMessages) // Set a list of contact messages that this message wraps
        .create(); // Create the message
api.sendMessage(chat, contactsMessage); // Send the contacts array message
```

### Advanced message structure

Whatsapp Web defines several types of messages:
1. Standard messages(most common)
    - TextMessage
    - ContactMessage
    - ContactsArrayMessage
    - GroupInviteMessage
    - LocationMessage
    - LiveLocationMessage
    - Media messages
        - ImageMessage
        - AudioMessage
        - DocumentMessage
        - StickerMessage
2. Whatsapp Business messages
    - Payment messages
        - RequestPaymentMessage
        - CancelPaymentRequestMessage
        - DeclinePaymentRequestMessage
        - SendPaymentMessage
    - ProductMessage
    - TemplateButtonReplyMessage
    - TemplateMessage
    - HighlyStructuredMessage
3. Server messages
    - ProtocolMessage
4. Security messages(Signal's Protocol)
    - SenderKeyDistributionMessage
5. Device sent messages:
    - DeviceSentMessage
    - DeviceSyncMessage

All messages implement the [Message](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/protobuf/message/Message.html) interface.
All standard messages and some Whatsapp business messages extend the [ContextualMessage](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/protobuf/message/ContextualMessage.html) class which provides a [ContextInfo](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/protobuf/info/ContextInfo.html) property.
Only ContextualMessages can quote another message or be marked as forwarded. This property also exposes other useful properties for Whatsapp Business, though they are irrelevant for most use cases.
All messages provide an all arguments' constructor, and a builder class.
Messages are wrapped in a [MessageContainer](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/protobuf/message/MessageContainer.html), a container class which can be initialized through a one argument constructor that takes any type of message, or a builder class.
Finally, a MessageContainer is wrapped by a [MessageInfo](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/protobuf/info/MessageInfo.html).
This class provides several properties, though, for most use cases, the container, key and timestamp are enough.
The container property has already been mentioned and explained.
The key property is of type [MessageKey](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/protobuf/message/MessageKey.html) and defines the id of the message, and the chat where it is located. It can be initialized using a single argument constructor which takes said chat as an argument or using a builder class.
The timestamp property indicates the time when the message was sent in seconds since the epoch.
If any of the properties above are not initialized, Whatsapp will refuse to send the message.
At this point the message can be sent using the method sendMessage in [WhatsappAPI](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/api/WhatsappAPI.html).
Here is a handy example:
```java
var chat = api.findChatByName("My Awesome Friend").orElseThrow(); // Query a chat by name
var key = new MessageKey(chat); // Create a message key
var textMessage = new TextMessage("Hello :)"); // Create a text message        
var message = new MessageContainer(textMessage); // Create a message container that wraps the textMessage
var info = new MessageInfo(key, message); // Create a message info that wraps the key and info property
var textResponse = whatsappAPI.sendMessage(info).get(); // Send the message
```
As shown in the [previous section](#send-a-message), crafting a message in this verbose way is not necessary for most use cases.

### Online status

To change your global [ContactStatus](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/protobuf/contact/ContactStatus.html):
``` java
api.changePresence(status);
```

To change your [ContactStatus](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/protobuf/contact/ContactStatus.html) for a specific [Chat](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/protobuf/chat/Chat.html):
``` java
api.changePresence(status, chat);
```

To query the last known status of a [Contact](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/it/auties/whatsapp4j/protobuf/contact/Contact.html):
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


If the chat is already in memory, or you are not sure:
``` java
api.loadChatHistory(chat).get(); // Loads the twenty messages that came chronologically before the oldest one
api.loadChatHistory(chat, message, numOfMessages).get(); // Loads the numOfMessages that came chronologically before the specified message
```

If you want to load all the messages in a chat:
``` java
api.loadEntireChatHistory(chat).get(); // Loads the entire chat in memory, might take several minutes if the chat has thousands of messages
```

### Search messages

To access messages in memory:
``` java
var messages = chat.messages();
```

To search messages in any chat on Whatsapp's servers:
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

##### Query a group's invite code
``` java
var future = api.queryGroupInviteCode(group);  // A future for the request
var response = future.get(); // Wait for the future to complete
```

