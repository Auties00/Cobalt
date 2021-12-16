package it.auties.whatsapp4j.test.ci;

import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.chat.GroupPolicy;
import it.auties.whatsapp4j.protobuf.chat.GroupSetting;
import it.auties.whatsapp4j.protobuf.contact.Contact;
import it.auties.whatsapp4j.protobuf.contact.ContactStatus;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.message.model.MessageContainer;
import it.auties.whatsapp4j.protobuf.message.model.MessageKey;
import it.auties.whatsapp4j.protobuf.message.standard.*;
import it.auties.whatsapp4j.response.impl.json.UserInformationResponse;
import it.auties.whatsapp4j.test.github.GithubActions;
import it.auties.whatsapp4j.test.utils.ConfigUtils;
import it.auties.whatsapp4j.test.utils.MediaUtils;
import it.auties.whatsapp4j.test.utils.StatusUtils;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import it.auties.whatsapp4j.utils.internal.Validate;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A simple class to check that the library is working
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Log
@TestMethodOrder(OrderAnnotation.class)
public class WhatsappTest implements WhatsappListener {
    private WhatsappAPI whatsappAPI;
    private CountDownLatch latch;
    private String contactName;
    private boolean noKeys;
    private Contact contact;
    private Chat contactChat;
    private Chat group;

    @BeforeAll
    public void init() throws IOException {
        createApi();
        loadConfig();
        createLatch();
    }

    private void createApi() {
        sensitiveInfo("Initializing api to start testing...");
        if(GithubActions.isActionsEnvironment()){
            whatsappAPI = new WhatsappAPI(loadGithubKeys());
            return;
        }

        sensitiveInfo("Detected local environment");
        whatsappAPI = new WhatsappAPI();
    }

    private void loadConfig() throws IOException {
        if(GithubActions.isActionsEnvironment()) {
            sensitiveInfo("Loading environment variables...");
            this.contactName = System.getenv(GithubActions.CONTACT_NAME);
            sensitiveInfo("Loaded environment variables...");
            return;
        }

        sensitiveInfo("Loading configuration file...");
        var props = ConfigUtils.loadConfiguration();
        this.contactName = Objects.requireNonNull(props.getProperty("contact"), "Missing contact property in config");
        this.noKeys = Boolean.parseBoolean(props.getProperty("no_keys", "false"));
        sensitiveInfo("Loaded configuration file");
    }

    private WhatsappKeysManager loadGithubKeys(){
        sensitiveInfo("Detected github actions environment");
        var keysJson = Base64.getDecoder().decode(System.getenv(GithubActions.CREDENTIALS_NAME));
        var keys = WhatsappKeysManager.fromJson(keysJson);
        return Validate.isValid(keys, keys.mayRestore(), "WhatsappTest: Cannot start CI as credentials are incomplete");
    }

    private void createLatch() {
        latch = new CountDownLatch(3);
    }

    @Test
    @Order(1)
    public void registerListener() {
        sensitiveInfo("Registering listener...");
        whatsappAPI.registerListener(this);
        sensitiveInfo("Registered listener");
    }

    @Test
    @Order(2)
    public void testConnection() throws InterruptedException {
        sensitiveInfo("Connecting...");
        whatsappAPI.connect();
        latch.await();
        sensitiveInfo("Connected!");
        deleteKeys();
    }

    private void deleteKeys() {
        if (!noKeys) {
            return;
        }

        sensitiveInfo("Deleting keys from memory...");
        whatsappAPI.keys().deleteKeysFromMemory();
        sensitiveInfo("Deleted keys from memory");
    }

    @Test
    @Order(3)
    public void testChangeGlobalPresence() throws ExecutionException, InterruptedException {
        sensitiveInfo("Changing global presence...");
        var response = whatsappAPI.changePresence(ContactStatus.AVAILABLE).get();
        StatusUtils.checkStatusCode(response, "Cannot change individual presence, %s");
        sensitiveInfo("Changed global presence...");
    }

