package com.github.auties00.cobalt.calls.config.param;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies matched dynamic rate control overrides onto a live voip param set, guarded by an
 * already updated set.
 *
 * <p>Each rate control round, the rule engine selects the dynamic rules whose
 * {@link VoipParamCondition}s all hold and collects their overrides. This updater writes
 * those overrides onto the live {@link VoipParams}. The native engine guards the write with
 * an "already updated" bitmap so that, once a parameter has been set by a higher priority
 * rule in the current round, a later rule cannot clobber it; this updater reproduces that
 * guard with a {@link Set} of the keys written so far in the round.
 *
 * <p>An updater instance holds the guard set for one round. {@link #beginRound()} clears the
 * guard so a fresh round starts with every parameter eligible; {@link #apply(VoipParams,
 * List)} writes each override whose key has not yet been written this round and records it.
 * The override carries its value decoded in advance into a {@link DynRuleEntry}, so this updater
 * does not parse the raw little endian value bytes itself; that decoding belongs to the
 * rate control reader that owns the rule table wire format.
 *
 * <p>This updater is single writer: the call's rate control tick drives it from one thread,
 * matching the single transport thread model the rest of the rate control stack uses.
 *
 * @implNote This implementation keys each override by its {@link VoipParamKey} and keeps the
 * already updated guard as a {@link Set} of written keys rather than as a bit index into the
 * parameter table, so the guard needs no fixed parameter ordering.
 */
public final class DynVoipParamUpdater {
    /**
     * The logger for {@link DynVoipParamUpdater}.
     */
    private static final System.Logger LOGGER = Log.get(DynVoipParamUpdater.class);

    /**
     * The keys already written in the current round, which later rules must not clobber.
     */
    private final Set<VoipParamKey> updated;

    /**
     * Constructs a dynamic voip param updater with an empty guard.
     *
     * <p>The new updater starts a round in which no parameter has yet been written; callers
     * may apply overrides immediately or call {@link #beginRound()} first.
     */
    public DynVoipParamUpdater() {
        this.updated = new HashSet<>();
    }

    /**
     * Clears the already updated guard to start a fresh rate control round.
     *
     * <p>After this call every parameter is eligible to be written again, so the next
     * {@link #apply(VoipParams, List)} reflects the current round's matched rules without
     * carrying over the previous round's writes.
     */
    public void beginRound() {
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "dynamic voip param round reset, previousCount={0}", updated.size());
        }
        updated.clear();
    }

    /**
     * Applies the given overrides onto the live parameter set, honouring the guard.
     *
     * <p>The overrides are processed in order; an override is written only if its key has
     * not already been written in the current round, after which the key is recorded so a
     * later override for the same key in the same list is skipped. The number of overrides
     * actually written is returned so the caller can tell whether the round changed
     * anything.
     *
     * @param params    the live parameter set to write into
     * @param overrides the matched overrides for this round, in priority order
     * @return the count of overrides written
     * @throws NullPointerException if {@code params} or {@code overrides} is {@code null}
     */
    public int apply(VoipParams params, List<DynRuleEntry> overrides) {
        if (params == null) {
            throw new NullPointerException("params must not be null");
        }
        if (overrides == null) {
            throw new NullPointerException("overrides must not be null");
        }
        var written = 0;
        for (var override : overrides) {
            if (updated.add(override.key())) {
                override.writeOnto(params);
                written++;
            }
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "applied dynamic voip param overrides, matched={0}, written={1}", overrides.size(), written);
        }
        return written;
    }

    /**
     * Returns whether the given key has already been written in the current round.
     *
     * @param key the key to test
     * @return {@code true} if the key has been written this round, {@code false} otherwise
     */
    public boolean isUpdated(VoipParamKey key) {
        return updated.contains(key);
    }

    /**
     * Carries one matched dynamic override: the parameter to set and its decoded value.
     *
     * <p>The parameter is identified by a {@link VoipParamKey} and the value is held
     * decoded in advance into the view matching the key's {@link VoipParamType}. Exactly one of the
     * scalar value views is meaningful for a given entry, selected by the key's type; an
     * integer override carries its value in {@link #integerValue()}, a floating point
     * override in {@link #doubleValue()}, and a string override in {@link #stringValue()}.
     * The two unused views hold their zero defaults and must not be read.
     *
     * @param key          the parameter this override sets
     * @param integerValue the integer value, meaningful when the key's type is integer
     * @param doubleValue  the floating point value, meaningful when the key's type is float
     * @param stringValue  the string value, meaningful when the key's type is string
     */
    public record DynRuleEntry(VoipParamKey key, long integerValue, double doubleValue, String stringValue) {
        /**
         * Returns an override that sets an integer parameter to the given value.
         *
         * @param key   the integer parameter to set
         * @param value the integer value to write
         * @return the override
         * @throws IllegalArgumentException if the key's type is not
         *                                  {@link VoipParamType#INTEGER}
         */
        public static DynRuleEntry ofInteger(VoipParamKey key, long value) {
            if (key.type() != VoipParamType.INTEGER) {
                throw new IllegalArgumentException("not an integer param: " + key);
            }
            return new DynRuleEntry(key, value, 0.0, null);
        }

        /**
         * Returns an override that sets a floating point parameter to the given value.
         *
         * @param key   the floating point parameter to set
         * @param value the floating point value to write
         * @return the override
         * @throws IllegalArgumentException if the key's type is not
         *                                  {@link VoipParamType#FLOAT}
         */
        public static DynRuleEntry ofDouble(VoipParamKey key, double value) {
            if (key.type() != VoipParamType.FLOAT) {
                throw new IllegalArgumentException("not a floating-point param: " + key);
            }
            return new DynRuleEntry(key, 0L, value, null);
        }

        /**
         * Returns an override that sets a string parameter to the given value.
         *
         * @param key   the string parameter to set
         * @param value the string value to write
         * @return the override
         * @throws IllegalArgumentException if the key's type is not
         *                                  {@link VoipParamType#STRING}
         */
        public static DynRuleEntry ofString(VoipParamKey key, String value) {
            if (key.type() != VoipParamType.STRING) {
                throw new IllegalArgumentException("not a string param: " + key);
            }
            return new DynRuleEntry(key, 0L, 0.0, value);
        }

        /**
         * Writes this override's value onto the given parameter set under its key.
         *
         * <p>The value view matching the key's {@link VoipParamType} is written; an override
         * for a key of any other type has no effect, since only scalar parameters carry dynamic
         * overrides.
         *
         * @param params the parameter set to write into
         */
        void writeOnto(VoipParams params) {
            switch (key.type()) {
                case INTEGER -> params.putInteger(key, integerValue);
                case FLOAT -> params.putDouble(key, doubleValue);
                case STRING -> params.putString(key, stringValue);
                case ARRAY, ARRAY_COUNT, UNKNOWN -> {
                    // Array parameters carry no scalar dynamic override, and an unmodelled key has no
                    // typed override to apply.
                    if (Log.TRACE) {
                        LOGGER.log(Level.TRACE, "dynamic voip param override skipped, unsupported type for key={0}", key);
                    }
                }
            }
        }
    }
}
