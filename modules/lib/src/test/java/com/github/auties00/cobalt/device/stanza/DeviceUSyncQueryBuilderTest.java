package com.github.auties00.cobalt.device.stanza;

import com.github.auties00.cobalt.wire.linked.device.info.DeviceListHashInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural tests for {@link DeviceUSyncQueryBuilder}, covering the USync IQ
 * stanza shape, the per-user device-hash delta updates, the 500-user batching
 * rule, and factory input validation. The builder is a pure static factory, so
 * every assertion walks the returned {@link Stanza} tree directly and no captured
 * fixtures are needed.
 */
@DisplayName("DeviceUSyncQueryBuilder")
class DeviceUSyncQueryBuilderTest {
    // Matches the business VoIP capture session so inputs round-trip with the fixture machinery elsewhere.
    private static final Jid USER_A = Jid.of("19254863482@s.whatsapp.net");

    private static final Jid USER_B = Jid.of("393495089819@s.whatsapp.net");

    private static final Jid PSA_ACCOUNT = Jid.announcementsAccount();

    @Nested
    @DisplayName("attribute and child shape")
    class AttributeAndChildShape {
        @Test
        @DisplayName("produces a single IQ with the canonical usync attribute order")
        void singleIq() {
            var builders = DeviceUSyncQueryBuilder.build(linked(USER_A), "message", null, false);
            assertEquals(1, builders.size());

            var iq = builders.getFirst().build();
            assertEquals("iq", iq.description());
            assertEquals(JidServer.user().toString(), iq.getAttributeAsString("to").orElseThrow());
            assertEquals("usync", iq.getAttributeAsString("xmlns").orElseThrow());
            assertEquals("get", iq.getAttributeAsString("type").orElseThrow());
        }

        @Test
        @DisplayName("emits <usync> with sid, index, last, mode, context attributes")
        void usyncEnvelope() {
            var iq = DeviceUSyncQueryBuilder.build(linked(USER_A), "message", null, false)
                    .getFirst()
                    .build();

            var usync = childByDescription(iq, "usync");
            assertNotNull(usync.getAttributeAsString("sid").orElse(null));
            assertEquals("0", usync.getAttributeAsString("index").orElseThrow());
            assertEquals("true", usync.getAttributeAsString("last").orElseThrow());
            assertEquals("query", usync.getAttributeAsString("mode").orElseThrow());
            assertEquals("message", usync.getAttributeAsString("context").orElseThrow());
        }

        @Test
        @DisplayName("query has only <devices version=\"2\"/> when username protocol is disabled")
        void devicesOnly() {
            var iq = DeviceUSyncQueryBuilder.build(linked(USER_A), "query", null, false)
                    .getFirst()
                    .build();

            var query = childByDescription(childByDescription(iq, "usync"), "query");
            var children = query.streamChildren().toList();
            assertEquals(1, children.size());
            assertEquals("devices", children.getFirst().description());
            assertEquals("2", children.getFirst().getAttributeAsString("version").orElseThrow());
        }

        @Test
        @DisplayName("query has both <devices/> and <username/> when username protocol is enabled")
        void devicesAndUsername() {
            var iq = DeviceUSyncQueryBuilder.build(linked(USER_A), "query", null, true)
                    .getFirst()
                    .build();

            var query = childByDescription(childByDescription(iq, "usync"), "query");
            var children = query.streamChildren().toList();
            assertEquals(2, children.size());
            assertEquals("devices", children.getFirst().description());
            assertEquals("username", children.get(1).description());
        }

        @Test
        @DisplayName("each user appears as a <user jid=...> entry inside <list>")
        void userEntries() {
            var iq = DeviceUSyncQueryBuilder.build(linked(USER_A, USER_B), "message", null, false)
                    .getFirst()
                    .build();

            var list = childByDescription(childByDescription(iq, "usync"), "list");
            var users = list.streamChildren()
                    .filter(child -> "user".equals(child.description()))
                    .toList();
            assertEquals(2, users.size());

            var jids = users.stream()
                    .map(user -> user.getAttributeAsString("jid").orElseThrow())
                    .collect(Collectors.toSet());
            assertTrue(jids.contains(USER_A.toString()));
            assertTrue(jids.contains(USER_B.toString()));
        }

