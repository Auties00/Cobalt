package com.github.auties00.cobalt.message.send.senderkey;

import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.device.icdc.IcdcResult;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryptedPayload;
import com.github.auties00.cobalt.message.send.icdc.IcdcEnricher;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.linked.message.system.DeviceSentMessage;
import com.github.auties00.cobalt.wire.linked.message.system.DeviceSentMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.group.SenderKeyDistributionMessageBuilder;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSignalStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.lang.System.Logger.Level;
import java.util.*;

/**
 * Distributes the sender's group encryption key to participants that do not yet hold it.
 *
 * <p>Every SKMSG produced by {@link MessageEncryption#encryptForGroup(Jid, Jid, byte[])} can only
 * be decrypted by a device that already holds the sender's {@code SenderKeyDistributionMessage}.
 * This service encrypts that distribution message under each recipient device's per-device Signal
 * session and hands the resulting payloads back to the send pipeline, which ships them before the
 * SKMSG itself goes out. The group, status, and broadcast send paths each invoke one of the
 * encryption methods here.
 */
@WhatsAppWebModule(moduleName = "WAWebGetGroupKeyDistributionMsg")
public final class SenderKeyDistribution {
    /**
     * The logger for {@link SenderKeyDistribution}.
     */
    private static final System.Logger LOGGER = Log.get(SenderKeyDistribution.class);

    /**
     * Encrypts the distribution payload under each recipient device's Signal session.
     */
    private final MessageEncryption encryption;

    /**
     * Computes ICDC metadata for the sender and for each recipient account.
     */
    private final DeviceService deviceService;

    /**
     * Resolves the self {@link Jid} and records the identity range after distribution.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Constructs a sender-key distribution service bound to the given dependencies.
     *
     * <p>The three collaborators must share the same {@link LinkedWhatsAppStore} so that the post-send
     * {@link LinkedWhatsAppSignalStore#updateIdentityRange(Collection)} call and the per-recipient
     * {@link DeviceService#computeIcdc(Jid)} lookups observe consistent state.
     *
     * @param encryption    the {@link MessageEncryption} service for per-device encryption
     * @param deviceService the {@link DeviceService} for ICDC metadata
     * @param store         the {@link LinkedWhatsAppStore} for the self {@link Jid} and identity range
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebGetGroupKeyDistributionMsg", exports = "getKeyDistributionMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public SenderKeyDistribution(
            MessageEncryption encryption,
            DeviceService deviceService,
            LinkedWhatsAppStore store
    ) {
        this.encryption = Objects.requireNonNull(encryption, "encryption");
        this.deviceService = Objects.requireNonNull(deviceService, "deviceService");
        this.store = Objects.requireNonNull(store, "store");
    }

    /**
     * Encrypts the sender-key distribution message for each device without DSM wrapping or a participant hash.
     *
     * <p>Equivalent to {@link #encrypt(Jid, byte[], Collection, boolean, String)} with
     * {@code shouldWrapDsm} set to {@code false} and {@code phash} set to {@code null}. This is the
     * path taken by the group SKMSG and status send flows, where the distribution is not wrapped in
     * a {@link DeviceSentMessage}.
     *
     * @param groupJid       the group {@link Jid}
     * @param senderKeyBytes the serialised {@code SenderKeyDistributionMessage} bytes produced by
     *                       {@link MessageEncryption#getSenderKeyBytes(Jid, Jid)}
     * @param devices        the device {@link Jid}s that need the sender key
     * @return one {@link MessageEncryptedPayload} per device that successfully encrypted
     * @throws NullPointerException                  if any argument is {@code null}
     * @throws WhatsAppMessageException.Send.Unknown if encryption fails for a primary device
     */
    @WhatsAppWebExport(moduleName = "WAWebGetGroupKeyDistributionMsg", exports = "getKeyDistributionMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public List<MessageEncryptedPayload> encrypt(
            Jid groupJid,
            byte[] senderKeyBytes,
            Collection<Jid> devices
    ) {
        return encrypt(groupJid, senderKeyBytes, devices, false, null);
    }

