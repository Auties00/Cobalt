package com.github.auties00.cobalt.bot;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.stanza.mex.json.bot.FetchBotCertificateRevocationListMexRequest;
import com.github.auties00.cobalt.wire.stanza.mex.json.bot.FetchBotCertificateRevocationListMexResponse;
import com.github.auties00.cobalt.util.ScheduledTask;

import java.io.ByteArrayInputStream;
import java.lang.System.Logger.Level;
import java.math.BigInteger;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Production {@link BotCertificateRevocationService} that fetches and caches the bot-feature CRL over
 * MEX and serves revocation queries from the cached snapshot.
 *
 * @implNote
 * This implementation parses the CRL with the JDK {@link CertificateFactory} and refreshes it through
 * a single {@link ScheduledTask#schedule(Duration, Duration, Runnable)} recurrence rather than WA
 * Web's {@code self.setInterval} timer or a dedicated parked thread: the first refresh fires
 * immediately and each subsequent tick fires {@link #REFRESH_INTERVAL} after the previous one
 * finishes, so the recurrence holds a carrier thread only while a fetch is in flight and the schedule
 * is cancellable through one {@link ScheduledTask} handle. A failed fetch is logged by the scheduler
 * and the next tick still runs. The revoked set, last-fetch and next-update watermarks are published
 * together through one {@link AtomicReference} so a query never observes a half-applied refresh.
 */
@WhatsAppWebModule(moduleName = "WAWebBotCertificateRevocationService")
public final class LiveBotCertificateRevocationService implements BotCertificateRevocationService {
    /**
     * The logger for {@link LiveBotCertificateRevocationService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveBotCertificateRevocationService.class);

    /**
     * The CRL refresh interval, six hours, matching WA Web's {@code 216e5} ms timer.
     */
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(6);

    /**
     * The fallback {@code next_update} horizon, forty-eight hours, used when the relay omits the
     * watermark; matches WA Web's {@code 1728e5} ms default.
     */
    private static final Duration DEFAULT_NEXT_UPDATE = Duration.ofHours(48);

    /**
     * The bound client used to issue the CRL MEX query.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The current CRL state, replaced atomically on each successful refresh.
     */
    private final AtomicReference<CrlState> state;

    /**
     * The guard ensuring the periodic refresh loop is started at most once and stops rescheduling
     * once cancelled.
     */
    private final AtomicBoolean started;

    /**
     * The handle of the running periodic refresh, or {@code null} when none is running.
     */
    private volatile ScheduledTask refreshJob;

    /**
     * Constructs a service bound to the given client.
     *
     * @param client the bound client, must not be {@code null}
     * @throws NullPointerException if {@code client} is {@code null}
     */
    public LiveBotCertificateRevocationService(LinkedWhatsAppClient client) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.state = new AtomicReference<>(CrlState.EMPTY);
        this.started = new AtomicBoolean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BotRevocationStatus checkRevocationStatus(BigInteger serial, long nowMs) {
        var current = state.get();
        if (current.lastFetchMs == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "revocation check skipped, crl never fetched");
            return BotRevocationStatus.CRL_UNAVAILABLE;
        }
        if (current.nextUpdateMs != null && nowMs > current.nextUpdateMs) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "revocation check skipped, crl stale, next_update={0}", current.nextUpdateMs);
            return BotRevocationStatus.CRL_STALE;
        }
        var status = current.revokedSerials.contains(serial) ? BotRevocationStatus.REVOKED : BotRevocationStatus.VALID;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "revocation check result {0}", status);
        return status;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation schedules the recurrence with a zero initial delay so the first refresh
     * runs at once on a virtual thread, then each tick fires {@link #REFRESH_INTERVAL} after the
     * previous one finishes. The {@link #started} guard makes a second call while running a no-op.
     */
    @Override
    public void startPeriodicRefresh() {
        if (!started.compareAndSet(false, true)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "crl periodic refresh already started");
            return;
        }
        if (Log.INFO) LOGGER.log(Level.INFO, "starting crl periodic refresh, interval={0}", REFRESH_INTERVAL);
        refreshJob = ScheduledTask.schedule(Duration.ZERO, REFRESH_INTERVAL, this::refresh);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopPeriodicRefresh() {
        started.set(false);
        var job = refreshJob;
        if (job != null) {
            job.cancel();
            refreshJob = null;
            if (Log.INFO) LOGGER.log(Level.INFO, "stopped crl periodic refresh");
        }
    }

    /**
     * Fetches and parses the CRL, replacing the published {@link #state} on success.
     *
     * <p>A reply that carries no {@code xwa2_fetch_feature_pki_crl} envelope or no {@code crl} scalar
     * leaves the previous state untouched.
     */
    private void refresh() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "fetching bot certificate revocation list");
        var response = client.sendNode(new FetchBotCertificateRevocationListMexRequest());
        var parsed = FetchBotCertificateRevocationListMexResponse.of(response).orElse(null);
        if (parsed == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "crl refresh response missing fetch envelope, keeping previous state");
            return;
        }
        var crl = parsed.crl().orElse(null);
        if (crl == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "crl refresh response missing crl scalar, keeping previous state");
            return;
        }
        var revoked = parseRevokedSerials(Base64.getDecoder().decode(crl));
        var nextUpdateMs = parsed.nextUpdate()
                .map(Instant::toEpochMilli)
                .orElseGet(() -> System.currentTimeMillis() + DEFAULT_NEXT_UPDATE.toMillis());
        state.set(new CrlState(revoked, System.currentTimeMillis(), nextUpdateMs));
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "crl refreshed, revoked_count={0}, next_update={1}", revoked.size(), nextUpdateMs);
        }
    }

    /**
     * Parses the revoked serial numbers from a DER-encoded CRL.
     *
     * @param crlBytes the DER-encoded CRL
     * @return the set of revoked serial numbers, empty when the CRL is empty or unparsable
     */
    private Set<BigInteger> parseRevokedSerials(byte[] crlBytes) {
        try {
            var factory = CertificateFactory.getInstance("X.509");
            var crl = (X509CRL) factory.generateCRL(new ByteArrayInputStream(crlBytes));
            var revoked = crl.getRevokedCertificates();
            if (revoked == null) {
                return Set.of();
            }
            var serials = new HashSet<BigInteger>(revoked.size());
            for (var entry : revoked) {
                serials.add(entry.getSerialNumber());
            }
            return serials;
        } catch (Exception exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to parse bot certificate revocation list", exception);
            return Set.of();
        }
    }

    /**
     * An immutable snapshot of the CRL: the revoked serial numbers and the fetch/next-update
     * watermarks.
     *
     * @param revokedSerials the revoked certificate serial numbers
     * @param lastFetchMs    the epoch-millisecond time of the last successful fetch, or {@code null}
     * @param nextUpdateMs   the epoch-millisecond {@code next_update} watermark, or {@code null}
     */
    private record CrlState(Set<BigInteger> revokedSerials, Long lastFetchMs, Long nextUpdateMs) {
        /**
         * The empty state published before any successful fetch.
         */
        static final CrlState EMPTY = new CrlState(Set.of(), null, null);
    }
}
