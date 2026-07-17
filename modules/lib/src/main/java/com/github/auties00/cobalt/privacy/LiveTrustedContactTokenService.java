package com.github.auties00.cobalt.privacy;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Objects;

/**
 * Computes the lifetime gates for trusted-contact (TC) privacy tokens from the AB-prop bucket
 * parameters and the system clock.
 */
@WhatsAppWebModule(moduleName = "WAWebTrustedContactsUtils")
public final class LiveTrustedContactTokenService implements TrustedContactTokenService {
    /**
     * The logger for {@link LiveTrustedContactTokenService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveTrustedContactTokenService.class);

    /**
     * Caps {@code tctoken_duration} at 180 days expressed in seconds.
     *
     * @implNote
     * The value {@code 15552000} matches WA Web's {@code e = 15552e3} constant.
     */
    @WhatsAppWebExport(moduleName = "WAWebTrustedContactsUtils", exports = "getTcTokenDuration",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int MAX_TC_TOKEN_DURATION_SECONDS = 15_552_000;

    /**
     * Holds the {@link ABPropsService} consulted for the token lifetime parameters.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs a service bound to the given AB-props service.
     *
     * <p>The service is otherwise stateless; constructing more than one over the same
     * {@link ABPropsService} yields interchangeable instances.
     *
     * @param abPropsService the {@link ABPropsService}
     * @throws NullPointerException if {@code abPropsService} is {@code null}
     */
    public LiveTrustedContactTokenService(ABPropsService abPropsService) {
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService");
    }

    /**
     * Returns the token duration in seconds for the given role, clamped to
     * {@link #MAX_TC_TOKEN_DURATION_SECONDS}.
     *
     * <p>Returns the receiver-side prop value for {@link TcTokenMode#RECEIVER} and the sender-side
     * prop value for {@link TcTokenMode#SENDER}.
     *
     * @implNote
     * The clamp protects against AB-prop misconfigurations that would otherwise yield extreme
     * bucket sizes.
     *
     * @param mode the {@link TcTokenMode}
     * @return the token lifetime in seconds
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebTrustedContactsUtils", exports = "getTcTokenDuration",
            adaptation = WhatsAppAdaptation.DIRECT)
    private int getTcTokenDuration(TcTokenMode mode) {
        var durationProp = mode == TcTokenMode.RECEIVER
                ? ABProp.TCTOKEN_DURATION
                : ABProp.TCTOKEN_DURATION_SENDER;
        var duration = Math.min(abPropsService.getInt(durationProp), MAX_TC_TOKEN_DURATION_SECONDS);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "tc token duration for {0}: {1}s", mode, duration);
        return duration;
    }

    /**
     * Returns the unix-seconds cutoff below which a token timestamp is considered expired for the
     * given role.
     *
     * <p>The cutoff is computed from a rolling bucket: every token issued within the last
     * {@code numBuckets} buckets of duration {@link #getTcTokenDuration(TcTokenMode)} is still
     * valid. The boundary instant is {@code (currentBucket - (numBuckets - 1)) * duration} where
     * {@code currentBucket = floor(unixTime / duration)}.
     *
     * @implNote
     * This implementation guards against a zero or negative duration by returning
     * {@link Long#MAX_VALUE}, which forces every token to be considered expired; the
     * {@link ABProp#TCTOKEN_DURATION} default of 604800 seconds means the guard rarely fires in
     * practice.
     *
     * @param mode the {@link TcTokenMode}
     * @return the cutoff in seconds since the Unix epoch
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebTrustedContactsUtils", exports = "tokenExpirationCutoff",
            adaptation = WhatsAppAdaptation.DIRECT)
    private long tokenExpirationCutoff(TcTokenMode mode) {
        var bucketsProp = mode == TcTokenMode.RECEIVER
                ? ABProp.TCTOKEN_NUM_BUCKETS
                : ABProp.TCTOKEN_NUM_BUCKETS_SENDER;

        var numBuckets = abPropsService.getInt(bucketsProp);

        var duration = getTcTokenDuration(mode);
        if (duration <= 0) {
            return Long.MAX_VALUE;
        }

        var currentBucket = Math.floorDiv(Instant.now().getEpochSecond(), duration);
        var cutoffBucket = currentBucket - (numBuckets - 1);
        var cutoff = cutoffBucket * duration;
        if (Log.TRACE) LOGGER.log(Level.TRACE, "tc token cutoff for {0}: {1}", mode, cutoff);
        return cutoff;
    }

    @Override
    @WhatsAppWebExport(moduleName = "WAWebTrustedContactsUtils", exports = "isTokenExpired",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean hasTokenExpired(Instant tokenTimestamp, TcTokenMode mode) {
        var expired = tokenTimestamp.getEpochSecond() < tokenExpirationCutoff(mode);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tc token expired check for {0}: {1}", mode, expired);
        return expired;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation guards against a zero or negative duration by forcing a rotation; the
     * {@link ABProp#TCTOKEN_DURATION_SENDER} default is 604800 seconds.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebTrustedContactsUtils", exports = "shouldSendNewToken",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean shouldSendNewToken(Instant tokenTimestamp) {
        if (tokenTimestamp == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no prior tc token, issuing a new one");
            return true;
        }
        var duration = abPropsService.getInt(ABProp.TCTOKEN_DURATION_SENDER);
        if (duration <= 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "tctoken_duration_sender prop is non-positive: {0}", duration);
            return true;
        }
        var nowBucket = Math.floorDiv(Instant.now().getEpochSecond(), duration);
        var tokenBucket = Math.floorDiv(tokenTimestamp.getEpochSecond(), duration);
        var shouldSend = nowBucket > tokenBucket;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "should send new tc token: {0}", shouldSend);
        return shouldSend;
    }
}
