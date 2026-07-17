/**
 * Defines the hand-written protobuf domain model for Cobalt's Linked (WhatsApp Web, Desktop and Mobile) transport.
 *
 * <p>This module is used by the client library as the payload of nearly every Linked operation: messages and
 * their content variants, chats, contacts, newsletters and calls, the WhatsApp Business and commerce models,
 * user settings, preferences and privacy, the app-state (syncd) replication models, and the low-level Signal,
 * pairing and device-fanout wire types. Every type is a {@code @ProtobufMessage}, {@code @ProtobufEnum} or
 * {@code @ProtobufMixin}, so the module carries no client, socket or transport logic; the dependency runs one
 * way, from the client library into here, and a type here can never reference a client-library type.
 */
module com.github.auties00.cobalt.wire.linked {
    // transitive: @ProtobufMessage accessors expose protobuf-base value types
    requires transitive it.auties.protobuf.base;
    // immutable collection helpers backing the domain models
    requires com.github.auties00.collections;
    // transitive: signal key material types appear on public accessors
    requires transitive com.github.auties00.libsignal;
    // transitive: the JID family and shared mixins are exposed across this module's public API
    requires transitive com.github.auties00.cobalt.wire.core;
    // transitive: SizedInputStream is exposed by GroupPicture, NewsletterCreate and NewsletterMetadataEdit accessors
    requires transitive com.github.auties00.cobalt.stanza;
    requires com.alibaba.fastjson2;
    requires com.googlecode.ezvcard;

    // Messaging: the LinkedMessageContainer envelope and its content variants
    exports com.github.auties00.cobalt.wire.linked.message;
    exports com.github.auties00.cobalt.wire.linked.message.text;
    exports com.github.auties00.cobalt.wire.linked.message.media;
    exports com.github.auties00.cobalt.wire.linked.message.interactive;
    exports com.github.auties00.cobalt.wire.linked.message.context;
    exports com.github.auties00.cobalt.wire.linked.message.addon;
    exports com.github.auties00.cobalt.wire.linked.message.call;
    exports com.github.auties00.cobalt.wire.linked.message.commerce;
    exports com.github.auties00.cobalt.wire.linked.message.contact;
    exports com.github.auties00.cobalt.wire.linked.message.event;
    exports com.github.auties00.cobalt.wire.linked.message.group;
    exports com.github.auties00.cobalt.wire.linked.message.list;
    exports com.github.auties00.cobalt.wire.linked.message.location;
    exports com.github.auties00.cobalt.wire.linked.message.newsletter;
    exports com.github.auties00.cobalt.wire.linked.message.payment;
    exports com.github.auties00.cobalt.wire.linked.message.poll;
    exports com.github.auties00.cobalt.wire.linked.message.status;
    exports com.github.auties00.cobalt.wire.linked.message.bot;

    // Conversations: chats, groups, contacts, newsletters and calls
    exports com.github.auties00.cobalt.wire.linked.chat;
    exports com.github.auties00.cobalt.wire.linked.chat.group;
    exports com.github.auties00.cobalt.wire.linked.contact;
    exports com.github.auties00.cobalt.wire.linked.newsletter;
    exports com.github.auties00.cobalt.wire.linked.call;

    // Media transfer descriptors and geographic points
    exports com.github.auties00.cobalt.wire.linked.media;
    exports com.github.auties00.cobalt.wire.linked.location;

    // AI bots: profile queries plus the BotMetadata content tree
    exports com.github.auties00.cobalt.wire.linked.bot;
    exports com.github.auties00.cobalt.wire.linked.bot.ai;
    exports com.github.auties00.cobalt.wire.linked.bot.feedback;
    exports com.github.auties00.cobalt.wire.linked.bot.metrics;
    exports com.github.auties00.cobalt.wire.linked.bot.plugin;
    exports com.github.auties00.cobalt.wire.linked.bot.profile;
    exports com.github.auties00.cobalt.wire.linked.bot.rendering;
    exports com.github.auties00.cobalt.wire.linked.bot.response;
    exports com.github.auties00.cobalt.wire.linked.bot.session;

    // WhatsApp Business and commerce queried through the client
    exports com.github.auties00.cobalt.wire.linked.business;
    exports com.github.auties00.cobalt.wire.linked.business.profile;
    exports com.github.auties00.cobalt.wire.linked.business.aichannel;
    exports com.github.auties00.cobalt.wire.linked.business.acs;
    exports com.github.auties00.cobalt.wire.linked.business.cart;
    exports com.github.auties00.cobalt.wire.linked.business.order;
    exports com.github.auties00.cobalt.wire.linked.business.ctwa;
    exports com.github.auties00.cobalt.wire.linked.business.promotion;
    exports com.github.auties00.cobalt.wire.linked.business.subscription;
    exports com.github.auties00.cobalt.wire.linked.business.compliance;
    exports com.github.auties00.cobalt.wire.linked.business.crossposting;
    exports com.github.auties00.cobalt.wire.linked.business.linking;
    exports com.github.auties00.cobalt.wire.linked.business.postcode;
    exports com.github.auties00.cobalt.wire.linked.business.flow;
    exports com.github.auties00.cobalt.wire.linked.business.webgraphql;


