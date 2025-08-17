package it.auties.whatsapp.socket.stream;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.info.ChatMessageInfoBuilder;
import it.auties.whatsapp.model.info.ChatMessageStubType;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.ChatMessageKeyBuilder;
import it.auties.whatsapp.model.message.model.MessageStatus;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.newsletter.NewsletterMetadataBuilder;
import it.auties.whatsapp.model.newsletter.NewsletterReaction;
import it.auties.whatsapp.model.newsletter.NewsletterVerification;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.response.NewsletterLeaveResponse;
import it.auties.whatsapp.model.response.NewsletterMuteResponse;
import it.auties.whatsapp.model.response.NewsletterResponse;
import it.auties.whatsapp.model.response.NewsletterStateResponse;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.sync.PatchType;
import it.auties.whatsapp.socket.SocketConnection;
import it.auties.whatsapp.util.Bytes;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.chrono.ChronoZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

final class NotificationHandler extends NodeHandler.Dispatcher {
    private static final byte[][] CALL_RELAY = new byte[][]{
            new byte[]{-105, 99, -47, -29, 13, -106},
            new byte[]{-99, -16, -53, 62, 13, -106},
            new byte[]{-99, -16, -25, 62, 13, -106},
            new byte[]{-99, -16, -5, 62, 13, -106},
            new byte[]{-71, 60, -37, 62, 13, -106}
    };
    private static final int DEFAULT_NEWSLETTER_MESSAGES = 100;

    NotificationHandler(SocketConnection socketConnection) {
        super(socketConnection, "notification");
    }

    @Override
    void execute(Node node) {
        var from = node.attributes()
                .getRequiredJid("from");
        try {
            var type = node.attributes().getString("type", null);
            switch (type) {
                case "w:gp2" -> handleGroupNotification(node);
                case "server_sync" -> handleServerSyncNotification(node);
                case "account_sync" -> handleAccountSyncNotification(node);
                case "encrypt" -> handleEncryptNotification(node);
                case "picture" -> handlePictureNotification(node);
                case "registration" -> handleRegistrationNotification(node);
                case "link_code_companion_reg" -> handleCompanionRegistration(node);
                case "newsletter" -> handleNewsletter(from, node);
                case "mex" -> handleMexNamespace(node);
            }
        } finally {
            socketConnection.sendMessageAck(from, node);
        }
    }

    private void handleNewsletter(Jid newsletterJid, Node node) {
        var newsletter = socketConnection.store()
                .findNewsletterByJid(newsletterJid);
        if (newsletter.isEmpty()) {
            return;
        }

        var liveUpdates = node.findChild("live_updates");
        if (liveUpdates.isEmpty()) {
            return;
        }


        var messages = liveUpdates.get().findChild("messages");
        if (messages.isEmpty()) {
            return;
        }

        for (var messageNode : messages.get().listChildren("message")) {
            var messageId = messageNode.attributes()
                    .getRequiredString("server_id");
            var newsletterMessage = socketConnection.store().findMessageById(newsletter.get(), messageId);
            if (newsletterMessage.isEmpty()) {
                continue;
            }

            messageNode.findChild("reactions")
                    .map(reactions -> reactions.listChildren("reaction"))
                    .stream()
                    .flatMap(Collection::stream)
                    .forEach(reaction -> onNewsletterReaction(reaction, newsletterMessage.get()));
        }
    }

    private void onNewsletterReaction(Node reaction, NewsletterMessageInfo newsletterMessage) {
        var reactionCode = reaction.attributes()
                .getRequiredString("code");
        var reactionCount = reaction.attributes()
                .getRequiredInt("count");
        var newReaction = new NewsletterReaction(reactionCode, reactionCount, false);
        newsletterMessage.addReaction(newReaction)
                .ifPresent(oldReaction -> newReaction.setFromMe(oldReaction.fromMe()));
    }

