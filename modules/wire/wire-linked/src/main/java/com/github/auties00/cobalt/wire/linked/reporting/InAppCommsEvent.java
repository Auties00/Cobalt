package com.github.auties00.cobalt.wire.linked.reporting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.reportInAppCommsEvent} — emits
 * a single in-app marketing telemetry event (impression, click,
 * dismissal) to the relay.
 *
 * <p>All fields are optional: the relay accepts and silently coalesces
 * partial events to be aggregated server-side. {@link #eventTimestampSec}
 * is a primitive {@code long} defaulting to {@code 0} when unset.
 */
@ProtobufMessage
public final class InAppCommsEvent {
    /**
     * Identifier of the in-app promotion the event refers to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String eventPromotionId;

    /**
     * Event-type code (e.g. impression / click / dismissal).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String eventType;

    /**
     * Event time in seconds since the Unix epoch.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final long eventTimestampSec;

    /**
     * Opaque JSON payload owned by the promotion-authoring tool.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String eventLogdata;

    /**
     * Constructs a new {@code InAppCommsEvent}.
     *
     * @param eventPromotionId  the optional promotion identifier
     * @param eventType         the optional event-type code
     * @param eventTimestampSec the event time in seconds
     * @param eventLogdata      the optional JSON payload
     */
    InAppCommsEvent(String eventPromotionId, String eventType, long eventTimestampSec, String eventLogdata) {
        this.eventPromotionId = eventPromotionId;
        this.eventType = eventType;
        this.eventTimestampSec = eventTimestampSec;
        this.eventLogdata = eventLogdata;
    }

    /**
     * Returns the promotion identifier.
     *
     * @return an {@link Optional} carrying the id, or empty when unset
     */
    public Optional<String> eventPromotionId() {
        return Optional.ofNullable(eventPromotionId);
    }

    /**
     * Returns the event-type code.
     *
     * @return an {@link Optional} carrying the code, or empty when unset
     */
    public Optional<String> eventType() {
        return Optional.ofNullable(eventType);
    }

    /**
     * Returns the event timestamp.
     *
     * @return the timestamp, in seconds since the Unix epoch
     */
    public long eventTimestampSec() {
        return eventTimestampSec;
    }

    /**
     * Returns the opaque JSON payload.
     *
     * @return an {@link Optional} carrying the payload, or empty when unset
     */
    public Optional<String> eventLogdata() {
        return Optional.ofNullable(eventLogdata);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (InAppCommsEvent) obj;
        return Objects.equals(eventPromotionId, that.eventPromotionId) &&
                Objects.equals(eventType, that.eventType) &&
                eventTimestampSec == that.eventTimestampSec &&
                Objects.equals(eventLogdata, that.eventLogdata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventPromotionId, eventType, eventTimestampSec, eventLogdata);
    }

    @Override
    public String toString() {
        return "InAppCommsEvent[" +
                "eventPromotionId=" + eventPromotionId + ", " +
                "eventType=" + eventType + ", " +
                "eventTimestampSec=" + eventTimestampSec + ", " +
                "eventLogdata=" + eventLogdata + ']';
    }
}
