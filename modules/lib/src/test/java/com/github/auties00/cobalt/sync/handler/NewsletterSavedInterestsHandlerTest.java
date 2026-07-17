package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.NewsletterSavedInterestsAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.NewsletterSavedInterestsActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSettingsStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link NewsletterSavedInterestsHandler} for the protobuf-only
 * {@code "newsletter_saved_interests"} action: the handler accepts only {@link SyncdOperation#SET}
 * with a non-empty interests token, persists it via
 * {@link LinkedWhatsAppSettingsStore#setNewsletterSavedInterests}, and rejects a wrong-typed value or an empty
 * token as {@link SyncActionState#MALFORMED}.
 *
 * <p>No public outgoing-mutation factory exists for this action, so each test drives the handler
 * directly through {@link NewsletterSavedInterestsHandler#applyMutation} with hand-built
 * {@link DecryptedMutation.Trusted} mutations.
 */
@DisplayName("NewsletterSavedInterestsHandler")
class NewsletterSavedInterestsHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private LinkedWhatsAppStore store;
    private LinkedWhatsAppClient client;
    private NewsletterSavedInterestsHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
        handler = new NewsletterSavedInterestsHandler();
    }

    private DecryptedMutation.Trusted build(NewsletterSavedInterestsAction action, SyncdOperation op, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) {
            valueBuilder.newsletterSavedInterestsAction(action);
        }
        var index = JSON.toJSONString(List.of(handler.actionName()));
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), op, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns 'newsletter_saved_interests'")
        void actionName() {
            assertEquals(NewsletterSavedInterestsAction.ACTION_NAME, handler.actionName());
            assertEquals("newsletter_saved_interests", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(NewsletterSavedInterestsAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - SET persists the interests token")
    class ApplySetHappy {
        @Test
        @DisplayName("SET with a non-empty interests token persists the value on the store")
        void setsInterests() {
            assertTrue(store.settingsStore().newsletterSavedInterests().isEmpty(), "precondition: interests are unset");
            var action = new NewsletterSavedInterestsActionBuilder()
                    .newsletterSavedInterests("topics:tech,sports").build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals("topics:tech,sports", store.settingsStore().newsletterSavedInterests().orElseThrow());
        }

        @Test
        @DisplayName("SET overwrites any prior interests token")
        void setsOverwrites() {
            store.settingsStore().setNewsletterSavedInterests("old");
            var action = new NewsletterSavedInterestsActionBuilder()
                    .newsletterSavedInterests("new").build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals("new", store.settingsStore().newsletterSavedInterests().orElseThrow());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedActionValue {
        @Test
        @DisplayName("a SET with the wrong action type returns MALFORMED")
        void wrongActionType() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"newsletter_saved_interests\"]", value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a SET whose newsletterSavedInterests field is unset returns MALFORMED")
        void emptyInterests() {
            var action = new NewsletterSavedInterestsActionBuilder().build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE is UNSUPPORTED")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE returns UNSUPPORTED without touching the store")
        void removeIsUnsupported() {
            var action = new NewsletterSavedInterestsActionBuilder()
                    .newsletterSavedInterests("foo").build();

            var result = handler.applyMutation(client, build(action, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(store.settingsStore().newsletterSavedInterests().isEmpty());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ConflictResolution {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var action = new NewsletterSavedInterestsActionBuilder()
                    .newsletterSavedInterests("foo").build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var action = new NewsletterSavedInterestsActionBuilder()
                    .newsletterSavedInterests("foo").build();
            var local = build(action, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = build(action, SyncdOperation.SET, Instant.ofEpochSecond(1_000));

            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }
    }

}
