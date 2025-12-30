package com.github.auties00.cobalt.device;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.device.adv.DeviceADVValidator;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.util.SecureBytes;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;
import com.github.auties00.libsignal.groups.SignalSenderKeyName;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;
import com.github.auties00.libsignal.protocol.SignalSenderKeyDistributionMessage;
import com.github.auties00.libsignal.state.SignalPreKeyBundleBuilder;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DeviceService {
    private final WhatsAppClient client;
    private final SignalSessionCipher sessionCipher;
    private final SignalGroupCipher groupCipher;

    public DeviceService(WhatsAppClient client, SignalSessionCipher sessionCipher, SignalGroupCipher groupCipher) {
        this.client = client;
        this.sessionCipher = sessionCipher;
        this.groupCipher = groupCipher;
    }

    /**
     * Queries the device list for message sending and ensures Signal sessions exist.
     * This includes querying the device list via usync and fetching pre-keys for devices
     * that messages should be encrypted for.
     *
     * @param jids the list of user JIDs to query devices for
     * @return the swr of device JIDs that should receive the encrypted message
     */
    public Set<? extends Jid> queryDevices(Collection<? extends Jid> jids) {
        if (jids == null) {
            return Set.of();
        }

        // Get all devices for the given JIDs
        var devices = queryDevicesForJids(jids);
        if (devices.isEmpty()) {
            return Set.of();
        }

        // Ensure sessions exist for all devices
        var devicesNeedingSessions = devices.stream()
                .filter(device -> client.store()
                        .findSessionByAddress(device.toSignalAddress()).isEmpty())
                .collect(Collectors.toUnmodifiableSet());

        if (!devicesNeedingSessions.isEmpty()) {
            fetchPreKeysAndCreateSessions(devicesNeedingSessions);
        }

        return devices;
    }

    private Set<? extends Jid> queryDevicesForJids(Collection<? extends Jid> jids) {
        var userNodes = jids.stream()
                .distinct()
                .map(this::buildUserNode)
                .toList();

        var devicesNode = new NodeBuilder()
                .description("devices")
                .attribute("version", "2")
                .build();
        var queryNode = new NodeBuilder()
                .description("query")
                .content(devicesNode)
                .build();
        var listNode = new NodeBuilder()
                .description("list")
                .content(userNodes.toArray(Node[]::new))
                .build();
        var sideListNode = new NodeBuilder()
                .description("side_list")
                .build();
        var syncNode = new NodeBuilder()
                .description("usync")
                .attribute("sid", SecureBytes.randomSid())
                .attribute("mode", "query")
                .attribute("last", "true")
                .attribute("index", "0")
                .attribute("context", "message")
                .content(queryNode, listNode, sideListNode)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "usync")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(syncNode);

        var response = client.sendNode(iqNode);

        return response.streamChildren("usync")
                .flatMap(node -> node.streamChild("list"))
                .flatMap(node -> node.streamChildren("user"))
                .flatMap(this::parseDevice)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Stream<Jid> parseDevice(Node user) {
        var userJid = user.getAttributeAsJid("jid");
        if (userJid.isEmpty()) {
            return Stream.empty();
        } else {
            return user.streamChild("devices")
                    .flatMap(devices -> devices.streamChild("device-list"))
                    .flatMap(deviceList -> deviceList.streamChildren("device"))
                    .map(device -> {
                        var deviceId = (int) device.getAttributeAsLong("id", 0L);
                        return userJid.get().withDevice(deviceId);
                    });
        }
    }

    private void fetchPreKeysAndCreateSessions(Set<? extends Jid> devices) {
        if (devices.isEmpty()) {
            return;
        }

        var keyNodes = devices.stream()
                .map(this::buildUserNode)
                .toArray(Node[]::new);
        var keyNode = new NodeBuilder()
                .description("key")
                .content(keyNodes)
                .build();
        var iqNode = new NodeBuilder()
                .description("iq")
                .attribute("xmlns", "encrypt")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(keyNode);

        client.sendNode(iqNode)
                .streamChild("list")
                .flatMap(list -> list.streamChildren("user"))
                .forEach(this::processPreKeyResponse);
    }

    private Node buildUserNode(Jid jid) {
        return new NodeBuilder()
                .description("user")
                .attribute("jid", jid)
                .build();
    }

    private void processPreKeyResponse(Node userNode) {
        var localJid = client.store()
                .jid()
                .orElseThrow(() -> new IllegalStateException("Local jid is not available"));
        var companionIdentity = client.store()
                .companionIdentity()
                .orElseThrow(() -> new IllegalStateException("Companion identity is not available"));

        var remoteJid = userNode.getRequiredAttributeAsJid("jid");
        var registrationId = userNode.getChild("registration")
                .flatMap(Node::toContentBytes)
                .map(bytes -> SecureBytes.bytesToInt(bytes, Math.min(bytes.length, 4)))
                .orElse(0);
        var identityKeyBytes = userNode.getChild("identity")
                .flatMap(Node::toContentBytes)
                .orElse(null);
        var identityKey = identityKeyBytes != null
                ? SignalIdentityPublicKey.ofDirect(identityKeyBytes)
                : null;

        var signedPreKey = userNode.getChild("skey", null);
        var preKey = userNode.getChild("key", null);
        if (identityKey == null || signedPreKey == null) {
            return;
        }

        DeviceADVValidator.extractAndValidateRemoteSignedDeviceIdentity(localJid, remoteJid, companionIdentity, userNode, identityKeyBytes);

        var signedPreKeyId = signedPreKey.getChild("id")
                .flatMap(Node::toContentBytes)
                .map(bytes -> SecureBytes.bytesToInt(bytes, Math.min(bytes.length, 4)))
                .orElse(0);
        var signedPreKeyPublic = signedPreKey.getChild("value")
                .flatMap(Node::toContentBytes)
                .map(SignalIdentityPublicKey::ofDirect)
                .orElse(null);
        var signedPreKeySignature = signedPreKey.getChild("signature")
                .flatMap(Node::toContentBytes)
                .orElse(null);
        if (signedPreKeyPublic == null || signedPreKeySignature == null) {
            return;
        }

        int preKeyId = 0;
        SignalIdentityPublicKey preKeyPublic = null;
        if (preKey != null) {
            preKeyId = preKey.getChild("id")
                    .flatMap(Node::toContentBytes)
                    .map(bytes -> SecureBytes.bytesToInt(bytes, Math.min(bytes.length, 4)))
                    .orElse(0);
            preKeyPublic = preKey.getChild("value")
                    .flatMap(Node::toContentBytes)
                    .map(SignalIdentityPublicKey::ofDirect)
                    .orElse(null);
        }

        var bundle = new SignalPreKeyBundleBuilder()
                .registrationId(registrationId)
                .deviceId(remoteJid.device())
                .preKeyId(preKeyId)
                .preKeyPublic(preKeyPublic)
                .signedPreKeyId(signedPreKeyId)
                .signedPreKeyPublic(signedPreKeyPublic)
                .signedPreKeySignature(signedPreKeySignature)
                .identityKey(identityKey)
                .build();

        sessionCipher.process(remoteJid.toSignalAddress(), bundle);
    }

    public void processDistributionMessage(SignalSenderKeyName groupName, SignalSenderKeyDistributionMessage signalDistributionMessage) {
        groupCipher.process(groupName, signalDistributionMessage);
    }
}
