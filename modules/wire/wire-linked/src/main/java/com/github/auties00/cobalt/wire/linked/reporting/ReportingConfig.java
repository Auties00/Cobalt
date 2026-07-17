package com.github.auties00.cobalt.wire.linked.reporting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Describes the reporting schema that governs which protobuf fields of a message may be
 * included when the message is reported to WhatsApp for abuse review.
 *
 * <p>When a user reports a message, the client does not send the full plaintext. Instead it
 * consults a reporting configuration that declares, on a field by field basis, which pieces
 * of the message are allowed to be attached to the report. The configuration is versioned so
 * that rules can evolve over time without breaking older clients.
 *
 * <p>Each entry in {@link #field()} is keyed by the protobuf field index of the message being
 * reported, and the associated {@link ReportingField} describes the version constraints and
 * any nested subfields that are considered reportable.
 */
@ProtobufMessage(name = "Config")
public final class ReportingConfig {
    /**
     * Map of protobuf field indexes to their corresponding reporting rules.
     *
     * <p>Each key is the numeric index of a field on the message being reported, and each
     * value describes whether that field may be attached to the report and, if it is itself
     * a message, how its nested fields should be treated.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MAP, mapKeyType = ProtobufType.UINT32, mapValueType = ProtobufType.MESSAGE)
    Map<Integer, ReportingField> field;

    /**
     * Schema version of this configuration.
     *
     * <p>Used by the client to decide whether the configuration it holds is compatible with
     * a message that was produced by a peer running a different version of the protocol.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer version;


    /**
     * Constructs a new reporting configuration with the given field rules and schema version.
     *
     * @param field   the map of protobuf field indexes to reporting rules, may be {@code null}
     * @param version the schema version, may be {@code null}
     */
    ReportingConfig(Map<Integer, ReportingField> field, Integer version) {
        this.field = field;
        this.version = version;
    }

    /**
     * Returns the map of protobuf field indexes to their reporting rules.
     *
     * <p>The returned map is unmodifiable. If no rules are configured, an empty map is
     * returned rather than {@code null}.
     *
     * @return an unmodifiable map from field index to {@link ReportingField}
     */
    public Map<Integer, ReportingField> field() {
        return field == null ? Map.of() : Collections.unmodifiableMap(field);
    }

    /**
     * Returns the schema version of this configuration.
     *
     * @return an {@link OptionalInt} containing the version, or empty if not set
     */
    public OptionalInt version() {
        return version == null ? OptionalInt.empty() : OptionalInt.of(version);
    }

    /**
     * Replaces the map of field reporting rules.
     *
     * @param field the new map of protobuf field indexes to reporting rules, may be {@code null}
     */
    public void setField(Map<Integer, ReportingField> field) {
        this.field = field;
    }

    /**
     * Sets the schema version of this configuration.
     *
     * @param version the new schema version, may be {@code null} to clear it
     */
    public void setVersion(Integer version) {
        this.version = version;
    }
}
