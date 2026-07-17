package com.github.auties00.cobalt.wire.linked.reporting;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.sendSupportContactForm} —
 * submits a support-ticket contact-form payload to the relay.
 *
 * <p>{@link #from}, {@link #description} and {@link #topic} are
 * required. Every other field is optional context routed through to
 * the {@code SmaxContactFormRequest}.
 */
@ProtobufMessage
public final class SupportContactForm {
    /**
     * JID of the user opening the support ticket.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid from;

    /**
     * Free-form narrative describing the issue.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String description;

    /**
     * Top-level topic identifying the support category.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String topic;

    /**
     * Optional fine-grained topic identifier within {@link #topic}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String topicId;

    /**
     * Optional JSON-encoded debug-information blob attached to the ticket.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String debugInformationJson;

    /**
     * Optional handle of a previously uploaded logs blob.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String uploadedLogsId;

    /**
     * Optional structured-attributes context-flow discriminator used by the
     * support-tooling pipeline to route the ticket.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    final SupportContactFormContextFlow additionalAttributesContextFlow;

    /**
     * Constructs a new {@code SupportContactForm}.
     *
     * @param from                            the reporter JID; required
     * @param description                     the narrative; required
     * @param topic                           the top-level topic; required
     * @param topicId                         the optional fine-grained topic id
     * @param debugInformationJson            the optional debug-info JSON blob
     * @param uploadedLogsId                  the optional uploaded-logs handle
     * @param additionalAttributesContextFlow the optional context-flow tag
     * @throws NullPointerException if {@code from}, {@code description} or
     *                              {@code topic} is {@code null}
     */
    SupportContactForm(Jid from, String description, String topic, String topicId,
                       String debugInformationJson, String uploadedLogsId,
                       SupportContactFormContextFlow additionalAttributesContextFlow) {
        this.from = Objects.requireNonNull(from, "from cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.topic = Objects.requireNonNull(topic, "topic cannot be null");
        this.topicId = topicId;
        this.debugInformationJson = debugInformationJson;
        this.uploadedLogsId = uploadedLogsId;
        this.additionalAttributesContextFlow = additionalAttributesContextFlow;
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
     * @return the description, never {@code null}
     */
    public String description() {
        return description;
    }

    /**
     * Returns the top-level topic.
     *
     * @return the topic, never {@code null}
     */
    public String topic() {
        return topic;
    }

    /**
     * Returns the optional fine-grained topic id.
     *
     * @return an {@link Optional} carrying the id, or empty when unset
     */
    public Optional<String> topicId() {
        return Optional.ofNullable(topicId);
    }

    /**
     * Returns the optional debug-info JSON blob.
     *
     * @return an {@link Optional} carrying the blob, or empty when unset
     */
    public Optional<String> debugInformationJson() {
        return Optional.ofNullable(debugInformationJson);
    }

    /**
     * Returns the optional uploaded-logs handle.
     *
     * @return an {@link Optional} carrying the handle, or empty when unset
     */
    public Optional<String> uploadedLogsId() {
        return Optional.ofNullable(uploadedLogsId);
    }

    /**
     * Returns the optional context-flow tag.
     *
     * @return an {@link Optional} carrying the tag, or empty when unset
     */
    public Optional<SupportContactFormContextFlow> additionalAttributesContextFlow() {
        return Optional.ofNullable(additionalAttributesContextFlow);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SupportContactForm) obj;
        return Objects.equals(from, that.from) &&
                Objects.equals(description, that.description) &&
                Objects.equals(topic, that.topic) &&
                Objects.equals(topicId, that.topicId) &&
                Objects.equals(debugInformationJson, that.debugInformationJson) &&
                Objects.equals(uploadedLogsId, that.uploadedLogsId) &&
                Objects.equals(additionalAttributesContextFlow, that.additionalAttributesContextFlow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, description, topic, topicId, debugInformationJson, uploadedLogsId,
                additionalAttributesContextFlow);
    }

    @Override
    public String toString() {
        return "SupportContactForm[" +
                "from=" + from + ", " +
                "description=" + description + ", " +
                "topic=" + topic + ", " +
                "topicId=" + topicId + ", " +
                "debugInformationJson=" + debugInformationJson + ", " +
                "uploadedLogsId=" + uploadedLogsId + ", " +
                "additionalAttributesContextFlow=" + additionalAttributesContextFlow + ']';
    }
}
