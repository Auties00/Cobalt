package it.auties.whatsapp.test;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.api.DisconnectReason;
import it.auties.whatsapp.api.Emojy;
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
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.chat.ChatMute;
import it.auties.whatsapp.model.chat.GroupPolicy;
import it.auties.whatsapp.model.contact.*;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.button.ButtonsMessage;
import it.auties.whatsapp.model.message.button.InteractiveMessage;
import it.auties.whatsapp.model.message.button.ListMessage;
import it.auties.whatsapp.model.message.button.TemplateMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.standard.*;
import it.auties.whatsapp.model.poll.PollOption;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.privacy.PrivacySettingValue;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.utils.ConfigUtils;
import it.auties.whatsapp.utils.MediaUtils;
import lombok.SneakyThrows;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.examples.ByteArrayHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// IMPORTANT !!!!
// If you run this CI on a brand-new number it will 99% ban it because it adds a person to a group which is considered spam
// I repeat: DO NOT RUN THIS CI LOCALLY ON A BRAND-NEW NUMBER OR IT WILL GET BANNED
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class RunCITest implements Listener, JacksonProvider {
    @SuppressWarnings("HttpUrlsUsage")
    private static final String VIDEO_URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";

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
        return SMILE.readValue(decrypted, type);
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

    @Test
    @Order(1)
    public void testHasWhatsapp() {
        if (skip) {
            return;
        }

        var response = api.hasWhatsapp(contact, ContactJid.of("123456789")).join();
        log("Has whatsapp? %s", response);
    }

    @Test
    @Order(2)
    public void testChangeGlobalPresence() {
        if (skip) {
            return;
        }
        log("Changing global presence...");
        api.changePresence(true).join();
        log("Changed global presence...");
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
    @Order(3)
    public void testUserPresenceSubscription() {
        if (skip) {
            return;
        }
        log("Subscribing to user presence...");
        var userPresenceResponse = api.subscribeToPresence(contact).join();
        log("Subscribed to user presence: %s", userPresenceResponse);
    }

    @Test
    @Order(3)
    public void testPrivacySettings() {
        if (skip) {
            return;
        }
        log("Changing privacy settings...");
        for(var settingType : PrivacySettingType.values()){
            for(var settingValue : PrivacySettingValue.values()){
                try{
                    log("Changing privacy setting %s to %s...", settingType, settingValue);
                    api.changePrivacySetting(settingType, settingValue, contact).join();
                    log("Changed privacy setting %s to %s", settingType, settingValue);
                }catch (IllegalArgumentException exception){
                    log("Value %s is not supported for setting %s: %s", settingValue, settingType, exception.getMessage());
                }
            }
        }
        log("Changed privacy settings");
    }

    @Test
    @Order(4)
    public void testPictureQuery() {
        if (skip) {
            return;
        }
        log("Loading picture...");
        var picResponse = api.queryPicture(contact).join();
        log("Loaded picture at: %s", picResponse);
    }

    @Test
    @Order(5)
    public void testStatusQuery() {
        if (skip) {
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
        if (skip) {
            return;
        }
        markAsUnread();
        markAsRead();
    }

    private void markAsUnread() {
        log("Marking chat as unread...");
        var markStatus = api.markUnread(contact).join();
        log("Marked chat as unread: %s", markStatus);
    }

    private void markAsRead() {
        log("Marking chat as read...");
        var markStatus = api.markRead(contact).join();
        log("Marked chat as read: %s", markStatus);
    }

    @Test
    @Order(9)
    public void testClearChat() {
        if (skip) {
            return;
        }
        log("Clearing chat...");
        var ephemeralResponse = api.clear(contact, false).join();
        log("Cleared chat: %s", ephemeralResponse);
    }

    @Test
    @Order(10)
    public void testDeleteChat() {
        if (skip) {
            return;
        }
        log("Deleting chat...");
        var ephemeralResponse = api.delete(contact).join();
        log("Deleted chat: %s", ephemeralResponse);
    }

    @Test
    @Order(11)
    public void testGroupCreation() {
        if (skip) {
            return;
        }
        log("Creating group...");
        var response = api.createGroup(Bytes.ofRandom(5).toHex(), contact).join();
        group = response.jid();
        log("Created group: %s", response);
    }

    @Test
    @Order(11)
    public void testCommunity() {
        if (skip) {
            return;
        }
        log("Creating community...");
        var communityCreationResponse = api.createCommunity(Bytes.ofRandom(5).toHex(), "A nice body").join();
        log("Created community: %s", communityCreationResponse);
        log("Querying community metadata...");
        var communityMetadataResponse = api.queryGroupMetadata(communityCreationResponse.jid()).join();
        Assertions.assertTrue(communityMetadataResponse.community(), "Expected a community");
        log("Queried community metadata: %s", communityMetadataResponse);
        log("Creating child group...");
        var communityChildCreationResponse = api.createGroup(Bytes.ofRandom(5).toHex(),  ChatEphemeralTimer.THREE_MONTHS, communityCreationResponse.jid()).join();
        log("Created child group: %s", communityChildCreationResponse);
        log("Querying child group metadata...");
        var communityChildMetadataResponse = api.queryGroupMetadata(communityChildCreationResponse.jid()).join();
        Assertions.assertFalse(communityChildMetadataResponse.community(), "Expected a group");
        log("Queried child group metadata: %s", communityChildMetadataResponse);
        log("Unlinking child group...");
        var unlinkChildCommunityResponse = api.unlinkGroupFromCommunity(communityMetadataResponse.jid(), communityChildMetadataResponse.jid()).join();
        Assertions.assertTrue(unlinkChildCommunityResponse, "Failed unlink");
        log("Unlinked child group");
        log("Linking child group...");
        var linkChildCommunityResponse = api.linkGroupsToCommunity(communityMetadataResponse.jid(), communityChildMetadataResponse.jid()).join();
        Assertions.assertTrue(linkChildCommunityResponse.get(communityChildMetadataResponse.jid()), "Failed link");
        log("Linked child group");
    }


    @Test
    @Order(12)
    public void testChangeIndividualPresence() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        for (var presence : ContactStatus.values()) {
            log("Changing individual presence to %s...", presence.name());
            var response = api.changePresence(group, presence).join();
            log("Changed individual presence: %s", response);
        }
    }

    @Test
    @Order(13)
    public void testChangeGroupName() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Changing group name...");
        var changeGroupResponse = api.changeGroupSubject(group, "omega").join();
        log("Changed group name: %s", changeGroupResponse);
    }

    @RepeatedTest(2)
    @Order(14)
    public void testChangeGroupDescription() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Changing group description...");
        var changeGroupResponse = api.changeGroupDescription(group, Bytes.ofRandom(12).toHex()).join();
        log("Changed group description: %s", changeGroupResponse);
    }

    @Test
    @Order(15)
    public void testRemoveGroupParticipant() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Removing %s...", contact);
        var changeGroupResponse = api.removeGroupParticipant(group, contact).join();
        log("Removed: %s", changeGroupResponse);
    }

    @Test
    @Order(16)
    public void testAddGroupParticipant() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Adding %s...", contact);
        var changeGroupResponse = api.addGroupParticipant(group, contact).join();
        log("Added: %s", changeGroupResponse);
    }

    @Test
    @Order(17)
    public void testPromotion() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Promoting %s...", contact);
        var changeGroupResponse = api.promote(group, contact).join();
        log("Promoted: %s", changeGroupResponse);
    }

    @Test
    @Order(18)
    public void testDemotion() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Demoting %s...", contact);
        var changeGroupResponse = api.demote(group, contact).join();
        log("Demoted: %s", changeGroupResponse);
    }

    @Test
    @Order(19)
    public void testChangeAllGroupSettings() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        for (var policy : GroupPolicy.values()) {
            log("Changing settings to %s...", policy.name());
            api.changeWhoCanEditInfo(group, policy).join();
            api.changeWhoCanEditInfo(group, policy).join();
            log("Changed settings to %s", policy.name());
        }
    }

    @Test
    @Order(20)
    public void testGroupQuery() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Querying group %s...", group);
        api.queryGroupMetadata(group).join();
        log("Queried group");
    }

    @Test
    @Order(21)
    public void testMute() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Muting chat...");
        var muteResponse = api.mute(group, ChatMute.mutedForOneWeek()).join();
        log("Muted chat: %s", muteResponse);
    }

    @Test
    @Order(22)
    public void testUnmute() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Unmuting chat...");
        var unmuteResponse = api.unmute(group).join();
        log("Unmuted chat: %s", unmuteResponse);
    }

    @Test
    @Order(23)
    public void testArchive() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Archiving chat...");
        var archiveResponse = api.archive(group).join();
        log("Archived chat: %s", archiveResponse);
    }

    @Test
    @Order(24)
    public void testUnarchive() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Unarchiving chat...");
        var unarchiveResponse = api.unarchive(group).join();
        log("Unarchived chat: %s", unarchiveResponse);
    }

    @Test
    @Order(25)
    public void testPin() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        if (api.store().pinnedChats().size() >= 3) {
            log("Skipping chat pinning as there are already three chats pinned...");
            return;
        }
        log("Pinning chat...");
        var pinResponse = api.pin(group).join();
        log("Pinned chat: %s", pinResponse);
    }

    @Test
    @Order(26)
    public void testUnpin() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        if (api.store().pinnedChats().size() >= 3) {
            log("Skipping chat unpinning as there are already three chats pinned...");
            return;
        }
        log("Unpinning chat...");
        var unpinResponse = api.unpin(group).join();
        log("Unpinned chat: %s", unpinResponse);
    }

    @Test
    @Order(27)
    public void testTextMessage() {
        if (skip) {
            return;
        }
        log("Sending simple text...");
        api.sendMessage(contact, "Hello").join();
        log("Sent simple text");
        log("Sending youtube video...");
        api.sendMessage(contact, "Hello: https://www.youtube.com/watch?v=4boXExbbGCk").join();
        log("Sent youtube video");
        log("Sending article...");
        api.sendMessage(contact, "Hello: it.wikipedia.org/wiki/Vulcano") // Missing schema by design
                .join();
        log("Sent article");
    }

    @Test
    @Order(28)
    public void deleteMessage() {
        if (skip) {
            return;
        }
        var example = api.sendMessage(contact, "Hello").join();
        log("Deleting for you...");
        api.delete(example, false).join();
        log("Deleted for you");
        log("Deleting for everyone...");
        api.delete(example, true).join();
        log("Deleted for everyone");
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
    @Order(30)
    public void testImageMessage() {
        if (skip) {
            return;
        }
        log("Sending image...");
        var image = ImageMessage.simpleBuilder()
                .media(MediaUtils.readBytes("https://2.bp.blogspot.com/-DqXILvtoZFA/Wmmy7gRahnI/AAAAAAAAB0g/59c8l63QlJcqA0591t8-kWF739DiOQLcACEwYBhgL/s1600/pol-venere-botticelli-01.jpg"))
                .caption("Image test")
                .build();
        var textResponse = api.sendMessage(contact, image).join();
        log("Sent image: %s", textResponse);
    }

    @Test
    @Order(31)
    public void testAudioMessage() {
        if (skip) {
            return;
        }
        log("Sending audio...");
        var audio = AudioMessage.simpleBuilder()
                .media(MediaUtils.readBytes("https://www.kozco.com/tech/organfinale.mp3"))
                .voiceMessage(true)
                .build();
        api.sendMessage(contact, audio).join();
        log("Sent audio");
    }

    @Test
    @Order(32)
    public void testVideoMessage() {
        if (skip) {
            return;
        }
        log("Sending video...");
        var video = VideoMessage.simpleVideoBuilder().media(MediaUtils.readBytes(VIDEO_URL)).caption("Video").build();
        api.sendMessage(contact, video).join();
        log("Sent video");
    }

    @Test
    @Order(33)
    public void testGifMessage() {
        if (skip) {
            return;
        }
        log("Sending gif...");
        var video = VideoMessage.simpleGifBuilder().media(MediaUtils.readBytes(VIDEO_URL)).caption("Gif").build();
        api.sendMessage(contact, video).join();
        log("Sent video");
    }

    @SuppressWarnings("HttpUrlsUsage")
    @Test
    @Order(34)
    public void testPdfMessage() {
        if (skip) {
            return;
        }
        log("Sending pdf...");
        var document = DocumentMessage.simpleBuilder()
                .media(MediaUtils.readBytes("http://www.orimi.com/pdf-test.pdf"))
                .title("Pdf test")
                .fileName("pdf-test.pdf")
                .pageCount(1)
                .build();
        api.sendMessage(contact, document).join();
        log("Sent pdf");
    }

    @Test
    @Order(35)
    public void testContactMessage() {
        if (skip) {
            return;
        }
        log("Sending contact message...");
        var vcard = ContactCard.builder().name("A nice contact").phoneNumber(contact).build();
        var contactMessage = ContactMessage.of("A nice contact", vcard);
        var response = api.sendMessage(contact, contactMessage).join();
        log("Sent contact: %s", response);
    }

    @Test
    @Order(36)
    public void testLocationMessage() {
        if (skip) {
            return;
        }
        log("Sending location message...");
        var location = LocationMessage.builder()
                .latitude(40.730610)
                .longitude(-73.935242)
                .magneticNorthOffset(0)
                .build();
        var textResponse = api.sendMessage(contact, location).join();
        log("Sent location: %s", textResponse);
    }

    @Test
    @Order(37)
    public void testGroupInviteMessage() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Querying group invite countryCode");
        var code = api.queryGroupInviteCode(group).join();
        log("Queried %s", code);
        log("Sending group invite message...");
        var invite = GroupInviteMessage.builder()
                .group(group)
                .code(code)
                .expiration(ZonedDateTime.now().plusDays(3).toEpochSecond())
                .groupName(group.user())
                .build();
        var textResponse = api.sendMessage(contact, invite).join();
        log("Sent invite: %s", textResponse);
    }

    @Test
    @Order(38)
    public void testEnableEphemeralMessages() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Enabling ephemeral messages...");
        var ephemeralResponse = api.changeEphemeralTimer(group, ChatEphemeralTimer.ONE_WEEK).join();
        log("Enabled ephemeral messages: %s", ephemeralResponse);
    }

    @Test
    @Order(39)
    public void testDisableEphemeralMessages() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Disabling ephemeral messages...");
        var ephemeralResponse = api.changeEphemeralTimer(group, ChatEphemeralTimer.OFF).join();
        log("Disabled ephemeral messages: %s", ephemeralResponse);
    }

    @Test
    @Order(40)
    public void testLeave() {
        if (skip) {
            return;
        }
        if (group == null) {
            testGroupCreation();
        }
        log("Leaving group...");
        var ephemeralResponse = api.leaveGroup(group).join();
        log("Left group: %s", ephemeralResponse);
    }

    @Test
    @Order(41)
    public void testInteractiveMessage() { // These are not even supported, though we have a test lol
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
        api.sendMessage(contact, listMessage).join();
        log("Sent list message");
    }

    @Test
    @Order(43)
    public void testPollMessage() {
        if (skip) {
            return;
        }

        var pollOptionFirst = PollOption.of("First");
        var pollOptionSecond = PollOption.of("Second");
        var pollMessage = PollCreationMessage.of("Example poll", List.of(pollOptionFirst, pollOptionSecond));
        var pollInfo = api.sendMessage(contact, pollMessage).join();
        var firstUpdate = PollUpdateMessage.of(pollInfo, List.of(pollOptionFirst));
        api.sendMessage(contact, firstUpdate).join();
        var secondUpdate = PollUpdateMessage.of(pollInfo, List.of(pollOptionFirst, pollOptionSecond));
        api.sendMessage(contact, secondUpdate).join();
        var finalUpdate = PollUpdateMessage.of(pollInfo, List.of());
        api.sendMessage(contact, finalUpdate).join();
        log("Sent poll message");
    }

    @Test
    @Order(44)
    public void testReaction() {
        if (skip) {
            return;
        }
        for (var emojy : Emojy.values()) {
            api.sendMessage(contact, emojy.name()).thenAcceptAsync(message -> api.sendReaction(message, emojy)).join();
        }
    }

    @Test
    @Order(45)
    public void testMediaDownload() {
        if (skip) {
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
                .map(info -> api.downloadMedia(info)
                        .thenApply(ignored -> success.incrementAndGet())
                        .exceptionallyAsync(ignored -> fail.incrementAndGet()))
                .collect(Collectors.collectingAndThen(Collectors.toUnmodifiableList(), list -> CompletableFuture.allOf(list.toArray(CompletableFuture[]::new))))
                .join();
        log("Decoded %s/%s medias!", success.get(), success.get() + fail.get());
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
