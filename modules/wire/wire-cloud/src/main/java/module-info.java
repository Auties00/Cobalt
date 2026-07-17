/**
 * Defines the Meta Cloud API JSON request, response and webhook models for Cobalt's Cloud transport.
 *
 * <p>This module is used by the Cloud client to marshal the bodies exchanged over {@code graph.facebook.com},
 * the inbound webhook change payloads, and the Cloud-native message envelope and content bodies. It depends only
 * on {@code cobalt-wire-core} for the shared contracts ({@code Jid}, {@code MessageKey}, {@code MessageStatus},
 * the message envelope and content interfaces, and the protobuf mixins), and deliberately not on the Linked
 * domain: the two transports carry genuinely different data, so the Cloud models implement the shared wire-core
 * contracts rather than reusing the Linked protobuf types. It carries no client, HTTP or webhook-server logic.
 */
module com.github.auties00.cobalt.wire.cloud {
    // transitive: the shared Jid, message envelope and content contracts appear on this module's public API
    requires transitive com.github.auties00.cobalt.wire.core;
    // immutable collection helpers used by the Cloud models
    requires com.github.auties00.collections;

    // Cloud message envelope, content bodies, and the per-edge request/response models
    exports com.github.auties00.cobalt.wire.cloud;
    exports com.github.auties00.cobalt.wire.cloud.content;
    exports com.github.auties00.cobalt.wire.cloud.analytics;
    exports com.github.auties00.cobalt.wire.cloud.commerce;
    exports com.github.auties00.cobalt.wire.cloud.flow;
    exports com.github.auties00.cobalt.wire.cloud.phone;
    exports com.github.auties00.cobalt.wire.cloud.signup;
    exports com.github.auties00.cobalt.wire.cloud.template;
    exports com.github.auties00.cobalt.wire.cloud.template.library;
    exports com.github.auties00.cobalt.wire.cloud.waba;
}