    /**
     * Encrypts the sender-key distribution message for each device, with optional DSM wrapping and per-recipient ICDC metadata.
     *
     * <p>For each device the distribution proto is encrypted through the Signal session cipher,
     * producing a PKMSG for a fresh session and a MSG once the recipient has acknowledged a prior
     * PreKey. When {@code shouldWrapDsm} is {@code true} and the recipient is a self-companion, the
     * proto is wrapped as a {@link DeviceSentMessage} with the group as destination together with
     * {@code phash}. Each proto is stamped with the appropriate ICDC: sender-only for self devices,
     * sender plus recipient for other accounts. Companion devices whose encryption fails are dropped
     * from the result; a primary-device failure propagates as
     * {@link WhatsAppMessageException.Send.Unknown} so the orchestrator can abort the whole send.
     * After encryption the supplied device set is passed to
     * {@link LinkedWhatsAppSignalStore#updateIdentityRange(Collection)} so the next fanout sees the
     * freshly-keyed recipients in the identity range.
     *
     * @param groupJid       the group {@link Jid}
     * @param senderKeyBytes the serialised {@code SenderKeyDistributionMessage} bytes
     * @param devices        the device {@link Jid}s that need the sender key
     * @param shouldWrapDsm  whether to wrap self-device protos in a {@link DeviceSentMessage}
     * @param phash          the participant hash to include in the DSM, or {@code null}
     * @return the per-device encrypted payloads, in source iteration order, excluding any failed companions
     * @throws NullPointerException                  if {@code groupJid}, {@code senderKeyBytes}, or {@code devices} is {@code null}
     * @throws WhatsAppMessageException.Send.Unknown if encryption fails for a primary device
     */
    @WhatsAppWebExport(moduleName = "WAWebGetGroupKeyDistributionMsg", exports = "getKeyDistributionMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public List<MessageEncryptedPayload> encrypt(
            Jid groupJid,
            byte[] senderKeyBytes,
            Collection<Jid> devices,
            boolean shouldWrapDsm,
            String phash
    ) {
        Objects.requireNonNull(groupJid, "groupJid");
        Objects.requireNonNull(senderKeyBytes, "senderKeyBytes");
        Objects.requireNonNull(devices, "devices");

        var skDistMessage = new SenderKeyDistributionMessageBuilder()
                .groupJid(groupJid)
                .axolotlSenderKeyDistributionMessage(senderKeyBytes)
                .build();
        var baseContainer = LinkedMessageContainer.of(skDistMessage);

        var protosByUser = generateMsgProtobufs(
                baseContainer, devices, shouldWrapDsm, groupJid, phash);

        var results = new ArrayList<MessageEncryptedPayload>(devices.size());
        for (var device : devices) {
            try {
                var userKey = device.toUserJid();
                var proto = protosByUser.getOrDefault(userKey, baseContainer);
                var plaintext = LinkedMessageContainerSpec.encode(proto);
                var payload = encryption.encryptForDevice(device, plaintext);
                results.add(payload);
            } catch (Exception e) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "getKeyDistributionMsg: encryption fail for " + new LogRedactable.User(String.valueOf(device)), e);
                }
                if (isPrimaryDevice(device)) {
                    throw new WhatsAppMessageException.Send.Unknown(
                            "getKeyDistributionMsg: encryption fail for primary device " + device, e);
                }
            }
        }

        store.signalStore().updateIdentityRange(devices);

        return Collections.unmodifiableList(results);
    }

    /**
     * Encrypts a {@link DeviceSentMessage} carrying the participant hash for delivery to the caller's own companion devices.
     *
     * <p>Used by the broadcast send path when the caller has at least one companion device that also
     * needs to learn the {@code phash} (participant hash) of the broadcast list. The supplied
     * {@code message} is wrapped as a {@link DeviceSentMessage} with the group or broadcast as
     * destination, stamped with the sender's ICDC only because the targets are self devices, and
     * encrypted per companion device. Failures are logged and dropped so the broadcast can still
     * ship.
     *
     * @param message          the message proto to wrap as a {@link DeviceSentMessage}
     * @param companionDevices the companion device {@link Jid}s
     * @param phash            the participant hash to include in the DSM
     * @param groupJid         the group or broadcast {@link Jid} used as the DSM destination
     * @return the per-device encrypted payloads, or {@code null} when {@code companionDevices} is
     *         {@code null} or empty, or when every encryption attempt fails
     * @throws NullPointerException if {@code message}, {@code phash}, or {@code groupJid} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebGetGroupKeyDistributionMsg", exports = "getCompanionDsmPhashMsg",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebDeviceSentMessageProtoUtils", exports = "wrapDeviceSentMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public List<MessageEncryptedPayload> encryptCompanionDsmPhash(
            LinkedMessageContainer message,
            Collection<Jid> companionDevices,
            String phash,
            Jid groupJid
    ) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(phash, "phash");
        Objects.requireNonNull(groupJid, "groupJid");

        if (companionDevices == null || companionDevices.isEmpty()) {
            return null;
        }

        var selfJid = store.accountStore().jid().orElse(null);
        IcdcResult senderIcdc = null;
        if (selfJid != null) {
            senderIcdc = deviceService.computeIcdc(selfJid).orElse(null);
        }

        var dsm = new DeviceSentMessageBuilder()
                .destinationJid(groupJid)
                .messageContainer(message)
                .phash(phash)
                .build();
        var dsmContainer = LinkedMessageContainer.of(dsm);

        var enriched = IcdcEnricher.enrich(dsmContainer, senderIcdc, null);
        var plaintext = LinkedMessageContainerSpec.encode(enriched);

        var results = new ArrayList<MessageEncryptedPayload>(companionDevices.size());
        for (var device : companionDevices) {
            try {
                var payload = encryption.encryptForDevice(device, plaintext);
                results.add(payload);
            } catch (Exception e) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "getCompanionDsmPhashMsg: encryption fail for " + new LogRedactable.User(String.valueOf(device)), e);
                }
            }
        }

        return results.isEmpty() ? null : results;
    }

    /**
     * Precomputes the per-user message proto by deduplicating devices on their user JID and stamping ICDC plus optional DSM wrapping.
     *
     * <p>The sender's ICDC is computed exactly once and the recipient ICDC is computed per unique
     * non-self user, so a multi-device recipient does not pay the ICDC cost per companion. Self
     * devices receive a {@link DeviceSentMessage}-wrapped proto only when {@code shouldWrapDsm} is
     * {@code true}; other accounts receive the plain proto.
     *
     * @param baseContainer the base distribution container shared across users
     * @param devices       the device {@link Jid}s to plan for
     * @param shouldWrapDsm whether self-device protos are wrapped in a {@link DeviceSentMessage}
     * @param groupJid      the group {@link Jid} used as the DSM destination
     * @param phash         the participant hash for the DSM, or {@code null}
     * @return a map from unique user {@link Jid} to the proto for that user's devices
     */
    @WhatsAppWebExport(moduleName = "WAWebGetGroupKeyDistributionMsg", exports = "generateMsgProtobufs",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebDeviceSentMessageProtoUtils", exports = "wrapDeviceSentMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Map<Jid, LinkedMessageContainer> generateMsgProtobufs(
            LinkedMessageContainer baseContainer,
            Collection<Jid> devices,
            boolean shouldWrapDsm,
            Jid groupJid,
            String phash
    ) {
        var selfJid = store.accountStore().jid().orElse(null);

        IcdcResult senderIcdc = null;
        if (selfJid != null) {
            senderIcdc = deviceService.computeIcdc(selfJid).orElse(null);
        }

        var uniqueUsers = devices.stream()
                .map(Jid::toUserJid)
                .distinct()
                .toList();

        var result = new HashMap<Jid, LinkedMessageContainer>(uniqueUsers.size());

        for (var userJid : uniqueUsers) {
            var isSelf = selfJid != null && userJid.equals(selfJid.toUserJid());

            LinkedMessageContainer proto;

            IcdcResult recipientIcdc = null;
            if (isSelf) {
                if (shouldWrapDsm) {
                    var dsmBuilder = new DeviceSentMessageBuilder()
                            .destinationJid(groupJid)
                            .messageContainer(baseContainer);
                    if (phash != null) {
                        dsmBuilder.phash(phash);
                    }
                    proto = LinkedMessageContainer.of(dsmBuilder.build());
                } else {
                    proto = baseContainer;
                }
            } else {
                proto = baseContainer;
                recipientIcdc = deviceService.computeIcdc(userJid).orElse(null);
            }

            proto = IcdcEnricher.enrich(proto, senderIcdc, recipientIcdc);
            result.put(userJid, proto);
        }

        return result;
    }

    /**
     * Returns whether the given device {@link Jid} identifies a primary device.
     *
     * <p>Used by {@link #encrypt(Jid, byte[], Collection, boolean, String)} to distinguish a
     * primary-device encryption failure, which must abort the whole send, from a companion failure,
     * which is logged and dropped. The device identifier {@code 0} denotes the primary device; every
     * other identifier denotes a companion.
     *
     * @param device the device {@link Jid} to check
     * @return {@code true} when {@code device} is the primary device
     */
    @WhatsAppWebExport(moduleName = "WAWebSendMsgCommonApi", exports = "isPrimaryDevice",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static boolean isPrimaryDevice(Jid device) {
        return device.device() == 0;
    }
}
