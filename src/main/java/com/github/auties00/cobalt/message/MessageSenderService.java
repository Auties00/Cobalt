package com.github.auties00.cobalt.message;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.message.signal.SignalMessageEncoder;
import com.github.auties00.cobalt.message.rcat.MessageRcatEncoder;
import com.github.auties00.cobalt.device.hash.DevicePhashEncoder;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.model.auth.SignedDeviceIdentitySpec;
import com.github.auties00.cobalt.model.chat.ChatParticipant;
import com.github.auties00.cobalt.model.chat.ChatRole;
import com.github.auties00.cobalt.model.chat.GroupOrCommunityMetadata;
import com.github.auties00.cobalt.model.info.ChatMessageInfo;
import com.github.auties00.cobalt.model.info.MessageInfo;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.model.MessageContainer;
import com.github.auties00.cobalt.model.message.server.DeviceSentMessage;
import com.github.auties00.cobalt.model.message.server.DeviceSentMessageBuilder;
import com.github.auties00.cobalt.model.message.server.ProtocolMessage;
import com.github.auties00.cobalt.model.message.standard.*;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.store.WhatsAppStore;
import com.github.auties00.cobalt.util.Clock;
import com.github.auties00.libsignal.SignalSessionCipher;
import com.github.auties00.libsignal.groups.SignalGroupCipher;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

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
    private final SignalMessageEncoder signalMessageEncoder;
    private final DeviceService deviceService;

    /**
     * Tracks which devices have received our sender key for each group.
     * Key: group JID string, Value: set of device JIDs that have received the key.
     * This is cleared when the sender key is rotated.
     */
    private final ConcurrentMap<String, Set<String>> senderKeyDistributedDevices;

    public MessageSenderService(WhatsAppClient whatsapp, DeviceService deviceService, SignalSessionCipher sessionCipher, SignalGroupCipher groupCipher) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.store = whatsapp.store();
        this.signalMessageEncoder = new SignalMessageEncoder(sessionCipher, groupCipher);
        this.deviceService = deviceService;
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
        var senderJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("No local JID available"));

        // Get all devices for recipient and sender
        var jidsToQuery = List.of(recipientJid, senderJid.toUserJid());

        // Query devices from server (this also ensures sessions exist)
        var devices = deviceService.queryDevices(jidsToQuery);

        // Send to all devices and handle response
        var ownDevices = devices.stream()
                .filter(device -> device.user().equals(senderJid.user()) && device.device() != senderJid.device())
                .collect(Collectors.toUnmodifiableSet());
        var response = sendToDevices(info, attributes, recipientJid, devices, ownDevices, false);

        // Handle phash mismatch if needed
        handleIndividualPhashMismatch(response, info, attributes, devices, Clock.nowSeconds());
    }

    /**
     * Sends encrypted message to a list of devices for individual chat.
     *
     * @param info         the message info
     * @param attributes   the additional attributes
     * @param recipientJid the recipient JID
     * @param devices      all devices to send to
     * @param ownDevices   subset of devices that are our own
     * @param isResend     whether this is a resend operation
     * @return the server response node
     */
    private Node sendToDevices(MessageInfo info, Map<String, ?> attributes, Jid recipientJid, Set<? extends Jid> devices, Set<? extends Jid> ownDevices, boolean isResend) {
        if(devices.isEmpty()) {
            throw new IllegalArgumentException("Cannot send message to " + recipientJid + ": no devices found");
        }

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
            var result = signalMessageEncoder.encode(device, messageToEncrypt);
            hasPreKeyMessage |= result.isPreKeyMessage();

            // Build the participant node
            var encNode = buildEncNode(result, getMediaType(info.message()));
            var toNode = new NodeBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .content(encNode)
                    .build();
            participantNodes.add(toNode);
        }

        // Build the message stanza
        var messageId = info.id();

        var messageBuilder = new NodeBuilder()
                .description("message")
                .attribute("id", messageId)
                .attribute("to", recipientJid)
                .attribute("type", getMessageType(info.message()));

        // Add additional attributes
        attributes.forEach((key, value) -> {
            if(value == null) {
                messageBuilder.attribute(key, "");
            } else {
                messageBuilder.attribute(key, value.toString());
            }
        });

        // Mark as resend if applicable (server won't do device fanout)
        if (isResend) {
            messageBuilder.attribute("device_fanout", "false");
        }

        // Add participants
        var participantsNode = new NodeBuilder()
                .description("participants")
                .content(participantNodes)
                .build();
        messageBuilder.content(participantsNode);

        // Add device identity if any pre-key messages
        if (hasPreKeyMessage) {
            buildDeviceIdentityNode()
                    .ifPresent(messageBuilder::content);
        }

        // Send the message
        return whatsapp.sendNode(messageBuilder);
    }

    /**
     * Sends a message to a group chat using sender key encryption.
     */
    private void sendGroupMessage(MessageInfo info, Map<String, ?> attributes) {
        var groupJid = info.parentJid();
        var senderDevice = store.jid()
                .orElseThrow(() -> new IllegalStateException("No local JID available"));

        // Get group participants
        var metadata = store.findGroupOrCommunityMetadata(groupJid)
                .orElseGet(() -> whatsapp.queryGroupOrCommunityMetadata(groupJid));
        var participants = metadata.participants()
                .stream()
                .map(ChatParticipant::jid)
                .toList();

        // Query devices for all participants
        var devices = deviceService.queryDevices(participants)
                .stream()
                .filter(entry -> !whatsapp.store().hasJid(entry))
                .collect(Collectors.toUnmodifiableSet());

        // Check for CAG logic
        if (metadata.parentCommunityJid().isPresent() && metadata.isIncognito()) {
            var amIAdmin = metadata.participants().stream()
                    .filter(p -> p.jid().toUserJid().equals(senderDevice.toUserJid()))
                    .findFirst()
                    .map(p -> p.role() == ChatRole.ADMIN || p.role() == ChatRole.FOUNDER)
                    .orElse(false);

            if (!amIAdmin) {
                sendGroupMessageDirect(info, attributes, metadata, devices, false);
                return;
            }
        }

        // Separate devices that need sender key distribution from those that already have it
        var devicesNeedingKey = devices.stream()
                .filter(device -> !hasSenderKeyForDevice(groupJid, device))
                .toList();

        // Calculate phash (all devices + sender)
        var phash = DevicePhashEncoder.calculateGroupPhash(groupJid, senderDevice, devices);

        // Encrypt the main message with sender key
        var groupEncResult = signalMessageEncoder.encodeForGroup(groupJid, senderDevice, info.message());

        // Build participant nodes for devices needing sender key distribution
        var participantNodes = new ArrayList<Node>();
        var hasPreKeyMessage = false;
        var distributedDevices = new ArrayList<Jid>();

        for (var device : devicesNeedingKey) {
            // Wrap sender key distribution in Signal session encryption
            var skdmResult = signalMessageEncoder.wrapSenderKeyDistribution(device.toSignalAddress(), groupJid, senderDevice);
            hasPreKeyMessage |= skdmResult.isPreKeyMessage();

            var encNode = buildEncNode(skdmResult, null);
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
                .attribute("addressing_mode", metadata.isLidAddressingMode() ? "lid" : "pn")
                .attribute("phash", phash);

        // Add additional attributes
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
        var skmsgNode = buildEncNode(groupEncResult, getMediaType(info.message()));
        messageBuilder.content(skmsgNode);

        // Add device identity if any pre-key messages
        if (hasPreKeyMessage) {
            buildDeviceIdentityNode()
                    .ifPresent(messageBuilder::content);
        }

        // Send the message
        var response = whatsapp.sendNode(messageBuilder);

        // Handle phash mismatch and 421 errors
        handleGroupMessageResponse(response, info, attributes, phash, devices, Clock.nowSeconds());

        // Mark sender key as distributed to these devices
        markSenderKeyDistributed(groupJid, distributedDevices);
    }

    /**
     * Sends a message to a broadcast list.
     * Broadcast messages are sent as individual encrypted messages to each recipient (no sender key).
     */
    private void sendBroadcastMessage(MessageInfo info, Map<String, ?> attributes) {
        var senderJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("No local JID available"));

        var broadcastJid = info.parentJid();
        var metadata = store.findGroupOrCommunityMetadata(broadcastJid)
                .orElseGet(() -> whatsapp.queryGroupOrCommunityMetadata(broadcastJid));

        var jidsToQuery = metadata.participants()
                .stream()
                .map(ChatParticipant::jid)
                .collect(Collectors.toSet());
        jidsToQuery.add(senderJid.toUserJid());
        var allDevices = deviceService.queryDevices(jidsToQuery);

        // Generate message secret for content binding (RCAT)
        var messageSecret = MessageRcatEncoder.generateMessageSecret();

        // Generate content bindings for all recipients
        var contentBindings = MessageRcatEncoder.generateContentBindings(
                info.id(), messageSecret, senderJid, allDevices
        );

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

            var result = signalMessageEncoder.encode(device, messageToEncrypt);
            hasPreKeyMessage |= result.isPreKeyMessage();

            var encNode = buildEncNode(result, getMediaType(info.message()));
            var toNode = new NodeBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .content(encNode)
                    .build();
            participantNodes.add(toNode);
        }

        // Then, encrypt for recipient devices (normal message)
        for (var device : recipientDevices) {
            var result = signalMessageEncoder.encode(device, info.message());
            hasPreKeyMessage |= result.isPreKeyMessage();

            var encNode = buildEncNode(result, getMediaType(info.message()));
            var toNode = new NodeBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .content(encNode)
                    .build();
            participantNodes.add(toNode);
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

        // Add additional attributes
        attributes.forEach((key, value) -> {
            if(value == null) {
                messageBuilder.attribute(key, "");
            } else {
                messageBuilder.attribute(key, value.toString());
            }
        });


        // Add device identity if any pre-key messages
        if (hasPreKeyMessage) {
            buildDeviceIdentityNode()
                    .ifPresent(messageBuilder::content);
        }

        // Add sender content binding node
        var senderContentBinding = contentBindings.get(senderJid.toUserJid().toString());
        if (senderContentBinding != null) {
            var contentBindingNode = new NodeBuilder()
                    .description("sender_content_binding")
                    .content(senderContentBinding.getBytes())
                    .build();
            messageBuilder.content(contentBindingNode);
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
    private Node buildEncNode(SignalMessageEncoder.Result result, String mediaType) {
        var builder = new NodeBuilder()
                .description("enc")
                .attribute("v", ENC_VERSION)
                .attribute("type", result.type());

        if (mediaType != null) {
            builder.attribute("mediatype", mediaType);
        }

        return builder.content(result.ciphertext()).build();
    }

    /**
     * Builds the device identity node for pre-key messages.
     */
    private Optional<Node> buildDeviceIdentityNode() {
        var companionIdentity = store.companionIdentity();
        if(companionIdentity.isEmpty()) {
            return Optional.empty();
        } else {
            var result = new NodeBuilder()
                    .description("device-identity")
                    .content(SignedDeviceIdentitySpec.encode(companionIdentity.get()))
                    .build();
            return Optional.of(result);
        }
    }

    /**
     * Gets the message type string for the node.
     */
    private String getMessageType(MessageContainer message) {
        return switch (message.content()) {
            case ImageMessage _, VideoOrGifMessage _, AudioMessage _, DocumentMessage _, StickerMessage _, ContactMessage _, ContactsMessage _, LocationMessage _, LiveLocationMessage _ -> "media";
            case ReactionMessage _ -> "reaction";
            case PollCreationMessage _, PollUpdateMessage _ ->  "poll";
            case ProtocolMessage _ -> "protocol";
            case GroupInviteMessage _ ->  "invite";
            case CallMessage _ -> "call_log";
            default -> "text";
        };
    }

    private String getMediaType(MessageContainer message) {
        return switch (message.content()) {
            case ImageMessage _ -> "image";
            case VideoOrGifMessage _ -> "video";
            case AudioMessage audioMessage -> audioMessage.voiceMessage() ? "ptt" : "audio";
            case  DocumentMessage _ -> "document";
            case StickerMessage _ -> "sticker";
            case ContactMessage _ -> "contact";
            case ContactsMessage _ -> "contacts";
            case LocationMessage _ -> "location";
            case LiveLocationMessage _ -> "livelocation";
            case GroupInviteMessage _ -> "groups_v4_invite";
            default -> null;
        };
    }

    private String getAddressingMode(Jid jid) {
        return jid.server().type() == JidServer.Type.LID ? "lid" : "pn";
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
                                               Set<? extends Jid> oldDevices, long sendTime) {
        var senderJid = store.jid()
                .orElseThrow(() -> new IllegalStateException("No local JID available"));

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
        var newDevices = deviceService.queryDevices(jidsToQuery);

        var missingDevices = newDevices.stream()
                .filter(user -> !oldDevices.contains(user))
                .collect(Collectors.toUnmodifiableSet());
        if (missingDevices.isEmpty()) {
            return;
        }

        // Separate own devices
        var ownDevices = missingDevices.stream()
                .filter(device -> device.user().equals(senderJid.user()) && device.device() != senderJid.device())
                .collect(Collectors.toUnmodifiableSet());

        // Resend to missing devices only (with device_fanout=false to indicate resend)
        sendToDevices(info, attributes, recipientJid, missingDevices, ownDevices, true);
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
     */
    private void handleGroupMessageResponse(Node response, MessageInfo info, Map<String, ?> attributes, String localPhash, Collection<? extends Jid> oldDevices, long sendTime) {
        if (response.hasAttribute("code", ERROR_STALE_ADDRESSING_MODE)) {
            try {
                whatsapp.queryGroupOrCommunityMetadata(info.parentJid());
            } catch (Exception e) {
                throw new IllegalStateException("Group addressing mode is stale (error 421). Group metadata has been refreshed. Please retry sending the message.");
            }
        } else {
            response.getAttributeAsString("phash").ifPresent(serverPhash -> {
                if (!serverPhash.equals(localPhash)) {
                    handleGroupPhashMismatch(info, attributes, oldDevices, sendTime);
                }
            });
        }
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

        var newDevices = deviceService.queryDevices(participants);

        var oldDevicesSet = new HashSet<>(oldDevices);
        var missingDevices = newDevices.stream()
                .filter(newDevice -> !oldDevicesSet.contains(newDevice))
                .toList();
        if (missingDevices.isEmpty()) {
            return;
        }

        sendGroupMessageDirect(info, attributes, metadata, missingDevices, true);
    }

    /**
     * Resends a group message using direct fanout (individual encryption).
     * This is used for phash mismatch recovery.
     */
    private void sendGroupMessageDirect(MessageInfo info, Map<String, ?> attributes, GroupOrCommunityMetadata metadata, Collection<? extends Jid> devices, boolean isResend) {
        var participantNodes = new ArrayList<Node>();

        var hasPreKeyMessage = false;
        for (var device : devices) {
            var result = signalMessageEncoder.encode(device, info.message());
            hasPreKeyMessage |= result.isPreKeyMessage();
            var encNode = buildEncNode(result, getMediaType(info.message()));
            var toNode = new NodeBuilder()
                    .description("to")
                    .attribute("jid", device)
                    .content(encNode)
                    .build();
            participantNodes.add(toNode);
        }

        // Build the stanza
        var messageId = info.id();
        var participantsNode = new NodeBuilder()
                .description("participants")
                .content(participantNodes)
                .build();

        var messageBuilder = new NodeBuilder()
                .description("message")
                .attribute("id", messageId)
                .attribute("to", metadata.jid())
                .attribute("type", getMessageType(info.message()))
                .attribute("addressing_mode", metadata.isLidAddressingMode() ? "lid" : "pn")
                .content(participantsNode);

        if (isResend) {
            messageBuilder.attribute("device_fanout", "false");
        }

        // Add additional attributes
        attributes.forEach((key, value) -> {
            if(value == null) {
                messageBuilder.attribute(key, "");
            } else {
                messageBuilder.attribute(key, value.toString());
            }
        });

        if (hasPreKeyMessage) {
            buildDeviceIdentityNode()
                    .ifPresent(messageBuilder::content);
        }

        // Send the message
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

    /**
     * Resends a message to a specific device that failed to decrypt it.
     * This is called when we receive a "retry" receipt indicating a device
     * couldn't decrypt a message we sent.
     *
     * @param message    the original message that needs to be resent
     * @param deviceJid  the specific device JID that needs the retry
     */
    public void resendToDevice(ChatMessageInfo message, Jid deviceJid) {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");

        // Encrypt the message for the specific device
        var encResult = signalMessageEncoder.encode(deviceJid, message.message());
        var encNode = buildEncNode(encResult, getMediaType(message.message()));

        var participantNode = new NodeBuilder()
                .description("to")
                .attribute("jid", deviceJid)
                .content(encNode)
                .build();

        var participantsNode = new NodeBuilder()
                .description("participants")
                .content(participantNode)
                .build();

        // Build the retry message stanza
        var messageBuilder = new NodeBuilder()
                .description("message")
                .attribute("id", message.id())
                .attribute("to", message.chatJid())
                .attribute("type", getMessageType(message.message()))
                .attribute("addressing_mode", getAddressingMode(message.chatJid()))
                .attribute("device_fanout", "false") // Indicate this is a resend
                .content(participantsNode);

        // Add device identity if this is a pre-key message
        if (encResult.isPreKeyMessage()) {
            buildDeviceIdentityNode()
                    .ifPresent(messageBuilder::content);
        }

        // Send the retry
        whatsapp.sendNode(messageBuilder);
    }
}
