package com.github.auties00.cobalt.sync.handler;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wam.TestWamService;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.ContactAction;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.ContactActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.contact.PinActionBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.factory.ContactActionMutationFactory;
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
 * Tests for {@link ContactActionHandler} - Cobalt's adapter for
 * {@code WAWebContactSync}.
 *
 * <p>The handler creates or updates contacts from address-book sync mutations
 * and clears name/username fields on REMOVE. These tests pin the wire metadata,
 * the SET happy path (including username gating, LID mapping registration, and
 * short-name derivation), the REMOVE path (including the
 * {@code addedByUsername} carve-out), the LID/bot skip rules, malformed
 * fallbacks, the default timestamp-based conflict resolution, and the static
 * builder helper.
 */
@DisplayName("ContactActionHandler")
class ContactActionHandlerTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594000@lid");
    private static final Jid CONTACT_PN = Jid.of("33330000@s.whatsapp.net");
    private static final Jid CONTACT_LID = Jid.of("70000000000000@lid");

    private LinkedWhatsAppStore store;
    private TestABPropsService props;
    private LinkedWhatsAppClient client;
    private ContactActionHandler handler;
    private ContactActionMutationFactory factory;

    @BeforeEach
    void setUp() {
        store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        props = TestABPropsService.builder().build();
        client = TestWhatsAppClient.create().withStore(store).withAbPropsService(props);
        handler = new ContactActionHandler(props, new UserStatusMuteHandler(TestWamService.create(client)));
        factory = new ContactActionMutationFactory();
    }

    /**
     * Builds a SET mutation whose value carries the given contact action under the
     * canonical {@code ["contact", contactJid]} index.
     *
     * @param contactJid the contact JID
     * @param action     the action payload, may be {@code null}
     * @param op         the sync operation
     * @param ts         the timestamp
     * @return the trusted mutation
     */
    private DecryptedMutation.Trusted build(Jid contactJid, ContactAction action, SyncdOperation op, Instant ts) {
        var valueBuilder = new SyncActionValueBuilder().timestamp(ts);
        if (action != null) valueBuilder.contactAction(action);
        var index = JSON.toJSONString(List.of("contact", contactJid.toString()));
        return new DecryptedMutation.Trusted(index, valueBuilder.build(), op, ts, handler.version());
    }

    @Nested
    @DisplayName("metadata - wire identity")
    class Metadata {
        @Test
        @DisplayName("actionName() returns the ContactAction wire constant")
        void actionName() {
            assertEquals(ContactAction.ACTION_NAME, handler.actionName());
            assertEquals("contact", handler.actionName());
        }

        @Test
        @DisplayName("collectionName() returns the ContactAction collection")
        void collectionName() {
            assertEquals(ContactAction.COLLECTION_NAME, handler.collectionName());
            assertEquals(SyncPatchType.CRITICAL_UNBLOCK_LOW, handler.collectionName());
        }

        @Test
        @DisplayName("version() returns the declared ContactAction version")
        void version() {
            assertEquals(ContactAction.ACTION_VERSION, handler.version());
        }
    }

    @Nested
    @DisplayName("applyMutation - happy SET")
    class ApplySetHappy {
        @Test
        @DisplayName("creates the contact when it does not exist and writes name fields")
        void createsAndWritesNames() {
            assertTrue(store.contactStore().findContactByJid(CONTACT_PN).isEmpty(), "precondition: no contact");

            var action = new ContactActionBuilder()
                    .fullName("Maria Garcia")
                    .firstName("Maria")
                    .build();

            var result = handler.applyMutation(client, build(CONTACT_PN, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            var contact = store.contactStore().findContactByJid(CONTACT_PN).orElseThrow();
            assertEquals("Maria Garcia", contact.fullName().orElseThrow());
            assertEquals("Maria", contact.shortName().orElseThrow());
        }

        @Test
        @DisplayName("when firstName is absent, shortName is derived from the first word of fullName")
        void derivesShortNameFromFullName() {
            var action = new ContactActionBuilder().fullName("Maria Garcia").build();

            handler.applyMutation(client, build(CONTACT_PN, action, SyncdOperation.SET, Instant.now()));

            var contact = store.contactStore().findContactByJid(CONTACT_PN).orElseThrow();
            assertEquals("Maria", contact.shortName().orElseThrow(),
                    "WAWebContactShortName.getShortName takes the first whitespace-separated token");
        }

        @Test
        @DisplayName("lidJid: contact LID is set and a LID<->PN mapping is registered")
        void registersLidMapping() {
            var action = new ContactActionBuilder()
                    .fullName("X")
                    .lidJid(CONTACT_LID)
                    .build();

            handler.applyMutation(client, build(CONTACT_PN, action, SyncdOperation.SET, Instant.now()));

            var contact = store.contactStore().findContactByJid(CONTACT_PN).orElseThrow();
            assertEquals(CONTACT_LID, contact.lid().orElseThrow());
            assertEquals(CONTACT_PN, store.contactStore().findPhoneByLid(CONTACT_LID).orElseThrow(),
                    "createLidPnMappings: a regular PN-form contact with a LID must register the bidirectional mapping");
        }

        @Test
        @DisplayName("username with leading '@' is stripped before persistence when the AB-prop is on")
        void usernameNormalisationGatedByProp() {
            props.set(ABProp.USERNAME_CONTACT_SYNCD_SUPPORT_ENABLE, true);
            var action = new ContactActionBuilder().fullName("X").username("@maria").build();

            handler.applyMutation(client, build(CONTACT_PN, action, SyncdOperation.SET, Instant.now()));

            var contact = store.contactStore().findContactByJid(CONTACT_PN).orElseThrow();
            assertEquals("maria", contact.username().orElseThrow(),
                    "WA Web's setUsernamesJob strips the leading '@' before writing");
        }

        @Test
        @DisplayName("username is ignored when the AB-prop is off")
        void usernameIgnoredWhenPropOff() {
            // Default: USERNAME_CONTACT_SYNCD_SUPPORT_ENABLE = false
            var action = new ContactActionBuilder().fullName("X").username("@maria").build();

            handler.applyMutation(client, build(CONTACT_PN, action, SyncdOperation.SET, Instant.now()));

            var contact = store.contactStore().findContactByJid(CONTACT_PN).orElseThrow();
            assertTrue(contact.username().isEmpty(),
                    "the username branch is gated on the AB-prop being true");
        }

        @Test
        @DisplayName("LID-server JIDs in the index are SKIPPED on SET (the lid_contact handler owns them)")
        void lidIndexIsSkippedOnSet() {
            var action = new ContactActionBuilder().fullName("X").build();

            var result = handler.applyMutation(client, build(CONTACT_LID, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SKIPPED, result.actionState(),
                    "WAWebContactSync delegates LID-server contacts to WAWebLidContactSync");
        }
    }

    @Nested
    @DisplayName("applyMutation - orphan dimension is n/a")
    class OrphanDimension {
        @Test
        @DisplayName("the contact is upserted rather than orphaned when absent")
        void upsertNotOrphan() {
            var action = new ContactActionBuilder().fullName("X").build();
            var result = handler.applyMutation(client, build(CONTACT_PN, action, SyncdOperation.SET, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertFalse(result.isOrphan());
            assertNull(result.modelId());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed value")
    class MalformedValue {
        @Test
        @DisplayName("a SET value carrying the wrong action returns MALFORMED")
        void wrongActionType() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder()
                    .timestamp(ts)
                    .pinAction(new PinActionBuilder().pinned(true).build())
                    .build();
            var index = JSON.toJSONString(List.of("contact", CONTACT_PN.toString()));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("a SET value with no contactAction returns MALFORMED")
        void missingActionPayload() {
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).build();
            var index = JSON.toJSONString(List.of("contact", CONTACT_PN.toString()));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - malformed index")
    class MalformedIndex {
        @Test
        @DisplayName("an index with an empty contactJid slot returns MALFORMED")
        void emptyContactJid() {
            var action = new ContactActionBuilder().fullName("X").build();
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).contactAction(action).build();
            var index = JSON.toJSONString(List.of("contact", ""));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }

        @Test
        @DisplayName("an index missing the contactJid slot returns MALFORMED")
        void shortIndex() {
            var action = new ContactActionBuilder().fullName("X").build();
            var ts = Instant.now();
            var value = new SyncActionValueBuilder().timestamp(ts).contactAction(action).build();
            var index = JSON.toJSONString(List.of("contact"));
            var mutation = new DecryptedMutation.Trusted(index, value, SyncdOperation.SET, ts, handler.version());

            assertEquals(SyncActionState.MALFORMED, handler.applyMutation(client, mutation).actionState());
        }
    }

    @Nested
    @DisplayName("applyMutation - REMOVE clears address-book fields")
    class ApplyRemove {
        @Test
        @DisplayName("REMOVE on a regular PN contact clears name and username fields")
        void removeClearsFields() {
            var contact = store.contactStore().addNewContact(CONTACT_PN);
            contact.setFullName("Maria");
            contact.setShortName("Maria");
            contact.setUsername("maria");

            var result = handler.applyMutation(client, build(CONTACT_PN, null, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertTrue(contact.fullName().isEmpty(), "fullName must be cleared on REMOVE");
            assertTrue(contact.shortName().isEmpty(), "shortName must be cleared on REMOVE");
            assertTrue(contact.username().isEmpty(), "username must be cleared on REMOVE");
        }

        @Test
        @DisplayName("REMOVE on a LID-server JID is SKIPPED (the lid_contact handler owns LIDs)")
        void removeOnLidIsSkipped() {
            var result = handler.applyMutation(client, build(CONTACT_LID, null, SyncdOperation.REMOVE, Instant.now()));
            assertEquals(SyncActionState.SKIPPED, result.actionState());
        }

        @Test
        @DisplayName("REMOVE on a username-added contact retains the address-book fields when the AB-prop is on")
        void removeRetainsAddressBookForUsernameContact() {
            props.set(ABProp.USERNAME_CONTACT_SYNCD_SUPPORT_ENABLE, true);

            var contact = store.contactStore().addNewContact(CONTACT_PN);
            contact.setFullName("Maria");
            contact.setShortName("Maria");
            contact.setUsername("maria");
            contact.setAddedByUsername(true);

            var result = handler.applyMutation(client, build(CONTACT_PN, null, SyncdOperation.REMOVE, Instant.now()));

            assertEquals(SyncActionState.SUCCESS, result.actionState());
            assertEquals("Maria", contact.fullName().orElseThrow(),
                    "username-added contacts retain their address-book entries on REMOVE under the AB-prop");
        }

        @Test
        @DisplayName("REMOVE on an absent contact still returns SUCCESS")
        void removeAbsentReturnsSuccess() {
            var result = handler.applyMutation(client, build(CONTACT_PN, null, SyncdOperation.REMOVE, Instant.now()));
            assertEquals(SyncActionState.SUCCESS, result.actionState());
        }
    }

    @Nested
    @DisplayName("resolveConflicts - default timestamp comparison")
    class ResolveConflicts {
        @Test
        @DisplayName("newer remote -> APPLY_REMOTE_DROP_LOCAL")
        void newerRemoteApplies() {
            var local = build(CONTACT_PN, action("A"), SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            var remote = build(CONTACT_PN, action("B"), SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(local, remote).state());
        }

        @Test
        @DisplayName("equal timestamps -> APPLY_REMOTE_DROP_LOCAL (remote wins on tie)")
        void equalTiesGoToRemote() {
            var ts = Instant.ofEpochSecond(1_500);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL,
                    handler.resolveConflicts(build(CONTACT_PN, action("A"), SyncdOperation.SET, ts),
                                             build(CONTACT_PN, action("B"), SyncdOperation.SET, ts)).state());
        }

        @Test
        @DisplayName("older remote -> SKIP_REMOTE")
        void olderRemoteSkipped() {
            var local = build(CONTACT_PN, action("A"), SyncdOperation.SET, Instant.ofEpochSecond(2_000));
            var remote = build(CONTACT_PN, action("B"), SyncdOperation.SET, Instant.ofEpochSecond(1_000));
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE,
                    handler.resolveConflicts(local, remote).state());
        }

        private ContactAction action(String name) {
            return new ContactActionBuilder().fullName(name).build();
        }
    }

    @Nested
    @DisplayName("deriveShortName - first-token + letter-presence")
    class DeriveShortName {
        @Test
        @DisplayName("empty / null input returns the empty string")
        void emptyInputs() {
            assertEquals("", ContactActionHandler.deriveShortName(null));
            assertEquals("", ContactActionHandler.deriveShortName(""));
        }

        @Test
        @DisplayName("first whitespace-separated token is returned when it contains a letter")
        void firstToken() {
            assertEquals("Maria", ContactActionHandler.deriveShortName("Maria Garcia"));
            assertEquals("Anne-Marie", ContactActionHandler.deriveShortName("Anne-Marie Pierre"));
        }

        @Test
        @DisplayName("token without any letter character returns the empty string")
        void letterlessToken() {
            assertEquals("", ContactActionHandler.deriveShortName("123 456"),
                    "first token has no letter - WA Web's WAWebAlphaRegex fails to match");
        }
    }

}
