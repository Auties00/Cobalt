package com.github.auties00.cobalt.socket.notification;

import com.github.auties00.cobalt.socket.SocketStream;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;
import com.github.auties00.libsignal.key.SignalPreKeyPair;
import it.auties.curve25519.Curve25519;
import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.io.json.response.NewsletterLeaveResponse;
import com.github.auties00.cobalt.io.json.response.NewsletterMuteResponse;
import com.github.auties00.cobalt.io.json.response.NewsletterResponse;
import com.github.auties00.cobalt.io.json.response.NewsletterStateResponse;
import com.github.auties00.cobalt.io.node.Node;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.model.info.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.model.info.ChatMessageStubType;
import com.github.auties00.cobalt.model.info.NewsletterMessageInfo;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.model.ChatMessageKey;
import com.github.auties00.cobalt.model.message.model.ChatMessageKeyBuilder;
import com.github.auties00.cobalt.model.message.model.MessageStatus;
import com.github.auties00.cobalt.model.mobile.PhoneNumber;
import com.github.auties00.cobalt.model.newsletter.NewsletterMetadataBuilder;
import com.github.auties00.cobalt.model.newsletter.NewsletterReaction;
import com.github.auties00.cobalt.model.newsletter.NewsletterVerification;
import com.github.auties00.cobalt.model.privacy.PrivacySettingEntry;
import com.github.auties00.cobalt.model.privacy.PrivacySettingEntryBuilder;
import com.github.auties00.cobalt.model.privacy.PrivacySettingType;
import com.github.auties00.cobalt.model.privacy.PrivacySettingValue;
import com.github.auties00.cobalt.model.sync.PatchType;
import com.github.auties00.cobalt.util.Bytes;
import com.github.auties00.cobalt.util.PhonePairingCode;
import com.github.auties00.cobalt.util.Scalar;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.chrono.ChronoZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class NotificationStreamNodeHandler extends SocketStream.Handler {
    private static final int DEFAULT_NEWSLETTER_MESSAGES = 100;
    private static final byte[] SIGNAL_KEY_TYPE = {SignalIdentityPublicKey.type()};

    private final PhonePairingCode pairingCode;
    public NotificationStreamNodeHandler(Whatsapp whatsapp, PhonePairingCode pairingCode) {
        super(whatsapp, "notification");
        this.pairingCode = pairingCode;
    }

    @Override
    public void handle(Node node) {
        try {
            var type = node.getRequiredAttribute("type")
                    .toString();
            switch (type) {
                case "w:gp2" -> handleGroupNotification(node);
                case "server_sync" -> handleServerSyncNotification(node);
                case "account_sync" -> handleAccountSyncNotification(node);
                case "encrypt" -> handleEncryptNotification(node);
                case "picture" -> handlePictureNotification(node);
                case "registration" -> handleRegistrationNotification(node);
                case "link_code_companion_reg" -> handleCompanionRegistration(node);
                case "newsletter" -> handleNewsletter(node);
                case "mex" -> handleMexNamespace(node);
            }
        } finally {
            whatsapp.sendAck(node);
        }
    }

    private void handleNewsletter(Node node) {
        var newsletterJid = node.getRequiredAttribute("from")
                .toJid();
        var newsletter = whatsapp.store()
                .findNewsletterByJid(newsletterJid)
                .orElse(null);
        if (newsletter == null) {
            return;
        }

        var liveUpdates = node.firstChildByDescription("live_updates")
                .orElse(null);
        if (liveUpdates == null) {
            return;
        }


        var messages = liveUpdates.firstChildByDescription("messages")
                .orElse(null);
        if (messages == null) {
            return;
        }

        messages.streamChildrenByDescription("message").forEachOrdered(messageNode -> {
            var messageId = messageNode.getRequiredAttribute("server_id")
                    .toString();
            var newsletterMessage = whatsapp.store()
                    .findMessageById(newsletter, messageId)
                    .orElse(null);
            if (newsletterMessage == null) {
                return;
            }

            messageNode.firstChildByDescription("reactions")
                    .stream()
                    .flatMap(reactions -> reactions.streamChildrenByDescription("reaction"))
                    .forEachOrdered(reaction -> onNewsletterReaction(reaction, newsletterMessage));
        });
    }

    private void onNewsletterReaction(Node reaction, NewsletterMessageInfo newsletterMessage) {
        var reactionCode = reaction.getRequiredAttribute("code")
                .toString();
        var reactionCountValue = reaction.getRequiredAttribute("count")
                .toString();
        var reactionCount = Integer.parseUnsignedInt(reactionCountValue);
        var newReaction = new NewsletterReaction(reactionCode, reactionCount, false);
        newsletterMessage.addReaction(newReaction)
                .ifPresent(oldReaction -> newReaction.setFromMe(oldReaction.fromMe()));
    }

    private void handleMexNamespace(Node node) {
        var update = node.firstChildByDescription("update")
                .orElse(null);
        if (update == null) {
            return;
        }

        var operationName = update.getRequiredAttribute("op_name")
                .toString();
        switch (operationName) {
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
        var updatePayload = update.toContentString()
                .orElseThrow(() -> new NoSuchElementException("Missing state update payload"));
        NewsletterStateResponse.ofJson(updatePayload).ifPresent(response -> {
            var newsletter = whatsapp.store()
                    .findNewsletterByJid(response.jid())
                    .orElseThrow(() -> new NoSuchElementException("Missing newsletter"));
            response.state()
                    .ifPresent(newsletter::setState);
        });
    }

    private void handleNewsletterMetadataUpdate(Node update) {
        var updatePayload = update.toContentBytes()
                .orElseThrow(() -> new NoSuchElementException("Missing update payload"));
        NewsletterResponse.ofJson(updatePayload).ifPresent(response -> {
            var updatedNewsletter = response.newsletter();
            var newsletter = whatsapp.store()
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
        var joinPayload = update.toContentBytes()
                .orElseThrow(() -> new NoSuchElementException("Missing join payload"));
        NewsletterResponse.ofJson(joinPayload).ifPresent(response -> {
            var newsletter = response.newsletter();
            whatsapp.store().addNewsletter(newsletter);
            if (!whatsapp.store().historyLength().isZero()) {
                whatsapp.queryNewsletterMessages(newsletter.jid(), DEFAULT_NEWSLETTER_MESSAGES);
            }
        });
    }

    private void handleNewsletterMute(Node update) {
        var mutePayload = update.toContentString()
                .orElseThrow(() -> new NoSuchElementException("Missing mute payload"));
        NewsletterMuteResponse.ofJson(mutePayload).ifPresent(response -> {
            var newsletter = whatsapp.store()
                    .findNewsletterByJid(response.jid())
                    .orElseThrow(() -> new NoSuchElementException("Missing newsletter"));
            newsletter.viewerMetadata()
                    .ifPresent(viewerMetadata -> viewerMetadata.setMute(response.mute()));
        });
    }

    private void handleNewsletterLeave(Node update) {
        var leavePayload = update.toContentString()
                .orElseThrow(() -> new NoSuchElementException("Missing leave payload"));
        NewsletterLeaveResponse.ofJson(leavePayload)
                .ifPresent(response -> whatsapp.store().removeNewsletter(response.jid()));
    }

    private void handleRegistrationNotification(Node node) {
        var child = node.firstChildByDescription("wa_old_registration");
        if (child.isEmpty()) {
            return;
        }

        var code = child.get().attributes().getOptionalLong("code");
        if (code.isEmpty()) {
            return;
        }

        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onRegistrationCode(code.getAsLong()));
            Thread.startVirtualThread(() -> listener.onRegistrationCode(whatsapp, code.getAsLong()));
        }
    }

    private void handlePictureNotification(Node node) {
        var fromJid = node.attributes()
                .getRequiredJid("from");
        if (fromJid.hasServer(JidServer.groupOrCommunity())) {
            var fromChat = whatsapp.store()
                    .findChatByJid(fromJid)
                    .orElseGet(() -> whatsapp.store().addNewChat(fromJid));
            var timestamp = node.attributes().getLong("t");
            var participantJid = node.attributes()
                    .getOptionalJid("participant")
                    .orElse(null);
            addMessageForGroupStubType(timestamp, fromChat, participantJid, ChatMessageStubType.GROUP_CHANGE_ICON, node);
            return;
        }
        var fromContact = whatsapp.store().findContactByJid(fromJid).orElseGet(() -> {
            var contact = whatsapp.store().addContact(fromJid);
            for (var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onNewContact(contact));
                Thread.startVirtualThread(() -> listener.onNewContact(whatsapp, contact));
            }
            return contact;
        });
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onProfilePictureChanged(fromContact));
            Thread.startVirtualThread(() -> listener.onProfilePictureChanged(whatsapp, fromContact));
        }
    }

    private void handleGroupNotification(Node node) {
        var timestamp = node.attributes().getLong("t");
        var fromJid = node.attributes()
                .getRequiredJid("from");
        var fromChat = whatsapp.store()
                .findChatByJid(fromJid)
                .orElseGet(() -> whatsapp.store().addNewChat(fromJid));
        var participantJid = node.attributes()
                .getOptionalJid("participant")
                .orElse(null);
        var notificationType = node.description();
        var child = node.firstChildByDescription();
        var bodyType = child.map(Node::description)
                .orElse(null);
        var stubType = ChatMessageStubType.getStubType(notificationType, bodyType);
        addMessageForGroupStubType(timestamp, fromChat, participantJid, stubType, node);
    }

    private void addMessageForGroupStubType(long timestamp, Chat chat, Jid sender, ChatMessageStubType stubType, Node metadata) {
        var key = new ChatMessageKeyBuilder()
                .id(ChatMessageKey.randomId(whatsapp.store().clientType()))
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
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onNewMessage(message));
            Thread.startVirtualThread(() -> listener.onNewMessage(whatsapp, message));
        }
    }

    private void handleEncryptNotification(Node node) {
        var chat = node.attributes()
                .getRequiredJid("from");
        if (!chat.isServerJid(JidServer.user())) {
            return;
        }
        var keysSize = node.firstChildByDescription("count")
                .orElseThrow(() -> new NoSuchElementException("Missing count in notification"))
                .attributes()
                .getInt("value");
        var keys = whatsapp.keys();
        var startId = keys.hasPreKeys() ? keys.preKeys().getLast().id() + 1 : 1;
        var preKeys = IntStream.range(startId, startId + keysSize)
                .mapToObj(SignalPreKeyPair::random)
                .peek(keys::addPreKey)
                .map(keyPair -> Node.of(
                        "key",
                        Node.of("id", Scalar.intToBytes(keyPair.id(), 3)),
                        Node.of("value", keyPair.publicKey()))
                )
                .toList();
        var keyPair = keys.signedKeyPair();
        whatsapp.sendQuery(
                "set",
                "encrypt",
                Node.of("registration", keys.encodedRegistrationId()),
                Node.of("type", SIGNAL_KEY_TYPE),
                Node.of("identity", keys.identityKeyPair().publicKey()),
                Node.of("list", preKeys),
                Node.of("skey",
                        Node.of("id", Scalar.intToBytes(keyPair.id(), 3)),
                        Node.of("value", keyPair.publicKey()),
                        Node.of("signature", keyPair.signature())
                )
        );
    }

    private void handleAccountSyncNotification(Node node) {
        var child = node.firstChildByDescription();
        if (child.isEmpty()) {
            return;
        }
        switch (child.get().description()) {
            case "devices" -> handleDevices(child.get());
            case "privacy" -> changeUserPrivacySetting(child.get());
            case "disappearing_mode" -> updateUserDisappearingMode(child.get());
            case "status" -> updateUserAbout();
            case "picture" -> updateUserPicture();
            case "blocklist" -> updateBlocklist(child.orElse(null));
        }
    }

    public void updateUserPicture() {
        var user = whatsapp
                .store()
                .jid()
                .orElse(null);
        if(user == null) {
            return;
        }

        var result = whatsapp.queryPicture(user.withoutData());
        whatsapp.store()
                .setProfilePicture(result.orElse(null));
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onProfilePictureChanged(user.withoutData()));
            Thread.startVirtualThread(() -> listener.onProfilePictureChanged(whatsapp, user.withoutData()));
        }
    }


    private void updateUserAbout() {
        var user = whatsapp
                .store()
                .jid()
                .orElse(null);
        if(user == null) {
            return;
        }

        var response = whatsapp.queryAbout(user.withoutData())
                .orElse(null);
        if(response == null) {
            return;
        }

        var oldAbout = whatsapp.store()
                .about()
                .orElse(null);
        var newAbout = response.about()
                .orElse(null);
        whatsapp.store()
                .setAbout(newAbout);
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onAboutChanged(oldAbout, newAbout));
            Thread.startVirtualThread(() -> listener.onAboutChanged(whatsapp, oldAbout, newAbout));
        }
    }

    private void handleDevices(Node child) {
        var deviceHash = child.attributes().getString("dhash");
        whatsapp.store().setDeviceHash(deviceHash);
        var devices = child.listChildren("device")
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.attributes().getRequiredJid("value"),
                        entry -> entry.attributes().getInt("key-index"),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
        var companionJid = whatsapp.store()
                .jid()
                .orElseThrow(() -> new IllegalStateException("The session isn't connected"))
                .withoutData();
        var companionDevice = devices.remove(companionJid);
        devices.put(companionJid, companionDevice);
        whatsapp.store().setLinkedDevicesKeys(devices);
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onLinkedDevices(devices.keySet()));
            Thread.startVirtualThread(() -> listener.onLinkedDevices(whatsapp, devices.keySet()));
        }
        var keyIndexListNode = child.firstChildByDescription("key-index-list")
                .orElseThrow(() -> new NoSuchElementException("Missing index key node from device sync"));
        var signedKeyIndexBytes = keyIndexListNode.toContentBytes()
                .orElseThrow(() -> new NoSuchElementException("Missing index key from device sync"));
        whatsapp.keys().setSignedKeyIndex(signedKeyIndexBytes);
        var signedKeyIndexTimestamp = keyIndexListNode.attributes().getLong("ts");
        whatsapp.keys().setSignedKeyIndexTimestamp(signedKeyIndexTimestamp);
    }

    private void updateBlocklist(Node child) {
        child.listChildren("item").forEach(this::updateBlocklistEntry);
    }

    private void updateBlocklistEntry(Node entry) {
        entry.attributes()
                .getOptionalJid("value")
                .flatMap(whatsapp.store()::findContactByJid)
                .ifPresent(contact -> {
                    contact.setBlocked(Objects.equals(entry.attributes().getString("action"), "block"));
                    for (var listener : whatsapp.store().listeners()) {
                        Thread.startVirtualThread(() -> listener.onContactBlocked(contact));
                        Thread.startVirtualThread(() -> listener.onContactBlocked(whatsapp, contact));
                    }
                });
    }

    private void changeUserPrivacySetting(Node child) {
        var category = child.listChildren("category");
        for (Node entry : category) {
            var privacySettingName = entry.attributes().getString("name");
            var privacyType = PrivacySettingType.of(privacySettingName);
            if(privacyType.isEmpty()) {
                continue;
            }

            var privacyValueName = entry.attributes().getString("value");
            var privacyValue = PrivacySettingValue.of(privacyValueName);
            if(privacyValue.isEmpty()) {
                continue;
            }

            var privacySetting = whatsapp.store()
                    .findPrivacySetting(privacyType.get());
            var excluded = getExcludedContacts(entry, privacySetting, privacyValue.get());
            var newEntry = new PrivacySettingEntryBuilder()
                    .type(privacyType.get())
                    .value(privacyValue.get())
                    .excluded(excluded)
                    .build();
            whatsapp.store()
                    .addPrivacySetting(privacyType.get(), newEntry);
            for(var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onPrivacySettingChanged(privacySetting, newEntry));
                Thread.startVirtualThread(() -> listener.onPrivacySettingChanged(whatsapp, privacySetting, newEntry));
            }
        }
    }

    private List<Jid> getExcludedContacts(Node node, PrivacySettingEntry privacyEntry, PrivacySettingValue privacyValue) {
        if (privacyValue != PrivacySettingValue.CONTACTS_EXCEPT) {
            return List.of();
        }

        var newValues = new ArrayList<>(privacyEntry.excluded());
        for (var entry : node.listChildren("user")) {
            var jid = entry.attributes()
                    .getRequiredJid("value");
            if (entry.attributes().hasValue("action", "add")) {
                newValues.add(jid);
                continue;
            }

            newValues.remove(jid);
        }
        return newValues;
    }


    private void updateUserDisappearingMode(Node child) {
        var timer = ChatEphemeralTimer.of(child.attributes().getInt("duration"));
        whatsapp.store().setNewChatsEphemeralTimer(timer);
    }

    private void handleServerSyncNotification(Node node) {
        var patches = node.listChildren("collection")
                .stream()
                .map(entry -> entry.attributes().getRequiredString("name"))
                .map(PatchType::of)
                .toArray(PatchType[]::new);
        whatsapp.pullWebAppStatePatches(false, patches);
    }


    private void handleCompanionRegistration(Node node) {
        try {
            var phoneNumber = whatsapp.store()
                    .phoneNumber()
                    .map(PhoneNumber::toJid)
                    .orElseThrow(() -> new IllegalArgumentException("Missing phone value"));
            var linkCodeCompanionReg = node.firstChildByDescription("link_code_companion_reg")
                    .orElseThrow(() -> new NoSuchElementException("Missing link_code_companion_reg: " + node));
            var ref = linkCodeCompanionReg.firstChildByDescription("link_code_pairing_ref")
                    .flatMap(Node::toContentBytes)
                    .orElseThrow(() -> new IllegalArgumentException("Missing link_code_pairing_ref: " + node));
            var primaryIdentityPublicKey = linkCodeCompanionReg.firstChildByDescription("primary_identity_pub")
                    .flatMap(Node::toContentBytes)
                    .orElseThrow(() -> new IllegalArgumentException("Missing primary_identity_pub: " + node));
            var primaryEphemeralPublicKeyWrapped = linkCodeCompanionReg.firstChildByDescription("link_code_pairing_wrapped_primary_ephemeral_pub")
                    .flatMap(Node::toContentBytes)
                    .orElseThrow(() -> new IllegalArgumentException("Missing link_code_pairing_wrapped_primary_ephemeral_pub: " + node));
            var codePairingPublicKey = pairingCode.decrypt(primaryEphemeralPublicKeyWrapped);
            var companionPrivateKey = whatsapp.store()
                    .companionKeyPair()
                    .orElseThrow(() -> new InternalError("No companion key pair was set"))
                    .privateKey()
                    .toEncodedPoint();
            var companionSharedKey = Curve25519.sharedKey(companionPrivateKey, codePairingPublicKey);
            var random = Bytes.random(32);
            var linkCodeSalt = Bytes.random(32);
            var secretKeyHkdf = KDF.getInstance("HKDF-SHA256");
            var secretKeyHkdfParams = HKDFParameterSpec.ofExtract()
                    .addSalt(linkCodeSalt)
                    .addIKM(new SecretKeySpec(companionSharedKey, "AES"))
                    .thenExpand("link_code_pairing_key_bundle_encryption_key".getBytes(StandardCharsets.UTF_8), 32);
            var secretKey = secretKeyHkdf.deriveKey("AES", secretKeyHkdfParams);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(128, Bytes.random(12))
            );
            cipher.update(whatsapp.keys().identityKeyPair().publicKey().toEncodedPoint());
            cipher.update(primaryIdentityPublicKey);
            cipher.update(random);
            var encrypted = cipher.doFinal();
            var encryptedPayload = Bytes.concat(linkCodeSalt, Bytes.random(12), encrypted);
            var identitySharedKey = Curve25519.sharedKey(whatsapp.store().identityKeyPair().privateKey().toEncodedPoint(), primaryIdentityPublicKey);
            var identityPayload = Bytes.concat(companionSharedKey, identitySharedKey, random);
            var companionKeyHkdf = KDF.getInstance("HKDF-SHA256");
            var companionKeyHkdfParams = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(identityPayload, "AES"))
                    .thenExpand("adv_secret".getBytes(StandardCharsets.UTF_8), 32);
            var companionKey = companionKeyHkdf.deriveData(companionKeyHkdfParams);
            var companionPublicKey = SignalIdentityPublicKey.ofDirect(companionKey);
            var companionKeyPair = new SignalIdentityKeyPair(companionPublicKey, companionPrivateKey);
            whatsapp.keys().setCompanionKeyPair(companionKeyPair);
            var confirmation = Node.of(
                    "link_code_companion_reg",
                    Map.of("value", phoneNumber, "stage", "companion_finish"),
                    Node.of("link_code_pairing_wrapped_key_bundle", encryptedPayload),
                    Node.of("companion_identity_public", whatsapp.keys().identityKeyPair().publicKey()),
                    Node.of("link_code_pairing_ref", ref)
            );
            whatsapp.sendQuery("set", "md", confirmation);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot encrypt companion registration", exception);
        }
    }
}