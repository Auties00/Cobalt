package com.github.auties00.cobalt.wire.linked.setting.notice;

import java.util.Objects;
import java.util.Optional;

/**
 * Carries one user-notice stage entry returned by the relay's
 * {@code get_disclosure_stage_by_ids} fetch.
 *
 * <p>This RPC verifies the local cache against the server's view of
 * which Terms-of-Service / privacy notices the user has acknowledged
 * or dismissed. Unlike a full {@link UserNotice} disclosure, the
 * version and type attributes are optional: the relay only commits
 * to surfacing the {@code (id, t, stage)} triple.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class UserNoticeStage {
    /**
     * The relay-side timestamp. Seconds since UNIX epoch.
     */
    private final long timestampSeconds;

    /**
     * The optional notice version (≥ 1).
     */
    private final Integer version;

    /**
     * The optional notice type (≥ 0).
     */
    private final Integer type;

    /**
     * The disclosure id.
     */
    private final long noticeId;

    /**
     * The current presentation stage ({@code 0..1000}).
     */
    private final int stage;

    /**
     * Constructs a new stage entry.
     *
     * @param timestampSeconds the relay-side timestamp
     * @param version          the optional notice version; may be
     *                         {@code null}
     * @param type             the optional notice type; may be
     *                         {@code null}
     * @param noticeId         the disclosure id
     * @param stage            the current stage
     */
    public UserNoticeStage(long timestampSeconds, Integer version, Integer type,
                           long noticeId, int stage) {
        this.timestampSeconds = timestampSeconds;
        this.version = version;
        this.type = type;
        this.noticeId = noticeId;
        this.stage = stage;
    }

    /**
     * Returns the relay-side timestamp.
     *
     * @return the timestamp in seconds
     */
    public long timestampSeconds() {
        return timestampSeconds;
    }

    /**
     * Returns the optional notice version.
     *
     * @return an {@link Optional} carrying the version
     */
    public Optional<Integer> version() {
        return Optional.ofNullable(version);
    }

    /**
     * Returns the optional notice type.
     *
     * @return an {@link Optional} carrying the type
     */
    public Optional<Integer> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the disclosure id.
     *
     * @return the id
     */
    public long noticeId() {
        return noticeId;
    }

    /**
     * Returns the current presentation stage.
     *
     * @return the stage ({@code 0..1000})
     */
    public int stage() {
        return stage;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (UserNoticeStage) obj;
        return this.timestampSeconds == that.timestampSeconds
                && this.noticeId == that.noticeId
                && this.stage == that.stage
                && Objects.equals(this.version, that.version)
                && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestampSeconds, version, type, noticeId, stage);
    }

    @Override
    public String toString() {
        return "UserNoticeStage[timestampSeconds=" + timestampSeconds
                + ", version=" + version
                + ", type=" + type
                + ", noticeId=" + noticeId
                + ", stage=" + stage + ']';
    }
}
