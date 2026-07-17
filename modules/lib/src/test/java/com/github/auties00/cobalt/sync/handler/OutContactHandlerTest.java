package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.linked.contact.OutContactBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.chat.ArchiveChatActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.OutContactAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.OutContactActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
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
 * Covers {@link OutContactHandler}: the AB-prop gate
 * ({@link ABProp#OUT_CONTACT_INVITES_ENABLED} {@code == 1}), the {@link SyncdOperation#SET} upsert
 * and {@link SyncdOperation#REMOVE} paths, the JID validation (a non-phone-user JID surfaces as
 * {@link SyncActionState#MALFORMED}), and the {@code firstName} fallback derived from the first
 * whitespace-separated token of {@code fullName}.
 *
 * <p>No public outgoing-mutation factory exists for this action, so each test builds mutations
 * directly via the local {@code setMutation} / {@code removeMutation} helpers.
 */
@DisplayName("OutContactHandler")
class OutContactHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid PEER = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid GROUP = Jid.of("99001112224@g.us");

    private LinkedWhatsAppStore store;
    private TestABPropsService props;
    private LinkedWhatsAppClient client;
    private OutContactHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        props = TestABPropsService.builder()
                .with(ABProp.OUT_CONTACT_INVITES_ENABLED, 1L) // open the gate
                .build();
        client = TestWhatsAppClient.create().withStore(store).withAbPropsService(props);
        handler = new OutContactHandler(props);
    }

    private static String index(Jid jid) {
        return "[\"out_contact\",\"" + jid.toString() + "\"]";
    }

    private static DecryptedMutation.Trusted setMutation(Jid jid, String fullName, String firstName, Instant ts) {
        var builder = new OutContactActionBuilder();
        if (fullName != null) builder.fullName(fullName);
        if (firstName != null) builder.firstName(firstName);
        var value = new SyncActionValueBuilder().timestamp(ts).outContactAction(builder.build()).build();
        return new DecryptedMutation.Trusted(index(jid), value, SyncdOperation.SET, ts, OutContactAction.ACTION_VERSION);
    }

    private static DecryptedMutation.Trusted removeMutation(Jid jid, Instant ts) {
        var value = new SyncActionValueBuilder().timestamp(ts).outContactAction(new OutContactActionBuilder().build()).build();
        return new DecryptedMutation.Trusted(index(jid), value, SyncdOperation.REMOVE, ts, OutContactAction.ACTION_VERSION);
    }

    @Nested
    @DisplayName("metadata â€” wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the OutContactAction wire constant")
        void actionName() {
            assertEquals(OutContactAction.ACTION_NAME, handler.actionName());
            assertEquals("out_contact", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR_LOW")
        void collectionName() {
            assertEquals(OutContactAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.REGULAR_LOW, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared action version (1)")
        void version() {
            assertEquals(OutContactAction.ACTION_VERSION, handler.version());
            assertEquals(1, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” gating on out_contact_invites_enabled")
    class Gating {
        @Test
        @DisplayName("when the AB-prop is not 1, every mutation returns UNSUPPORTED")
        void gateClosed() {
            props.set(ABProp.OUT_CONTACT_INVITES_ENABLED, 0L);
            var result = handler.applyMutation(client,
                    setMutation(PEER, "Maria Garcia", null, Instant.now()));
            assertEquals(SyncActionState.UNSUPPORTED, result.actionState());
            assertTrue(store.contactStore().findOutContact(PEER).isEmpty());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” happy SET")
    class ApplySetHappy {
        @Test
        @DisplayName("SET adds the out-contact record with the supplied fullName/firstName")
        void setsExplicitFields() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client,
                    setMutation(PEER, "Maria Garcia", "Maria", ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var contact = store.contactStore().findOutContact(PEER).orElseThrow();
            assertEquals("Maria Garcia", contact.fullName().orElseThrow());
            assertEquals("Maria", contact.firstName().orElseThrow());
        }

        @Test
        @DisplayName("a missing firstName is derived from the first whitespace-separated token of fullName")
        void firstNameDerivedFromFullName() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            handler.applyMutation(client, setMutation(PEER, "Maria Garcia", null, ts));
            var contact = store.contactStore().findOutContact(PEER).orElseThrow();
            assertEquals("Maria", contact.firstName().orElseThrow(),
                    "WA Web's p(e): t = e.trim().split(\" \")[0]");
        }

        @Test
        @DisplayName("empty strings on the action coalesce to null")
        void emptyStringsCoalesce() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            handler.applyMutation(client, setMutation(PEER, "", "", ts));
            var contact = store.contactStore().findOutContact(PEER).orElseThrow();
            assertTrue(contact.fullName().isEmpty());
            assertTrue(contact.firstName().isEmpty(),
                    "WA Web's m(e): e == null || e === \"\" -> null");
        }
    }

    @Nested
    @DisplayName("applyMutation â€” orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("out-contact is its own store; the record IS the entity, so no orphan path applies")
        void noOrphan() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client, setMutation(PEER, "X", null, ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” malformed action value")
    class MalformedActionValue {
        @Test
        @DisplayName("a SET mutation carrying a different action returns MALFORMED")
        void wrongActionIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .archiveChatAction(new ArchiveChatActionBuilder().archived(true).build())
                    .build();
            var mutation = new DecryptedMutation.Trusted(index(PEER), value, SyncdOperation.SET, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }

        @Test
        @DisplayName("a non-user-server JID at indexParts[1] returns MALFORMED")
        void nonUserServerJidIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client, setMutation(GROUP, "X", null, ts));
            assertEquals(SyncActionState.MALFORMED, result.actionState(),
                    "WA Web requires the JID to be a phoneUser; group/g.us JIDs are rejected");
        }
    }

    @Nested
    @DisplayName("applyMutation â€” malformed action index")
    class MalformedActionIndex {
        @Test
        @DisplayName("a missing JID at indexParts[1] returns MALFORMED")
        void missingJidIsMalformed() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var value = new SyncActionValueBuilder().timestamp(ts).outContactAction(new OutContactActionBuilder().fullName("X").build()).build();
            var mutation = new DecryptedMutation.Trusted("[\"out_contact\"]", value, SyncdOperation.SET, ts, 1);
            var result = handler.applyMutation(client, mutation);
            assertEquals(SyncActionState.MALFORMED, result.actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation â€” REMOVE drops the out-contact")
    class RemoveOperation {
        @Test
        @DisplayName("REMOVE clears the record keyed by the supplied JID and returns SUCCESS")
        void removeDropsContact() {
            store.contactStore().addOutContact(new OutContactBuilder().jid(PEER).fullName("Maria").firstName("Maria").build());
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client, removeMutation(PEER, ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(store.contactStore().findOutContact(PEER).isEmpty());
        }

        @Test
        @DisplayName("REMOVE on an absent JID still returns SUCCESS")
        void removeAbsentSucceeds() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var result = handler.applyMutation(client, removeMutation(PEER, ts));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts â€” inherits default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote â†’ APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = setMutation(PEER, "A", null, Instant.ofEpochSecond(1_000));
            var remote = setMutation(PEER, "B", null, Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("older remote â†’ SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = setMutation(PEER, "A", null, Instant.ofEpochSecond(2_000));
            var remote = setMutation(PEER, "B", null, Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }
    }

    @Nested
    @DisplayName("applyMutationBatch â€” inherits default sequential apply")
    class ApplyBatch {
        @Test
        @DisplayName("default batch path applies each mutation in order")
        void sequentialApply() {
            var ts = Instant.ofEpochSecond(1_700_000_000L);
            var results = handler.applyMutationBatch(client, List.of(
                    setMutation(PEER, "Maria", null, ts),
                    removeMutation(PEER, ts.plusSeconds(1))
            ));
            assertEquals(2, results.size());
            assertEquals(SyncActionState.SUCCESS, results.get(0).actionState());
            assertEquals(SyncActionState.SUCCESS, results.get(1).actionState());
            assertTrue(store.contactStore().findOutContact(PEER).isEmpty(),
                    "REMOVE in the batch tail must override the earlier SET");
        }
    }

}