    private void handleMexNamespace(Node node) {
        var update = node.findChild("update")
                .orElse(null);
        if (update == null) {
            return;
        }

        switch (update.attributes().getString("op_name")) {
            case "MexNotificationEvent" -> {
               // TODO MexNotificationEvent
            }
            case "NotificationNewsletterJoin" -> handleNewsletterJoin(update);
            case "NotificationNewsletterMuteChange" -> handleNewsletterMute(update);
            case "NotificationNewsletterLeave" -> handleNewsletterLeave(update);
            case "NotificationNewsletterUpdate" -> handleNewsletterMetadataUpdate(update);
            case "NotificationNewsletterStateChange" -> handleNewsletterStateUpdate(update);
            case "NotificationNewsletterAdminMetadataUpdate" -> {
                // TODO MexNotificationEvent
            }
            case "NotificationNewsletterOwnerUpdate" -> {
                // TODO NotificationNewsletterOwnerUpdate
            }
            case "NotificationNewsletterAdminPromote" -> {
                // TODO NotificationNewsletterAdminPromote
            }
            case "NotificationNewsletterAdminDemote" -> {
                // TODO NotificationNewsletterAdminDemote
            }
            case "NotificationNewsletterAdminInviteRevoke" -> {
                // TODO NotificationNewsletterAdminInviteRevoke
            }
            case "NotificationNewsletterWamoSubStatusChange" -> {
                // TODO NotificationNewsletterWamoSubStatusChange
            }
            case "TextStatusUpdateNotification" -> {
                // TODO TextStatusUpdateNotification
            }
            case "TextStatusUpdateNotificationSideSub" -> {
                // TODO TextStatusUpdateNotificationSideSub
            }
            case "NotificationGroupPropertyUpdate" -> {
                // TODO NotificationGroupPropertyUpdate
            }
            case "NotificationGroupHiddenPropertyUpdate" -> {
                // TODO NotificationGroupHiddenPropertyUpdate
            }
            case "NotificationGroupSafetyCheckPropertyUpdate" -> {
                // TODO NotificationGroupSafetyCheckPropertyUpdate
            }
            case "NotificationCommunityOwnerUpdate" -> {
                // TODO NotificationCommunityOwnerUpdate
            }
            case "UsernameSetNotification" -> {
                // TODO UsernameSetNotification
            }
            case "UsernameDeleteNotification" -> {
                // TODO UsernameDeleteNotification
            }
            case "UsernameUpdateNotification" -> {
                // TODO UsernameUpdateNotification
            }
            case "AccountSyncUsernameNotification" -> {
                // TODO AccountSyncUsernameNotification
            }
            case "LidChangeNotification" -> {
                // TODO LidChangeNotification
            }
            case "NotificationUserBrigadingUpdate" -> {
                // TODO NotificationUserBrigadingUpdate
            }
            case "NotificationGroupLimitSharingPropertyUpdate" -> {
                // TODO NotificationGroupLimitSharingPropertyUpdate
            }
            case "NotificationUserReachoutTimelockUpdate" -> {
                // TODO NotificationUserReachoutTimelockUpdate
            }
        }
    }

    private void handleNewsletterStateUpdate(Node update) {
        var updatePayload = update.contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing state update payload"));
        NewsletterStateResponse.ofJson(updatePayload).ifPresent(response -> {
            var newsletter = socketConnection.store()
                    .findNewsletterByJid(response.jid())
                    .orElseThrow(() -> new NoSuchElementException("Missing newsletter"));
            response.state()
                    .ifPresent(newsletter::setState);
        });
    }

