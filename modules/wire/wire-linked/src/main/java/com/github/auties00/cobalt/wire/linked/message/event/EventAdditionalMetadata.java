package com.github.auties00.cobalt.wire.linked.message.event;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Holds auxiliary flags attached by the server to a WhatsApp event message.
 *
 * <p>When an {@link EventMessage} is delivered to a client, the server may
 * piggyback additional metadata describing the transport-level state of the
 * event. At present the only such flag is whether the event is considered
 * stale, which clients use to decide whether to render or to ignore a
 * redelivered payload.
 *
 * <p>Additional fields may be introduced in the future without breaking
 * existing consumers, so user code should treat {@code EventAdditionalMetadata}
 * as an extensible container rather than a fixed record.
 */
@ProtobufMessage(name = "EventAdditionalMetadata")
public final class EventAdditionalMetadata {
    /**
     * Raw server-provided stale flag.
     *
     * <p>A value of {@code true} marks the associated event as outdated with
     * respect to newer state the server already has. A value of {@code null}
     * is treated as {@code false} by {@link #isStale()}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isStale;

    /**
     * Constructs a new {@code EventAdditionalMetadata} with the supplied
     * stale flag.
     *
     * <p>The constructor is package-private. Application code should build
     * instances through the generated
     * {@code EventAdditionalMetadataBuilder}.
     *
     * @param isStale the raw stale flag, or {@code null} when the server did
     *                not supply a value
     */
    EventAdditionalMetadata(Boolean isStale) {
        this.isStale = isStale;
    }

    /**
     * Returns whether the associated event has been marked stale by the
     * server.
     *
     * <p>This is a convenience accessor: a missing field is treated as
     * {@code false}, so callers never need to check for {@code null}.
     *
     * @return {@code true} if the event is stale, {@code false} otherwise
     */
    public boolean isStale() {
        return isStale != null && isStale;
    }

    /**
     * Sets the stale flag for the associated event.
     *
     * @param isStale the new stale flag, or {@code null} to clear the field
     */
    public void setStale(Boolean isStale) {
        this.isStale = isStale;
    }
}
