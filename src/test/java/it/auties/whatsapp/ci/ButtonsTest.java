package it.auties.whatsapp.ci;

import it.auties.whatsapp.api.DisconnectReason;
import it.auties.whatsapp.api.QrHandler;
import it.auties.whatsapp.api.WebHistoryLength;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.controller.KeysSpec;
import it.auties.whatsapp.controller.StoreSpec;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.GithubActions;
import it.auties.whatsapp.model.button.base.Button;
import it.auties.whatsapp.model.button.base.ButtonText;
import it.auties.whatsapp.model.button.interactive.InteractiveButton;
import it.auties.whatsapp.model.button.interactive.InteractiveHeaderSimpleBuilder;
import it.auties.whatsapp.model.button.interactive.InteractiveNativeFlowBuilder;
import it.auties.whatsapp.model.button.misc.ButtonRow;
import it.auties.whatsapp.model.button.misc.ButtonSection;
import it.auties.whatsapp.model.button.template.hydrated.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.info.ChatMessageInfoBuilder;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.ChatMessageKeyBuilder;
import it.auties.whatsapp.model.message.model.MessageContainerBuilder;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.message.standard.TextMessage;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.util.ConfigUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.examples.ByteArrayHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;

// A mirror of RunCITest for buttons
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class ButtonsTest implements Listener {
    private static Whatsapp api;
    private static CompletableFuture<?> future;
    private static CountDownLatch latch;
    private static Jid contact;
    private static boolean skip;

    @BeforeAll
    public void init() throws IOException, InterruptedException {
        createLatch();
        createApi();
        if (skip) {
            return;
        }
        loadConfig();
        future = api.connect();
        latch.await();
    }

    private void createApi() {
        log("Initializing api to start testing...");
        if (!GithubActions.isActionsEnvironment()) {
            if (GithubActions.isReleaseEnv()) {
                System.out.println("Skipping api test: detected local release environment");
                skip = true;
                return;
            }
            api = Whatsapp.webBuilder()
                    .lastConnection()
                    .historyLength(WebHistoryLength.zero())
                    .unregistered(QrHandler.toTerminal())
                    .addListener(this);
            return;
        }
        log("Detected github actions environment");
        api = Whatsapp.customBuilder()
                .store(loadGithubParameter(GithubActions.STORE_NAME, StoreSpec::decode))
                .keys(loadGithubParameter(GithubActions.CREDENTIALS_NAME, KeysSpec::decode))
                .build()
                .addListener(this);
    }

    private <T> T loadGithubParameter(String parameter, Function<byte[], T> reader) {
        try {
            var passphrase = System.getenv(GithubActions.GPG_PASSWORD);
            var path = Path.of("ci/%s.gpg".formatted(parameter));
            var decrypted = ByteArrayHandler.decrypt(Files.readAllBytes(path), passphrase.toCharArray());
            return reader.apply(decrypted);
        }catch (Throwable throwable) {
            throw new RuntimeException("Cannot read github parameter " + parameter, throwable);
        }
    }

    private void loadConfig() throws IOException {
        if (GithubActions.isActionsEnvironment()) {
            log("Loading environment variables...");
            contact = Jid.of(System.getenv(GithubActions.CONTACT_NAME));
            log("Loaded environment variables...");
            return;
        }
        log("Loading configuration file...");
        var props = ConfigUtils.loadConfiguration();
        contact = Jid.of(Objects.requireNonNull(props.getProperty("contact"), "Missing contact property in config"));
        log("Loaded configuration file");
    }

    private void createLatch() {
        latch = new CountDownLatch(3);
    }

    private void log(String message, Object... params) {
        System.out.printf(message + "%n", redactParameters(params));
    }

    private Object[] redactParameters(Object... params) {
        if (!GithubActions.isActionsEnvironment()) {
            return params;
        }
        return Arrays.stream(params).map(entry -> "***").toArray(String[]::new);
    }

    @Test
    @Order(1)
    public void testButtonsMessage() {
        if (skip) {
            return;
        }
        log("Sending buttons...");
        var imageButtons = new ButtonsMessageSimpleBuilder()
                .header(new ButtonsMessageHeaderText("Header"))
                .body("A nice body")
                .footer("A nice footer")
                .buttons(createButtons())
                .build();
        api.sendMessage(contact, imageButtons).join();
        log("Sent buttons");
    }

    private List<Button> createButtons() {
        return IntStream.range(0, 3)
                .mapToObj(index -> new ButtonText("Button %s".formatted(index)))
                .map(Button::of)
                .toList();
    }

    @Test
    @Order(2)
    public void testListMessage() {
        if (skip) {
            return;
        }
        var buttons = List.of(ButtonRow.of("First option", "A nice description"), ButtonRow.of("Second option", "A nice description"), ButtonRow.of("Third option", "A nice description"));
        var section = new ButtonSection("First section", buttons);
        var otherButtons = List.of(ButtonRow.of("First option", "A nice description"), ButtonRow.of("Second option", "A nice description"), ButtonRow.of("Third option", "A nice description"));
        var anotherSection = new ButtonSection("First section", otherButtons);
        var listMessage = new ListMessageBuilder()
                .sections(List.of(section, anotherSection))
                .button("Click me")
                .title("A nice title")
                .description("A nice description")
                .footer("A nice footer")
                .listType(ListMessage.Type.SINGLE_SELECT)
                .build();
        var container = new MessageContainerBuilder()
                .listMessage(listMessage)
                .textMessage(TextMessage.of("Test"))
                .build();
        var jid = api.store()
                .jid()
                .orElseThrow();
        var keyInfo = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId())
                .chatJid(contact)
                .senderJid(jid)
                .fromMe(true)
                .build();
        var messageInfo = new ChatMessageInfoBuilder()
                .status(MessageStatus.PENDING)
                .key(keyInfo)
                .senderJid(jid)
                .message(container)
                .build();
        var result = api.sendMessage(messageInfo).join();
        log("Sent list message: " + result);
    }

    @Test
    @Order(3)
    public void testTemplateMessage() {
        if (skip) {
            return;
        }
        log("Sending template message...");
        var quickReplyButton = HydratedTemplateButton.of(HydratedQuickReplyButton.of("Click me"));
        var urlButton = HydratedTemplateButton.of(new HydratedURLButton("Search it", "https://google.com"));
        var callButton = HydratedTemplateButton.of(new HydratedCallButton("Call me", contact.toPhoneNumber()));
        var fourRowTemplate = new HydratedFourRowTemplateSimpleBuilder()
                .body("A nice body")
                .footer("A nice footer")
                .buttons(List.of(quickReplyButton, urlButton, callButton))
                .build();
        var template = new TemplateMessageSimpleBuilder()
                .format(fourRowTemplate)
                .build();
        api.sendMessage(contact, template).join();
        log("Sent template message");
    }

    // Just have a test to see if it gets sent, it's not actually a functioning button because it's designed for more complex use cases
    @Test
    @Order(4)
    public void testInteractiveMessage() {
        if (skip) {
            return;
        }
        log("Sending interactive messages..");
        var nativeFlowMessage = new InteractiveNativeFlowBuilder()
                .buttons(List.of(new InteractiveButton("review_and_pay"), new InteractiveButton("review_order")))
                .build();
        var nativeHeader = new InteractiveHeaderSimpleBuilder()
                .title("Title")
                .subtitle("Subtitle")
                .build();
        var interactiveMessageWithFlow = new InteractiveMessageSimpleBuilder()
                .header(nativeHeader)
                .content(nativeFlowMessage)
                .footer("Footer")
                .build();
        api.sendMessage(contact, interactiveMessageWithFlow).join();
        log("Sent interactive messages");
    }

    @SuppressWarnings("JUnit3StyleTestMethodInJUnit4Class")
    @AfterAll
    public void testDisconnect() {
        if (skip) {
            return;
        }
        log("Logging off...");
        CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES).execute(api::disconnect);
        future.join();
        log("Logged off");
    }

    @Override
    public void onNodeSent(Node outgoing) {
        System.out.printf("Sent node %s%n", outgoing);
    }

    @Override
    public void onNodeReceived(Node incoming) {
        System.out.printf("Received node %s%n", incoming);
    }

    @Override
    public void onLoggedIn() {
        latch.countDown();
        log("Logged in: -%s", latch.getCount());
    }

    @Override
    public void onDisconnected(DisconnectReason reason) {
        System.out.printf("Disconnected: %s%n", reason);
    }

    @Override
    public void onContacts(Collection<Contact> contacts) {
        latch.countDown();
        log("Got contacts: -%s", latch.getCount());
    }

    @Override
    public void onChats(Collection<Chat> chats) {
        latch.countDown();
        log("Got chats: -%s", latch.getCount());
    }

    @Override
    public void onChatMessagesSync(Chat contact, boolean last) {
        if (!last) {
            return;
        }
        System.out.printf("%s is ready with %s messages%n", contact.name(), contact.messages().size());
    }

    @Override
    public void onNewMessage(Whatsapp whatsapp, MessageInfo info) {
        System.out.println(info.toJson());
    }
}
