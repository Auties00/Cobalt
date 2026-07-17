package com.github.auties00.cobalt.message.addon;

import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.message.poll.PollEncValue;
import com.github.auties00.cobalt.wire.linked.message.text.CommentMessage;
import com.github.auties00.cobalt.wire.linked.message.text.CommentMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.security.EncCommentMessage;
import com.github.auties00.cobalt.wire.linked.message.security.EncReactionMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link EncMessageFactory}, the wrapping layer above
 * {@link MessageAddonEncryption}: sender resolution from the parent message
 * key, HKDF context-label selection per addon type, poll-option hashing, and
 * the resulting {@link EncReactionMessage}, {@link EncCommentMessage}, and
 * {@link PollEncValue} envelope shapes. Round-trip key isolation is asserted
 * by decrypting through {@link MessageAddonEncryption} with a mismatched
 * secret or voter. Secrets are fixed 32-byte fills and JIDs are synthetic.
 */
@DisplayName("EncMessageFactory")
class EncMessageFactoryTest {

    private static final byte[] PARENT_SECRET = repeatedByte(32, (byte) 0x42);

    private static final byte[] OTHER_SECRET = repeatedByte(32, (byte) 0x55);

    private static final String PARENT_KEY_ID = "3EB0CAFEBABE0123456789";

    private static final Jid CHAT_JID = Jid.of("12025550100@s.whatsapp.net");

    private static final Jid OTHER_SENDER = Jid.of("12025550200@s.whatsapp.net");

    private static final Jid SELF_JID = Jid.of("12025550999@s.whatsapp.net");

    @Test
    @DisplayName("encryptReaction: result has ciphertext, 12-byte IV, and propagates target key")
    void encryptReactionShape() {
        var parent = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var reactionTarget = new MessageKeyBuilder()
                .id("3EB0TARGET")
                .fromMe(false)
                .parentJid(CHAT_JID)
                .build();
        var reaction = new ReactionMessageBuilder()
                .key(reactionTarget)
                .text("👍")
                .senderTimestampMs(Instant.ofEpochSecond(1700000000L))
                .build();

        var enc = EncMessageFactory.encryptReaction(reaction, parent, SELF_JID);

        assertNotNull(enc);
        assertNotNull(enc.encPayload().orElseThrow());
        assertNotNull(enc.encIv().orElseThrow());
        assertTrue(enc.encPayload().orElseThrow().length > 0, "ciphertext must be non-empty");
        assertEquals(12, enc.encIv().orElseThrow().length, "AES-GCM IV is 12 bytes");
        assertEquals(reactionTarget, enc.targetMessageKey().orElseThrow(),
                "target message key must propagate to the encrypted wrapper");
    }

    @Test
    @DisplayName("encryptReaction: each call samples a fresh IV (different output per call)")
    void encryptReactionFreshIv() {
        var parent = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var reaction = new ReactionMessageBuilder()
                .text("🎯")
                .build();
        var first = EncMessageFactory.encryptReaction(reaction, parent, SELF_JID);
        var second = EncMessageFactory.encryptReaction(reaction, parent, SELF_JID);
        assertNotSame(first, second);
        assertNotEquals(toHex(first.encIv().orElseThrow()), toHex(second.encIv().orElseThrow()));
        assertNotEquals(toHex(first.encPayload().orElseThrow()), toHex(second.encPayload().orElseThrow()));
    }

    @Test
    @DisplayName("encryptReaction: missing parent secret throws IllegalArgumentException")
    void encryptReactionMissingSecret() {
        var parent = parent(null, PARENT_KEY_ID, false, OTHER_SENDER);
        var reaction = new ReactionMessageBuilder().text("👍").build();
        var ex = assertThrows(IllegalArgumentException.class,
                () -> EncMessageFactory.encryptReaction(reaction, parent, SELF_JID));
        assertTrue(ex.getMessage().contains("messageSecret"));
    }

    @Test
    @DisplayName("encryptReaction: null reaction / parent / self all throw NullPointerException")
    void encryptReactionNullArgs() {
        var parent = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var reaction = new ReactionMessageBuilder().text("👍").build();
        assertThrows(NullPointerException.class,
                () -> EncMessageFactory.encryptReaction(null, parent, SELF_JID));
        assertThrows(NullPointerException.class,
                () -> EncMessageFactory.encryptReaction(reaction, null, SELF_JID));
        assertThrows(NullPointerException.class,
                () -> EncMessageFactory.encryptReaction(reaction, parent, null));
    }

