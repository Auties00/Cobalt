/**
 * Defines the Facebook, WhatsApp and WhatsApp Web GraphQL operation models for Cobalt.
 *
 * <p>This module is used by the client library's GraphQL dispatcher clients to marshal the typed request and
 * response bodies of the three GraphQL host families (Facebook comet, WhatsApp, and WhatsApp Web relay/MEX),
 * which travel as JSON. It carries only the schema, referencing the Linked domain model and the stanza models
 * for its payloads and carrying WhatsApp source-provenance annotations; the thin HTTP clients that actually send
 * these operations remain in the client library as transport machinery.
 */
module com.github.auties00.cobalt.wire.graphql {
    // transitive: operation payloads name Linked domain types on their public API
    requires transitive com.github.auties00.cobalt.wire.linked;
    // transitive: MEX operations embed stanza models
    requires transitive com.github.auties00.cobalt.wire.stanza;
    // source-provenance annotations on the operations; SOURCE retention, so static
    requires static com.github.auties00.cobalt.meta;
    // JSON marshalling of the operation bodies
    requires com.alibaba.fastjson2;

    // Facebook comet GraphQL operations
    exports com.github.auties00.cobalt.wire.graphql.facebook.ads;
    exports com.github.auties00.cobalt.wire.graphql.facebook.auth;
    exports com.github.auties00.cobalt.wire.graphql.facebook.business;
    exports com.github.auties00.cobalt.wire.graphql.facebook.group;
    exports com.github.auties00.cobalt.wire.graphql.facebook.misc;
    exports com.github.auties00.cobalt.wire.graphql.facebook.promotion;
    exports com.github.auties00.cobalt.wire.graphql.facebook;
    // WhatsApp GraphQL operations
    exports com.github.auties00.cobalt.wire.graphql.whatsapp.ads;
    exports com.github.auties00.cobalt.wire.graphql.whatsapp.auth;
    exports com.github.auties00.cobalt.wire.graphql.whatsapp.business;
    exports com.github.auties00.cobalt.wire.graphql.whatsapp.misc;
    exports com.github.auties00.cobalt.wire.graphql.whatsapp;
    // WhatsApp Web relay/MEX GraphQL operations
    exports com.github.auties00.cobalt.wire.graphql.whatsappWeb.acs;
    exports com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;
    exports com.github.auties00.cobalt.wire.graphql.whatsappWeb.business;
    exports com.github.auties00.cobalt.wire.graphql.whatsappWeb.group;
    exports com.github.auties00.cobalt.wire.graphql.whatsappWeb.misc;
    exports com.github.auties00.cobalt.wire.graphql.whatsappWeb.promotion;
    exports com.github.auties00.cobalt.wire.graphql.whatsappWeb.user;
    exports com.github.auties00.cobalt.wire.graphql.whatsappWeb;
}
