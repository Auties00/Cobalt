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
import com.github.auties00.cobalt.wire.linked.sync.action.chat.InteractiveMessageAction;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.InteractiveMessageAction.InteractiveMessageActionMode;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.InteractiveMessageActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppBusinessStore;
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
 * Covers the {@link InteractiveMessageHandler} for the
 * {@code interactive_message_action} app-state sync action: metadata, the SET
 * happy path that records the per-AGM and per-message state, the orphan branches
 * when chat or message are missing, the malformed-value and malformed-index
 * branches, the REMOVE rejection, timestamp-based conflict resolution and the
 * {@link InteractiveMessageHandler#buildDisableCTAAction} helper.
 *
 * <p>Tests run against a fresh in-memory {@link DeviceFixtures#temporaryStore}
 * through {@link TestWhatsAppClient} so the per-AGM and composite-index state
 * recorded by the handler can be read back through
 * {@link LinkedWhatsAppBusinessStore#interactiveMessageStates()}.
 */
@DisplayName("InteractiveMessageHandler")
class InteractiveMessageHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PEER = Jid.of("1234567890@s.whatsapp.net");
    private static final String MESSAGE_ID = "msg-1";
    private static final String SUB_ID = "sub-7";

    private TestWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted interactiveMutation(InteractiveMessageActionMode mode, String agmId,
                                                                 Jid chatJid, String messageId, String fromMe,
                                                                 String participant, String subId, Instant ts) {
        var builder = new InteractiveMessageActionBuilder().type(mode);
        if (agmId != null) builder.agmId(agmId);
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .interactiveMessageAction(builder.build())
                .build();
        var index = JSON.toJSONString(List.of("interactive_message_action", chatJid.toString(), messageId, fromMe, participant, subId));
        return new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, 1);
    }

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
        @DisplayName("actionName returns \"interactive_message_action\"")
        void actionName() {
            assertEquals("interactive_message_action", new InteractiveMessageHandler().actionName());
            assertEquals(InteractiveMessageAction.ACTION_NAME, new InteractiveMessageHandler().actionName());
        }

        @Test
        @DisplayName("collectionName resolves to REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new InteractiveMessageHandler().collectionName());
        }

        @Test
        @DisplayName("version returns the declared action version 1")
        void version() {
            assertEquals(1, new InteractiveMessageHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation SET - happy path")
    class HappySet {
        @Test
        @DisplayName("DISABLE_CTA against an existing message records the interactive state")
        void recordsInteractiveState() {
            seedMessage();

            var result = new InteractiveMessageHandler().applyMutation(client,
                    interactiveMutation(InteractiveMessageActionMode.DISABLE_CTA, null,
                            PEER, MESSAGE_ID, "0", "0", SUB_ID, Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var states = client.store().businessStore().interactiveMessageStates();
            assertFalse(states.isEmpty(), "interactive message state must be recorded");
            assertTrue(states.stream().anyMatch(s -> s.messageId().startsWith("messageId|")),
                    "the messageId-keyed state must be present");
            assertTrue(states.stream().anyMatch(s -> s.messageId().contains(SUB_ID)),
                    "the composite-index state including the subId must be present");
        }

        @Test
        @DisplayName("agmId-only path: when message is missing but agmId is present, the agmId state is recorded and the result is SUCCESS")
        void agmIdRecordedWhenMessageMissing() {
            // Chat exists; message does not. AgmId present.
            client.store().chatStore().addNewChat(PEER);

            var result = new InteractiveMessageHandler().applyMutation(client,
                    interactiveMutation(InteractiveMessageActionMode.DISABLE_CTA, "agm-42",
                            PEER, "absent-id", "0", "0", SUB_ID, Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(client.store().businessStore().interactiveMessageStates().stream()
                            .anyMatch(s -> s.messageId().equals("agmId|agm-42")),
                    "agmId state must be recorded even when the message is absent");
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan")
    class Orphan {
        @Test
        @DisplayName("SET against an unknown chat JID returns ORPHAN with modelType=Msg")
        void orphanReturnsMsgModel() {
            var result = new InteractiveMessageHandler().applyMutation(client,
                    interactiveMutation(InteractiveMessageActionMode.DISABLE_CTA, null,
                            PEER, MESSAGE_ID, "0", "0", SUB_ID, Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals("Msg", result.modelType());
            assertNotNull(result.modelId());
        }

        @Test
        @DisplayName("SET against a known chat but missing message + no agmId returns ORPHAN")
        void missingMessageWithoutAgmIdIsOrphan() {
            client.store().chatStore().addNewChat(PEER);

            var result = new InteractiveMessageHandler().applyMutation(client,
                    interactiveMutation(InteractiveMessageActionMode.DISABLE_CTA, null,
                            PEER, "absent-id", "0", "0", SUB_ID, Instant.ofEpochSecond(1L)));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals("Msg", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SyncActionValue carrying a pinAction instead of interactiveMessageAction is MALFORMED")
        void wrongActionTypeIsMalformed() {
            seedMessage();
            var wrong = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    JSON.toJSONString(List.of("interactive_message_action", PEER.toString(), MESSAGE_ID, "0", "0", SUB_ID)),
                    wrong, SyncdOperation.SET, Instant.ofEpochSecond(1L), 1);

            var result = new InteractiveMessageHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("a 5-element index missing the subId slot is MALFORMED")
        void shortIndexIsMalformed() {
            seedMessage();
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .interactiveMessageAction(new InteractiveMessageActionBuilder().type(InteractiveMessageActionMode.DISABLE_CTA).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"interactive_message_action\",\"" + PEER + "\",\"" + MESSAGE_ID + "\",\"0\",\"0\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 1);

            var result = new InteractiveMessageHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("an empty subId at slot 5 is MALFORMED")
        void emptySubIdIsMalformed() {
            seedMessage();
            var value = new SyncActionValueBuilder()
                    .timestamp(Instant.ofEpochSecond(1L))
                    .interactiveMessageAction(new InteractiveMessageActionBuilder().type(InteractiveMessageActionMode.DISABLE_CTA).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"interactive_message_action\",\"" + PEER + "\",\"" + MESSAGE_ID + "\",\"0\",\"0\",\"\"]",
                    value, SyncdOperation.SET, Instant.ofEpochSecond(1L), 1);

            var result = new InteractiveMessageHandler().applyMutation(client, mutation);
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
                    JSON.toJSONString(List.of("interactive_message_action", PEER.toString(), MESSAGE_ID, "0", "0", SUB_ID)),
                    new SyncActionValueBuilder()
                            .timestamp(Instant.ofEpochSecond(1L))
                            .interactiveMessageAction(new InteractiveMessageActionBuilder().type(InteractiveMessageActionMode.DISABLE_CTA).build())
                            .build(),
                    SyncdOperation.REMOVE, Instant.ofEpochSecond(1L), 1);

            var result = new InteractiveMessageHandler().applyMutation(client, mutation);
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(client.store().businessStore().interactiveMessageStates().isEmpty(),
                    "REMOVE must not record any state");
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based behaviour")
    class ResolveConflicts {
        // InteractiveMessageHandler does NOT override resolveConflicts; the interface default applies.
        @Test
        @DisplayName("older local vs. newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteWins() {
            var local = interactiveMutation(InteractiveMessageActionMode.DISABLE_CTA, null,
                    PEER, MESSAGE_ID, "0", "0", SUB_ID, Instant.ofEpochSecond(100L));
            var remote = interactiveMutation(InteractiveMessageActionMode.DISABLE_CTA, null,
                    PEER, MESSAGE_ID, "0", "0", SUB_ID, Instant.ofEpochSecond(200L));

            var resolution = new InteractiveMessageHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("newer local vs. older remote -> SKIP_REMOTE")
        void newerLocalWins() {
            var local = interactiveMutation(InteractiveMessageActionMode.DISABLE_CTA, null,
                    PEER, MESSAGE_ID, "0", "0", SUB_ID, Instant.ofEpochSecond(300L));
            var remote = interactiveMutation(InteractiveMessageActionMode.DISABLE_CTA, null,
                    PEER, MESSAGE_ID, "0", "0", SUB_ID, Instant.ofEpochSecond(200L));

            var resolution = new InteractiveMessageHandler().resolveConflicts(local, remote);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }
    }

    @Nested
    @DisplayName("buildDisableCTAAction - builder helper")
    class BuilderHelpers {
        @Test
        @DisplayName("buildDisableCTAAction carries the type and agmId fields")
        void carriesTypeAndAgmId() {
            var action = new InteractiveMessageHandler().buildDisableCTAAction(
                    InteractiveMessageActionMode.DISABLE_CTA, "agm-99");
            assertEquals(InteractiveMessageActionMode.DISABLE_CTA, action.type());
            assertEquals("agm-99", action.agmId().orElseThrow());
        }

        @Test
        @DisplayName("buildDisableCTAAction without an agmId leaves the field empty")
        void agmIdIsOptional() {
            var action = new InteractiveMessageHandler().buildDisableCTAAction(
                    InteractiveMessageActionMode.DISABLE_CTA, null);
            assertEquals(InteractiveMessageActionMode.DISABLE_CTA, action.type());
            assertTrue(action.agmId().isEmpty());
        }
    }

}
