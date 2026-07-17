package com.github.auties00.cobalt.message.addon;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.poll.PollEncValue;
import com.github.auties00.cobalt.wire.linked.message.poll.PollEncValueBuilder;
import com.github.auties00.cobalt.wire.linked.message.poll.PollVoteMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.poll.PollVoteMessageSpec;
import com.github.auties00.cobalt.wire.linked.message.security.EncCommentMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.security.EncCommentMessage;
import com.github.auties00.cobalt.wire.linked.message.security.EncReactionMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.security.EncReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.text.CommentMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessageSpec;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Wraps plaintext comments, reactions, and poll votes into the
 * encrypted-addon wire form WhatsApp uses inside community and announcement
 * group threads.
 *
 * <p>Each addon ships as an outer Signal envelope plus an inner AES-GCM
 * ciphertext bound to the parent message's {@code messageSecret}, so the
 * server can route the addon without reading its body. Each factory method
 * produces the inner ciphertext through {@link MessageAddonEncryption#encrypt(
 * byte[], byte[], String, Jid, Jid, MessageAddonType)} and packages the
 * resulting bytes into the matching protobuf model
 * ({@link EncCommentMessage}, {@link EncReactionMessage}, or
 * {@link PollEncValue}).
 *
 * @implNote This implementation collapses WA Web's three separate per-addon
 * wrappers ({@code WAWebCommentUtils.encryptComment},
 * {@code WAWebReactionEncryptMsgData.encryptReaction},
 * {@code WAWebPollsVoteEncryption.encryptVote}) into one factory because the
 * only shared work, the sender resolution and the
 * {@link MessageAddonEncryption} call, is identical across all three paths.
 */
@WhatsAppWebModule(moduleName = "WAWebAddonEncryption")
public final class EncMessageFactory {
    /** The logger for {@link EncMessageFactory}. */
    private static final System.Logger LOGGER = Log.get(EncMessageFactory.class);

    /**
     * Prevents instantiation of this static factory.
     *
     * @throws AssertionError always
     */
    private EncMessageFactory() {
        throw new AssertionError();
    }

