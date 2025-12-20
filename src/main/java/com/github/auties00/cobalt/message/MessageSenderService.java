package com.github.auties00.cobalt.message;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.message.crypto.DevicePhashEncoder;
import com.github.auties00.cobalt.message.crypto.MessageEncoder;
import com.github.auties00.cobalt.model.auth.SignedDeviceIdentitySpec;
import com.github.auties00.cobalt.model.chat.ChatParticipant;
import com.github.auties00.cobalt.model.info.MessageInfo;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.model.MessageContainer;
import com.github.auties00.cobalt.model.message.server.DeviceSentMessage;
import com.github.auties00.cobalt.model.message.server.DeviceSentMessageBuilder;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.store.WhatsAppStore;
import com.github.auties00.cobalt.util.Clock;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.auties00.cobalt.util.SignalProtocolConstants.*;

/**
 * Service for sending encrypted messages through the WhatsApp protocol.
 * Handles device fanout, encryption, and message node construction.
 */
public final class MessageSenderService {
    private static final String ENC_VERSION = "2";
    private static final int RESEND_TIMEOUT_SECONDS = 600; // 10 minutes
    private static final int ERROR_STALE_ADDRESSING_MODE = 421;

    private final WhatsAppClient whatsapp;
    private final WhatsAppStore store;
    private final MessageEncoder encoder;

    /**
     * Tracks which devices have received our sender key for each group.
     * Key: group JID string, Value: set of device JIDs that have received the key.
     * This is cleared when the sender key is rotated.
     */
    private final ConcurrentMap<String, Set<String>> senderKeyDistributedDevices;

