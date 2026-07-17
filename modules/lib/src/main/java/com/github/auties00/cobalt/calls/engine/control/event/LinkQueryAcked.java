package com.github.auties00.cobalt.calls.engine.control.event;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.wire.linked.call.CallLink;

import java.util.Objects;

/**
 * A {@link ControlCallEvent} delivering the resolved metadata of an acknowledged call link query.
 *
 * <p>When the relay answers a call link query, the engine emits this event carrying the resolved
 * {@link #link() call link}: its token and media kind, the creator identity hints, whether it is bound to
 * a scheduled event, and the nested waiting room state. The host uses it to present a link preview before
 * joining or to populate an edit form.
 *
 * @param link the resolved call link metadata decoded from the query acknowledgement; never {@code null}
 */
public record LinkQueryAcked(CallLink link) implements ControlCallEvent {
    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code link} is {@code null}
     */
    public LinkQueryAcked {
        Objects.requireNonNull(link, "link cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CallEventType#LINK_QUERY_ACKED}
     */
    @Override
    public CallEventType type() {
        return CallEventType.LINK_QUERY_ACKED;
    }
}
