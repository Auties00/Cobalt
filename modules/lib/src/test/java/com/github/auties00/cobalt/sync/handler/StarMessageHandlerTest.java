package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.StarAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.StarActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link StarMessageHandler}: applying an incoming star mutation
 * and asserting the message store side-effect. The fixture seeds a single
 * peer chat and an inbound message keyed under {@link #MESSAGE_ID}; unstar
 * tests pre-flip the starred flag so they can observe the transition back
 * to {@code false}.
 */
@DisplayName("StarMessageHandler")
class StarMessageHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PEER = Jid.of("1234567890@s.whatsapp.net");
    private static final String MESSAGE_ID = "msg-1";

    private TestWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    // Five-slot index ["star", chatJid, messageId, fromMe, participant];
    // "0" for fromMe and participant selects the 1:1 chat branch.
    private static DecryptedMutation.Trusted starMutation(boolean starred, Jid chatJid, String messageId, String fromMe, String participant, Instant ts) {
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .starAction(new StarActionBuilder().starred(starred).build())
                .build();
        var index = JSON.toJSONString(List.of("star", chatJid.toString(), messageId, fromMe, participant));
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 2);
    }

    // Orphan-branch tests deliberately skip this seed so findMessageById misses.
    private void seedMessage() {
        var chat = client.store().chatStore().addNewChat(PEER);
        var key = new MessageKeyBuilder()
                .id(MESSAGE_ID).fromMe(false).parentJid(PEER)
                .build();
        chat.addMessage(new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.of("hello"))
                .timestamp(Instant.now())
                .build());
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName returns \"star\"")
        void actionName() {
            assertEquals("star", new StarMessageHandler().actionName());
            assertEquals(StarAction.ACTION_NAME, new StarMessageHandler().actionName());
        }

        @Test
        @DisplayName("collectionName resolves to REGULAR_HIGH")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_HIGH, new StarMessageHandler().collectionName());
        }

        @Test
        @DisplayName("version returns the declared action version 2")
        void version() {
            assertEquals(2, new StarMessageHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation SET - happy path")
    class HappySet {
        @Test
        @DisplayName("starred=true on an existing message stars it")
        void starsTheMessage() {
            seedMessage();

            var result = new StarMessageHandler().applyMutation(client,
                    starMutation(true, PEER, MESSAGE_ID, "0", "0", Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var message = client.store().chatStore().findMessageById(client.store().chatStore().findChatByJid(PEER).orElseThrow(), MESSAGE_ID).orElseThrow();
            assertTrue(message.starred());
        }

        @Test
        @DisplayName("starred=false unstars an already-starred message")
        void unstarsTheMessage() {
            seedMessage();
            client.store().chatStore().findMessageById(client.store().chatStore().findChatByJid(PEER).orElseThrow(), MESSAGE_ID)
                    .orElseThrow().setStarred(true);

            var result = new StarMessageHandler().applyMutation(client,
                    starMutation(false, PEER, MESSAGE_ID, "0", "0", Instant.ofEpochSecond(2L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var message = client.store().chatStore().findMessageById(client.store().chatStore().findChatByJid(PEER).orElseThrow(), MESSAGE_ID).orElseThrow();
            assertFalse(message.starred());
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan")
    class Orphan {
        @Test
        @DisplayName("SET against an unknown message returns ORPHAN with modelType=Msg")
        void orphanReturnsMsgModel() {
            // Chat exists but the message is missing.
            client.store().chatStore().addNewChat(PEER);

            var result = new StarMessageHandler().applyMutation(client,
                    starMutation(true, PEER, "absent-id", "0", "0", Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals("Msg", result.modelType());
            assertNotNull(result.modelId(),
                    "orphan modelId must carry the serialized MsgKey for replay");
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SyncActionValue carrying an archiveChatAction instead of starAction is MALFORMED")
        void wrongActionTypeIsMalformed() {
            seedMessage();
            var wrong = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("star", PEER.toString(), MESSAGE_ID, "0", "0")),
                    wrong, SyncdOperation.SET, Instant.ofEpochSecond(1L), 2);

            var result = new StarMessageHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("a 4-element index missing the participant slot is MALFORMED")
        void shortIndexIsMalformed() {
            seedMessage();
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .starAction(new StarActionBuilder().starred(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"star\",\"" + PEER + "\",\"" + MESSAGE_ID + "\",\"0\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 2);

            var result = new StarMessageHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("an index whose chatJid slot is empty is MALFORMED")
        void emptyChatJidIsMalformed() {
            seedMessage();
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .starAction(new StarActionBuilder().starred(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"star\",\"\",\"" + MESSAGE_ID + "\",\"0\",\"0\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 2);

            var result = new StarMessageHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED")
        void removeReturnsUnsupported() {
            seedMessage();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("star", PEER.toString(), MESSAGE_ID, "0", "0")),
                    new SyncActionValueBuilder()
                            .timestamp(Instant.ofEpochSecond(1L))
                            .starAction(new StarActionBuilder().starred(true).build())
                            .build(),
                    SyncdOperation.REMOVE, Instant.ofEpochSecond(1L), 2);

            var result = new StarMessageHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based behaviour")
    class ResolveConflicts {
        @Test
        @DisplayName("older local vs. newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteWins() {
            var local = starMutation(false, PEER, MESSAGE_ID, "0", "0", Instant.ofEpochSecond(100L));
            var remote = starMutation(true, PEER, MESSAGE_ID, "0", "0", Instant.ofEpochSecond(200L));

            var resolution = new StarMessageHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("newer local vs. older remote -> SKIP_REMOTE")
        void newerLocalWins() {
            var local = starMutation(true, PEER, MESSAGE_ID, "0", "0", Instant.ofEpochSecond(300L));
            var remote = starMutation(false, PEER, MESSAGE_ID, "0", "0", Instant.ofEpochSecond(200L));

            var resolution = new StarMessageHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }
    }

}
