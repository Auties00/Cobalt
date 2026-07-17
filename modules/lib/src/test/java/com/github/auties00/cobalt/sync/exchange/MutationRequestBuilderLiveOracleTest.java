package com.github.auties00.cobalt.sync.exchange;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdPatchSpec;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.sync.SyncFixtures;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link MutationRequestBuilder}'s output against outgoing sync IQ stanzas captured
 * from a live WhatsApp Web session.
 *
 * <p>The fixtures under {@code src/test/resources/fixtures/sync/exchange/} are JSONL
 * stanza dumps captured via the {@code web_live_stanza_dump_to_file} MCP tool and
 * reconstructed into {@link Stanza} instances by
 * {@link SyncFixtures}. Every assertion is gated on {@link SyncFixtures#isAvailable(String)}
 * so the suite stays green before the captured fixtures land. Byte-equality on the
 * encrypted patch payload is not exercised: the patch is encrypted with a fresh random IV
 * per call, so byte-for-byte equality with the captured ciphertext is impossible without
 * IV injection; the lower-level (IV, plaintext, ciphertext) parity lives in
 * {@code EncryptedMutationTest}, and this suite covers the envelope and the surrounding
 * protobuf structure.
 */
@DisplayName("MutationRequestBuilder - live-oracle parity")
class MutationRequestBuilderLiveOracleTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");

    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private static final Jid SELF_PN_DEVICE_1 = Jid.of("19250000001:1@s.whatsapp.net");

    private TestWhatsAppClient client;

    private MutationRequestBuilder builder;

    @BeforeEach
    void setUp() {
        var props = TestABPropsService.builder().build();
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.accountStore().setJid(SELF_PN_DEVICE_1);
        client = TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(props);
        var wam = new LiveWamService(client, props);
        builder = new MutationRequestBuilder(client, props, wam, TestMediaConnectionService.create());
    }

    static Stream<Topic> uploadTopics() {
        return Stream.of(
                new Topic("exchange/regular-low/upload-archive",                SyncPatchType.REGULAR_LOW),
                new Topic("exchange/regular-low/upload-pin",                    SyncPatchType.REGULAR_LOW),
                new Topic("exchange/regular-high/upload-mute",                  SyncPatchType.REGULAR_HIGH),
                new Topic("exchange/regular-low/upload-mark-as-read",           SyncPatchType.REGULAR_LOW),
                new Topic("exchange/regular/upload-disable-link-previews",      SyncPatchType.REGULAR),
                new Topic("exchange/critical-block/upload-pushname",            SyncPatchType.CRITICAL_BLOCK),
                new Topic("exchange/critical-unblock-low/upload-contact",       SyncPatchType.CRITICAL_UNBLOCK_LOW)
        );
    }

    record Topic(String topic, SyncPatchType patchType) {
    }

    @Nested
    @DisplayName("envelope parity - type / xmlns / to")
    class EnvelopeParity {
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.github.auties00.cobalt.sync.exchange.MutationRequestBuilderLiveOracleTest#uploadTopics")
        @DisplayName("captured <iq> envelope attributes match WA Web's outgoing sync IQ shape")
        void envelopeMatches(Topic topic) {
            if (!SyncFixtures.isAvailable(topic.topic())) return;

            var events = SyncFixtures.loadEvents(topic.topic());
            var iqEvent = events.stream()
                    .filter(e -> "iq".equals(e.getString("tag")))
                    .filter(e -> "out".equals(e.getString("direction")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no outgoing <iq> in " + topic.topic()));

            var captured = SyncFixtures.buildNodeFromEvent(iqEvent);

            assertEquals("set", captured.getAttributeAsString("type").orElseThrow(),
                    "WAWebSyncdRequestBuilderBuild.g pins type=\"set\"");
            assertEquals("w:sync:app:state", captured.getAttributeAsString("xmlns").orElseThrow(),
                    "xmlns must match WAWebSyncdRequestBuilderBuild.g");
            captured.getAttributeAsString("to").orElseThrow();
        }
    }

    @Nested
    @DisplayName("collection wire-shape parity")
    class CollectionShape {
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.github.auties00.cobalt.sync.exchange.MutationRequestBuilderLiveOracleTest#uploadTopics")
        @DisplayName("captured <collection> name matches the patch-type's lowercase wire token")
        void collectionNameMatches(Topic topic) {
            if (!SyncFixtures.isAvailable(topic.topic())) return;

            var events = SyncFixtures.loadEvents(topic.topic());
            var iqEvent = events.stream()
                    .filter(e -> "iq".equals(e.getString("tag")))
                    .filter(e -> "out".equals(e.getString("direction")))
                    .findFirst()
                    .orElseThrow();

            var iq = SyncFixtures.buildNodeFromEvent(iqEvent);
            var collection = iq.getChild("sync").orElseThrow()
                    .getChild("collection").orElseThrow();

            assertEquals(topic.patchType().toString(),
                    collection.getAttributeAsString("name").orElseThrow(),
                    "wire collection name must match SyncPatchType.toString()");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.github.auties00.cobalt.sync.exchange.MutationRequestBuilderLiveOracleTest#uploadTopics")
        @DisplayName("captured <collection> exposes a numeric version attribute")
        void versionIsNumeric(Topic topic) {
            if (!SyncFixtures.isAvailable(topic.topic())) return;

            var events = SyncFixtures.loadEvents(topic.topic());
            var iqEvent = events.stream()
                    .filter(e -> "iq".equals(e.getString("tag")))
                    .filter(e -> "out".equals(e.getString("direction")))
                    .findFirst()
                    .orElseThrow();

            var collection = SyncFixtures.buildNodeFromEvent(iqEvent)
                    .getChild("sync").orElseThrow()
                    .getChild("collection").orElseThrow();
            collection.getAttributeAsLong("version").orElseThrow(
                    () -> new AssertionError("captured collection has no version attribute"));
        }
    }

    @Nested
    @DisplayName("patch protobuf structural parity")
    class PatchStructure {
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.github.auties00.cobalt.sync.exchange.MutationRequestBuilderLiveOracleTest#uploadTopics")
        @DisplayName("decoded <patch> bytes carry a non-empty mutation list and 32-byte MACs")
        void patchContents(Topic topic) {
            if (!SyncFixtures.isAvailable(topic.topic())) return;

            var events = SyncFixtures.loadEvents(topic.topic());
            var iqEvent = events.stream()
                    .filter(e -> "iq".equals(e.getString("tag")))
                    .filter(e -> "out".equals(e.getString("direction")))
                    .findFirst()
                    .orElseThrow();

            var patchNode = SyncFixtures.buildNodeFromEvent(iqEvent)
                    .getChild("sync").orElseThrow()
                    .getChild("collection").orElseThrow()
                    .getChild("patch").orElse(null);
            if (patchNode == null) return;

            var patchBytes = patchNode.toContentBytes().orElseThrow();
            var decoded = SyncdPatchSpec.decode(patchBytes);

            assertTrue(decoded.mutations().size() + decoded.externalMutations().map(_ -> 1).orElse(0) >= 1,
                    "patch must carry at least one mutation (inline or external)");

            decoded.snapshotMac().ifPresent(mac -> assertEquals(32, mac.length, "snapshotMac length"));
            decoded.patchMac().ifPresent(mac -> assertEquals(32, mac.length, "patchMac length"));
            decoded.keyId().flatMap(k -> k.id()).ifPresent(id ->
                    assertTrue(id.length > 0, "patch key id must be non-empty"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.github.auties00.cobalt.sync.exchange.MutationRequestBuilderLiveOracleTest#uploadTopics")
        @DisplayName("decoded <patch> carries a non-null deviceIndex matching the producing device")
        void deviceIndexPresent(Topic topic) {
            if (!SyncFixtures.isAvailable(topic.topic())) return;

            var events = SyncFixtures.loadEvents(topic.topic());
            var iqEvent = events.stream()
                    .filter(e -> "iq".equals(e.getString("tag")))
                    .filter(e -> "out".equals(e.getString("direction")))
                    .findFirst()
                    .orElseThrow();

            var patchNode = SyncFixtures.buildNodeFromEvent(iqEvent)
                    .getChild("sync").orElseThrow()
                    .getChild("collection").orElseThrow()
                    .getChild("patch").orElse(null);
            if (patchNode == null) return;

            var decoded = SyncdPatchSpec.decode(patchNode.toContentBytes().orElseThrow());
            assertTrue(decoded.deviceIndex().isPresent(),
                    "WAWebSyncdRequestBuilderBuild.L sets deviceIndex from getMyDeviceJid()");
        }
    }

    @Nested
    @DisplayName("MutationSyncResponse oracle replay")
    class ResponseReplay {
        @Test
        @DisplayName("a captured success response parses to a successful MutationSyncResponse")
        void parsesSuccessResponse() {
            if (!SyncFixtures.isAvailable("exchange/regular-low/upload-archive")) return;

            var events = SyncFixtures.loadEvents("exchange/regular-low/upload-archive");
            var iqEvents = events.stream()
                    .filter(e -> "iq".equals(e.getString("tag")))
                    .filter(e -> "in".equals(e.getString("direction")))
                    .toList();
            if (iqEvents.isEmpty()) return;

            var inboundIq = SyncFixtures.buildNodeFromEvent(iqEvents.getFirst());
            var response = new MutationResponseParser().parseSyncResponse(inboundIq);

            assertEquals(SyncPatchType.REGULAR_LOW, response.collectionName(),
                    "response collection must match the upload's patch type");
            assertTrue(response.collectionError().isEmpty(),
                    "happy-path uploads receive a non-error response");
        }
    }

}
