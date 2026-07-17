package com.github.auties00.cobalt.wam;
import com.github.auties00.cobalt.wire.wam.event.*;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wam.WamFixtures;
import com.github.auties00.cobalt.wam.binary.WamEventDecoder;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Audit test that cross-checks Cobalt's generated
 * {@link WamEventRegistry} against the event registry captured from
 * the live WhatsApp Web bundle.
 *
 * <p>For every event the live bundle's
 * {@code WAWebWamCodegenUtils.events} registry exposes, this test
 * asserts that Cobalt's {@link WamEventRegistry#decode} can dispatch
 * the corresponding event id to a concrete {@link WamEventSpec} impl
 * and that the impl reports the same {@code id()} and
 * {@code channel()}.
 *
 * <p>The fixture covers only the events the live runtime registers
 * lazily at startup (typically ~80–100 events out of the ~1000 Cobalt
 * generates), so a Cobalt impl with no live counterpart is not a
 * failure — Cobalt may carry legacy ids that have not yet loaded in
 * the current session. The reverse — a live id with no Cobalt impl
 * — is a hard failure.
 *
 * <p>Vectors live in {@code fixtures/wam/wam-event-definitions.json}
 */
@DisplayName("WamEventRegistry audit against live WhatsApp Web bundle")
class WamEventRegistryAuditTest {
    /**
     * Snapshot revision the vectors were captured against.
     */
    private static final long PINNED_SNAPSHOT_REVISION = 1039260921L;

    /**
     * Scratch buffer size for the minimal event-marker payload fed to
     * {@link WamEventRegistry#decode}. An event marker with no fields
     * is at most three bytes for tiny ids and four bytes for WIDE_ID
     * ids; eight is comfortable.
     */
    private static final int MARKER_BUFFER = 8;

    /**
     * Returns one dynamic test per captured event id, asserting
     * Cobalt's {@link WamEventRegistry} dispatches it to a concrete
     * impl with matching id and channel.
     *
     * @return the test factory stream
     */
    @TestFactory
    List<DynamicTest> liveEventsHaveCobaltImpl() {
        var fixture = WamFixtures.loadOracle("wam-event-definitions");
        WamFixtures.requireSnapshotRevision(fixture, PINNED_SNAPSHOT_REVISION);
        var events = fixture.getJSONArray("events");
        var tests = new ArrayList<DynamicTest>(events.size());
        for (var entry : events) {
            var event = (JSONObject) entry;
            var id = event.get("id");
            if (id == null) {
                // Captured event whose JS constructor refused to instantiate
                // with the empty arg map (typically because validators read
                // store state). The audit can't pin its id, so skip.
                continue;
            }
            var label = event.getString("name") + "(id=" + id + ")";
            tests.add(dynamicTest(label, () -> assertCobaltImplExists(event)));
        }
        return tests;
    }

    /**
     * Verifies Cobalt has a {@link WamEventSpec} impl for the
     * captured event by routing a minimal event-marker payload
     * through {@link WamEventRegistry#decode}.
     *
     * @param event the captured event descriptor
     */
    private static void assertCobaltImplExists(JSONObject event) {
        var eventId = event.getIntValue("id");
        var liveChannel = event.getString("channel");
        var name = event.getString("name");

        WamEventSpec decoded;
        try {
            decoded = decodeMinimalMarker(eventId);
        } catch (RuntimeException error) {
            throw new AssertionError("Cobalt WamEventRegistry has no impl for live event "
                    + name + " (id=" + eventId + "): " + error.getMessage(), error);
        }

        assertNotNull(decoded, () -> "WamEventRegistry returned null for " + name + " (id=" + eventId + ")");
        assertEquals(eventId, decoded.id(),
                () -> "id() drift for " + name + ": Cobalt returns " + decoded.id()
                        + " but live id is " + eventId);

        var cobaltChannel = decoded.channel();
        var expected = WamChannel.valueOf(liveChannel.toUpperCase(Locale.ROOT));
        assertEquals(expected, cobaltChannel,
                () -> "channel() drift for " + name + " (id=" + eventId + "): Cobalt="
                        + cobaltChannel + " live=" + expected);
    }

    /**
     * Builds a minimal event marker (no fields) for the given event
     * id and decodes it through {@link WamEventRegistry#decode},
     * returning the resulting spec.
     *
     * @param eventId the event id to decode
     * @return the decoded event spec
     */
    private static WamEventSpec decodeMinimalMarker(int eventId) {
        var buffer = new byte[MARKER_BUFFER];
        var encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeEventMarker(eventId, 0, false);
        var written = encoder.written();
        var decoder = WamEventDecoder.fromBytes(buffer, 0, written);
        return WamEventRegistry.decode(decoder);
    }

    /**
     * Smoke test that the fixture contains at least one event with a
     * resolvable id; without this, an empty fixture would silently
     * pass the per-event audit.
     *
     * @return a single dynamic test asserting the captured registry
     *         is non-empty
     */
    @TestFactory
    DynamicTest captureRegistryIsNonEmpty() {
        return dynamicTest("captured registry contains at least one event with id", () -> {
            var fixture = WamFixtures.loadOracle("wam-event-definitions");
            var events = fixture.getJSONArray("events");
            var withId = 0;
            for (var entry : events) {
                if (((JSONObject) entry).get("id") != null) {
                    withId++;
                }
            }
            if (withId == 0) {
                throw new AssertionError("wam-event-definitions.json has no events with a resolved id");
            }
        });
    }
}