        @Test
        @DisplayName("filters out the PSA announcements account")
        void filtersPsa() {
            var iq = DeviceUSyncQueryBuilder.build(linked(USER_A, PSA_ACCOUNT), "message", null, false)
                    .getFirst()
                    .build();

            var list = childByDescription(childByDescription(iq, "usync"), "list");
            var users = list.streamChildren()
                    .filter(child -> "user".equals(child.description()))
                    .toList();
            assertEquals(1, users.size());
            assertEquals(USER_A.toString(), users.getFirst().getAttributeAsString("jid").orElseThrow());
        }
    }

    @Nested
    @DisplayName("hash info delta updates")
    class HashInfoDeltaUpdates {
        // Arbitrary but fixed instants so the ts/expected_ts attribute assertions stay byte-comparable.
        private final Instant ts = Instant.parse("2026-01-01T00:00:00Z");

        private final Instant expectedTs = Instant.parse("2026-01-02T00:00:00Z");

        @Test
        @DisplayName("omits the per-user <devices> element when hash info is null")
        void noHashInfo() {
            var iq = DeviceUSyncQueryBuilder.build(linked(USER_A), "message", null, false)
                    .getFirst()
                    .build();

            var user = firstUser(iq);
            assertEquals(0, user.streamChildren().count());
        }

        @Test
        @DisplayName("emits <devices device_hash ts expected_ts> when all three are known")
        void fullHashInfo() {
            var hashInfo = new DeviceListHashInfoBuilder()
                    .hash("dGVzdA==")
                    .timestamp(ts)
                    .expectedTimestamp(expectedTs)
                    .build();

            var iq = DeviceUSyncQueryBuilder.build(
                    linked(USER_A), "message", Map.of(USER_A.toUserJid(), hashInfo), false)
                    .getFirst()
                    .build();

            var devices = childByDescription(firstUser(iq), "devices");
            assertEquals("dGVzdA==", devices.getAttributeAsString("device_hash").orElseThrow());
            assertEquals(String.valueOf(ts.getEpochSecond()), devices.getAttributeAsString("ts").orElseThrow());
            assertEquals(String.valueOf(expectedTs.getEpochSecond()), devices.getAttributeAsString("expected_ts").orElseThrow());
        }

        @Test
        @DisplayName("omits the inner <devices> element when every hash field is null")
        void allHashFieldsAbsent() {
            var hashInfo = new DeviceListHashInfoBuilder().build();

            var iq = DeviceUSyncQueryBuilder.build(
                    linked(USER_A), "message", Map.of(USER_A.toUserJid(), hashInfo), false)
                    .getFirst()
                    .build();

            var user = firstUser(iq);
            assertFalse(user.hasChild("devices"));
        }

        @Test
        @DisplayName("only attaches per-user <devices> for users with a matching hash info entry")
        void mixedHashInfo() {
            var hashInfo = new DeviceListHashInfoBuilder()
                    .hash("dGVzdA==")
                    .timestamp(ts)
                    .build();

            var iq = DeviceUSyncQueryBuilder.build(
                    linked(USER_A, USER_B), "message",
                    Map.of(USER_A.toUserJid(), hashInfo), false)
                    .getFirst()
                    .build();

            var list = childByDescription(childByDescription(iq, "usync"), "list");
            var users = list.streamChildren()
                    .filter(child -> "user".equals(child.description()))
                    .toList();
            assertEquals(2, users.size());

            var withDevices = users.stream()
                    .filter(user -> user.getAttributeAsString("jid").orElseThrow().equals(USER_A.toString()))
                    .findFirst()
                    .orElseThrow();
            assertTrue(withDevices.hasChild("devices"));

            var withoutDevices = users.stream()
                    .filter(user -> user.getAttributeAsString("jid").orElseThrow().equals(USER_B.toString()))
                    .findFirst()
                    .orElseThrow();
            assertFalse(withoutDevices.hasChild("devices"));
        }
    }

    @Nested
    @DisplayName("batching")
    class Batching {
        @Test
        @DisplayName("returns a single batch for fewer than 500 users")
        void singleBatch() {
            var users = generateUsers(250);
            var builders = DeviceUSyncQueryBuilder.build(users, "message", null, false);
            assertEquals(1, builders.size());
            assertEquals(250, countUsers(builders.getFirst().build()));
        }

