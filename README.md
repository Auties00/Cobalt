# Cobalt
Whatsapp4j has been renamed to Cobalt to comply with an official request coming from Whatsapp.
To be clear, this library is not affiliated with Whatsapp LLC in any way.
This is a personal project that I mantain in my free time

### What is Cobalt

Cobalt is a library built to interact with Whatsapp.
It can be used to work with:
1. WhatsappWeb (MultiDevice)

   This functionality is fully documented and ready.
   No browser, application or any additional software is necessary.
   Everything as of now has been successfully reverse engineered.
   A companion device with the Whatsapp app installed is needed to scan the QR code the first time.
   As the multi device api is supported, the companion doesn't need to remain online for the client to work.

2. Whatsapp Mobile App

   This functionality is currently in beta.
   The documentation is expanding, but some functions may still be undocumented. 
   Most functions have been reversed engineered, but some may still not work. 

### Donations

If you like my work, you can become a sponsor here on GitHub or tip me on [Paypal](https://www.paypal.com/paypalme/my/profile).

If you prefer crypto, this is my eth address: 0xA7842cDb100fb91718961153149C86e4F4030a76

I can also work on sponsored features and/or projects!

### Java version

This library was built for [Java 17](https://openjdk.java.net/projects/jdk/17/), the latest LTS.

### Optimizing memory usage

If the machine you are hosting this library on has memory constraints, please look into how to tune a JVM.
The easiest thing you can do is use the -Xmx argument to specify the maximum size, in bytes, of the memory allocation pool.
I have written this disclaimer because many new devs tend to get confused by Java's opportunistic memory allocation.

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
    <artifactId>whatsappweb4j</artifactId>
    <version>3.4.8</version>
</dependency>
```

#### Gradle

1. Groovy DSL
   ```groovy
   implementation 'com.github.auties00:whatsappweb4j:3.4.8'
   ```

2. Kotlin DSL
   ```kotlin
   implementation("com.github.auties00:whatsappweb4j:3.4.8")
   ```

### Examples

If you need some examples to get started, check
the [examples' directory](https://github.com/Auties00/Whatsapp4j/tree/master/examples) in this project.
There are several easy and documented projects and more will come.
Any contribution is welcomed!

### Javadocs & Documentation

Javadocs for Whatsapp4j are available [here](https://www.javadoc.io/doc/com.github.auties00/whatsappweb4j/latest/whatsapp4j/index.html).
The documentation for this project reaches most of the publicly available APIs(i.e. public members in exported packages), but sometimes the Javadoc may be incomplete 
or some methods could be absent from the project's README. If you find any of the latter, know that even small contributions are welcomed!

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

### Disclaimer about async operations 
This library heavily depends on async operations using the CompletableFuture construct.
Remember to handle them as your application will terminate without doing anything if the main thread is not executing any task.
Please do not open redundant issues on GitHub because of this.

### How to create a connection
Here are two examples:
- Web 
    ```java
      Whatsapp.webBuilder()
                .newConnection()
                .build()
                .addLoggedInListener(() -> System.out.println("Connected"))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .connect()
                .join();
    ```
- Mobile
    ```java
      Whatsapp.mobileBuilder()
                .newConnection()
                .unregistered()
                .register(yourPhoneNumber, () -> {
                   System.out.println("Enter OTP: ");
                   return new Scanner(System.in).nextLine().trim();
                })
                .join()
                .addLoggedInListener(() -> System.out.println("Connected"))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .connect()
                .join();
    ```

If you want to understand this code, read the following walkthrough.
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
- Retrieve the first connection that was serialized if available, otherwise create a new one
  ```java
  .firstConnection()
  ```
- Retrieve the last connection that was serialized if available, otherwise create a new one
  ```java
  .lastConnection()
  ```
- Retrieve a connection by id if available, otherwise create a new one
  ```java
  .knownConnection(someUuid)
  ```
You can now customize the API with these options:
- name - The device's name for Whatsapp Web, the push name for Whatsapp's Mobile (Serialized)
  ```java
  .name("Some Custom Name :)")
  ```
- version - The version of Whatsapp to use (Serialized)
  ```java
  .version(new Version("x.xx.xx"))
  ```
- autodetectListeners - Whether listeners annotated with `@RegisterListener` should automatically be registered (Serialized)
  ```java
  .autodetectListeners(true)
  ```
- textPreviewSetting - Whether a media preview should be generated for text messages containing links (Serialized)
  ```java
  .textPreviewSetting(TextPreviewSetting.ENABLED_WITH_INFERENCE)
  ```
- errorHandler - The error handler to use for this session (Not serialized, specify this every time)
  ```java
  .errorHandler(ErrorHandler.toTerminal())
  ```
- proxy - The proxy to use for the socket connection (Serialized)
  ```java
  .proxy(someProxy)
  ```
- socketExecutor - The custom executor to handle the socket asynchronously (Not serialized, specify this every time)
  ```java
  .socketExecutor(someCustomExecutor)
  ```
If you are using the web api you can also set these options:
- historyLength: The amount of messages to sync from the companion device (Serialized)
  ```java
  .historyLength(WebHistoryLength.THREE_MONTHS)
  ```
Otherwise, if you are using the mobile api, select the registration status of your session:
- Creates a new session from a registered phone number: this means that the OTP was already sent to Whatsapp
  ```java
  .registered()
  ```
- Creates a new session from an unverified phone number: this means that the OTP was already sent to the companion as an SMS/Call, but that it hasn't been sent to Whatsapp yet
  ```java
  .unverified()
  ```
  Finally, use:
  ```java
  .verify(this::getOTPLogic)
  ```
  to verify the account (this doesn't create a connection to Whatsapp's Socket, it uses the HTTP api)
- Creates a new session from an unregistered phone number: this means that the OTP wasn't sent to the companion as an SMS/Call and that it wasn't forwarded Whatsapp
  ```java
  .unregistered()
  ```
  Finally, use:
  ```java
  .register(phoneNumber, this::getOTPLogic)
  ```
  to register the account (this doesn't create a connection to Whatsapp's Socket, it uses the HTTP api)

Finally, use
  ```java
  .connect()
  ```
to connect to Whatsapp.
Remember to handle the result using, for example, `join` to await the connection's result.
If you want the current thread to wait for the connection to be closed, use `awaitDisconnection`.

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

   Or to register it automatically using the `@RegisterListener` annotation:

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
                .join();
   ```

### How to handle serialization

In the original version of WhatsappWeb, chats, contacts and messages could be queried at any from Whatsapp's servers.
The multi-device implementation, instead, sends all of this information progressively when the connection is initialized for the first time and doesn't allow any subsequent queries to access the latter.
In practice, this means that this data needs to be serialized somewhere.
The same is true for the mobile api.

By default, this library serializes data regarding a session at `$HOME/.whatsapp4j/[web|mobile]/<session_id>` in two different files, respectively for the store(chats, contacts and messages) and keys(cryptographic data).
The latter is serialized every time a modification occurs to the model, while the store is serialized everytime a ping is sent by the socket to the server.
Both are serialized when the socket is closed.

If your application needs to serialize data in a different way, for example in a database create a custom implementation of ControllerSerializer.
Then make sure to specify your implementation in the `Whatsapp` builder.
This is explained in the "How to create a connection" section.

### How to handle session disconnects

When the session is closed, the onDisconnect method in any listener is invoked.
These are the three reasons that can cause a disconnect:

1. DISCONNECTED

    A normal disconnection.
    This doesn't indicate any error being thrown.

2. RECONNECT

    The client is being disconnected but only to reopen the connection.
    This always happens when the QR is first scanned for example.

3. LOGGED_OUT

    The client was logged out by itself or by its companion.
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

- Text

    ```java
    api.sendMessage(chat,  "This is a text message!");
    ```

- Complex text

    ```java
    var message = TextMessage.builder() // Create a new text message
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
    var location = LocationMessage.builder() // Create a new location message
            .caption("Look at this!") // Set the caption of the message, that is the text below the file
            .latitude(38.9193) // Set the longitude of the location to share
            .longitude(1183.1389) // Set the latitude of the location to share
            .build(); // Create the message
    api.sendMessage(chat, location);
    ```

- Live location

    ```java
    var location = LiveLocationMessage.builder() // Create a new live location message
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
    var groupInvite = GroupInviteMessage.builder() // Create a new group invite message
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
     var vcard = ContactCard.builder() // Create a new vcard
            .name("A nice friend") // Set the name of the contact
            .phoneNumber(contact) // Set the phone number of the contact
            .build(); // Create the vcard
    var contactMessage = ContactMessage.builder()  // Create a new contact message
            .name("A nice friend") // Set the display name of the contact
            .vcard(vcard) // Set the vcard(https://en.wikipedia.org/wiki/VCard) of the contact
            .build(); // Create the message
    api.sendMessage(chat, contactMessage);
    ```

- Contact array

    ```java
    var contactsMessage = ContactsArrayMessage.builder()  // Create a new contacts array message
            .name("A nice friend") // Set the display name of the first contact that this message contains
            .contacts(List.of(jack,lucy,jeff)) // Set a list of contact messages that this message wraps
            .build(); // Create the message
    api.sendMessage(chat, contactsMessage);
    ```

- Button
   
   Here is how you create a button:
   ```java
   var button = Button.of("A nice button!"); // Create a button
   var anotherButton = Button.of("Another button :)"); // Create another button with different text
   ```
  
   Buttons can be used in several messages:
   
   - Response button

     - Empty header

          ```java
          var buttons = ButtonsMessage.simpleBuilder() // Create a new button message builder
               .body("A nice body") // Set the body
               .footer("A nice footer") // Set the footer
               .buttons(List.of(button, anotherButton)) // Set the buttons
               .build(); // Create the message
          api.sendMessage(contact, emptyButtons);
          ```

     - Text header

          ```java
          var buttons = ButtonsMessage.simpleBuilder() // Create a new button message builder
               .header(TextMessage.of("A nice header :)")) // Set the header
               .body("A nice body") // Set the body
               .footer("A nice footer") // Set the footer
               .buttons(List.of(button, anotherButton)) // Set the buttons
               .build(); // Create the message
          api.sendMessage(contact, buttons);
          ```
  
     - Document header

          ```java
          var buttons = ButtonsMessage.simpleBuilder() // Create a new button message builder
               .header(documentMessage) // Set the header
               .body("A nice body") // Set the body
               .footer("A nice footer") // Set the footer
               .buttons(List.of(button, anotherButton)) // Set the buttons
               .build(); // Create the message
          api.sendMessage(contact, buttons);
          ```
  
     - Image header

          ```java
          var buttons = ButtonsMessage.simpleBuilder() // Create a new button message builder
               .header(imageMessage) // Set the header
               .body("A nice body") // Set the body
               .footer("A nice footer") // Set the footer
               .buttons(List.of(button, anotherButton)) // Set the buttons
               .build(); // Create the message
          api.sendMessage(contact, buttons);
          ```
  
     - Video header

          ```java
          var buttons = ButtonsMessage.simpleBuilder() // Create a new button message builder
               .header(videoMessage) // Set the header
               .body("A nice body") // Set the body
               .footer("A nice footer") // Set the footer
               .buttons(List.of(button, anotherButton)) // Set the buttons
               .build(); // Create the message
          api.sendMessage(contact, buttons);
          ```

     - Location header

          ```java
          var buttons = ButtonsMessage.simpleBuilder() // Create a new button message builder
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
     var listMessage = ListMessage.builder() // Create a list message builder
           .sections(List.of(section, anotherSection)) // Set the sections
           .button("Click me") // Set the button name that opens the menu
           .title("A nice title") // Set the title of the message
           .description("A nice description") // Set the description of the message
           .footer("A nice footer") // Set the footer of the message
           .type(ListMessage.Type.SINGLE_SELECT) // Set the type of the message
           .build(); // Create a list message
     api.sendMessage(contact, listMessage);
    ```

  - Template button
     ```java
     var highlyStructuredQuickReplyButton = HydratedButtonTemplate.of(HydratedQuickReplyButton.of("Click me!")); // Create a quick reply button
     var highlyStructuredUrlButton = HydratedButtonTemplate.of(HydratedURLButton.of("Search it", "https://google.com")); // Create an url button
     var highlyStructuredCallButton = HydratedButtonTemplate.of(HydratedCallButton.of("Call me", "some_phone_number")); // Create a call button
     var highlyStructuredFourRowTemplate = HydratedFourRowTemplate.simpleBuilder() // Create a new template builder
           .title(TextMessage.of("A nice title")) // Set the title
           .body("A nice body") // Set the body
           .buttons(List.of(highlyStructuredQuickReplyButton, highlyStructuredUrlButton, highlyStructuredCallButton)) // Set the buttons
           .build(); // Create the template
     var templateMessage = TemplateMessage.of(highlyStructuredFourRowTemplate); // Create a template message
     api.sendMessage(contact, templateMessage);
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
     var image = ImageMessage.simpleBuilder() // Create a new image message builder
           .media(media) // Set the image of this message
           .caption("A nice image") // Set the caption of this message
           .build(); // Create the message
     api.sendMessage(chat,  image);
     ```

  - Audio or voice

    ```java
     var audio = AudioMessage.simpleBuilder() // Create a new audio message builder
           .media(urlMedia) // Set the audio of this message
           .voiceMessage(false) // Set whether this message is a voice message
           .build(); // Create the message
     api.sendMessage(chat,  audio);
    ```

  -  Video

     ```java
     var video = VideoMessage.simpleVideoBuilder() // Create a new video message builder
           .media(urlMedia) // Set the video of this message
           .caption("A nice video") // Set the caption of this message
           .width(100) // Set the width of the video
           .height(100) // Set the height of the video
           .build(); // Create the message
     api.sendMessage(chat,  video); 
     ```
     
  -  GIF(Video)

     ```java
     var gif = VideoMessage.simpleGifBuilder() // Create a new gif message builder
           .media(urlMedia) // Set the gif of this message
           .caption("A nice video") // Set the caption of this message
           .gifAttribution(VideoMessageAttribution.TENOR) // Set the source of the gif
           .build(); // Create the message
     api.sendMessage(chat,  gif);
     ```
     > **_IMPORTANT:_** Whatsapp doesn't support conventional gifs. Instead, videos can be played as gifs if particular attributes are set. This is the reason why the gif builder is under the VideoMessage class. Sending a conventional gif will result in an exception if detected or in undefined behaviour.

  -  Document

     ```java
     var document = DocumentMessage.simpleBuilder() // Create a new document message builder
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
var status = api.queryPicture(contact) // A completable future
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
var future = api.mute(chat);
```

##### Unmute a chat

``` java
var future = api.mute(chat);
```

##### Archive a chat

``` java
var future = api.archive(chat);
```

##### Unarchive a chat

``` java
var future = api.unarchive(chat);
```

##### Change ephemeral message status in a chat

``` java
var future = api.changeEphemeralTimer(chat,  ChatEphemeralTimer.ONE_WEEK);
```   

##### Mark a chat as read

``` java
var future = api.markAsRead(chat);
```   

##### Mark a chat as unread

``` java
var future = api.markAsUnread(chat);
```   

##### Pin a chat

``` java
var future = api.pin(chat);
``` 

##### Unpin a chat

``` java
var future = api.unpin(chat);
```

##### Clear a chat

``` java
var future = api.clear(chat);
```

> **_IMPORTANT:_** This method is experimental and may not work

##### Delete a chat

``` java
var future = api.delete(chat);
```

> **_IMPORTANT:_** This method is experimental and may not work

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

##### Change who can send messages in a group

``` java
var future = api.changeWhoCanSendMessages(group, GroupPolicy.ANYONE);
```

##### Change who can edit the metadata/settings in a group

``` java
var future = api.changeWhoCanEditInfo(group, GroupPolicy.ANYONE);
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
var future = api.revokeGroupInviteCode(group);
```

##### Query a group's invite code

``` java
var future = api.acceptGroupInvite(inviteCode);
```

### Companions (Mobile api only)

### Link a companion

``` java
var future = api.linkCompanion(qrCode);
```

### Unlink a companion

``` java
var future = api.unlinkCompanion(companionJid);
```

### Unlink all companions

``` java
var future = api.unlinkCompanions();
```

Some methods may not be listed here, all contributions are welcomed to this documentation!
Some methods may not be supported on the mobile api, please report them so I can fix them.
Ideally I'd like all of them to work.
