package com.github.auties00.cobalt.socket.notification;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.migration.LidMigrationService;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.model.info.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.model.info.MessageInfoStubType;
import com.github.auties00.cobalt.model.info.NewsletterMessageInfo;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.model.ChatMessageKey;
import com.github.auties00.cobalt.model.message.model.ChatMessageKeyBuilder;
import com.github.auties00.cobalt.model.message.model.MessageStatus;
import com.github.auties00.cobalt.model.newsletter.NewsletterMetadataBuilder;
import com.github.auties00.cobalt.model.newsletter.NewsletterReaction;
import com.github.auties00.cobalt.model.newsletter.NewsletterVerification;
import com.github.auties00.cobalt.model.privacy.PrivacySettingEntry;
import com.github.auties00.cobalt.model.privacy.PrivacySettingEntryBuilder;
import com.github.auties00.cobalt.model.privacy.PrivacySettingType;
import com.github.auties00.cobalt.model.privacy.PrivacySettingValue;
import com.github.auties00.cobalt.model.sync.PatchType;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.node.mex.json.response.*;
import com.github.auties00.cobalt.socket.SocketPhonePairing;
import com.github.auties00.cobalt.socket.SocketStream;
import com.github.auties00.cobalt.util.SecureBytes;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.chrono.ChronoZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class NotificationStreamNodeHandler extends SocketStream.Handler {
    private static final int DEFAULT_NEWSLETTER_MESSAGES = 100;

    private final SocketPhonePairing pairingCode;
    private final LidMigrationService lidMigrationService;
    public NotificationStreamNodeHandler(WhatsAppClient whatsapp, SocketPhonePairing pairingCode, LidMigrationService lidMigrationService) {
        super(whatsapp, "notification");
        this.pairingCode = pairingCode;
        this.lidMigrationService = lidMigrationService;
    }

    @Override
    public void handle(Node node) {
        try {
            var type = node.getRequiredAttributeAsString("type");
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
        var newsletterJid = node.getRequiredAttributeAsJid("from");
        var newsletter = whatsapp.store()
                .findNewsletterByJid(newsletterJid)
                .orElse(null);
        if (newsletter == null) {
            return;
        }

        var liveUpdates = node.getChild("live_updates")
                .orElse(null);
        if (liveUpdates == null) {
            return;
        }


        var messages = liveUpdates.getChild("messages")
                .orElse(null);
        if (messages == null) {
            return;
        }

        messages.streamChildren("message").forEachOrdered(messageNode -> {
            var messageId = messageNode.getRequiredAttribute("server_id")
                    .toString();
            var newsletterMessage = whatsapp.store()
                    .findMessageById(newsletter, messageId)
                    .orElse(null);
            if (newsletterMessage == null) {
                return;
            }

            messageNode.getChild("reactions")
                    .stream()
                    .flatMap(reactions -> reactions.streamChildren("reaction"))
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
        var update = node.getChild("update")
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
            case "LidChangeNotification" -> handleLidChangeNotification(update);
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
            var historyPolicy = whatsapp.store().webHistoryPolicy();
            if (historyPolicy.isPresent() && !historyPolicy.get().isZero() && historyPolicy.get().hasNewsletters()) {
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
        var child = node.getChild("wa_old_registration");
        if (child.isEmpty()) {
            return;
        }

        var code = child.get().getAttributeAsLong("code");
        if (code.isEmpty()) {
            return;
        }

        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onRegistrationCode(whatsapp, code.getAsLong()));
        }
    }

    private void handlePictureNotification(Node node) {
        var fromJid = node.getRequiredAttributeAsJid("from");
        if (fromJid.hasServer(JidServer.groupOrCommunity())) {
            var fromChat = whatsapp.store()
                    .findChatByJid(fromJid)
                    .orElseGet(() -> whatsapp.store().addNewChat(fromJid));
            var timestamp = node.getRequiredAttributeAsLong("t");
            var participantJid = node.getAttributeAsJid("participant", null);
            addMessageForGroupStubType(timestamp, fromChat, participantJid, MessageInfoStubType.GROUP_CHANGE_ICON, node);
            return;
        }
        if(whatsapp.store().findContactByJid(fromJid).isEmpty()) {
            var contact = whatsapp.store().addNewContact(fromJid);
            for (var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onNewContact(whatsapp, contact));
            }
        }
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onProfilePictureChanged(whatsapp, fromJid));
        }
    }

    private void handleGroupNotification(Node node) {
        var timestamp = node.getRequiredAttributeAsLong("t");
        var fromJid = node.getRequiredAttributeAsJid("from");
        var fromChat = whatsapp.store()
                .findChatByJid(fromJid)
                .orElseGet(() -> whatsapp.store().addNewChat(fromJid));
        var participantJid = node.getAttributeAsJid("participant", null);
        var notificationType = node.description();
        var child = node.getChild();
        var bodyType = child.map(Node::description)
                .orElse(null);
        var stubType = MessageInfoStubType.getStubType(notificationType, bodyType);
        addMessageForGroupStubType(timestamp, fromChat, participantJid, stubType, node);
    }

    private void addMessageForGroupStubType(long timestamp, Chat chat, Jid sender, MessageInfoStubType stubType, Node metadata) {
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
        chat.addMessage(message);
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onNewMessage(whatsapp, message));
        }
    }

    private void handleEncryptNotification(Node node) {
        var chat = node.getRequiredAttributeAsJid("from");
        if (!chat.isServerJid(JidServer.user())) {
            return;
        }
        var keysCount = node.getRequiredChild("count")
                .getRequiredAttributeAsLong("value");
        whatsapp.sendPreKeys(keysCount);
    }

    private void handleAccountSyncNotification(Node node) {
        var child = node.getChild();
        if (child.isEmpty()) {
            return;
        }
        switch (child.get().description()) {
            case "privacy" -> changeUserPrivacySetting(child.get());
            case "disappearing_mode" -> updateUserDisappearingMode(child.get());
            case "status" -> updateUserAbout();
            case "picture" -> updateUserPicture();
            case "blocklist" -> updateBlocklist(child.orElse(null));
        }
    }

    private void handleLidChangeNotification(Node update) {
        var payloadString = update.toContentBytes()
                .orElse(null);
        if (payloadString == null) {
            return;
        }

        var notification = LidChangeNotificationResponse.ofJson(payloadString);
        if (notification.isEmpty()) {
            return;
        }

        var lidChange = notification.get();
        lidMigrationService.handleNotification(lidChange);
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

        var newAbout = whatsapp.queryAbout(user.withoutData())
                .orElse(null);
        if(newAbout == null) {
            return;
        }

        var oldAbout = whatsapp.store()
                .about()
                .orElse(null);
        whatsapp.store()
                .setAbout(newAbout);
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onAboutChanged(whatsapp, oldAbout, newAbout));
        }
    }

    private void updateBlocklist(Node child) {
        child.streamChildren("item").forEachOrdered(entry -> {
            var value = entry.getAttributeAsJid("value");
            if(value.isEmpty()) {
                return;
            }

            whatsapp.store()
                    .findContactByJid(value.get())
                    .ifPresent(contact -> contact.setBlocked(entry.hasAttribute("action", "block")));
            for (var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onContactBlocked(whatsapp, value.get()));
            }
        });
    }

    private void changeUserPrivacySetting(Node child) {
        child.streamChildren("category").forEachOrdered(entry -> {
            var privacyType = entry.getAttributeAsString("name")
                    .flatMap(PrivacySettingType::of);
            if(privacyType.isEmpty()) {
                return;
            }

            var privacyValue = entry.getAttributeAsString("value")
                    .flatMap(PrivacySettingValue::of);
            if(privacyValue.isEmpty()) {
                return;
            }

            var privacySetting = whatsapp.store()
                    .findPrivacySetting(privacyType.get());
            var excluded = getExcludedContacts(entry, privacySetting.orElse(null), privacyValue.get());
            var newEntry = new PrivacySettingEntryBuilder()
                    .type(privacyType.get())
                    .value(privacyValue.get())
                    .excluded(excluded)
                    .build();
            whatsapp.store()
                    .addPrivacySetting(newEntry);
            for(var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onPrivacySettingChanged(whatsapp, newEntry));
            }
        });
    }

    private List<Jid> getExcludedContacts(Node node, PrivacySettingEntry privacyEntry, PrivacySettingValue privacyValue) {
        if (privacyValue != PrivacySettingValue.CONTACTS_EXCEPT) {
            return List.of();
        }

        if(privacyEntry == null) {
            return List.of();
        }

        var newValues = new ArrayList<>(privacyEntry.excluded());
        node.streamChildren("user").forEachOrdered(entry -> {
            var jid = entry.getRequiredAttributeAsJid("value");
            if (entry.hasAttribute("action", "add")) {
                newValues.add(jid);
            }else {
                newValues.remove(jid);
            }
        });
        return newValues;
    }


    private void updateUserDisappearingMode(Node child) {
        var duration = Math.toIntExact(child.getRequiredAttributeAsLong("duration"));
        var timer = ChatEphemeralTimer.of(duration);
        whatsapp.store().setNewChatsEphemeralTimer(timer);
    }

    private void handleServerSyncNotification(Node node) {
        var patches = node.streamChildren("collection")
                .map(entry -> entry.getRequiredAttributeAsString("name"))
                .map(PatchType::of)
                .flatMap(Optional::stream)
                .toArray(PatchType[]::new);
        whatsapp.pullWebAppState(patches);
    }


    private void handleCompanionRegistration(Node node) {
        try {
            var phoneNumber = whatsapp.store()
                    .phoneNumber()
                    .orElseThrow(() -> new IllegalArgumentException("Missing phone value"));
            var linkCodeCompanionReg = node.getChild("link_code_companion_reg")
                    .orElseThrow(() -> new NoSuchElementException("Missing link_code_companion_reg: " + node));
            var ref = linkCodeCompanionReg.getChild("link_code_pairing_ref")
                    .flatMap(Node::toContentBytes)
                    .orElseThrow(() -> new IllegalArgumentException("Missing link_code_pairing_ref: " + node));
            var primaryIdentityPublicKey = linkCodeCompanionReg.getChild("primary_identity_pub")
                    .flatMap(Node::toContentBytes)
                    .orElseThrow(() -> new IllegalArgumentException("Missing primary_identity_pub: " + node));
            var primaryEphemeralPublicKeyWrapped = linkCodeCompanionReg.getChild("link_code_pairing_wrapped_primary_ephemeral_pub")
                    .flatMap(Node::toContentBytes)
                    .orElseThrow(() -> new IllegalArgumentException("Missing link_code_pairing_wrapped_primary_ephemeral_pub: " + node));
            var codePairingPublicKey = pairingCode.decrypt(primaryEphemeralPublicKeyWrapped);
            var companionPrivateKey = whatsapp.store()
                    .companionKeyPair()
                    .orElseThrow(() -> new InternalError("No companion key pair was set"))
                    .privateKey();
            var companionSharedKey = Curve25519.sharedKey(companionPrivateKey.toEncodedPoint(), codePairingPublicKey);
            var random = SecureBytes.random(32);
            var linkCodeSalt = SecureBytes.random(32);
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
                    new GCMParameterSpec(128, SecureBytes.random(12))
            );
            var identityPublicKey = whatsapp.store().identityKeyPair().publicKey().toEncodedPoint();
            cipher.update(identityPublicKey);
            cipher.update(primaryIdentityPublicKey);
            cipher.update(random);
            var encrypted = cipher.doFinal();
            var encryptedPayload = SecureBytes.concat(linkCodeSalt, SecureBytes.random(12), encrypted);
            var identitySharedKey = Curve25519.sharedKey(whatsapp.store().identityKeyPair().privateKey().toEncodedPoint(), primaryIdentityPublicKey);
            var identityPayload = SecureBytes.concat(companionSharedKey, identitySharedKey, random);
            var companionKeyHkdf = KDF.getInstance("HKDF-SHA256");
            var companionKeyHkdfParams = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(identityPayload, "AES"))
                    .thenExpand("adv_secret".getBytes(StandardCharsets.UTF_8), 32);
            var companionKey = companionKeyHkdf.deriveData(companionKeyHkdfParams);
            var companionPublicKey = SignalIdentityPublicKey.ofDirect(companionKey);
            var companionKeyPair = new SignalIdentityKeyPair(companionPublicKey, companionPrivateKey);
            whatsapp.store().setCompanionKeyPair(companionKeyPair);
            var linkCodePairingWrappedKeyBundle = new NodeBuilder()
                    .description("link_code_pairing_wrapped_key_bundle")
                    .content(encryptedPayload)
                    .build();
            var companionIdentityPublicKey = new NodeBuilder()
                    .description("companion_identity_public")
                    .content(identityPublicKey)
                    .build();
            var linkCodePairingRef = new NodeBuilder()
                    .description("link_code_pairing_ref")
                    .content(ref)
                    .build();
            var confirmationBody = new NodeBuilder()
                    .description("link_code_companion_reg")
                    .attribute("value", phoneNumber)
                    .attribute("stage", "companion_finish")
                    .content(linkCodePairingWrappedKeyBundle, companionIdentityPublicKey, linkCodePairingRef)
                    .build();
            var confirmationRequest = new NodeBuilder()
                    .description("iq")
                    .attribute("to", JidServer.user())
                    .attribute("type", "set")
                    .attribute("xmlns", "md")
                    .content(confirmationBody);
            whatsapp.sendNode(confirmationRequest);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot encrypt companion registration", exception);
        }
    }
}