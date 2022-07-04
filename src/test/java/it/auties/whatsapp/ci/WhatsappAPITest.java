package it.auties.whatsapp.ci;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.github.GithubActions;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.chat.ChatMute;
import it.auties.whatsapp.model.chat.GroupPolicy;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.message.standard.*;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.utils.ConfigUtils;
import it.auties.whatsapp.utils.MediaUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class WhatsappAPITest implements Listener, JacksonProvider {
    private static Whatsapp api;
    private static CountDownLatch latch;
    private static ContactJid contact;
    private static ContactJid group;

    @BeforeAll
    public void init() throws IOException, InterruptedException {
        createApi();
        loadConfig();
        createLatch();
        api.connect();
        latch.await();
    }

    private void createApi() throws IOException {
        log("Initializing api to start testing...");
        if (!GithubActions.isActionsEnvironment()) {
            log("Detected local environment");
            api = Whatsapp.newConnection();
            api.addListener(this);
            return;
        }

        log("Detected github actions environment");
        api = Whatsapp.newConnection(Whatsapp.Options.defaultOptions(),
                loadGithubParameter(GithubActions.STORE_NAME, Store.class),
                loadGithubParameter(GithubActions.CREDENTIALS_NAME, Keys.class));
        api.addListener(this);
    }

    private <T> T loadGithubParameter(String parameter, Class<T> type) throws IOException {
        var keysJson = Base64.getDecoder()
                .decode(System.getenv(parameter));
        return JSON.readValue(keysJson, type);
    }

    private void loadConfig() throws IOException {
        if (GithubActions.isActionsEnvironment()) {
            log("Loading environment variables...");
            var jid = new String(Base64.getDecoder()
                    .decode(System.getenv(GithubActions.CONTACT_NAME)), StandardCharsets.UTF_8);
            contact = ContactJid.of(jid);
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
    public void onChats() {
        latch.countDown();
        log("Got chats: -%s", latch.getCount());
    }

    @Override
    public void onContacts() {
        latch.countDown();
        log("Got contacts: -%s", latch.getCount());
    }

    @Test
    @Order(2)
    public void testChangeGlobalPresence() throws Exception {
        log("Changing global presence...");
        api.changePresence(true)
                .get();
        log("Changed global presence...");
    }

    @Test
    @Order(3)
    public void testUserPresenceSubscription() throws Exception {
        log("Subscribing to user presence...");
        var userPresenceResponse = api.subscribeToPresence(contact)
                .get();
        log("Subscribed to user presence: %s", userPresenceResponse);
    }

    @Test
    @Order(4)
    public void testPictureQuery() throws Exception {
        log("Loading picture...");
        var picResponse = api.queryPic(contact)
                .get();
        log("Loaded picture at: %s", picResponse);
    }

    @Test
    @Order(5)
    public void testStatusQuery() throws Exception {
        log("Querying %s's status...", contact);
        api.queryStatus(contact)
                .get()
                .ifPresentOrElse(status -> log("Queried %s", status), () -> log("%s doesn't have a status", contact));
    }

    @Test
    @Order(8)
    public void testMarkChat() throws Exception {
        markAsUnread();
        markAsRead();
    }

    private void markAsUnread() throws Exception {
        log("Marking chat as unread...");
        var markStatus = api.markUnread(contact)
                .get();
        log("Marked chat as unread: %s", markStatus);
    }

    private void markAsRead() throws Exception {
        log("Marking chat as read...");
        var markStatus = api.markRead(contact)
                .get();
        log("Marked chat as read: %s", markStatus);
    }

    @Test
    @Order(9)
    public void testGroupCreation() throws Exception {
        log("Creating group...");
        var response = api.create(Bytes.ofRandom(5)
                        .toHex(), contact)
                .get();
        group = response.jid();
        log("Created group: %s", response);
    }

    @Test
    @Order(10)
    public void testChangeIndividualPresence() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        for (var presence : ContactStatus.values()) {
            log("Changing individual presence to %s...", presence.name());
            var response = api.changePresence(group, presence)
                    .get();
            log("Changed individual presence: %s", response);
        }
    }

    @Test
    @Order(11)
    public void testChangeGroupName() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Changing group name...");
        var changeGroupResponse = api.changeSubject(group, "omega")
                .get();
        log("Changed group name: %s", changeGroupResponse);
    }

    @RepeatedTest(2)
    @Order(12)
    public void testChangeGroupDescription() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Changing group description...");
        var changeGroupResponse = api.changeDescription(group, Bytes.ofRandom(12)
                        .toHex())
                .get();
        log("Changed group description: %s", changeGroupResponse);
    }

    @Test
    @Order(13)
    public void testRemoveGroupParticipant() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Removing %s...", contact);
        var changeGroupResponse = api.remove(group, contact)
                .get();
        log("Removed: %s", changeGroupResponse);
    }

    @Test
    @Order(14)
    public void testAddGroupParticipant() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Adding %s...", contact);
        var changeGroupResponse = api.add(group, contact)
                .get();
        log("Added: %s", changeGroupResponse);
    }

    @Test
    @Order(15)
    public void testPromotion() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Promoting %s...", contact);
        var changeGroupResponse = api.promote(group, contact)
                .get();
        log("Promoted: %s", changeGroupResponse);
    }

    @Test
    @Order(16)
    public void testDemotion() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Demoting %s...", contact);
        var changeGroupResponse = api.demote(group, contact)
                .get();
        log("Demoted: %s", changeGroupResponse);
    }

    @Test
    @Order(17)
    public void testChangeAllGroupSettings() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        for (var policy : GroupPolicy.values()) {
            log("Changing settings to %s...", policy.name());
            api.changeWhoCanEditInfo(group, policy)
                    .get();
            api.changeWhoCanEditInfo(group, policy)
                    .get();
            log("Changed settings to %s", policy.name());
        }
    }

    @Test
    @Order(19)
    public void testGroupQuery() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Querying group %s...", group);
        api.queryGroupMetadata(group)
                .get();
        log("Queried group");
    }

    @Test
    @Order(20)
    public void testMute() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Muting chat...");
        var muteResponse = api.mute(group, ChatMute.mutedForOneWeek())
                .get();
        log("Muted chat: %s", muteResponse);
    }

    @Test
    @Order(21)
    public void testUnmute() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Unmuting chat...");
        var unmuteResponse = api.unmute(group)
                .get();
        log("Unmuted chat: %s", unmuteResponse);
    }

    @Test
    @Order(22)
    public void testArchive() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Archiving chat...");
        var archiveResponse = api.archive(group)
                .get();
        log("Archived chat: %s", archiveResponse);
    }

    @Test
    @Order(23)
    public void testUnarchive() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Unarchiving chat...");
        var unarchiveResponse = api.unarchive(group)
                .get();
        log("Unarchived chat: %s", unarchiveResponse);
    }

    @Test
    @Order(24)
    public void testPin() throws Exception {
        if(group == null){
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
                .get();
        log("Pinned chat: %s", pinResponse);
    }

    @Test
    @Order(25)
    public void testUnpin() throws Exception {
        if(group == null){
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
                .get();
        log("Unpinned chat: %s", unpinResponse);
    }

    @Test
    @Order(26)
    public void testTextMessage() throws Exception {
        log("Sending text...");
        var textResponse = api.sendMessage(contact, "Hello :)")
                .get();
        log("Sent text: %s", textResponse);
    }

    @Test
    @Order(27)
    public void testImageMessage() throws Exception {
        log("Sending image...");
        var image = ImageMessage.newImageMessage()
                .storeId(api.store()
                        .id())
                .media(MediaUtils.readBytes(
                        "https://2.bp.blogspot.com/-DqXILvtoZFA/Wmmy7gRahnI/AAAAAAAAB0g/59c8l63QlJcqA0591t8-kWF739DiOQLcACEwYBhgL/s1600/pol-venere-botticelli-01.jpg"))
                .caption("Image test")
                .create();
        var textResponse = api.sendMessage(contact, image)
                .get();
        log("Sent image: %s", textResponse);
    }

    @Test
    @Order(28)
    public void testAudioMessage() throws Exception {
        log("Sending audio...");
        var audio = AudioMessage.newAudioMessage()
                .storeId(api.store()
                        .id())
                .media(MediaUtils.readBytes("https://www.kozco.com/tech/organfinale.mp3"))
                .create();
        var textResponse = api.sendMessage(contact, audio)
                .get();
        log("Sent audio: %s", textResponse);
    }

    @SuppressWarnings("HttpUrlsUsage")
    @Test
    @Order(29)
    public void testVideoMessage() throws Exception {
        log("Sending video...");
        var video = VideoMessage.newVideoMessage()
                .storeId(api.store()
                        .id())
                .media(MediaUtils.readBytes("http://techslides.com/demos/sample-videos/small.mp4"))
                .caption("Video")
                .create();
        var textResponse = api.sendMessage(contact, video)
                .get();
        log("Sent video: %s", textResponse);
    }

    @SuppressWarnings("HttpUrlsUsage")
    @Test
    @Order(30)
    public void testGifMessage() throws Exception {
        log("Sending gif...");
        var video = VideoMessage.newGifMessage()
                .storeId(api.store()
                        .id())
                .media(MediaUtils.readBytes("http://techslides.com/demos/sample-videos/small.mp4"))
                .caption("Gif")
                .create();
        var textResponse = api.sendMessage(contact, video)
                .get();
        log("Sent video: %s", textResponse);
    }

    @SuppressWarnings("HttpUrlsUsage")
    @Test
    @Order(31)
    public void testPdfMessage() throws Exception {
        log("Sending pdf...");
        var document = DocumentMessage.newDocumentMessage()
                .storeId(api.store()
                        .id())
                .media(MediaUtils.readBytes("http://www.orimi.com/pdf-test.pdf"))
                .title("Pdf test")
                .fileName("pdf-test.pdf")
                .pageCount(1)
                .create();
        var textResponse = api.sendMessage(contact, document)
                .get();
        log("Sent pdf: %s", textResponse);
    }

    @Test
    @Order(32)
    public void testContactMessage() throws Exception {
        log("Sending contact message...");
        var vcard = buildVcard();
        var document = ContactMessage.newContactMessage()
                .name("Test")
                .vcard(vcard)
                .create();
        var textResponse = api.sendMessage(contact, document)
                .get();
        log("Sent contact: %s", textResponse);
    }

    private String buildVcard() {
        return """
                BEGIN:VCARD
                VERSION:3.0
                N:%s
                FN:%s
                TEL;type=CELL:+%s
                END:VCARD
                """.formatted("Test", "Testing", contact.user());
    }

    @Test
    @Order(33)
    public void testLocationMessage() throws Exception {
        log("Sending location message...");
        var location = LocationMessage.newLocationMessage()
                .latitude(40.730610)
                .longitude(-73.935242)
                .magneticNorthOffset(0)
                .create();
        var textResponse = api.sendMessage(contact, location)
                .get();
        log("Sent location: %s", textResponse);
    }

    @Test
    @Order(34)
    public void testGroupInviteMessage() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Querying group invite code");
        var code = api.queryInviteCode(group)
                .get();
        log("Queried %s", code);

        log("Sending group invite message...");
        var invite = GroupInviteMessage.newGroupInviteMessage()
                .groupId(group)
                .code(code)
                .expiration(ZonedDateTime.now()
                        .plusDays(3)
                        .toEpochSecond())
                .name(group.user())
                .create();
        var textResponse = api.sendMessage(contact, invite)
                .get();
        log("Sent invite: %s", textResponse);
    }

    @Test
    @Order(35)
    public void testEnableEphemeralMessages() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Enabling ephemeral messages...");
        var ephemeralResponse = api.changeEphemeralTimer(group, ChatEphemeralTimer.ONE_WEEK)
                .get();
        log("Enabled ephemeral messages: %s", ephemeralResponse);
    }

    @Test
    @Order(36)
    public void testDisableEphemeralMessages() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Disabling ephemeral messages...");
        var ephemeralResponse = api.changeEphemeralTimer(group, ChatEphemeralTimer.OFF)
                .get();
        log("Disabled ephemeral messages: %s", ephemeralResponse);
    }

    @Test
    @Order(37)
    public void testLeave() throws Exception {
        if(group == null){
            testGroupCreation();
        }

        log("Leaving group...");
        var ephemeralResponse = api.leave(group)
                .get();
        log("Left group: %s", ephemeralResponse);
    }

    @SuppressWarnings("JUnit3StyleTestMethodInJUnit4Class")
    @AfterAll
    public void testDisconnect() {
        log("Logging off...");
        CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES)
                .execute(api::disconnect);
        api.await();
        log("Logged off");
    }

    @Override
    public void onChatMessages(Chat chat, boolean last) {
        if(!last){
            return;
        }

        System.out.printf("%s is ready with %s messages%n", chat.name(), chat.messages().size());
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
