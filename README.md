# Cobalt

Whatsapp4j has been renamed to Cobalt to comply with an official request coming from Whatsapp.
The repository's history was cleared to comply with this request, but keep in mind that the project has been actively developed for over two years.
To be clear, this library is not affiliated with Whatsapp LLC in any way.
This is a personal project that I maintain in my free time

### What is Cobalt

Cobalt is a library built to interact with Whatsapp.
It can be used with:
1. Whatsapp Web (Companion)
2. Whatsapp Mobile (Personal and Business)

### Donations

If you like my work, you can become a sponsor here on GitHub or tip me through:
- [Paypal](https://www.paypal.me/AutiesDevelopment)

I can also work on sponsored features and/or projects!

### Java version

This library requires at least [Java 21](https://openjdk.java.net/projects/jdk/21/).

GraalVM native compilation is supported!

### Breaking changes policy

Until the library doesn't reach release 1.0, there will be major breaking changes between each release.
This is needed to finalize the design of the API.
After this milestone, breaking changes will be present only in major releases.

### Can this library get my device banned?

While there is no risk in using this library with your main account, keep in mind that Whatsapp has anti-spam measures for their web client.
If you add a participant from a brand-new number to a group, it will most likely get you banned.
If you compile the library yourself, don't run the CI on a brand-new number, or it will get banned for spamming too many requests(the CI has to test that all the library works).
In short, if you use this library without a malicious intent, you will never get banned.

### How to install

#### Maven
```xml
<dependency>
    <groupId>com.github.auties00</groupId>
    <artifactId>cobalt</artifactId>
    <version>0.0.9</version>
</dependency>
```

#### Gradle

- Groovy DSL
    ```groovy
    implementation 'com.github.auties00:cobalt:0.0.9'
    ```

- Kotlin DSL
    ```groovy
    implementation("com.github.auties00:cobalt:0.0.9")
    ```

### Javadocs & Documentation

Javadocs for Cobalt are available [here](https://www.javadoc.io/doc/com.github.auties00/cobalt/0.0.9).
The documentation for this project reaches most of the publicly available APIs(i.e. public members in exported packages), but sometimes the Javadoc may be incomplete
or some methods could be absent from the project's README. If you find any of the latter, know that even small contributions are welcomed!

### How to contribute

As of today, no additional configuration or artifact building is needed to edit this project.
I recommend using the latest version of IntelliJ, though any other IDE should work.
If you are not familiar with git, follow these short tutorials in order:

1. [Fork this project](https://docs.github.com/en/github/getting-started-with-github/fork-a-repo)
2. [Clone the new repo](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/cloning-a-repository)
3. [Create a new branch](https://docs.github.com/en/desktop/contributing-and-collaborating-using-github-desktop/managing-branches#creating-a-branch)
4. Once you have implemented the new feature, [create a new merge request](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request)

Check the frida module to understand how I go about reversing features

### Disclaimer about async operations
This library heavily depends on async operations using the CompletableFuture construct.
Remember to handle them as your application will terminate without doing anything if the main thread is not executing any task.
Please do not open redundant issues on GitHub because of this.

### How to create a connection
<details>
  <summary>Detailed Walkthrough</summary>


To create a new connection, start by creating a builder with the api you need:
- Web
    ```java
    Whatsapp.webBuilder()
    ```
- Mobile
  ```java
    Whatsapp.mobileBuilder()
  ```
If you want to use a custom serializer, specify it:
  ```java
  .serializer(new CustomControllerSerializer())
  ```  
Now select the type of connection that you need:
- Create a fresh connection
  ```java
  .newConnection(someUuid)
  ```   
- Retrieve a connection by id if available, otherwise create a new one
  ```java
  .newConnection(someUuid)
  ```
- Retrieve a connection by phone number if available, otherwise create a new one
  ```java
  .newConnection(phoneNumber)
  ```
- Retrieve a connection by an alias if available, otherwise create a new one
  ```java
  .newConnection(alias)
  ```
- Retrieve a connection by id if available, otherwise returns an empty Optional
  ```java
  .newOptionalConnection(someUuid)
  ```
- Retrieve the first connection that was serialized if available, otherwise create a new one
  ```java
  .firstConnection()
  ```
- Retrieve the first connection that was serialized if available, otherwise returns an empty Optional
  ```java
  .firstOptionalConnection()
  ```
- Retrieve the last connection that was serialized if available, otherwise create a new one
  ```java
  .lastConnection()
  ```
- Retrieve the last connection that was serialized if available, otherwise returns an empty Optional
  ```java
  .lastOptionalConnection()
  ```
You can now customize the API with these options:
- name - The device's name for Whatsapp Web, the push name for Whatsapp's Mobile
  ```java
  .name("Some Custom Name :)")
  ```
- version - The version of Whatsapp to use
  ```java
  .version(new Version("x.xx.xx"))
  ```
- autodetectListeners - Whether listeners annotated with `@RegisterListener` should automatically be registered
  ```java
  .autodetectListeners(true)
  ```
- textPreviewSetting - Whether a media preview should be generated for text messages containing links
  ```java
  .textPreviewSetting(TextPreviewSetting.ENABLED_WITH_INFERENCE)
  ```
- checkPatchMacs - Whether patch macs coming from app state pulls should be validated
  ```java
  .checkPatchMacs(checkPatchMacs)
  ```
- proxy - The proxy to use for the socket connection
  ```java
  .proxy(someProxy)
  ```

There are also platform specific options:
1. Web
    - historyLength: The amount of messages to sync from the companion device
      ```java
      .historyLength(WebHistoryLength.THREE_MONTHS)
      ```
2. Mobile
    - device: the device you want to fake:
      ```java
      .device(CompanionDevice.android(false)) // Standard Android
      .device(CompanionDevice.android(true)) //Business android
      .device(CompanionDevice.ios(false)) // Standard iOS
      .device(CompanionDevice.ios(true)) // Business iOS
      .device(CompanionDevice.kaiOs()) // Standard KaiOS
       ```
    - businessCategory: the category of your business account
      ```java
      .businessCategory(new BusinessCategory(id, name))
       ```
    - businessEmail: the email of your business account
      ```java
      .businessEmail("email@domanin.com")
       ```
    - businessWebsite: the website of your business account
      ```java
      .businessWebsite("https://google.com")
       ```
    - businessDescription: the description of your business account
      ```java
      .businessDescription("A nice description")
       ```
    - businessLatitude: the latitude of your business account
      ```java
      .businessLatitude(37.386051)
       ```
    - businessLongitude: the longitude of your business account
      ```java
      .businessLongitude(-122.083855)
       ```
    - businessAddress: the address of your business account
      ```java
      .businessAddress("1600 Amphitheatre Pkwy, Mountain View")
       ```

> **_IMPORTANT:_** All options are serialized: there is no need to specify them again when deserializing an existing session

Finally select the registration status of your session:
- Creates a new registered session: this means that the QR code was already scanned / the OTP was already sent to Whatsapp
  ```java
  .registered()
  ```
- Creates a new unregistered session: this means that the QR code wasn't scanned / the OTP wasn't sent to the companion's phone via SMS/Call/OTP

  If you are using the Web API, you can either register via QR code:
  ```java
  .unregistered(QrHandler.toTerminal())
  ```  
  or with a pairing code(new feature):
  ```java
  .unregistered(yourPhoneNumberWithCountryCode, PairingCodeHandler.toTerminal())
  ```  
  Otherwise, if you are using the mobile API, you can decide if you want to receive an SMS, a call or an OTP:
  ```java
  .verificationCodeMethod(VerificationCodeMethod.SMS)
  ```  
  Then provide a supplier for that verification method:
  ```java
  .verificationCodeSupplier(() -> yourAsyncOrSyncLogic())
  ```
  Finally, register:
  ```java
  .register(yourPhoneNumberWithCountryCode)
  ```

Now you can connect to your session:
  ```java
  .connect()
  ```
to connect to Whatsapp.
Remember to handle the result using, for example, `join` to await the connection's result.
Finally, if you want to pause the current thread until the connection is closed, use:
  ```java
  .awaitDisconnection()
  ```
</details>

<details>
  <summary>Web QR Pairing Example</summary>

  ```java
  Whatsapp.webBuilder() // Use the Web api
        .newConnection() // Create a new connection
        .unregistered(QrHandler.toTerminal()) // Print the QR to the terminal
        .addLoggedInListener(api -> System.out.printf("Connected: %s%n", api.store().privacySettings())) // Print a message when connected
        .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason)) // Print a message when disconnected
        .addNewChatMessageListener(message -> System.out.printf("New message: %s%n", message.toJson())) // Print a message when a new chat message arrives
        .connect() // Connect to Whatsapp asynchronously
        .join() // Await the result
        .awaitDisconnection(); // Wait 
  ```
</details>

<details>
  <summary>Web Pairing Code Example</summary>

  ```java
  System.out.println("Enter the phone number(include the country code prefix, but no +, spaces or parenthesis):")
var scanner = new Scanner(System.in);
var phoneNumber = scanner.nextLong();
  Whatsapp.webBuilder() // Use the Web api
        .newConnection() // Create a new connection
        .unregistered(phoneNumber, PairingCodeHandler.toTerminal()) // Print the pairing code to the terminal
        .addLoggedInListener(api -> System.out.printf("Connected: %s%n", api.store().privacySettings())) // Print a message when connected
        .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason)) // Print a message when disconnected
        .addNewChatMessageListener(message -> System.out.printf("New message: %s%n", message.toJson())) // Print a message when a new chat message arrives
        .connect() // Connect to Whatsapp asynchronously
        .join() // Await the result
        .awaitDisconnection(); // Wait 
  ```
</details>

<details>
  <summary>Mobile Example</summary>

  ```java
  System.out.println("Enter the phone number(include the country code prefix, but no +, spaces or parenthesis):")
var scanner = new Scanner(System.in);
var phoneNumber = scanner.nextLong();
  Whatsapp.mobileBuilder() // Use the Mobile api
        .newConnection() // Create a new connection
        .device(CompanionDevice.ios(false)) // Use a non-business iOS account
        .unregistered() // If the connection was just created, it needs to be registered
        .verificationCodeMethod(VerificationCodeMethod.SMS) // If the connection was just created, send an SMS OTP
        .verificationCodeSupplier(() -> { // Called when the OTP needs to be sent to Whatsapp
        System.out.println("Enter OTP: ");
var scanner = new Scanner(System.in);
            return scanner.nextLine();
        })
                .register(phoneNumber) // Register the phone number asynchronously, if necessary
        .join() // Await the result
        .whatsapp() // Access the Whatsapp instance
        .addLoggedInListener(api -> System.out.printf("Connected: %s%n", api.store().privacySettings())) // Print a message when connected
        .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason)) // Print a message when disconnected
        .addNewChatMessageListener(message -> System.out.printf("New message: %s%n", message.toJson())) // Print a message when a new chat message arrives
        .connect() // Connect to Whatsapp asynchronously
        .join() // Await the result
        .awaitDisconnection(); // Wait 
  ```
</details>

### How to close a connection

There are three ways to close a connection:

1. Disconnect

   ```java
   api.disconnect();
   ```
   > **_IMPORTANT:_** The session remains valid for future uses

2. Reconnect

   ```java
   api.reconnect();
   ```
   > **_IMPORTANT:_** The session remains valid for future uses

3. Log out

   ```java
   api.logout();
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
   import it.auties.whatsapp.api.Listener;

   public class MyListener implements Listener {
    @Override
    public void onLoggedIn() {
        System.out.println("Hello :)");
    }
   }
   ```

   Remember to register this listener:

   ```java
   api.addListener(new MyListener());
   ```

2. Functional interface

   If your application is very simple or only requires this library in small operations,
   it's preferable to add a listener using a lambda instead of using full-fledged classes.
   To declare a new functional listener, call the method add followed by the name of the listener that you want to implement without the on suffix:
   ```java
   api.addDisconnectedListener(reason -> System.out.println("Goodbye: " + reason));
   ```

   All lambda listeners can access the instance of `Whatsapp` that called them:
   ```java
   api.addDisconnectedListener((whatsapp, reason) -> System.out.println("Goodbye: " + reason));
   ```

   This is extremely useful if you want to implement a functionality for your application in a compact manner:
   ```java
    Whatsapp.newConnection()
                .addLoggedInListener(() -> System.out.println("Connected"))
                .addNewMessageListener((whatsapp, info) -> whatsapp.sendMessage(info.chatJid(), "Automatic answer", info))
                .connect()
                .join();
   ```

### How to handle serialization

In the original version of WhatsappWeb, chats, contacts and messages could be queried at any from Whatsapp's servers.
The multi-device implementation, instead, sends all of this information progressively when the connection is initialized for the first time and doesn't allow any subsequent queries to access the latter.
In practice, this means that this data needs to be serialized somewhere.
The same is true for the mobile api.

By default, this library serializes data regarding a session at `$HOME/.whatsapp4j/[web|mobile]/<session_id>`.
The data is stored in protobuf files.

If your application needs to serialize data in a different way, for example in a database create a custom implementation of ControllerSerializer.
Then make sure to specify your implementation in the `Whatsapp` builder.
This is explained in the "How to create a connection" section.

### How to handle session disconnects

When the session is closed, the onDisconnect method in any listener is invoked.
These are the three reasons that can cause a disconnect:

1. DISCONNECTED

   A normal disconnection.
   This doesn't indicate any error being thrown.

2. RECONNECTING

   The client is being disconnected but only to reopen the connection.
   This always happens when the QR is first scanned for example.

3. LOGGED_OUT

   The client was logged out by itself or by its companion.
   By default, no error is thrown if this happens, though this behaviour can be changed easily:
    ```java
    import it.auties.whatsapp.api.DisconnectReason;
    import it.auties.whatsapp.api.Listener;import it.auties.whatsapp.api.WhatsappListener;

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

4. BANNED

   The client was banned by Whatsapp, usually happens when sending spam messages to people that aren't in your contact list

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

Or even the status:

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

### How to query other data

To access information about the companion device:
```java
var companion = store.jid();
```
This object is a jid like any other, but it has the device field filled to distinguish it from the main one.
Instead, if you only need the phone number:
```java
var phoneNumber = store.jid().toPhoneNumber();
```
All the settings and metadata about the companion is available inside the Store class
```java
var store = api.store();
```
Explore of the available methods!

### How to query cryptographic data

Access keys store associated with a connection by calling the keys method:
```java
var keys = api.keys();
```
There are several methods to access and query cryptographic data, but as it's only necessary for advanced users,
please check the javadocs if this is what you need.

### How to send messages

To send a message, start by finding the chat where the message should be sent. Here is an example:

```java
var chat = api.store()
        .findChatByName("My Awesome Friend")
        .orElseThrow(() -> new NoSuchElementException("Hey, you don't exist"));
``` 

All types of messages supported by Whatsapp are supported by this library:
> **_IMPORTANT:_** Buttons are not documented here because they are unstable.
> If you are interested you can try to use them, but they are not guaranteed to work.
> There are some examples in the tests directory.

- Text

    ```java
    api.sendMessage(chat,  "This is a text message!");
    ```

- Complex text

    ```java
    var message = new TextMessageBuilder() // Create a new text message
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
    var location = new LocationMessageBuilder() // Create a new location message
            .caption("Look at this!") // Set the caption of the message, that is the text below the file
            .latitude(38.9193) // Set the longitude of the location to share
            .longitude(1183.1389) // Set the latitude of the location to share
            .build(); // Create the message
    api.sendMessage(chat, location);
    ```

- Live location

    ```java
    var location = new LiveLocationMessageBuilder() // Create a new live location message
            .caption("Look at this!") // Set the caption of the message, that is the text below the file. Not available if this message is live
            .latitude(38.9193) // Set the longitude of the location to share
            .longitude(1183.1389) // Set the latitude of the location to share
            .accuracy(10) // Set the accuracy of the location in meters
            .speed(12) // Set the speed of the device sharing the location in meter per endTimeStamp
            .build(); // Create the message
    api.sendMessage(chat, location);
    ```
  > **_IMPORTANT:_** Live location updates are not supported by Whatsapp multi-device. No ETA has been given for a fix.

- Group invite
    ```java
    var group = api.store()
            .findChatByName("Programmers")
            .filter(Chat::isGroup)
            .orElseThrow(() -> new NoSuchElementException("Hey, you don't exist"));
    var inviteCode = api.queryGroupInviteCode(group).join();
    var groupInvite = new GroupInviteMessageBuilder() // Create a new group invite message
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
     var vcard = new ContactCardBuilder() // Create a new vcard
            .name("A nice friend") // Set the name of the contact
            .phoneNumber(contact) // Set the phone number of the contact
            .build(); // Create the vcard
    var contactMessage = new ContactMessageBuilder()  // Create a new contact message
            .name("A nice friend") // Set the display name of the contact
            .vcard(vcard) // Set the vcard(https://en.wikipedia.org/wiki/VCard) of the contact
            .build(); // Create the message
    api.sendMessage(chat, contactMessage);
    ```

- Contact array

    ```java
    var contactsMessage = new ContactsArrayMessageBuilder()  // Create a new contacts array message
            .name("A nice friend") // Set the display name of the first contact that this message contains
            .contacts(List.of(jack,lucy,jeff)) // Set a list of contact messages that this message wraps
            .build(); // Create the message
    api.sendMessage(chat, contactsMessage);
    ```

- Media

  > **_IMPORTANT:_**
  >
  > The thumbnail for videos and gifs is generated automatically only if ffmpeg is installed on the host machine.
  >
  > The length of videos, gifs and audios in seconds is computed automatically only if ffprobe is installed on the host machine.

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
      var image = new ImageMessageSimpleBuilder() // Create a new image message builder
            .media(media) // Set the image of this message
            .caption("A nice image") // Set the caption of this message
            .build(); // Create the message
      api.sendMessage(chat,  image);
      ```

    - Audio or voice

      ```java
       var audio = new AudioMessageSimpleBuilder() // Create a new audio message builder
             .media(urlMedia) // Set the audio of this message
             .voiceMessage(false) // Set whether this message is a voice message
             .build(); // Create the message
       api.sendMessage(chat,  audio);
      ```

    -  Video

       ```java
       var video = new VideoMessageSimpleBuilder() // Create a new video message builder
             .media(urlMedia) // Set the video of this message
             .caption("A nice video") // Set the caption of this message
             .width(100) // Set the width of the video
             .height(100) // Set the height of the video
             .build(); // Create the message
       api.sendMessage(chat,  video); 
       ```

    -  GIF(Video)

       ```java
       var gif = new GifMessageSimpleBuilder() // Create a new gif message builder
             .media(urlMedia) // Set the gif of this message
             .caption("A nice gif") // Set the caption of this message
             .gifAttribution(VideoMessageAttribution.TENOR) // Set the source of the gif
             .build(); // Create the message
       api.sendMessage(chat,  gif);
       ```
       > **_IMPORTANT:_** Whatsapp doesn't support conventional gifs. Instead, videos can be played as gifs if particular attributes are set. Sending a conventional gif will result in an exception if detected or in undefined behaviour.

    -  Document

       ```java
       var document = new DocumentMessageSimpleBuilder() // Create a new document message builder
             .media(urlMedia) // Set the document of this message
             .title("A nice pdf") // Set the title of the document
             .fileName("pdf-test.pdf") // Set the name of the document
             .pageCount(1) // Set the number of pages of the document
             .build(); // Create the message
       api.sendMessage(chat,  document);
       ```
- Reaction

    - Send a reaction

    ```java
    var someMessage = ...; // The message to react to
    api.sendReaction(someMessage, Emoji.RED_HEART); // Use the Emoji class for a list of all Emojis
    ```

    - Remove a reaction

    ```java
    var someMessage = ...; // The message to react to
    api.removeReaction(someMessage); // Use the Emoji class for a list of all Emojis
    ```

### How to wait for replies

If you want to wait for a single reply, use:
``` java
var response = api.awaitReply(info).join(); 
```

You can also register a listener, but in many cases the async/await paradigm is easier to use then callback based listeners.

### How to delete messages

``` java
var result = api.delete(someMessage, everyone); // Deletes a message for yourself or everyone
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
api.subscribeToPresence(contact);
```

Then, after the subscribeToUserPresence's future is completed, query again the presence of that contact.

### Query data about a group, or a contact

##### About

``` java
var status = api.queryAbout(contact) // A completable future
      .join() // Wait for the future to complete
      .flatMap(ContactAboutResponse::about) // Map the response to its status
      .orElse(null); // If no status is available yield null
```

##### Profile picture or chat picture

``` java
var picture = api.queryPicture(contact) // A completable future
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
var future = api.muteChat(chat);
```

##### Unmute a chat

``` java
var future = api.unmuteChat(chat);
```

##### Archive a chat

``` java
var future = api.archiveChat(chat);
```

##### Unarchive a chat

``` java
var future = api.unarchiveChat(chat);
```

##### Change ephemeral message status in a chat

``` java
var future = api.changeEphemeralTimer(chat,  ChatEphemeralTimer.ONE_WEEK);
```   

##### Mark a chat as read

``` java
var future = api.markChatRead(chat);
```   

##### Mark a chat as unread

``` java
var future = api.markChatUnread(chat);
```   

##### Pin a chat

``` java
var future = api.pinChat(chat);
``` 

##### Unpin a chat

``` java
var future = api.unpinChat(chat);
```

##### Clear a chat

``` java
var future = api.clearChat(chat, false);
```

##### Delete a chat

``` java
var future = api.deleteChat(chat);
```

### Change the state of a participant of a group

##### Add a contact to a group

``` java
var future = api.addGroupParticipant(group, contact);
```

##### Remove a contact from a group

``` java
var future = api.removeGroupParticipant(group, contact);
```

##### Promote a contact to admin in a group

``` java
var future = api.promoteGroupParticipant(group, contact);
```

##### Demote a contact to user in a group

``` java
var future = api.demoteGroupParticipant(group, contact);
```

### Change the metadata or settings of a group

##### Change group's name/subject

``` java
var future = api.changeGroupSubject(group, newName);
```

##### Change or remove group's description

``` java
var future = api.changeGroupDescription(group, newDescription);
```

##### Change a setting in a group

``` java
var future = api.changeGroupSetting(group, GroupSetting.EDIT_GROUP_INFO, GroupPolicy.ANYONE);
```

##### Change or remove the picture of a group

``` java
var future = api.changeGroupPicture(group, img);
```

### Other group related methods

##### Create a group

``` java
var future = api.createGroup("A nice name :)", friend, friend2);
```

##### Leave a group

``` java
var future = api.leaveGroup(group);
```

##### Query a group's invite code

``` java
var future = api.queryGroupInviteCode(group);
```

##### Revoke a group's invite code

``` java
var future = api.revokeGroupInvite(group);
```

##### Accept a group invite

``` java
var future = api.acceptGroupInvite(inviteCode);
```

### 2FA (Mobile api only)

##### Enable 2FA

``` java
var future = api.enable2fa("000000", "mail@domain.com");
```

##### Disable 2FA

``` java
var future = api.disable2fa();
```

### Calls (Mobile api only)

##### Start a call

``` java
var future = api.startCall(contact);
```

> **_IMPORTANT:_** Currently there is no audio/video support

##### Stop or reject a call

``` java
var future = api.stopCall(contact);
```

### Communities

-   **Create a community:**
    ```java
    var future = api.createCommunity("New Community Name", "Optional community description");
    ```
    
-   **Query community metadata:**
    ```java
    var future = api.queryCommunityMetadata(communityJid);
    ```
    
-   **Deactivate a community:**
    ```java
    var future = api.deactivateCommunity(communityJid);
    ```
-   **Change community picture:**
    ```java
    byte[] imageBytes = ...; // Or use URI
    var future = api.changeCommunityPicture(communityJid, imageBytes);
    var removeFuture = api.changeCommunityPicture(communityJid, (byte[]) null); // Remove picture
    ```
    
-   **Change community subject (name):**
    ```java
    var future = api.changeCommunitySubject(communityJid, "Updated Community Name");
    ```
    
-   **Change community description:**
    ```java
    var future = api.changeCommunityDescription(communityJid, "Updated description");
    var removeFuture = api.changeCommunityDescription(communityJid, null); // Remove description
    ```
    
-   **Change community setting:**
    ```java
    // Who can add groups (MODIFY_GROUPS) or add participants (ADD_PARTICIPANTS)
    var future = api.changeCommunitySetting(communityJid, CommunitySetting.MODIFY_GROUPS, ChatSettingPolicy.ADMINS);
    ```
    
-   **Link groups to a community:**
    ```java
    var future = api.addCommunityGroups(communityJid, groupJid1, groupJid2);
    ```
    
-   **Unlink a group from a community:**
    ```java
    var future = api.removeCommunityGroup(communityJid, groupJid);
    ```
    
-   **Promote participants to admin in a community:**
    ```java
    var future = api.promoteCommunityParticipants(communityJid, contactJid1, contactJid2);
    ```
    
-   **Demote participants to member in a community:**
    ```java
    var future = api.demoteCommunityParticipants(communityJid, contactJid1, contactJid2);
    ```
    
-   **Add participants to a community:** (Adds to announcement group)
    ```java
    var future = api.addCommunityParticipants(communityJid, contactJid1, contactJid2);
    ```
    
-   **Remove participants from a community:**
    ```java
    var future = api.removeCommunityParticipants(communityJid, contactJid1, contactJid2);
    ```
    
-   **Leave a community:** (Leaves community and all linked groups)
    ```java
    var future = api.leaveCommunity(communityJid);
    ```

### Newsletters / Channels

-   **Query recommended newsletters:**
    ```java
    var future = api.queryRecommendedNewsletters("US", 50); // Country code and limit
    ```
    
-   **Query newsletter messages:**
    ```java
    var future = api.queryNewsletterMessages(newsletterJid, 100); // Query last 100 messages
    ```
    
-   **Subscribe to newsletter reactions:**
    ```java
    var future = api.subscribeToNewsletterReactions(newsletterJid);
    ```
    
-   **Create a newsletter:**
    ```java
    byte[] pictureBytes = ...; // Optional picture
    var future = api.createNewsletter("My Newsletter", "Description", pictureBytes);
    var simpleFuture = api.createNewsletter("Simple Newsletter"); // Name only
    ```
    
-   **Change newsletter description:**
    ```java
    var future = api.changeNewsletterDescription(newsletterJid, "New Description");
    var removeFuture = api.changeNewsletterDescription(newsletterJid, null); // Remove description
    ```
    
-   **Join a newsletter:**
    ```java
    var future = api.joinNewsletter(newsletterJid);
    ```
    
-   **Leave a newsletter:**
    ```java
    var future = api.leaveNewsletter(newsletterJid);
    ```
    
-   **Query newsletter subscribers count:**
    ```java
    var future = api.queryNewsletterSubscribers(newsletterJid);
    ```
    
-   **Invite newsletter admins:** (Sends an invite message to the user)
    ```java
    var future = api.inviteNewsletterAdmins(newsletterJid, "Join as admin!", adminJid1, adminJid2);
    ```
    
-   **Revoke newsletter admin invite:**
    ```java
    var future = api.revokeNewsletterAdminInvite(newsletterJid, adminJid);
    ```
    
-   **Accept newsletter admin invite:**
    ```java
    var future = api.acceptNewsletterAdminInvite(newsletterJid);
    ```
    
-   **Query newsletter metadata:**
    ```java
    // Role required depends on what info you need (e.g., GUEST, SUBSCRIBER, ADMIN)
    var future = api.queryNewsletter(newsletterJid, NewsletterViewerRole.SUBSCRIBER);
    ```
    
-   **Send newsletter message:**
    ```java
    var future = api.sendNewsletterMessage(newsletterJid, message);
    ```
    
-   **Edit newsletter message:**
    ```java
    var future = api.editMessage(oldMessage, newContent);
    ```
    
-   **Delete newsletter message:**
    ```java
    var future = api.deleteMessage(messageToDelete);
    
    ```
-   **Download newsletter media:**
    ```java
    var future = api.downloadMedia(mediaMessageInfo);
    ```

Some methods may not be listed here, all contributions are welcomed to this documentation!

Some methods may not be supported on the mobile api, please report them, so I can fix them.

Ideally I'd like all of them to work.