    @Test
    @Order(4)
    public void testContactLookup() {
        sensitiveInfo("Looking up a contact...");
        contact = whatsappAPI.manager().findContactByName(contactName)
                .orElseThrow(() -> new AssertionFailedError("Cannot lookup contact"));
        contactChat = whatsappAPI.manager().findChatByJid(contact.jid())
                .orElseThrow(() -> new AssertionFailedError("Cannot lookup chat"));
        sensitiveInfo("Looked up: %s", contact);
    }

    @Test
    @Order(5)
    public void testUserPresenceSubscription() throws ExecutionException, InterruptedException {
        sensitiveInfo("Subscribing to user presence...");
        var userPresenceResponse = whatsappAPI.subscribeToContactPresence(contact).get();
        StatusUtils.checkStatusCode(userPresenceResponse, "Cannot subscribe to user presence: %s");
        sensitiveInfo("Subscribed to user presence: %s", userPresenceResponse);
    }

    @Test
    @Order(6)
    public void testPictureQuery() throws IOException, ExecutionException, InterruptedException {
        sensitiveInfo("Loading picture...");
        var picResponse = whatsappAPI.queryChatPicture(contactChat).get();
        switch (picResponse.status()){
            case 200 -> {
                if(GithubActions.isActionsEnvironment()){
                    return;
                }

                var file = Files.createTempFile(UUID.randomUUID().toString(), ".jpg");
                Files.write(file, MediaUtils.readBytes(picResponse.url()), StandardOpenOption.CREATE);
                sensitiveInfo("Loaded picture: %s", file.toString());
            }
            case 401 -> sensitiveInfo("Cannot query pic because the contact blocked you");
            case 404 -> sensitiveInfo("The contact doesn't have a pic");
            default -> fail("Cannot query pic, erroneous status code: %s".formatted(picResponse));
        }
    }

    @Test
    @Order(7)
    public void testStatusQuery() throws ExecutionException, InterruptedException {
        sensitiveInfo("Querying %s's status...", contact.bestName());
        whatsappAPI.queryUserStatus(contact)
                .get()
                .status()
                .ifPresentOrElse(status -> sensitiveInfo("Queried %s", status), () -> sensitiveInfo("%s doesn't have a status", contact.bestName()));
    }

    @Test
    @Order(8)
    public void testFavouriteMessagesQuery() throws ExecutionException, InterruptedException {
        sensitiveInfo("Loading 20 favourite messages...");
        var favouriteMessagesResponse = whatsappAPI.queryFavouriteMessagesInChat(contactChat, 20).get();
        sensitiveInfo("Loaded favourite messages: %s", favouriteMessagesResponse.data());
    }

    @Test
    @Order(9)
    public void testGroupsInCommonQuery() throws ExecutionException, InterruptedException {
        sensitiveInfo("Loading groups in common...");
        var groupsInCommonResponse = whatsappAPI.queryGroupsInCommon(contact).get();
        assertEquals(200, groupsInCommonResponse.status(), "Cannot query groups in common: %s".formatted(groupsInCommonResponse));
        sensitiveInfo("Loaded groups in common: %s", groupsInCommonResponse.groups());
    }

    @Test
    @Order(10)
    public void testMarkChat() throws ExecutionException, InterruptedException {
        if(contactChat.hasUnreadMessages()){
            markAsRead();
            markAsUnread();
            return;
        }

        markAsUnread();
        markAsRead();
    }

    private void markAsUnread() throws ExecutionException, InterruptedException {
        sensitiveInfo("Marking chat as unread...");
        var markStatus = whatsappAPI.markAsUnread(contactChat).get();
        StatusUtils.checkStatusCode(markStatus, "Cannot mark chat as unread: %s");
        sensitiveInfo("Marked chat as unread");
    }

    private void markAsRead() throws ExecutionException, InterruptedException {
        sensitiveInfo("Marking chat as read...");
        var markStatus = whatsappAPI.markAsRead(contactChat).get();
        StatusUtils.checkStatusCode(markStatus, "Cannot mark chat as read: %s");
        sensitiveInfo("Marked chat as read");
    }