    /**
     * Encrypts a plaintext {@link CommentMessage} into an
     * {@link EncCommentMessage} ready to ride as an addon on the parent
     * stanza.
     *
     * <p>The comment's inner
     * {@link com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer} is
     * serialised via {@link LinkedMessageContainerSpec#encode(
     * com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer)} and then
     * dual-encrypted under {@link MessageAddonType#ENC_COMMENT} so the server
     * can route the addon without reading the comment body. The sender bound
     * into the key derivation is resolved through
     * {@link #resolveOriginalSender(MessageKey, Jid, Jid)}, and the
     * {@code targetMessageKey} of the input is propagated as-is onto the
     * result.
     *
     * @param comment       the plaintext comment to encrypt
     * @param parentMessage the message the comment is attached to
     * @param selfJid       the current user's JID, used when the parent was
     *                      authored by the current user
     * @return the encrypted comment wrapper, ready to be added to the
     *         outbound stanza
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the parent carries no
     *                                  {@code messageSecret}, the parent key
     *                                  lacks an id or parent JID, or the
     *                                  comment has no inner message
     */
    @WhatsAppWebExport(moduleName = "WAWebAddonEncryption", exports = "encryptAddOn",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static EncCommentMessage encryptComment(CommentMessage comment, ChatMessageInfo parentMessage, Jid selfJid) {
        Objects.requireNonNull(comment, "comment cannot be null");
        Objects.requireNonNull(parentMessage, "parentMessage cannot be null");
        Objects.requireNonNull(selfJid, "selfJid cannot be null");

        var parentSecret = parentMessage.messageSecret()
                .orElseThrow(() -> new IllegalArgumentException("Parent message has no messageSecret"));

        var parentKey = parentMessage.key();
        var parentKeyId = parentKey.id()
                .orElseThrow(() -> new IllegalArgumentException("Parent key has no keyId"));
        var parentKeyJid = parentKey.parentJid()
                .orElseThrow(() -> new IllegalArgumentException("Parent key has no parentJid"));

        var originalSender = resolveOriginalSender(parentKey, parentKeyJid, selfJid);

        var commentContent = comment.message()
                .orElseThrow(() -> new IllegalArgumentException("Comment has no message content"));
        var plaintext = LinkedMessageContainerSpec.encode(commentContent);

        var encrypted = MessageAddonEncryption.encrypt(
                plaintext, parentSecret, parentKeyId,
                originalSender, selfJid.toUserJid(),
                MessageAddonType.ENC_COMMENT);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "encrypted comment addon for parent {0}, bytes={1}",
                    parentKeyId, encrypted.ciphertext().length);
        }
        return new EncCommentMessageBuilder()
                .targetMessageKey(comment.targetMessageKey().orElse(null))
                .encPayload(encrypted.ciphertext())
                .encIv(encrypted.iv())
                .build();
    }

    /**
     * Encrypts a plaintext {@link ReactionMessage} into an
     * {@link EncReactionMessage} ready to ride as an addon on the parent
     * stanza.
     *
     * <p>The reaction is encrypted in community and announcement group
     * threads, where the default non-encrypted reaction wire format would leak
     * the emoji content to the server. The plaintext fed to the cipher is the
     * protobuf encoding of the reaction itself (not an inner container),
     * produced with {@link ReactionMessageSpec#encode(ReactionMessage)}, and
     * is dual-encrypted under {@link MessageAddonType#ENC_REACTION}; the
     * sender bound into the key derivation is resolved through
     * {@link #resolveOriginalSender(MessageKey, Jid, Jid)}. The
     * {@code targetMessageKey} on the input (the message being reacted to) is
     * propagated as the {@code targetMessageKey} on the resulting wrapper.
     *
     * @param reaction      the plaintext reaction to encrypt
     * @param parentMessage the message the reaction is attached to
     * @param selfJid       the current user's JID, used when the parent was
     *                      authored by the current user
     * @return the encrypted reaction wrapper, ready to be added to the
     *         outbound stanza
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if the parent carries no
     *                                  {@code messageSecret} or the parent
     *                                  key lacks an id or parent JID
     */
    @WhatsAppWebExport(moduleName = "WAWebAddonEncryption", exports = "encryptAddOn",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static EncReactionMessage encryptReaction(ReactionMessage reaction, ChatMessageInfo parentMessage, Jid selfJid) {
        Objects.requireNonNull(reaction, "reaction cannot be null");
        Objects.requireNonNull(parentMessage, "parentMessage cannot be null");
        Objects.requireNonNull(selfJid, "selfJid cannot be null");

        var parentSecret = parentMessage.messageSecret()
                .orElseThrow(() -> new IllegalArgumentException("Parent message has no messageSecret"));

        var parentKey = parentMessage.key();
        var parentKeyId = parentKey.id()
                .orElseThrow(() -> new IllegalArgumentException("Parent key has no keyId"));
        var parentKeyJid = parentKey.parentJid()
                .orElseThrow(() -> new IllegalArgumentException("Parent key has no parentJid"));

        var originalSender = resolveOriginalSender(parentKey, parentKeyJid, selfJid);

        var plaintext = ReactionMessageSpec.encode(reaction);

        var encrypted = MessageAddonEncryption.encrypt(
                plaintext, parentSecret, parentKeyId,
                originalSender, selfJid.toUserJid(),
                MessageAddonType.ENC_REACTION);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "encrypted reaction addon for parent {0}, bytes={1}",
                    parentKeyId, encrypted.ciphertext().length);
        }
        return new EncReactionMessageBuilder()
                .targetMessageKey(reaction.key().orElse(null))
                .encPayload(encrypted.ciphertext())
                .encIv(encrypted.iv())
                .build();
    }

    /**
     * Encrypts the voter's selected option labels into a {@link PollEncValue}
     * ready to embed in an outgoing
     * {@link com.github.auties00.cobalt.wire.linked.message.poll.PollUpdateMessage}.
     *
     * <p>Each label is SHA-256-hashed to its canonical 32-byte option digest,
     * the digests are wrapped in a
     * {@link com.github.auties00.cobalt.wire.linked.message.poll.PollVoteMessage},
     * the protobuf is serialised, and the bytes are encrypted under
     * {@link MessageAddonType#POLL_VOTE} (an AAD-bound use case) so the server
     * cannot rebind a vote from one user to another. The sender bound into the
     * key derivation is resolved through
     * {@link #resolveOriginalSender(MessageKey, Jid, Jid)}.
     *
     * @implNote This implementation stores the raw 32-byte digest rather than
     * the hex-encoded form; that matches WA Web's
     * {@code WAWebPollsCreateOptionLocalIdMap.getLocalIdForHash}, which accepts
     * the raw buffer.
     *
     * @param selectedOptions the option labels the voter chose, in any order;
     *                        list entries must not be {@code null}
     * @param pollCreation    the poll-creation message the vote refers to
     * @param voterJid        the JID of the user casting the vote
     * @return the {@link PollEncValue} containing the ciphertext and IV
     * @throws NullPointerException     if any argument is {@code null}, or if
     *                                  {@code selectedOptions} contains a
     *                                  {@code null} entry
     * @throws IllegalArgumentException if {@code pollCreation} carries no
     *                                  {@code messageSecret} or its key lacks
     *                                  an id or parent JID
     */
    @WhatsAppWebExport(moduleName = "WAWebPollsVoteEncryption", exports = "encryptVote",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebPollOptionHashUtils", exports = "getHashBufferForString",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static PollEncValue encryptPollVote(List<String> selectedOptions, ChatMessageInfo pollCreation, Jid voterJid) {
        Objects.requireNonNull(selectedOptions, "selectedOptions cannot be null");
        Objects.requireNonNull(pollCreation, "pollCreation cannot be null");
        Objects.requireNonNull(voterJid, "voterJid cannot be null");

        var pollSecret = pollCreation.messageSecret()
                .orElseThrow(() -> new IllegalArgumentException("Poll creation has no messageSecret"));

        var pollKey = pollCreation.key();
        var pollKeyId = pollKey.id()
                .orElseThrow(() -> new IllegalArgumentException("Poll creation key has no id"));
        var pollKeyJid = pollKey.parentJid()
                .orElseThrow(() -> new IllegalArgumentException("Poll creation key has no parentJid"));

        var originalSender = resolveOriginalSender(pollKey, pollKeyJid, voterJid);

        var optionHashes = new ArrayList<byte[]>(selectedOptions.size());
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (var option : selectedOptions) {
                Objects.requireNonNull(option, "selectedOptions cannot contain null entries");
                digest.reset();
                optionHashes.add(digest.digest(option.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (NoSuchAlgorithmException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "poll vote encryption failed, sha-256 unavailable", e);
            throw new RuntimeException("SHA-256 not available", e);
        }

        var voteMessage = new PollVoteMessageBuilder()
                .selectedOptions(optionHashes)
                .build();
        var plaintext = PollVoteMessageSpec.encode(voteMessage);

        var encrypted = MessageAddonEncryption.encrypt(
                plaintext, pollSecret, pollKeyId,
                originalSender, voterJid.toUserJid(),
                MessageAddonType.POLL_VOTE);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "encrypted poll vote for poll {0}, options={1}",
                    pollKeyId, optionHashes.size());
        }
        return new PollEncValueBuilder()
                .encPayload(encrypted.ciphertext())
                .encIv(encrypted.iv())
                .build();
    }

    /**
     * Decrypts a poll vote produced by {@link #encryptPollVote(List, ChatMessageInfo, Jid)}.
     *
     * <p>Re-derives the per-vote key from the poll-creation message's
     * {@code messageSecret}, the poll key id, and the resolved original sender,
     * then decrypts {@code vote} and decodes the inner {@code PollVoteMessage}.
     * The returned list holds the SHA-256 hashes of the options the voter
     * selected; callers map those hashes back to option labels by hashing the
     * poll-creation message's option names.
     *
     * @param vote         the encrypted vote received from the voter
     * @param pollCreation the poll-creation message the vote refers to
     * @param voterJid     the JID of the user that cast the vote
     * @return the SHA-256 hashes of the selected options
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code pollCreation} carries no
     *                                  {@code messageSecret}, its key lacks an id
     *                                  or parent JID, or {@code vote} carries no
     *                                  payload or IV
     * @throws RuntimeException         if the underlying cipher rejects the
     *                                  authentication tag (for example on a
     *                                  sender JID-form mismatch)
     */
    @WhatsAppWebExport(moduleName = "WAWebPollsVoteDecryption", exports = "decryptVote",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static List<byte[]> decryptPollVote(PollEncValue vote, ChatMessageInfo pollCreation, Jid voterJid) {
        Objects.requireNonNull(vote, "vote cannot be null");
        Objects.requireNonNull(pollCreation, "pollCreation cannot be null");
        Objects.requireNonNull(voterJid, "voterJid cannot be null");

        var pollSecret = pollCreation.messageSecret()
                .orElseThrow(() -> new IllegalArgumentException("Poll creation has no messageSecret"));

        var pollKey = pollCreation.key();
        var pollKeyId = pollKey.id()
                .orElseThrow(() -> new IllegalArgumentException("Poll creation key has no id"));
        var pollKeyJid = pollKey.parentJid()
                .orElseThrow(() -> new IllegalArgumentException("Poll creation key has no parentJid"));

        var originalSender = resolveOriginalSender(pollKey, pollKeyJid, voterJid);

        var ciphertext = vote.encPayload()
                .orElseThrow(() -> new IllegalArgumentException("Poll vote has no encrypted payload"));
        var iv = vote.encIv()
                .orElseThrow(() -> new IllegalArgumentException("Poll vote has no encryption IV"));

        var plaintext = MessageAddonEncryption.decrypt(
                new MessageEncryptedAddon(ciphertext, iv), pollSecret, pollKeyId,
                originalSender, voterJid.toUserJid(),
                MessageAddonType.POLL_VOTE);

        var selectedOptions = PollVoteMessageSpec.decode(plaintext).selectedOptions();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "decrypted poll vote for poll {0}, options={1}",
                    pollKeyId, selectedOptions.size());
        }
        return selectedOptions;
    }

    /**
     * Resolves the original-sender JID that the addon HKDF info parameter is
     * bound to.
     *
     * <p>When the parent message was authored by the current user
     * ({@link MessageKey#fromMe()} is {@code true}) the self JID is used;
     * otherwise the parent key's explicit {@link MessageKey#senderJid()} wins,
     * falling back to the parent's chat JID when no sender is set. The return
     * value is forced to user form via {@link Jid#toUserJid()} so device
     * suffixes do not leak into the HKDF input.
     *
     * @param parentKey    the parent message's key
     * @param parentKeyJid the parent key's chat JID
     * @param selfJid      the current user's JID
     * @return the resolved original sender, in user form
     */
    private static Jid resolveOriginalSender(
            MessageKey parentKey,
            Jid parentKeyJid,
            Jid selfJid
    ) {
        Jid originalSender;
        if (parentKey.fromMe()) {
            originalSender = selfJid;
        } else {
            originalSender = parentKey.senderJid().orElse(parentKeyJid);
        }
        return originalSender.toUserJid();
    }
}
