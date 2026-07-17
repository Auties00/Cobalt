package com.github.auties00.cobalt.calls.signaling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SignalingType taxonomy")
class SignalingTypeTest {
    @ParameterizedTest
    @EnumSource(SignalingType.class)
    @DisplayName("each constant round-trips through its native index")
    void indexRoundTrip(SignalingType type) {
        assertSame(type, SignalingType.ofIndex(type.index()).orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(SignalingType.class)
    @DisplayName("each call-child constant resolves its own wire tag, except the shared extension element")
    void wireTagRoundTrip(SignalingType type) {
        if (type.mechanism() != SignalingType.Mechanism.CALL_CHILD) {
            assertTrue(type.wireTag().isEmpty(), () -> "envelope leg " + type + " must carry no wire tag");
            return;
        }
        var tag = type.wireTag().orElseThrow();
        var resolved = SignalingType.ofWireTag(tag).orElseThrow();
        // ADD_EXTENSION and REMOVE_EXTENSION both emit the <extension> element; the inbound index keeps
        // the lower-id ADD_EXTENSION as the canonical owner, so REMOVE_EXTENSION resolves to it by tag.
        if (type == SignalingType.REMOVE_EXTENSION) {
            assertSame(SignalingType.ADD_EXTENSION, resolved);
        } else {
            assertSame(type, resolved);
        }
    }

    @Test
    @DisplayName("no two constants share a native index")
    void noDuplicateIndex() {
        var seen = new HashMap<Integer, SignalingType>();
        for (var type : SignalingType.values()) {
            var prior = seen.put(type.index(), type);
            assertTrue(prior == null,
                    () -> "duplicate index " + type.index() + " on " + type + " and " + seen.get(type.index()));
        }
    }

    @Test
    @DisplayName("no two call-child constants share a wire tag, except the shared extension element")
    void noDuplicateWireTag() {
        var seen = new HashSet<String>();
        for (var type : SignalingType.values()) {
            var tag = type.wireTag().orElse(null);
            if (tag == null) {
                continue;
            }
            if (type == SignalingType.REMOVE_EXTENSION) {
                // Shares the <extension> element with ADD_EXTENSION by design; disambiguated by message id.
                assertTrue(seen.contains("extension"), "ADD_EXTENSION must register the extension element first");
                continue;
            }
            assertTrue(seen.add(tag), () -> "duplicate wire tag " + tag + " on " + type);
        }
    }

    @Test
    @DisplayName("the five native table gaps resolve to no constant")
    void gapIndicesAreEmpty() {
        for (var gap : new int[]{40, 41, 43, 44, 64}) {
            assertTrue(SignalingType.ofIndex(gap).isEmpty(), () -> "gap id " + gap + " must not resolve");
        }
    }

    @Test
    @DisplayName("an unknown wire tag and null resolve to empty")
    void unknownWireTagIsEmpty() {
        assertTrue(SignalingType.ofWireTag("definitely_not_a_call_tag").isEmpty());
        assertTrue(SignalingType.ofWireTag(null).isEmpty());
    }

    @Test
    @DisplayName("the action tags carry their recovered literal")
    void confirmedAnchorTags() {
        assertEquals("offer", SignalingType.OFFER.wireTag().orElseThrow());
        assertEquals("accept", SignalingType.ACCEPT.wireTag().orElseThrow());
        assertEquals("terminate", SignalingType.TERMINATE.wireTag().orElseThrow());
        assertEquals("enc_rekey", SignalingType.REKEY.wireTag().orElseThrow());
    }

    @ParameterizedTest
    @CsvSource({"MUTE, 12", "WEB_CLIENT, 22", "CALL_RELAY, 35"})
    @DisplayName("the reserved internal ids map their native index but name no wire child")
    void internalIdsHaveNoWireTag(SignalingType type, int index) {
        assertSame(SignalingType.Mechanism.INTERNAL, type.mechanism());
        assertTrue(type.wireTag().isEmpty(), () -> type + " internal id must carry no wire tag");
        assertSame(type, SignalingType.ofIndex(index).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"mute", "web_client", "call_relay"})
    @DisplayName("the legacy mute and internal folded names resolve to no inbound type")
    void foldedNamesAreNotDispatched(String name) {
        // mute is superseded by mute_v2; web_client/call_relay have no wire literal at all. The engine
        // never compares these, so a faithful receiver must resolve them to nothing and drop the child.
        assertTrue(SignalingType.ofWireTag(name).isEmpty(),
                () -> name + " must not resolve to any signaling type");
    }

    @ParameterizedTest
    @EnumSource(SignalingType.class)
    @DisplayName("a wire tag is present exactly when the mechanism is a call child")
    void wireTagPresenceMatchesMechanism(SignalingType type) {
        var isCallChild = type.mechanism() == SignalingType.Mechanism.CALL_CHILD;
        assertEquals(isCallChild, type.wireTag().isPresent(),
                () -> type + " tag presence must match its CALL_CHILD mechanism");
    }

    @Test
    @DisplayName("envelope legs carry no wire tag")
    void envelopeLegsHaveNoTag() {
        assertTrue(SignalingType.OFFER_ACK.wireTag().isEmpty());
        assertSame(SignalingType.Mechanism.ACK, SignalingType.OFFER_ACK.mechanism());
        assertTrue(SignalingType.OFFER_RECEIPT.wireTag().isEmpty());
        assertSame(SignalingType.Mechanism.RECEIPT, SignalingType.OFFER_RECEIPT.mechanism());
    }

    // The 22 types whose native id routes to the check_msg_header (fn11495) br_table default and therefore
    // carry no validated fixed-header length; recovered from r-msgtable.json headerLengths (length == null).
    // These are the only constants whose fixedHeaderLength() is permitted to be empty.
    private static final Set<SignalingType> VARIABLE_LENGTH = EnumSet.of(
            SignalingType.NONE,
            SignalingType.HEARTBEAT,
            SignalingType.CALL_RELAY,
            SignalingType.REMOVE_USER,
            SignalingType.DTMF_TONE,
            SignalingType.BCALL_START,
            SignalingType.BCALL_START_ACK,
            SignalingType.BCALL_JOIN,
            SignalingType.BCALL_JOIN_ACK,
            SignalingType.BCALL_LEAVE,
            SignalingType.BCALL_LEAVE_ACK,
            SignalingType.BCALL_UPDATE,
            SignalingType.BCALL_END,
            SignalingType.BCALL_END_ACK,
            SignalingType.BCALL_NOTIFY,
            SignalingType.LINK_EDIT,
            SignalingType.CONNECT_STAT,
            SignalingType.PREACCEPT_ACK,
            SignalingType.WAITING_ROOM_LEAVE,
            SignalingType.WAITING_ROOM_TOGGLE,
            SignalingType.WAITING_ROOM_ADMIT,
            SignalingType.WAITING_ROOM_DENY);

    @ParameterizedTest
    @EnumSource(SignalingType.class)
    @DisplayName("a fixed-header length is present for every type except the recovered variable-length set")
    void fixedHeaderLengthPresenceIsTotal(SignalingType type) {
        if (VARIABLE_LENGTH.contains(type)) {
            assertTrue(type.fixedHeaderLength().isEmpty(),
                    () -> type + " routes to the br_table default and must report no fixed-header length");
        } else {
            assertTrue(type.fixedHeaderLength().isPresent(),
                    () -> type + " has a recovered check_msg_header case and must report a fixed-header length");
        }
    }

    @ParameterizedTest
    @DisplayName("each recovered fixed-header length matches the check_msg_header validator table")
    @CsvSource({
            "OFFER, 777400", "OFFER_RECEIPT, 100", "ACCEPT, 271184", "REJECT, 240", "TERMINATE, 6528",
            "TRANSPORT, 1112", "OFFER_ACK, 1539272", "OFFER_NACK, 1539272", "RELAY_LATENCY, 1144",
            "RELAY_LATENCY_ACK, 104", "INTERRUPTION, 108", "MUTE, 0", "PREACCEPT, 284", "ACCEPT_RECEIPT, 100",
            "VIDEO_STATE, 269696", "NOTIFY, 104", "GROUP_UPDATE, 497848", "REKEY, 548", "PEER_STATE, 1896",
            "VIDEO_STATE_ACK, 269592", "FLOW_CONTROL, 116", "WEB_CLIENT, 0", "ACCEPT_ACK, 313232",
            "LOBBY, 104", "LOBBY_ACK, 228272", "MUTE_V2, 104", "LINK_CREATE, 184", "LINK_CREATE_ACK, 132",
            "HEARTBEAT_ACK, 104", "LINK_QUERY, 132", "LINK_QUERY_ACK, 43840", "LINK_JOIN, 312",
            "LINK_JOIN_ACK, 531808", "REMOVE_USER_ACK, 108", "SCREEN_SHARE, 108", "SCREEN_SHARE_ACK, 108",
            "LINK_EDIT_ACK, 128", "GROUP_CALL_REMINDER, 271912", "USER_ACTION, 104", "RECONFIGURE_BOT, 112",
            "DURATION, 240", "GROUP_CALL_DURATION, 236", "READY, 104", "RELAY_INFO_UPDATE, 9792",
            "WAITING_ROOM_LEAVE_ACK, 104", "WAITING_ROOM_TOGGLE_ACK, 128", "WAITING_ROOM_ADMIT_ACK, 43628",
            "WAITING_ROOM_DENY_ACK, 43628", "WAITING_ROOM_UPDATE, 43728", "REMOVE, 356", "REMOVE_ACK, 1120",
            "CANCEL_OFFER, 356", "ADD_EXTENSION, 488936", "ADD_EXTENSION_ACK, 228272", "REMOVE_EXTENSION, 104",
            "REMOVE_EXTENSION_ACK, 104", "GROUP_CALL_TOS_ACCEPTED, 104", "GROUP_CALL_TOS_ACCEPTED_ACK, 104"
    })
    void recoveredFixedHeaderLengths(SignalingType type, int expected) {
        assertEquals(expected, type.fixedHeaderLength().orElseThrow(),
                () -> type + " fixed-header length must match the recovered validator constant");
    }

    @Test
    @DisplayName("the recovered zero-length cases are present-zero, not empty")
    void zeroLengthIsPresentNotEmpty() {
        // MUTE (id 12) and WEB_CLIENT (id 22) have an explicit br_table case with expected length 0,
        // which must read as OptionalInt.of(0) and be distinct from the empty variable-length result.
        assertEquals(0, SignalingType.MUTE.fixedHeaderLength().orElseThrow());
        assertEquals(0, SignalingType.WEB_CLIENT.fixedHeaderLength().orElseThrow());
        assertFalse(SignalingType.MUTE.fixedHeaderLength().isEmpty());
        assertFalse(SignalingType.WEB_CLIENT.fixedHeaderLength().isEmpty());
    }

    @ParameterizedTest
    @DisplayName("the acknowledgement legs ride the <ack> envelope and the receipt legs ride <receipt>")
    @CsvSource({
            "OFFER_ACK, ACK", "OFFER_NACK, ACK", "ACCEPT_ACK, ACK", "LINK_CREATE_ACK, ACK",
            "LINK_QUERY_ACK, ACK", "LINK_JOIN_ACK, ACK", "LINK_EDIT_ACK, ACK", "ADD_EXTENSION_ACK, ACK",
            "GROUP_CALL_TOS_ACCEPTED_ACK, ACK", "PREACCEPT_ACK, ACK", "BCALL_START_ACK, ACK",
            "OFFER_RECEIPT, RECEIPT", "RELAY_LATENCY_ACK, RECEIPT", "ACCEPT_RECEIPT, RECEIPT",
            "VIDEO_STATE_ACK, RECEIPT", "LOBBY_ACK, RECEIPT", "HEARTBEAT_ACK, RECEIPT",
            "REMOVE_USER_ACK, RECEIPT", "SCREEN_SHARE_ACK, RECEIPT", "REMOVE_ACK, RECEIPT",
            "WAITING_ROOM_LEAVE_ACK, RECEIPT", "REMOVE_EXTENSION_ACK, RECEIPT"
    })
    void recoveredEnvelopeMechanisms(SignalingType type, SignalingType.Mechanism mechanism) {
        assertSame(mechanism, type.mechanism(),
                () -> type + " must ride the recovered " + mechanism + " envelope and carry no wire tag");
        assertTrue(type.wireTag().isEmpty(), () -> type + " envelope leg must carry no wire tag");
    }
}
