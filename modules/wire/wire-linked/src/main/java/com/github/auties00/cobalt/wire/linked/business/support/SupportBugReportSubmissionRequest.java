package com.github.auties00.cobalt.wire.linked.business.support;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for the WhatsApp support bug-report submission flow.
 *
 * <p>When a user files an in-app "report a problem" form, the form collects a category for the bug
 * (so support can route the report), a short title, a free-text description of what went wrong, a
 * pre-serialized diagnostic blob, and optional plumbing that ties the report to the reporting session
 * (a device log handle, a client-server join key, and uploaded media attachments). Each field is optional
 * and unset fields are omitted from the request.
 */
@ProtobufMessage(name = "SupportBugReportSubmissionRequest")
public final class SupportBugReportSubmissionRequest {
    /**
     * Server-defined bug category code that routes the report to the right
     * support queue. Recognised values are listed by
     * {@code WAWebBugReportCategoryTypes.CATEGORY_TO_GRAPHQL} in the
     * WhatsApp Web source. Unset omits the field.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String category;

    /**
     * Free-text description of the bug the user is reporting. Carries the
     * user-supplied summary of what went wrong, which support reads to
     * triage the report. Unset omits the field.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String description;

    /**
     * Short title summarising the report. Unset omits the field.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String title;

    /**
     * Pre-serialized diagnostic blob attached to the report, emitted as the
     * {@code debug_info_json} field. It is a serialized string, not a nested
     * object, so it is carried as a plain {@link String}. Unset omits the field.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String debugInfoJson;

    /**
     * Handle of the uploaded device log attached to the report. Unset omits
     * the field.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String deviceLogHandle;

    /**
     * Key tying the report to the reporting client-server session. Unset omits
     * the field.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String clientServerJoinKey;

    /**
     * Media attachments uploaded with the report, in the order they are sent.
     * Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final List<BusinessSupportBugReportMedia> media;

    /**
     * Constructs a new {@code SupportBugReportSubmissionRequest}. Every scalar
     * argument may be {@code null} to omit the corresponding field, and a
     * {@code null} {@code media} is coerced to an empty list.
     *
     * @param category            the server-defined bug category code, or {@code null}
     * @param description         the free-text bug description, or {@code null}
     * @param title               the short report title, or {@code null}
     * @param debugInfoJson       the pre-serialized diagnostic blob, or {@code null}
     * @param deviceLogHandle     the uploaded device-log handle, or {@code null}
     * @param clientServerJoinKey the reporting-session join key, or {@code null}
     * @param media               the uploaded media attachments; {@code null} treated as empty
     */
    public SupportBugReportSubmissionRequest(String category, String description, String title,
                                             String debugInfoJson, String deviceLogHandle,
                                             String clientServerJoinKey, List<BusinessSupportBugReportMedia> media) {
        this.category = category;
        this.description = description;
        this.title = title;
        this.debugInfoJson = debugInfoJson;
        this.deviceLogHandle = deviceLogHandle;
        this.clientServerJoinKey = clientServerJoinKey;
        this.media = media == null ? List.of() : List.copyOf(media);
    }

    /**
     * Returns the bug category code that routes the report.
     *
     * @return an {@link Optional} carrying the category, or empty when unset
     */
    public Optional<String> category() {
        return Optional.ofNullable(category);
    }

    /**
     * Returns the free-text description of the bug.
     *
     * @return an {@link Optional} carrying the description, or empty when unset
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the short title summarising the report.
     *
     * @return an {@link Optional} carrying the title, or empty when unset
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the pre-serialized diagnostic blob attached to the report.
     *
     * @return an {@link Optional} carrying the diagnostic blob, or empty when unset
     */
    public Optional<String> debugInfoJson() {
        return Optional.ofNullable(debugInfoJson);
    }

    /**
     * Returns the handle of the uploaded device log.
     *
     * @return an {@link Optional} carrying the device-log handle, or empty when unset
     */
    public Optional<String> deviceLogHandle() {
        return Optional.ofNullable(deviceLogHandle);
    }

    /**
     * Returns the key tying the report to the reporting session.
     *
     * @return an {@link Optional} carrying the join key, or empty when unset
     */
    public Optional<String> clientServerJoinKey() {
        return Optional.ofNullable(clientServerJoinKey);
    }

    /**
     * Returns the media attachments uploaded with the report.
     *
     * @return an unmodifiable view of the media attachments; never {@code null}, possibly empty
     */
    public List<BusinessSupportBugReportMedia> media() {
        return media;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SupportBugReportSubmissionRequest) obj;
        return Objects.equals(category, that.category)
                && Objects.equals(description, that.description)
                && Objects.equals(title, that.title)
                && Objects.equals(debugInfoJson, that.debugInfoJson)
                && Objects.equals(deviceLogHandle, that.deviceLogHandle)
                && Objects.equals(clientServerJoinKey, that.clientServerJoinKey)
                && Objects.equals(media, that.media);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, description, title, debugInfoJson, deviceLogHandle,
                clientServerJoinKey, media);
    }

    @Override
    public String toString() {
        return "SupportBugReportSubmissionRequest[" +
                "category=" + category + ", " +
                "description=" + description + ", " +
                "title=" + title + ", " +
                "debugInfoJson=" + debugInfoJson + ", " +
                "deviceLogHandle=" + deviceLogHandle + ", " +
                "clientServerJoinKey=" + clientServerJoinKey + ", " +
                "media=" + media + ']';
    }
}
