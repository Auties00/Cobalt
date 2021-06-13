package it.auties.whatsapp4j.test;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.listener.WhatsappListener;
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
import it.auties.whatsapp4j.utils.WhatsappUtils;
import it.auties.whatsapp4j.utils.internal.Validate;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.opentest4j.AssertionFailedError;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

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
        loadContactName();
        log.info("Initializing api to start testing...");
        whatsappAPI = new WhatsappAPI();
        latch = new CountDownLatch(3);
    }

    private void loadContactName() throws IOException {
        log.info("Loading configuration file...");

        var config = new File(Path.of(".").toRealPath().toFile(), "/.test/config.properties");
        Validate.isTrue(config.exists(), "Before running any unit test please create a file at %s and specify the name of the contact used for testing like this: contact=name", config.getPath(), FileNotFoundException.class);

        var props = new Properties();
        props.load(new FileReader(config));

        this.contactName = props.getProperty("contact");
        this.noKeys = Boolean.parseBoolean(props.getProperty("no_keys"));

        log.info("Loaded configuration file");
    }

    @Test
    @Order(1)
    public void registerListener() {
        log.info("Registering listener...");
        whatsappAPI.registerListener(this);
        log.info("Registered listener");
    }

    @Test
    @Order(2)
    public void testConnection() throws InterruptedException {
        log.info("Connecting...");
        whatsappAPI.connect();
        latch.await();
        log.info("Connected!");
        deleteKeys();
    }

    private void deleteKeys() {
        if (!noKeys) {
            return;
        }

        log.info("Deleting keys from memory...");
        whatsappAPI.keys().deleteKeysFromMemory();
        log.info("Deleted keys from memory");
    }

    @Test
    @Order(3)
    public void testChangeGlobalPresence() throws ExecutionException, InterruptedException {
        log.info("Changing global presence...");
        var response = whatsappAPI.changePresence(ContactStatus.AVAILABLE).get();
        Assertions.assertEquals(200, response.status(), "Cannot change individual presence, %s".formatted(response));
        log.info("Changed global presence...");
    }

    @Test
    @Order(4)
    public void testContactLookup() {
        log.info("Looking up a contact...");
        contact = whatsappAPI.manager().findContactByName(contactName).orElseThrow(() -> new AssertionFailedError("Cannot lookup contact"));
        contactChat = whatsappAPI.manager().findChatByJid(contact.jid()).orElseThrow();
        log.info("Looked up: %s".formatted(contact));
    }

    @Test
    @Order(5)
    public void testUserPresenceSubscription() throws ExecutionException, InterruptedException {
        log.info("Subscribing to user presence...");
        var userPresenceResponse = whatsappAPI.subscribeToContactPresence(contact).get();
        Assertions.assertEquals(200, userPresenceResponse.status(), "Cannot subscribe to user presence: %s".formatted(userPresenceResponse));
        log.info("Subscribed to user presence: %s".formatted(userPresenceResponse));
    }

    @Test
    @Order(6)
    public void testPictureQuery() throws IOException, ExecutionException, InterruptedException {
        log.info("Loading picture...");
        var picResponse = whatsappAPI.queryChatPicture(contactChat).get();
        switch (picResponse.status()){
            case 200 -> {
                var file = Files.createTempFile(UUID.randomUUID().toString(), ".jpg");
                Files.write(file, readBytes(picResponse.url()), StandardOpenOption.CREATE);
                log.info("Loaded picture: %s".formatted(file.toString()));
            }
            case 404 -> log.info("The contact doesn't have a pic");
            default -> throw new AssertionFailedError("Cannot query pic: %s".formatted(picResponse));
        }
    }

    @Test
    @Order(7)
    public void testStatusQuery() throws ExecutionException, InterruptedException {
        log.info("Querying %s's status...".formatted(contact.bestName()));
        whatsappAPI.queryUserStatus(contact)
                .get()
                .status()
                .ifPresentOrElse(status -> log.info("Queried %s".formatted(status)), () -> log.info("%s doesn't have a status".formatted(contact.bestName())));
    }

    @Test
    @Order(8)
    public void testFavouriteMessagesQuery() throws ExecutionException, InterruptedException {
        log.info("Loading 20 favourite messages...");
        var favouriteMessagesResponse = whatsappAPI.queryFavouriteMessagesInChat(contactChat, 20).get();
        log.info("Loaded favourite messages: %s".formatted(favouriteMessagesResponse.data()));
    }

    @Test
    @Order(9)
    public void testGroupsInCommonQuery() throws ExecutionException, InterruptedException {
        log.info("Loading groups in common...");
        var groupsInCommonResponse = whatsappAPI.queryGroupsInCommon(contact).get();
        Assertions.assertEquals(200, groupsInCommonResponse.status(), "Cannot query groups in common: %s".formatted(groupsInCommonResponse));
        log.info("Loaded groups in common: %s".formatted(groupsInCommonResponse.groups()));
    }

    @Test
    @Order(10)
    public void testMarkChatAsUnread() throws ExecutionException, InterruptedException {
        log.info("Marking chat as unread...");
        System.out.println(contactChat);
        var markStatus = whatsappAPI.markAsUnread(contactChat).get();
        System.out.println(contactChat);
        Assertions.assertEquals(200, markStatus.status(), "Cannot mark chat as unread: %s".formatted(markStatus));
        log.info("Marked chat as unread");
    }

    @Test
    @Order(11)
    public void testMarkChatAsRead() throws ExecutionException, InterruptedException {
        log.info("Marking chat as read...");
        var markStatus = whatsappAPI.markAsRead(contactChat).get();
        Assertions.assertEquals(200, markStatus.status(), "Cannot mark chat as read: %s".formatted(markStatus));
        log.info("Marked chat as read");
    }

    @Test
    @Order(12)
    public void testGroupCreation() throws InterruptedException, ExecutionException {
        log.info("Creating group...");
        group = whatsappAPI.createGroup(BinaryArray.random(5).toHex(), contact).get();
        log.info("Created group: %s".formatted(group));
    }

    @Test
    @Order(13)
    public void testChangeIndividualPresence() throws ExecutionException, InterruptedException {
        for(var presence : ContactStatus.values()) {
            log.info("Changing individual presence to %s...".formatted(presence.name()));
            var response = whatsappAPI.changePresence(group, presence).get();
            Assertions.assertEquals(200, response.status(), "Cannot change individual presence, %s".formatted(response));
            log.info("Changed individual presence...");
        }
    }

    @Test
    @Order(14)
    public void testChangeGroupName() throws InterruptedException, ExecutionException {
        log.info("Changing group name...");
        var changeGroupResponse = whatsappAPI.changeGroupName(group, "omega").get();
        Assertions.assertEquals(200, changeGroupResponse.status(), "Cannot change group name: %s".formatted(changeGroupResponse));
        log.info("Changed group name");
    }

    @RepeatedTest(2)
    @Order(15)
    public void testChangeGroupDescription() throws InterruptedException, ExecutionException {
        log.info("Changing group description...");
        var changeGroupResponse = whatsappAPI.changeGroupDescription(group, BinaryArray.random(12).toHex()).get();
        Assertions.assertEquals(200, changeGroupResponse.status(), "Cannot change group description: %s".formatted(changeGroupResponse));
        log.info("Changed group description");
    }

    @Test
    @Order(16)
    public void testRemoveGroupParticipant() throws InterruptedException, ExecutionException {
        log.info("Removing %s...".formatted(contact.bestName()));
        var changeGroupResponse = whatsappAPI.remove(group, contact).get();
        switch (changeGroupResponse.status()){
            case 200 -> log.info("Cannot remove %s: %s".formatted(contact.bestName(), changeGroupResponse));
            case 207 -> {
                Assertions.assertTrue(changeGroupResponse.modifications().stream().allMatch(modification -> modification.status().code() == 200), "Cannot remove %s: %s".formatted(contact.bestName(), changeGroupResponse));
                log.info("Removed %s".formatted(contact.bestName()));
            }

            default -> throw new AssertionFailedError("Cannot remove %s: %s".formatted(contact.bestName(), changeGroupResponse));
        }
    }

    @Test
    @Order(17)
    public void testAddGroupParticipant() throws InterruptedException, ExecutionException {
        log.info("Adding %s...".formatted(contact.bestName()));
        var changeGroupResponse = whatsappAPI.add(group, contact).get();
        switch (changeGroupResponse.status()){
            case 200 -> log.info("Cannot add %s: %s".formatted(contact.bestName(), changeGroupResponse));
            case 207 -> {
                Assertions.assertTrue(changeGroupResponse.modifications().stream().allMatch(modification -> modification.status().code() == 200), "Cannot add %s: %s".formatted(contact.bestName(), changeGroupResponse));
                log.info("Added %s".formatted(contact.bestName()));
            }

            default -> throw new AssertionFailedError("Cannot add %s: %s".formatted(contact.bestName(), changeGroupResponse));
        }
    }

    @Test
    @Order(18)
    public void testPromotion() throws InterruptedException, ExecutionException {
        log.info("Promoting %s...".formatted(contact.bestName()));
        var changeGroupResponse = whatsappAPI.promote(group, contact).get();
        switch (changeGroupResponse.status()){
            case 200 -> log.info("Promoted %s".formatted(contact.bestName()));
            case 207 -> {
                Assertions.assertTrue(changeGroupResponse.modifications().stream().allMatch(modification -> modification.status().code() == 200), "Cannot promote %s: %s".formatted(contact.bestName(), changeGroupResponse));
                log.info("Promoted %s".formatted(contact.bestName()));
            }

            default -> throw new AssertionFailedError("Cannot promote %s: %s".formatted(contact.bestName(), changeGroupResponse));
        }
    }

    @Test
    @Order(19)
    public void testDemotion() throws InterruptedException, ExecutionException {
        log.info("Demoting %s...".formatted(contact.bestName()));
        var changeGroupResponse = whatsappAPI.demote(group, contact).get();
        switch (changeGroupResponse.status()){
            case 200 -> log.info("Promoted %s".formatted(contact.bestName()));
            case 207 -> {
                Assertions.assertTrue(changeGroupResponse.modifications().stream().allMatch(modification -> modification.status().code() == 200), "Cannot demote %s: %s".formatted(contact.bestName(), changeGroupResponse));
                log.info("Demoted %s".formatted(contact.bestName()));
            }

            default -> throw new AssertionFailedError("Cannot demote %s: %s".formatted(contact.bestName(), changeGroupResponse));
        }
    }

    @Test
    @Order(20)
    public void testChangeAllGroupSettings() throws InterruptedException, ExecutionException {
        for (var setting : GroupSetting.values()) {
            for (var policy : GroupPolicy.values()) {
                log.info("Changing setting %s to %s...".formatted(setting.name(), policy.name()));
                var changeGroupResponse = whatsappAPI.changeGroupSetting(group, setting, policy).get();
                Assertions.assertEquals(200, changeGroupResponse.status(), "Cannot change setting %s to %s, %s".formatted(setting.name(), policy.name(), changeGroupResponse));
                log.info("Changed setting %s to %s...".formatted(setting.name(), policy.name()));
            }
        }
    }

    @Test
    @Order(21)
    public void testChangeAndRemoveGroupPicture() {
        log.warning("Not implemented");
    }

    @Test
    @Order(22)
    public void testGroupQuery() throws InterruptedException, ExecutionException {
        log.info("Querying group %s...".formatted(group.jid()));
        whatsappAPI.queryChat(group.jid()).get();
        log.info("Queried group");
    }

    @Test
    @Order(23)
    public void testLoadConversation() throws InterruptedException, ExecutionException {
        log.info("Loading conversation(%s)...".formatted(group.messages().size()));
        whatsappAPI.loadChatHistory(group).get();
        log.info("Loaded conversation(%s)!".formatted(group.messages().size()));
    }

    @Test
    @Order(24)
    public void testMute() throws ExecutionException, InterruptedException {
        log.info("Muting chat...");
        var muteResponse = whatsappAPI.mute(group, ZonedDateTime.now().plusDays(14)).get();
        Assertions.assertEquals(200, muteResponse.status(), "Cannot mute chat: %s".formatted(muteResponse));
        log.info("Muted chat");
    }

    @Test
    @Order(25)
    public void testUnmute() throws ExecutionException, InterruptedException {
        log.info("Unmuting chat...");
        var unmuteResponse = whatsappAPI.unmute(group).get();
        Assertions.assertEquals(200, unmuteResponse.status(), "Cannot unmute chat: %s".formatted(unmuteResponse));
        log.info("Unmuted chat");
    }

    @Test
    @Order(26)
    public void testArchive() throws ExecutionException, InterruptedException {
        log.info("Archiving chat...");
        var archiveResponse = whatsappAPI.archive(group).get();
        Assertions.assertEquals(200, archiveResponse.status(), "Cannot archive chat: %s".formatted(archiveResponse));
        log.info("Archived chat");
    }

    @Test
    @Order(27)
    public void testUnarchive() throws ExecutionException, InterruptedException {
        log.info("Unarchiving chat...");
        var unarchiveResponse = whatsappAPI.unarchive(group).get();
        Assertions.assertEquals(200, unarchiveResponse.status(), "Cannot unarchive chat: %s".formatted(unarchiveResponse));
        log.info("Unarchived chat");
    }

    @Test
    @Order(28)
    public void testPin() throws ExecutionException, InterruptedException {
        if(whatsappAPI.manager().chats().stream().filter(Chat::isPinned).count() >= 3){
            log.info("Skipping chat pinning as there are already three chats pinned...");
            return;
        }

        log.info("Pinning chat...");
        var pinResponse = whatsappAPI.pin(group).get();
        Assertions.assertEquals(200, pinResponse.status(), "Cannot pin chat: %s".formatted(pinResponse));
        log.info("Pinned chat");
    }

    @Test
    @Order(29)
    public void testUnpin() throws ExecutionException, InterruptedException {
        if(whatsappAPI.manager().chats().stream().filter(Chat::isPinned).count() >= 3){
            log.info("Skipping chat unpinning as there are already three chats pinned...");
            return;
        }

        log.info("Unpinning chat...");
        var unpinResponse = whatsappAPI.unpin(group).get();
        Assertions.assertEquals(200, unpinResponse.status(), "Cannot unpin chat: %s".formatted(unpinResponse));
        log.info("Unpinned chat");
    }

    @Test
    @Order(30)
    public void testTextMessage() throws ExecutionException, InterruptedException {
        log.info("Sending text...");
        var key = new MessageKey(contactChat);
        var message = new MessageContainer("Test");
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        Assertions.assertEquals(200, textResponse.status(), "Cannot send text: %s".formatted(textResponse));
        log.info("Sent text");
    }

    @Test
    @Order(31)
    public void testImageMessage() throws ExecutionException, InterruptedException, IOException {
        log.info("Sending image...");
        var key = new MessageKey(group);
        var image = ImageMessage.newImageMessage()
                .media(readBytes("https://2.bp.blogspot.com/-DqXILvtoZFA/Wmmy7gRahnI/AAAAAAAAB0g/59c8l63QlJcqA0591t8-kWF739DiOQLcACEwYBhgL/s1600/pol-venere-botticelli-01.jpg"))
                .caption("Image test")
                .create();
        var message = new MessageContainer(image);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        Assertions.assertEquals(200, textResponse.status(), "Cannot send image: %s".formatted(textResponse));
        log.info("Sent image");
    }

    @Test
    @Order(32)
    public void testAudioMessage() throws ExecutionException, InterruptedException, IOException {
        log.info("Sending audio...");
        var key = new MessageKey(group);
        var audio = AudioMessage.newAudioMessage()
                .media(readBytes("https://www.kozco.com/tech/organfinale.mp3"))
                .create();
        var message = new MessageContainer(audio);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        Assertions.assertEquals(200, textResponse.status(), "Cannot send audio: %s".formatted(textResponse));
        log.info("Sent audio");
    }

    @Test
    @Order(33)
    public void testVideoMessage() throws ExecutionException, InterruptedException, IOException {
        log.info("Sending video...");
        var key = new MessageKey(group);
        var video = VideoMessage.newVideoMessage()
                .media(readBytes("http://techslides.com/demos/sample-videos/small.mp4"))
                .caption("Video")
                .create();
        var message = new MessageContainer(video);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        Assertions.assertEquals(200, textResponse.status(), "Cannot send video: %s".formatted(textResponse));
        log.info("Sent video");
    }

    @Test
    @Order(34)
    public void testGifMessage() throws ExecutionException, InterruptedException, IOException {
        log.info("Sending gif...");
        var key = new MessageKey(group);
        var video = VideoMessage.newGifMessage()
                .media(readBytes("http://techslides.com/demos/sample-videos/small.mp4"))
                .caption("Gif")
                .create();
        var message = new MessageContainer(video);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        Assertions.assertEquals(200, textResponse.status(), "Cannot send gif: %s".formatted(textResponse));
        log.info("Sent gif");
    }

    @Test
    @Order(35)
    public void testPdfMessage() throws ExecutionException, InterruptedException, IOException {
        log.info("Sending pdf...");
        var key = new MessageKey(group);
        var document = DocumentMessage.newDocumentMessage()
                .media(readBytes("http://www.orimi.com/pdf-test.pdf"))
                .title("Pdf test")
                .fileName("pdf-test.pdf")
                .pageCount(1)
                .create();
        var message = new MessageContainer(document);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        Assertions.assertEquals(200, textResponse.status(), "Cannot send pdf: %s".formatted(textResponse));
        log.info("Sent pdf");
    }

    @Test
    @Order(36)
    public void testContactMessage() throws ExecutionException, InterruptedException {
        log.info("Sending contact message...");
        var key = new MessageKey(group);
        var vcard = buildVcard();
        var document = ContactMessage.newContactMessage()
                .displayName("Carletto")
                .vcard(vcard)
                .create();
        var message = new MessageContainer(document);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        Assertions.assertEquals(200, textResponse.status(), "Cannot send contact message: %s".formatted(textResponse));
        log.info("Sent contact message");
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
    @Order(37)
    public void testLocationMessage() throws ExecutionException, InterruptedException {
        log.info("Sending location message...");
        var key = new MessageKey(group);
        var location = LocationMessage.newLocationMessage()
                .degreesLatitude(40.730610)
                .degreesLongitude(-73.935242)
                .degreesClockwiseFromMagneticNorth(0)
                .create();
        var message = new MessageContainer(location);
        var info = new MessageInfo(key, message);
        var textResponse = whatsappAPI.sendMessage(info).get();
        Assertions.assertEquals(200, textResponse.status(), "Cannot send location message: %s".formatted(textResponse));
        log.info("Sent location message");
    }

    @Test
    @Order(38)
    public void testGroupInviteMessage() throws ExecutionException, InterruptedException {
        log.info("Querying group invite code");
        var code = whatsappAPI.queryGroupInviteCode(group).get().code();
        log.info("Queried %s".formatted(code));
        log.info("Sending group invite message...");
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
        Assertions.assertEquals(200, textResponse.status(), "Cannot send group invite message: %s".formatted(textResponse));
        log.info("Sent group invite message");
    }


    @Test
    @Order(39)
    public void testEnableEphemeralMessages() throws ExecutionException, InterruptedException {
        log.info("Enabling ephemeral messages...");
        var ephemeralResponse = whatsappAPI.enableEphemeralMessages(group).get();
        Assertions.assertEquals(200, ephemeralResponse.status(), "Cannot enable ephemeral messages: %s".formatted(ephemeralResponse));
        log.info("Enabled ephemeral messages");
    }

    @Test
    @Order(40)
    public void testDisableEphemeralMessages() throws ExecutionException, InterruptedException {
        log.info("Disabling ephemeral messages...");
        var ephemeralResponse = whatsappAPI.disableEphemeralMessages(group).get();
        Assertions.assertEquals(200, ephemeralResponse.status(), "Cannot disable ephemeral messages: %s".formatted(ephemeralResponse));
        log.info("Disabled ephemeral messages");
    }

    @Test
    @Order(41)
    public void testLeave() throws ExecutionException, InterruptedException {
        log.info("Leaving group...");
        var ephemeralResponse = whatsappAPI.leave(group).get();
        Assertions.assertEquals(200, ephemeralResponse.status(), "Cannot leave group: %s".formatted(ephemeralResponse));
        log.info("Left group");
    }

    @Override
    public void onLoggedIn(@NonNull UserInformationResponse info) {
        log.info("Logged in!");
        latch.countDown();
    }

    @Override
    public void onChats() {
        log.info("Got chats!");
        latch.countDown();
    }

    @Override
    public void onContacts() {
        log.info("Got contacts!");
        latch.countDown();
    }

    private byte[] readBytes(String url) throws IOException {
        return new URL(url).openStream().readAllBytes();
    }
}