    @Test
    @Order(11)
    public void testGroupCreation() throws InterruptedException, ExecutionException {
        sensitiveInfo("Creating group...");
        group = whatsappAPI.createGroup(BinaryArray.random(5).toHex(), contact).get();
        sensitiveInfo("Created group: %s", group);
    }

    @Test
    @Order(12)
    public void testChangeIndividualPresence() throws ExecutionException, InterruptedException {
        for(var presence : ContactStatus.values()) {
            sensitiveInfo("Changing individual presence to %s...", presence.name());
            var response = whatsappAPI.changePresence(group, presence).get();
            StatusUtils.checkStatusCode(response, "Cannot change individual presence, %s");
            sensitiveInfo("Changed individual presence...");
        }
    }

    @Test
    @Order(13)
    public void testChangeGroupName() throws InterruptedException, ExecutionException {
        sensitiveInfo("Changing group name...");
        var changeGroupResponse = whatsappAPI.changeGroupName(group, "omega").get();
        StatusUtils.checkStatusCode(changeGroupResponse, "Cannot change group name: %s");
        sensitiveInfo("Changed group name");
    }

    @RepeatedTest(2)
    @Order(14)
    public void testChangeGroupDescription() throws InterruptedException, ExecutionException {
        sensitiveInfo("Changing group description...");
        var changeGroupResponse = whatsappAPI.changeGroupDescription(group, BinaryArray.random(12).toHex()).get();
        StatusUtils.checkStatusCode(changeGroupResponse, "Cannot change group description, erroneous response: %s".formatted(changeGroupResponse.status()));
        sensitiveInfo("Changed group description");
    }

    @Test
    @Order(15)
    public void testRemoveGroupParticipant() throws InterruptedException, ExecutionException {
        sensitiveInfo("Removing %s...", contact.bestName());
        var changeGroupResponse = whatsappAPI.remove(group, contact).get();
        switch (changeGroupResponse.status()){
            case 200 -> sensitiveInfo("Cannot remove participant, erroneous response: %s", changeGroupResponse);
            case 207 -> {
                assertTrue(StatusUtils.checkStatusCode(changeGroupResponse), "Cannot remove participant, erroneous response: %s");
                sensitiveInfo("Removed %s", contact.bestName());
            }

            default -> fail("Cannot remove participant, erroneous response code: %s".formatted(changeGroupResponse));
        }
    }

    @Test
    @Order(16)
    public void testAddGroupParticipant() throws InterruptedException, ExecutionException {
        sensitiveInfo("Adding %s...", contact.bestName());
        var changeGroupResponse = whatsappAPI.add(group, contact).get();
        switch (changeGroupResponse.status()){
            case 200 -> sensitiveInfo("Added participant %s", changeGroupResponse);
            case 207 -> {
                assertTrue(StatusUtils.checkStatusCode(changeGroupResponse),
                        "Cannot add participant, erroneous response: %s".formatted(changeGroupResponse));
                sensitiveInfo("Added %s", contact.bestName());
            }

            default -> fail("Cannot add participant, erroneous response code: %s".formatted(changeGroupResponse));
        }
    }

    @Test
    @Order(17)
    public void testPromotion() throws InterruptedException, ExecutionException {
        sensitiveInfo("Promoting %s...", contact.bestName());
        var changeGroupResponse = whatsappAPI.promote(group, contact).get();
        switch (changeGroupResponse.status()){
            case 200 -> sensitiveInfo("Cannot promote participant, erroneous response: %s", changeGroupResponse);
            case 207 -> {
                assertTrue(StatusUtils.checkStatusCode(changeGroupResponse), "Cannot promote participant, erroneous response: %s");
                sensitiveInfo("Promoted %s", contact.bestName());
            }

            default -> fail("Cannot promote participant, erroneous response code: %s".formatted(changeGroupResponse));
        }
    }

