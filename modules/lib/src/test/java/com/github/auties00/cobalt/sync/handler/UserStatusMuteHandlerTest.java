package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wam.TestWamService;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadataBuilder;
import com.github.auties00.cobalt.wire.linked.contact.ContactBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.UserStatusMuteAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.UserStatusMuteActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesAction;
import com.github.auties00.cobalt.wire.linked.sync.action.media.FavoritesActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
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
 * Verifies {@link UserStatusMuteHandler} and the outgoing
 * {@link UserStatusMuteHandler#getMutationForStatusMute(Jid, boolean, Instant)}
 * builder: applying an incoming mutation and asserting the {@code statusMuted}
 * side-effect on either the local
 * {@link com.github.auties00.cobalt.wire.linked.contact.Contact} or the
 * {@link GroupMetadata#statusMuted()} field. The fixture pre-seeds neither
 * the contact nor the group metadata, so tests opt in case by case.
 */
@DisplayName("UserStatusMuteHandler")
class UserStatusMuteHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid CONTACT = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid GROUP = Jid.of("12025550100-1609459200@g.us");

    private LinkedWhatsAppClient client;

    @BeforeEach
    void setUp() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        client = TestWhatsAppClient.create().withStore(store);
    }

    // widString takes arbitrary raw strings to drive the wid-parse malformed branches.
    private static DecryptedMutation.Trusted mutationFor(String widString, Boolean muted, SyncdOperation op, Instant ts) {
        var action = new UserStatusMuteActionBuilder().muted(muted).build();
        var value = new SyncActionValueBuilder()
                .timestamp(ts)
                .userStatusMuteAction(action)
                .build();
        var index = "[\"userStatusMute\",\"" + widString + "\"]";
        return new DecryptedMutation.Trusted(index, value, op, ts, 7);
    }

    @Nested
    @DisplayName("metadata")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the WA wire constant 'userStatusMute'")
        void actionName() {
            assertEquals(UserStatusMuteAction.ACTION_NAME, new UserStatusMuteHandler(TestWamService.create(client)).actionName());
            assertEquals("userStatusMute", new UserStatusMuteHandler(TestWamService.create(client)).actionName());
        }

        @Test
        @DisplayName("collectionName() resolves to SyncPatchType.REGULAR_HIGH")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR_HIGH, new UserStatusMuteHandler(TestWamService.create(client)).collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version")
        void version() {
            assertEquals(UserStatusMuteAction.ACTION_VERSION, new UserStatusMuteHandler(TestWamService.create(client)).version());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET on a known contact")
    class SetContact {
        @Test
        @DisplayName("flips the contact's statusMuted flag and returns SUCCESS")
        void appliesToContact() {
            client.store().contactStore().addContact(new ContactBuilder().jid(CONTACT).build());

            var result = new UserStatusMuteHandler(TestWamService.create(client)).applyMutation(
                    client, mutationFor(CONTACT.toString(), Boolean.TRUE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "muting a known contact must succeed");
            assertTrue(client.store().contactStore().findContactByJid(CONTACT).orElseThrow().statusMuted(),
                    "the contact's statusMuted field must reflect the mutation");
        }

        @Test
        @DisplayName("a second mutation can flip the flag back to false")
        void canUnmute() {
            var contact = new ContactBuilder().jid(CONTACT).statusMuted(true).build();
            client.store().contactStore().addContact(contact);

            var result = new UserStatusMuteHandler(TestWamService.create(client)).applyMutation(
                    client, mutationFor(CONTACT.toString(), Boolean.FALSE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(client.store().contactStore().findContactByJid(CONTACT).orElseThrow().statusMuted());
        }
    }

    @Nested
    @DisplayName("applyMutation: SET on a known group")
    class SetGroup {
        @Test
        @DisplayName("writes statusMuted to the GroupMetadata row")
        void appliesToGroup() {
            var metadata = new GroupMetadataBuilder()
                    .jid(GROUP)
                    .subject("Test Group")
                    .build();
            client.store().chatStore().addChatMetadata(metadata);

            var result = new UserStatusMuteHandler(TestWamService.create(client)).applyMutation(
                    client, mutationFor(GROUP.toString(), Boolean.TRUE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L)));

            assertEquals(SyncActionState.SUCCESS, result.actionState(),
                    "muting a known group must succeed");
            var stored = client.store().chatStore().findChatMetadata(GROUP).orElseThrow();
            assertTrue(stored instanceof GroupMetadata, "group metadata row must be present");
            assertTrue(((GroupMetadata) stored).statusMuted(),
                    "the group's statusMuted field must reflect the mutation");
        }
    }

    @Nested
    @DisplayName("applyMutation: orphan paths")
    class Orphan {
        @Test
        @DisplayName("unknown contact wid returns ORPHAN with the wid as the model id")
        void unknownContact() {
            var result = new UserStatusMuteHandler(TestWamService.create(client)).applyMutation(
                    client, mutationFor(CONTACT.toString(), Boolean.TRUE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(CONTACT.toString(), result.modelId());
            assertEquals("UserStatusMute", result.modelType());
        }

        @Test
        @DisplayName("unknown group wid returns ORPHAN with the wid as the model id")
        void unknownGroup() {
            var result = new UserStatusMuteHandler(TestWamService.create(client)).applyMutation(
                    client, mutationFor(GROUP.toString(), Boolean.TRUE, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.ORPHAN, result.actionState());
            assertEquals(GROUP.toString(), result.modelId());
            assertEquals("UserStatusMute", result.modelType());
        }
    }

    @Nested
    @DisplayName("applyMutation: malformed paths")
    class Malformed {
        @Test
        @DisplayName("empty wid string yields MALFORMED (action index)")
        void emptyWid() {
            var action = new UserStatusMuteActionBuilder().muted(Boolean.TRUE).build();
            var value = new SyncActionValueBuilder().timestamp(Instant.now()).userStatusMuteAction(action).build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"userStatusMute\",\"\"]", value, SyncdOperation.SET, Instant.now(), 7);

            var result = new UserStatusMuteHandler(TestWamService.create(client)).applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("missing wid in index yields MALFORMED (action index)")
        void missingWid() {
            var action = new UserStatusMuteActionBuilder().muted(Boolean.TRUE).build();
            var value = new SyncActionValueBuilder().timestamp(Instant.now()).userStatusMuteAction(action).build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"userStatusMute\"]", value, SyncdOperation.SET, Instant.now(), 7);

            var result = new UserStatusMuteHandler(TestWamService.create(client)).applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("value carrying a different action yields MALFORMED (action value)")
        void wrongActionInValue() {
            var wrongValue = new SyncActionValueBuilder()
                    .timestamp(Instant.now())
                    .favoritesAction(new FavoritesActionBuilder().favorites(List.of()).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(
                    "[\"userStatusMute\",\"" + CONTACT + "\"]",
                    wrongValue, SyncdOperation.SET, Instant.now(), 7);

            var result = new UserStatusMuteHandler(TestWamService.create(client)).applyMutation(client, mutation);

            assertEquals(SyncActionState.MALFORMED, result.actionState(),
                    "a value carrying " + FavoritesAction.class.getSimpleName() + " must be tagged malformed");
        }
    }

    @Nested
    @DisplayName("applyMutation: non-SET operations are UNSUPPORTED")
    class NonSet {
        @Test
        @DisplayName("REMOVE is rejected before any side effect")
        void removeIsUnsupported() {
            client.store().contactStore().addContact(new ContactBuilder().jid(CONTACT).build());

            var result = new UserStatusMuteHandler(TestWamService.create(client)).applyMutation(
                    client, mutationFor(CONTACT.toString(), Boolean.TRUE, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertFalse(client.store().contactStore().findContactByJid(CONTACT).orElseThrow().statusMuted(),
                    "REMOVE must not flip the contact's mute flag");
        }
    }

    @Nested
    @DisplayName("getMutationForStatusMute - builder helper")
    class Builder {
        @Test
        @DisplayName("emits a SET pending mutation with the requested wid and flag")
        void buildsPendingMutation() {
            var ts = Instant.ofEpochSecond(1700000000L);

            var pending = new UserStatusMuteHandler(TestWamService.create(client)).getMutationForStatusMute(CONTACT, true, ts);

            assertEquals(SyncdOperation.SET, pending.mutation().operation());
            assertEquals(ts, pending.mutation().timestamp());
            assertEquals(UserStatusMuteAction.ACTION_VERSION, pending.mutation().actionVersion());
            assertEquals("[\"userStatusMute\",\"" + CONTACT + "\"]", pending.mutation().index());
            var action = (UserStatusMuteAction) pending.mutation().value().flatMap(sav -> sav.action()).orElseThrow();
            assertTrue(action.muted(), "the action carries the muted=true flag verbatim");
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp-based")
    class ResolveConflicts {
        @Test
        @DisplayName("remote with the later timestamp wins (APPLY_REMOTE_DROP_LOCAL)")
        void remoteWins() {
            var earlier = mutationFor(CONTACT.toString(), Boolean.FALSE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));
            var later   = mutationFor(CONTACT.toString(), Boolean.TRUE,  SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));

            var resolution = new UserStatusMuteHandler(TestWamService.create(client)).resolveConflicts(earlier, later);

            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
        }

        @Test
        @DisplayName("local with the strictly later timestamp wins (SKIP_REMOTE)")
        void localWins() {
            var later   = mutationFor(CONTACT.toString(), Boolean.TRUE,  SyncdOperation.SET, Instant.ofEpochSecond(1700000010L));
            var earlier = mutationFor(CONTACT.toString(), Boolean.FALSE, SyncdOperation.SET, Instant.ofEpochSecond(1700000000L));

            var resolution = new UserStatusMuteHandler(TestWamService.create(client)).resolveConflicts(later, earlier);

            assertEquals(MutationConflictResolutionState.SKIP_REMOTE, resolution.state());
        }
    }

}
