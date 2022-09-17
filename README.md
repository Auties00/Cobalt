# WhatsappWeb4j

### What is WhatsappWeb4j

WhatsappWeb4j is a standalone library built to interact with [WhatsappWeb](https://web.whatsapp.com/).
This means that no browser, application or any additional software is necessary to use this library.
This library was built for [Java 17](https://openjdk.java.net/projects/jdk/17/), the latest LTS, with preview features
enabled.

### Does this library support multi device?

Yes, the master branch now fully supports the multi device feature.
Considering that support for legacy WhatsappWeb has been dropped by Whatsapp, this library has also dropped support for
the latter.
If, for whatever reason, you'd like to use a version that supports the legacy version, use any release before 3.0.

### How to install

#### Maven

```xml
<dependency>
    <groupId>com.github.auties00</groupId>
    <artifactId>whatsappweb4j</artifactId>
    <version>3.0-RC20</version>
</dependency>
```

#### Gradle

1. Groovy DSL
   ```groovy
   implementation 'com.github.auties00:whatsappweb4j:3.0-RC20'
   ```

2. Kotlin DSL
   ```kotlin
   implementation("com.github.auties00:whatsappweb4j:3.0-RC20")
   ```

### Examples

If you need some examples to get started, check
the [examples' directory](https://github.com/Auties00/WhatsappWeb4j/tree/master/examples) in this project.
There are several easy and documented projects and more will come.
Any contribution is welcomed!

### Javadocs

Javadocs for WhatsappWeb4j are
available [here](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/index.html).
Any contribution is welcomed!

### How to contribute

As of today, no additional configuration or artifact building is needed to edit this project.
I recommend using the latest version of IntelliJ, though any other IDE should work.
If you are not familiar with git, follow these short tutorials in order:

1. [Fork this project](https://docs.github.com/en/github/getting-started-with-github/fork-a-repo)
2. [Clone the new repo](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/cloning-a-repository)
3. [Create a new branch](https://docs.github.com/en/desktop/contributing-and-collaborating-using-github-desktop/managing-branches#creating-a-branch)
4. Once you have implemented the new
   feature, [create a new merge request](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request)

If you are trying to implement a feature that is present on WhatsappWeb's WebClient, for example audio or video calls,
consider using [WhatsappWeb4jRequestAnalyzer](https://github.com/Auties00/whatsappweb4j-request-analyzer), a tool I
built for this exact purpose.

### How to create a connection

The most important class of this API is Whatsapp, an interface between your application and WhatsappWeb's socket.

There are numerous named constructors that can be used to initiate a connection:

1. New simple connection

   ```java
   var api = Whatsapp.newConnection();
   ```

2. Configurable new connection

   ```java
   var configuration = WhatsappOptions.newOptions() // Implement only the options that you need!
           .id(ThreadLocalRandom.current().nextInt()) // A random unique ID associated with the session
           .autodetectListeners(true) // Marks whether listeners marked with @RegisterListener should be automatically registered
           .defaultSerialization(true) // Whether the default serialization mechanism should be used
           .automaticTextPreview(true) // Whether link previews should automatically be generated for texts containing links
           .version(new Version(2,2212,7)) // The default version of this client
           .url("wss://web.whatsapp.com/ws") // The URL of WhatsappWeb's Socket
           .description("WhatsappWeb4j") // The name of the service that is displayed in Whatsapp's devices tab
           .historyLength(HistoryLength.THREE_MONTHS) // The amount of chat history that Whatsapp sends to the client on the first scan
           .errorHandler(ErrorHandler.toTerminal()) // Socket errrors handler
           .qrHandler(QrHandler.toTerminal()) // Qr code handler
           .build(); // Creates an instance of WhatsappOptions
   var api = Whatsapp.newConnection(options);
   ```

3. Last known connection chronologically

   ```java
    var api = Whatsapp.lastConnection();
   ```
   > **_IMPORTANT:_**  If no previous session exists, a new one will be created silently

4. First known connection chronologically
   ```java
   var api = Whatsapp.firstConnection();
   ```
   > **_IMPORTANT:_**  If no previous session exists, a new one will be created silently


### How to open a connection

Once you have created a new connection, you probably want to open it and wait until the operation succeeds:
```java
api.connect().join();
```

> **_IMPORTANT:_**
> Remember that this library heavily depends on async operations using the CompletableFuture construct.
> As a matter of fact, the connect method returns a CompletableFuture that is resolved only when the connection is successfully created.
> If you forget to call the get() method, or to handle this construct in any way, your application may terminate as there is no active work on the main thread.
> For the same reason, remember to also await for the connection to be closed if the logic of your application is based on listeners:
> ```java
> api.await();
> ```

### How to close a connection

There are three ways to close a connection:

1. Disconnect
   
   ```java
   api.disconnect().join();
   ```
   > **_IMPORTANT:_** The session remains valid for future uses

2. Reconnect

   ```java
   api.reconnect().join();
   ```
   > **_IMPORTANT:_** The session remains valid for future uses

3. Log out

   ```java
   api.logout().join();
   ```
   > **_IMPORTANT:_** The session doesn't remain valid for future uses


### What is a listener and how to register it

Listeners are crucial to handle events related to Whatsapp and implement logic for your application.
Listeners can be used either as:

1. Standalone concrete implementation
   
   If your application is complex enough, 
   it's preferable to divide your listeners' logic across multiple specialized classes.
   To create a new concrete listener, declare a class or record that implements the Listener interface:

   ```java
   import it.auties.whatsapp.listener.Listener;

   public class MyListener implements Listener {
    @Override
    public void onLoggedIn() {
        System.out.println("Hello :)");
    }
   }
   ```

   Remember to manually register this listener:

   ```java
   api.addListener(new MyListener());
   ```

   Or to register it automatically using the @RegisterListener annotation:

   ```java
   import it.auties.whatsapp.listener.RegisterListener;
   import it.auties.whatsapp.listener.Listener;

   @RegisterListener // Automatically registers this listener
   public class MyListener implements Listener {
    @Override
    public void onLoggedIn() {
        System.out.println("Hello :)");
    }
   }
   ```
   
   Listeners often need access to the Whatsapp instance that registered them to, for example, send messages. 
   If your listener is marked with @RegisterListener and a single argument constructor that takes a Whatsapp instance as a parameter exists,
   the latter can be injected automatically, regardless of if your implementation uses a class or a record.
   Records, though, are usually more elegant:

   ```java
   import it.auties.whatsapp.listener.RegisterListener;
   import it.auties.whatsapp.api.Whatsapp;
   import it.auties.whatsapp.listener.Listener;

   @RegisterListener // Automatically registers this listener
   public record MyListener(Whatsapp api) implements Listener { // A non-null whatsapp instance is injected
    @Override
    public void onLoggedIn() {
        System.out.println("Hello :)");
    }
   }
   ```

   > **_IMPORTANT:_** Only non-abstract classes that provide a no arguments constructor or
   > a single parameter constructor of type Whatsapp can be registered automatically
   
2. Functional interface
   
   If your application is very simple or only requires this library in small operations, 
   it's preferable to add a listener using a lambda instead of using full-fledged classes.
   To declare a new functional listener, call the method add followed by the name of the listener that you want to implement without the on suffix:
   ```java
   api.addLoggedInListener(() -> System.out.println("Hello :)"));
   ```
   
   Functional listeners can also access the instance of Whatsapp that registered them:
   ```java
   api.addLoggedInListener(whatsapp -> System.out.println("Someone sent a new message!"));
   ```
   
   This is extremely useful if you want to implement a functionality for your application in a compact manner:
   ```java
    Whatsapp.newConnection()
                .addLoggedInListener(() -> System.out.println("Connected"))
                .addNewMessageListener((whatsapp, info) -> whatsapp.sendMessage(info.chatJid(), "Automatic answer", info))
                .connect()
                .join()
                .await();
   ```

### How to handle serialization

In the original version of WhatsappWeb, chats, contacts and messages could be queried at any from Whatsapp's servers.
The multi-device implementation, instead, sends all of this information progressively when the connection is initialized for the first time and doesn't allow any subsequent queries to access the latter.
In practice, this means that this data needs to be serialized somewhere.

By default, this library serializes data regarding a session at `$HOME/.whatsappweb4j/<session_id>` in two different files, respectively for the store(chats, contacts and messages) and keys(cryptographic data).
The latter is serialized every time a modification occurs to the model, while the store is serialized everytime a ping is sent by the socket to the server.
Both are serialized when the socket is closed.
Here is the default implementation:

```java
public class DefaultControllerProvider implements ControllerProvider {
    @Override
    public LinkedList<Integer> ids() {
        try (var walker = Files.walk(Preferences.home(), 1)
                .sorted(Comparator.comparing(this::getLastModifiedTime))) {
            return walker.map(this::parsePathAsId)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(LinkedList::new));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot list known ids", exception);
        }
    }

    private FileTime getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot get last modification date", exception);
        }
    }

    private Optional<Integer> parsePathAsId(Path file) {
        try {
            return Optional.of(Integer.parseInt(file.getFileName()
                    .toString()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }


    @Override
    public void serialize(Controller<?> controller) {
        controller.preferences()
                .writeJson(controller, true);
    }
}
```
If your application needs to serialize data in a different way, for example in a database:
1. Disable the default serialization mechanism (optional)

    ```java
    var options = Options.defaultOptions() // Use the default options
            .withDefaultSerialization(false); // Disables default serialization
    var api = Whatsapp.newConnection(options); // Any named constructor can be used
    ```

2. Create a custom serializer 

    ```java
    public class CustomProvider implements ControllerProvider {
        @Override
        public LinkedList<Integer> ids() {
           // List all the ids that your serializer has created
        }
   
        @Override
        public void serialize(Controller<?> controller) {
            // Your logic
        }
    }
    ```

3. Register the custom serializer in the manifest

   - Create a directory called services inside the META-INF. 
   - Inside the folder that was just created, create a file called `it.auties.whatsapp.controller.ControllerProvider`.
   - Finally, inside the file that was just created write the fully qualified name of your implementation, for example `com.example.CustomProvider`.


### How to delete a session

To delete a particular session, call the delete method on a Whatsapp instance:
```java
api.delete();
```

Instead, if you want to delete all sessions use:
```java
Whatsapp.deleteSessions();
```

### How to handle session disconnects

When the session is closed, the onDisconnect method in any listener is invoked.
There are three types of reasons for which this can happen:
1. DISCONNECTED

    A normal disconnection.
    This doesn't indicate any error being thrown.

2. RECONNECT

    The client is being disconnected but only to reopen the connection.
    This always happens when the QR is first scanned for example.

3. LOGGED_OUT

    The client was logged out by itself or by its companion.
    When this happens the connection is terminated and becomes expired.
    By default, no error is thrown if this happens, though this behaviour can be changed easily:
    ```java
    import it.auties.whatsapp.api.DisconnectReason;
    import it.auties.whatsapp.listener.Listener;

    class ThrowOnLogOut implements Listener {
        @Override
        public void onDisconnected(DisconnectReason reason) {
            if (reason != SocketEvent.LOGGED_OUT) {
                return;
            }

            throw new RuntimeException("Hey, I was logged off :/");
        }
    }
    ```

### How to query chats, contacts, messages and status

Access the store associated with a connection by calling the store method:
```java
var store = api.store();
```

> **_IMPORTANT:_** When your program first starts up, these fields will be empty. For each type of data, an event is
> fired and listenable using a WhatsappListener

You can access all the chats that are in memory:

```java
var chats = store.chats();
```

Or the contacts:

```java
var contacts = store.contacts();
```

Or even the media status:

```java
var status = store.status();
```

Data can also be easily queried by using these methods:

- Chats
   - Query a chat by its jid
     ```java
     var chat = store.findChatByJid(jid);
     ```
  - Query a chat by its name
    ```java
    var chat = store.findChatByName(name);
    ```  
  - Query a chat by a message inside it
    ```java
    var chat = store.findChatByMessage(message);
    ```   
  - Query all chats that match a name
    ```java
    var chats = store.findChatsByName(name);
    ```  
- Contacts
   - Query a contact by its jid
     ```java
     var chat = store.findContactByJid(jid);
     ```  
  - Query a contact by its name
    ```java
    var contact = store.findContactByName(name);
    ```
   - Query all contacts that match a name
     ```java
     var contacts = store.findContactsByName(name);
     ```     
- Media status
  - Query status by sender
    ```java
    var chat = store.findStatusBySender(contact);
    ```  

### How to access companion and cryptographic data

Access keys store associated with a connection by calling the keys method:
```java
var keys = api.keys();
```

There are several methods to access and query cryptographic data, but as it's only necessary for advanced users, 
please check the javadocs if this is what you need. 

To access information about the companion device:
```java
var companion = keys.companion();
```
This object is a jid like any other, but it has the device field filled to distinguish it from the main one.
Instead, if you only need the phone number:
```java
var phoneNumber = "+%s".formatted(keys.companion().user());
```

### How to send messages

To send a message, start by finding the chat where the message should be sent. Here is an example:

```java
var chat = api.store()
        .findChatByName("My Awesome Friend")
        .orElseThrow(() -> new NoSuchElementException("Hey, you don't exist"));
``` 

All types of messages supported by Whatsapp are supported by this library:

- Text

    ```java
    api.sendMessage(chat,  "This is a text message!");
    ```

- Complex text

    ```java
    var message = TextMessage.newTextMessageBuilder() // Create a new text message
            .text("Check this video out: https://www.youtube.com/watch?v=dQw4w9WgXcQ") // Set the text of the message
            .canonicalUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ") // Set the url of the message
            .matchedText("https://www.youtube.com/watch?v=dQw4w9WgXcQ") // Set the matched text for the url in the message
            .title("A nice suprise") // Set the title of the url
            .description("Check me out") // Set the description of the url
            .build(); // Create the message
    api.sendMessage(chat,  message); 
    ```

- Location

    ```java
    var location = LocationMessage.newLocationMessageBuilder() // Create a new location message
            .caption("Look at this!") // Set the caption of the message, that is the text below the file
            .latitude(38.9193) // Set the longitude of the location to share
            .longitude(1183.1389) // Set the latitude of the location to share
            .build(); // Create the message
    api.sendMessage(chat, location);
    ```

- Live location

    ```java
    var location = LiveLocationMessage.newLiveLocationMessageBuilder() // Create a new live location message
            .caption("Look at this!") // Set the caption of the message, that is the text below the file. Not available if this message is live
            .latitude(38.9193) // Set the longitude of the location to share
            .longitude(1183.1389) // Set the latitude of the location to share
            .accuracy(10) // Set the accuracy of the location in meters
            .speed(12) // Set the speed of the device sharing the location in meter per endTimeStamp
            .build(); // Create the message
    api.sendMessage(chat, location);
    ```
  > **_IMPORTANT:_** Updating the position of a live location message is not supported as of now out of the box.
  > The tools to do so, though, are in the API.

- Group invite
    ```java
    var group = api.store()
            .findChatByName("Programmers")
            .filter(Chat::isGroup)
            .orElseThrow(() -> new NoSuchElementException("Hey, you don't exist"));
    var inviteCode = api.queryInviteCode(group).join();
    var groupInvite = GroupInviteMessage.newGroupInviteMessageBuilder() // Create a new group invite message
            .caption("Come join my group of fellow programmers") // Set the caption of this message
            .name(group.name()) // Set the name of the group
            .groupJid(group.jid())) // Set the jid of the group
            .inviteExpiration(ZonedDateTime.now().plusDays(3).toEpochSecond()) // Set the expiration of this invite
            .inviteCode(inviteCode) // Set the code of the group
            .build(); // Create the message
    api.sendMessage(chat, groupInvite); 
    ```

- Contact
    ```java
     var vcard = ContactCard.newContactCardBuilder() // Create a new vcard
            .name("A nice friend") // Set the name of the contact
            .phoneNumber(contact) // Set the phone number of the contact
            .build(); // Create the vcard
    var contactMessage = ContactMessage.newContactMessageBuilder()  // Create a new contact message
            .name("A nice friend") // Set the display name of the contact
            .vcard(vcard) // Set the vcard(https://en.wikipedia.org/wiki/VCard) of the contact
            .build(); // Create the message
    api.sendMessage(chat, contactMessage);
    ```

- Contact array

    ```java
    var contactsMessage = ContactsArrayMessage.newContactsArrayMessageBuilder()  // Create a new contacts array message
            .name("A nice friend") // Set the display name of the first contact that this message contains
            .contacts(List.of(jack,lucy,jeff)) // Set a list of contact messages that this message wraps
            .build(); // Create the message
    api.sendMessage(chat, contactsMessage);
    ```

- Button
   
   To create any number of messages to attach to a buttons message use the Button class:
   ```java
   var button = Button.newTextResponseButton("A nice button!"); // Create a button
   var anotherButton = Button.newTextResponseButton("Another button :)"); // Create another button with different text
   ```
  
   There are many types of buttons:
   
   - Response button

     - Empty header

          ```java
          var buttons = ButtonsMessage.newButtonsWithoutHeaderMessageBuilder() // Create a new button message builder
               .body("A nice body") // Set the body
               .footer("A nice footer") // Set the footer
               .buttons(List.of(button, anotherButton)) // Set the buttons
               .build(); // Create the message
          api.sendMessage(contact, emptyButtons);
          ```

     - Text header

          ```java
          var buttons = ButtonsMessage.newButtonsWithTextHeaderMessageBuilder() // Create a new button message builder
               .header("A nice header :)") // Set the header
               .body("A nice body") // Set the body
               .footer("A nice footer") // Set the footer
               .buttons(List.of(button, anotherButton)) // Set the buttons
               .build(); // Create the message
          api.sendMessage(contact, buttons);
          ```
  
     - Document header

          ```java
          var buttons = ButtonsMessage.newButtonsWithDocumentHeaderMessageBuilder() // Create a new button message builder
               .header(documentMessage) // Set the header
               .body("A nice body") // Set the body
               .footer("A nice footer") // Set the footer
               .buttons(List.of(button, anotherButton)) // Set the buttons
               .build(); // Create the message
          api.sendMessage(contact, buttons);
          ```
  
     - Image header

          ```java
          var buttons = ButtonsMessage.newButtonsWithImageHeaderMessageBuilder() // Create a new button message builder
               .header(imageMessage) // Set the header
               .body("A nice body") // Set the body
               .footer("A nice footer") // Set the footer
               .buttons(List.of(button, anotherButton)) // Set the buttons
               .build(); // Create the message
          api.sendMessage(contact, buttons);
          ```
  
     - Video header

          ```java
          var buttons = ButtonsMessage.newButtonsWithVideoHeaderMessageBuilder() // Create a new button message builder
               .header(videoMessage) // Set the header
               .body("A nice body") // Set the body
               .footer("A nice footer") // Set the footer
               .buttons(List.of(button, anotherButton)) // Set the buttons
               .build(); // Create the message
          api.sendMessage(contact, buttons);
          ```

     - Location header

          ```java
          var buttons = ButtonsMessage.newButtonsWithLocationHeaderMessageBuilder() // Create a new button message builder
               .header(locationMessage) // Set the header
               .body("A nice body") // Set the body
               .footer("A nice footer") // Set the footer
               .buttons(List.of(button, anotherButton)) // Set the buttons
               .build(); // Create the message
          api.sendMessage(contact, buttons);
          ```

  - Native flow button

    > **_IMPORTANT:_** There is no documentation for this type of button. Contributions are welcomed.

  - Interactive button

    > **_IMPORTANT:_** This type of message is currently not supported by mobile Whatsapp(still in beta).

  - List message 
     ```java
     var buttons = List.of(ButtonRow.of("First option", "A nice description"),
           ButtonRow.of("Second option", "A nice description"),
           ButtonRow.of("Third option", "A nice description")); // Create some buttons
     var section = ButtonSection.of("First section", buttons); // Create a section from those buttons
     var otherButtons = List.of(ButtonRow.of("First option", "A nice description"),
           ButtonRow.of("Second option", "A nice description"),
           ButtonRow.of("Third option", "A nice description")); // Create some other buttons
     var anotherSection = ButtonSection.of("First section", otherButtons); // Create another section from those buttons
     var listMessage = ListMessage.newListMessageBuilder() // Create a list message builder
           .sections(List.of(section, anotherSection)) // Set the sections
           .button("Click me") // Set the button name that opens the menu
           .title("A nice title") // Set the title of the message
           .description("A nice description") // Set the description of the message
           .footer("A nice footer") // Set the footer of the message
           .type(ListMessage.Type.SINGLE_SELECT) // Set the type of the message
           .build(); // Create a list message
     api.sendMessage(contact, listMessage);
    ```

  - Template button (NOT RECOMMENDED, use list message instead)
     ```java
     var quickReplyButton = HydratedButtonTemplate.of(1, HydratedQuickReplyButton.of("Click me!", "random")); // Create a quick reply button
     var urlButton = HydratedButtonTemplate.of(2, HydratedURLButton.of("Search it", "https://google.com")); // Create an url button
     var callButton = HydratedButtonTemplate.of(3, HydratedCallButton.of("Call me", "some_phone_number")); // Create a call button
     var fourRowTemplate = HydratedFourRowTemplate.newHydratedFourRowTemplateWithTextTitleBuilder() // Create a new template builder
           .title("A nice title") // Set the title
           .body("A nice body") // Set the body
           .buttons(List.of(quickReplyButton, urlButton, callButton)) // Set the buttons
           .build(); // Create the template
     var templateMessage = TemplateMessage.newHydratedTemplateMessage(fourRowTemplate); // Create a template message
     api.sendMessage(contact, templateMessage);
    ```

- Media

    To send a media, start by reading the content inside a byte array.
    You might want to read it from a file:

    ```java
    var media = Files.readAllBytes(Path.of("somewhere"));
    ```

    Or from a URL:

    ```java
    var media = new URL(url).openStream().readAllBytes();
    ```
   
   All medias supported by Whatsapp are supported by this library:

   - Image
  
     ```java
     var image = ImageMessage.newImageMessageBuilder() // Create a new image message builder
           .mediaConnection(api.store().mediaConnection()) // The media connection to use for the upload
           .media(media) // Set the image of this message
           .caption("A nice image") // Set the caption of this message
           .build(); // Create the message
     api.sendMessage(chat,  image);
     ```

  - Audio or voice

    ```java
     var audio = AudioMessage.newAudioMessageBuilder() // Create a new audio message builder
           .mediaConnection(api.store().mediaConnection()) // The media connection to use for the upload
           .media(urlMedia) // Set the audio of this message
           .voiceMessage(false) // Set whether this message is a voice message
           .build(); // Create the message
     api.sendMessage(chat,  audio);
    ```

  -  Video

     ```java
     var video = VideoMessage.newVideoMessageBuilder() // Create a new video message builder
           .mediaConnection(api.store().mediaConnection()) // The media connection to use for the upload
           .media(urlMedia) // Set the video of this message
           .caption("A nice video") // Set the caption of this message
           .width(100) // Set the width of the video
           .height(100) // Set the height of the video
           .build(); // Create the message
     api.sendMessage(chat,  video); 
     ```
     
  -  GIF(Video)

     ```java
     var gif = VideoMessage.newGifMessageBuilder() // Create a new gif message builder
           .mediaConnection(api.store().mediaConnection()) // The media connection to use for the upload
           .media(urlMedia) // Set the gif of this message
           .caption("A nice video") // Set the caption of this message
           .gifAttribution(VideoMessageAttribution.TENOR) // Set the source of the gif
           .build(); // Create the message
     api.sendMessage(chat,  gif);
     ```
     > **_IMPORTANT:_** Whatsapp doesn't support conventional gifs. Instead, videos can be played as gifs if particular attributes are set. This is the reason why the gif builder is under the VideoMessage class. Sending a conventional gif will result in an exception if detected or in undefined behaviour.

  -  Document

     ```java
     var document = DocumentMessage.newDocumentMessageBuilder() // Create a new document message builder
           .mediaConnection(api.store().mediaConnection()) // The media connection to use for the upload
           .media(urlMedia) // Set the document of this message
           .title("A nice pdf") // Set the title of the document
           .fileName("pdf-test.pdf") // Set the name of the document
           .pageCount(1) // Set the number of pages of the document
           .build(); // Create the message
     api.sendMessage(chat,  document);
     ```

### How to delete messages

``` java
var result = api.delete(someMessage, everyone).join(); // Deletes a message for yourself or everyone
```

### How to send or remove reactions

``` java
var sendResult = api.sendReaction(someMessage, "💖").join(); // Send a reaction to a message
var removeResult = api.removeReaction(someMessage).join(); // Removes your reaction from a message
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
api.changePresence(chat,  presence);
```

> **_IMPORTANT:_** The changePresence method returns a CompletableFuture: remember to handle this async construct if
> needed

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
var status = api.queryStatus(contact) // A completable future
      .join() // Wait for the future to complete
      .map(ContactStatusResponse::status) // Map the response to its status
      .orElse(null); // If no status is available yield null
```

##### Profile picture or chat picture

``` java
var status = api.queryPic(contact) // A completable future
      .join() // Wait for the future to complete
      .orElse(null); // If no picture is available yield null
```

##### Group's Metadata

``` java
var metadata = api.queryGroupMetadata(group); // A completable future
      .join(); // Wait for the future to complete
```

### Search messages

``` java
var messages = chat.messages(); // All the messages in a chat
var firstMessage = chat.firstMessage(); // First message in a chat chronologically
var lastMessage = chat.lastMessage(); // Last message in a chat chronologically 
var starredMessages = chat.starredMessages(); // All the starred messages in a chat
```

### Change the state of a chat

##### Mute a chat

``` java
var future = api.mute(chat);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Unmute a chat

``` java
var future = api.mute(chat);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Archive a chat

``` java
var future = api.archive(chat);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Unarchive a chat

``` java
var future = api.unarchive(chat);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Change ephemeral message status in a chat

``` java
var future = api.changeEphemeralTimer(chat,  ChatEphemeralTimer.ONE_WEEK);  // A future for the request
var response = future.join(); // Wait for the future to complete
```   

##### Mark a chat as read

``` java
var future = api.markAsRead(chat);  // A future for the request
var response = future.join(); // Wait for the future to complete
```   

##### Mark a chat as unread

``` java
var future = api.markAsUnread(chat);  // A future for the request
var response = future.join(); // Wait for the future to complete
```   

##### Pin a chat

``` java
var future = api.pin(chat);  // A future for the request
var response = future.join(); // Wait for the future to complete
``` 

##### Unpin a chat

``` java
var future = api.unpin(chat);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Clear a chat

``` java
var future = api.clear(chat);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

> **_IMPORTANT:_** This method is experimental and may not work

##### Delete a chat

``` java
var future = api.delete(chat);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

> **_IMPORTANT:_** This method is experimental and may not work

### Change the state of a participant of a group

##### Add a contact to a group

``` java
var future = api.add(group, contact);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Remove a contact from a group

``` java
var future = api.remove(group, contact);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Promote a contact to admin in a group

``` java
var future = api.promote(group, contact);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Demote a contact to user in a group

``` java
var future = api.demote(group, contact);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

### Change the metadata or settings of a group

##### Change group's name/subject

``` java
var future = api.changeSubject(group, newName);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Change or remove group's description

``` java
var future = api.changeDescription(group, newDescription);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Change who can send messages in a group

``` java
var future = api.changeWhoCanSendMessages(group, GroupPolicy.ANYONE);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Change who can edit the metadata/settings in a group

``` java
var future = api.changeWhoCanEditInfo(group, GroupPolicy.ANYONE);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Change or remove the picture of a group

``` java
var future = api.changePicture(group, img);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

### Other group related methods

##### Create a group

``` java
var future = api.create("A nice name :)", friend, friend2);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Leave a group

``` java
var future = api.leave(group);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Query a group's invite code

``` java
var future = api.queryInviteCode(group);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Revoke a group's invite code

``` java
var future = api.revokeInviteCode(group);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

##### Query a group's invite code

``` java
var future = api.acceptInvite(inviteCode);  // A future for the request
var response = future.join(); // Wait for the future to complete
```

