package com.github.auties00.cobalt.wire.linked.reporting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalInt;

/**
 * Describes the version constraints that decide whether a given message or field is eligible
 * to be included in an abuse report to WhatsApp.
 *
 * <p>A constraint expresses a contiguous range of protocol versions in which reporting is
 * allowed, together with optional blacklist information. It is typically attached to a
 * message type or to a specific field inside a message so that the client can decide, based
 * on the version of the protocol that produced the content, whether it may be forwarded to
 * the moderation backend.
 *
 * <p>The rules are evaluated as follows:
 * <ul>
 *   <li>If {@link #never()} returns {@code true}, the entity is never reportable.</li>
 *   <li>Otherwise the current version must be greater than or equal to {@link #minVersion()}
 *       when set, and less than or equal to {@link #maxVersion()} when set.</li>
 *   <li>If {@link #notReportableMinVersion()} is set, versions at or above that threshold
 *       are excluded from reporting even when they satisfy the other bounds.</li>
 * </ul>
 */
@ProtobufMessage(name = "Reportable")
public final class ReportingConstraint {
    /**
     * Inclusive lower bound of the protocol version range in which the entity is reportable.
     *
     * <p>When absent, no lower bound is applied.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    Integer minVersion;

    /**
     * Inclusive upper bound of the protocol version range in which the entity is reportable.
     *
     * <p>When absent, no upper bound is applied.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer maxVersion;

    /**
     * Minimum version at which the entity becomes non reportable, overriding the
     * {@link #minVersion} / {@link #maxVersion} range.
     *
     * <p>Used to deprecate reporting of an entity from a certain version onwards without
     * altering the base eligibility range.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    Integer notReportableMinVersion;

    /**
     * Hard flag that disables reporting regardless of any version based rule.
     *
     * <p>When {@code true}, the entity must never be attached to an abuse report.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean never;


    /**
     * Constructs a new reporting constraint with the given version bounds and override flag.
     *
     * @param minVersion              the inclusive minimum reportable version, may be {@code null}
     * @param maxVersion              the inclusive maximum reportable version, may be {@code null}
     * @param notReportableMinVersion the version at or above which reporting is disabled, may be {@code null}
     * @param never                   {@code true} to disable reporting unconditionally, may be {@code null}
     */
    ReportingConstraint(Integer minVersion, Integer maxVersion, Integer notReportableMinVersion, Boolean never) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.notReportableMinVersion = notReportableMinVersion;
        this.never = never;
    }

    /**
     * Returns the inclusive minimum protocol version at which this entity may be reported.
     *
     * @return an {@link OptionalInt} containing the minimum version, or empty if no lower bound applies
     */
    public OptionalInt minVersion() {
        return minVersion == null ? OptionalInt.empty() : OptionalInt.of(minVersion);
    }

    /**
     * Returns the inclusive maximum protocol version at which this entity may be reported.
     *
     * @return an {@link OptionalInt} containing the maximum version, or empty if no upper bound applies
     */
    public OptionalInt maxVersion() {
        return maxVersion == null ? OptionalInt.empty() : OptionalInt.of(maxVersion);
    }

    /**
     * Returns the protocol version at or above which the entity is no longer reportable.
     *
     * @return an {@link OptionalInt} containing the cutoff version, or empty if no cutoff applies
     */
    public OptionalInt notReportableMinVersion() {
        return notReportableMinVersion == null ? OptionalInt.empty() : OptionalInt.of(notReportableMinVersion);
    }

    /**
     * Returns whether this entity is flagged as never reportable.
     *
     * <p>When {@code true}, the entity must be excluded from every abuse report regardless of
     * the version bounds declared by the other fields.
     *
     * @return {@code true} if the entity must never be reported, {@code false} otherwise
     */
    public boolean never() {
        return never != null && never;
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
     * Sets the unconditional never reportable flag.
     *
     * @param never {@code true} to disable reporting unconditionally, {@code false} or {@code null} to rely on version bounds
     */
    public void setNever(Boolean never) {
        this.never = never;
    }
}
