package com.github.auties00.cobalt.props;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppABPropException;
import com.github.auties00.cobalt.exception.linked.WhatsAppServerRuntimeException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.WefrClientExposureEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.WefrGroupClientExposureEventBuilder;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Production {@link ABPropsService} implementation that fetches and applies AB-props from
 * WhatsApp's relay.
 *
 * <p>The service issues the {@code <iq xmlns="abt">} request, parses the response, and maintains
 * three independent in-memory caches: the synced experiment values keyed by {@code config_code},
 * the WAM event sampling-weight overrides keyed by event code, and the set of prop codes the host
 * application has read. The persisted local-storage attributes (AB key, hash, refresh interval,
 * last sync time, refresh ids) are read from and written to the shared store through the
 * {@link LinkedWhatsAppClient}. Queries issued before the first sync completes block on a
 * {@link CompletableFuture} for up to {@link #syncTimeout}, then fall back to the prop default if
 * the timeout elapses, so callers observe a consistent view rather than a partially populated
 * cache.
 *
 * @implNote
 * This implementation absorbs the WA Web {@code instanceof} cascades over the WhatsApp Web GraphQL response
 * shape: each {@code <prop>} child is dispatched through the disjunction defined in
 * {@code WASmaxInAbPropsConfigs.parseConfigs} and the
 * {@code parseExperimentOrSamplingConfigMixinGroup} wrapper, trying
 * {@code parseExperimentConfigMixin} first and falling back to {@code parseSamplingConfigMixin}.
 * Sampling-config parsing applies the bounds from
 * {@code WASmaxInAbPropsSamplingConfigMixin.parseSamplingConfigMixin} directly. Local-storage
 * attributes and the sampling cache are only replaced on full (non-delta) updates, matching the
 * JS gating on {@code !isDeltaUpdate}.
 *
 * @see ABProp
 */
@WhatsAppWebModule(moduleName = "WAWebAbPropsSyncJob")
@WhatsAppWebModule(moduleName = "WAGetAbPropsProtocol")
@WhatsAppWebModule(moduleName = "WAGetGroupAbPropsProtocol")
@WhatsAppWebModule(moduleName = "WAWebApiAbPropConfig")
@WhatsAppWebModule(moduleName = "WAWebApiAbPropEventSamplingConfig")
@WhatsAppWebModule(moduleName = "WAWebABPropsLocalStorage")
@WhatsAppWebModule(moduleName = "WAWebABPropsParseConfigValue")
@WhatsAppWebModule(moduleName = "WASmaxInAbPropsSamplingConfigMixin")
@WhatsAppWebModule(moduleName = "WASmaxInAbPropsConfigs")
@WhatsAppWebModule(moduleName = "WASmaxInAbPropsExperimentOrSamplingConfigMixinGroup")
@WhatsAppWebModule(moduleName = "WASmaxInAbPropsEnums")
@WhatsAppWebModule(moduleName = "WAWebABPropsGlobals")
@WhatsAppWebModule(moduleName = "WAWebGroupABPropsGlobals")
@WhatsAppWebModule(moduleName = "WAWebABPropsExpoKeyUtils")
@WhatsAppWebModule(moduleName = "WAWebCanonicalUtils")
@WhatsAppWebModule(moduleName = "WAWebWefrClientExposureWamEvent")
@WhatsAppWebModule(moduleName = "WAWebWefrGroupClientExposureWamEvent")
public final class LiveABPropsService implements ABPropsService {
    /**
     * The logger for {@link LiveABPropsService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveABPropsService.class);

    /**
     * Default timeout that query methods wait for the first sync to complete before falling back
     * to the prop's default value.
     *
     * <p>Used by the single-argument constructor; the two-argument constructor accepts a
     * caller-supplied timeout instead.
     */
    private static final Duration DEFAULT_SYNC_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Lower bound, inclusive, in seconds, for the {@code refresh} attribute persisted by
     * {@link #updateAttributesLocalStorage(String, String, Long, Instant)}.
     *
     * @implNote
     * This implementation uses the WA Web floor of 600 seconds (10 minutes) so the persisted
     * refresh interval never falls below it.
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "updateAttributesLocalStorage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final long REFRESH_MIN_SECONDS = 600L;

    /**
     * Upper bound, inclusive, in seconds, for the {@code refresh} attribute persisted by
     * {@link #updateAttributesLocalStorage(String, String, Long, Instant)}.
     *
     * @implNote
     * This implementation uses the WA Web ceiling of 604800 seconds (7 days) so the persisted
     * refresh interval never exceeds it.
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "updateAttributesLocalStorage",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final long REFRESH_MAX_SECONDS = 604800L;

    /**
     * Default refresh interval of one day, in seconds, returned by {@link #refresh()} when no
     * value has been recorded yet.
     *
     * @implNote
     * This implementation uses the WA Web fallback of 86400 seconds from
     * {@code WAWebABPropsLocalStorage.getRefresh}.
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "getRefresh",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final long REFRESH_DEFAULT_SECONDS = 86400L;

    /**
     * Inclusive lower bound accepted for the {@code event_code} attribute on {@code SamplingConfig}
     * {@code <prop>} children.
     *
     * <p>Entries below this bound are dropped with a warning during {@link #process(Stanza)}.
     *
     * @implNote
     * This implementation mirrors the validation in
     * {@code WASmaxInAbPropsSamplingConfigMixin.parseSamplingConfigMixin}.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsSamplingConfigMixin",
            exports = "parseSamplingConfigMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int SAMPLING_EVENT_CODE_MIN = 1;

    /**
     * Inclusive lower bound accepted for the {@code sampling_weight} attribute on
     * {@code SamplingConfig} {@code <prop>} children.
     *
     * <p>Entries below this bound are dropped with a warning during {@link #process(Stanza)}.
     *
     * @implNote
     * This implementation mirrors the validation in
     * {@code WASmaxInAbPropsSamplingConfigMixin.parseSamplingConfigMixin}.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsSamplingConfigMixin",
            exports = "parseSamplingConfigMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int SAMPLING_WEIGHT_MIN = -10_000;

    /**
     * Inclusive upper bound accepted for the {@code sampling_weight} attribute on
     * {@code SamplingConfig} {@code <prop>} children.
     *
     * <p>Entries above this bound are dropped with a warning during {@link #process(Stanza)}.
     *
     * @implNote
     * This implementation mirrors the validation in
     * {@code WASmaxInAbPropsSamplingConfigMixin.parseSamplingConfigMixin}.
     */
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsSamplingConfigMixin",
            exports = "parseSamplingConfigMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int SAMPLING_WEIGHT_MAX = 10_000;

    /**
     * Client used to issue the {@code <iq xmlns="abt">} stanza and to read or mutate the AB-props
     * slots on the shared store.
     */
    private final LinkedWhatsAppClient client;

    /**
     * WAM telemetry sink used to commit the WEFR client- and group-exposure pulse events.
     *
     * <p>Injected after construction through {@link #setWamService(WamService)} rather than through
     * the constructor because the WAM service depends on this service (it reads sampling weights
     * through {@link #getSamplingWeight(int)}), so it does not yet exist when this service is built.
     * Every emit site guards on {@code null} so a session that never wires the sink simply skips the
     * exposure beacons.
     *
     * @implNote
     * This implementation stands in for the ambient {@code WAWebWamGlobals}-backed {@code .commit()}
     * calls in {@code WAWebABPropsGlobals} and {@code WAWebGroupABPropsGlobals}, which reach the WAM
     * runtime through a module-level singleton rather than an injected field.
     */
    private WamService wamService;

    /**
     * Synced AB-prop values keyed by their numeric {@code config_code}.
     *
     * <p>Populated from {@code <prop>} children whose shape matches {@code ExperimentConfig};
     * queried by the typed {@code getXxx} accessors.
     */
    private final Map<Integer, String> props;

    /**
     * WAM event sampling-weight overrides keyed by event code.
     *
     * <p>Populated from the {@code SamplingConfig} entries returned alongside the prop list during
     * {@link #process(Stanza)}; queried by {@link #getSamplingWeight(int)} during WAM event commit.
     */
    private final Map<Integer, Integer> samplingConfigs;

    /**
     * Codes of AB props that the host application has read at least once, tracked for the WAM
     * exposure-key attribute.
     *
     * @implNote
     * WA Web exposes this through its IndexedDB {@code abpropConfigs} table's {@code hasAccessed}
     * column; this implementation collapses it into an in-memory set per Cobalt's store-flattening
     * pattern.
     */
    @WhatsAppWebExport(moduleName = "WAWebApiAbPropConfig", exports = "setConfigAccessed",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private final Set<Integer> accessedConfigs;

    /**
     * Future that completes once a sync round finishes, releasing threads blocked in
     * {@link #awaitSync()}.
     *
     * @implNote
     * This implementation holds the future in an {@link AtomicReference} so {@link #clear()} can
     * swap in a fresh future for the next session.
     */
    private final AtomicReference<CompletableFuture<Boolean>> syncFuture;

    /**
     * Timeout that query methods wait for the first sync to complete.
     */
    private final Duration syncTimeout;

    /**
     * Constructs a service bound to {@code client} with the default query timeout of
     * {@link #DEFAULT_SYNC_TIMEOUT}.
     *
     * <p>This is the production wiring; the {@link Duration} overload supplies a session-specific
     * timeout instead.
     *
     * @param client the WhatsApp client used to issue sync requests
     */
    public LiveABPropsService(LinkedWhatsAppClient client) {
        this(client, DEFAULT_SYNC_TIMEOUT);
    }

    /**
     * Constructs a service bound to {@code client} with a caller-supplied query timeout.
     *
     * <p>This dials the per-query wait up or down for a specific session profile, such as CI
     * fixtures or latency-tolerant embedders.
     *
     * @implNote
     * This implementation seeds {@link #syncFuture} with a fresh incomplete future so queries
     * issued before the first sync block on it.
     *
     * @param client      the WhatsApp client used to issue sync requests
     * @param syncTimeout the timeout query methods wait for the first sync
     */
    public LiveABPropsService(LinkedWhatsAppClient client, Duration syncTimeout) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.props = new ConcurrentHashMap<>();
        this.samplingConfigs = new ConcurrentHashMap<>();
        this.accessedConfigs = ConcurrentHashMap.newKeySet();
        this.syncFuture = new AtomicReference<>(new CompletableFuture<>());
        this.syncTimeout = syncTimeout;
    }

    /**
     * Injects the WAM telemetry sink used to emit the WEFR exposure pulse events.
     *
     * <p>Called once during client assembly, after the WAM service has been constructed, to break
     * the construction-order cycle between this service and the WAM service. Until this is called
     * the exposure emit sites are inert.
     *
     * @param wamService the WAM service that receives committed exposure events
     */
    public void setWamService(WamService wamService) {
        this.wamService = wamService;
    }

    /**
     * Runs a sync round with the default options.
     *
     * <p>Equivalent to {@code sync(null, true)}: selects the regular {@code propsHash} branch with
     * hash inclusion gated on {@link #isAfterFirstSync()}.
     *
     * @return {@code true} when at least one of the three attempts succeeded, {@code false}
     *         otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebAbPropsSyncJob", exports = "syncABPropsTask",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean sync() {
        return sync(null, true);
    }

    /**
     * Runs a sync round with caller-supplied options, retrying up to three times.
     *
     * <p>A non-{@code null} {@code localRefreshId} takes the emergency push branch; a {@code null}
     * {@code localRefreshId} takes the regular {@code propsHash} branch. On the regular branch
     * {@code shouldSendHash} is ANDed with {@link #isAfterFirstSync()} and with the in-memory prop
     * cache being non-empty, so the persisted hash is sent only once a previous sync established it
     * and the props it summarises are actually held in memory.
     *
     * @implNote
     * This implementation completes {@link #syncFuture} on success so blocked queries proceed,
     * fails it exceptionally on terminal failure so queries do not hang, and sleeps for a jittered
     * {@code 10000 * Math.random()} millisecond delay between attempts to mirror the JS
     * {@code WAPromiseDelays.delayMs} backoff. Unlike WA Web, which persists the whole {@code ABPROPS}
     * blob (hash and config values together) in IndexedDB, Cobalt persists only the hash, AB key, and
     * sync time; the config values live in a transient in-memory map that starts empty after a
     * restart. Sending the persisted hash with an empty cache would make the relay reply with an
     * empty no-change response and leave the session permanently propless, so the hash is withheld
     * whenever {@link #props} is empty to force a full re-fetch.
     *
     * @param localRefreshId the refresh-id override used by the emergency push branch, or
     *                       {@code null} to take the regular {@code propsHash} branch
     * @param shouldSendHash whether the {@code propsHash} branch may include the persisted hash on
     *                       the request
     * @return {@code true} when at least one attempt succeeded, {@code false} when all attempts
     *         failed
     */
    @WhatsAppWebExport(moduleName = "WAWebAbPropsSyncJob", exports = "syncABPropsTask",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean sync(Long localRefreshId, boolean shouldSendHash) {
        var afterFirstSync = isAfterFirstSync();
        var effectiveShouldSendHash = localRefreshId != null
                ? shouldSendHash
                : afterFirstSync && shouldSendHash && !props.isEmpty();
        Throwable lastFailure = null;
        for (var attempt = 3; attempt-- > 0; ) {
            try {
                var success = syncABProps(localRefreshId, effectiveShouldSendHash);
                if (success) {
                    completeSync(true);
                    return true;
                }
            } catch (Throwable throwable) {
                lastFailure = throwable;
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "ab props sync attempt failed, remaining=" + attempt, throwable);
                }
            }
            if (attempt > 0) {
                try {
                    Thread.sleep((long) (10_000L * Math.random()));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (lastFailure != null) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "ab props sync failed after 3 attempts", lastFailure);
            failSync(lastFailure);
        } else {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "ab props sync failed after 3 attempts");
            completeSync(false);
        }
        return false;
    }

    /**
     * Performs a single sync round trip.
     *
     * <p>A non-{@code null} {@code localRefreshId} takes the emergency push branch; a {@code null}
     * {@code localRefreshId} takes the regular branch. The regular branch sends the persisted hash
     * from the store when {@code shouldSendHash} is true so the relay can reply with a delta
     * update.
     *
     * @param localRefreshId the refresh-id override that selects the emergency push branch, or
     *                       {@code null} to take the regular branch
     * @param shouldSendHash whether the persisted hash is included on the regular branch
     * @return {@code true} when the response was processed successfully, {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebAbPropsSyncJob", exports = "syncABProps",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean syncABProps(Long localRefreshId, boolean shouldSendHash) {
        var emergencyBranch = localRefreshId != null;
        var request = emergencyBranch
                ? getAbPropsProtocol(null, localRefreshId)
                : getAbPropsProtocol(shouldSendHash ? client.store().syncStore().abPropsHash().orElse(null) : null, null);
        var response = client.sendNode(request);
        if (response == null) {
            return false;
        }
        return process(response);
    }

    /**
     * Completes {@link #syncFuture} with {@code success}, releasing every thread blocked in
     * {@link #awaitSync()}.
     *
     * <p>Called by {@link #sync(Long, boolean)} on terminal success or terminal no-throw failure.
     *
     * @param success the result delivered to waiters
     */
    private void completeSync(boolean success) {
        syncFuture.get().complete(success);
    }

    /**
     * Completes {@link #syncFuture} exceptionally so waiting threads do not hang when every sync
     * attempt fails.
     *
     * <p>Called by {@link #sync(Long, boolean)} when the final attempt throws.
     *
     * @param throwable the failure observed on the last attempt
     */
    private void failSync(Throwable throwable) {
        syncFuture.get().completeExceptionally(throwable);
    }

    /**
     * Blocks until the next sync completes or {@link #syncTimeout} elapses.
     *
     * <p>Used by the typed query methods to defer reads until props are actually available;
     * callers that opt out of the wait pass {@code waitForSync = false} to the two-argument query
     * overload.
     *
     * @implNote
     * This implementation maps timeouts, interruption, and any other failure mode to {@code false}
     * so the caller proceeds with the prop's default value rather than failing the read path.
     *
     * @return {@code true} when the sync completed successfully within the timeout, {@code false}
     *         on timeout, interruption, or failure
     */
    private boolean awaitSync() {
        try {
            var result = syncFuture.get().get(syncTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return result != null && result;
        } catch (TimeoutException e) {
            throw new WhatsAppABPropException.SyncTimeout(syncTimeout, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (Log.WARNING) LOGGER.log(Level.WARNING, "interrupted while waiting for ab props sync");
            return false;
        } catch (Throwable e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "error waiting for ab props sync", e);
            return false;
        }
    }

    /**
     * Builds the {@code <iq xmlns="abt" type="get">} stanza that requests the experiment-config
     * blob from {@code s.whatsapp.net}.
     *
     * <p>Used by {@link #syncABProps(Long, boolean)} as the request builder. The inner
     * {@code <props>} child carries the literal {@code protocol="1"} together with an optional
     * {@code hash} (the regular delta-update branch) or an optional {@code refresh_id} (the
     * emergency push branch). Callers normally populate exactly one of the two depending on which
     * branch they take.
     *
     * @param propsHash      the AB-props hash for delta updates, or {@code null} to omit the
     *                       attribute
     * @param propsRefreshId the refresh id used by the emergency push branch, or {@code null} to
     *                       omit the attribute
     * @return a {@link StanzaBuilder} wrapping the constructed stanza
     */
    @WhatsAppWebExport(moduleName = "WAGetAbPropsProtocol", exports = "getAbPropsProtocol",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private StanzaBuilder getAbPropsProtocol(String propsHash, Long propsRefreshId) {
        var propsNode = new StanzaBuilder()
                .description("props")
                .attribute("protocol", "1");
        if (propsHash != null) {
            propsNode.attribute("hash", propsHash);
        }
        if (propsRefreshId != null) {
            propsNode.attribute("refresh_id", propsRefreshId);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "abt")
                .attribute("to", "s.whatsapp.net")
                .attribute("type", "get")
                .content(propsNode.build());
    }

    /**
     * Parses a sync response, applies it to the in-memory caches, and persists the {@code ABPROPS}
     * local-storage attributes.
     *
     * <p>The {@code <props>} child carries the {@code hash}, {@code ab_key}, {@code refresh},
     * {@code refresh_id}, and {@code delta_update} attributes plus a list of {@code <prop>}
     * children. Each child is either an {@code ExperimentConfig} (carrying {@code config_code} and
     * {@code config_value}) or a {@code SamplingConfig} (carrying {@code event_code} and
     * {@code sampling_weight}). Local-storage attributes and the sampling-config cache are
     * replaced only on full (non-delta) updates; delta responses leave both in place even if they
     * happen to carry sampling entries, matching the JS gating on {@code !isDeltaUpdate}.
     *
     * @implNote
     * This implementation dispatches each {@code <prop>} child through the disjunction defined in
     * {@code WASmaxInAbPropsConfigs.parseConfigs} and the
     * {@code parseExperimentOrSamplingConfigMixinGroup} wrapper: it tries
     * {@code parseExperimentConfigMixin} first, falls back to {@code parseSamplingConfigMixin}, and
     * logs a warning equivalent to {@code WASmaxParseUtils.errorMixinDisjunction} when neither
     * shape matches. Sampling entries are accepted only when {@code event_code >= 1} and
     * {@code sampling_weight} lies in the closed range
     * [{@value #SAMPLING_WEIGHT_MIN}, {@value #SAMPLING_WEIGHT_MAX}].
     *
     * @param response the server response stanza
     * @return {@code true} when the response was parsed successfully, {@code false} when the
     *         {@code <props>} child was missing
     * @throws NullPointerException when {@code response} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebApiAbPropConfig", exports = "updateABPropConfigs",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsSamplingConfigMixin",
            exports = "parseSamplingConfigMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsConfigs", exports = "parseConfigs",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInAbPropsExperimentOrSamplingConfigMixinGroup",
            exports = "parseExperimentOrSamplingConfigMixinGroup",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean process(Stanza response) {
        Objects.requireNonNull(response, "response cannot be null");

        var propsNode = response.getChild("props", null);
        if (propsNode == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "ab props response missing <props> stanza");
            return false;
        }

        var responseHash = propsNode.getAttributeAsString("hash").orElse(null);
        var responseAbKey = propsNode.getAttributeAsString("ab_key").orElse(null);
        var responseRefreshOpt = propsNode.getAttributeAsLong("refresh");
        if (responseHash != null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "updated ab props hash: {0}", responseHash);
        }

        var isDelta = propsNode.getAttributeAsBool("delta_update", false);
        if (!isDelta) {
            props.clear();
            var responseRefresh = responseRefreshOpt.isPresent() ? responseRefreshOpt.getAsLong() : null;
            updateAttributesLocalStorage(responseAbKey, responseHash, responseRefresh, Instant.now());
        }
        propsNode.getAttributeAsLong("refresh_id")
                .ifPresent(this::setRefreshId);

        var propNodes = propsNode.getChildren("prop");
        var experimentCount = 0;
        var parsedSamplingConfigs = new LinkedHashMap<Integer, Integer>();
        for (var propNode : propNodes) {
            var configCode = propNode.getAttributeAsInt("config_code");
            var configValue = propNode.getAttributeAsString("config_value");
            if (configCode.isPresent() && configValue.isPresent()) {
                props.put(configCode.getAsInt(), configValue.get());
                experimentCount++;
                continue;
            }

            var eventCode = propNode.getAttributeAsInt("event_code");
            var samplingWeight = propNode.getAttributeAsInt("sampling_weight");
            if (eventCode.isPresent() && samplingWeight.isPresent()) {
                var eventCodeValue = eventCode.getAsInt();
                var samplingWeightValue = samplingWeight.getAsInt();
                if (eventCodeValue < SAMPLING_EVENT_CODE_MIN) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING,
                                "skipping samplingconfig prop: event_code={0} below minimum {1}",
                                eventCodeValue, SAMPLING_EVENT_CODE_MIN);
                    }
                    continue;
                }
                if (samplingWeightValue < SAMPLING_WEIGHT_MIN
                        || samplingWeightValue > SAMPLING_WEIGHT_MAX) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING,
                                "skipping samplingconfig prop: sampling_weight={0} outside [{1}, {2}]",
                                samplingWeightValue, SAMPLING_WEIGHT_MIN, SAMPLING_WEIGHT_MAX);
                    }
                    continue;
                }
                parsedSamplingConfigs.put(eventCodeValue, samplingWeightValue);
                continue;
            }

            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "skipping prop matching neither experimentconfig nor samplingconfig");
            }
        }

        var samplingUpdated = !isDelta && updateEventSamplingConfigs(parsedSamplingConfigs);

        if (Log.INFO) {
            LOGGER.log(Level.INFO,
                    "synced {0} ab props and {1} sampling configs from server (delta={2}, samplingUpdated={3})",
                    experimentCount, parsedSamplingConfigs.size(), isDelta, samplingUpdated);
        }

        var eridNode = response.getChild("erid", null);
        if (eridNode != null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ab props response included <erid> blob");
        }

        return true;
    }

    /**
     * Returns the AB key most recently received from the server.
     *
     * <p>Embedders that mirror WA Web's telemetry surface write this as the WAM {@code abKey2}
     * global attribute (field 4473) on the {@code regular} channel unless the
     * {@code wam_disable_abkey_attribute} prop is enabled.
     *
     * @return the AB key, or empty when the server has not provided one
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "getABKey",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<String> abKey() {
        return client.store().syncStore().abPropsAbKey();
    }

    /**
     * Returns the {@code hash} attribute most recently received from the server.
     *
     * <p>The hash is sent on subsequent sync requests so the relay can reply with a delta update
     * instead of the full prop list.
     *
     * @return the AB-props hash, or empty when no sync has completed
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "getHash",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<String> hash() {
        return client.store().syncStore().abPropsHash();
    }

    /**
     * Returns the configured refresh interval in seconds.
     *
     * <p>Falls back to {@value #REFRESH_DEFAULT_SECONDS} (one day) when no value has been recorded
     * yet.
     *
     * @return the refresh interval in seconds
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "getRefresh",
            adaptation = WhatsAppAdaptation.DIRECT)
    public long refresh() {
        return client.store().syncStore().abPropsRefresh()
                .orElse(REFRESH_DEFAULT_SECONDS);
    }

    /**
     * Returns the timestamp of the most recent successful sync.
     *
     * <p>Persisted as the {@code lastSyncTime} field of the {@code ABPROPS} JSON blob in WA Web.
     *
     * @return the last sync timestamp, or empty when none has been recorded
     */
    public Optional<Instant> lastSyncTime() {
        return client.store().syncStore().abPropsLastSyncTime();
    }

    /**
     * Returns whether at least one sync has completed since the session was created.
     *
     * <p>Used by {@link #sync(Long, boolean)} to gate inclusion of the persisted hash on the
     * regular branch.
     *
     * @implNote
     * WA Web tests whether the persisted {@code ABPROPS} JSON blob is present at all. Because
     * Cobalt stores the attributes as separate store fields rather than as a single JSON blob,
     * this implementation approximates that predicate by checking whether any of the three
     * persisted attributes (last sync time, AB key, hash) has been populated.
     *
     * @return {@code true} when a previous sync established AB-props state, {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "isABPropsAfterFirstSync",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean isAfterFirstSync() {
        var store = client.store();
        return store.syncStore().abPropsLastSyncTime().isPresent()
                || store.syncStore().abPropsAbKey().isPresent()
                || store.syncStore().abPropsHash().isPresent();
    }

    /**
     * Persists the {@code ABPROPS} blob attributes returned on the most recent sync.
     *
     * <p>Each non-{@code null} parameter overwrites the corresponding store field; a {@code null}
     * parameter keeps the previous value, matching the {@code abKey ?? m.abKey} fallback chain in
     * the JS source. Called by {@link #process(Stanza)} on full (non-delta) updates.
     *
     * @implNote
     * This implementation clamps {@code refreshSeconds} to the closed range
     * [{@value #REFRESH_MIN_SECONDS}, {@value #REFRESH_MAX_SECONDS}] before persisting, matching WA
     * Web's pre-write clamping inside
     * {@code WAWebABPropsLocalStorage.updateAttributesLocalStorage}.
     *
     * @param abKey          the AB key to persist, or {@code null} to keep the previous value
     * @param hash           the AB-props hash to persist, or {@code null} to keep the previous
     *                       value
     * @param refreshSeconds the refresh interval in seconds, or {@code null} to keep the previous
     *                       value
     * @param lastSyncTime   the sync completion instant
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "updateAttributesLocalStorage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void updateAttributesLocalStorage(String abKey, String hash, Long refreshSeconds, Instant lastSyncTime) {
        var store = client.store();
        if (abKey != null) {
            store.syncStore().setAbPropsAbKey(abKey);
        }
        if (hash != null) {
            store.syncStore().setAbPropsHash(hash);
        }
        if (refreshSeconds != null) {
            long clamped;
            if (refreshSeconds < REFRESH_MIN_SECONDS) {
                clamped = REFRESH_MIN_SECONDS;
            } else if (refreshSeconds > REFRESH_MAX_SECONDS) {
                clamped = REFRESH_MAX_SECONDS;
            } else {
                clamped = refreshSeconds;
            }
            store.syncStore().setAbPropsRefresh(clamped);
        }
        store.syncStore().setAbPropsLastSyncTime(lastSyncTime);
    }

    /**
     * Returns the AB-props refresh id.
     *
     * <p>Sent as the {@code propsRefreshId} attribute on the next sync request when the justknobx
     * {@code 3330} emergency push gate is enabled.
     *
     * @return the AB-props refresh id, or {@code 0} when never set
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "getRefreshId",
            adaptation = WhatsAppAdaptation.DIRECT)
    public long refreshId() {
        return client.store().syncStore().abPropsRefreshId();
    }

    /**
     * Persists the AB-props refresh id received from the server.
     *
     * <p>Called from {@link #process(Stanza)} when the response carries a {@code refresh_id}
     * attribute.
     *
     * @param refreshId the refresh id to persist
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "setRefreshId",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void setRefreshId(long refreshId) {
        client.store().syncStore().setAbPropsRefreshId(refreshId);
    }

    /**
     * Returns the web-only AB-props refresh id.
     *
     * <p>Gates the justknobx {@code 2086} emergency push request specific to the web tier.
     *
     * @return the web AB-props refresh id, or {@code 0} when never set
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "getWebRefreshId",
            adaptation = WhatsAppAdaptation.DIRECT)
    public long webRefreshId() {
        return client.store().syncStore().abPropsWebRefreshId();
    }

    /**
     * Persists the web-only AB-props refresh id received from the server.
     *
     * @param webRefreshId the refresh id to persist
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "setWebRefreshId",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void setWebRefreshId(long webRefreshId) {
        client.store().syncStore().setAbPropsWebRefreshId(webRefreshId);
    }

    /**
     * Returns the group AB-props refresh id received from the server.
     *
     * <p>Used by the group AB-props sync job to gate per-group refreshes.
     *
     * @return the group refresh id, or {@code 0} when never set
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "getGroupAbPropsRefreshId",
            adaptation = WhatsAppAdaptation.DIRECT)
    public long groupAbPropsRefreshId() {
        return client.store().syncStore().groupAbPropsRefreshId();
    }

    /**
     * Persists the group AB-props refresh id received from the server.
     *
     * @param groupRefreshId the refresh id to persist
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "setGroupAbPropsRefreshId",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void setGroupAbPropsRefreshId(long groupRefreshId) {
        client.store().syncStore().setGroupAbPropsRefreshId(groupRefreshId);
    }

    /**
     * Returns the timestamp of the last group AB-props emergency push recorded on the
     * {@code <success>} stanza.
     *
     * <p>Used by the group AB-props sync job to decide whether the locally cached group props are
     * stale relative to the latest server-pushed emergency notification.
     *
     * @return the last emergency push timestamp, or empty when none has been recorded
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "getGroupAbPropsEmergencyPushTimestamp",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Instant> groupAbPropsEmergencyPushTimestamp() {
        return client.store().syncStore().groupAbPropsEmergencyPushTimestamp();
    }

    /**
     * Persists the timestamp of the last group AB-props emergency push.
     *
     * @param timestamp the emergency push timestamp
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsLocalStorage", exports = "setGroupAbPropsEmergencyPushTimestamp",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void setGroupAbPropsEmergencyPushTimestamp(Instant timestamp) {
        client.store().syncStore().setGroupAbPropsEmergencyPushTimestamp(timestamp);
    }

    /**
     * Fetches the per-group AB-props bundle from the relay and projects the response into a typed
     * {@link GroupAbPropsResult}.
     *
     * <p>This drives per-group experiment overrides for group surfaces. It returns empty on any
     * failure variant, including the relay returning a non-success response or the client raising
     * {@link WhatsAppServerRuntimeException}.
     *
     * @implNote
     * This implementation drops the exposure key at the bundle layer because consumers observing
     * the result via {@link #abPropConfigs()} only key on {@code configCode} and
     * {@code configValue}.
     *
     * @param groupJid  the target group JID; never {@code null}
     * @param propsHash the cached group-props hash, or {@code null} for an unconditional fetch
     * @return an {@link Optional} carrying the projected result on success, empty on any failure
     *         variant
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAGetGroupAbPropsProtocol", exports = "getGroupAbPropsProtocol",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public Optional<GroupAbPropsResult> getGroupAbPropsProtocol(Jid groupJid, String propsHash) {
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        try {
            var bundle = client.queryGroupExperimentConfig(groupJid, propsHash).orElse(null);
            if (bundle == null) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "get group ab props protocol failed for {0}: response did not parse as success",
                            groupJid);
                }
                return Optional.empty();
            }

            var entries = new ArrayList<GroupAbPropsResult.Entry>(bundle.experiments().size());
            for (var experiment : bundle.experiments().entrySet()) {
                entries.add(new GroupAbPropsResult.Entry(experiment.getKey(), experiment.getValue(), null));
            }
            var result = new GroupAbPropsResult(
                    groupJid,
                    bundle.hash().orElse(null),
                    bundle.refresh().orElse(null),
                    bundle.refreshId().orElse(null),
                    entries);
            emitGroupExposurePulse(groupJid, entries);
            return Optional.of(result);
        } catch (WhatsAppServerRuntimeException e) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "get group ab props protocol failed for " + new LogRedactable.User(String.valueOf(groupJid)), e);
            }
            return Optional.empty();
        }
    }

    /**
     * Returns an unmodifiable snapshot of every synced AB prop keyed by its numeric
     * {@code config_code}.
     *
     * <p>The snapshot is intended for diagnostics or for exporting the synced state to a
     * debug-overlay UI; {@link #getString(ABProp)} and the other typed accessors are the read path
     * for individual props.
     *
     * @return a defensive copy of the {@code config_code -> config_value} map
     */
    @WhatsAppWebExport(moduleName = "WAWebApiAbPropConfig", exports = "getABPropConfigs",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public Map<Integer, String> abPropConfigs() {
        return Map.copyOf(props);
    }

    /**
     * Flags {@code prop} as having been read by the host application, driving the WAM exposure-key
     * attribute.
     *
     * <p>Embedders that mirror WA Web's exposure telemetry call this on first read of each prop so
     * the WAM server can attribute downstream events to the right experiment cell. A first access
     * grows the accessed-config set and therefore changes the combined client exposure key, so it
     * fires the real-time client-exposure pulse, matching WA Web's {@code updateGlobalExpoKey}
     * dispatch that debounces into the {@code WefrClientExposure} beacon.
     *
     * @param prop the AB prop that was just read
     * @return {@code true} when this is the first access, {@code false} when the prop was already
     *         flagged
     * @throws NullPointerException when {@code prop} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebApiAbPropConfig", exports = "setConfigAccessed",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebABPropsGlobals", exports = "updateGlobalExpoKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean setConfigAccessed(ABProp prop) {
        Objects.requireNonNull(prop, "prop cannot be null");
        var firstAccess = accessedConfigs.add(prop.code());
        if (firstAccess) {
            emitClientExposurePulse(false);
        }
        return firstAccess;
    }

    /**
     * Returns whether {@code prop} has previously been flagged as accessed.
     *
     * <p>Code paths that need to know whether the WAM exposure attribute has already been emitted
     * for an AB prop query this predicate.
     *
     * @param prop the AB prop to query
     * @return {@code true} when {@link #setConfigAccessed(ABProp)} has already been called for
     *         {@code prop}
     * @throws NullPointerException when {@code prop} is {@code null}
     */
    public boolean isConfigAccessed(ABProp prop) {
        Objects.requireNonNull(prop, "prop cannot be null");
        return accessedConfigs.contains(prop.code());
    }

    /**
     * Returns the raw string value for {@code prop}, blocking until the first sync completes.
     *
     * <p>Equivalent to {@code getString(prop, true)}. The fallback is
     * {@link ABProp#debugDefaultValue()} when the WhatsApp Web Beta flag is set on the store,
     * otherwise {@link ABProp#defaultValue()}.
     *
     * @param prop the AB prop definition
     * @return the string value, or the appropriate default
     */
    public String getString(ABProp prop) {
        return getString(prop, true);
    }

    /**
     * Returns the raw string value for {@code prop}, optionally skipping the wait for the first
     * sync.
     *
     * <p>Passing {@code waitForSync = false} reads from hot paths where blocking on the first sync
     * would stall an unrelated pipeline; the read then accepts the prop's default on a cold cache.
     *
     * @implNote
     * This implementation picks the debug default ({@link ABProp#debugDefaultValue()}) over the
     * production default when the store reports the external-web-beta flag is set, matching WA
     * Web's beta-build override.
     *
     * @param prop        the AB prop definition
     * @param waitForSync whether to block on {@link #awaitSync()} before reading
     * @return the string value, or the appropriate default
     */
    public String getString(ABProp prop, boolean waitForSync) {
        if (waitForSync) {
            awaitSync();
        }
        var defaultValue = client.store().syncStore().externalWebBeta() ? prop.debugDefaultValue() : prop.defaultValue();
        return props.getOrDefault(prop.code(), defaultValue);
    }

    /**
     * Returns the boolean value for {@code prop}, blocking until the first sync completes.
     *
     * <p>Equivalent to {@code getBool(prop, true)}; falls back to the default when the synced value
     * is unparseable.
     *
     * @param prop the AB prop definition
     * @return the parsed boolean, or the default
     * @see ABProp#toBoolean(String)
     */
    public boolean getBool(ABProp prop) {
        return getBool(prop, true);
    }

    /**
     * Returns the boolean value for {@code prop}, optionally skipping the wait for the first sync.
     *
     * <p>Passing {@code waitForSync = false} reads from hot paths where blocking on the first sync
     * would stall an unrelated pipeline.
     *
     * @param prop        the AB prop definition
     * @param waitForSync whether to block on {@link #awaitSync()} before reading
     * @return the parsed boolean, or the default
     * @see ABProp#toBoolean(String)
     */
    public boolean getBool(ABProp prop, boolean waitForSync) {
        return ABProp.toBoolean(getString(prop, waitForSync));
    }

    /**
     * Returns the integer value for {@code prop}, blocking until the first sync completes.
     *
     * <p>Equivalent to {@code getInt(prop, true)}; returns {@code 0} when neither the synced value
     * nor the default parses as an integer.
     *
     * @param prop the AB prop definition
     * @return the parsed integer, the default, or {@code 0}
     * @see ABProp#toInt(String)
     */
    public int getInt(ABProp prop) {
        return getInt(prop, true);
    }

    /**
     * Returns the integer value for {@code prop}, optionally skipping the wait for the first sync.
     *
     * <p>Passing {@code waitForSync = false} reads from hot paths where blocking on the first sync
     * would stall an unrelated pipeline.
     *
     * @param prop        the AB prop definition
     * @param waitForSync whether to block on {@link #awaitSync()} before reading
     * @return the parsed integer, the default, or {@code 0}
     * @see ABProp#toInt(String)
     */
    public int getInt(ABProp prop, boolean waitForSync) {
        var value = getString(prop, waitForSync);
        var result = ABProp.toInt(value);
        if (result.isPresent()) {
            return result.getAsInt();
        }
        var fallback = ABProp.toInt(prop.defaultValue());
        return fallback.orElse(0);
    }

    /**
     * Returns the long value for {@code prop}, blocking until the first sync completes.
     *
     * <p>Equivalent to {@code getLong(prop, true)}; returns {@code 0L} when neither the synced
     * value nor the default parses as a long.
     *
     * @param prop the AB prop definition
     * @return the parsed long, the default, or {@code 0L}
     * @see ABProp#toLong(String)
     */
    public long getLong(ABProp prop) {
        return getLong(prop, true);
    }

    /**
     * Returns the long value for {@code prop}, optionally skipping the wait for the first sync.
     *
     * <p>Passing {@code waitForSync = false} reads from hot paths where blocking on the first sync
     * would stall an unrelated pipeline.
     *
     * @param prop        the AB prop definition
     * @param waitForSync whether to block on {@link #awaitSync()} before reading
     * @return the parsed long, the default, or {@code 0L}
     * @see ABProp#toLong(String)
     */
    public long getLong(ABProp prop, boolean waitForSync) {
        var value = getString(prop, waitForSync);
        var result = ABProp.toLong(value);
        if (result.isPresent()) {
            return result.getAsLong();
        }
        var fallback = ABProp.toLong(prop.defaultValue());
        return fallback.orElse(0L);
    }

    /**
     * Returns the double value for {@code prop}, blocking until the first sync completes.
     *
     * <p>Equivalent to {@code getDouble(prop, true)}; returns {@code 0.0} when neither the synced
     * value nor the default parses as a double.
     *
     * @param prop the AB prop definition
     * @return the parsed double, the default, or {@code 0.0}
     * @see ABProp#toDouble(String)
     */
    public double getDouble(ABProp prop) {
        return getDouble(prop, true);
    }

    /**
     * Returns the double value for {@code prop}, optionally skipping the wait for the first sync.
     *
     * <p>Passing {@code waitForSync = false} reads from hot paths where blocking on the first sync
     * would stall an unrelated pipeline.
     *
     * @param prop        the AB prop definition
     * @param waitForSync whether to block on {@link #awaitSync()} before reading
     * @return the parsed double, the default, or {@code 0.0}
     * @see ABProp#toDouble(String)
     */
    public double getDouble(ABProp prop, boolean waitForSync) {
        var value = getString(prop, waitForSync);
        var result = ABProp.toDouble(value);
        if (result.isPresent()) {
            return result.getAsDouble();
        }
        var fallback = ABProp.toDouble(prop.defaultValue());
        return fallback.orElse(0.0);
    }

    /**
     * Returns the sampling-weight override for the given WAM event code.
     *
     * <p>Called from the WAM event commit site when the WAM property's hard-coded sampling weight
     * is overridable by the AB-props sampling channel.
     *
     * @param eventCode the WAM event identifier
     * @return the override weight, or empty when none was synced for this event
     */
    @WhatsAppWebExport(moduleName = "WAWebApiAbPropEventSamplingConfig",
            exports = "getEventSamplingWeight",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public OptionalInt getSamplingWeight(int eventCode) {
        var weight = samplingConfigs.get(eventCode);
        return weight != null ? OptionalInt.of(weight) : OptionalInt.empty();
    }

    /**
     * Returns an unmodifiable snapshot of every WAM event sampling-weight override parsed from the
     * most recent sync.
     *
     * <p>The snapshot is intended for diagnostics or for exporting the sampling state to a
     * debug-overlay UI.
     *
     * @return a defensive copy of the sampling-config map
     */
    @WhatsAppWebExport(moduleName = "WAWebApiAbPropEventSamplingConfig",
            exports = "getEventSamplingConfigs",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public Map<Integer, Integer> samplingConfigs() {
        return Map.copyOf(samplingConfigs);
    }

    /**
     * Replaces the sampling-config cache with the supplied entries.
     *
     * <p>Called from {@link #process(Stanza)} when the response is a full (non-delta) update carrying
     * sampling configs. A {@code null} or empty argument is a no-op and returns {@code false}; a
     * non-empty argument clears and replaces the cache and returns {@code true}, matching the JS
     * export contract.
     *
     * @param configs the new sampling configs, or {@code null} or empty for a no-op
     * @return {@code true} when the cache was replaced, {@code false} when the argument was
     *         {@code null} or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebApiAbPropEventSamplingConfig",
            exports = "updateEventSamplingConfigs",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean updateEventSamplingConfigs(Map<Integer, Integer> configs) {
        if (configs == null || configs.isEmpty()) {
            return false;
        }
        samplingConfigs.clear();
        samplingConfigs.putAll(configs);
        return true;
    }

    /**
     * Returns the number of synced AB props currently held in memory.
     *
     * <p>The count is intended for diagnostics; individual prop reads go through the typed
     * accessors.
     *
     * @return the count of synced props
     */
    public int size() {
        return props.size();
    }

    /**
     * Returns whether no AB props have been synced yet.
     *
     * <p>The predicate is intended for diagnostics; individual prop reads go through the typed
     * accessors.
     *
     * @return {@code true} when {@link #props} is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return props.isEmpty();
    }

    /**
     * Emits the daily {@link com.github.auties00.cobalt.wire.wam.event.WefrClientExposureEvent} pulse.
     *
     * <p>Re-emits the combined client exposure key with the {@code sentWithDaily} flag set, so the
     * WAM server observes a once-per-day exposure beacon alongside the private-stats heartbeat in
     * addition to the real-time change pulses fired from {@link #setConfigAccessed(ABProp)}. The
     * intended cadence is one call per daily-stats cycle; it is a no-op until a WAM sink is wired
     * through {@link #setWamService(WamService)}.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code logClientExposurePulseEventFromDailyStatsTask}
     * export, which the daily-stats task invokes on its once-per-day tick.
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsGlobals",
            exports = "logClientExposurePulseEventFromDailyStatsTask",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void logClientExposurePulseFromDailyStatsTask() {
        emitClientExposurePulse(true);
    }

    /**
     * Commits a {@link com.github.auties00.cobalt.wire.wam.event.WefrClientExposureEvent} carrying the
     * combined client exposure key.
     *
     * <p>Backs both the real-time client-exposure pulse WA Web fires from
     * {@code WAWebABPropsGlobals.updateGlobalExpoKey} (the {@code sentWithDaily = false} branch) and
     * the daily-stats variant {@code logClientExposurePulseEventFromDailyStatsTask} (the
     * {@code sentWithDaily = true} branch). Does nothing when no WAM sink has been wired through
     * {@link #setWamService(WamService)}.
     *
     * @implNote
     * This implementation derives the exposure key from the accessed config codes joined by commas,
     * the Cobalt analog of {@code combineExposuresIntoExpoKey(exposureKeys)}, and reports
     * canonical-entity presence from the store login state because Cobalt has no browser canonical
     * token counterpart. The other schema fields ({@code deviceExpId}, {@code guestId},
     * {@code userLid}, {@code fromMetaconfig}, {@code canonicalEntLastValidationTsMs}) are left
     * unset because WA Web does not populate them at this callsite either.
     *
     * @param sentWithDaily whether this pulse rides the daily-stats task rather than the debounced
     *                      real-time exposure-key change
     */
    private void emitClientExposurePulse(boolean sentWithDaily) {
        var sink = wamService;
        if (sink == null) {
            return;
        }
        sink.commit(new WefrClientExposureEventBuilder()
                .exposureKey(currentClientExposureKey())
                .sentWithDaily(sentWithDaily)
                .isCanonicalEntPresent(isCanonicalEntPresent())
                .build());
    }

    /**
     * Commits a {@link com.github.auties00.cobalt.wire.wam.event.WefrGroupClientExposureEvent} for the
     * resolved group props.
     *
     * <p>Mirrors the debounced per-group exposure pulse WA Web fires from
     * {@code WAWebGroupABPropsGlobals.updateGroupExpoKey} (the {@code sentWithDaily = false}
     * branch). Does nothing when no WAM sink has been wired through
     * {@link #setWamService(WamService)}, or when the resolved bundle carried no experiment entries.
     *
     * @implNote
     * This implementation derives the group exposure key from the resolved experiment config codes
     * joined by commas, the Cobalt analog of the group-scoped
     * {@code combineExposuresIntoExpoKey} over the per-group exposure-key set.
     *
     * @param groupJid the group whose exposure is being reported
     * @param entries  the resolved experiment entries for the group
     */
    @WhatsAppWebExport(moduleName = "WAWebGroupABPropsGlobals", exports = "updateGroupExpoKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitGroupExposurePulse(Jid groupJid, List<GroupAbPropsResult.Entry> entries) {
        var sink = wamService;
        if (sink == null || entries.isEmpty()) {
            return;
        }
        var exposureKey = entries.stream()
                .map(GroupAbPropsResult.Entry::configCode)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        sink.commit(new WefrGroupClientExposureEventBuilder()
                .exposureKey(exposureKey)
                .groupJid(groupJid.toString())
                .sentWithDaily(false)
                .build());
    }

    /**
     * Returns the combined client exposure key: the accessed config codes sorted ascending and
     * joined by commas.
     *
     * <p>This is the Cobalt analog of {@code WAWebABPropsExpoKeyUtils.combineExposuresIntoExpoKey}
     * applied to the global exposure-key set; Cobalt keys the exposure surface on the accessed
     * config codes rather than the server-supplied per-config exposure-key strings it does not
     * retain.
     *
     * @return the comma-joined exposure key, or the empty string when no config has been accessed
     */
    @WhatsAppWebExport(moduleName = "WAWebABPropsExpoKeyUtils", exports = "combineExposuresIntoExpoKey",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private String currentClientExposureKey() {
        return accessedConfigs.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * Returns whether a canonical entity credential is present for the current session.
     *
     * <p>Mirrors {@code WAWebCanonicalUtils.isCanonicalPresent}, which is satisfied when the user is
     * logged in or a canonical token has been installed. Cobalt has no browser canonical token, so
     * this implementation reports presence from the store login state, that is, a bound account JID.
     *
     * @return {@code true} when the session is authenticated, {@code false} otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebCanonicalUtils", exports = "isCanonicalPresent",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isCanonicalEntPresent() {
        return client.store().accountStore().jid().isPresent();
    }

    /**
     * Resets the service for a fresh session by clearing every in-memory cache, dropping the
     * persisted local-storage attributes that depend on the current session, and replacing the
     * sync future so subsequent queries block on the next sync.
     *
     * <p>Called on logout. The refresh interval and refresh ids stay at their persisted values
     * because the JS exports also leave them in place across sign-outs.
     */
    public void clear() {
        props.clear();
        samplingConfigs.clear();
        accessedConfigs.clear();
        var store = client.store();
        store.syncStore().setAbPropsAbKey(null);
        store.syncStore().setAbPropsHash(null);
        store.syncStore().setAbPropsLastSyncTime(null);
        syncFuture.set(new CompletableFuture<>());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cleared all ab props and reset sync state");
    }
}