    @Test
    @DisplayName("encryptComment: result wraps ciphertext + IV and propagates target message key")
    void encryptCommentShape() {
        var parent = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var targetKey = new MessageKeyBuilder()
                .id("3EB0TARGET")
                .fromMe(false)
                .parentJid(CHAT_JID)
                .build();
        var comment = new CommentMessageBuilder()
                .messageContainer(LinkedMessageContainer.of("hi"))
                .targetMessageKey(targetKey)
                .build();

        var enc = EncMessageFactory.encryptComment(comment, parent, SELF_JID);

        assertNotNull(enc);
        assertEquals(targetKey, enc.targetMessageKey().orElseThrow());
        assertTrue(enc.encPayload().orElseThrow().length > 0);
        assertEquals(12, enc.encIv().orElseThrow().length);
    }

    @Test
    @DisplayName("encryptComment: missing inner message throws IllegalArgumentException")
    void encryptCommentMissingInner() {
        var parent = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var emptyComment = new CommentMessageBuilder().build();
        var ex = assertThrows(IllegalArgumentException.class,
                () -> EncMessageFactory.encryptComment(emptyComment, parent, SELF_JID));
        assertTrue(ex.getMessage().toLowerCase().contains("message"));
    }

    @Test
    @DisplayName("encryptComment: missing parent secret throws IllegalArgumentException")
    void encryptCommentMissingSecret() {
        var parent = parent(null, PARENT_KEY_ID, false, OTHER_SENDER);
        var comment = new CommentMessageBuilder()
                .messageContainer(LinkedMessageContainer.of("hi"))
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> EncMessageFactory.encryptComment(comment, parent, SELF_JID));
    }

    @Test
    @DisplayName("encryptComment: ciphertext is bound to the parent secret; different secret breaks decrypt")
    void encryptCommentKeyIsolation() {
        var parent = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var comment = new CommentMessageBuilder()
                .messageContainer(LinkedMessageContainer.of("isolated"))
                .build();
        var enc = EncMessageFactory.encryptComment(comment, parent, SELF_JID);

        var encryptedAddon = new MessageEncryptedAddon(enc.encPayload().orElseThrow(), enc.encIv().orElseThrow());
        assertThrows(RuntimeException.class, () -> MessageAddonEncryption.decrypt(
                encryptedAddon, OTHER_SECRET, PARENT_KEY_ID, OTHER_SENDER, SELF_JID,
                MessageAddonType.ENC_COMMENT));
    }

    @Test
    @DisplayName("encryptComment: null arguments throw NullPointerException")
    void encryptCommentNullArgs() {
        var parent = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var comment = new CommentMessageBuilder()
                .messageContainer(LinkedMessageContainer.of("hi"))
                .build();
        assertThrows(NullPointerException.class,
                () -> EncMessageFactory.encryptComment(null, parent, SELF_JID));
        assertThrows(NullPointerException.class,
                () -> EncMessageFactory.encryptComment(comment, null, SELF_JID));
        assertThrows(NullPointerException.class,
                () -> EncMessageFactory.encryptComment(comment, parent, null));
    }

    @Test
    @DisplayName("encryptPollVote: result has ciphertext + 12-byte IV")
    void encryptPollVoteShape() {
        var poll = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var encValue = EncMessageFactory.encryptPollVote(List.of("Option A"), poll, SELF_JID);
        assertNotNull(encValue.encPayload().orElseThrow());
        assertTrue(encValue.encPayload().orElseThrow().length > 0);
        assertEquals(12, encValue.encIv().orElseThrow().length);
    }

    @Test
    @DisplayName("encryptPollVote: encryption is AAD-bound; decrypt with a different voter is rejected")
    void encryptPollVoteAadBindsVoter() {
        var poll = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var encValue = EncMessageFactory.encryptPollVote(List.of("Option A"), poll, SELF_JID);

        var encryptedAddon = new MessageEncryptedAddon(encValue.encPayload().orElseThrow(), encValue.encIv().orElseThrow());
        var otherVoter = Jid.of("12025550900@s.whatsapp.net");
        assertThrows(RuntimeException.class, () -> MessageAddonEncryption.decrypt(
                encryptedAddon, PARENT_SECRET, PARENT_KEY_ID, OTHER_SENDER, otherVoter,
                MessageAddonType.POLL_VOTE));
    }

    @Test
    @DisplayName("encryptPollVote: missing parent secret throws IllegalArgumentException")
    void encryptPollVoteMissingSecret() {
        var poll = parent(null, PARENT_KEY_ID, false, OTHER_SENDER);
        assertThrows(IllegalArgumentException.class,
                () -> EncMessageFactory.encryptPollVote(List.of("Option A"), poll, SELF_JID));
    }

    @Test
    @DisplayName("encryptPollVote: null selected entry throws NullPointerException")
    void encryptPollVoteNullEntry() {
        var poll = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var withNull = Arrays.asList("A", null);
        assertThrows(NullPointerException.class,
                () -> EncMessageFactory.encryptPollVote(withNull, poll, SELF_JID));
    }

    @Test
    @DisplayName("encryptPollVote: empty option list still produces a valid envelope (zero votes)")
    void encryptPollVoteEmptyOptions() {
        var poll = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var encValue = EncMessageFactory.encryptPollVote(List.of(), poll, SELF_JID);
        assertNotNull(encValue.encPayload().orElseThrow());
        // even a zero-option vote carries the 16-byte AES-GCM tag, so the ciphertext is never empty
        assertTrue(encValue.encPayload().orElseThrow().length >= 16,
                "ciphertext must include the 16-byte GCM tag even for empty input");
    }

    @Test
    @DisplayName("encryptPollVote: null arguments throw NullPointerException")
    void encryptPollVoteNullArgs() {
        var poll = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        assertThrows(NullPointerException.class,
                () -> EncMessageFactory.encryptPollVote(null, poll, SELF_JID));
        assertThrows(NullPointerException.class,
                () -> EncMessageFactory.encryptPollVote(List.of("A"), null, SELF_JID));
        assertThrows(NullPointerException.class,
                () -> EncMessageFactory.encryptPollVote(List.of("A"), poll, null));
    }

    @Test
    @DisplayName("sender resolution: fromMe=true parent encrypts under selfJid (mirrors WAWebMsgGetters.getOriginalSender)")
    void senderResolutionFromMeUsesSelf() {
        var parentFromMe = parent(PARENT_SECRET, PARENT_KEY_ID, true, null);
        var reaction = new ReactionMessageBuilder().text("✓").build();
        var enc = EncMessageFactory.encryptReaction(reaction, parentFromMe, SELF_JID);
        var addon = new MessageEncryptedAddon(enc.encPayload().orElseThrow(), enc.encIv().orElseThrow());

        var recovered = MessageAddonEncryption.decrypt(
                addon, PARENT_SECRET, PARENT_KEY_ID, SELF_JID, SELF_JID,
                MessageAddonType.ENC_REACTION);
        assertTrue(recovered.length > 0);

        assertThrows(RuntimeException.class, () -> MessageAddonEncryption.decrypt(
                addon, PARENT_SECRET, PARENT_KEY_ID, CHAT_JID, SELF_JID,
                MessageAddonType.ENC_REACTION));
    }

    @Test
    @DisplayName("sender resolution: fromMe=false with explicit senderJid encrypts under senderJid")
    void senderResolutionUsesExplicitSender() {
        var parent = parent(PARENT_SECRET, PARENT_KEY_ID, false, OTHER_SENDER);
        var reaction = new ReactionMessageBuilder().text("✓").build();
        var enc = EncMessageFactory.encryptReaction(reaction, parent, SELF_JID);
        var addon = new MessageEncryptedAddon(enc.encPayload().orElseThrow(), enc.encIv().orElseThrow());

        var recovered = MessageAddonEncryption.decrypt(
                addon, PARENT_SECRET, PARENT_KEY_ID, OTHER_SENDER, SELF_JID,
                MessageAddonType.ENC_REACTION);
        assertTrue(recovered.length > 0);

        assertThrows(RuntimeException.class, () -> MessageAddonEncryption.decrypt(
                addon, PARENT_SECRET, PARENT_KEY_ID, SELF_JID, SELF_JID,
                MessageAddonType.ENC_REACTION));
    }

    @Test
    @DisplayName("sender resolution: fromMe=false with no senderJid falls back to parentJid")
    void senderResolutionFallsBackToParentJid() {
        var parent = parent(PARENT_SECRET, PARENT_KEY_ID, false, null);
        var reaction = new ReactionMessageBuilder().text("✓").build();
        var enc = EncMessageFactory.encryptReaction(reaction, parent, SELF_JID);
        var addon = new MessageEncryptedAddon(enc.encPayload().orElseThrow(), enc.encIv().orElseThrow());

        var recovered = MessageAddonEncryption.decrypt(
                addon, PARENT_SECRET, PARENT_KEY_ID, CHAT_JID, SELF_JID,
                MessageAddonType.ENC_REACTION);
        assertTrue(recovered.length > 0);
    }

    private static ChatMessageInfo parent(byte[] secret, String keyId, boolean fromMe, Jid senderJid) {
        var key = new MessageKeyBuilder()
                .id(keyId)
                .fromMe(fromMe)
                .parentJid(CHAT_JID)
                .senderJid(senderJid)
                .build();
        var builder = new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.of("parent body"));
        if (secret != null) {
            builder.messageSecret(secret);
        }
        return builder.build();
    }

    private static byte[] repeatedByte(int len, byte b) {
        var out = new byte[len];
        Arrays.fill(out, b);
        return out;
    }

    private static String toHex(byte[] bytes) {
        var sb = new StringBuilder(bytes.length * 2);
        for (var b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
