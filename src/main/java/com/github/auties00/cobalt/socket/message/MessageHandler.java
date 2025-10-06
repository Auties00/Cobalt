package com.github.auties00.cobalt.socket.message;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.model.contact.Contact;
import com.github.auties00.cobalt.model.contact.ContactStatus;
import com.github.auties00.cobalt.model.info.ChatMessageInfo;
import com.github.auties00.cobalt.model.info.ContextInfo;
import com.github.auties00.cobalt.model.info.DeviceContextInfo;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.message.button.*;
import com.github.auties00.cobalt.model.message.model.Message;
import com.github.auties00.cobalt.model.message.model.MessageStatus;
import com.github.auties00.cobalt.model.message.payment.PaymentOrderMessage;
import com.github.auties00.cobalt.model.message.standard.*;
import com.github.auties00.cobalt.model.poll.PollUpdateBuilder;
import com.github.auties00.cobalt.model.poll.PollUpdateEncryptedOptionsSpec;
import com.github.auties00.cobalt.util.Clock;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.ZonedDateTime;
import java.util.*;

public abstract sealed class MessageHandler
    extends AbstractHandler
        permits MessageSerializerHandler, MessageDeserializerHandler {
    static final String SKMSG = "skmsg";
    static final String PKMSG = "pkmsg";
    static final String MSG = "msg";
    static final String MSMG = "msmsg";
    
    MessageHandler(Whatsapp whatsapp) {
        super(whatsapp);
    }

    void attributeMessageReceipt(ChatMessageInfo info) {
        var self = whatsapp.store()
                .jid()
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

    void saveMessage(ChatMessageInfo info, boolean notify) {
        if (Jid.statusBroadcastAccount().equals(info.chatJid())) {
            whatsapp.store().addStatus(info);
            for (var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onNewStatus(info));
                Thread.startVirtualThread(() -> listener.onNewStatus(whatsapp, info));
            }
            return;
        }

        if (info.message().hasCategory(Message.Category.SERVER)) {
            return;
        }

        var chat = info.chat()
                .orElseGet(() -> whatsapp.store().addNewChat(info.chatJid()));
        var result = chat.addNewMessage(info);
        if (!result || info.timestampSeconds().orElse(0L) <= whatsapp.store().initializationTimeStamp()) {
            return;
        }
        if (chat.archived() && whatsapp.store().unarchiveChats()) {
            chat.setArchived(false);
        }
        info.sender()
                .filter(this::isTyping)
                .ifPresent(sender -> {
                    var contact = whatsapp.store()
                            .findContactByJid(sender);
                    if (contact.isPresent()) {
                        contact.get().setLastKnownPresence(ContactStatus.AVAILABLE);
                        contact.get().setLastSeen(ZonedDateTime.now());
                    }

                    var provider = contact.orElse(sender);
                    chat.addPresence(sender, ContactStatus.AVAILABLE);
                    for (var listener : whatsapp.store().listeners()) {
                        Thread.startVirtualThread(() -> listener.onContactPresence(chat, provider));
                        Thread.startVirtualThread(() -> listener.onContactPresence(whatsapp, chat, provider));
                    }
                });
        if (!info.ignore() && !info.fromMe()) {
            chat.setUnreadMessagesCount(chat.unreadMessagesCount() + 1);
        }

        if (notify) {
            for (var listener : whatsapp.store().listeners()) {
                Thread.startVirtualThread(() -> listener.onNewMessage(info));
                Thread.startVirtualThread(() -> listener.onNewMessage(whatsapp, info));
            }
        }
    }

    private boolean isTyping(Contact sender) {
        return sender.lastKnownPresence() == ContactStatus.COMPOSING
                || sender.lastKnownPresence() == ContactStatus.RECORDING;
    }

    void attributeChatMessage(ChatMessageInfo info) {
        var chat = whatsapp.store().findChatByJid(info.chatJid())
                .orElseGet(() -> whatsapp.store().addNewChat(info.chatJid()));
        info.setChat(chat);
        var me = whatsapp.store().jid().orElse(null);
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
            var originalPollInfo = whatsapp.store().findMessageByKey(pollUpdateMessage.pollCreationMessageKey());
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
        whatsapp.store().findMessageByKey(reactionMessage.key())
                .ifPresent(message -> message.reactions().add(reactionMessage));
    }

    private void attributeSender(ChatMessageInfo info, Jid senderJid) {
        if (senderJid.server() != JidServer.user() && senderJid.server() != JidServer.legacyUser()) {
            return;
        }

        var contact = whatsapp.store().findContactByJid(senderJid)
                .orElseGet(() -> whatsapp.store().addContact(senderJid));
        info.setSender(contact);
    }

    private void attributeContext(ContextInfo contextInfo) {
        contextInfo.quotedMessageSenderJid().ifPresent(senderJid -> attributeContextSender(contextInfo, senderJid));
        contextInfo.quotedMessageParentJid().ifPresent(parentJid -> attributeContextParent(contextInfo, parentJid));
    }

    private void attributeContextParent(ContextInfo contextInfo, Jid parentJid) {
        if(parentJid.hasServer(JidServer.newsletter())) {
            var newsletter = whatsapp.store()
                    .findNewsletterByJid(parentJid)
                    .orElseGet(() -> whatsapp.store().addNewNewsletter(parentJid));
            contextInfo.setQuotedMessageParent(newsletter);
        }else {
            var chat = whatsapp.store()
                    .findChatByJid(parentJid)
                    .orElseGet(() -> whatsapp.store().addNewChat(parentJid));
            contextInfo.setQuotedMessageParent(chat);
        }
    }

    private void attributeContextSender(ContextInfo contextInfo, Jid senderJid) {
        var contact = whatsapp.store().findContactByJid(senderJid)
                .orElseGet(() -> whatsapp.store().addContact(senderJid));
        contextInfo.setQuotedMessageSender(contact);
    }


    String getMediaType(Message content) {
        return switch (content) {
            case ImageMessage ignored -> "image";
            case VideoOrGifMessage videoMessage -> videoMessage.gifPlayback() ? "gif" : "video";
            case AudioMessage audioMessage -> audioMessage.voiceMessage() ? "ptt" : "audio";
            case ContactMessage ignored -> "vcard";
            case DocumentMessage ignored -> "document";
            case ContactsMessage ignored -> "contact_array";
            case LiveLocationMessage ignored -> "livelocation";
            case StickerMessage ignored -> "sticker";
            case ListMessage ignored -> "list";
            case ListResponseMessage ignored -> "list_response";
            case ButtonsResponseMessage ignored -> "buttons_response";
            case PaymentOrderMessage ignored -> "order";
            case ProductMessage ignored -> "product";
            case NativeFlowResponseMessage ignored -> "native_flow_response";
            case ButtonsMessage buttonsMessage ->
                    buttonsMessage.headerType().hasMedia() ? buttonsMessage.headerType().name().toLowerCase() : null;
            case null, default -> null;
        };
    }

    @SafeVarargs
    final <T> List<T> toSingleList(List<T>... all) {
        return switch (all.length) {
            case 0 -> List.of();
            case 1 -> all[0];
            default -> {
                var length = 0;
                for (var entry : all) {
                    if (entry != null) {
                        length += entry.size();
                    }
                }
                var result = new ArrayList<T>(length);
                for (List<T> entry : all) {
                    if (entry != null) {
                        result.addAll(entry);
                    }
                }
                yield result;
            }
        };
    }
}
