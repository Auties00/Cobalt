/**
 * Defines the shared, transport-agnostic wire contracts and leaf value types that both Cobalt transports build on.
 *
 * <p>This module is the common base of the {@code cobalt-wire} reactor: applications never depend on it directly,
 * but every other wire module and the client library do. It carries the {@code Jid} addressing family, the
 * protobuf type-conversion mixins, the shared leaf utilities, and the transport-agnostic message envelope, key,
 * status and content contracts that the Linked and Cloud transports each implement in their own way. It depends
 * only on the protobuf runtime, libsignal, and the leaf telemetry facade ({@code cobalt-telemetry-core}), and
 * references nothing transport-specific, so it sits near the bottom of the dependency graph, above only
 * {@code cobalt-telemetry-core}.
 */
module com.github.auties00.cobalt.wire.core {
    // transitive: Jid.of(ProtobufString) and other public custom (de)serializers expose protobuf-base types
    requires transitive it.auties.protobuf.base;
    // transitive: Jid.toSignalAddress() returns a libsignal SignalProtocolAddress on the public API
    requires transitive com.github.auties00.libsignal;
    // transitive: Jid implements LogRedactableProvider, so the interface is part of Jid's public API
    requires transitive com.github.auties00.cobalt.telemetry;

    // JID addressing family
    exports com.github.auties00.cobalt.wire.core.jid;
    // Shared leaf utilities
    exports com.github.auties00.cobalt.wire.core.util;
    // Protobuf type-conversion mixins
    exports com.github.auties00.cobalt.wire.core.mixin;
    // Transport-agnostic message contracts: envelope, content marker, key/status, content bodies
    exports com.github.auties00.cobalt.wire.core.message;
}