    @Test
    @Order(18)
    public void testDemotion() throws InterruptedException, ExecutionException {
        sensitiveInfo("Demoting %s...", contact.bestName());
        var changeGroupResponse = whatsappAPI.demote(group, contact).get();
        switch (changeGroupResponse.status()){
            case 200 -> sensitiveInfo("Cannot demote participant, erroneous response: %s", changeGroupResponse);
            case 207 -> {
                assertTrue(StatusUtils.checkStatusCode(changeGroupResponse), "Cannot demote participant, erroneous response: %s");
                sensitiveInfo("Demoted %s", contact.bestName());
            }

            default -> fail("Cannot demote participant, erroneous response code: %s".formatted(changeGroupResponse));
        }
    }

    @Test
    @Order(19)
    public void testChangeAllGroupSettings() throws InterruptedException, ExecutionException {
        for (var setting : GroupSetting.values()) {
            for (var policy : GroupPolicy.values()) {
                sensitiveInfo("Changing setting %s to %s...", setting.name(), policy.name());
                var changeGroupResponse = whatsappAPI.changeGroupSetting(group, setting, policy).get();
                assertEquals(200, changeGroupResponse.status(), "Cannot change setting %s to %s: %s".formatted(setting.name(), policy.name(), changeGroupResponse));
                sensitiveInfo("Changed setting %s to %s...", setting.name(), policy.name());
            }
        }
    }

    @Test
    @Order(20)
    public void testChangeAndRemoveGroupPicture() {
        log.warning("Not implemented");
    }

    @Test
    @Order(21)
    public void testGroupQuery() throws InterruptedException, ExecutionException {
        sensitiveInfo("Querying group %s...", group.jid());
        whatsappAPI.queryChat(group.jid()).get();
        sensitiveInfo("Queried group");
    }

    @Test
    @Order(22)
    public void testLoadConversation() throws InterruptedException, ExecutionException {
        sensitiveInfo("Loading conversation(%s)...", group.messages().size());
        whatsappAPI.loadChatHistory(group).get();
        sensitiveInfo("Loaded conversation(%s)!", group.messages().size());
    }

    @Test
    @Order(23)
    public void testMute() throws ExecutionException, InterruptedException {
        sensitiveInfo("Muting chat...");
        var muteResponse = whatsappAPI.mute(group, ZonedDateTime.now().plusDays(14)).get();
        StatusUtils.checkStatusCode(muteResponse, "Cannot mute chat: %s");
        sensitiveInfo("Muted chat");
    }

    @Test
    @Order(24)
    public void testUnmute() throws ExecutionException, InterruptedException {
        sensitiveInfo("Unmuting chat...");
        var unmuteResponse = whatsappAPI.unmute(group).get();
        StatusUtils.checkStatusCode(unmuteResponse, "Cannot unmute chat: %s");
        sensitiveInfo("Unmuted chat");
    }

    @Test
    @Order(25)
    public void testArchive() throws ExecutionException, InterruptedException {
        sensitiveInfo("Archiving chat...");
        var archiveResponse = whatsappAPI.archive(group).get();
        StatusUtils.checkStatusCode(archiveResponse, "Cannot archive chat: %s");
        sensitiveInfo("Archived chat");
    }

    @Test
    @Order(26)
    public void testUnarchive() throws ExecutionException, InterruptedException {
        sensitiveInfo("Unarchiving chat...");
        var unarchiveResponse = whatsappAPI.unarchive(group).get();
        StatusUtils.checkStatusCode(unarchiveResponse, "Cannot unarchive chat: %s");
        sensitiveInfo("Unarchived chat");
    }

    @Test
    @Order(27)
    public void testPin() throws ExecutionException, InterruptedException {
        if(whatsappAPI.manager().pinnedChats() >= 3){
            sensitiveInfo("Skipping chat pinning as there are already three chats pinned...");
            return;
        }

        sensitiveInfo("Pinning chat...");
        var pinResponse = whatsappAPI.pin(group).get();
        StatusUtils.checkStatusCode(pinResponse, "Cannot pin chat: %s");
        sensitiveInfo("Pinned chat");
    }

