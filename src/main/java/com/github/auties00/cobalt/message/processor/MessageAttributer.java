package com.github.auties00.cobalt.message.processor;

import com.github.auties00.cobalt.model.info.ChatMessageInfo;
import com.github.auties00.cobalt.model.info.ContextInfo;
import com.github.auties00.cobalt.model.info.DeviceContextInfo;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.model.Message;
import com.github.auties00.cobalt.model.message.model.MessageStatus;
import com.github.auties00.cobalt.model.message.standard.PollCreationMessage;
import com.github.auties00.cobalt.model.message.standard.PollUpdateMessage;
import com.github.auties00.cobalt.model.message.standard.ReactionMessage;
import com.github.auties00.cobalt.model.poll.PollUpdateBuilder;
import com.github.auties00.cobalt.model.poll.PollUpdateEncryptedOptionsSpec;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.util.Clock;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class MessageAttributer {
    private final WhatsappStore store;

    public MessageAttributer(WhatsappStore store) {
        this.store = store;
    }

    public void attributeChatMessage(ChatMessageInfo info) {
        attributeMessageReceipt(info);
        var chat = store.findChatByJid(info.chatJid())
                .orElseGet(() -> store.addNewChat(info.chatJid()));
        info.setChat(chat);
        var me = store.jid().orElse(null);
        if (info.fromMe() && me != null) {
            info.key().setSenderJid(me.withoutData());
        }

        attributeSender(info, info.senderJid());
        info.message()
                .contentWithContext()
                .ifPresent(message -> {
                    message.contextInfo()
                            .ifPresent(this::attributeContext);
                    processMessageWithSecret(info, message);
                });
    }

    private void processMessageWithSecret(ChatMessageInfo info, Message message) {
        switch (message) {
            case PollCreationMessage pollCreationMessage -> handlePollCreation(info, pollCreationMessage);
            case PollUpdateMessage pollUpdateMessage -> handlePollUpdate(info, pollUpdateMessage);
            case ReactionMessage reactionMessage -> handleReactionMessage(info, reactionMessage);
            default -> {}
        }
    }

    private void handlePollCreation(ChatMessageInfo info, PollCreationMessage pollCreationMessage) {
        if (pollCreationMessage.encryptionKey().isPresent()) {
            return;
        }

        info.message()
                .deviceInfo()
                .flatMap(DeviceContextInfo::messageSecret)
                .or(info::messageSecret)
                .ifPresent(pollCreationMessage::setEncryptionKey);
    }

    private void handlePollUpdate(ChatMessageInfo info, PollUpdateMessage pollUpdateMessage) {
        try {
            var originalPollInfo = store
                    .findChatMessageByKey(pollUpdateMessage.pollCreationMessageKey());
            if (originalPollInfo.isEmpty()) {
                return;
            }

            if(!(originalPollInfo.get().message().content() instanceof PollCreationMessage originalPollMessage)) {
                return;
            }

            pollUpdateMessage.setPollCreationMessage(originalPollMessage);
            var originalPollSenderJid = originalPollInfo.get()
                    .senderJid()
                    .withoutData();
            var modificationSenderJid = info.senderJid().withoutData();
            pollUpdateMessage.setVoter(modificationSenderJid);
            var originalPollId = originalPollInfo.get().id();
            var useSecretPayload = originalPollId + originalPollSenderJid + modificationSenderJid + pollUpdateMessage.secretName();
            var encryptionKey = originalPollMessage.encryptionKey()
                    .orElseThrow(() -> new NoSuchElementException("Missing encryption key"));
            var hkdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(encryptionKey, "AES"))
                    .thenExpand(useSecretPayload.getBytes(), 32);
            var useCaseSecret = hkdf.deriveData(params);
            var additionalData = "%s\0%s".formatted(
                    originalPollId,
                    modificationSenderJid
            );
            var metadata = pollUpdateMessage.encryptedMetadata()
                    .orElseThrow(() -> new NoSuchElementException("Missing encrypted metadata"));
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(useCaseSecret, "AES"),
                    new GCMParameterSpec(128, metadata.iv())
            );
            cipher.updateAAD(additionalData.getBytes(StandardCharsets.UTF_8));
            var decrypted = cipher.doFinal(metadata.payload());
            var pollVoteMessage = PollUpdateEncryptedOptionsSpec.decode(decrypted);
            var selectedOptions = pollVoteMessage.selectedOptions()
                    .stream()
                    .map(sha256 -> originalPollMessage.getSelectableOption(HexFormat.of().formatHex(sha256)))
                    .flatMap(Optional::stream)
                    .toList();
            originalPollMessage.addSelectedOptions(modificationSenderJid, selectedOptions);
            pollUpdateMessage.setVotes(selectedOptions);
            var update = new PollUpdateBuilder()
                    .pollUpdateMessageKey(info.key())
                    .vote(pollVoteMessage)
                    .senderTimestampMilliseconds(Clock.nowMilliseconds())
                    .build();
            info.pollUpdates()
                    .add(update);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot decrypt poll update", exception);
        }
    }

    private void handleReactionMessage(ChatMessageInfo info, ReactionMessage reactionMessage) {
        info.setIgnore(true);
        store
                .findChatMessageByKey(reactionMessage.key())
                .ifPresent(message -> message.reactions().add(reactionMessage));
    }

    private void attributeSender(ChatMessageInfo info, Jid senderJid) {
        if (senderJid.server() != JidServer.user() && senderJid.server() != JidServer.legacyUser()) {
            return;
        }

        store.findContactByJid(senderJid)
                .ifPresent(info::setSender);
    }

    private void attributeContext(ContextInfo contextInfo) {
        contextInfo.quotedMessageSenderJid().ifPresent(senderJid -> attributeContextSender(contextInfo, senderJid));
        contextInfo.quotedMessageParentJid().ifPresent(parentJid -> attributeContextParent(contextInfo, parentJid));
    }

    private void attributeContextParent(ContextInfo contextInfo, Jid parentJid) {
        if(parentJid.hasServer(JidServer.newsletter())) {
            var newsletter = store
                    .findNewsletterByJid(parentJid)
                    .orElseGet(() -> store.addNewNewsletter(parentJid));
            contextInfo.setQuotedMessageParent(newsletter);
        }else {
            var chat = store
                    .findChatByJid(parentJid)
                    .orElseGet(() -> store.addNewChat(parentJid));
            contextInfo.setQuotedMessageParent(chat);
        }
    }

    private void attributeContextSender(ContextInfo contextInfo, Jid senderJid) {
        store.findContactByJid(senderJid)
                .ifPresent(contextInfo::setQuotedMessageSender);
    }

    private void attributeMessageReceipt(ChatMessageInfo info) {
        var self = store.jid()
                .map(Jid::withoutData)
                .orElse(null);
        if (!info.fromMe() || (self != null && !info.chatJid().equals(self))) {
            return;
        }
        info.receipt().setReadTimestampSeconds(info.timestampSeconds().orElse(0L));
        info.receipt().addDeliveredJid(self);
        info.receipt().addReadJid(self);
        info.setStatus(MessageStatus.READ);
    }
}
