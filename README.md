# WhatsappWeb4j 

### What is WhatsappWeb4j

WhatsappWeb4j is a standalone library built to interact with [WhatsappWeb](https://web.whatsapp.com/). 
This means that no browser, application or any additional software is necessary to use this library. 
This library was built for [Java 17](https://openjdk.java.net/projects/jdk/17/) with preview features enabled. 

### Does this library support multi device?

Yes, the master branch now fully supports the multi device feature. 
Considering that support for legacy WhatsappWeb has been dropped by Whatsapp, this library has also dropped support for the latter. 
If, you'd like to use a version that supports the legacy version, use any release before 3.0.

### How to install 

#### Maven
Add this dependency to your dependencies in the pom:
```xml
<dependency>
    <groupId>com.github.auties00</groupId>
    <artifactId>whatsappweb4j</artifactId>
    <version>3.0-RC1</version>
</dependency>
```

#### Gradle

1. Groovy DSL
   ```groovy
   implementation 'com.github.auties00:whatsappweb4j:3.0-RC1'
   ```
   
2. Kotlin DSL
   ```kotlin
   implementation("com.github.auties00:whatsappweb4j:3.0-RC1")
   ```

### Examples

If you need some examples to get started, check the [examples' directory](https://github.com/Auties00/WhatsappWeb4j/tree/master/examples) in this project. 
There are several easy and documented projects and more will come.
Any contribution is welcomed!

### Javadocs
Javadocs for WhatsappWeb4j are available [here](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/index.html).
Any contribution is welcomed!

### How to contribute

As of today, no additional configuration or artifact building is needed to edit this project. 
I recommend using the latest version of IntelliJ, though any other IDE should work.
If you are not familiar with git, follow these short tutorials in order:

1. [Fork this project](https://docs.github.com/en/github/getting-started-with-github/fork-a-repo)
2. [Clone the new repo](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/cloning-a-repository)
3. [Create a new branch](https://docs.github.com/en/desktop/contributing-and-collaborating-using-github-desktop/managing-branches#creating-a-branch)
4. Once you have implemented the new feature, [create a new merge request](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request)

If you are trying to implement a feature that is present on WhatsappWeb's WebClient, for example audio or video calls, 
consider using [WhatsappWeb4jRequestAnalyzer](https://github.com/Auties00/whatsappweb4j-request-analyzer), a tool I built for this exact purpose.

### What is a listener and how to register it

Listeners are crucial to handle events related to Whatsapp. 

To create a new listener, declare a class or record that implements the WhatsappListener interface:

```java
import it.auties.whatsapp.api.WhatsappListener;

public class MyListener implements WhatsappListener {

}
```

Remember to manually register this listener:
```java
api.registerListener(new MyListener());
```
Or to register it automatically:

```java
import it.auties.whatsapp.api.RegisterListener;
import it.auties.whatsapp.api.WhatsappListener;

@RegisterListener // Automatically registers this listener
public class MyListener implements WhatsappListener {

}
```
If you need the instance of Whatsapp that registered this listener inside it's declaration, it can be injected automically:

```java
import it.auties.whatsapp.api.RegisterListener;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappListener;

@RegisterListener // Automatically registers this listener
public record MyListener(Whatsapp api) implements WhatsappListener {

}
```
> **_IMPORTANT:_** Only concrete classes that provide a no arguments constructor a single parameter constructor that takes a Whatsapp instance can be registered automatically

For a complete list of all events that can be implemented, check the documentation.


### How to create a connection

The most important class of this API is Whatsapp, an interface between your application and WhatsappWeb's socket.

To create a new Whatsapp instance associated with a new connection, start with:
```java
var api = Whatsapp.newConnection();
```

If the connection needs to be configured further, a configuration can be passed to the newConnection constructor like so:
```java
// Implement only the options that you need!
var configuration = WhatsappOptions.newOptions()
        .id(1838138) // A random unique ID associated with the session
        .version(new Version(2, 2212, 7)) // The version of this client, make sure it's not too outdated 
        .url("wss://web.whatsapp.com/ws") // The URL of WhatsappWeb's Socket
        .serialization(true) // Whether this session should be serialized
        .historyLength(HistoryLength.THREE_MONTHS) // The amount of chat history that Whatsapp sends to the client on the first scan
        .serializationStrategy() // An implementation, built in or custom(for example in a database), of the serialization mechanism
        .create(); // Creates an instance of WhatsappOptions
var api = Whatsapp.newConnection(options);
```

If you have already created a connection, you might want to use that one, instead of creating a new one.
Here is an example:
```java
var api = Whatsapp.lastConnection();
```
> **_IMPORTANT:_**  If no previous session exists, a new one will be created silently

You can also connect to the first session that you have ever created like so:
```java
var api = Whatsapp.firstConnection();
```
> **_IMPORTANT:_**  If no previous session exists, a new one will be created silently

A list of all serialized connections can be listed:
```java
var connections = Whatsapp.listConnections();
```
Or streamed:
```java
var connections = Whatsapp.streamConnection();
```

Once you have created a new connection, you probably want to open it and wait until the operation succeeds:
```java
api.connect().get();
```
> **_IMPORTANT:_** The connect method returns a CompletableFuture: remember to handle this async construct

Once you are connected, your application will stop if no work is going on the main thread as this library is completely asynchronous.
You can block the main thread until the connection is closed like this:
```java
api.await();
```
> **_IMPORTANT:_** Never call await inside a Whatsapp listener: the thread that calls this function is blocked until the connection ends

### How to query chats, contacts, messages and status

All the chats, contacts and messages are sent progressively when the qr code is first scanned unlike in the legacy version.

Start by accessing the in memory store:
```java
var store = api.store();
```
> **_IMPORTANT:_** When your program first starts up, these fields will be empty. For each type of data, an event is fired and listenable using a WhatsappListener

You can access all the chats that are in memory:
```java
var chats = store.chats();
```
Or the contacts:
```java
var contacts = store.contacts();
```
Or even the status broadcasts:
```java
var status = store.status();
```
Data can also be easily queried by using these methods:
```java
// Query a chat by its jid
var chat = store.findChatByJid(jid);

// Query a chat by its name
ar chat = store.findChatByName(name); // If multiple chats match this name, the first one will be selected with no guarantees.

// Query all chats that match a name
var chats = store.findChatsByName(name);

// Query a chat by a message inside it
var chat = store.findChatByMessage(message);

// Query a contact by its jid
var contact = store.store.findContactByJid(jid);

// Query a contact by its name
var contact = store.findContactByName(name);  // If multiple contacts match this name, the first one will be selected with no guarantees.

// Query all contacts that match a name
var contacts = store.findContactsByName(name);

// Query a message by its id and chat
var message = store.findMessageById(Chat chat, String id);
``` 
Further details are available in the documentation.


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

### How to send messages

To send a message, start by finding the chat where the message should be sent. Here is an example:
```java
var chat = api.store()
        .findChatByName("My Awesome Friend")
        .orElseThrow(() -> new NoSuchElementException("Hey, you don't exist"));
``` 

If you want to send a simple text message, just use:
```java
api.sendMessage(chat, "This is a text message!");
```

If you'd like to quote a previous message:
```java
api.sendMessage(chat, "This is a text message!", anotherMessage);
```

Add a bit of spice to your simple text message by using a TextMessage. 
They are particularly useful when the message contains a hyperlink or styling.
Here is an example for the first case:
```java
var message = TextMessage.newTextMessage() // Create a new text message
        .text("Check this video out: https://www.youtube.com/watch?v=dQw4w9WgXcQ") // Set the text of the message
        .canonicalUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ") // Set the url of the message
        .matchedText("https://www.youtube.com/watch?v=dQw4w9WgXcQ") // Set the matched text for the url in the message
        .title("A nice suprise") // Set the title of the url
        .description("Check me out") // Set the description of the url
        .create(); // Create the message
api.sendMessage(chat, message); 
```
For a list of all fields check the documentation.

Media messages are also supported!
> **_IMPORTANT:_** All media messages need to link back to the id of their message store. If you miss this field in the builder, an exception will be thrown

To send a media, start by reading the content inside a byte array.
You might want to read it from a file:
```java
var media = Files.readAllBytes(Path.of("somewhere"));
```

Or from a URL:
```java
var media = new URL(url).openStream().readAllBytes();
```

For a list of all fields for each media class, check the documentation.
If your media is an image, you can use:
 ```java
var image = ImageMessage.newImageMessage() // Create a new image message builder
        .storeId(api.store().id()) // All media messages need a reference to their store
        .media(media) // Set the image of this message
        .caption("A nice image") // Set the caption of this message
        .create(); // Create the message
api.sendMessage(chat, image);
```

If it's an audio message or a voice message, use instead:
   ```java
var audio = AudioMessage.newAudioMessage() // Create a new audio message builder
        .storeId(api.store().id()) // All media messages need a reference to their store
        .media(urlMedia) // Set the audio of this message
        .voiceMessage(false) // Set whether this message is a voice message or a standard audio message
        .create(); // Create the message
api.sendMessage(chat, audio); 
```

It's a video? No problem:
```java
var video = VideoMessage.newVideoMessage() // Create a new video message builder
        .storeId(api.store().id()) // All media messages need a reference to their store
        .media(urlMedia) // Set the video of this message
        .caption("A nice video") // Set the caption of this message
        .width(100) // Set the width of the video
        .height(100) // Set the height of the video
        .create(); // Create the message
api.sendMessage(chat, video); 
```

Videos can be sent also as GIFs:
```java
var gif = VideoMessage.newGifMessage() // Create a new gif message builder
        .storeId(api.store().id()) // All media messages need a reference to their store
        .media(urlMedia) // Set the gif of this message
        .caption("A nice video") // Set the caption of this message
        .gifAttribution(VideoMessageAttribution.TENOR) // Set the source of the gif
        .create(); // Create the message
api.sendMessage(chat, gif); 
```

> **_IMPORTANT:_** Whatsapp doesn't support conventional gifs. 
> Instead, videos can be played as gifs if particular attributes are set. 
> This is the reason why the gif builder is under the VideoMessage class. 
> Sending a conventional gif will result in an exception if detected or in undefined behaviour.


Documents can be sent as easily:
```java
var document = DocumentMessage.newDocumentMessage() // Create a new document message builder
        .storeId(api.store().id()) // All media messages need a reference to their store
        .media(urlMedia) // Set the document of this message
        .title("A nice pdf") // Set the title of the document
        .fileName("pdf-test.pdf") // Set the name of the document
        .pageCount(1) // Set the number of pages of the document
        .create(); // Create the message
api.sendMessage(chat, document);
```

Location messages are also supported:
```java
var location = LocationMessage.newLocationMessage() // Create a new location message
        .caption("Look at this!") // Set the caption of the message, that is the text below the file
        .latitude(38.9193) // Set the longitude of the location to share
        .longitude(1183.1389) // Set the latitude of the location to share
        .create(); // Create the message
api.sendMessage(chat, location);
```

Or even live location messages:
```java
var location = LiveLocationMessage.newLiveLocationMessage() // Create a new live location message
        .caption("Look at this!") // Set the caption of the message, that is the text below the file. Not available if this message is live
        .latitude(38.9193) // Set the longitude of the location to share
        .longitude(1183.1389) // Set the latitude of the location to share
        .accuracy(10) // Set the accuracy of the location in meters
        .speed(12) // Set the speed of the device sharing the location in meter per seconds
        .create(); // Create the message
api.sendMessage(chat, location);
```
> **_IMPORTANT:_** Updating the position of a live location message is not supported as of now out of the box.
> The tools to do so, though, are in the API.

Need to invite a friend inside a group?
Start by finding the group:
```java
var group = api.store()
        .findChatByName("Programmers")
        .filter(Chat::isGroup)
        .orElseThrow(() -> new NoSuchElementException("Hey, you don't exist"));
``` 

Then query the invite code:
```java
var inviteCode = api.queryInviteCode(group).get();
```

Finally send your message:
```java
var groupInvite = GroupInviteMessage.newGroupInviteMessage() // Create a new group invite message
        .caption("Come join my group of fellow programmers") // Set the caption of this message
        .groupName(group.name()) // Set the name of the group
        .groupJid(group.jid())) // Set the jid of the group
        .inviteExpiration(ZonedDateTime.now().plusDays(3).toInstant().toEpochMilli()) // Set the expiration of this invite
        .inviteCode(inviteCode) // Set the code of the group
        .create(); // Create the message
api.sendMessage(chat, groupInvite); 
```

Contacts can also be sent individually through messages:
```java
var contactMessage = ContactMessage.newContactMessage()  // Create a new contact message
        .name("A nice friend") // Set the display name of the contact
        .vcard(vcard) // Set the vcard(https://en.wikipedia.org/wiki/VCard) of the contact
        .create(); // Create the message
api.sendMessage(chat, contactMessage);
```

Or in groups:
```java
var contactsMessage = ContactsArrayMessage.newContactsArrayMessage()  // Create a new contacts array message
        .name("A nice friend") // Set the display name of the first contact that this message contains
        .contacts(List.of(jack, lucy, jeff)) // Set a list of contact messages that this message wraps
        .create(); // Create the message
api.sendMessage(chat, contactsMessage);
```

### How to change your status

To change the status of the client:
``` java
api.changePresence(true); // online
api.changePresence(false); // offline
```

If you want to change the status of your companion, start by choosing the right presence:
These are the allowed values:
- AVAILABLE
- UNAVAILABLE
- COMPOSING
- RECORDING
- PAUSED

Then, execute this method:
``` java
api.changePresence(chat, presence);
```

> **_IMPORTANT:_** The changePresence method returns a CompletableFuture: remember to handle this async construct if needed


### How to query the last known presence for a contact
To query the last known status of a Contact, use the following snippet:
``` java
var lastKnownPresenceOptional = contact.lastKnownPresence();
```
If the returned value is an empty Optional, the last status of the contact is unknown.

Whatsapp starts sending updates regarding the presence of a contact only when:
- A message was recently exchanged between you and said contact
- A new message arrives from said contact
- You send a message to said contact

To force Whatsapp to send these updates use:
``` java
api.subscribeToUserPresence(contact);
```

Then, after the subscribeToUserPresence's future is completed, query again the presence of that contact. 

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
var messages = response.toByteArray().orElseThrow(); // The requested messages
```

To search messages for a specific chat on Whatsapp's servers:
``` java
var future = api.search(stringToSearch, chat, numOfMessages, page);  // A future for the request
var response = future.get(); // Wait for the future to complete
var messages = response.toByteArray().orElseThrow(); // The requested messages
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

##### Unarchive a chat
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