    public MessageSenderService(WhatsAppClient whatsapp) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.store = whatsapp.store();
        this.encoder = new MessageEncoder(store);
        this.senderKeyDistributedDevices = new ConcurrentHashMap<>();
    }

    /**
     * Sends a message to the specified recipient.
     * Handles both individual and group chats.
     *
     * @param info       the message info containing recipient and message content
     * @param attributes additional attributes for the message node
     */
    public void sendMessage(MessageInfo info, Map<String, ?> attributes) {
        Objects.requireNonNull(info, "info cannot be null");
        Objects.requireNonNull(info.parentJid(), "message recipient cannot be null");
        Objects.requireNonNull(info.message(), "message content cannot be null");

        var recipientJid = info.parentJid();
        if (isGroupJid(recipientJid)) {
            sendGroupMessage(info, attributes);
        } else if (isIndividualJid(recipientJid)) {
            sendIndividualMessage(info, attributes);
        } else if (isBroadcastJid(recipientJid)) {
            sendBroadcastMessage(info, attributes);
        } else {
            throw new IllegalArgumentException("Unknown recipient type: " + recipientJid);
        }
    }

    /**
     * Checks if a JID represents a group chat.
     *
     * @param jid the JID to check
     * @return true if the JID is a group JID
     */
    public static boolean isGroupJid(Jid jid) {
        return jid != null && jid.server().type() == JidServer.Type.GROUP_OR_COMMUNITY;
    }

    /**
     * Checks if a JID represents an individual (1:1) chat.
     *
     * @param jid the JID to check
     * @return true if the JID is an individual user JID
     */
    public static boolean isIndividualJid(Jid jid) {
        if (jid == null) {
            return false;
        } else {
            var type = jid.server().type();
            return type == JidServer.Type.USER
                   || type == JidServer.Type.LEGACY_USER
                   || type == JidServer.Type.LID;
        }
    }

    /**
     * Checks if a JID represents a broadcast list.
     *
     * @param jid the JID to check
     * @return true if the JID is a broadcast JID
     */
    public static boolean isBroadcastJid(Jid jid) {
        return jid != null && jid.server().type() == JidServer.Type.BROADCAST;
    }

    /**
     * Sends a message to an individual chat (1:1).
     */
    private void sendIndividualMessage(MessageInfo info, Map<String, ?> attributes) {
        var recipientJid = info.parentJid().toUserJid();
        var senderJid = store.jid().orElseThrow(() -> new IllegalStateException("No local JID available"));

        // Get all devices for recipient and sender
        var jidsToQuery = List.of(recipientJid, senderJid.toUserJid());

        // Query devices from server (this also ensures sessions exist)
        var devices = whatsapp.querySessions(jidsToQuery);

        // Separate own devices from recipient devices
        var ownDevices = devices.stream()
                .filter(d -> d.user().equals(senderJid.user()))
                .filter(d -> d.device() != senderJid.device()) // Exclude current device
                .toList();
        var recipientDevices = devices.stream()
                .filter(d -> d.user().equals(recipientJid.user()))
                .toList();

        // Combine all devices
        var allDevices = Stream.concat(ownDevices.stream(), recipientDevices.stream())
                .toList();

        // Calculate phash for all devices
        var phash = DevicePhashEncoder.calculatePhash(allDevices);

        // Send to all devices and handle response
        var response = sendToDevices(info, attributes, recipientJid, allDevices, ownDevices, phash, false);

        // Handle phash mismatch if needed
        handleIndividualPhashMismatch(response, info, attributes, allDevices, Clock.nowSeconds());
    }

    /**
     * Sends encrypted message to a list of devices for individual chat.
     *
     * @param info         the message info
     * @param attributes   the additional attributes
     * @param recipientJid the recipient JID
     * @param devices      all devices to send to
     * @param ownDevices   subset of devices that are our own
     * @param phash        calculated participant hash (may be null for resends)
     * @param isResend     whether this is a resend operation
     * @return the server response node
     */
    private Node sendToDevices(MessageInfo info, Map<String, ?> attributes, Jid recipientJid, Collection<? extends Jid> devices,
                               Collection<? extends Jid> ownDevices, String phash, boolean isResend) {
        // Build encrypted message nodes for each device
        var participantNodes = new ArrayList<Node>();
        var hasPreKeyMessage = false;

        for (var device : devices) {
            var messageToEncrypt = info.message();

            // For own devices, wrap in DeviceSentMessage
            if (ownDevices.contains(device)) {
                messageToEncrypt = MessageContainer.of(createDeviceSentMessage(recipientJid, info.message()));
            }

            // Encrypt the message
            var result = encoder.encode(device, messageToEncrypt);
            hasPreKeyMessage |= result.isPreKeyMessage();

            // Build the participant node
            var encNode = buildEncNode(result);
            var toNode = new NodeBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .content(encNode)
                    .build();
            participantNodes.add(toNode);
        }

        if (participantNodes.isEmpty()) {
            throw new IllegalStateException("Encryption failed for all devices");
        }

        // Build the message stanza
        var messageId = info.id();
        var participantsNode = new NodeBuilder()
                .description("participants")
                .content(participantNodes)
                .build();

        var messageBuilder = new NodeBuilder()
                .description("message")
                .attribute("id", messageId)
                .attribute("to", recipientJid)
                .attribute("type", getMessageType(info.message()));

        attributes.forEach((key, value) -> {
            if(value == null) {
                messageBuilder.attribute(key, "");
            } else {
                messageBuilder.attribute(key, value.toString());
            }
        });

        // Add phash for non-resend messages
        if (phash != null && !isResend) {
            messageBuilder.attribute("phash", phash);
        }

        // Mark as resend if applicable (server won't do device fanout)
        if (isResend) {
            messageBuilder.attribute("device_fanout", "false");
        }

        // Add participants
        messageBuilder.content(participantsNode);

        // Add device identity if any pre-key messages
        if (hasPreKeyMessage) {
            var deviceIdentityNode = buildDeviceIdentityNode();
            if (deviceIdentityNode != null) {
                messageBuilder.content(deviceIdentityNode);
            }
        }

        // Send the message
        return whatsapp.sendNode(messageBuilder);
    }

    /**
     * Sends a message to a group chat using sender key encryption.
     */
    private void sendGroupMessage(MessageInfo info, Map<String, ?> attributes) {
        var groupJid = info.parentJid();
        var senderDevice = store.jid().orElseThrow(() -> new IllegalStateException("No local JID available"));

        // Get group participants
        var participants = store.findGroupOrCommunityMetadata(groupJid)
                .map(meta -> meta.participants().stream()
                        .map(ChatParticipant::jid)
                        .toList())
                .orElseThrow(() -> new IllegalStateException("Group metadata not found for: " + groupJid));

        // Query devices for all participants
        var devices = whatsapp.querySessions(participants);

        // Separate devices that need sender key distribution from those that already have it
        var devicesNeedingKey = devices.stream()
                .filter(d -> !hasSenderKeyForDevice(groupJid, d))
                .toList();
        var devicesWithKey = devices.stream()
                .filter(d -> hasSenderKeyForDevice(groupJid, d))
                .toList();

        // Calculate phash (all devices + sender)
        var phash = DevicePhashEncoder.calculateGroupPhash(groupJid, senderDevice, devices);

        // Encrypt the main message with sender key
        var groupEncResult = encoder.encodeForGroup(groupJid, senderDevice, info.message());

        // Build participant nodes for devices needing sender key distribution
        var participantNodes = new ArrayList<Node>();
        var hasPreKeyMessage = false;
        var distributedDevices = new ArrayList<Jid>();

        for (var device : devicesNeedingKey) {
            // Wrap sender key distribution in Signal session encryption
            var skdmResult = encoder.wrapSenderKeyDistribution(device.toSignalAddress(), groupJid, senderDevice);
            hasPreKeyMessage |= skdmResult.isPreKeyMessage();

            var encNode = buildEncNode(skdmResult);
            var toNode = new NodeBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .content(encNode)
                    .build();
            participantNodes.add(toNode);
            distributedDevices.add(device);
        }

        // Build the message stanza
        var messageId = info.id();
        var messageBuilder = new NodeBuilder()
                .description("message")
                .attribute("id", messageId)
                .attribute("to", groupJid)
                .attribute("type", getMessageType(info.message()))
                .attribute("phash", phash);

        attributes.forEach((key, value) -> {
            if(value == null) {
                messageBuilder.attribute(key, "");
            } else {
                messageBuilder.attribute(key, value.toString());
            }
        });

        // Add participants node if there are devices needing sender key
        if (!participantNodes.isEmpty()) {
            var participantsNode = new NodeBuilder()
                    .description("participants")
                    .content(participantNodes)
                    .build();
            messageBuilder.content(participantsNode);
        }

        // Add the sender key encrypted message
        var skmsgNode = new NodeBuilder()
                .description("enc")
                .attribute("v", ENC_VERSION)
                .attribute("type", SKMSG)
                .content(groupEncResult.ciphertext())
                .build();
        messageBuilder.content(skmsgNode);

        // Add device identity if any pre-key messages
        if (hasPreKeyMessage) {
            var deviceIdentityNode = buildDeviceIdentityNode();
            if (deviceIdentityNode != null) {
                messageBuilder.content(deviceIdentityNode);
            }
        }

        // Send the message
        var response = whatsapp.sendNode(messageBuilder);

        // Handle phash mismatch and 421 errors
        var success = handleGroupMessageResponse(response, info, attributes, phash, devices, Clock.nowSeconds());

        // If successful, mark sender key as distributed to these devices
        if (success) {
            markSenderKeyDistributed(groupJid, distributedDevices);
        }
    }

    /**
     * Sends a message to a broadcast list.
     * Broadcast messages are sent as individual encrypted messages to each recipient (no sender key).
     */
    private void sendBroadcastMessage(MessageInfo info, Map<String, ?> attributes) {
        var broadcastJid = info.parentJid();
        var senderJid = store.jid().orElseThrow(() -> new IllegalStateException("No local JID available"));

        var metadata = store.findGroupOrCommunityMetadata(broadcastJid)
                .orElseThrow(() -> new IllegalStateException("Broadcast list metadata not found for: " + broadcastJid));

        var jidsToQuery = metadata.participants()
                .stream()
                .map(ChatParticipant::jid)
                .collect(Collectors.toSet());
        jidsToQuery.add(senderJid.toUserJid());
        var allDevices = whatsapp.querySessions(jidsToQuery);

        // Separate own devices (excluding current device)
        var ownDevices = allDevices.stream()
                .filter(d -> d.user().equals(senderJid.user()))
                .filter(d -> d.device() != senderJid.device())
                .toList();

        // Group recipient devices by user
        var recipientDevices = allDevices.stream()
                .filter(d -> !d.user().equals(senderJid.user()))
                .toList();

        // Build encrypted message nodes for each device
        var participantNodes = new ArrayList<Node>();
        var hasPreKeyMessage = false;

        // First, encrypt for own devices (wrapped in DeviceSentMessage with broadcast destination)
        for (var device : ownDevices) {
            var deviceSentMessage = new DeviceSentMessageBuilder()
                    .destinationJid(broadcastJid)
                    .message(info.message())
                    .build();
            var messageToEncrypt = MessageContainer.of(deviceSentMessage);

            var result = encoder.encode(device, messageToEncrypt);
            hasPreKeyMessage |= result.isPreKeyMessage();

            var encNode = buildEncNode(result);
            var toNode = new NodeBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .content(encNode)
                    .build();
            participantNodes.add(toNode);
        }

        // Then, encrypt for recipient devices (normal message)
        boolean hasPreKeyMessage1 = hasPreKeyMessage;
        for (var device : recipientDevices) {
            var result = encoder.encode(device, info.message());
            hasPreKeyMessage1 |= result.isPreKeyMessage();

            var encNode = buildEncNode(result);
            var toNode = new NodeBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .content(encNode)
                    .build();
            participantNodes.add(toNode);
        }
        hasPreKeyMessage = hasPreKeyMessage1;

        if (participantNodes.isEmpty()) {
            throw new IllegalStateException("Encryption failed for all broadcast devices");
        }

        // Build the message stanza
        var messageId = info.id();
        var participantsNode = new NodeBuilder()
                .description("participants")
                .content(participantNodes)
                .build();

        var messageBuilder = new NodeBuilder()
                .description("message")
                .attribute("id", messageId)
                .attribute("to", broadcastJid)
                .attribute("type", getMessageType(info.message()))
                .content(participantsNode);

        attributes.forEach((key, value) -> {
            if(value == null) {
                messageBuilder.attribute(key, "");
            } else {
                messageBuilder.attribute(key, value.toString());
            }
        });


        // Add device identity if any pre-key messages
        if (hasPreKeyMessage) {
            var deviceIdentityNode = buildDeviceIdentityNode();
            if (deviceIdentityNode != null) {
                messageBuilder.content(deviceIdentityNode);
            }
        }

        // Send the message
        var response = whatsapp.sendNode(messageBuilder);

        // Handle phash mismatch (broadcast uses direct fanout, similar to individual)
        handleIndividualPhashMismatch(response, info, attributes, allDevices, Clock.nowSeconds());
    }

    /**
     * Creates a DeviceSentMessage wrapper for syncing to own devices.
     */
    private DeviceSentMessage createDeviceSentMessage(Jid destinationJid, MessageContainer message) {
        return new DeviceSentMessageBuilder()
                .destinationJid(destinationJid)
                .message(message)
                .build();
    }

    /**
     * Builds an encryption node for the message.
     */
    private Node buildEncNode(MessageEncoder.Result result) {
        return new NodeBuilder()
                .description("enc")
                .attribute("v", ENC_VERSION)
                .attribute("type", result.type())
                .content(result.ciphertext())
                .build();
    }

    /**
     * Builds the device identity node for pre-key messages.
     */
    private Node buildDeviceIdentityNode() {
        return store.companionIdentity()
                .map(identity -> new NodeBuilder()
                        .description("device-identity")
                        .content(SignedDeviceIdentitySpec.encode(identity))
                        .build())
                .orElse(null);
    }

    /**
     * Gets the message type string for the node.
     */
    private String getMessageType(MessageContainer message) {
        // Determine message type based on content
        var content = message.content();
        return switch (content) {
            case com.github.auties00.cobalt.model.message.standard.TextMessage ignored -> "text";
            case com.github.auties00.cobalt.model.message.standard.ImageMessage ignored -> "media";
            case com.github.auties00.cobalt.model.message.standard.VideoOrGifMessage ignored -> "media";
            case com.github.auties00.cobalt.model.message.standard.AudioMessage ignored -> "media";
            case com.github.auties00.cobalt.model.message.standard.DocumentMessage ignored -> "media";
            case com.github.auties00.cobalt.model.message.standard.StickerMessage ignored -> "media";
            default -> "text";
        };
    }

    /**
     * Handles phash mismatch in the server response for individual/broadcast messages.
     * When a phash mismatch occurs, we need to:
     * 1. Query the updated device list from the server
     * 2. Calculate the difference (new devices)
     * 3. Resend the message to the new devices only
     *
     * @param response   the server response
     * @param info       the message info
     * @param attributes the additional attributes
     * @param oldDevices the device list used in the original send
     * @param sendTime   the time when the original send was initiated
     */
    private void handleIndividualPhashMismatch(Node response, MessageInfo info, Map<String, ?> attributes,
                                               Collection<? extends Jid> oldDevices, long sendTime) {
        var senderJid = store.jid()
                .orElse(null);
        if(senderJid == null) {
            return;
        }

        var serverPhash = response.getAttributeAsString("phash")
                .orElse(null);
        if (serverPhash == null) {
            return;
        }

        if (Clock.nowSeconds() - sendTime > RESEND_TIMEOUT_SECONDS) {
            return;
        }

        var recipientJid = info.parentJid()
                .toUserJid();

        var jidsToQuery = List.of(recipientJid, senderJid.toUserJid());
        var newDevices = whatsapp.querySessions(jidsToQuery);

        var oldDeviceStrings = oldDevices.stream()
                .map(Jid::toString)
                .collect(Collectors.toUnmodifiableSet());
        var missingDevices = newDevices.stream()
                .filter(d -> !oldDeviceStrings.contains(d.toString()))
                .toList();
        if (missingDevices.isEmpty()) {
            return;
        }

        // Separate own devices
        var ownDevices = missingDevices.stream()
                .filter(device -> device.user().equals(senderJid.user()) && device.device() != senderJid.device())
                .toList();

        // Resend to missing devices only (with device_fanout=false to indicate resend)
        sendToDevices(info, attributes, recipientJid, missingDevices, ownDevices, null, true);
    }

    /**
     * Handles group message response including 421 errors and phash mismatch.
     *
     * @param response   the server response
     * @param info       the message info
     * @param attributes the additional attributes
     * @param localPhash the phash we calculated locally
     * @param oldDevices the device list used in the original send
     * @param sendTime   the time when the original send was initiated
     * @return true if the send was successful (no errors), false if there was an error
     */
    private boolean handleGroupMessageResponse(Node response, MessageInfo info, Map<String, ?> attributes, String localPhash, Collection<? extends Jid> oldDevices, long sendTime) {
        if (response.hasAttribute("code", ERROR_STALE_ADDRESSING_MODE)) {
            try {
                whatsapp.queryGroupOrCommunityMetadata(info.parentJid());
            } catch (Exception e) {
                throw new IllegalStateException("Group addressing mode is stale (error 421). " +
                                                "Group metadata has been refreshed. Please retry sending the message.");
            }
        }

        var serverPhash = response.getAttributeAsString("phash").orElse(null);
        if (serverPhash != null && !serverPhash.equals(localPhash)) {
            handleGroupPhashMismatch(info, attributes, oldDevices, sendTime);
        }

        return true;
    }

    /**
     * Handles phash mismatch for group messages.
     * For groups, we resend using direct fanout to the new devices only.
     */
    private void handleGroupPhashMismatch(MessageInfo info, Map<String, ?> attributes, Collection<? extends Jid> oldDevices, long sendTime) {
        if (Clock.nowSeconds() - sendTime > RESEND_TIMEOUT_SECONDS) {
            return;
        }

        var groupJid = info.parentJid();

        var metadata = store.findGroupOrCommunityMetadata(groupJid)
                .orElseGet(() -> whatsapp.queryGroupOrCommunityMetadata(groupJid));

        var participants = metadata.participants()
                .stream()
                .map(ChatParticipant::jid)
                .toList();

        var newDevices = whatsapp.querySessions(participants);

        var oldDevicesSet = new HashSet<>(oldDevices);
        var missingDevices = newDevices.stream()
                .filter(newDevice -> !oldDevicesSet.contains(newDevice))
                .toList();
        if (missingDevices.isEmpty()) {
            return;
        }

        resendGroupMessageDirect(info, attributes, groupJid, missingDevices);
    }

    /**
     * Resends a group message using direct fanout (individual encryption).
     * This is used for phash mismatch recovery.
     */
    private void resendGroupMessageDirect(MessageInfo info, Map<String, ?> attributes, Jid groupJid, Collection<? extends Jid> devices) {
        var participantNodes = new ArrayList<Node>();

        var hasPreKeyMessage = false;
        for (var device : devices) {
            var result = encoder.encode(device, info.message());
            hasPreKeyMessage |= result.isPreKeyMessage();
            var encNode = buildEncNode(result);
            var toNode = new NodeBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .content(encNode)
                    .build();
            participantNodes.add(toNode);
        }

        if (participantNodes.isEmpty()) {
            System.err.println("No devices could be encrypted for group resend");
            return;
        }

        // Build the resend stanza
        var messageId = info.id();
        var participantsNode = new NodeBuilder()
                .description("participants")
                .content(participantNodes)
                .build();

        var messageBuilder = new NodeBuilder()
                .description("message")
                .attribute("id", messageId)
                .attribute("to", groupJid)
                .attribute("type", getMessageType(info.message()))
                .attribute("device_fanout", "false") // Indicate this is a resend
                .content(participantsNode);

        addAdditionalAttributes(messageBuilder, attributes);

        if (hasPreKeyMessage) {
            var deviceIdentityNode = buildDeviceIdentityNode();
            if (deviceIdentityNode != null) {
                messageBuilder.content(deviceIdentityNode);
            }
        }

        // Send the resend
        whatsapp.sendNode(messageBuilder);
    }

    /**
     * Checks if we have distributed our sender key to a specific device for a group.
     */
    private boolean hasSenderKeyForDevice(Jid groupJid, Jid deviceJid) {
        var distributedDevices = senderKeyDistributedDevices.get(groupJid.toString());
        if (distributedDevices == null) {
            return false;
        }
        return distributedDevices.contains(deviceJid.toString());
    }

    /**
     * Marks that we have distributed our sender key to the specified devices.
     */
    private void markSenderKeyDistributed(Jid groupJid, List<Jid> devices) {
        var distributedDevices = senderKeyDistributedDevices.computeIfAbsent(
                groupJid.toString(),
                _ -> ConcurrentHashMap.newKeySet()
        );
        for (var device : devices) {
            distributedDevices.add(device.toString());
        }
    }

    private static void addAdditionalAttributes(NodeBuilder nodeBuilder, Map<String, ?> attributes) {
        attributes.forEach((key, value) -> {
            if(value == null) {
                nodeBuilder.attribute(key, "");
            } else {
                nodeBuilder.attribute(key, value.toString());
            }
        });
    }
}
