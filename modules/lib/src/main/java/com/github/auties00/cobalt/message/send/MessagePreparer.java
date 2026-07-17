package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.message.addon.EncMessageFactory;
import com.github.auties00.cobalt.message.send.id.MessageIdGenerator;
import com.github.auties00.cobalt.message.send.id.MessageIdVersion;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfoBuilder;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.message.*;
import com.github.auties00.cobalt.wire.core.message.*;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.linked.message.event.EncEventResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollCreationMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollUpdateMessage;
import com.github.auties00.cobalt.wire.linked.message.security.*;
import com.github.auties00.cobalt.wire.linked.message.text.CommentMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfoBuilder;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.ProtobufValidationErrorEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ProtobufValidationFlow;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Converts a raw {@link LinkedMessageContainer} into a fully-populated
 * {@link LinkedMessageInfo} ready for dispatch.
 *
 * <p>The preparer generates the wire id and a 32-byte per-message secret,
 * stamps the secret onto both the resulting info and the container's
 * {@code messageContextInfo} so the encryption stage can read it back, and
 * auto-promotes a {@link ReactionMessage} or {@link CommentMessage} into the
 * corresponding {@code Enc*} addon variant when the chat is a CAG default
 * subgroup. ICDC metadata is populated later by the per-device encryption
 * stage; random padding is added at the Signal binary level by
 * {@link com.github.auties00.cobalt.message.send.crypto.MessageEncryption}.
 */
@WhatsAppWebModule(moduleName = "WAWebOutgoingMessage")
@WhatsAppWebModule(moduleName = "WAWebE2EProtoGenerator")
@WhatsAppWebModule(moduleName = "WAWebAddonEncryptAddonMsgData")
final class MessagePreparer {
    /**
     * The logger for {@link MessagePreparer}.
     */
    private static final System.Logger LOGGER = Log.get(MessagePreparer.class);

