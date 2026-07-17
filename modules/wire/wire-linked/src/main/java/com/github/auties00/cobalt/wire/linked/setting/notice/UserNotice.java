package com.github.auties00.cobalt.wire.linked.setting.notice;

import java.util.Objects;

/**
 * Carries one user-notice disclosure entry returned by the relay.
 *
 * <p>"User-notice disclosures" are the modal pop-ups WhatsApp shows
 * when the Terms of Service, privacy policy, or a regional
 * compliance notice changes. Each entry surfaces:
 *
 * <ul>
 *     <li>{@link #noticeId()} — the disclosure id, used to record the
 *     dismissal once the user closes the modal;</li>
 *     <li>{@link #version()} — the notice version, incremented when
 *     the wording is rewritten;</li>
 *     <li>{@link #type()} — the relay-side type discriminator;</li>
 *     <li>{@link #stage()} — the current presentation stage
 *     ({@code 0..1000}) of the disclosure progression marker;</li>
 *     <li>{@link #timestampSeconds()} — the relay-issued timestamp.</li>
 * </ul>
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class UserNotice {
    /**
     * The relay-side timestamp. Seconds since UNIX epoch.
     */
    private final long timestampSeconds;

    /**
     * The notice version (≥ 1).
     */
    private final int version;

    /**
     * The notice type discriminator (≥ 0).
     */
    private final int type;

    /**
     * The disclosure id.
     */
    private final long noticeId;

    /**
     * The current presentation stage ({@code 0..1000}).
     */
    private final int stage;

    /**
     * Constructs a new disclosure.
     *
     * @param timestampSeconds the relay-side timestamp
     * @param version          the notice version
     * @param type             the notice type
     * @param noticeId         the disclosure id
     * @param stage            the current stage
     */
    public UserNotice(long timestampSeconds, int version, int type, long noticeId, int stage) {
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
     * Returns the notice version.
     *
     * @return the version (≥ 1)
     */
    public int version() {
        return version;
    }

    /**
     * Returns the notice type discriminator.
     *
     * @return the type (≥ 0)
     */
    public int type() {
        return type;
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
        var that = (UserNotice) obj;
        return this.timestampSeconds == that.timestampSeconds
                && this.version == that.version
                && this.type == that.type
                && this.noticeId == that.noticeId
                && this.stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestampSeconds, version, type, noticeId, stage);
    }

    @Override
    public String toString() {
        return "UserNotice[timestampSeconds=" + timestampSeconds
                + ", version=" + version
                + ", type=" + type
                + ", noticeId=" + noticeId
                + ", stage=" + stage + ']';
    }
}
