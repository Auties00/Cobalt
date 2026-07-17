package com.github.auties00.cobalt.calls.transport;

/**
 * Enumerates the transport layer events that drive the call transport state machine.
 *
 * <p>The media transport reports its progress to the {@link CallTransportController} as a sequence of
 * these events: the relay being allocated, downlink and uplink media traffic starting and stopping,
 * the relay bind phase failing when no relay answers, and inbound application data arriving. The
 * controller advances its bring up sequence and emits the matching engine call events in response.
 */
public enum TransportEvent {
    /**
     * Signals that the relay was allocated and its remote fingerprint set.
     *
     * <p>This is the success outcome of relay election; the controller proceeds to set up the data
     * channel.
     */
    RELAY_CREATE_SUCCESS,

    /**
     * Signals that no relay answered the bind requests.
     *
     * <p>The controller surfaces this as a transport failure to the application; the call cannot reach
     * a relay.
     */
    RELAY_BINDS_FAILED,

    /**
     * Signals that downlink media and stream started for the winning candidate pair or relay index.
     *
     * <p>The controller begins the receive media pipeline and the application data side channel.
     */
    RX_TRAFFIC_STARTED,

    /**
     * Signals that downlink media stopped.
     */
    RX_TRAFFIC_STOPPED,

    /**
     * Signals that uplink media to the chosen pair or relay started.
     *
     * <p>The controller begins the send media pipeline; the transport is now bidirectional.
     */
    TX_TRAFFIC_START,

    /**
     * Signals that uplink media stopped.
     */
    TX_TRAFFIC_STOPPED,

    /**
     * Signals that inbound application data arrived from the peer or SFU.
     *
     * <p>The controller hands the bytes to the application data controller, which demultiplexes them
     * into reaction, rekey, subscription, and feedback handlers.
     */
    RX_APP_DATA
}