    // Account device: capabilities, ADV identity, device-list info, pairing config and device-sync keys
    exports com.github.auties00.cobalt.wire.linked.device.capabilities;
    exports com.github.auties00.cobalt.wire.linked.device.identity;
    exports com.github.auties00.cobalt.wire.linked.device.info;
    exports com.github.auties00.cobalt.wire.linked.device.pairing;
    exports com.github.auties00.cobalt.wire.linked.device.sync;

    // Settings, preferences and privacy
    exports com.github.auties00.cobalt.wire.linked.setting;
    exports com.github.auties00.cobalt.wire.linked.setting.notice;
    exports com.github.auties00.cobalt.wire.linked.setting.privacy;
    exports com.github.auties00.cobalt.wire.linked.setting.push;
    exports com.github.auties00.cobalt.wire.linked.preference;
    exports com.github.auties00.cobalt.wire.linked.privacy;
    exports com.github.auties00.cobalt.wire.linked.props;
    exports com.github.auties00.cobalt.wire.linked.tos;

    // Server-pushed account integrity challenge payload (listener-facing)
    exports com.github.auties00.cobalt.wire.linked.integrity;

    // Payments
    exports com.github.auties00.cobalt.wire.linked.payment;

    // App-state (syncd): the SyncAction hierarchy, action values and orphan mutations
    exports com.github.auties00.cobalt.wire.linked.sync;
    exports com.github.auties00.cobalt.wire.linked.sync.action;
    exports com.github.auties00.cobalt.wire.linked.sync.action.bot;
    exports com.github.auties00.cobalt.wire.linked.sync.action.chat;
    exports com.github.auties00.cobalt.wire.linked.sync.action.device;
    exports com.github.auties00.cobalt.wire.linked.sync.action.media;
    exports com.github.auties00.cobalt.wire.linked.sync.action.payment;
    exports com.github.auties00.cobalt.wire.linked.sync.action.privacy;
    exports com.github.auties00.cobalt.wire.linked.sync.action.setting;
    exports com.github.auties00.cobalt.wire.linked.sync.mutation;

    // System/control message wrappers exposed to the client (app-state keys, peer device control)
    exports com.github.auties00.cobalt.wire.linked.message.system.appstate;
    exports com.github.auties00.cobalt.wire.linked.message.system.peer;

    // Error and diagnostic models carried on the public exceptions
    exports com.github.auties00.cobalt.wire.linked.error;

    // Signal/Noise protocol key material (the client library uses libsignal types at runtime)
    exports com.github.auties00.cobalt.wire.linked.signal;

    // Device metadata, props and platform enum (internal fanout bookkeeping)
    exports com.github.auties00.cobalt.wire.linked.device;

    // Low-level syncd wire types plus the linked-device-only sync actions
    exports com.github.auties00.cobalt.wire.linked.sync.data;
    exports com.github.auties00.cobalt.wire.linked.sync.history;
    exports com.github.auties00.cobalt.wire.linked.sync.action.business;
    exports com.github.auties00.cobalt.wire.linked.sync.action.call;
    exports com.github.auties00.cobalt.wire.linked.sync.action.contact;

    // Protocol, history-sync and encrypted system message wrappers
    exports com.github.auties00.cobalt.wire.linked.message.system;
    exports com.github.auties00.cobalt.wire.linked.message.system.history;
    exports com.github.auties00.cobalt.wire.linked.message.security;

    // PN-to-LID chat migration wire models
    exports com.github.auties00.cobalt.wire.linked.jid.migration;

    // Community metadata resolved by internal stream handlers
    exports com.github.auties00.cobalt.wire.linked.chat.community;

    // Low-level VOIP RTC data-channel wire types
    exports com.github.auties00.cobalt.wire.linked.call.datachannel;

    // Business back-office models reached only through internal GraphQL operations
    exports com.github.auties00.cobalt.wire.linked.business.ads;
    exports com.github.auties00.cobalt.wire.linked.business.ai;
    exports com.github.auties00.cobalt.wire.linked.business.auth;
    exports com.github.auties00.cobalt.wire.linked.business.catalog;
    exports com.github.auties00.cobalt.wire.linked.business.marketing;
    exports com.github.auties00.cobalt.wire.linked.business.support;
    exports com.github.auties00.cobalt.wire.linked.business.waa;

    // Abuse-report token wire schema
    exports com.github.auties00.cobalt.wire.linked.reporting;

    // Federated-identity (Waffle) relay wire models
    exports com.github.auties00.cobalt.wire.linked.federated;
}
