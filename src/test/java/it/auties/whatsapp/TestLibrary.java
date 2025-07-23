package it.auties.whatsapp;

import it.auties.whatsapp.api.*;
import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.model.button.base.*;
import it.auties.whatsapp.model.button.interactive.InteractiveButtonBuilder;
import it.auties.whatsapp.model.button.interactive.InteractiveHeaderSimpleBuilder;
import it.auties.whatsapp.model.button.interactive.InteractiveNativeFlowBuilder;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplateSimpleBuilder;
import it.auties.whatsapp.model.button.template.hydrated.HydratedQuickReplyButtonBuilder;
import it.auties.whatsapp.model.button.template.hydrated.HydratedTemplateButton;
import it.auties.whatsapp.model.button.template.hydrated.HydratedURLButtonBuilder;
import it.auties.whatsapp.model.chat.*;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactCard;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.ChatMessageInfoBuilder;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.button.*;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.standard.*;
import it.auties.whatsapp.model.mobile.SixPartsKeys;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.newsletter.NewsletterMetadata;
import it.auties.whatsapp.model.newsletter.NewsletterName;
import it.auties.whatsapp.model.newsletter.NewsletterViewerRole;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.poll.PollOptionBuilder;
import it.auties.whatsapp.model.privacy.PrivacySettingType;
import it.auties.whatsapp.model.sync.HistorySyncMessage;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.ConfigUtils;
import it.auties.whatsapp.util.GithubActions;
import it.auties.whatsapp.util.MediaUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// IMPORTANT !!!!
// If you run this CI on a brand-new number it will 99% ban it because it adds a person to a group which is considered spam
// I repeat: DO NOT RUN THIS CI LOCALLY ON A BRAND-NEW NUMBER OR IT WILL GET BANNED
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class TestLibrary implements Listener {
    @SuppressWarnings("HttpUrlsUsage")
    private static final String VIDEO_URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";

    private static Whatsapp api;
    private static CountDownLatch latch;
    private static Jid contact;
    private static SixPartsKeys account;
    private static Jid group;

    @BeforeAll
    public void init() throws IOException, InterruptedException  {
        loadConfig();
        createApi();
        createLatch();
        latch.await();
    }

    private void createApi()  {
        log("Initializing api to start testing...");
        if(account == null) {
            log("Using web api to test...");
            api = configureApi(Whatsapp.webBuilder())
                    .unregistered(QrHandler.toTerminal());
        }else {
            log("Using mobile api to test...");
            api = configureApi(Whatsapp.mobileBuilder())
                    .device(CompanionDevice.ios(true)) // Make sure to select the correct account type(business or personal) or you'll get error 401
                    .registered()
                    .orElse(null);
        }
        if(api != null) {
            api.addListener(this)
                    .connect()
                    .exceptionally(Assertions::fail);
        }
    }

    private <T extends OptionsBuilder<T>> T configureApi(ConnectionBuilder<T> optionsBuilder) {
        return optionsBuilder.serializer(ControllerSerializer.discarding())
                .newConnection(Objects.requireNonNull(account, "Missing account"))
                .errorHandler((whatsapp, location, throwable) -> {
                    Assertions.fail(throwable);
                    System.exit(1);
                    return ErrorHandler.Result.DISCONNECT;
                });
    }

    private void loadConfig() throws IOException  {
        if (GithubActions.isActionsEnvironment())  {
            log("Loading environment variables...");
            contact = Jid.of(System.getenv(GithubActions.CONTACT_NAME));
            account = SixPartsKeys.of(System.getenv(GithubActions.ACCOUNT));
            log("Loaded environment variables...");
            return;
        }

        log("Loading configuration file...");
        var props = ConfigUtils.loadConfiguration();
        contact = Jid.of(Objects.requireNonNull(props.getProperty("contact"), "Missing contact property in config"));
        var account = props.getProperty("account");
        if(account != null) {
            TestLibrary.account = SixPartsKeys.of(account);
        }
        log("Loaded configuration file");
    }

    private void createLatch()  {
        latch = new CountDownLatch(4);
    }

    @Test
    @Order(1)
    public void testHasWhatsapp()  {
        var dummy = Jid.of("123456789");
        var response = api.hasWhatsapp(contact, dummy).join();
        var contactResponse = response.get(contact);
        Assertions.assertNotNull(contactResponse, "Missing response");
        var dummyResponse = response.get(dummy);
        Assertions.assertNotNull(dummy, "Missing response");
        Assertions.assertFalse(dummyResponse, "Erroneous response");
        try {
            Assertions.assertTrue(contactResponse, "Erroneous response");
        }catch (AssertionError error) {
            log("%s is not on WhatsApp: cannot run tests", contact);
            System.exit(1);
            throw error;
        }
    }

    @Test
    @Order(2)
    public void testChangeGlobalPresence()  {
        api.changePresence(false).join();
        assertContactOnlineStatus(ContactStatus.UNAVAILABLE);
        Assertions.assertFalse(api.store().online(), "Erroneous status");
        api.changePresence(true).join();
        assertContactOnlineStatus(ContactStatus.AVAILABLE);
    }

    private void assertContactOnlineStatus(ContactStatus contactStatus)  {
        var expectedOnlineStatus = api.store().online();
        var actualOnlineStatus = contactStatus != ContactStatus.UNAVAILABLE;
        Assertions.assertSame(expectedOnlineStatus, actualOnlineStatus, "Erroneous online status: expected %s, got %s".formatted(expectedOnlineStatus, actualOnlineStatus));
        var companionJid = api.store()
                .jid()
                .orElse(null);
       Assertions.assertNotNull(companionJid, "No companion jid");
       var companionContact = api.store()
               .findContactByJid(companionJid)
               .orElse(null);
        Assertions.assertNotNull(companionContact, "No companion contact");
        var actualContactStatus = companionContact.lastKnownPresence();
        Assertions.assertSame(actualContactStatus, contactStatus, "Erroneous contact status: expected %s, got %s".formatted(contactStatus, actualContactStatus));
    }

    @Test
    @Order(3)
    public void testUserPresenceSubscription()  {
        log("Subscribing to user presence...");
        var userPresenceResponse = api.subscribeToPresence(contact).join();
        log("Subscribed to user presence: %s", userPresenceResponse);
    }

    @Test
    @Order(3)
    public void testPrivacySettings()  {
        log("Changing privacy settings...");
        for(var settingType : PrivacySettingType.values()){
            for(var settingValue : settingType.supportedValues()){
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
    public void testPictureQuery()  {
        log("Loading picture...");
        var picResponse = api.queryPicture(contact).join();
        log("Loaded picture at: %s", picResponse);
    }

    @Test
    @Order(4)
    public void testChangeProfilePic()  {
        log("Setting picture...");
        var picResponse = api.changeProfilePicture(MediaUtils.readBytes("https://upload.wikimedia.org/wikipedia/commons/d/d2/Solid_white.png?20060513000852")).join();
        log("Result: %s", picResponse);
    }

    @Test
    @Order(5)
    public void testStatusQuery()  {
        log("Querying %s's status...", contact);
        api.queryAbout(contact)
                .join()
                .ifPresentOrElse(status -> log("Queried %s", status), () -> log("%s doesn't have a status", contact));
    }

    @Test
    @Order(8)
    public void testMarkChat()  {
        markAsUnread();
        markAsRead();
    }

    private void markAsUnread()  {
        log("Marking chat as unread...");
        var markStatus = api.markChatUnread(contact).join();
        log("Marked chat as unread: %s", markStatus);
    }

    private void markAsRead()  {
        log("Marking chat as read...");
        var markStatus = api.markChatRead(contact).join();
        log("Marked chat as read: %s", markStatus);
    }

    @Test
    @Order(9)
    public void testClearChat()  {
        log("Clearing chat...");
        var ephemeralResponse = api.clearChat(contact, false).join();
        log("Cleared chat: %s", ephemeralResponse);
    }

    @Test
    @Order(10)
    public void testDeleteChat()  {
        log("Deleting chat...");
        var ephemeralResponse = api.deleteChat(contact).join();
        log("Deleted chat: %s", ephemeralResponse);
    }

    @Test
    @Order(11)
    @Disabled
    public void testNewsletters() {
        log("Creating newsletter...");
        var newsletter = api.createNewsletter("Newsletter", "A brand new newsletter", null)
                .join()
                .orElseThrow(() -> new NoSuchElementException("Cannot create newsletter"));
        log("Created newsletter: %s", newsletter);

        // TODO: Change newsletter title

        log("Changing newsletter name...");
        api.changeNewsletterDescription(newsletter, "A new description")
                .join();
        log("Changed newsletter name");

        for(var role : NewsletterViewerRole.values()) {
            if(role != NewsletterViewerRole.UNKNOWN) {
                log("Querying newsletter as %s...", role);
                var result = api.queryNewsletter(newsletter.jid(), role)
                        .join();
                log("Queried newsletter: %s", result);
            }
        }

        log("Querying 100 messages...", newsletter);
        api.queryNewsletterMessages(newsletter, 100)
                .join();
        log("Queried %s messages", newsletter.messages().size());

        log("Querying subscribers...", newsletter);
        var subscribers = api.queryNewsletterSubscribers(newsletter).join();
        log("Queried subscribers: %s", subscribers);

        log("Querying recommended newsletters...", newsletter);
        var recommendedNewsletters = api.queryRecommendedNewsletters("IT")
                .join();
        log("Queried recommended newsletters: %s", recommendedNewsletters);

        if(!recommendedNewsletters.isEmpty()) {
            var recommendedNewsletter = recommendedNewsletters.getFirst();
            var nameOrJid = recommendedNewsletter.metadata()
                    .flatMap(NewsletterMetadata::name)
                    .map(NewsletterName::text)
                    .orElseGet(recommendedNewsletter.jid()::toString);
            log("Joining newsletter: %s", nameOrJid);
            api.joinNewsletter(recommendedNewsletter)
                    .join();
            System.out.println("Joined newsletter");

            log("Leaving newsletter: %s", nameOrJid);
            api.leaveNewsletter(recommendedNewsletter)
                    .join();
            System.out.println("Left newsletter");
        }
    }

    @Test
    @Order(11)
    public void testGroupCreation()  {
        log("Creating group...");
        var response = api.createGroup(randomId(), contact).join();
        Assertions.assertTrue(response.isPresent(), "Cannot create group");
        group = response.get().jid();
        log("Created group: %s", response);
    }

    @Test
    @Order(12)
    public void testChangeIndividualPresence()  {
        Assertions.assertNotNull(group, "No group");
        for (var presence : ContactStatus.values())  {
            log("Changing individual presence to %s...", presence.name());
            var response = api.changePresence(group, presence).join();
            log("Changed individual presence: %s", response);
        }
        api.changePresence(group, ContactStatus.AVAILABLE).join();
    }

    @Test
    @Order(13)
    public void testChangeGroupPicture()  {
        Assertions.assertNotNull(group, "No group");
        log("Changing group pic...");
        var picResponse = api.changeGroupPicture(group, MediaUtils.readBytes("https://upload.wikimedia.org/wikipedia/commons/d/d2/Solid_white.png?20060513000852")).join();
        log("Changed group pic: %s", picResponse);
    }

    @Test
    @Order(14)
    public void testChangeGroupName()  {
        Assertions.assertNotNull(group, "No group");
        log("Changing group name...");
        var changeGroupResponse = api.changeGroupSubject(group, "omega").join();
        log("Changed group name: %s", changeGroupResponse);
    }

    @RepeatedTest(2)
    @Order(15)
    public void testChangeGroupDescription()  {
        Assertions.assertNotNull(group, "No group");
        log("Changing group description...");
        var changeGroupResponse = api.changeGroupDescription(group, randomId()).join();
        log("Changed group description: %s", changeGroupResponse);
    }

    @Test
    @Order(16)
    public void testRemoveGroupParticipant()  {
        Assertions.assertNotNull(group, "No group");
        log("Removing %s...", contact);
        var changeGroupResponse = api.removeGroupParticipants(group, contact).join();
        log("Removed: %s", changeGroupResponse);
    }

    @Test
    @Order(17)
    public void testAddGroupParticipant()  {
        Assertions.assertNotNull(group, "No group");
        log("Adding %s...", contact);
        var changeGroupResponse = api.addGroupParticipants(group, contact).join();
        log("Added: %s", changeGroupResponse);
    }

    @Test
    @Order(18)
    public void testPromotion()  {
        Assertions.assertNotNull(group, "No group");
        log("Promoting %s...", contact);
        var changeGroupResponse = api.promoteGroupParticipants(group, contact).join();
        log("Promoted: %s", changeGroupResponse);
    }

    @Test
    @Order(19)
    public void testDemotion()  {
        Assertions.assertNotNull(group, "No group");
        log("Demoting %s...", contact);
        var changeGroupResponse = api.demoteGroupParticipants(group, contact).join();
        log("Demoted: %s", changeGroupResponse);
    }

    @Test
    @Order(20)
    public void testChangeAllGroupSettings()  {
        Assertions.assertNotNull(group, "No group");
        for(var setting : GroupSetting.values())  {
            for (var policy : ChatSettingPolicy.values())  {
                log("Changing setting %s to %s...", setting.name(), policy.name());
                api.changeGroupSetting(group, setting, policy).join();
                log("Changed setting %s to %s", setting.name(), policy.name());
            }
        }
    }

    @Test
    @Order(21)
    public void testGroupQuery()  {
        Assertions.assertNotNull(group, "No group");
        log("Querying group %s...", group);
        api.queryGroupMetadata(group).join();
        log("Queried group");
    }

    @Test
    @Order(22)
    public void testCommunity()  {
        log("Creating community...");
        var communityCreationResponse = api.createCommunity(randomId(), "A nice body")
                .join()
                .orElse(null);
        Assertions.assertNotNull(communityCreationResponse, "Cannot create community");
        log("Created community: %s", communityCreationResponse);

        log("Querying community metadata...");
        var communityMetadataResponse = api.queryCommunityMetadata(communityCreationResponse.jid()).join();
        Assertions.assertTrue(communityMetadataResponse.isCommunity(), "Expected a community");
        log("Queried community metadata: %s", communityMetadataResponse);

        log("Querying community link...");
        var link = api.queryGroupInviteLink(communityCreationResponse.jid()).join();
        log("Queried community link: " + link);

        log("Creating child group...");
        var placeholderResponse = api.createCommunityGroup(randomId(), ChatEphemeralTimer.THREE_MONTHS, communityCreationResponse.jid())
                .join()
                .orElse(null);
        Assertions.assertNotNull(placeholderResponse, "Cannot create child group");
        log("Created child group: %s", placeholderResponse);

        log("Creating another child group...");
        var communityChildCreationResponse = api.createCommunityGroup(randomId(), ChatEphemeralTimer.THREE_MONTHS, communityCreationResponse.jid())
                .join()
                .orElse(null);
        Assertions.assertNotNull(communityChildCreationResponse, "Cannot create another child group");
        api.addCommunityParticipants(communityMetadataResponse.jid(), contact).join();
        log("Created another child group: %s", communityChildCreationResponse);

        log("Querying child group metadata...");
        var communityChildMetadataResponse = api.queryGroupMetadata(communityChildCreationResponse.jid()).join();
        Assertions.assertFalse(communityChildMetadataResponse.isCommunity(), "Expected a group");
        log("Queried child group metadata: %s", communityChildMetadataResponse);

        log("Changing community pic...");
        var picResponse = api.changeCommunityPicture(communityCreationResponse.jid(), MediaUtils.readBytes("https://upload.wikimedia.org/wikipedia/commons/d/d2/Solid_white.png?20060513000852")).join();
        log("Changed community pic: %s", picResponse);

        log("Changing community subject...");
        var subjectResponse = api.changeCommunitySubject(communityCreationResponse.jid(), "Renamed").join();
        log("Changed community subject: %s", subjectResponse);

        for(var i = 0; i < 2; i++) {
            log("Changing community description...");
            var descriptionResponse = api.changeCommunityDescription(communityCreationResponse.jid(), randomId()).join();
            log("Changed community description: %s", descriptionResponse);
        }

        for (var setting : CommunitySetting.values()) {
            log("Changing community setting: %s", setting);
            log("Changing community setting %s to admin", setting);
            api.changeCommunitySetting(communityCreationResponse.jid(), setting, ChatSettingPolicy.ADMINS).join();
            log("Changing community setting %s to anyone", setting);
            api.changeCommunitySetting(communityCreationResponse.jid(), setting, ChatSettingPolicy.ANYONE).join();
            log("Changed community setting: %s", setting);
        }

        log("Unlinking child group...");
        var unlinkChildCommunityResponse = api.removeCommunityGroup(communityMetadataResponse.jid(), communityChildMetadataResponse.jid()).join();
        Assertions.assertTrue(unlinkChildCommunityResponse, "Failed unlink");
        log("Unlinked child group");

        log("Relinking child group...");
        var linkChildCommunityResponse = api.addCommunityGroups(communityMetadataResponse.jid(), communityChildMetadataResponse.jid()).join();
        Assertions.assertTrue(linkChildCommunityResponse.get(communityChildMetadataResponse.jid()), "Failed link");
        log("Relinked child group");

        log("Removing contact from community...");
        api.removeCommunityParticipants(communityCreationResponse.jid(), contact).join();
        log("Removed contact from community");

        log("Deleting community...");
        var deleteResponse = api.deactivateCommunity(communityCreationResponse.jid()).join();
        log("Deleted community: " + deleteResponse);
    }

    @Test
    @Order(23)
    public void testMute()  {
        Assertions.assertNotNull(group, "No group");
        log("Muting chat...");
        var muteResponse = api.muteChat(group, ChatMute.mutedForOneWeek()).join();
        log("Muted chat: %s", muteResponse);
    }

    @Test
    @Order(24)
    public void testUnmute()  {
        Assertions.assertNotNull(group, "No group");
        log("Unmuting chat...");
        var unmuteResponse = api.unmuteChat(group).join();
        log("Unmuted chat: %s", unmuteResponse);
    }

    @Test
    @Order(25)
    public void testArchive()  {
        Assertions.assertNotNull(group, "No group");
        log("Archiving chat...");
        var archiveResponse = api.archiveChat(group).join();
        log("Archived chat: %s", archiveResponse);
    }

    @Test
    @Order(26)
    public void testUnarchive()  {
        Assertions.assertNotNull(group, "No group");
        log("Unarchiving chat...");
        var unarchiveResponse = api.unarchive(group).join();
        log("Unarchived chat: %s", unarchiveResponse);
    }

    @Test
    @Order(27)
    public void testPin()  {
        Assertions.assertNotNull(group, "No group");
        if (api.store().pinnedChats().size() >= 3)  {
            log("Skipping chat pinning as there are already three chats pinned...");
            return;
        }
        log("Pinning chat...");
        var pinResponse = api.pinChat(group).join();
        log("Pinned chat: %s", pinResponse);
    }

    @Test
    @Order(28)
    public void testUnpin()  {
        Assertions.assertNotNull(group, "No group");
        if (api.store().pinnedChats().size() >= 3)  {
            log("Skipping chat unpinning as there are already three chats pinned...");
            return;
        }
        log("Unpinning chat...");
        var unpinResponse = api.unpinChat(group).join();
        log("Unpinned chat: %s", unpinResponse);
    }

    @Test
    @Order(29)
    public void testTextMessage()  {
        log("Sending simple text...");
        api.sendMessage(contact, "Hello").join();
        log("Sent simple text");
    }

    @Test
    @Order(30)
    public void testTextMessageExtended() {
        log("Sending youtube video...");
        api.sendMessage(contact, "Hello: https://www.youtube.com/watch?v=4boXExbbGCk").join();
        log("Sent youtube video");
        log("Sending article...");
        api.sendMessage(contact, "Hello: it.wikipedia.org/wiki/Vulcano") // Missing schema by design
                .join();
        log("Sent article");
    }

    @Test
    @Order(31)
    public void deleteMessage()  {
        var example = (ChatMessageInfo) api.sendMessage(contact, "Hello").join();
        log("Deleting for you...");
        api.deleteMessage(example, false).join();
        log("Deleted for you");
        log("Deleting for everyone...");
        api.deleteMessage(example, true).join();
        log("Deleted for everyone");
    }

    @Test
    @Order(32)
    public void testImageMessage()  { // Run image first to check for media timeout
        log("Sending image...");
        var image = new ImageMessageSimpleBuilder()
                .media(MediaUtils.readBytes("https://2.bp.blogspot.com/-DqXILvtoZFA/Wmmy7gRahnI/AAAAAAAAB0g/59c8l63QlJcqA0591t8-kWF739DiOQLcACEwYBhgL/s1600/pol-venere-botticelli-01.jpg"))
                .caption("Image test")
                .build();
        var textResponse = api.sendMessage(contact, image).join();
        log("Sent image: %s", textResponse);
    }

    @Test
    @Order(33)
    public void testAudioMessage()  {
        log("Sending audio...");
        var audio = new AudioMessageSimpleBuilder()
                .media(MediaUtils.readBytes("https://www.kozco.com/tech/organfinale.mp3"))
                .voiceMessage(true)
                .build();
        api.sendMessage(contact, audio).join();
        log("Sent audio");
    }

    @Test
    @Order(34)
    public void testVideoMessage()  {
        log("Sending video...");
        var video = new VideoMessageSimpleBuilder()
                .media(MediaUtils.readBytes(VIDEO_URL))
                .caption("Video")
                .build();
        api.sendMessage(contact, "Hello").join();
        api.sendMessage(contact, video).join();
        log("Sent video");
    }

    @Test
    @Order(35)
    public void testGifMessage()  {
        log("Sending gif...");
        var video = new GifMessageSimpleBuilder()
                .media(MediaUtils.readBytes(VIDEO_URL))
                .caption("Gif")
                .build();
        api.sendMessage(contact, video).join();
        log("Sent video");
    }

    @Test
    @Order(36)
    public void testPdfMessage()  {
        log("Sending pdf...");
        var document = new DocumentMessageSimpleBuilder()
                .media(MediaUtils.readBytes("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"))
                .title("Pdf test")
                .fileName("pdf-test.pdf")
                .pageCount(1)
                .build();
        api.sendMessage(contact, document).join();
        log("Sent pdf");
    }

    @Test
    @Order(37)
    public void testDocumentMessage()  {
        log("Sending document...");
        var document = new DocumentMessageSimpleBuilder()
                .media(MediaUtils.readBytes("https://calibre-ebook.com/downloads/demos/demo.docx"))
                .title("Document test")
                .fileName("doc-test.docx")
                .build();
        api.sendMessage(contact, document).join();
        log("Sent document");
    }

    @Test
    @Order(38)
    public void testPowerpointMessage()  {
        log("Sending powerpoint...");
        var document = new DocumentMessageSimpleBuilder()
                .media(MediaUtils.readBytes("https://scholar.harvard.edu/files/torman_personal/files/samplepptx.pptx"))
                .title("Presentation test")
                .fileName("presentation-test.pptx")
                .build();
        api.sendMessage(contact, document).join();
        log("Sent powerpoint");
    }

    @Test
    @Order(39)
    public void testContactMessage()  {
        log("Sending contact message...");
        var vcard = ContactCard.of("""
                        BEGIN:VCARD
                        VERSION:3.0
                        FN:John Doe
                        ORG:ABC Corporation
                        EMAIL:john.doe@example.com
                        TEL:(123) 456-7890
                        ADR:123 Main Street; Springfield; IL; 12345; USA
                        END:VCARD
                        """);
        var contactMessage = new ContactMessageBuilder()
                .name("John Doe")
                .vcard(vcard)
                .build();
        var response = api.sendMessage(contact, contactMessage).join();
        log("Sent contact: %s", response);
    }

    @Test
    @Order(40)
    public void testLocationMessage()  {
        log("Sending location message...");
        var location = new LocationMessageBuilder()
                .latitude(40.730610)
                .longitude(-73.935242)
                .magneticNorthOffset(0)
                .build();
        var textResponse = api.sendMessage(contact, location).join();
        log("Sent location: %s", textResponse);
    }

    @Test
    @Order(41)
    public void testGroupInviteMessage()  {
        Assertions.assertNotNull(group, "No group");
        log("Querying group invite countryCode");
        var code = api.queryGroupInviteCode(group).join();
        log("Queried %s", code);
        log("Sending group invite message...");
        var invite = new GroupInviteMessageBuilder()
                .group(group)
                .code(code)
                .expirationSeconds(ZonedDateTime.now().plusDays(3).toEpochSecond())
                .groupName(group.user())
                .build();
        var textResponse = api.sendMessage(contact, invite).join();
        log("Sent invite: %s", textResponse);
    }

    @Test
    @Order(42)
    public void testEnableEphemeralMessages()  {
        Assertions.assertNotNull(group, "No group");
        log("Enabling ephemeral messages...");
        var ephemeralResponse = api.changeEphemeralTimer(group, ChatEphemeralTimer.ONE_WEEK).join();
        log("Enabled ephemeral messages: %s", ephemeralResponse);
    }

    @Test
    @Order(43)
    public void testDisableEphemeralMessages()  {
        Assertions.assertNotNull(group, "No group");
        log("Disabling ephemeral messages...");
        var ephemeralResponse = api.changeEphemeralTimer(group, ChatEphemeralTimer.OFF).join();
        log("Disabled ephemeral messages: %s", ephemeralResponse);
    }

    @Test
    @Order(44)
    public void testLeave()  {
        Assertions.assertNotNull(group, "No group");
        log("Leaving group...");
        var ephemeralResponse = api.leaveGroup(group).join();
        log("Left group: %s", ephemeralResponse);
    }

    @Test
    @Order(45)
    public void testPollMessage()  {
        var pollOptionFirst = new PollOptionBuilder()
                .name("First")
                .build();
        var pollOptionSecond = new PollOptionBuilder()
                .name("Second")
                .build();
        var pollMessage = new PollCreationMessageBuilder()
                .title("Example poll")
                .selectableOptions(List.of(pollOptionFirst, pollOptionSecond))
                .selectableOptionsCount(2)
                .build();
        var pollInfo = (ChatMessageInfo) api.sendMessage(contact, pollMessage).join();
        var firstUpdate = new PollUpdateMessageSimpleBuilder()
                .poll(pollInfo)
                .votes(List.of(pollOptionFirst))
                .build();
        api.sendMessage(contact, firstUpdate).join();
        var secondUpdate = new PollUpdateMessageSimpleBuilder()
                .poll(pollInfo)
                .votes(List.of(pollOptionFirst, pollOptionSecond))
                .build();
        api.sendMessage(contact, secondUpdate).join();
        log("Sent poll message");
    }

    @Test
    @Order(46)
    public void testReaction()  {
        var values = Emoji.values();
        for (var i = 0; i < 10; i++) {
            var emoji = values[i];
            api.sendMessage(contact, emoji.name())
                    .thenAcceptAsync(message -> api.sendReaction(message, emoji)).join();
        }
    }

    @Test
    @Order(47)
    public void testMediaDownload()  {
        log("Trying to decode some medias...");
        var success = new AtomicInteger();
        var fail = new AtomicInteger();
        api.store()
                .chats()
                .stream()
                .map(Chat::messages)
                .flatMap(Collection::stream)
                .map(HistorySyncMessage::messageInfo)
                .filter(info -> !info.fromMe() && info.message().category() == Message.Category.MEDIA)
                .limit(30)
                .map(info -> api.downloadMedia(info)
                        .thenApply(ignored -> success.incrementAndGet())
                        .exceptionallyAsync(ignored -> fail.incrementAndGet()))
                .collect(Collectors.collectingAndThen(Collectors.toUnmodifiableList(), list -> CompletableFuture.allOf(list.toArray(CompletableFuture[]::new))))
                .join();
        log("Decoded %s/%s medias!", success.get(), success.get() + fail.get());
    }

    @Test
    @Order(48)
    public void testButtonsMessage()  {
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

    private List<Button> createButtons()  {
        return IntStream.range(0, 3)
            .mapToObj(this::createButton)
            .map(Button::of)
            .toList();
    }

    private ButtonText createButton(int index) {
        return new ButtonTextBuilder()
                .content("Button %s".formatted(index))
                .build();
    }

    @Test
    @Order(49)
    public void testListMessage()  {
        var buttons = List.of(ButtonRow.of("First option", "A nice description"), ButtonRow.of("Second option", "A nice description"), ButtonRow.of("Third option", "A nice description"));
        var section = new ButtonSectionBuilder()
                .title("First section")
                .rows(buttons)
                .build();
        var otherButtons = List.of(ButtonRow.of("First option", "A nice description"), ButtonRow.of("Second option", "A nice description"), ButtonRow.of("Third option", "A nice description"));
        var anotherSection = new ButtonSectionBuilder()
                .title("First section")
                .rows(otherButtons)
                .build();
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
                .id(ChatMessageKey.randomId(api.store().clientType()))
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
    @Order(50)
    public void testTemplateMessage()  {
        log("Sending template message...");
        var quickReplyButton = new HydratedQuickReplyButtonBuilder()
                .text("Click me")
                .build();
        var quickReplyTemplateButton = HydratedTemplateButton.of(quickReplyButton);
        var urlButton = new HydratedURLButtonBuilder()
                .text("Click me")
                .url( "https://google.com")
                .build();
        var urlTemplateButton = HydratedTemplateButton.of(urlButton);
        var callButton = new HydratedURLButtonBuilder()
                .text("Call me")
                .url(contact.toPhoneNumber().orElseThrow())
                .build();
        var callTemplateButton = HydratedTemplateButton.of(callButton);
        var fourRowTemplate = new HydratedFourRowTemplateSimpleBuilder()
                .body("A nice body")
                .footer("A nice footer")
                .buttons(List.of(quickReplyTemplateButton, urlTemplateButton, callTemplateButton))
                .build();
        var template = new TemplateMessageSimpleBuilder()
                .format(fourRowTemplate)
                .build();
        api.sendMessage(contact, template).join();
        log("Sent template message");
    }

    // Just have a test to see if it gets sent, it's not a functioning button because it's designed for more complex use cases
    @Test
    @Order(51)
    public void testInteractiveMessage()  {
        log("Sending interactive messages..");
        var reviewAndPay = new InteractiveButtonBuilder()
                .name("review_and_pay")
                .build();
        var reviewOrder = new InteractiveButtonBuilder()
                .name("review_order")
                .build();
        var nativeFlowMessage = new InteractiveNativeFlowBuilder()
                .buttons(List.of(reviewAndPay, reviewOrder))
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
    public void testDisconnect()  {
        log("Logging off...");
        api.disconnect().join();
        log("Logged off");
    }

    @Override
    public void onNodeSent(Node outgoing)  {
        System.out.printf("Sent node %s%n", outgoing);
    }

    @Override
    public void onNodeReceived(Node incoming)  {
        System.out.printf("Received node %s%n", incoming);
    }

    @Override
    public void onLoggedIn()  {
        latch.countDown();
        log("Logged in: -%s", latch.getCount());
    }

    @Override
    public void onDisconnected(DisconnectReason reason)  {
        System.out.printf("Disconnected: %s%n", reason);
        if(reason != DisconnectReason.RECONNECTING) {
            System.exit(1);
        }
    }

    @Override
    public void onContacts(Collection<Contact> contacts)  {
        latch.countDown();
        log("Got contacts: -%s", latch.getCount());
    }

    @Override
    public void onChats(Collection<Chat> chats)  {
        latch.countDown();
        log("Got chats: -%s", latch.getCount());
    }

    @Override
    public void onNewsletters(Collection<Newsletter> newsletters) {
        latch.countDown();
        log("Got newsletters: %s", latch.getCount());
    }

    @Override
    public void onChatMessagesSync(Chat chat, boolean last) {
        if (!last)  {
            return;
        }

        System.out.printf("%s is ready with %s messages%n", chat.name(), chat.messages().size());
    }

    @Override
    public void onNewMessage(Whatsapp whatsapp, MessageInfo<?> info) {
        System.out.println(info);
    }

    private String randomId()  {
        return HexFormat.of().formatHex(Bytes.random(5));
    }

    private void log(String message, Object... params)  {
        System.out.printf(message + "%n", redactParameters(params));
    }

    private Object[] redactParameters(Object... params)  {
        return params;
    }
}
