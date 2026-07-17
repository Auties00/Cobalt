package com.github.auties00.cobalt.wire.linked.reporting;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.reportBug} — submits a
 * bug-report payload to the WASmaxBugReporting pipeline.
 *
 * <p>{@link #from} is the only required field; every other field is
 * optional. The wire request takes {@code null} for any missing
 * attribute and the relay surfaces them as empty placeholders.
 */
@ProtobufMessage
public final class BugReport {
    /**
     * JID of the user submitting the bug report.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid from;

    /**
     * Optional narrative description of the bug.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String description;

    /**
     * Optional JSON-encoded debug-information blob.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String debugInformationJson;

    /**
     * Optional handle of a previously uploaded device-log blob.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String deviceLogHandle;

    /**
     * Optional list of media uploads attached to the report.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final List<BugReportMediaUpload> mediaUploads;

    /**
     * Optional short title summarising the report.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String title;

    /**
     * Optional category code routing the report to a triage queue.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String category;

    /**
     * Optional join key correlating client- and server-side traces.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String clientServerJoinKey;

    /**
     * Optional reproducibility marker (e.g. {@code "always"},
     * {@code "intermittent"}).
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String reproducibility;

    /**
     * Constructs a new {@code BugReport}.
     *
     * @param from                 the reporter JID; required
     * @param description          optional narrative, or {@code null}
     * @param debugInformationJson optional debug JSON, or {@code null}
     * @param deviceLogHandle      optional log handle, or {@code null}
     * @param mediaUploads         optional media attachments
     * @param title                optional title, or {@code null}
     * @param category             optional category, or {@code null}
     * @param clientServerJoinKey  optional join key, or {@code null}
     * @param reproducibility      optional reproducibility, or {@code null}
     * @throws NullPointerException if {@code from} is {@code null}
     */
    BugReport(Jid from, String description, String debugInformationJson, String deviceLogHandle,
              List<BugReportMediaUpload> mediaUploads, String title, String category,
              String clientServerJoinKey, String reproducibility) {
        this.from = Objects.requireNonNull(from, "from cannot be null");
        this.description = description;
        this.debugInformationJson = debugInformationJson;
        this.deviceLogHandle = deviceLogHandle;
        this.mediaUploads = mediaUploads;
        this.title = title;
        this.category = category;
        this.clientServerJoinKey = clientServerJoinKey;
        this.reproducibility = reproducibility;
    }

    /**
     * Returns the reporter JID.
     *
     * @return the JID, never {@code null}
     */
    public Jid from() {
        return from;
    }

    /**
     * Returns the narrative description.
     *
     * @return an {@link Optional} carrying the description, or empty
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the debug-info JSON blob.
     *
     * @return an {@link Optional} carrying the JSON blob, or empty
     */
    public Optional<String> debugInformationJson() {
        return Optional.ofNullable(debugInformationJson);
    }

    /**
     * Returns the device-log handle.
     *
     * @return an {@link Optional} carrying the handle, or empty
     */
    public Optional<String> deviceLogHandle() {
        return Optional.ofNullable(deviceLogHandle);
    }

    /**
     * Returns the media uploads.
     *
     * @return the uploads; never {@code null}
     */
    public List<BugReportMediaUpload> mediaUploads() {
        return mediaUploads == null ? List.of() : mediaUploads;
    }

    /**
     * Returns the report title.
     *
     * @return an {@link Optional} carrying the title, or empty
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the category code.
     *
     * @return an {@link Optional} carrying the category, or empty
     */
    public Optional<String> category() {
        return Optional.ofNullable(category);
    }

    /**
     * Returns the join key.
     *
     * @return an {@link Optional} carrying the join key, or empty
     */
    public Optional<String> clientServerJoinKey() {
        return Optional.ofNullable(clientServerJoinKey);
    }

    /**
     * Returns the reproducibility marker.
     *
     * @return an {@link Optional} carrying the marker, or empty
     */
    public Optional<String> reproducibility() {
        return Optional.ofNullable(reproducibility);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BugReport) obj;
        return Objects.equals(from, that.from) &&
                Objects.equals(description, that.description) &&
                Objects.equals(debugInformationJson, that.debugInformationJson) &&
                Objects.equals(deviceLogHandle, that.deviceLogHandle) &&
                Objects.equals(mediaUploads, that.mediaUploads) &&
                Objects.equals(title, that.title) &&
                Objects.equals(category, that.category) &&
                Objects.equals(clientServerJoinKey, that.clientServerJoinKey) &&
                Objects.equals(reproducibility, that.reproducibility);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, description, debugInformationJson, deviceLogHandle, mediaUploads,
                title, category, clientServerJoinKey, reproducibility);
    }

    @Override
    public String toString() {
        return "BugReport[" +
                "from=" + from + ", " +
                "description=" + description + ", " +
                "debugInformationJson=" + debugInformationJson + ", " +
                "deviceLogHandle=" + deviceLogHandle + ", " +
                "mediaUploads=" + mediaUploads + ", " +
                "title=" + title + ", " +
                "category=" + category + ", " +
                "clientServerJoinKey=" + clientServerJoinKey + ", " +
                "reproducibility=" + reproducibility + ']';
    }
}