        @Test
        @DisplayName("returns a single batch at exactly 500 users")
        void exactlyAtBoundary() {
            var users = generateUsers(500);
            var builders = DeviceUSyncQueryBuilder.build(users, "message", null, false);
            assertEquals(1, builders.size());
            assertEquals(500, countUsers(builders.getFirst().build()));
        }

        @Test
        @DisplayName("splits into two batches when the user list exceeds the 500-cap")
        void splitsAtBoundary() {
            var users = generateUsers(501);
            var builders = DeviceUSyncQueryBuilder.build(users, "message", null, false);
            assertEquals(2, builders.size());

            var total = builders.stream()
                    .mapToInt(b -> countUsers(b.build()))
                    .sum();
            assertEquals(501, total);
        }

        @Test
        @DisplayName("issues one batch per 500-user slice and a tail for the remainder")
        void multipleBatches() {
            var users = generateUsers(1234);
            var builders = DeviceUSyncQueryBuilder.build(users, "message", null, false);
            assertEquals(3, builders.size());

            var sizes = builders.stream().map(b -> countUsers(b.build())).toList();
            assertEquals(500, sizes.get(0));
            assertEquals(500, sizes.get(1));
            assertEquals(234, sizes.get(2));
        }

        // Reuse would let the server collapse the batches into one in-flight session.
        @Test
        @DisplayName("each batch carries its own sid (no reuse across batches)")
        void distinctSids() {
            var users = generateUsers(1234);
            var sids = new HashSet<String>();
            for (var b : DeviceUSyncQueryBuilder.build(users, "message", null, false)) {
                var sid = childByDescription(b.build(), "usync").getAttributeAsString("sid").orElseThrow();
                sids.add(sid);
            }
            assertEquals(3, sids.size());
        }
    }

    @Nested
    @DisplayName("input validation")
    class InputValidation {
        @Test
        @DisplayName("returns an empty-list batch when userJids is empty")
        void emptyUserJids() {
            var builders = DeviceUSyncQueryBuilder.build(Set.of(), "message", null, false);
            assertEquals(1, builders.size());
            assertEquals(0, countUsers(builders.getFirst().build()));
        }

        @Test
        @DisplayName("rejects null userJids")
        void nullUserJids() {
            try {
                DeviceUSyncQueryBuilder.build(null, "message", null, false);
                throw new AssertionError("expected NullPointerException");
            } catch (NullPointerException expected) {
            }
        }

        @Test
        @DisplayName("rejects null context")
        void nullContext() {
            try {
                DeviceUSyncQueryBuilder.build(Set.of(USER_A), null, null, false);
                throw new AssertionError("expected NullPointerException");
            } catch (NullPointerException expected) {
            }
        }
    }

    // Insertion-ordered so the builder sees a deterministic iteration order for the structural assertions.
    private static LinkedHashSet<Jid> linked(Jid... jids) {
        var set = new LinkedHashSet<Jid>();
        for (var jid : jids) {
            set.add(jid);
        }
        return set;
    }

    // Insertion-ordered synthetic JIDs starting at 10_000_000_000 so the batches partition deterministically.
    private static Set<Jid> generateUsers(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> Jid.of((10_000_000_000L + i) + "@s.whatsapp.net"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static int countUsers(Stanza iq) {
        var list = childByDescription(childByDescription(iq, "usync"), "list");
        return (int) list.streamChildren()
                .filter(child -> "user".equals(child.description()))
                .count();
    }

    private static Stanza firstUser(Stanza iq) {
        var list = childByDescription(childByDescription(iq, "usync"), "list");
        return list.streamChildren()
                .filter(child -> "user".equals(child.description()))
                .findFirst()
                .orElseThrow();
    }

    private static Stanza childByDescription(Stanza parent, String description) {
        var hit = parent.streamChildren()
                .filter(child -> description.equals(child.description()))
                .findFirst();
        if (hit.isEmpty()) {
            throw new AssertionError("no <" + description + "> child under <" + parent.description() + ">");
        }
        return hit.get();
    }
}