    private void handleNewsletterMetadataUpdate(Node update) {
        var updatePayload = update.contentAsBytes()
                .orElseThrow(() -> new NoSuchElementException("Missing update payload"));
        NewsletterResponse.ofJson(updatePayload).ifPresent(response -> {
            var updatedNewsletter = response.newsletter();
            var newsletter = socketConnection.store()
                    .findNewsletterByJid(updatedNewsletter.jid())
                    .orElseThrow(() -> new NoSuchElementException("Missing newsletter"));
            var updatedMetadata = updatedNewsletter.metadata()
                    .orElse(null);
            var oldMetadata = newsletter.metadata()
                    .orElse(null);
            if (oldMetadata == null) {
                newsletter.setMetadata(updatedMetadata);
            } else if (updatedMetadata != null) {
                var name = updatedMetadata.name()
                        .or(oldMetadata::name)
                        .orElse(null);
                var description = updatedMetadata.description()
                        .or(oldMetadata::description)
                        .orElse(null);
                var picture = updatedMetadata.picture()
                        .or(oldMetadata::picture)
                        .orElse(null);
                var handle = updatedMetadata.handle()
                        .or(oldMetadata::handle)
                        .orElse(null);
                var settings = updatedMetadata.settings()
                        .or(oldMetadata::settings)
                        .orElse(null);
                var invite = updatedMetadata.invite()
                        .or(oldMetadata::invite)
                        .orElse(null);
                var verification = updatedMetadata.verification().filter(NewsletterVerification::verified).isPresent() || oldMetadata.verification().filter(NewsletterVerification::verified).isPresent()
                        ? NewsletterVerification.enabled()
                        : NewsletterVerification.disabled();
                var creationTimestamp = updatedMetadata.creationTimestamp()
                        .or(oldMetadata::creationTimestamp)
                        .map(ChronoZonedDateTime::toEpochSecond)
                        .orElse(0L);
                var mergedMetadata = new NewsletterMetadataBuilder()
                        .name(name)
                        .description(description)
                        .picture(picture)
                        .handle(handle)
                        .settings(settings)
                        .invite(invite)
                        .verification(verification)
                        .creationTimestampSeconds(creationTimestamp)
                        .build();
                newsletter.setMetadata(mergedMetadata);
            }
        });
    }

    private void handleNewsletterJoin(Node update) {
        var joinPayload = update.contentAsBytes()
                .orElseThrow(() -> new NoSuchElementException("Missing join payload"));
        NewsletterResponse.ofJson(joinPayload).ifPresent(response -> {
            var newsletter = response.newsletter();
            socketConnection.store().addNewsletter(newsletter);
            if (!socketConnection.store().webHistorySetting().isZero()) {
                socketConnection.queryNewsletterMessages(newsletter.jid(), DEFAULT_NEWSLETTER_MESSAGES);
            }
        });
    }

    private void handleNewsletterMute(Node update) {
        var mutePayload = update.contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing mute payload"));
        NewsletterMuteResponse.ofJson(mutePayload).ifPresent(response -> {
            var newsletter = socketConnection.store()
                    .findNewsletterByJid(response.jid())
                    .orElseThrow(() -> new NoSuchElementException("Missing newsletter"));
            newsletter.viewerMetadata()
                    .ifPresent(viewerMetadata -> viewerMetadata.setMute(response.mute()));
        });
    }

    private void handleNewsletterLeave(Node update) {
        var leavePayload = update.contentAsString()
                .orElseThrow(() -> new NoSuchElementException("Missing leave payload"));
        NewsletterLeaveResponse.ofJson(leavePayload)
                .ifPresent(response -> socketConnection.store().removeNewsletter(response.jid()));
    }

    private void handleRegistrationNotification(Node node) {
        var child = node.findChild("wa_old_registration");
        if (child.isEmpty()) {
            return;
        }

        var code = child.get().attributes().getOptionalLong("code");
        if (code.isEmpty()) {
            return;
        }

        socketConnection.onRegistrationCode(code.getAsLong());
    }

    private void handlePictureNotification(Node node) {
        var fromJid = node.attributes()
                .getRequiredJid("from");
        if (fromJid.hasServer(JidServer.groupOrCommunity())) {
            var fromChat = socketConnection.store()
                    .findChatByJid(fromJid)
                    .orElseGet(() -> socketConnection.store().addNewChat(fromJid));
            var timestamp = node.attributes().getLong("t");
            var participantJid = node.attributes()
                    .getOptionalJid("participant")
                    .orElse(null);
            addMessageForGroupStubType(timestamp, fromChat, participantJid, ChatMessageStubType.GROUP_CHANGE_ICON, node);
            return;
        }
        var fromContact = socketConnection.store().findContactByJid(fromJid).orElseGet(() -> {
            var contact = socketConnection.store().addContact(fromJid);
            socketConnection.onNewContact(contact);
            return contact;
        });
        socketConnection.onContactPictureChanged(fromContact);
    }