    /**
     * Defines the byte length of the per-message {@code messageSecret}
     * generated for every outbound chat message.
     *
     * <p>The receiver asserts the same 32-byte size via WA Web's
     * {@code getValidatedMessageSecret}; a mismatch causes the receiver to
     * reject the message with the
     * {@link com.github.auties00.cobalt.ack.NackReason#MISSING_MESSAGE_SECRET}
     * nack.
     */
    @WhatsAppWebExport(moduleName = "WAWebAddonEncryptionError", exports = "getValidatedMessageSecret",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int MESSAGE_SECRET_SIZE = 32;

    /**
     * Caps how deep the outgoing {@code messageSecret}-location walk descends
     * before it gives up and treats the tree as clean.
     *
     * <p>Guards against pathological or maliciously deep reply chains: once the
     * walk reaches this nesting depth it stops recursing, logs a diagnostic, and
     * reports no violation rather than risking unbounded recursion.
     *
     * @implNote This implementation reuses WA Web's
     * {@code WAWebMessageSecretLocationUtils} depth ceiling of {@code 15}.
     */
    private static final int MESSAGE_SECRET_MAX_DEPTH = 15;

    /**
     * Identifies the validation rule that flags a stray {@code messageSecret}
     * nested below the top level of an outgoing message.
     *
     * <p>Recorded verbatim as the {@code protobufValidationRuleId} of the
     * emitted {@link ProtobufValidationErrorEventBuilder} so the telemetry
     * pipeline can attribute the violation to the outgoing-message rule rather
     * than to the group-history variant.
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageSecretLocationUtils", exports = "MESSAGE_SECRET_RULE_ID_OUTGOING",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final String MESSAGE_SECRET_RULE_ID_OUTGOING = "79";

    /**
     * Supplies self-JID resolution, newsletter lookups, chat metadata, and
     * parent-message resolution during addon promotion.
     */
    private final LinkedWhatsAppStore store;

    /**
     * Records the {@code ProtobufValidationError} telemetry emitted when an
     * outgoing message carries a {@code messageSecret} nested below its top
     * level.
     */
    private final WamService wamService;

    /**
     * Constructs a {@link MessagePreparer} bound to the supplied store.
     *
     * <p>Constructed once by {@link MessageSendingService}; embedders should not
     * instantiate directly.
     *
     * @param store      the {@link LinkedWhatsAppStore} providing JID and metadata lookups
     * @param wamService the {@link WamService} that records the outgoing
     *                   {@code messageSecret}-location validation error
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebE2EProtoGenerator", exports = "getProtobufMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    MessagePreparer(LinkedWhatsAppStore store, WamService wamService) {
        this.store = Objects.requireNonNull(store, "store");
        this.wamService = Objects.requireNonNull(wamService, "wamService");
    }

    /**
     * Builds a fully-populated {@link ChatMessageInfo} for the supplied chat
     * {@link Jid} and {@link LinkedMessageContainer}.
     *
     * <p>Generates a {@link MessageIdVersion#V2} wire id, samples a fresh
     * 32-byte {@code messageSecret}, runs the addon validation and
     * auto-promotion pipeline, and stamps the secret onto both the info and the
     * inner container's {@code messageContextInfo}. The {@code broadcast} flag
     * is set when the chat is the status broadcast account.
     *
     * @param chatJid   the recipient chat {@link Jid}
     * @param container the raw {@link LinkedMessageContainer}
     * @return the prepared {@link ChatMessageInfo}
     * @throws NullPointerException  if any argument is {@code null}
     * @throws IllegalStateException if the client is not logged in
     */
    @WhatsAppWebExport(moduleName = "WAWebOutgoingMessage", exports = "createOutgoingMessageProtobuf",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebOutgoingMessage", exports = "createOutgoingMsgModelProtobuf",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebE2EProtoGenerator", exports = "getProtobufMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    ChatMessageInfo prepareChat(Jid chatJid, LinkedMessageContainer container) {
        Objects.requireNonNull(chatJid, "chatJid");
        Objects.requireNonNull(container, "container");

        var localJid = store.accountStore().jid()
                .orElseThrow(() -> new IllegalStateException("Not logged in"));
        var messageId = MessageIdGenerator.generate(MessageIdVersion.V2, localJid);
        var timestamp = Instant.now();

        var messageSecret = DataUtils.randomByteArray(MESSAGE_SECRET_SIZE);

        var preparedContainer = prepareAddonContent(container, chatJid, localJid, messageSecret);

        var deviceInfo = new ChatMessageContextInfoBuilder()
                .messageSecret(messageSecret)
                .build();
        preparedContainer = preparedContainer.withMessageContextInfo(deviceInfo);

        verifyTopLevelMessageSecret(preparedContainer);

        var key = new MessageKeyBuilder()
                .id(messageId)
                .parentJid(chatJid)
                .fromMe(true)
                .senderJid(localJid)
                .build();

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "prepared chat message {0} for {1}", messageId, chatJid);
        }