    @Test
    @Order(28)
    public void testUnpin() throws ExecutionException, InterruptedException {
        if(whatsappAPI.manager().pinnedChats() >= 3){
            sensitiveInfo("Skipping chat unpinning as there are already three chats pinned...");
            return;
        }

        sensitiveInfo("Unpinning chat...");
        var unpinResponse = whatsappAPI.unpin(group).get();
        StatusUtils.checkStatusCode(unpinResponse, "Cannot unpin chat: %s");
        sensitiveInfo("Unpinned chat");
    }

    @Test
    @Order(29)
    public void testTextMessage() throws ExecutionException, InterruptedException {
        sensitiveInfo("Sending text...");
        var key = new MessageKey(contactChat);
        var message = new MessageContainer("Test");
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        assertEquals(200, textResponse.status(), "Cannot send text: %s".formatted(textResponse));
        sensitiveInfo("Sent text");
    }

    @Test
    @Order(30)
    public void testImageMessage() throws ExecutionException, InterruptedException, IOException {
        sensitiveInfo("Sending image...");
        var key = new MessageKey(group);
        var image = ImageMessage.newImageMessage()
                .media(MediaUtils.readBytes("https://2.bp.blogspot.com/-DqXILvtoZFA/Wmmy7gRahnI/AAAAAAAAB0g/59c8l63QlJcqA0591t8-kWF739DiOQLcACEwYBhgL/s1600/pol-venere-botticelli-01.jpg"))
                .caption("Image test")
                .create();
        var message = new MessageContainer(image);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        assertEquals(200, textResponse.status(), "Cannot send image: %s".formatted(textResponse));
        sensitiveInfo("Sent image");
    }

    @Test
    @Order(31)
    public void testAudioMessage() throws ExecutionException, InterruptedException, IOException {
        sensitiveInfo("Sending audio...");
        var key = new MessageKey(group);
        var audio = AudioMessage.newAudioMessage()
                .media(MediaUtils.readBytes("https://www.kozco.com/tech/organfinale.mp3"))
                .create();
        var message = new MessageContainer(audio);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        assertEquals(200, textResponse.status(), "Cannot send audio: %s".formatted(textResponse));
        sensitiveInfo("Sent audio");
    }

    @Test
    @Order(32)
    public void testVideoMessage() throws ExecutionException, InterruptedException, IOException {
        sensitiveInfo("Sending video...");
        var key = new MessageKey(group);
        var video = VideoMessage.newVideoMessage()
                .media(MediaUtils.readBytes("http://techslides.com/demos/sample-videos/small.mp4"))
                .caption("Video")
                .create();
        var message = new MessageContainer(video);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        assertEquals(200, textResponse.status(), "Cannot send video: %s".formatted(textResponse));
        sensitiveInfo("Sent video");
    }

    @Test
    @Order(33)
    public void testGifMessage() throws ExecutionException, InterruptedException, IOException {
        sensitiveInfo("Sending gif...");
        var key = new MessageKey(group);
        var video = VideoMessage.newGifMessage()
                .media(MediaUtils.readBytes("http://techslides.com/demos/sample-videos/small.mp4"))
                .caption("Gif")
                .create();
        var message = new MessageContainer(video);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        assertEquals(200, textResponse.status(), "Cannot send gif: %s".formatted(textResponse));
        sensitiveInfo("Sent gif");
    }

    @Test
    @Order(34)
    public void testPdfMessage() throws ExecutionException, InterruptedException, IOException {
        sensitiveInfo("Sending pdf...");
        var key = new MessageKey(group);
        var document = DocumentMessage.newDocumentMessage()
                .media(MediaUtils.readBytes("http://www.orimi.com/pdf-test.pdf"))
                .title("Pdf test")
                .fileName("pdf-test.pdf")
                .pageCount(1)
                .create();
        var message = new MessageContainer(document);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        assertEquals(200, textResponse.status(), "Cannot send pdf: %s".formatted(textResponse));
        sensitiveInfo("Sent pdf");
    }

