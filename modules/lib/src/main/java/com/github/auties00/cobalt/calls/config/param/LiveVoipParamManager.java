package com.github.auties00.cobalt.calls.config.param;

import com.github.auties00.cobalt.calls.config.VoipSettingsType;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Owns the voip param sets for one call: the stored raw sets keyed by device JID and settings
 * type, the selected active set, the participant count override, and the dynamic rate control engine.
 *
 * <p>This manager drives the voip param lifecycle the specification describes. A parsed
 * {@link VoipParams} set is stored under a {@code (device JID, settings type)} key with
 * {@link #store(Jid, VoipSettingsType, VoipParams)}; the in use set is chosen for a given
 * device with {@link #selectActive(Jid, VoipSettingsType)}, which copies the stored baseline so
 * the stored set is never mutated by later overrides; the active set is retuned for the current
 * call size with {@link #overrideForParticipantCount(int, List)}; and each rate control
 * round its dynamic overrides are written onto the active set through
 * {@link #applyDynamicRules(List)} under the round guard of the manager's
 * {@link DynVoipParamUpdater}.
 *
 * <p>The server keys each {@code <voip_settings>} bundle by the device it applies to: the offer
 * acknowledgement delivers one bundle tagged with {@code jid} per callee device (the caller selects the
 * answering device's bundle), while the delivered offer carries a single untagged bundle (the
 * callee's own config, stored under a {@code null} device key). Selection therefore takes a device
 * JID: it resolves that device's stored bundle, falling back to the untagged ({@code null}) own
 * bundle when the device has none. Within the chosen device, an absent requested settings type
 * falls back to the mandatory {@link VoipSettingsType#NONE} default; selecting when even that is
 * absent leaves the manager with no active set.
 *
 * <p>Selecting an active set always starts from a fresh copy of the stored baseline, so
 * participant count and dynamic overrides accumulate only onto the current selection and are
 * discarded when a different device or settings type is selected.
 *
 * <p>The manager is guarded by a single {@link ReentrantLock} so the rate control tick, the
 * participant count retune, and any reconfiguration that restores stored sets observe a
 * consistent active set. Reads of the active set return a copy, so a caller never holds a
 * reference that a concurrent override can mutate underneath it.
 *
 * @implNote This implementation copies the sparse {@link VoipParams} map on both store and
 * select rather than cloning a fixed size parameter struct, so a stored baseline is snapshotted
 * cheaply and the active set never shares mutable state with the stored bundle it was promoted
 * from.
 */
public final class LiveVoipParamManager {
    /**
     * The logger for {@link LiveVoipParamManager}.
     */
    private static final System.Logger LOGGER = Log.get(LiveVoipParamManager.class);

    /**
     * The stored raw parameter sets, keyed by device JID then by settings type.
     *
     * <p>The outer key is the device JID a {@code <voip_settings>} bundle was tagged with, or
     * {@code null} for the untagged own bundle the delivered offer carries; the inner
     * {@link EnumMap} holds that device's bundle per {@link VoipSettingsType}. Iteration follows
     * insertion (wire) order, so the first entry is the first delivered bundle, the first peer device
     * a group selection falls back to.
     */
    private final Map<Jid, EnumMap<VoipSettingsType, VoipParams>> stored;

    /**
     * The dynamic rate control updater whose round guard spans one rate control tick.
     */
    private final DynVoipParamUpdater dynamicUpdater;

    /**
     * The lock serialising store, select, count override, and dynamic rule passes.
     */
    private final ReentrantLock lock;

    /**
     * The settings type currently selected as active, or {@code null} when none is selected.
     */
    private VoipSettingsType activeType;

    /**
     * The live active parameter set, or {@code null} when none is selected.
     */
    private VoipParams active;

    /**
     * Constructs a voip param manager with no stored sets and no active selection.
     *
     * <p>The new manager holds an empty store and a fresh {@link DynVoipParamUpdater};
     * callers populate it by storing parsed sets and then selecting one active.
     */
    public LiveVoipParamManager() {
        this.stored = new LinkedHashMap<>();
        this.dynamicUpdater = new DynVoipParamUpdater();
        this.lock = new ReentrantLock();
    }

    /**
     * Stores a parsed parameter set under the given device JID and settings type.
     *
     * <p>A previously stored set under the same {@code (deviceJid, type)} pair is replaced.
     * Distinct device JIDs do not overwrite each other, so the per device offer acknowledgement
     * bundles coexist rather than collapsing. A {@code null} device JID stores the untagged own
     * bundle the delivered offer carries. Storing does not change the active selection; a
     * subsequent {@link #selectActive(Jid, VoipSettingsType)} is needed to promote a stored set.
     *
     * @param deviceJid the device JID the bundle is tagged with, or {@code null} for the untagged
     *                  own bundle
     * @param type      the settings type to store under
     * @param params    the parsed parameter set to store
     * @throws NullPointerException if {@code type} or {@code params} is {@code null}
     */
    public void store(Jid deviceJid, VoipSettingsType type, VoipParams params) {
        if (type == null) {
            throw new NullPointerException("type must not be null");
        }
        if (params == null) {
            throw new NullPointerException("params must not be null");
        }
        lock.lock();
        try {
            stored.computeIfAbsent(deviceJid, key -> new EnumMap<>(VoipSettingsType.class))
                    .put(type, params);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "stored voip params, device={0}, type={1}, size={2}", deviceJid, type, params.size());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Selects the stored set for the given device and settings type as the active set.
     *
     * <p>The device's stored bundle is resolved first: when the requested device JID has no stored
     * bundle (or is {@code null}), selection falls back to the untagged ({@code null}) own bundle, and
     * when neither is present to the first stored bundle in wire order (the group call case: that first entry
     * is the first peer participant device, and a codec homogeneous group resolves the same codec from any of
     * its peer bundles). Within the chosen device,
     * when the requested settings type is absent the mandatory {@link VoipSettingsType#NONE} default is
     * selected instead. The active set is initialised from a fresh copy of that baseline, so prior
     * participant count or dynamic overrides do not carry across selections, and the dynamic rule round
     * guard is reset. If no bundle yields a set of the requested type or the default, no active set is
     * selected and {@link #activeType()} becomes empty.
     *
     * @param deviceJid the device JID whose bundle to promote, or {@code null} to promote the
     *                  untagged own bundle directly
     * @param type      the settings type to promote
     * @return the settings type actually selected, or {@link Optional#empty()} if no matching set
     *         is stored
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public Optional<VoipSettingsType> selectActive(Jid deviceJid, VoipSettingsType type) {
        if (type == null) {
            throw new NullPointerException("type must not be null");
        }
        lock.lock();
        try {
            var deviceSets = stored.get(deviceJid);
            if (deviceSets == null) {
                deviceSets = stored.get(null);
            }
            if (deviceSets == null && !stored.isEmpty()) {
                // No bundle for the requested device and no untagged own bundle: the group call case,
                // where the offer acknowledgement delivers one bundle per peer participant device. Select the
                // first peer device's bundle in wire order (the stored map preserves insertion order). Every
                // stored bundle is a peer device bundle and a group's forwarded single stream is
                // codec homogeneous, so this resolves the one codec the local side encodes for all peers.
                deviceSets = stored.values().iterator().next();
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "voip params: no bundle for device={0} or own bundle, falling back to first peer device bundle", deviceJid);
                }
            }
            if (deviceSets == null) {
                activeType = null;
                active = null;
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "voip params: no stored bundle available, device={0}, requestedType={1}", deviceJid, type);
                }
                return Optional.empty();
            }
            var chosenType = type;
            var baseline = deviceSets.get(chosenType);
            if (baseline == null) {
                chosenType = VoipSettingsType.NONE;
                baseline = deviceSets.get(chosenType);
            }
            if (baseline == null) {
                activeType = null;
                active = null;
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "voip params: no set of requestedType={0} or default, device={1}", type, deviceJid);
                }
                return Optional.empty();
            }
            activeType = chosenType;
            active = new VoipParams(baseline);
            dynamicUpdater.beginRound();
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "voip params active set selected, device={0}, requestedType={1}, selectedType={2}", deviceJid, type, chosenType);
            }
            return Optional.of(chosenType);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Applies participant count overrides onto the active set for the current call size.
     *
     * <p>The supplied overrides are the count dependent parameter values for a call of the
     * given participant count; they are written onto the active set directly, outside the
     * dynamic rule round guard, because the count retune is a one shot reconfiguration rather
     * than a per round adjustment. With no active set, this is a no op.
     *
     * @param participantCount the current number of participants in the call
     * @param overrides        the count dependent overrides to write
     * @return the count of overrides written, or {@code 0} if no set is active
     * @throws NullPointerException     if {@code overrides} is {@code null}
     * @throws IllegalArgumentException if {@code participantCount} is negative
     */
    public int overrideForParticipantCount(int participantCount, List<DynVoipParamUpdater.DynRuleEntry> overrides) {
        if (overrides == null) {
            throw new NullPointerException("overrides must not be null");
        }
        if (participantCount < 0) {
            throw new IllegalArgumentException("participant count must not be negative");
        }
        lock.lock();
        try {
            if (active == null) {
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "voip params: no active set, skipping participant count override, count={0}", participantCount);
                }
                return 0;
            }
            var written = 0;
            for (var override : overrides) {
                override.writeOnto(active);
                written++;
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "applied participant count overrides, count={0}, written={1}", participantCount, written);
            }
            return written;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Applies one rate control round's matched dynamic overrides onto the active set.
     *
     * <p>The round guard is reset before the overrides are written, so each round reflects
     * the rules matched this tick without carrying the previous tick's writes, and within the
     * round the {@link DynVoipParamUpdater}'s already updated guard prevents a lower priority
     * override from clobbering a higher priority one. With no active set, this is a no op.
     *
     * @param overrides the matched overrides for this round, in priority order
     * @return the count of overrides written, or {@code 0} if no set is active
     * @throws NullPointerException if {@code overrides} is {@code null}
     */
    public int applyDynamicRules(List<DynVoipParamUpdater.DynRuleEntry> overrides) {
        if (overrides == null) {
            throw new NullPointerException("overrides must not be null");
        }
        lock.lock();
        try {
            if (active == null) {
                if (Log.TRACE) {
                    LOGGER.log(Level.TRACE, "voip params: no active set, skipping dynamic rule round, matched={0}", overrides.size());
                }
                return 0;
            }
            dynamicUpdater.beginRound();
            return dynamicUpdater.apply(active, overrides);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a copy of the live active parameter set.
     *
     * <p>The returned set is a snapshot copy taken under the lock; mutating it does not affect
     * the manager's active set, and a concurrent override does not mutate the returned copy.
     *
     * @return a copy of the active set, or {@link Optional#empty()} if no set is active
     */
    public Optional<VoipParams> activeParams() {
        lock.lock();
        try {
            return active == null ? Optional.empty() : Optional.of(new VoipParams(active));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the settings type currently selected as active.
     *
     * @return the active settings type, or {@link Optional#empty()} if no set is active
     */
    public Optional<VoipSettingsType> activeType() {
        lock.lock();
        try {
            return Optional.ofNullable(activeType);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns whether any stored device bundle holds a parameter set of the given settings type.
     *
     * @param type the settings type to test
     * @return {@code true} if some device bundle holds a set of the type, {@code false} otherwise
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public boolean hasStored(VoipSettingsType type) {
        if (type == null) {
            throw new NullPointerException("type must not be null");
        }
        lock.lock();
        try {
            for (var deviceSets : stored.values()) {
                if (deviceSets.containsKey(type)) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears all stored sets and the active selection.
     *
     * <p>After this call the manager holds no stored sets and has no active set, matching the
     * engine's clear path at the end of a call.
     */
    public void clear() {
        lock.lock();
        try {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "cleared voip param manager, storedDevices={0}", stored.size());
            }
            stored.clear();
            activeType = null;
            active = null;
            dynamicUpdater.beginRound();
        } finally {
            lock.unlock();
        }
    }
}
