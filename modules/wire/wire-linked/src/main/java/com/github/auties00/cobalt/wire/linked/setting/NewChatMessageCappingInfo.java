package com.github.auties00.cobalt.wire.linked.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Server-issued quota verdict that meters how many brand-new chat
 * threads the local user can open per billing cycle.
 *
 * <p>WhatsApp meters new-chat initiation per account: each cycle has
 * a {@link #totalQuota()} of new-chat threads the user is allowed to
 * open, and the server tracks {@link #usedQuota()} consumed so far.
 * The cycle window is delimited by {@link #cycleStartTimestamp()} and
 * {@link #cycleEndTimestamp()} (inclusive Unix-time epoch seconds).
 *
 * <p>Three opaque status flags drive the per-message warnings and
 * throttling UI on the official clients:
 * <ul>
 *   <li>{@link #oteStatus()} — the over-the-edge enforcement status
 *       tag;</li>
 *   <li>{@link #mvStatus()} — the metered-volume status tag;</li>
 *   <li>{@link #cappingStatus()} — the capping enforcement tag.</li>
 * </ul>
 *
 * <p>{@link #serverSentTimestamp()} carries the relay's own send time
 * so the client can detect drift between its clock and the server's
 * billing clock.
 *
 * <p>Applications that want to gate their own send path on the
 * capping verdict should consult these fields when starting brand-new
 * chats.
 */
@ProtobufMessage
public final class NewChatMessageCappingInfo {
    /**
     * The total new-chat thread quota for the current cycle, as a
     * decimal string. {@code null} when the relay omitted the field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String totalQuota;

    /**
     * The new-chat thread quota already consumed in the current cycle,
     * as a decimal string. {@code null} when the relay omitted the
     * field.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String usedQuota;

    /**
     * The Unix-time epoch second at which the current cycle started.
     * {@code null} when the relay omitted the field.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String cycleStartTimestamp;

    /**
     * The Unix-time epoch second at which the current cycle ends.
     * {@code null} when the relay omitted the field.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String cycleEndTimestamp;

    /**
     * The Unix-time epoch second at which the relay emitted this
     * payload. {@code null} when the relay omitted the field.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String serverSentTimestamp;

    /**
     * The over-the-edge enforcement status tag. {@code null} when the
     * relay omitted the field.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String oteStatus;

    /**
     * The metered-volume status tag. {@code null} when the relay
     * omitted the field.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String mvStatus;

    /**
     * The capping enforcement tag. {@code null} when the relay
     * omitted the field.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String cappingStatus;

    /**
     * Constructs a new {@code NewChatMessageCappingInfo} verdict
     * carrying the relay-issued quota counters and status flags.
     *
     * @param totalQuota          the total cycle quota
     * @param usedQuota           the consumed cycle quota
     * @param cycleStartTimestamp the cycle start timestamp
     * @param cycleEndTimestamp   the cycle end timestamp
     * @param serverSentTimestamp the relay-side send timestamp
     * @param oteStatus           the over-the-edge enforcement tag
     * @param mvStatus            the metered-volume tag
     * @param cappingStatus       the capping enforcement tag
     */
    NewChatMessageCappingInfo(String totalQuota, String usedQuota, String cycleStartTimestamp,
                              String cycleEndTimestamp, String serverSentTimestamp,
                              String oteStatus, String mvStatus, String cappingStatus) {
        this.totalQuota = totalQuota;
        this.usedQuota = usedQuota;
        this.cycleStartTimestamp = cycleStartTimestamp;
        this.cycleEndTimestamp = cycleEndTimestamp;
        this.serverSentTimestamp = serverSentTimestamp;
        this.oteStatus = oteStatus;
        this.mvStatus = mvStatus;
        this.cappingStatus = cappingStatus;
    }

    /**
     * Returns the total new-chat quota for the current cycle.
     *
     * @return an {@link Optional} carrying the decimal-string quota,
     *         or empty when the relay omitted the field
     */
    public Optional<String> totalQuota() {
        return Optional.ofNullable(totalQuota);
    }

    /**
     * Returns the new-chat quota consumed so far in the current cycle.
     *
     * @return an {@link Optional} carrying the decimal-string quota,
     *         or empty when the relay omitted the field
     */
    public Optional<String> usedQuota() {
        return Optional.ofNullable(usedQuota);
    }

    /**
     * Returns the Unix-time epoch second at which the current cycle
     * started.
     *
     * @return an {@link Optional} carrying the timestamp, or empty
     *         when the relay omitted the field
     */
    public Optional<String> cycleStartTimestamp() {
        return Optional.ofNullable(cycleStartTimestamp);
    }

    /**
     * Returns the Unix-time epoch second at which the current cycle
     * ends.
     *
     * @return an {@link Optional} carrying the timestamp, or empty
     *         when the relay omitted the field
     */
    public Optional<String> cycleEndTimestamp() {
        return Optional.ofNullable(cycleEndTimestamp);
    }

    /**
     * Returns the Unix-time epoch second at which the relay emitted
     * this payload.
     *
     * @return an {@link Optional} carrying the timestamp, or empty
     *         when the relay omitted the field
     */
    public Optional<String> serverSentTimestamp() {
        return Optional.ofNullable(serverSentTimestamp);
    }

    /**
     * Returns the over-the-edge enforcement status tag.
     *
     * @return an {@link Optional} carrying the tag, or empty when the
     *         relay omitted the field
     */
    public Optional<String> oteStatus() {
        return Optional.ofNullable(oteStatus);
    }

    /**
     * Returns the metered-volume status tag.
     *
     * @return an {@link Optional} carrying the tag, or empty when the
     *         relay omitted the field
     */
    public Optional<String> mvStatus() {
        return Optional.ofNullable(mvStatus);
    }

    /**
     * Returns the capping enforcement tag.
     *
     * @return an {@link Optional} carrying the tag, or empty when the
     *         relay omitted the field
     */
    public Optional<String> cappingStatus() {
        return Optional.ofNullable(cappingStatus);
    }

    /**
     * Sets the total new-chat quota.
     *
     * @param totalQuota the new value, or {@code null}
     */
    public void setTotalQuota(String totalQuota) {
        this.totalQuota = totalQuota;
    }

    /**
     * Sets the consumed new-chat quota.
     *
     * @param usedQuota the new value, or {@code null}
     */
    public void setUsedQuota(String usedQuota) {
        this.usedQuota = usedQuota;
    }

    /**
     * Sets the cycle start timestamp.
     *
     * @param cycleStartTimestamp the new value, or {@code null}
     */
    public void setCycleStartTimestamp(String cycleStartTimestamp) {
        this.cycleStartTimestamp = cycleStartTimestamp;
    }

    /**
     * Sets the cycle end timestamp.
     *
     * @param cycleEndTimestamp the new value, or {@code null}
     */
    public void setCycleEndTimestamp(String cycleEndTimestamp) {
        this.cycleEndTimestamp = cycleEndTimestamp;
    }

    /**
     * Sets the relay-side send timestamp.
     *
     * @param serverSentTimestamp the new value, or {@code null}
     */
    public void setServerSentTimestamp(String serverSentTimestamp) {
        this.serverSentTimestamp = serverSentTimestamp;
    }

    /**
     * Sets the over-the-edge enforcement tag.
     *
     * @param oteStatus the new value, or {@code null}
     */
    public void setOteStatus(String oteStatus) {
        this.oteStatus = oteStatus;
    }

    /**
     * Sets the metered-volume tag.
     *
     * @param mvStatus the new value, or {@code null}
     */
    public void setMvStatus(String mvStatus) {
        this.mvStatus = mvStatus;
    }

    /**
     * Sets the capping enforcement tag.
     *
     * @param cappingStatus the new value, or {@code null}
     */
    public void setCappingStatus(String cappingStatus) {
        this.cappingStatus = cappingStatus;
    }
}
