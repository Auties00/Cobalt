package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.ack.AckParser;
import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamService;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Objects;

/**
 * Sends peer protocol messages to one of the user's own devices.
 *
 * <p>Used for app-state-sync key shares and requests, fatal-exception
 * notifications, peer-data operation requests and responses, and
 * ephemeral-sync responses. Each send produces a single
 * {@code <message category="peer" push_priority="high">} stanza wrapping a
 * per-device Signal envelope with no {@code <participants>} layer.
 */
@WhatsAppWebModule(moduleName = "WAWebSendAppStateSyncMsgJob")
@WhatsAppWebModule(moduleName = "WAWebSendMsgCreateDeviceStanza")
final class PeerMessageSender extends MessageSender<ChatMessageInfo> {
    /**
     * The logger for {@link PeerMessageSender}.
     */
    private static final System.Logger LOGGER = Log.get(PeerMessageSender.class);

    /**
     * Performs per-device Signal encryption.
     */
    private final MessageEncryption encryption;

    /**
     * Ensures an E2E session is established before encryption.
     */
    private final DeviceService deviceService;

    /**
     * Constructs a {@link PeerMessageSender} bound to the supplied dependencies.
     *
     * <p>Constructed once by {@link MessageSendingService}; embedders should not
     * instantiate directly.
     *
     * @param client         the {@link LinkedWhatsAppClient} used to dispatch stanzas
     * @param encryption     the {@link MessageEncryption} service
     * @param deviceService  the {@link DeviceService} used to manage Signal
     *                       sessions
     * @param abPropsService the {@link ABPropsService} consulted by the base
     *                       sender
     * @param wamService     the {@link WamService} shared with the base sender
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSendAppStateSyncMsgJob", exports = "encryptAndSendKeyMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    PeerMessageSender(
            LinkedWhatsAppClient client,
            MessageEncryption encryption,
            DeviceService deviceService,
            ABPropsService abPropsService,
            WamService wamService
    ) {
        super(client, abPropsService, wamService);
        this.encryption = Objects.requireNonNull(encryption, "encryption");
        this.deviceService = Objects.requireNonNull(deviceService, "deviceService");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Encrypts the payload for the supplied {@code targetDevice}, wraps the
     * envelope in a {@code <message category="peer" push_priority="high">}
     * stanza alongside a {@code <meta appdata="default">} child and an optional
     * {@code <device-identity>} child (PKMSG only), and blocks until the server
     * returns the ack.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendAppStateSyncMsgJob", exports = "encryptAndSendKeyMsg",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCreateDeviceStanza", exports = "createUserDeviceMsgStanza",
            adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    AckResult doSend(Jid targetDevice, ChatMessageInfo messageInfo) {
        var container = messageInfo.message();
        var plaintext = LinkedMessageContainerSpec.encode(container);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "sending peer message {0} to {1}", messageInfo.key().id().orElse(null), targetDevice);
        }

        deviceService.ensureSessions(List.of(targetDevice));
        MessageEncryptedPayload payload;
        try {
            payload = encryption.encryptForDevice(targetDevice, plaintext);
            emitE2eMessageSendEvent(targetDevice, container, true, payload.type(), 0);
        } catch (RuntimeException encryptionError) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "peer message encryption failed for " + new LogRedactable.User(targetDevice.toString()), encryptionError);
            }
            emitE2eMessageSendEvent(targetDevice, container, false, null, 0);
            throw encryptionError;
        }

        var identityNode = payload.isPreKeyMessage()
                ? buildIdentityNode() : null;

        var encNode = new StanzaBuilder()
                .description("enc")
                .attribute("v", String.valueOf(MessageEncryption.CIPHERTEXT_VERSION))
                .attribute("type", payload.type().protocolValue())
                .content(payload.ciphertext())
                .build();

        var metaNode = new StanzaBuilder()
                .description("meta")
                .attribute("appdata", "default")
                .build();

        var stanza = new StanzaBuilder()
                .description("message")
                .attribute("id", messageInfo.key().id().orElseThrow())
                .attribute("to", targetDevice)
                .attribute("type", resolveStanzaType(container))
                .attribute("category", "peer")
                .attribute("push_priority", "high")
                .content(
                        encNode,
                        identityNode,
                        metaNode
                );

        flushStore();
        var ackNode = client.sendNode(stanza);
        var ack = AckParser.parse(ackNode);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "peer message send to {0} finished, success={1}", targetDevice, ack.isSuccess());
        }
        return ack;
    }

}
