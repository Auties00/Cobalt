package com.github.auties00.cobalt.message.send;
import com.github.auties00.cobalt.media.transcode.LiveMediaTranscoderService;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.StubDeviceService;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.media.transcode.MediaTranscoderService;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfoBuilder;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import com.github.auties00.cobalt.message.crypto.SignalCryptoLocks;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the {@link MessageSendingService} validation and rejection branches:
 * a {@link com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo} to a
 * newsletter JID and a
 * {@link com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo}
 * to a non-newsletter JID both raise
 * {@link WhatsAppMessageException.Send.InvalidRecipient}; a missing
 * {@code messageId} or {@code parentJid} raises
 * {@link IllegalArgumentException};
 * {@link MessageSendingService#sendKeyDistribution(Jid, MessageKey)} rejects
 * non-group JIDs; null arguments raise {@link NullPointerException}. The
 * successful dispatch-by-sender-kind path is left to the per-sender families
 * and the live-corpus oracle; this suite needs only a minimal real DI graph.
 */
@DisplayName("MessageSendingService")
class MessageSendingServiceTest {

    private static final Jid SELF_PN = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("258252122116273@lid");
    private static final Jid PEER_PN = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid GROUP = Jid.of("120363023250764418@g.us");
    private static final Jid NEWSLETTER = Jid.of("120363402045452944@newsletter");

    @Test
    @DisplayName("send(LinkedMessageInfo): ChatMessageInfo with a newsletter parent JID -> InvalidRecipient")
    void chatMessageInfoToNewsletterFails() {
        var service = buildService();
        var info = new ChatMessageInfoBuilder()
                .key(new MessageKeyBuilder()
                        .id("3EB0CROSS01")
                        .parentJid(NEWSLETTER)
                        .fromMe(true)
                        .build())
                .message(LinkedMessageContainer.of("nope"))
                .build();

        assertThrows(WhatsAppMessageException.Send.InvalidRecipient.class,
                () -> service.send(info),
                "ChatMessageInfo to a newsletter JID is an unsupported combination");
    }

    @Test
    @DisplayName("send(LinkedMessageInfo): NewsletterMessageInfo with a non-newsletter parent JID -> InvalidRecipient")
    void newsletterMessageInfoToChatFails() {
        var service = buildService();
        var info = new NewsletterMessageInfoBuilder()
                .key(new MessageKeyBuilder()
                        .id("3EB0CROSS02")
                        .parentJid(PEER_PN)
                        .fromMe(true)
                        .build())
                .serverId(1)
                .message(LinkedMessageContainer.of("nope"))
                .build();

        assertThrows(WhatsAppMessageException.Send.InvalidRecipient.class,
                () -> service.send(info),
                "NewsletterMessageInfo to a non-newsletter JID is an unsupported combination");
    }

    @Test
    @DisplayName("send(LinkedMessageInfo): missing messageId on the key throws IllegalArgumentException")
    void missingMessageIdThrows() {
        var service = buildService();
        var info = new ChatMessageInfoBuilder()
                .key(new MessageKeyBuilder()
                        .parentJid(PEER_PN)
                        .fromMe(true)
                        .build())
                .message(LinkedMessageContainer.of("hi"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> service.send(info));
    }

    @Test
    @DisplayName("send(LinkedMessageInfo): missing parentJid on the key throws IllegalArgumentException")
    void missingParentJidThrows() {
        var service = buildService();
        var info = new ChatMessageInfoBuilder()
                .key(new MessageKeyBuilder()
                        .id("3EB0NOPARENT")
                        .fromMe(true)
                        .build())
                .message(LinkedMessageContainer.of("hi"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> service.send(info));
    }

    @Test
    @DisplayName("sendKeyDistribution: non-group JID throws InvalidRecipient")
    void sendKeyDistributionNonGroup() {
        var service = buildService();
        var key = new MessageKeyBuilder()
                .id("3EB0SKD0001")
                .parentJid(PEER_PN)
                .fromMe(true)
                .build();
        assertThrows(WhatsAppMessageException.Send.InvalidRecipient.class,
                () -> service.sendKeyDistribution(PEER_PN, key));
    }

    @Test
    @DisplayName("sendKeyDistribution: key without id throws IllegalArgumentException")
    void sendKeyDistributionMissingId() {
        var service = buildService();
        var key = new MessageKeyBuilder()
                .parentJid(GROUP)
                .fromMe(true)
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> service.sendKeyDistribution(GROUP, key));
    }

    @Test
    @DisplayName("send(Jid, LinkedMessageContainer): null arguments throw NullPointerException")
    void sendNullArgs() {
        var service = buildService();
        assertThrows(NullPointerException.class,
                () -> service.send(null, LinkedMessageContainer.of("hi")));
        assertThrows(NullPointerException.class,
                () -> service.send(PEER_PN, null));
    }

    @Test
    @DisplayName("send(LinkedMessageInfo): null arg throws NullPointerException")
    void sendInfoNullArg() {
        var service = buildService();
        assertThrows(NullPointerException.class, () -> service.send(null));
    }

    @Test
    @DisplayName("sendPeer: null arguments throw NullPointerException")
    void sendPeerNullArgs() {
        var service = buildService();
        var info = new ChatMessageInfoBuilder()
                .key(new MessageKeyBuilder()
                        .id("3EB0PEER01")
                        .parentJid(SELF_PN)
                        .fromMe(true)
                        .build())
                .message(LinkedMessageContainer.of("peer payload"))
                .build();
        assertThrows(NullPointerException.class,
                () -> service.sendPeer(null, info));
        assertThrows(NullPointerException.class,
                () -> service.sendPeer(SELF_PN, null));
    }

    private static MessageSendingService buildService() {
        var store = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(TestABPropsService.builder().build());
        var encryption = new MessageEncryption(store,
                new SignalSessionCipher(store.signalStore()),
                new SignalGroupCipher(store.signalStore()),
                new SignalCryptoLocks());
        var wam = new LiveWamService(client, client.abPropsService());
        var migration = new LiveLidMigrationService(client, client.abPropsService(), wam);
        return new LiveMessageSendingService(client, encryption,
                StubDeviceService.create(), migration, client.abPropsService(), wam,
                new LiveMediaTranscoderService(client, client.abPropsService(), TestMediaConnectionService.create(), wam));
    }
}
