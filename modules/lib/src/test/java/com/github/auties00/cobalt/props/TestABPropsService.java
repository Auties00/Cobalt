package com.github.auties00.cobalt.props;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.linked.props.ABProp;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * In-memory {@link ABPropsService} double for tests. Holds a mutable {@code ABProp -> value} map
 * so a test can pin specific feature flags; unset props fall back to {@link ABProp#defaultValue()}.
 * The sync, sampling-persistence, and group-props branches are stubbed so tests need not stand up a
 * live relay; only the value accessors carry real behavior.
 */
public final class TestABPropsService implements ABPropsService {
    private final Map<ABProp, String> values;

    private final Map<Integer, Integer> samplingConfigs;

    private final Set<Integer> accessedConfigs;

    private String abKey;

    private String hash;

    private Long refreshSeconds;

    private Instant lastSyncTime;

    private long refreshId;

    private long webRefreshId;

    private long groupRefreshId;

    private Instant groupEmergencyPushTimestamp;

    public TestABPropsService(Map<ABProp, String> values) {
        this.values = new HashMap<>(Objects.requireNonNull(values, "values"));
        this.samplingConfigs = new HashMap<>();
        this.accessedConfigs = new HashSet<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public TestABPropsService set(ABProp prop, String value) {
        values.put(prop, value);
        return this;
    }

    public TestABPropsService set(ABProp prop, boolean value) {
        values.put(prop, Boolean.toString(value));
        return this;
    }

    public TestABPropsService set(ABProp prop, long value) {
        values.put(prop, Long.toString(value));
        return this;
    }

    @Override
    public boolean sync(Long localRefreshId, boolean shouldSendHash) {
        return true;
    }

    @Override
    public boolean process(Stanza response) {
        return true;
    }

    @Override
    public Optional<String> abKey() {
        return Optional.ofNullable(abKey);
    }

    @Override
    public Optional<String> hash() {
        return Optional.ofNullable(hash);
    }

    @Override
    public long refresh() {
        return refreshSeconds != null ? refreshSeconds : 86_400L;
    }

    @Override
    public Optional<Instant> lastSyncTime() {
        return Optional.ofNullable(lastSyncTime);
    }

    @Override
    public boolean isAfterFirstSync() {
        return lastSyncTime != null || abKey != null || hash != null;
    }

    @Override
    public void updateAttributesLocalStorage(String abKey, String hash, Long refreshSeconds, Instant lastSyncTime) {
        if (abKey != null) this.abKey = abKey;
        if (hash != null) this.hash = hash;
        if (refreshSeconds != null) this.refreshSeconds = refreshSeconds;
        this.lastSyncTime = lastSyncTime;
    }

    @Override
    public long refreshId() {
        return refreshId;
    }

    @Override
    public void setRefreshId(long refreshId) {
        this.refreshId = refreshId;
    }

    @Override
    public long webRefreshId() {
        return webRefreshId;
    }

    @Override
    public void setWebRefreshId(long webRefreshId) {
        this.webRefreshId = webRefreshId;
    }

    @Override
    public long groupAbPropsRefreshId() {
        return groupRefreshId;
    }

    @Override
    public void setGroupAbPropsRefreshId(long groupRefreshId) {
        this.groupRefreshId = groupRefreshId;
    }

    @Override
    public Optional<Instant> groupAbPropsEmergencyPushTimestamp() {
        return Optional.ofNullable(groupEmergencyPushTimestamp);
    }

    @Override
    public void setGroupAbPropsEmergencyPushTimestamp(Instant timestamp) {
        this.groupEmergencyPushTimestamp = timestamp;
    }

    @Override
    public Optional<GroupAbPropsResult> getGroupAbPropsProtocol(Jid groupJid, String propsHash) {
        return Optional.empty();
    }

    @Override
    public Map<Integer, String> abPropConfigs() {
        var snapshot = new HashMap<Integer, String>();
        values.forEach((prop, value) -> snapshot.put(prop.code(), value));
        return Collections.unmodifiableMap(snapshot);
    }

    @Override
    public boolean setConfigAccessed(ABProp prop) {
        Objects.requireNonNull(prop, "prop");
        return accessedConfigs.add(prop.code());
    }

    @Override
    public boolean isConfigAccessed(ABProp prop) {
        Objects.requireNonNull(prop, "prop");
        return accessedConfigs.contains(prop.code());
    }

    @Override
    public String getString(ABProp prop, boolean waitForSync) {
        Objects.requireNonNull(prop, "prop");
        return values.getOrDefault(prop, prop.defaultValue());
    }

    @Override
    public boolean getBool(ABProp prop, boolean waitForSync) {
        return ABProp.toBoolean(getString(prop, waitForSync));
    }

    @Override
    public int getInt(ABProp prop, boolean waitForSync) {
        var raw = ABProp.toInt(getString(prop, waitForSync));
        if (raw.isPresent()) return raw.getAsInt();
        var fallback = ABProp.toInt(prop.defaultValue());
        return fallback.orElse(0);
    }

    @Override
    public long getLong(ABProp prop, boolean waitForSync) {
        var raw = ABProp.toLong(getString(prop, waitForSync));
        if (raw.isPresent()) return raw.getAsLong();
        var fallback = ABProp.toLong(prop.defaultValue());
        return fallback.orElse(0L);
    }

    @Override
    public double getDouble(ABProp prop, boolean waitForSync) {
        var raw = ABProp.toDouble(getString(prop, waitForSync));
        if (raw.isPresent()) return raw.getAsDouble();
        var fallback = ABProp.toDouble(prop.defaultValue());
        return fallback.orElse(0.0);
    }

    @Override
    public OptionalInt getSamplingWeight(int eventCode) {
        var weight = samplingConfigs.get(eventCode);
        return weight != null ? OptionalInt.of(weight) : OptionalInt.empty();
    }

    @Override
    public Map<Integer, Integer> samplingConfigs() {
        return Collections.unmodifiableMap(samplingConfigs);
    }

    @Override
    public boolean updateEventSamplingConfigs(Map<Integer, Integer> configs) {
        if (configs == null || configs.isEmpty()) {
            return false;
        }
        samplingConfigs.clear();
        samplingConfigs.putAll(configs);
        return true;
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public void clear() {
        values.clear();
        samplingConfigs.clear();
        accessedConfigs.clear();
        abKey = null;
        hash = null;
        lastSyncTime = null;
    }

    public static final class Builder {
        private final Map<ABProp, String> values = new HashMap<>();

        public Builder with(ABProp prop, boolean value) {
            values.put(prop, Boolean.toString(value));
            return this;
        }

        public Builder with(ABProp prop, long value) {
            values.put(prop, Long.toString(value));
            return this;
        }

        public Builder with(ABProp prop, String value) {
            values.put(prop, value);
            return this;
        }

        public TestABPropsService build() {
            return new TestABPropsService(values);
        }
    }
}
