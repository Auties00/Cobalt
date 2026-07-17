package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.model.WamEventSpec;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Recording {@link WamService} test harness: a drop-in replacement for
 * {@link LiveWamService} that captures every committed event without
 * driving the sampling, channel-routing, encoding, or upload pipeline.
 *
 * <p>{@link #commit(WamEventSpec)} appends the raw event into a
 * {@link CopyOnWriteArrayList} that {@link #committedEvents()} exposes;
 * the synthetic clock advances on each {@link #sleep(long)} so loops
 * polling {@link #now()} terminate without real wall time elapsing, and
 * the scheduling hooks are no-ops because tests invoke any scheduled
 * task directly.
 */
public final class TestWamService extends LiveWamService {
    private final List<WamEventSpec> committed = new CopyOnWriteArrayList<>();

    private volatile Instant now = Instant.ofEpochSecond(1_780_000_000L);

    private TestWamService(LinkedWhatsAppClient client, ABPropsService props, WamBeaconingService beaconing) {
        super(client, props, beaconing);
    }

    public static TestWamService create(LinkedWhatsAppClient client) {
        var props = TestABPropsService.builder().build();
        return new TestWamService(client, props, new LiveWamBeaconingService());
    }

    public List<WamEventSpec> committedEvents() {
        return List.copyOf(committed);
    }

    @Override
    public void commit(WamEventSpec event) {
        committed.add(event);
    }

    @Override
    protected Instant now() {
        return now;
    }

    @Override
    protected void sleep(long millis) {
        now = now.plusMillis(millis);
    }

    @Override
    protected void scheduleRecurring(Runnable task, long initialDelaySeconds, long periodSeconds) {
    }

    @Override
    protected void cancelAllScheduled() {
    }
}
