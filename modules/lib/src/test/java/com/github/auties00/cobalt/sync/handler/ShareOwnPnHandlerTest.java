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
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValue;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link ShareOwnPnHandler}: applying an incoming shareOwnPn
 * mutation and asserting the contact store side-effect, gated on the
 * {@link ABProp#SHARE_OWN_PN_SYNC} AB-prop. The {@code shareOwnPn} action
 * carries no value payload of its own, so the
 * {@link SyncActionValue} fixture
 * sets only the wire timestamp.
 */
@DisplayName("ShareOwnPnHandler")
class ShareOwnPnHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid CONTACT_LID = Jid.of("70000000000000@lid");
    private static final Jid CONTACT_PN = Jid.of("33330000@s.whatsapp.net");

    private LinkedWhatsAppStore store;
    private TestABPropsService props;
    private LinkedWhatsAppClient client;
    private ShareOwnPnHandler handler;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        props = TestABPropsService.builder()
                .with(ABProp.SHARE_OWN_PN_SYNC, true)
                .build();
        client = TestWhatsAppClient.create().withStore(store).withAbPropsService(props);
        handler = new ShareOwnPnHandler(props);
    }

    private DecryptedMutation.Trusted build(Jid lidJid, SyncdOperation op, Instant ts) {
        var value = new SyncActionValueBuilder().timestamp(ts).build();
        var index = JSON.toJSONString(List.of("shareOwnPn", lidJid.toString()));
        return new DecryptedMutation.Trusted(index, value, op, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the WAWebShareOwnPnSync wire constant")
        void actionName() {
            assertEquals("shareOwnPn", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns REGULAR")
        void collectionName() {
            assertEquals(SyncPatchType.REGULAR, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns 8 (the declared WAWebShareOwnPnSync version)")
        void version() {
            assertEquals(8, handler.version());
        }
    }

    @Nested
    @DisplayName("AB-prop gating - share_own_pn_sync")
    class AbPropGating {
        @Test
        @DisplayName("when the prop is off, the mutation returns UNSUPPORTED")
        void propOffReturnsUnsupported() {
            props.set(ABProp.SHARE_OWN_PN_SYNC, false);

            var result = handler.applyMutation(client, build(CONTACT_LID, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState(),
                    "WAWebShareOwnPnSync.applyMutations returns Unsupported when the gating prop is off");
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET")
    class ApplySetHappy {
        @Test
        @DisplayName("upserts the LID contact with phoneNumberShared=true when none exists")
        void upsertsContact() {
            assertTrue(store.contactStore().findContactByJid(CONTACT_LID).isEmpty(), "precondition: no contact");

            var result = handler.applyMutation(client, build(CONTACT_LID, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var contact = store.contactStore().findContactByJid(CONTACT_LID).orElseThrow();
            assertTrue(contact.isPhoneNumberShared(),
                    "WAWebShareOwnPnSync.applyMutations sets shareOwnPn=true via bulkCreateOrMerge");
        }

        @Test
        @DisplayName("sets phoneNumberShared=true on an existing contact without clobbering other fields")
        void mergesIntoExisting() {
            var contact = store.contactStore().addNewContact(CONTACT_LID);
            contact.setFullName("Maria");
            assertFalse(contact.isPhoneNumberShared(), "precondition: shareOwnPn unset");

            var result = handler.applyMutation(client, build(CONTACT_LID, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(contact.isPhoneNumberShared());
            assertEquals("Maria", contact.fullName().orElseThrow(),
                    "merge semantics - non-target fields must be left untouched");
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("the contact is upserted rather than orphaned when absent")
        void noOrphanPath() {
            var result = handler.applyMutation(client, build(CONTACT_LID, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(result.isOrphan());
            assertNull(result.modelId());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value dimension is n/a")
    class MalformedValue {
        @Test
        @DisplayName("the action has no value payload - value contents are ignored")
        void valueContentsIgnored() {
            // shareOwnPn carries no value payload; the handler never reads value.action(),
            // so a well-formed index alone yields SUCCESS regardless of the value contents.
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).build();
            var index = JSON.toJSONString(List.of("shareOwnPn", CONTACT_LID.toString()));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.SUCCESS, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("an empty lidJid slot returns MALFORMED")
        void emptyLidJid() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).build();
            var index = JSON.toJSONString(List.of("shareOwnPn", ""));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a non-LID JID in the index returns MALFORMED")
        void nonLidJid() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).build();
            var index = JSON.toJSONString(List.of("shareOwnPn", CONTACT_PN.toString()));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState(),
                    "createUserLidOrThrow would throw on a non-@lid JID; Cobalt maps that to MALFORMED");
        }

        @Test
        @DisplayName("a short index missing the lidJid slot returns MALFORMED")
        void shortIndex() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).build();
            var index = JSON.toJSONString(List.of("shareOwnPn"));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE operation returns UNSUPPORTED")
        void removeUnsupported() {
            var result = handler.applyMutation(client, build(CONTACT_LID, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.UNSUPPORTED, result.actionState(),
                    "WA Web's WAWebShareOwnPnSync.applyMutations counts non-set operations as Unsupported");
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = build(CONTACT_LID, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = build(CONTACT_LID, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("equal timestamps -> APPLY_REMOTE_DROP_LOCAL (remote wins on tie)")
        void equalTiesGoToRemote() {
            var ts = Instant.ofEpochSecond(1_500);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(
                            build(CONTACT_LID, SyncdOperation.SET, ts),
                            build(CONTACT_LID, SyncdOperation.SET, ts)).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = build(CONTACT_LID, SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = build(CONTACT_LID, SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }
    }

}
