package it.auties.whatsapp.test;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.api.DisconnectReason;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappOptions.WebOptions;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.github.GithubActions;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.business.BusinessCollection;
import it.auties.whatsapp.model.business.BusinessNativeFlow;
import it.auties.whatsapp.model.business.BusinessShop;
import it.auties.whatsapp.model.button.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.button.ButtonsMessage;
import it.auties.whatsapp.model.message.button.InteractiveMessage;
import it.auties.whatsapp.model.message.button.ListMessage;
import it.auties.whatsapp.model.message.button.TemplateMessage;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.TextMessage;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.utils.ConfigUtils;
import it.auties.whatsapp.utils.MediaUtils;
import it.auties.whatsapp.utils.Smile;
import lombok.SneakyThrows;
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
import java.util.stream.IntStream;

// A mirror of RunCITest for buttons
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class ButtonsTest implements Listener {
    private static Whatsapp api;
    private static CompletableFuture<Void> future;
    private static CountDownLatch latch;
    private static ContactJid contact;
    private static ContactJid group;
    private static boolean skip;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeAll
    public void init() throws IOException, InterruptedException {
        createApi();
        if (skip) {
            return;
        }
        loadConfig();
        createLatch();
        future = api.connect().thenComposeAsync(Whatsapp::onDisconnected);
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
            api = Whatsapp.lastConnection();
            api.addListener(this);
            return;
        }
        log("Detected github actions environment");
        api = Whatsapp.newConnection(WebOptions.defaultOptions(), loadGithubParameter(GithubActions.STORE_NAME, Store.class), loadGithubParameter(GithubActions.CREDENTIALS_NAME, Keys.class));
        api.addListener(this);
    }

    @SneakyThrows
    private <T> T loadGithubParameter(String parameter, Class<T> type) {
        var passphrase = System.getenv(GithubActions.GPG_PASSWORD);
        var path = Path.of("ci/%s.gpg".formatted(parameter));
        var decrypted = ByteArrayHandler.decrypt(Files.readAllBytes(path), passphrase.toCharArray());
        return Smile.readValue(decrypted, type);
    }

    private void loadConfig() throws IOException {
        if (GithubActions.isActionsEnvironment()) {
            log("Loading environment variables...");
            contact = ContactJid.of(System.getenv(GithubActions.CONTACT_NAME));
            log("Loaded environment variables...");
            return;
        }
        log("Loading configuration file...");
        var props = ConfigUtils.loadConfiguration();
        contact = ContactJid.of(Objects.requireNonNull(props.getProperty("contact"), "Missing contact property in config"));
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

    @SuppressWarnings("HttpUrlsUsage")
    @Test
    @Order(29)
    public void testButtonsMessage() {
        if (skip) {
            return;
        }
        log("Sending buttons...");
        var emptyButtons = ButtonsMessage.simpleBuilder()
                .body("A nice body")
                .footer("A nice footer")
                .buttons(createButtons())
                .build();
        api.sendMessage(contact, emptyButtons).join();
        var textButtons = ButtonsMessage.simpleBuilder()
                .header(TextMessage.of("A nice header"))
                .body("A nice body")
                .footer("A nice footer")
                .buttons(createButtons())
                .build();
        api.sendMessage(contact, textButtons).join();
        var document = DocumentMessage.simpleBuilder()
                .media(MediaUtils.readBytes("http://www.orimi.com/pdf-test.pdf"))
                .title("Pdf test")
                .fileName("pdf-test.pdf")
                .pageCount(1)
                .build();
        var documentButtons = ButtonsMessage.simpleBuilder()
                .header(document)
                .body("A nice body")
                .footer("A nice footer")
                .buttons(createButtons())
                .build();
        api.sendMessage(contact, documentButtons).join();
        var image = ImageMessage.simpleBuilder()
                .media(MediaUtils.readBytes("https://2.bp.blogspot.com/-DqXILvtoZFA/Wmmy7gRahnI/AAAAAAAAB0g/59c8l63QlJcqA0591t8-kWF739DiOQLcACEwYBhgL/s1600/pol-venere-botticelli-01.jpg"))
                .caption("Image test")
                .build();
        var imageButtons = ButtonsMessage.simpleBuilder()
                .header(image)
                .body("A nice body")
                .footer("A nice footer")
                .buttons(createButtons())
                .build();
        api.sendMessage(contact, imageButtons).join();
        log("Sent buttons");
    }

    private List<Button> createButtons() {
        return IntStream.range(0, 3).mapToObj("Button %s"::formatted).map(Button::of).toList();
    }

    @Test
    @Order(41)
    public void testInteractiveMessage() {
        if (skip) {
            return;
        }
        log("Sending interactive messages..");
        var collectionMessage = BusinessCollection.builder()
                .business(ContactJid.of("15086146312@s.whatsapp.net"))
                .id("15086146312")
                .version(3)
                .build();
        var interactiveMessageWithCollection = InteractiveMessage.simpleBuilder()
                .content(collectionMessage)
                .build();
        api.sendMessage(contact, interactiveMessageWithCollection).join();
        var shopMessage = BusinessShop.builder()
                .id(Bytes.ofRandom(5).toHex())
                .version(3)
                .surfaceType(BusinessShop.SurfaceType.WHATSAPP)
                .build();
        var interactiveMessageWithShop = InteractiveMessage.simpleBuilder().content(shopMessage).build();
        api.sendMessage(contact, interactiveMessageWithShop).join();
        var nativeFlowMessage = BusinessNativeFlow.builder()
                .buttons(List.of(NativeFlowButton.of("hello :)", "")))
                .version(3)
                .parameters("")
                .build();
        var interactiveMessageWithFlow = InteractiveMessage.simpleBuilder()
                .content(nativeFlowMessage)
                .build();
        api.sendMessage(contact, interactiveMessageWithFlow).join();
        log("Sent interactive messages");
    }

    @Test
    @Order(42)
    public void testTemplateMessage() {
        if (skip) {
            return;
        }
        log("Sending template message...");
        var quickReplyButton = HydratedTemplateButton.of(HydratedQuickReplyButton.of("Click me"));
        var urlButton = HydratedTemplateButton.of(HydratedURLButton.of("Search it", "https://google.com"));
        var callButton = HydratedTemplateButton.of(HydratedCallButton.of("Call me", contact.toPhoneNumber()));
        var fourRowTemplate = HydratedFourRowTemplate.simpleBuilder()
                .body("A nice body")
                .footer("A nice footer")
                .buttons(List.of(quickReplyButton, urlButton, callButton))
                .build();
        var templateMessage = TemplateMessage.of(fourRowTemplate);
        api.sendMessage(contact, templateMessage).join();
        log("Sent template message");
    }

    @Test
    @Order(43)
    public void testListMessage() {
        if (skip) {
            return;
        }
        var buttons = List.of(ButtonRow.of("First option", "A nice description"), ButtonRow.of("Second option", "A nice description"), ButtonRow.of("Third option", "A nice description"));
        var section = ButtonSection.of("First section", buttons);
        var otherButtons = List.of(ButtonRow.of("First option", "A nice description"), ButtonRow.of("Second option", "A nice description"), ButtonRow.of("Third option", "A nice description"));
        var anotherSection = ButtonSection.of("First section", otherButtons);
        var listMessage = ListMessage.builder()
                .sections(List.of(section, anotherSection))
                .button("Click me")
                .title("A nice title")
                .description("A nice description")
                .footer("A nice footer")
                .listType(ListMessage.Type.SINGLE_SELECT)
                .build();
        var result = api.sendMessage(contact, listMessage).join();
        log("Sent list message: " + result);
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
    public void onChatMessagesSync(Chat chat, boolean last) {
        if (!last) {
            return;
        }
        System.out.printf("%s is ready with %s messages%n", chat.name(), chat.messages().size());
    }

    @Override
    public void onNewMessage(Whatsapp whatsapp, MessageInfo info) {
        System.out.println(info.toJson());
    }
}