        return new ChatMessageInfoBuilder()
                .status(MessageStatus.PENDING)
                .senderJid(localJid)
                .key(key)
                .message(preparedContainer)
                .timestamp(timestamp)
                .broadcast(chatJid.hasServer(JidServer.broadcast()))
                .messageSecret(messageSecret)
                .build();
    }

    /**
     * Builds a fully-populated {@link NewsletterMessageInfo} for the supplied
     * newsletter {@link Jid} and {@link LinkedMessageContainer}.
     *
     * <p>Newsletter sends are plaintext SMAX publishes so no
     * {@code messageSecret} is generated and no addon stage runs; the only
     * extras over a chat prepare are looking up the newsletter to derive the
     * monotonically-increasing {@code serverId} and the membership
     * precondition.
     *
     * @param newsletterJid the newsletter {@link Jid}
     * @param container     the raw {@link LinkedMessageContainer}
     * @return the prepared {@link NewsletterMessageInfo}
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalStateException    if the client is not logged in
     * @throws IllegalArgumentException if the user has not joined the newsletter
     */
    @WhatsAppWebExport(moduleName = "WAWebNewsletterSendMessageQueryJob", exports = "querySendNewsletterMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    NewsletterMessageInfo prepareNewsletter(Jid newsletterJid, LinkedMessageContainer container) {
        Objects.requireNonNull(newsletterJid, "newsletterJid");
        Objects.requireNonNull(container, "container");

        var localJid = store.accountStore().jid()
                .orElseThrow(() -> new IllegalStateException("Not logged in"));
        var newsletter = store.chatStore().findNewsletterByJid(newsletterJid)
                .orElseThrow(() -> {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "send rejected: newsletter {0} not joined", newsletterJid);
                    }
                    return new IllegalArgumentException(
                            "Cannot send to a newsletter that you didn't join: " + newsletterJid);
                });
        var oldServerId = newsletter.newestMessage()
                .map(NewsletterMessageInfo::serverId)
                .orElse(0);
        var key = new MessageKeyBuilder()
                .id(MessageIdGenerator.generate(MessageIdVersion.V2, localJid))
                .parentJid(newsletterJid)
                .fromMe(true)
                .build();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "prepared newsletter message for {0}, serverId={1}", newsletterJid, oldServerId + 1);
        }
        return new NewsletterMessageInfoBuilder()
                .key(key)
                .serverId(oldServerId + 1)
                .timestamp(Instant.now())
                .message(container)
                .status(MessageStatus.PENDING)
                .build();
    }

    /**
     * Validates the addon-encryption state of the container and auto-promotes a
     * {@link ReactionMessage} or {@link CommentMessage} to its {@code Enc*}
     * variant when the target is a CAG default subgroup.
     *
     * <p>Plain-content payloads pass through unchanged. CAG reactions and
     * comments are promoted via {@link EncMessageFactory} using the resolved
     * parent message; payloads already in {@code Enc*} form (or
     * {@link SecretEncMessage}) must carry both their {@code encPayload} and
     * {@code encIv}, otherwise the call fails fast.
     *
     * @param container      the raw {@link LinkedMessageContainer}
     * @param chatJid        the target chat {@link Jid}
     * @param selfJid        the sender's own {@link Jid}
     * @param messageSecret  the per-message secret stamped on the outgoing
     *                       message, adopted as a {@link PollCreationMessage}'s
     *                       {@code encKey} when none was supplied
     * @return the container, possibly with addon content converted in place
     * @throws IllegalArgumentException if a poll vote carries neither an
     *                                  encrypted payload nor raw selected
     *                                  options, its referenced poll-creation
     *                                  message cannot be resolved, an
     *                                  {@code Enc*}-typed addon is missing its
     *                                  payload or IV, or a CAG addon cannot
     *                                  resolve its parent message
     */
    @WhatsAppWebExport(moduleName = "WAWebAddonEncryptAddonMsgData", exports = "encryptAddOn",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebAddonEncryptAddonMsgData", exports = "createDualEncryptionHelper",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebPollsSendPollCreationMsgAction", exports = "createPollCreationMsgData",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebPollsSendVoteMsgAction", exports = "sendVote",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private LinkedMessageContainer prepareAddonContent(
            LinkedMessageContainer container,
            Jid chatJid,
            Jid selfJid,
            byte[] messageSecret
    ) {

        return switch (container.content()) {
            case PollCreationMessage poll when poll.encKey().isEmpty() -> {
                // The poll's vote-encryption key is the message secret recipients derive votes against.
                poll.setEncKey(messageSecret);
                yield container;
            }

            case PollUpdateMessage poll when poll.vote().isPresent() || poll.metadata().isPresent() ->
                    container;

            case PollUpdateMessage poll -> {
                var pollKey = poll.pollCreationMessageKey()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "PollUpdateMessage must carry a pollCreationMessageKey"));
                var pollCreation = resolveParentMessage(chatJid, pollKey)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Poll creation message not found in store for key " + pollKey));
                poll.setVote(EncMessageFactory.encryptPollVote(poll.selectedOptions(), pollCreation, selfJid));
                yield container;
            }

            case ReactionMessage reaction when requiresEncryptedReaction(chatJid) -> {
                var parentMessage = resolveParentMessage(chatJid, reaction.key().orElse(null));
                if (parentMessage.isEmpty()) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "cannot encrypt reaction for {0}: parent message not found", chatJid);
                    }
                    throw new IllegalArgumentException("Cannot encrypt reaction: parent message not found");
                }
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "promoting reaction to encrypted addon for {0}", chatJid);
                }
                var encrypted = EncMessageFactory.encryptReaction(reaction, parentMessage.get(), selfJid);
                yield LinkedMessageContainer.of(encrypted);
            }

            case EncReactionMessage enc -> {
                Objects.requireNonNull(enc.encPayload(),
                        "EncryptedReactionMessage must have encPayload populated");
                Objects.requireNonNull(enc.encIv(),
                        "EncryptedReactionMessage must have encIv populated");
                yield container;
            }

            case EncEventResponseMessage enc -> {
                Objects.requireNonNull(enc.encPayload(),
                        "EncryptedEventResponseMessage must have encPayload populated");
                Objects.requireNonNull(enc.encIv(),
                        "EncryptedEventResponseMessage must have encIv populated");
                yield container;
            }

            case SecretEncMessage enc -> {
                Objects.requireNonNull(enc.encPayload(),
                        "SecretEncryptedMessage must have encPayload populated");
                Objects.requireNonNull(enc.encIv(),
                        "SecretEncryptedMessage must have encIv populated");
                yield container;
            }

            case CommentMessage comment when requiresEncryptedReaction(chatJid) -> {
                var parentMessage = resolveParentMessage(chatJid, comment.targetMessageKey().orElse(null));
                if (parentMessage.isEmpty()) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "cannot encrypt comment for {0}: parent message not found", chatJid);
                    }
                    throw new IllegalArgumentException("Cannot encrypt comment: parent message not found");
                }
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "promoting comment to encrypted addon for {0}", chatJid);
                }
                var encrypted = EncMessageFactory.encryptComment(comment, parentMessage.get(), selfJid);
                yield LinkedMessageContainer.of(encrypted);
            }

            case EncCommentMessage enc -> {
                Objects.requireNonNull(enc.encPayload(),
                        "EncryptedCommentMessage must have encPayload populated");
                Objects.requireNonNull(enc.encIv(),
                        "EncryptedCommentMessage must have encIv populated");
                yield container;
            }

            default -> container;
        };
    }

    /**
     * Returns whether a reaction or comment to the supplied chat must be sent as
     * an encrypted addon.
     *
     * <p>Encrypted addons are required for CAG default subgroups (the same
     * branch WA Web's {@code isCagAddon} guards); standard groups, 1:1 chats,
     * and non-default community subgroups use the plain reaction payload.
     *
     * @param chatJid the target chat {@link Jid}
     * @return {@code true} when {@code chatJid} resolves to a CAG default
     *         subgroup
     */
    @WhatsAppWebExport(moduleName = "WAWebSendGroupMsgJob", exports = "isCagAddon",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean requiresEncryptedReaction(Jid chatJid) {
        if (!chatJid.hasGroupOrCommunityServer()) {
            return false;
        }

        var metadata = store.chatStore().findChatMetadata(chatJid).orElse(null);
        return metadata instanceof GroupMetadata group
                && group.isDefaultSubgroup();
    }

    /**
     * Resolves the parent chat message referenced by the given key.
     *
     * <p>The addon-promotion branches use this to fetch the target message; the
     * encryption helper needs the parent's id and sender to derive the addon's
     * symmetric key.
     *
     * @param parentJid the chat {@link Jid} containing the parent message
     * @param key       the {@link MessageKey} referencing the parent, or
     *                  {@code null}
     * @return the resolved {@link ChatMessageInfo}, or {@link Optional#empty()}
     *         when not found
     */
    @WhatsAppWebExport(moduleName = "WAWebAddonEncryptAddonMsgData", exports = "encryptAddOn",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Optional<ChatMessageInfo> resolveParentMessage(Jid parentJid, MessageKey key) {
        return key == null ? Optional.empty() : key.id()
                .flatMap(id -> store.chatStore().findMessageById(parentJid, id))
                .filter(entry -> entry instanceof ChatMessageInfo)
                .map(entry -> (ChatMessageInfo) entry);
    }

    /**
     * Verifies that the fully-prepared outgoing {@code message} carries its
     * {@code messageSecret} only at the top level and records a
     * {@code ProtobufValidationError} when it does not.
     *
     * <p>WhatsApp stamps the per-message secret onto the top-level
     * {@link LinkedMessageContainer#messageContextInfo()} exclusively; a secret found
     * on any nested submessage is a construction bug that lets the server
     * correlate the ciphertext across chats. When
     * {@link #findMessageSecretViolation(LinkedMessageContainer, int, String)}
     * reports such a nested secret, a {@link ProtobufValidationErrorEventBuilder}
     * is committed with {@code protobufValidationFlow} set to
     * {@link ProtobufValidationFlow#STANZA_MESSAGE_SEND} and the offending path;
     * a clean tree emits nothing, mirroring the sender-side branch of WA Web's
     * {@code verifyTopLevelMessageSecret}.
     *
     * @param message the prepared top-level {@link LinkedMessageContainer}
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageSecretLocationUtils", exports = "verifyTopLevelMessageSecret",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void verifyTopLevelMessageSecret(LinkedMessageContainer message) {
        findMessageSecretViolation(message, 0, "")
                .ifPresent(this::emitProtobufValidationError);
    }

    /**
     * Walks the reply chain of {@code node} looking for a submessage that
     * carries a {@code messageSecret} nested below the top level.
     *
     * <p>The top-level container (depth {@code 0}) is expected to hold the
     * secret and is never flagged; from depth {@code 1} onward any container
     * whose {@link LinkedMessageContainer#messageContextInfo()} exposes a
     * {@link ChatMessageContextInfo#messageSecret()} yields the accumulated
     * dotted {@code path} as the violation location. The walk descends through
     * the quoted-message content reachable from a payload's
     * {@link ContextInfo#quotedMessageContent()}, the nesting WA Web's
     * {@code findMessageSecretViolation} traverses when it recurses into
     * {@code contextInfo.quotedMessage}; the {@code deviceSentMessage} envelope
     * is skipped exactly as WA Web skips it. Recursion halts at
     * {@link #MESSAGE_SECRET_MAX_DEPTH}.
     *
     * @param node  the {@link LinkedMessageContainer} to inspect at this level
     * @param depth the current nesting depth, {@code 0} at the top level
     * @param path  the dotted field path traversed to reach {@code node}
     * @return the violation path when a nested secret is found, otherwise
     *         {@link Optional#empty()}
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageSecretLocationUtils", exports = "findMessageSecretViolation",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Optional<String> findMessageSecretViolation(LinkedMessageContainer node, int depth, String path) {
        if (depth >= MESSAGE_SECRET_MAX_DEPTH) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "messageSecret location check exceeded max depth at path {0}", path);
            }
            return Optional.empty();
        }

        if (depth > 0 && node.messageContextInfo()
                .flatMap(ChatMessageContextInfo::messageSecret)
                .isPresent()) {
            return Optional.of(path.isEmpty() ? "unknown" : path);
        }

        var quoted = node.contextualContent()
                .flatMap(ContextualMessage::contextInfo)
                .flatMap(ContextInfo::quotedMessageContent);
        if (quoted.isPresent()) {
            var childPath = path.isEmpty()
                    ? "contextInfo.quotedMessage"
                    : path + ".contextInfo.quotedMessage";
            return findMessageSecretViolation(quoted.get(), depth + 1, childPath);
        }

        return Optional.empty();
    }

    /**
     * Commits the {@code ProtobufValidationError} that reports a stray
     * {@code messageSecret} nested inside an outgoing message.
     *
     * <p>Populates the same fields WA Web's sender-side
     * {@code verifyTopLevelMessageSecret} sets: the drop flags are both
     * {@code false} (the message is still sent), the flow is
     * {@link ProtobufValidationFlow#STANZA_MESSAGE_SEND}, the path is the
     * offending location, and the rule id is
     * {@link #MESSAGE_SECRET_RULE_ID_OUTGOING}.
     *
     * @param violationPath the dotted path of the nested {@code messageSecret}
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageSecretLocationUtils", exports = "verifyTopLevelMessageSecret",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitProtobufValidationError(String violationPath) {
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "outgoing message carries a nested messageSecret at path {0}", violationPath);
        }
        wamService.commit(new ProtobufValidationErrorEventBuilder()
                .protobufValidationDropped(false)
                .protobufLegacyValidationDropped(false)
                .protobufValidationFlow(ProtobufValidationFlow.STANZA_MESSAGE_SEND)
                .protobufValidationPath(violationPath)
                .protobufValidationRuleId(MESSAGE_SECRET_RULE_ID_OUTGOING)
                .build());
    }
}
