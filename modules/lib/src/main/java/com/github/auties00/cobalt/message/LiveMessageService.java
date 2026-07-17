package com.github.auties00.cobalt.message;

import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.media.transcode.MediaTranscoderService;
import com.github.auties00.cobalt.message.crypto.SignalCryptoLocks;
import com.github.auties00.cobalt.message.receive.LiveMessageReceivingService;
import com.github.auties00.cobalt.message.receive.MessageReceivingService;
import com.github.auties00.cobalt.message.receive.crypto.MessageDecryption;
import com.github.auties00.cobalt.message.send.LiveMessageSendingService;
import com.github.auties00.cobalt.message.send.MessageSendingService;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Live implementation of {@link MessageService} that fans message traffic between the outbound
 * send pipeline and the inbound receive pipeline behind a single facade.
 *
 * <p>The two sub-services are assembled from the supplied collaborators in the constructor and
 * share the {@link LinkedWhatsAppClient#store() client store}, so the send and receive sides
 * observe a single source of truth for sessions, devices, and pending-message caches.
 *
 * @implNote This implementation collapses WA Web's two separate entry points,
 * {@code WAWebSendMsgJob.encryptAndSendMsg} for outbound fanout and
 * {@code WAWebCommsHandleMessagingStanza.handleMessagingStanza} for inbound
 * dispatch, into one facade that owns no state of its own.
 */
public final class LiveMessageService implements MessageService {
    /** The logger for {@link LiveMessageService}. */
    private static final System.Logger LOGGER = Log.get(LiveMessageService.class);

    /**
     * Holds the {@link MessageDecryption} used by
     * {@link #processCall(Jid, MessageEncryptionType, byte[])}.
     */
    private final MessageDecryption decryption;

    /**
     * Holds the {@link DeviceService} used by {@link #resolveCallPeerAddressing(Jid)} to resolve a
     * peer's LID and synchronise its device list before a call offer.
     */
    private final DeviceService deviceService;

    /**
     * Holds the {@link LidMigrationService} used by {@link #resolveCallPeerAddressing(Jid)} to resolve a
     * call peer to its LID, and by the user-chat send path for the PN-to-LID stanza rewrite.
     */
    private final LidMigrationService lidMigrationService;

    /**
     * Holds the outbound pipeline owning device fetch, fanout, encryption, and
     * stanza emission.
     */
    private final MessageSendingService sendingService;

    /**
     * Holds the inbound pipeline owning stanza parsing, Signal decryption, and
     * {@link LinkedMessageInfo} construction.
     */
    private final MessageReceivingService receivingService;


    /**
     * Wires the send and receive pipelines from the supplied collaborators.
     *
     * <p>The two pipelines share the {@link LinkedWhatsAppClient#store() client store}
     * via the supplied {@link MessageEncryption} and {@link MessageDecryption}, which
     * must themselves share a single {@link SignalCryptoLocks} registry so a concurrent
     * encrypt and decrypt for the same device session or sender-key chain serialise rather
     * than racing the non-atomic Signal ratchet. The caller (typically
     * {@code LiveLinkedWhatsAppClient}) constructs both encryption helpers from one
     * lock registry and hands them in here.
     *
     * @param client              the {@link LinkedWhatsAppClient} used to send
     *                            stanzas and to register inbound stanza
     *                            handlers
     * @param encryption          the {@link MessageEncryption} used by the outbound
     *                            pipeline; the same instance must be shared with any
     *                            other service that encrypts to Signal sessions (the
     *                            call-signaling layer's offer fanout)
     * @param decryption          the {@link MessageDecryption} used by the inbound
     *                            pipeline; must share its lock registry with {@code encryption}
     * @param deviceService       the {@link DeviceService} consulted to
     *                            resolve per-user device lists before each
     *                            fanout
     * @param lidMigrationService the {@link LidMigrationService} that gates
     *                            the PN-to-LID stanza rewrite in the
     *                            user-chat send path
     * @param abPropsService      the {@link ABPropsService} consulted to
     *                            gate optional protocol behaviour
     * @param wamService             the {@link WamService} forwarded to
     *                               the sending pipeline for end-to-end
     *                               telemetry events
     * @param mediaTranscoderService the {@link MediaTranscoderService}
     *                               threaded into the sending pipeline
     *                               for link-preview decoration
     * @throws NullPointerException if any argument is {@code null}
     */
    public LiveMessageService(
            LinkedWhatsAppClient client,
            MessageEncryption encryption,
            MessageDecryption decryption,
            DeviceService deviceService,
            LidMigrationService lidMigrationService,
            ABPropsService abPropsService,
            WamService wamService,
            MediaTranscoderService mediaTranscoderService
    ) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(encryption, "encryption");
        Objects.requireNonNull(decryption, "decryption");
        Objects.requireNonNull(deviceService, "deviceService");
        Objects.requireNonNull(lidMigrationService, "lidMigrationService");
        Objects.requireNonNull(abPropsService, "abPropsService");
        Objects.requireNonNull(wamService, "wamService");
        Objects.requireNonNull(mediaTranscoderService, "mediaTranscoderService");

        this.decryption = decryption;
        this.deviceService = deviceService;
        this.lidMigrationService = lidMigrationService;
        var store = client.store();
        this.sendingService = new LiveMessageSendingService(client, encryption, deviceService, lidMigrationService, abPropsService, wamService, mediaTranscoderService);
        this.receivingService = new LiveMessageReceivingService(store, decryption, wamService);
    }

    @Override
    public AckResult send(Jid chatJid, LinkedMessageContainer container) {
        return sendingService.send(chatJid, container);
    }

    @Override
    public AckResult send(LinkedMessageInfo messageInfo) {
        return sendingService.send(messageInfo);
    }

    @Override
    public AckResult sendPeer(Jid targetDevice, ChatMessageInfo messageInfo) {
        return sendingService.sendPeer(targetDevice, messageInfo);
    }

    @Override
    public LinkedMessageInfo process(Stanza stanza) {
        return receivingService.process(stanza);
    }

    @Override
    public void clearPendingMessages() {
        receivingService.clearPendingMessages();
    }

    @Override
    public CallPeerAddressing resolveCallPeerAddressing(Jid peer) {
        Objects.requireNonNull(peer, "peer cannot be null");

        // Resolve the peer to its LID: a cached mapping first, then a focused USync <lid> query. A peer
        // with no resolvable LID aborts the call; WA never falls back to a phone-number offer (rejected
        // with error="439").
        Jid peerLid;
        if (peer.hasLidServer()) {
            peerLid = peer.toUserJid();
        } else {
            var cached = lidMigrationService.toLid(peer);
            peerLid = cached != null
                    ? cached.toUserJid()
                    : deviceService.queryUserLid(peer.toUserJid()).map(Jid::toUserJid).orElse(null);
        }
        if (peerLid == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "call peer addressing failed, no lid resolved for {0}", peer);
            throw new IllegalStateException("Cannot place a call: no LID is known for peer "
                    + peer.toUserJid() + "; WhatsApp rejects phone-number-addressed call offers");
        }

        // Sync the peer's device list in the resolved LID addressing mode and fan out to every device.
        var deviceLists = deviceService.syncAndGetDeviceList(List.of(peerLid));
        var peerDeviceJids = new ArrayList<Jid>();
        for (var list : deviceLists) {
            for (var device : list.devices()) {
                peerDeviceJids.add(list.userJid().withDevice(device.id()));
            }
        }
        // A peer whose cached device record is a deleted tombstone yields no devices; fall back to the
        // primary device so the offer still addresses the peer, matching WA Web's getFanOutList primary
        // fallback.
        if (peerDeviceJids.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call peer {0} has no synced devices, falling back to primary device", peerLid);
            peerDeviceJids.add(peerLid.withDevice(0));
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call peer addressing resolved for {0}, devices={1}", peerLid, peerDeviceJids.size());
        return new CallPeerAddressing(peerLid, peerDeviceJids);
    }

    @Override
    public byte[] processCall(Jid senderJid, MessageEncryptionType encType, byte[] ciphertext) {
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        Objects.requireNonNull(encType, "encType cannot be null");
        Objects.requireNonNull(ciphertext, "ciphertext cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "decrypting call payload from {0}, type={1}, bytes={2}", senderJid, encType, ciphertext.length);
        var plaintext = decryption.decryptFromDevice(ciphertext, senderJid, encType);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call payload decrypted from {0}, bytes={1}", senderJid, plaintext.length);
        return plaintext;
    }

}