    @Test
    @Order(35)
    public void testContactMessage() throws ExecutionException, InterruptedException {
        sensitiveInfo("Sending contact message...");
        var key = new MessageKey(group);
        var vcard = buildVcard();
        var document = ContactMessage.newContactMessage()
                .displayName(contact.bestName(contact.jid()))
                .vcard(vcard)
                .create();
        var message = new MessageContainer(document);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        assertEquals(200, textResponse.status(), "Cannot send contact message: %s".formatted(textResponse));
        sensitiveInfo("Sent contact message");
    }

    private String buildVcard() {
        return """
                BEGIN:VCARD
                VERSION:3.0
                N:%s
                FN:%s
                TEL;type=CELL:+%s
                END:VCARD
                """.formatted(contact.shortName(), contact.name(), WhatsappUtils.phoneNumberFromJid(contact.jid()));
    }

    @Test
    @Order(36)
    public void testLocationMessage() throws ExecutionException, InterruptedException {
        sensitiveInfo("Sending location message...");
        var key = new MessageKey(group);
        var location = LocationMessage.newLocationMessage()
                .degreesLatitude(40.730610)
                .degreesLongitude(-73.935242)
                .degreesClockwiseFromMagneticNorth(0)
                .create();
        var message = new MessageContainer(location);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        assertEquals(200, textResponse.status(), "Cannot send location message: %s".formatted(textResponse));
        sensitiveInfo("Sent location message");
    }

    @Test
    @Order(37)
    public void testGroupInviteMessage() throws ExecutionException, InterruptedException {
        sensitiveInfo("Querying group invite code");
        var code = whatsappAPI.queryGroupInviteCode(group).get().code();
        sensitiveInfo("Queried %s", code);
        sensitiveInfo("Sending group invite message...");
        var key = new MessageKey(contactChat);
        var invite = GroupInviteMessage.newGroupInviteMessage()
                .groupJid(group.jid())
                .groupName(group.displayName())
                .inviteExpiration(ZonedDateTime.now().plusDays(3).toInstant().toEpochMilli())
                .inviteCode(code)
                .create();
        var message = new MessageContainer(invite);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        assertEquals(200, textResponse.status(), "Cannot send group invite message: %s".formatted(textResponse));
        sensitiveInfo("Sent group invite message");
    }


    @Test
    @Order(38)
    public void testEnableEphemeralMessages() throws ExecutionException, InterruptedException {
        sensitiveInfo("Enabling ephemeral messages...");
        var ephemeralResponse = whatsappAPI.enableEphemeralMessages(group).get();
        StatusUtils.checkStatusCode(ephemeralResponse, "Cannot enable ephemeral messages: %s");
        sensitiveInfo("Enabled ephemeral messages");
    }

    @Test
    @Order(39)
    public void testDisableEphemeralMessages() throws ExecutionException, InterruptedException {
        sensitiveInfo("Disabling ephemeral messages...");
        var ephemeralResponse = whatsappAPI.disableEphemeralMessages(group).get();
        StatusUtils.checkStatusCode(ephemeralResponse, "Cannot disable ephemeral messages: %s");
        sensitiveInfo("Disabled ephemeral messages");
    }

    @Test
    @Order(40)
    public void testLeave() throws ExecutionException, InterruptedException {
        sensitiveInfo("Leaving group...");
        var ephemeralResponse = whatsappAPI.leave(group).get();
        StatusUtils.checkStatusCode(ephemeralResponse, "Cannot leave group: %s");
        sensitiveInfo("Left group");
    }

    @Override
    public void onLoggedIn(@NonNull UserInformationResponse info) {
        sensitiveInfo("Logged in!");
        latch.countDown();
    }

    @Override
    public void onChats() {
        sensitiveInfo("Got chats!");
        latch.countDown();
    }

    @Override
    public void onContacts() {
        sensitiveInfo("Got contacts!");
        latch.countDown();
    }

    private void sensitiveInfo(String message, Object... params){
        log.info(message.formatted(redactParameters(params)));
    }

    private Object[] redactParameters(Object... params){
        if (!GithubActions.isActionsEnvironment()) {
            return params;
        }

        return Arrays.stream(params)
                .map(entry -> "***")
                .toArray(String[]::new);
    }
}