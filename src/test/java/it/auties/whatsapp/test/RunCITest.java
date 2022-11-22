package it.auties.whatsapp.test;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.github.GithubActions;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.business.BusinessCollection;
import it.auties.whatsapp.model.business.BusinessNativeFlow;
import it.auties.whatsapp.model.business.BusinessShop;
import it.auties.whatsapp.model.button.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.chat.ChatMute;
import it.auties.whatsapp.model.chat.GroupPolicy;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactCard;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.button.ButtonsMessage;
import it.auties.whatsapp.model.message.button.ListMessage;
import it.auties.whatsapp.model.message.button.TemplateMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.standard.*;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.utils.ConfigUtils;
import it.auties.whatsapp.utils.MediaUtils;
import lombok.SneakyThrows;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.examples.ByteArrayHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class RunCITest implements Listener, JacksonProvider {
    private static Whatsapp api;
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
        if(skip){
            return;
        }

        loadConfig();
        createLatch();
        api.connect();
        latch.await();
    }

    private void createApi() {
        log("Initializing api to start testing...");
        if (!GithubActions.isActionsEnvironment()) {
            System.out.println("Skipping api test: detected local environment");
            skip = true;
            return;
        }

        log("Detected github actions environment");
        api = Whatsapp.newConnection(Whatsapp.Options.defaultOptions(),
                loadGithubParameter(GithubActions.STORE_NAME, Store.class),
                loadGithubParameter(GithubActions.CREDENTIALS_NAME, Keys.class));
        api.addListener(this);
    }

    @SneakyThrows
    private <T> T loadGithubParameter(String parameter, Class<T> type) {
        var passphrase = System.getenv(GithubActions.GPG_PASSWORD);
        var path = Path.of("ci/%s.gpg".formatted(parameter));
        var decrypted = ByteArrayHandler.decrypt(
                Files.readAllBytes(path),
                passphrase.toCharArray()
        );
        var input = new ByteArrayInputStream(decrypted);
        var gzip = new GZIPInputStream(input, 65536);
        return SMILE.readValue(gzip, type);
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
        contact = ContactJid.of(
                Objects.requireNonNull(props.getProperty("contact"), "Missing contact property in config"));
        log("Loaded configuration file");
    }

    private void createLatch() {
        latch = new CountDownLatch(3);
    }

    @Override
    public void onLoggedIn() {
        latch.countDown();
        log("Logged in: -%s", latch.getCount());
    }

    @Override
    public void onChats(Collection<Chat> chats) {
        latch.countDown();
        log("Got chats: -%s", latch.getCount());
    }

    @Override
    public void onContacts(Collection<Contact> contacts) {
        latch.countDown();
        log("Got contacts: -%s", latch.getCount());
    }

    @Test
    @Order(2)
    public void testChangeGlobalPresence() {
        if(skip){
            return;
        }

        log("Changing global presence...");
        api.changePresence(true)
                .join();
        log("Changed global presence...");
    }

    @Test
    @Order(3)
    public void testUserPresenceSubscription() {
        if(skip){
            return;
        }

        log("Subscribing to user presence...");
        var userPresenceResponse = api.subscribeToPresence(contact)
                .join();
        log("Subscribed to user presence: %s", userPresenceResponse);
    }

    @Test
    @Order(4)
    public void testPictureQuery() {
        if(skip){
            return;
        }

        log("Loading picture...");
        var picResponse = api.queryPicture(contact)
                .join();
        log("Loaded picture at: %s", picResponse);
    }

    @Test
    @Order(5)
    public void testStatusQuery() {
        if(skip){
            return;
        }

        log("Querying %s's status...", contact);
        api.queryStatus(contact)
                .join()
                .ifPresentOrElse(status -> log("Queried %s", status), () -> log("%s doesn't have a status", contact));
    }

    @Test
    @Order(8)
    public void testMarkChat() {
        if(skip){
            return;
        }

        markAsUnread();
        markAsRead();
    }

    private void markAsUnread() {
        log("Marking chat as unread...");
        var markStatus = api.markUnread(contact)
                .join();
        log("Marked chat as unread: %s", markStatus);
    }

    private void markAsRead() {
        log("Marking chat as read...");
        var markStatus = api.markRead(contact)
                .join();
        log("Marked chat as read: %s", markStatus);
    }

    @Test
    @Order(9)
    public void testGroupCreation() {
        if(skip){
            return;
        }

        log("Creating group...");
        var response = api.createGroup(Bytes.ofRandom(5)
                        .toHex(), contact)
                .join();
        group = response.jid();
        log("Created group: %s", response);
    }

    @Test
    @Order(10)
    public void testChangeIndividualPresence() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        for (var presence : ContactStatus.values()) {
            log("Changing individual presence to %s...", presence.name());
            var response = api.changePresence(group, presence)
                    .join();
            log("Changed individual presence: %s", response);
        }
    }

    @Test
    @Order(11)
    public void testChangeGroupName() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Changing group name...");
        var changeGroupResponse = api.changeGroupSubject(group, "omega")
                .join();
        log("Changed group name: %s", changeGroupResponse);
    }

    @RepeatedTest(2)
    @Order(12)
    public void testChangeGroupDescription() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Changing group description...");
        var changeGroupResponse = api.changeGroupDescription(group, Bytes.ofRandom(12)
                        .toHex())
                .join();
        log("Changed group description: %s", changeGroupResponse);
    }

    @Test
    @Order(13)
    public void testRemoveGroupParticipant() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Removing %s...", contact);
        var changeGroupResponse = api.removeGroupParticipant(group, contact)
                .join();
        log("Removed: %s", changeGroupResponse);
    }

    @Test
    @Order(14)
    public void testAddGroupParticipant() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Adding %s...", contact);
        var changeGroupResponse = api.addGroupParticipant(group, contact)
                .join();
        log("Added: %s", changeGroupResponse);
    }

    @Test
    @Order(15)
    public void testPromotion() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Promoting %s...", contact);
        var changeGroupResponse = api.promote(group, contact)
                .join();
        log("Promoted: %s", changeGroupResponse);
    }

    @Test
    @Order(16)
    public void testDemotion() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Demoting %s...", contact);
        var changeGroupResponse = api.demote(group, contact)
                .join();
        log("Demoted: %s", changeGroupResponse);
    }

    @Test
    @Order(17)
    public void testChangeAllGroupSettings() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        for (var policy : GroupPolicy.values()) {
            log("Changing settings to %s...", policy.name());
            api.changeWhoCanEditInfo(group, policy)
                    .join();
            api.changeWhoCanEditInfo(group, policy)
                    .join();
            log("Changed settings to %s", policy.name());
        }
    }

    @Test
    @Order(19)
    public void testGroupQuery() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Querying group %s...", group);
        api.queryGroupMetadata(group)
                .join();
        log("Queried group");
    }

    @Test
    @Order(20)
    public void testMute() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Muting chat...");
        var muteResponse = api.mute(group, ChatMute.mutedForOneWeek())
                .join();
        log("Muted chat: %s", muteResponse);
    }

    @Test
    @Order(21)
    public void testUnmute() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Unmuting chat...");
        var unmuteResponse = api.unmute(group)
                .join();
        log("Unmuted chat: %s", unmuteResponse);
    }

    @Test
    @Order(22)
    public void testArchive() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Archiving chat...");
        var archiveResponse = api.archive(group)
                .join();
        log("Archived chat: %s", archiveResponse);
    }

    @Test
    @Order(23)
    public void testUnarchive() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Unarchiving chat...");
        var unarchiveResponse = api.unarchive(group)
                .join();
        log("Unarchived chat: %s", unarchiveResponse);
    }

    @Test
    @Order(24)
    public void testPin() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        if (api.store()
                .pinnedChats()
                .size() >= 3) {
            log("Skipping chat pinning as there are already three chats pinned...");
            return;
        }

        log("Pinning chat...");
        var pinResponse = api.pin(group)
                .join();
        log("Pinned chat: %s", pinResponse);
    }

    @Test
    @Order(25)
    public void testUnpin() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        if (api.store()
                .pinnedChats()
                .size() >= 3) {
            log("Skipping chat unpinning as there are already three chats pinned...");
            return;
        }

        log("Unpinning chat...");
        var unpinResponse = api.unpin(group)
                .join();
        log("Unpinned chat: %s", unpinResponse);
    }

    @Test
    @Order(26)
    public void testTextMessage() {
        if(skip){
            return;
        }

        log("Sending simple text...");
        var simple = api.sendMessage(contact, "Hello")
                .join();
        log("Sent simple text: %s", simple);

        log("Sending youtube video...");
        var context = api.sendMessage(contact, "Hello: https://www.youtube.com/watch?v=4boXExbbGCk")
                .join();
        log("Sent youtube video: %s", context);

        log("Sending article...");
        var another = api.sendMessage(contact, "Hello: it.wikipedia.org/wiki/Vulcano") // Missing schema by design
                .join();
        log("Sent article: %s", another);
    }

    @Test
    @Order(27)
    public void deleteMessage() {
        if(skip){
            return;
        }

        var example = api.sendMessage(contact, "Hello")
                .join();

        log("Deleting for you...");
        api.delete(example, false)
                .join();
        log("Deleted for you");

        log("Deleting for everyone...");
        api.delete(example, true)
                .join();
        log("Deleted for everyone");
    }

    @SuppressWarnings("HttpUrlsUsage")
    @Test
    @Order(28)
    public void testButtonsMessage() {
        if(skip){
            return;
        }

        if(group == null){
            testGroupCreation();
        }

        log("Sending buttons...");
        var emptyButtons = ButtonsMessage.newButtonsWithoutHeaderMessageBuilder()
                .body("A nice body")
                .footer("A nice footer")
                .buttons(createButtons())
                .build();
        api.sendMessage(group, emptyButtons)
                .join();

        var textButtons = ButtonsMessage.newButtonsWithTextHeaderMessageBuilder()
                .header("A nice header")
                .body("A nice body")
                .footer("A nice footer")
                .buttons(createButtons())
                .build();
        api.sendMessage(group, textButtons)
                .join();

        var document = DocumentMessage.newDocumentMessageBuilder()
                .mediaConnection(api.store()
                        .mediaConnection())
                .media(MediaUtils.readBytes("http://www.orimi.com/pdf-test.pdf"))
                .title("Pdf test")
                .fileName("pdf-test.pdf")
                .pageCount(1)
                .build();
        var documentButtons = ButtonsMessage.newButtonsWithDocumentHeaderMessageBuilder()
                .header(document)
                .body("A nice body")
                .footer("A nice footer")
                .buttons(createButtons())
                .build();
        api.sendMessage(group, documentButtons)
                .join();

        var image = ImageMessage.newImageMessageBuilder()
                .mediaConnection(api.store()
                        .mediaConnection())
                .media(MediaUtils.readBytes(
                        "https://2.bp.blogspot.com/-DqXILvtoZFA/Wmmy7gRahnI/AAAAAAAAB0g/59c8l63QlJcqA0591t8-kWF739DiOQLcACEwYBhgL/s1600/pol-venere-botticelli-01.jpg"))
                .caption("Image test")
                .build();
        var imageButtons = ButtonsMessage.newButtonsWithImageHeaderMessageBuilder()
                .header(image)
                .body("A nice body")
                .footer("A nice footer")
                .buttons(createButtons())
                .build();
        api.sendMessage(group, imageButtons)
                .join();

        log("Sent buttons");
    }

    private List<Button> createButtons() {
        return IntStream.range(0, 3)
                .mapToObj("Button %s"::formatted)
                .map(Button::newResponseButton)
                .toList();
    }

    @Test
    @Order(29)
    public void testImageMessage() {
        if(skip){
            return;
        }

        log("Sending image...");
        var image = ImageMessage.newImageMessageBuilder()
                .mediaConnection(api.store()
                        .mediaConnection())
                .media(MediaUtils.readBytes(
                        "https://2.bp.blogspot.com/-DqXILvtoZFA/Wmmy7gRahnI/AAAAAAAAB0g/59c8l63QlJcqA0591t8-kWF739DiOQLcACEwYBhgL/s1600/pol-venere-botticelli-01.jpg"))
                .caption("Image test")
                .build();
        var textResponse = api.sendMessage(contact, image)
                .join();
        log("Sent image: %s", textResponse);
    }

    @Test
    @Order(30)
    public void testAudioMessage() {
        if(skip){
            return;
        }

        log("Sending audio...");
        var audio = AudioMessage.newAudioMessageBuilder()
                .mediaConnection(api.store()
                        .mediaConnection())
                .media(MediaUtils.readBytes("https://www.kozco.com/tech/organfinale.mp3"))
                .build();
        var textResponse = api.sendMessage(contact, audio)
                .join();
        log("Sent audio: %s", textResponse);
    }

    @SuppressWarnings("HttpUrlsUsage")
    @Test
    @Order(31)
    public void testVideoMessage() {
        if(skip){
            return;
        }

        log("Sending video...");
        var video = VideoMessage.newVideoMessageBuilder()
                .mediaConnection(api.store()
                        .mediaConnection())
                .media(MediaUtils.readBytes("http://techslides.com/demos/sample-videos/small.mp4"))
                .caption("Video")
                .build();
        var textResponse = api.sendMessage(contact, video)
                .join();
        log("Sent video: %s", textResponse);
    }

    @SuppressWarnings("HttpUrlsUsage")
    @Test
    @Order(32)
    public void testGifMessage() {
        if(skip){
            return;
        }

        log("Sending gif...");
        var video = VideoMessage.newGifMessageBuilder()
                .mediaConnection(api.store()
                        .mediaConnection())
                .media(MediaUtils.readBytes("http://techslides.com/demos/sample-videos/small.mp4"))
                .caption("Gif")
                .build();
        var textResponse = api.sendMessage(contact, video)
                .join();
        log("Sent video: %s", textResponse);
    }

    @SuppressWarnings("HttpUrlsUsage")
    @Test
    @Order(33)
    public void testPdfMessage() {
        if(skip){
            return;
        }

        log("Sending pdf...");
        var document = DocumentMessage.newDocumentMessageBuilder()
                .mediaConnection(api.store()
                        .mediaConnection())
                .media(MediaUtils.readBytes("http://www.orimi.com/pdf-test.pdf"))
                .title("Pdf test")
                .fileName("pdf-test.pdf")
                .pageCount(1)
                .build();
        var textResponse = api.sendMessage(contact, document)
                .join();
        log("Sent pdf: %s", textResponse);
    }

    @Test
    @Order(34)
    public void testContactMessage() {
        if(skip){
            return;
        }

        log("Sending contact message...");
        var vcard = ContactCard.newContactCardBuilder()
                .name("A nice contact")
                .phoneNumber(contact)
                .build();
        var contactMessage = ContactMessage.newContactMessage("A nice contact", vcard);
        var response = api.sendMessage(contact, contactMessage)
                .join();
        log("Sent contact: %s", response);
    }

    @Test
    @Order(35)
    public void testLocationMessage() {
        if(skip){
            return;
        }

        log("Sending location message...");
        var location = LocationMessage.newLocationMessageBuilder()
                .latitude(40.730610)
                .longitude(-73.935242)
                .magneticNorthOffset(0)
                .build();
        var textResponse = api.sendMessage(contact, location)
                .join();
        log("Sent location: %s", textResponse);
    }

    @Test
    @Order(36)
    public void testGroupInviteMessage() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Querying group invite code");
        var code = api.queryGroupInviteCode(group)
                .join();
        log("Queried %s", code);

        log("Sending group invite message...");
        var invite = GroupInviteMessage.newGroupInviteMessageBuilder()
                .group(group)
                .code(code)
                .expiration(ZonedDateTime.now()
                        .plusDays(3)
                        .toEpochSecond())
                .groupName(group.user())
                .build();
        var textResponse = api.sendMessage(contact, invite)
                .join();
        log("Sent invite: %s", textResponse);
    }

    @Test
    @Order(37)
    public void testEnableEphemeralMessages() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Enabling ephemeral messages...");
        var ephemeralResponse = api.changeEphemeralTimer(group, ChatEphemeralTimer.ONE_WEEK)
                .join();
        log("Enabled ephemeral messages: %s", ephemeralResponse);
    }

    @Test
    @Order(38)
    public void testDisableEphemeralMessages() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Disabling ephemeral messages...");
        var ephemeralResponse = api.changeEphemeralTimer(group, ChatEphemeralTimer.OFF)
                .join();
        log("Disabled ephemeral messages: %s", ephemeralResponse);
    }

    @Test
    @Order(39)
    public void testLeave() {
        if(skip){
            return;
        }

        if (group == null) {
            testGroupCreation();
        }

        log("Leaving group...");
        var ephemeralResponse = api.leaveGroup(group)
                .join();
        log("Left group: %s", ephemeralResponse);
    }

    @Test
    @Order(40)
    public void testClearChat() {
        if(skip){
            return;
        }

        log("Clearing chat...");
        var ephemeralResponse = api.clear(contact, false)
                .join();
        log("Cleared chat: %s", ephemeralResponse);
    }

    @Test
    @Order(41)
    public void testDeleteChat() {
        if(skip){
            return;
        }

        log("Deleting chat...");
        var ephemeralResponse = api.delete(contact)
                .join();
        log("Deleted chat: %s", ephemeralResponse);
    }

    @Test
    @Order(42)
    public void testInteractiveMessage() { // These are not even supported as far as I can tell, though we have a test lol
        if(skip){
            return;
        }

        log("Sending interactive messages..");

        var collectionMessage = BusinessCollection.builder()
                .business(ContactJid.of("15086146312@s.whatsapp.net"))
                .id("15086146312")
                .version(3)
                .build();
        var interactiveMessageWithCollection = InteractiveMessage.newInteractiveWithCollectionMessageBuilder()
                .content(collectionMessage)
                .build();
        api.sendMessage(contact, interactiveMessageWithCollection)
                .join();

        var shopMessage = BusinessShop.newShopBuilder()
                .id(Bytes.ofRandom(5)
                        .toHex())
                .version(3)
                .surfaceType(BusinessShop.SurfaceType.WHATSAPP)
                .build();
        var interactiveMessageWithShop = InteractiveMessage.newInteractiveWithShopMessageBuilder()
                .content(shopMessage)
                .build();
        api.sendMessage(contact, interactiveMessageWithShop)
                .join();

        var nativeFlowMessage = BusinessNativeFlow.newNativeFlow()
                .buttons(List.of(NativeFlowButton.of("hello :)", "")))
                .version(3)
                .parameters("")
                .build();
        var interactiveMessageWithFlow = InteractiveMessage.newInteractiveWithNativeFlowMessageBuilder()
                .content(nativeFlowMessage)
                .build();
        api.sendMessage(contact, interactiveMessageWithFlow)
                .join();

        log("Sent interactive messages");
    }

    @Test
    @Order(43)
    public void testTemplateMessage() {
        if(skip){
            return;
        }

        log("Sending template message...");
        var quickReplyButton = HydratedButtonTemplate.of(1, HydratedQuickReplyButton.of("Click me!", "random"));
        var urlButton = HydratedButtonTemplate.of(2, HydratedURLButton.of("Search it", "https://google.com"));
        var callButton = HydratedButtonTemplate.of(3,
                HydratedCallButton.of("Call me", "+%s".formatted(contact.user())));
        var fourRowTemplate = HydratedFourRowTemplate.newHydratedFourRowTemplateWithTextTitleBuilder()
                .title("A nice title")
                .body("A nice body")
                .buttons(List.of(quickReplyButton, urlButton, callButton))
                .build();
        var templateMessage = TemplateMessage.newHydratedTemplateMessage(fourRowTemplate);
        api.sendMessage(contact, templateMessage)
                .join();
        log("Sent template message");
    }

    @Test
    @Order(44)
    public void testListMessage() {
        if(skip){
            return;
        }

        var buttons = List.of(ButtonRow.of("First option", "A nice description"),
                ButtonRow.of("Second option", "A nice description"),
                ButtonRow.of("Third option", "A nice description"));
        var section = ButtonSection.of("First section", buttons);
        var otherButtons = List.of(ButtonRow.of("First option", "A nice description"),
                ButtonRow.of("Second option", "A nice description"),
                ButtonRow.of("Third option", "A nice description"));
        var anotherSection = ButtonSection.of("First section", otherButtons);
        var listMessage = ListMessage.newListMessageBuilder()
                .sections(List.of(section, anotherSection))
                .button("Click me")
                .title("A nice title")
                .description("A nice description")
                .footer("A nice footer")
                .type(ListMessage.Type.SINGLE_SELECT)
                .build();
        api.sendMessage(contact, listMessage)
                .join();
        log("Sent list message");
    }

    @Test
    @Order(45)
    public void testReaction() throws InterruptedException {
        if(skip){
            return;
        }

        var example = api.sendMessage(contact, "Hello")
                .join();

        log("Sending heart reaction...");
        var simple = api.sendReaction(example, "💖")
                .join();
        log("Sent heart reaction: %s", simple);

        Thread.sleep(2000L);

        log("Removing reaction...");
        var context = api.removeReaction(example)
                .join();
        log("Removed reaction: %s", context);
    }

    @Test
    @Order(46)
    public void testMediaDownload(){
        if(skip){
            return;
        }

        log("Trying to decode some medias...");
        var success = new AtomicInteger();
        var fail = new AtomicInteger();
        api.store()
                .chats()
                .stream()
                .map(Chat::messages)
                .flatMap(Collection::stream)
                .filter(info -> !info.fromMe() && info.message().category() == MessageCategory.MEDIA)
                .limit(30)
                .forEach(info -> {
                    try {
                        api.downloadMedia(info).join();
                        success.incrementAndGet();
                    }catch (Throwable throwable){
                        fail.incrementAndGet();
                    }
                });
        log("Decoded %s/%s medias!", success.get(), success.get() + fail.get());
    }

    @SuppressWarnings("JUnit3StyleTestMethodInJUnit4Class")
    @AfterAll
    public void testDisconnect() {
        if(skip){
            return;
        }

        log("Logging off...");
        CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES)
                .execute(api::disconnect);
        api.await();
        log("Logged off");
    }

    @Override
    public void onNewMessage(MessageInfo info) {
        System.out.printf("New message: %s%n", info);
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
    public void onChatMessagesSync(Chat chat, boolean last) {
        if (!last) {
            return;
        }

        System.out.printf("%s is ready with %s messages%n", chat.name(), chat.messages()
                .size());
    }

    @Override
    @SneakyThrows
    public void onNewMessage(Whatsapp whatsapp, MessageInfo info) {
        System.out.println(info.toJson());
    }

    private void log(String message, Object... params) {
        System.out.printf(message + "%n", redactParameters(params));
    }

    private Object[] redactParameters(Object... params) {
        if (!GithubActions.isActionsEnvironment()) {
            return params;
        }

        return Arrays.stream(params)
                .map(entry -> "***")
                .toArray(String[]::new);
    }
}
