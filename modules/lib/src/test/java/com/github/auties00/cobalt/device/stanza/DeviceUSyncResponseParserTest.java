package com.github.auties00.cobalt.device.stanza;

import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.device.DeviceListResult;
import com.github.auties00.cobalt.device.adv.DeviceADVValidator;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.props.TestABPropsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixture-driven tests for {@link DeviceUSyncResponseParser}, replaying captured
 * USync IQ pairs from {@code fixtures/device/usync-*.jsonl} through the parser
 * and asserting the {@link DeviceListResult} classification each observed wire
 * shape produces: self lookup, other-contact lookup, nonexistent number, bot DM,
 * hosted-business PN-side, hosted-business LID-side, and group lookups of various
 * sizes.
 *
 * <p>The {@link DeviceListResult.Full} payload contents are intentionally not
 * verified: signature verification on the captured {@code <key-index-list>}
 * needs the original primary identity key, which the public fixture corpus does
 * not ship; that positive-path assertion lives in {@code DeviceADVValidatorTest}.
 * These tests only assert that the parser walks the captured shape without
 * exceptions and produces the right classification. A new parser is built per
 * test so accumulator state inside {@link DeviceADVValidator} does not leak
 * between cases.
 */
@DisplayName("DeviceUSyncResponseParser")
class DeviceUSyncResponseParserTest {
    // Matches the business VoIP capture session; SELF_LID is its paired LID.
    private static final Jid SELF_PN = Jid.of("19254863482@s.whatsapp.net");

    private static final Jid SELF_LID = Jid.of("83116928594056@lid");

    private static DeviceUSyncResponseParser newParser() {
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        var props = TestABPropsService.builder().build();
        return new DeviceUSyncResponseParser(new DeviceADVValidator(store, props));
    }

    // Returns the first direction="in" entry; the outbound query entry is discarded since the parser only consumes responses.
    private static Stanza loadResponseNode(String topic) {
        for (var event : DeviceFixtures.loadEvents(topic)) {
            if ("in".equals(event.getString("direction"))) {
                return DeviceFixtures.buildNodeFromEvent(event);
            }
        }
        throw new AssertionError("no inbound event in fixture " + topic);
    }

    // Without the original primary identity key, parseFullResult yields an empty stream; the contract under
    // test is the absence of Error entries, not a populated Full payload.
    @Test
    @DisplayName("self USync response carries a signed-key-index; parser routes through parseFullResult")
    void selfRoutesToFullPath() {
        var response = loadResponseNode("usync-self");
        var results = newParser().parse(response);

        var errors = results.stream().filter(r -> r instanceof DeviceListResult.Error).count();
        assertEquals(0, errors, "self USync response carries no <error> children");
    }

    @Test
    @DisplayName("other-contact USync response: same Full-path routing as self")
    void otherRoutesToFullPath() {
        var response = loadResponseNode("usync-other");
        var results = newParser().parse(response);

        var errors = results.stream().filter(r -> r instanceof DeviceListResult.Error).count();
        assertEquals(0, errors, "other-contact USync response carries no <error> children");
    }

    @Test
    @DisplayName("nonexistent-user USync: empty <device-list/> routes through parseOmittedResult")
    void nonexistentRoutesToOmittedPath() {
        var response = loadResponseNode("usync-nonexistent");
        var results = newParser().parse(response);

        assertEquals(1, results.size(), "single-user query returns one result");
        assertTrue(results.getFirst() instanceof DeviceListResult.Omitted,
                "nonexistent number should classify as Omitted, got "
                        + results.getFirst().getClass().getSimpleName());
    }

    @Test
    @DisplayName("none of the captured responses produce a fatal global error")
    void noFatalGlobalError() {
        for (var topic : List.of("usync-self", "usync-other", "usync-nonexistent", "usync-bot", "usync-hosted-pn", "usync-hosted-lid")) {
            var response = loadResponseNode(topic);
            var results = newParser().parse(response);
            var fatalError = results.stream()
                    .filter(r -> r instanceof DeviceListResult.Error err && err.fatal())
                    .findFirst();
            assertFalse(fatalError.isPresent(),
                    "Captured " + topic + " response should not be a fatal global error; got " + fatalError.orElse(null));
        }
    }

    @Test
    @DisplayName("bot DM USync: server returns empty <list/>; parser yields no results")
    void botUSyncReturnsEmpty() {
        var response = loadResponseNode("usync-bot");
        var results = newParser().parse(response);
        assertEquals(0, results.size(),
                "bot USync produces no DeviceListResult entries (empty <list/>)");
    }

    @Test
    @DisplayName("hosted-business coex USync (PN-server): single primary device, Omitted path")
    void hostedPnUSync() {
        var response = loadResponseNode("usync-hosted-pn");
        var results = newParser().parse(response);
        assertEquals(1, results.size(),
                "hosted contact returns one result");
        assertTrue(results.getFirst() instanceof DeviceListResult.Omitted,
                "single primary device with no key-index-list classifies as Omitted; got "
                        + results.getFirst().getClass().getSimpleName());
    }

    @Test
    @DisplayName("hosted-business coex USync (LID-server): single primary device, Omitted path")
    void hostedLidUSync() {
        var response = loadResponseNode("usync-hosted-lid");
        var results = newParser().parse(response);
        assertEquals(1, results.size());
        assertTrue(results.getFirst() instanceof DeviceListResult.Omitted,
                "LID-side hosted contact same Omitted classification; got "
                        + results.getFirst().getClass().getSimpleName());
    }

    @Test
    @DisplayName("group USyncs: small (2), medium (10), and large (205) all parse without exceptions")
    void groupUSyncShapes() {
        for (var topic : List.of("usync-group-small", "usync-group-medium", "usync-group-large")) {
            var response = loadResponseNode(topic);
            var results = newParser().parse(response);
            assertFalse(results.stream().anyMatch(r -> r instanceof DeviceListResult.Error err && err.fatal()),
                    topic + " should not produce a fatal global error");
        }
    }
}
