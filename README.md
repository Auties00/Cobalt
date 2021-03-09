# WhatsappWeb4j

### What is WhatsappWeb4j

WhatsappWeb4j is a standalone library built to interact with [WhatsappWeb](https://web.whatsapp.com/). This means that no browser, application or
any additional software is necessary to use this library. This library was built for [Java 15](https://openjdk.java.net/projects/jdk/15/) and [JakartaEE 9](https://jakarta.ee/release/9/). 
Support for Java 11, the latest LTS as of this date, will come soon. Any help to this library welcomed as long as the coding style of the project is respected. 

### How to install 

#### Maven
Add this dependency to your dependencies in the pom:
```xml
<dependency>
    <groupId>it.auties</groupId>
    <artifactId>whatsappweb4j</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

#### Gradle
Add this dependency to your build.gradle:
```groovy
implementation 'it.auties:whatsappweb4j:1.0-SNAPSHOT'
```

### How to configure WhatsappWeb4j
To use this library, start by initializing an instance of WhatsappAPI like this:
```java
var api = new WhatsappAPI();
```
Alternatively, you can provide a custom configuration like this:
```java
var configuration = WhatsappConfiguration.builder()
        .whatsappTag("whatsapp")
        .async(false)
        .build()
var api = new WhatsappAPI(configuration);
```
Now register your listeners:
```java
api.registerListener(new YourAwesomeListener());
```
Alternatively, you can annotate your listeners with @RegisterListener:
```java
import it.auties.whatsapp4j.listener.RegisterListener;
import it.auties.whatsapp4j.listener.WhatsappListener;

@RegisterListener
public class YourAwesomeListener implements WhatsappListener {
    
}
```

> **_IMPORTANT:_**  Only listeners that provide a no arguments constructor can be annotated with @RegisterListener

then enable auto-detection:
```java
api.autodetectListeners();
```
Open a connection with WhatsappWeb's WebSocket using:
```java
api.connect();
```
To disconnect from WhatsappWeb's WebSocket use:
```java
api.disconnect();
```
To disconnect and then reconnect to WhatsappWeb's WebSocket use:
```java
api.reconnect();
```
To disconnect and invalidate the credentials linked with this session use:
```java
api.logout();
```