package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wire.wam.event.PsIdUpdateEventBuilder;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wire.wam.type.PsIdAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link WamService}'s per-channel dirty-tracking for session
 * globals across three successive flushes: the encoder re-emits only the
 * global attributes whose values have changed since the previous flush on
 * the same channel.
 *
 * <p>The observed signal is the byte length of the WAM buffer captured via
 * the test client's send handler: the first flush carries every current
 * global, the second flush with identical state carries none, and the
 * third flush after mutating one global carries exactly that one global's
 * encoded entry.
 */
@DisplayName("WamService prev-session-globals dirty-write")
class WamServiceGlobalsDirtyWriteTest {
    @Test
    @DisplayName("unchanged globals are skipped on the second flush; mutated globals re-emit on the third")
    void dirtyTrackingSkipsUnchangedAndEmitsMutated() {
        var selfPn = Jid.of("19254863482@s.whatsapp.net");
        var props = TestABPropsService.builder().build();
        var capturedBuffers = new ArrayList<byte[]>();
        var store = DeviceFixtures.temporaryStore(selfPn, null);
        var client = TestWhatsAppClient.create()
                .withStore(store)
                .withAbPropsService(props)
                .withIsConnected(true)
                .withSendNodeHandler(builder -> {
                    capturedBuffers.add(extractBuffer(builder));
                    return successResponse();
                });
        var service = new InstrumentedWamService(client, props, new LiveWamBeaconingService());
        service.markInitializedForTesting();
        service.setSamplingOverride(2862, 1);

        service.commit(samplePsIdEvent());
        service.flushChannel(WamChannel.REGULAR);
        assertTrue(capturedBuffers.size() == 1,
                "expected exactly one sendNode call after first flush, got " + capturedBuffers.size());
        var firstSize = capturedBuffers.getFirst().length;

        service.commit(samplePsIdEvent());
        service.flushChannel(WamChannel.REGULAR);
        assertTrue(capturedBuffers.size() == 2,
                "expected exactly two sendNode calls after second flush, got " + capturedBuffers.size());
        var secondSize = capturedBuffers.get(1).length;
        assertTrue(secondSize < firstSize,
                () -> "second flush must be smaller than the first (no globals re-emitted): first="
                        + firstSize + " second=" + secondSize);

        service.setDeviceName("TestDevice");

        service.commit(samplePsIdEvent());
        service.flushChannel(WamChannel.REGULAR);
        assertTrue(capturedBuffers.size() == 3,
                "expected exactly three sendNode calls after third flush, got " + capturedBuffers.size());
        var thirdSize = capturedBuffers.get(2).length;
        assertTrue(thirdSize > secondSize,
                () -> "third flush must be larger than the second (mutated deviceName re-emitted): second="
                        + secondSize + " third=" + thirdSize);
    }

    // Each call returns a new instance so the spec's markCommitted() guard does
    // not reject repeat commits across the three flushes.
    private static WamEventSpec samplePsIdEvent() {
        return new PsIdUpdateEventBuilder()
                .psIdAction(PsIdAction.CREATED)
                .psIdKey(42)
                .psIdRotationFrequence(7)
                .build();
    }

    private static Stanza successResponse() {
        return new StanzaBuilder()
                .description("iq")
                .attribute("type", "result")
                .build();
    }

    // The buffer lives in the binary content of the inner <add> child of the
    // WAM upload <iq>; an AssertionError on a shape mismatch surfaces an encoder
    // pipeline regression immediately.
    private static byte[] extractBuffer(StanzaBuilder builder) {
        var built = builder.build();
        var add = built.getChild("add").orElseThrow(
                () -> new AssertionError("captured iq has no <add> child"));
        if (add instanceof Stanza.BytesStanza bytesNode) {
            return bytesNode.content();
        }
        throw new AssertionError("captured <add> child has no binary content");
    }

    /**
     * Test double for {@link WamService} that stubs the four protected
     * timing/scheduling hooks with no-ops; the dirty-tracking test never
     * sleeps or schedules and drives flushes synchronously via
     * {@link LiveWamService#flushChannel(WamChannel)}.
     */
    private static final class InstrumentedWamService extends LiveWamService {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.ofEpochSecond(1_747_000_000L));

        InstrumentedWamService(
                LinkedWhatsAppClient client,
                ABPropsService props,
                WamBeaconingService beaconing) {
            super(client, props, beaconing);
        }

        @Override
        protected Instant now() {
            return now.get();
        }

        @Override
        protected void sleep(long millis) {
        }

        @Override
        protected void scheduleRecurring(Runnable task, long initialDelaySeconds, long periodSeconds) {
        }

        @Override
        protected void cancelAllScheduled() {
        }
    }

}
