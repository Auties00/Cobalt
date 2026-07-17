package com.github.auties00.cobalt.wire.linked.reporting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Describes the reporting rules that apply to a single protobuf field of a message that may
 * be attached to an abuse report.
 *
 * <p>Each reporting field entry states the protocol version range in which the field is
 * eligible to be reported, whether the field is itself a nested message, and, when it is a
 * message, the rules that apply to each of its own fields.
 *
 * <p>The evaluation semantics are:
 * <ul>
 *   <li>The current protocol version must be greater than or equal to {@link #minVersion()}
 *       when set, and less than or equal to {@link #maxVersion()} when set.</li>
 *   <li>If {@link #notReportableMinVersion()} is set, versions at or above that threshold
 *       are excluded from reporting.</li>
 *   <li>If {@link #isMessage()} returns {@code true}, the field is a nested message and the
 *       per subfield rules in {@link #subfield()} are consulted recursively.</li>
 * </ul>
 *
 * <p>This structure is the building block used inside {@link ReportingConfig}, which maps
 * every top level protobuf field index to an instance of this class.
 */
@ProtobufMessage(name = "Field")
public final class ReportingField {
    /**
     * Inclusive lower bound of the protocol version range in which this field is reportable.
     *
     * <p>When absent, no lower bound is applied.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    Integer minVersion;

    /**
     * Inclusive upper bound of the protocol version range in which this field is reportable.
     *
     * <p>When absent, no upper bound is applied.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer maxVersion;

    /**
     * Minimum version at which this field becomes non reportable, overriding the version range.
     *
     * <p>Used to deprecate reporting of a field from a certain version onwards without
     * altering the base eligibility range.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    Integer notReportableMinVersion;

    /**
     * Flag indicating whether this field holds a nested protobuf message.
     *
     * <p>When {@code true}, the field is itself a composite value and the per subfield rules
     * in {@link #subfield} must be consulted to decide which inner fields may be reported.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean isMessage;

    /**
     * Map of nested protobuf field indexes to their reporting rules.
     *
     * <p>Only meaningful when {@link #isMessage} is {@code true}. Each key is the numeric
     * index of a field inside the nested message, and each value describes whether that
     * inner field may be attached to the report.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MAP, mapKeyType = ProtobufType.UINT32, mapValueType = ProtobufType.MESSAGE)
    Map<Integer, ReportingField> subfield;


    /**
     * Constructs a new reporting field rule with the given version bounds, message flag and
     * nested subfield rules.
     *
     * @param minVersion              the inclusive minimum reportable version, may be {@code null}
     * @param maxVersion              the inclusive maximum reportable version, may be {@code null}
     * @param notReportableMinVersion the version at or above which reporting is disabled, may be {@code null}
     * @param isMessage               {@code true} when the field is a nested message, may be {@code null}
     * @param subfield                the map of nested field indexes to their rules, may be {@code null}
     */
    ReportingField(Integer minVersion, Integer maxVersion, Integer notReportableMinVersion, Boolean isMessage, Map<Integer, ReportingField> subfield) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.notReportableMinVersion = notReportableMinVersion;
        this.isMessage = isMessage;
        this.subfield = subfield;
    }

    /**
     * Returns the inclusive minimum protocol version at which this field may be reported.
     *
     * @return an {@link OptionalInt} containing the minimum version, or empty if no lower bound applies
     */
    public OptionalInt minVersion() {
        return minVersion == null ? OptionalInt.empty() : OptionalInt.of(minVersion);
    }

    /**
     * Returns the inclusive maximum protocol version at which this field may be reported.
     *
     * @return an {@link OptionalInt} containing the maximum version, or empty if no upper bound applies
     */
    public OptionalInt maxVersion() {
        return maxVersion == null ? OptionalInt.empty() : OptionalInt.of(maxVersion);
    }

    /**
     * Returns the protocol version at or above which this field is no longer reportable.
     *
     * @return an {@link OptionalInt} containing the cutoff version, or empty if no cutoff applies
     */
    public OptionalInt notReportableMinVersion() {
        return notReportableMinVersion == null ? OptionalInt.empty() : OptionalInt.of(notReportableMinVersion);
    }

    /**
     * Returns whether this field carries a nested protobuf message.
     *
     * <p>When {@code true}, callers should consult {@link #subfield()} to apply the
     * per subfield rules recursively.
     *
     * @return {@code true} if the field is a nested message, {@code false} otherwise
     */
    public boolean isMessage() {
        return isMessage != null && isMessage;
    }

    /**
     * Returns the reporting rules that apply to each nested protobuf field index.
     *
     * <p>The returned map is unmodifiable. If no nested rules are configured, an empty map
     * is returned rather than {@code null}.
     *
     * @return an unmodifiable map from nested field index to {@link ReportingField}
     */
    public Map<Integer, ReportingField> subfield() {
        return subfield == null ? Map.of() : Collections.unmodifiableMap(subfield);
    }

    /**
     * Sets the inclusive minimum reportable version.
     *
     * @param minVersion the new minimum version, or {@code null} to remove the lower bound
     */
    public void setMinVersion(Integer minVersion) {
        this.minVersion = minVersion;
    }

    /**
     * Sets the inclusive maximum reportable version.
     *
     * @param maxVersion the new maximum version, or {@code null} to remove the upper bound
     */
    public void setMaxVersion(Integer maxVersion) {
        this.maxVersion = maxVersion;
    }

    /**
     * Sets the version at or above which reporting becomes disabled.
     *
     * @param notReportableMinVersion the new cutoff version, or {@code null} to remove the cutoff
     */
    public void setNotReportableMinVersion(Integer notReportableMinVersion) {
        this.notReportableMinVersion = notReportableMinVersion;
    }

    /**
     * Sets the flag that marks this field as a nested protobuf message.
     *
     * @param isMessage {@code true} if the field is a nested message, or {@code null} to clear it
     */
    public void setMessage(Boolean isMessage) {
        this.isMessage = isMessage;
    }

    /**
     * Replaces the map of nested subfield reporting rules.
     *
     * @param subfield the new map of nested field indexes to their rules, may be {@code null}
     */
    public void setSubfield(Map<Integer, ReportingField> subfield) {
        this.subfield = subfield;
    }
}