    private void handleGroupNotification(Node node) {
        var timestamp = node.attributes().getLong("t");
        var fromJid = node.attributes()
                .getRequiredJid("from");
        var fromChat = socketConnection.store()
                .findChatByJid(fromJid)
                .orElseGet(() -> socketConnection.store().addNewChat(fromJid));
        var participantJid = node.attributes()
                .getOptionalJid("participant")
                .orElse(null);
        var notificationType = node.description();
        var child = node.findChild();
        var bodyType = child.map(Node::description)
                .orElse(null);
        var stubType = ChatMessageStubType.getStubType(notificationType, bodyType);
        addMessageForGroupStubType(timestamp, fromChat, participantJid, stubType, node);
    }

    private void addMessageForGroupStubType(long timestamp, Chat chat, Jid sender, ChatMessageStubType stubType, Node metadata) {
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId(socketConnection.store().clientType()))
                .chatJid(chat.jid())
                .senderJid(sender)
                .build();
        var message = new ChatMessageInfoBuilder()
                .status(MessageStatus.DELIVERED)
                .timestampSeconds(timestamp)
                .key(key)
                .ignore(true)
                .stubType(stubType)
                .stubParameters(stubType.getParameters(metadata))
                .senderJid(sender)
                .build();
        chat.addNewMessage(message);
        socketConnection.onNewMessage(message);
    }

    private void handleEncryptNotification(Node node) {
        var chat = node.attributes()
                .getRequiredJid("from");
        if (!chat.isServerJid(JidServer.user())) {
            return;
        }
        var keysSize = node.findChild("count")
                .orElseThrow(() -> new NoSuchElementException("Missing count in notification"))
                .attributes()
                .getInt("value");
        socketConnection.sendPreKeys(keysSize);
    }

    private void handleAccountSyncNotification(Node node) {
        var child = node.findChild();
        if (child.isEmpty()) {
            return;
        }
        switch (child.get().description()) {
            case "devices" -> handleDevices(child.get());
            case "privacy" -> changeUserPrivacySetting(child.get());
            case "disappearing_mode" -> updateUserDisappearingMode(child.get());
            case "status" -> socketConnection.updateUserAbout(true);
            case "picture" -> socketConnection.updateUserPicture(true);
            case "blocklist" -> updateBlocklist(child.orElse(null));
        }
    }

    private void handleDevices(Node child) {
        var deviceHash = child.attributes().getString("dhash");
        socketConnection.store().setDeviceHash(deviceHash);
        var devices = child.listChildren("device")
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.attributes().getRequiredJid("jid"),
                        entry -> entry.attributes().getInt("key-index"),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
        var companionJid = socketConnection.store()
                .jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"))
                .withoutData();
        var companionDevice = devices.remove(companionJid);
        devices.put(companionJid, companionDevice);
        socketConnection.store().setLinkedDevicesKeys(devices);
        socketConnection.onDevices(devices);
        var keyIndexListNode = child.findChild("key-index-list")
                .orElseThrow(() -> new NoSuchElementException("Missing index key node from device sync"));
        var signedKeyIndexBytes = keyIndexListNode.contentAsBytes()
                .orElseThrow(() -> new NoSuchElementException("Missing index key from device sync"));
        socketConnection.keys().setSignedKeyIndex(signedKeyIndexBytes);
        var signedKeyIndexTimestamp = keyIndexListNode.attributes().getLong("ts");
        socketConnection.keys().setSignedKeyIndexTimestamp(signedKeyIndexTimestamp);
    }

    private void updateBlocklist(Node child) {
        child.listChildren("item").forEach(this::updateBlocklistEntry);
    }

    private void updateBlocklistEntry(Node entry) {
        entry.attributes()
                .getOptionalJid("jid")
                .flatMap(socketConnection.store()::findContactByJid)
                .ifPresent(contact -> {
                    contact.setBlocked(Objects.equals(entry.attributes().getString("action"), "block"));
                    socketConnection.onContactBlocked(contact);
                });
    }

    private void changeUserPrivacySetting(Node child) {
        var category = child.listChildren("category");
        category.forEach(entry -> socketConnection.addPrivacySetting(entry, true));
    }

    private void updateUserDisappearingMode(Node child) {
        var timer = ChatEphemeralTimer.of(child.attributes().getInt("duration"));
        socketConnection.store().setNewChatsEphemeralTimer(timer);
    }

    private void handleServerSyncNotification(Node node) {
        if(!socketConnection.keys().initialAppSync()) {
            return;
        }

        var patches = node.listChildren("collection")
                .stream()
                .map(entry -> entry.attributes().getRequiredString("name"))
                .map(PatchType::of)
                .toArray(PatchType[]::new);
        socketConnection.pullPatch(patches);
    }


    private void handleCompanionRegistration(Node node) {
        try {
            var phoneNumber = socketConnection.store()
                    .phoneNumber()
                    .map(PhoneNumber::toJid)
                    .orElseThrow(() -> new IllegalArgumentException("Missing phone number"));
            var linkCodeCompanionReg = node.findChild("link_code_companion_reg")
                    .orElseThrow(() -> new NoSuchElementException("Missing link_code_companion_reg: " + node));
            var ref = linkCodeCompanionReg.findChild("link_code_pairing_ref")
                    .flatMap(Node::contentAsBytes)
                    .orElseThrow(() -> new IllegalArgumentException("Missing link_code_pairing_ref: " + node));
            var primaryIdentityPublicKey = linkCodeCompanionReg.findChild("primary_identity_pub")
                    .flatMap(Node::contentAsBytes)
                    .orElseThrow(() -> new IllegalArgumentException("Missing primary_identity_pub: " + node));
            var primaryEphemeralPublicKeyWrapped = linkCodeCompanionReg.findChild("link_code_pairing_wrapped_primary_ephemeral_pub")
                    .flatMap(Node::contentAsBytes)
                    .orElseThrow(() -> new IllegalArgumentException("Missing link_code_pairing_wrapped_primary_ephemeral_pub: " + node));
            var codePairingPublicKey = socketConnection.decryptPairingKey(primaryEphemeralPublicKeyWrapped);
            var companionSharedKey = Curve25519.sharedKey(codePairingPublicKey, socketConnection.keys().companionKeyPair().privateKey());
            var random = Bytes.random(32);
            var linkCodeSalt = Bytes.random(32);
            var linkCodePairingExpanded = Hkdf.extractAndExpand(companionSharedKey, linkCodeSalt, "link_code_pairing_key_bundle_encryption_key".getBytes(StandardCharsets.UTF_8), 32);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(linkCodePairingExpanded, "AES"),
                    new GCMParameterSpec(128, Bytes.random(12))
            );
            cipher.update(socketConnection.keys().identityKeyPair().publicKey());
            cipher.update(primaryIdentityPublicKey);
            cipher.update(random);
            var encrypted = cipher.doFinal();
            var encryptedPayload = Bytes.concat(linkCodeSalt, Bytes.random(12), encrypted);
            var identitySharedKey = Curve25519.sharedKey(primaryIdentityPublicKey, socketConnection.keys().identityKeyPair().privateKey());
            var identityPayload = Bytes.concat(companionSharedKey, identitySharedKey, random);
            var advSecretPublicKey = Hkdf.extractAndExpand(identityPayload, "adv_secret".getBytes(StandardCharsets.UTF_8), 32);
            socketConnection.keys().setCompanionKeyPair(new SignalKeyPair(advSecretPublicKey, socketConnection.keys().companionKeyPair().privateKey()));
            var confirmation = Node.of(
                    "link_code_companion_reg",
                    Map.of("jid", phoneNumber, "stage", "companion_finish"),
                    Node.of("link_code_pairing_wrapped_key_bundle", encryptedPayload),
                    Node.of("companion_identity_public", socketConnection.keys().identityKeyPair().publicKey()),
                    Node.of("link_code_pairing_ref", ref)
            );
            socketConnection.sendQuery("set", "md", confirmation);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot encrypt companion registration", exception);
        }
    }
}