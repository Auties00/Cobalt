package com.github.auties00.cobalt.wire.cloud.flow;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The data-exchange availability of a WhatsApp Cloud API Flow endpoint.
 *
 * <p>A Flow that runs an endpoint reports whether the platform can currently exchange data with it:
 * {@link #AVAILABLE} when the endpoint is healthy, {@link #LIMITED} when it is degraded,
 * {@link #BLOCKED} when the platform has stopped sending to it, and {@link #PAUSED} when exchange is
 * temporarily suspended. The {@link #UNKNOWN} constant guards against tokens this client does not yet
 * model.
 */
@ProtobufEnum
public enum CloudFlowEndpointAvailability {
    /**
     * An availability that this client does not recognise. Resolved for any token outside the modelled
     * set so that an unexpected value never fails decoding.
     */
    UNKNOWN(0),

    /**
     * The endpoint is healthy and the platform can exchange data with it.
     */
    AVAILABLE(1),

    /**
     * The endpoint is degraded and data exchange is limited.
     */
    LIMITED(2),

    /**
     * The platform has stopped exchanging data with the endpoint.
     */
    BLOCKED(3),

    /**
     * Data exchange with the endpoint is temporarily suspended.
     */
    PAUSED(4);

    /**
     * The protobuf-assigned numeric index for this availability.
     */
    final int index;

    /**
     * Constructs a {@code CloudFlowEndpointAvailability} with the specified protobuf index.
     *
     * @param index the protobuf enum index
     */
    CloudFlowEndpointAvailability(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the {@code CloudFlowEndpointAvailability} matching the given wire token.
     *
     * <p>The lookup matches the constant name case-insensitively against {@code input}; any
     * unrecognised or {@code null} token resolves to {@link #UNKNOWN} so that decoding never fails on
     * an unexpected value.
     *
     * @param input the wire token, for example {@code "AVAILABLE"}, or {@code null}
     * @return the matching availability, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudFlowEndpointAvailability of(String input) {
        if (input == null) {
            return UNKNOWN;
        }
        for (var value : values()) {
            if (value != UNKNOWN && value.name().equalsIgnoreCase(input)) {
                return value;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns the WhatsApp wire token for this availability.
     *
     * @return the wire token, the constant name verbatim
     */
    public String token() {
        return name();
    }

    /**
     * Returns the protobuf-assigned numeric index for this availability.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
