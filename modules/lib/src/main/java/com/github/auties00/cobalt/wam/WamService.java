package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.binary.WamGlobalEncoder;
import com.github.auties00.cobalt.wire.wam.event.WebWamForceFlushEvent;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.privatestats.WamPrivateStatsUploader;
import com.github.auties00.cobalt.wire.wam.type.PsIdAction;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Collects, batches, and uploads WhatsApp Metrics (WAM) telemetry events
 * across the regular, realtime, and private transport channels behind one
 * transport-agnostic contract.
 *
 * <p>A WAM service has a two-phase lifecycle. Construction leaves it dormant:
 * events {@linkplain #commit(WamEventSpec) committed} before
 * {@link #initialize()} are deferred to an internal init queue and replayed
 * once the pipeline is armed. {@link #initialize()} snapshots the session
 * globals, loads sampling overrides from {@link ABPropsService}, and arms the
 * recurring serialize and flush schedulers; {@link #close()} tears the
 * schedulers down and performs a final drain. During the active phase each
 * {@link #commit(WamEventSpec)} validates and samples the event, then appends
 * the kept event to its {@link WamChannel} pending list;
 * {@link WamChannel#REALTIME} events flush immediately while regular and
 * private events accumulate until a timer or size threshold rotates them onto
 * the wire.
 *
 * <p>The client constructs and drives the service. Its direct embedder entry
 * points are the sampling-override methods
 * ({@link #setSamplingOverride(int, int)}, {@link #removeSamplingOverride(int)},
 * {@link #replaceSamplingOverrides(Map)}) for overriding WA Web sampling
 * weights at runtime. The stateless WAM message-classification helpers that
 * other Cobalt modules use to tag their events live in {@link WamMsgUtils}.
 *
 * <p>The production implementation is {@link LiveWamService}, which binds the
 * clock and schedulers to the system UTC clock and to virtual-thread
 * recurrences; tests substitute recording or deterministic doubles.
 *
 * @see LiveWamService
 * @see WamEventSpec
 * @see WamGlobalEncoder
 * @see WamChannel
 * @see WamPrivateStatsUploader
 */
@WhatsAppWebModule(moduleName = "WAWebWam")
@WhatsAppWebExport(moduleName = "WAWebWam", exports = "Wam", adaptation = WhatsAppAdaptation.ADAPTED)
public interface WamService {
    /**
     * Activates the WAM pipeline for the bound client.
     *
     * <p>This must be called once, after the client has authenticated and
     * the store's JID, name, and client version are populated. The call
     * snapshots every session global, loads sampling overrides from
     * {@link ABPropsService#samplingConfigs()}, primes per-channel sequence
     * numbers from the bound client's store, restores any pending buffers
     * persisted by a previous session, drains the pre-init queue, and arms
     * the five-second mid-cycle and 120-second rotation schedulers. After
     * the schedulers are armed, one {@code PsIdUpdateEvent} with action
     * {@link PsIdAction#CREATED} is committed per private-stats id to
     * announce the freshly initialised id set.
     */
    @WhatsAppWebExport(moduleName = "WAWebWam", exports = "initWamRuntime", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWam", exports = "commitOnSet", adaptation = WhatsAppAdaptation.ADAPTED)
    void initialize();

    /**
     * Submits a WAM event for batched transmission on its declared
     * channel.
     *
     * <p>This is the entry point used by every internal WAM emitter (the
     * {@code *EventBuilder} classes in the
     * {@code com.github.auties00.cobalt.wire.wam.event} package) and by embedders
     * that mirror WA Web telemetry surfaces. The contract has four
     * post-conditions depending on state:
     * <ul>
     *   <li>before {@link #initialize()}, the entire commit (including
     *       validation, sampling, and dispatch) is deferred to an internal
     *       init queue and replayed on the next {@link #initialize()};</li>
     *   <li>a redundant commit of the same event instance is logged
     *       and dropped (the per-spec {@code markCommitted()} guard);</li>
     *   <li>a failed pre-commit validation is logged and dropped;</li>
     *   <li>otherwise the event is appended to its channel's pending
     *       list. A {@link WamChannel#REALTIME} event additionally
     *       triggers an immediate flush of that channel rather than
     *       waiting for the next rotation.</li>
     * </ul>
     *
     * @param event the event to commit, must not be {@code null}
     * @throws NullPointerException if {@code event} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamCodegenWamEvent", exports = "WamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamInitQueue", exports = "queueEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    void commit(WamEventSpec event);

    /**
     * Submits a WAM event for batched transmission and returns a
     * future that completes when the buffer carrying the event has
     * been uploaded.
     *
     * <p>This is used by callers that must observe upload completion (the
     * WAM surfaces in WA Web that synchronise on a Promise return value).
     * The completion future has three terminal states:
     * <ul>
     *   <li>completes with {@code null} once the buffer that contains
     *       the event has been fully uploaded;</li>
     *   <li>completes with {@code null} immediately if the event is
     *       redundantly committed (the spec's {@code markCommitted}
     *       guard), fails pre-commit validation, or is sampled out;</li>
     *   <li>before {@link #initialize()}, completes whenever the
     *       deferred replay's own future completes, via a
     *       {@code whenComplete} bridge installed at deferral time.</li>
     * </ul>
     *
     * @param event the event to commit, must not be {@code null}
     * @return the completion future
     * @throws NullPointerException if {@code event} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebWamCodegenWamEvent", exports = "WamEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebWamInitQueue", exports = "queueEvent", adaptation = WhatsAppAdaptation.ADAPTED)
    CompletableFuture<Void> commitAndWaitForFlush(WamEventSpec event);

    /**
     * Registers a runtime sampling-weight override for the given
     * event id.
     *
     * <p>This mirrors WA Web's
     * {@code WAWebEventSampling.getClientEventSamplingWeight} which returns
     * the AB-prop-supplied weight in preference to the static event
     * {@code weight}. The override takes precedence over
     * {@link WamEventSpec#releaseWeight()} until
     * {@link #removeSamplingOverride(int)} is called or until
     * {@link #replaceSamplingOverrides(Map)} replaces the whole map. A
     * weight of {@code 1} means "always keep" (sampling disabled for this
     * event); higher values keep {@code 1/weight} of commits in
     * expectation.
     *
     * @param eventId the numeric WAM event identifier
     * @param weight  the overridden sampling weight, expected positive
     */
    void setSamplingOverride(int eventId, int weight);

    /**
     * Removes any runtime sampling-weight override for the given
     * event id.
     *
     * <p>This reverts the effective weight to the spec-declared
     * {@link WamEventSpec#releaseWeight()} for the removed id; it is a
     * no-op when no override was installed.
     *
     * @param eventId the numeric WAM event identifier
     */
    void removeSamplingOverride(int eventId);

    /**
     * Replaces every runtime sampling-weight override with the entries
     * from the given map.
     *
     * <p>{@link #initialize()} calls this to seed the override table from
     * {@link ABPropsService#samplingConfigs()}; embedders that mirror a
     * different AB-prop fetch flow can use it to swap the whole table
     * atomically rather than installing entries one by one.
     *
     * @param overrides the new event-id-to-weight map
     */
    void replaceSamplingOverrides(Map<Integer, Integer> overrides);

    /**
     * Flushes every channel's pending list.
     *
     * <p>This is bound to the 120-second rotation scheduler armed by
     * {@link #initialize()} and is also called explicitly from
     * {@link #close()} for a terminal flush. Each channel's pending list
     * is atomically swapped and piped through the encoder, retry, and
     * upload pipeline. It is a no-op when the service is not initialised.
     */
    @WhatsAppWebExport(moduleName = "WAWebWam", exports = "sendAllLogs", adaptation = WhatsAppAdaptation.ADAPTED)
    void flush();

    /**
     * Tears down the recurring schedulers, commits the
     * {@link WebWamForceFlushEvent} sentinel, performs a final drain of
     * any buffered events, and clears the initialised state.
     *
     * <p>The sentinel is committed before the terminal drain so it ships
     * in the same batch, marking the buffer as force-flushed on shutdown
     * rather than rotated by the scheduler. After this returns, every
     * {@link #commit(WamEventSpec)} again defers to the init queue and
     * {@link #flush()} becomes a no-op; a subsequent {@link #initialize()}
     * call is required to resume the pipeline.
     */
    void close();
}
