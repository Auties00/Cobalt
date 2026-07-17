package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wam.TestWamService;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.BotWelcomeRequestAction;
import com.github.auties00.cobalt.wire.linked.sync.action.bot.BotWelcomeRequestActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.BotWelcomeRequestMutationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link BotWelcomeRequestHandler}, which records whether the bot welcome message has been
 * delivered to a given bot chat, indexed by the chat's JID. The SET path requires the chat to
 * already exist in the store, so missing-chat cases assert the ORPHAN outcome.
 */
@DisplayName("BotWelcomeRequestHandler")
class BotWelcomeRequestHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid BOT_JID = Jid.of("12025550100@s.whatsapp.net");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    private static DecryptedMutation.Trusted botMutation(String chatJidString, Boolean isSent, SyncdOperation op, Instant ts) {
        var action = new BotWelcomeRequestActionBuilder().isSent(isSent).build();
        var value = new SyncActionValueBuilder().timestamp(ts).botWelcomeRequestAction(action).build();
        var index = "[\"bot_welcome_request\",\"" + chatJidString + "\"]";
        return new DecryptedMutation.Trusted(index, value, op, ts, 2);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'bot_welcome_request'")
        void actionName() {
            assertEquals(BotWelcomeRequestAction.ACTION_NAME, new BotWelcomeRequestHandler().actionName());
            assertEquals("bot_welcome_request", new BotWelcomeRequestHandler().actionName());
        }

        @Test
        @DisplayName("collectionName() is SyncPatchType.REGULAR_LOW")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_LOW, new BotWelcomeRequestHandler().collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (2)")
        void version() {
            assertEquals(BotWelcomeRequestAction.ACTION_VERSION, new BotWelcomeRequestHandler().version());
            assertEquals(2, new BotWelcomeRequestHandler().version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET on a known chat")
    class SetHappy {
        @Test
        @DisplayName("isSent=true writes a BotWelcomeRequestState keyed by the chat JID")
        void writesState() {
            client.store().chatStore().addNewChat(BOT_JID);

            var result = new BotWelcomeRequestHandler().applyMutation(
                    client, botMutation(BOT_JID.toString(), Boolean.TRUE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var state = client.store().businessStore().findBotWelcomeRequestState(BOT_JID).orElseThrow();
            assertEquals(BOT_JID, state.botJid());
            assertTrue(state.requested());
        }

        @Test
        @DisplayName("isSent=false is also SUCCESS and writes requested=false")
        void writesFalse() {
            client.store().chatStore().addNewChat(BOT_JID);

            var result = new BotWelcomeRequestHandler().applyMutation(
                    client, botMutation(BOT_JID.toString(), Boolean.FALSE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var state = client.store().businessStore().findBotWelcomeRequestState(BOT_JID).orElseThrow();
            assertFalse(state.requested());
        }
    }

    @Nested
    @DisplayName("applyMutation: orphan when chat is unknown")
    class Orphan {
        @Test
        @DisplayName("unknown chat JID returns ORPHAN with the JID as modelId")
        void orphanChat() {
            var result = new BotWelcomeRequestHandler().applyMutation(
                    client, botMutation(BOT_JID.toString(), Boolean.TRUE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(BOT_JID.toString(), result.modelId());
            assertEquals("Chat", result.modelType());
            assertTrue(client.store().businessStore().findBotWelcomeRequestState(BOT_JID).isEmpty(),
                    "orphan must not create a state entry");
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed paths")
    class Malformed {
        @Test
        @DisplayName("empty chat JID string yields MALFORMED (action index)")
        void emptyJid() {
            var action = new BotWelcomeRequestActionBuilder().isSent(Boolean.TRUE).build();
            var value = new SyncActionValueBuilder().timestamp(Instant.now()).botWelcomeRequestAction(action).build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"bot_welcome_request\",\"\"]", value, SyncdOperation.SET, Instant.now(), 2);

            var result = new BotWelcomeRequestHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("non-bot-welcome value yields MALFORMED (action value)")
        void wrongActionType() {
            client.store().chatStore().addNewChat(BOT_JID);

            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"bot_welcome_request\",\"" + BOT_JID + "\"]",
                    wrongValue, SyncdOperation.SET, Instant.now(), 2);

            var result = new BotWelcomeRequestHandler().applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation: REMOVE is UNSUPPORTED")
    class RemoveBranch {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED before any store write")
        void removeIsUnsupported() {
            client.store().chatStore().addNewChat(BOT_JID);

            var result = new BotWelcomeRequestHandler().applyMutation(
                    client, botMutation(BOT_JID.toString(), Boolean.TRUE, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(client.store().businessStore().findBotWelcomeRequestState(BOT_JID).isEmpty(),
                    "REMOVE must not create a state entry");
        }
    }

    @Nested
    @DisplayName("getBotWelcomeRequestSetMutation - builder helper")
    class Builder {
        @Test
        @DisplayName("emits a SET pending mutation with the requested chat JID and flag")
        void buildsPending() {
            var pending = new BotWelcomeRequestMutationFactory(TestWamService.create(client)).getBotWelcomeRequestSetMutation(BOT_JID, true);

            assertEquals(SyncdOperation.SET, pending.mutation().operation());
            assertEquals(BotWelcomeRequestAction.ACTION_VERSION, pending.mutation().actionVersion());
            assertEquals("[\"bot_welcome_request\",\"" + BOT_JID + "\"]", pending.mutation().index());
            var action = (BotWelcomeRequestAction) pending.mutation().value().flatMap(sav -> sav.action()).orElseThrow();
            assertTrue(action.isSent());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins")
        void remoteWins() {
            var local  = botMutation(BOT_JID.toString(), Boolean.FALSE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var remote = botMutation(BOT_JID.toString(), Boolean.TRUE,  SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = new BotWelcomeRequestHandler().resolveConflicts(local, remote);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch - n/a, default implementation")
    class BatchNa {
        @Test
        @DisplayName("default applyMutationBatch delegates to applyMutation per mutation")
        void perItem() {
            client.store().chatStore().addNewChat(BOT_JID);

            var batch = List.of(
                    botMutation(BOT_JID.toString(), Boolean.TRUE, SyncdOperation.SET, Instant.now()),
                    botMutation(BOT_JID.toString(), Boolean.TRUE, SyncdOperation.REMOVE, Instant.now()));

            var results = new BotWelcomeRequestHandler().applyMutationBatch(client, batch);

            assertEquals(2, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.UNSUPPORTED, results.get(1).actionState());
        }
    }

}
