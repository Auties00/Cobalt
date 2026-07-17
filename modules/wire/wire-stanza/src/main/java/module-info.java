/**
 * Defines the typed IQ/MEX/SMAX/USync stanza operation models for Cobalt.
 *
 * <p>This module is used by the client library's stream handlers to build and parse the typed request and
 * response models of the four binary-XMPP query families (IQ, MEX, SMAX, USync). It carries only the schema;
 * the {@code Stanza} node model and its binary tokenization codec live in {@code cobalt-stanza-core}. Operations
 * reference the Linked domain model for their payloads and carry WhatsApp source-provenance annotations.
 */
module com.github.auties00.cobalt.wire.stanza {
    // transitive: operation payloads name Linked domain types on their public API
    requires transitive com.github.auties00.cobalt.wire.linked;
    // transitive: operations are Stanza node trees built and read through the codec
    requires transitive com.github.auties00.cobalt.stanza;
    // source-provenance annotations on the operations; SOURCE retention, so static
    requires static com.github.auties00.cobalt.meta;
    // JUL logging in a few operation builders
    requires java.logging;
    // JSON marshalling of the MEX operation bodies
    requires com.alibaba.fastjson2;

    // IQ (info/query) operations
    exports com.github.auties00.cobalt.wire.stanza.iq.account;
    exports com.github.auties00.cobalt.wire.stanza.iq.biz;
    exports com.github.auties00.cobalt.wire.stanza.iq.ctwa;
    exports com.github.auties00.cobalt.wire.stanza.iq.debug;
    exports com.github.auties00.cobalt.wire.stanza.iq.dirty;
    exports com.github.auties00.cobalt.wire.stanza.iq.disappearing;
    exports com.github.auties00.cobalt.wire.stanza.iq.encrypt;
    exports com.github.auties00.cobalt.wire.stanza.iq.group;
    exports com.github.auties00.cobalt.wire.stanza.iq.media;
    exports com.github.auties00.cobalt.wire.stanza.iq.privacy;
    exports com.github.auties00.cobalt.wire.stanza.iq.profilepicture;
    exports com.github.auties00.cobalt.wire.stanza.iq.push;
    exports com.github.auties00.cobalt.wire.stanza.iq.stats;
    exports com.github.auties00.cobalt.wire.stanza.iq.status;
    exports com.github.auties00.cobalt.wire.stanza.iq.syncd;
    exports com.github.auties00.cobalt.wire.stanza.iq.tos;
    exports com.github.auties00.cobalt.wire.stanza.iq;
    // MEX (GraphQL-over-stanza) operations
    exports com.github.auties00.cobalt.wire.stanza.mex.json.bot;
    exports com.github.auties00.cobalt.wire.stanza.mex.json.community;
    exports com.github.auties00.cobalt.wire.stanza.mex.json.group;
    exports com.github.auties00.cobalt.wire.stanza.mex.json.misc;
    exports com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;
    exports com.github.auties00.cobalt.wire.stanza.mex.json.user;
    exports com.github.auties00.cobalt.wire.stanza.mex.json;
    exports com.github.auties00.cobalt.wire.stanza.mex;
    // SMAX (server-managed action exchange) operations
    exports com.github.auties00.cobalt.wire.stanza.smax.abprops;
    exports com.github.auties00.cobalt.wire.stanza.smax.account;
    exports com.github.auties00.cobalt.wire.stanza.smax.biz;
    exports com.github.auties00.cobalt.wire.stanza.smax.bot;
    exports com.github.auties00.cobalt.wire.stanza.smax.bugreporting;
    exports com.github.auties00.cobalt.wire.stanza.smax.chatstate;
    exports com.github.auties00.cobalt.wire.stanza.smax.clientexpiration;
    exports com.github.auties00.cobalt.wire.stanza.smax.coexistence;
    exports com.github.auties00.cobalt.wire.stanza.smax.groups;
    exports com.github.auties00.cobalt.wire.stanza.smax.inappcomms;
    exports com.github.auties00.cobalt.wire.stanza.smax.mdcompanion;
    exports com.github.auties00.cobalt.wire.stanza.smax.message;
    exports com.github.auties00.cobalt.wire.stanza.smax.newsletters;
    exports com.github.auties00.cobalt.wire.stanza.smax.offlinebatch;
    exports com.github.auties00.cobalt.wire.stanza.smax.passivemode;
    exports com.github.auties00.cobalt.wire.stanza.smax.pings;
    exports com.github.auties00.cobalt.wire.stanza.smax.prekeys;
    exports com.github.auties00.cobalt.wire.stanza.smax.presence;
    exports com.github.auties00.cobalt.wire.stanza.smax.privacy;
    exports com.github.auties00.cobalt.wire.stanza.smax.privatestats;
    exports com.github.auties00.cobalt.wire.stanza.smax.profilepicture;
    exports com.github.auties00.cobalt.wire.stanza.smax.psa;
    exports com.github.auties00.cobalt.wire.stanza.smax.pushconfig;
    exports com.github.auties00.cobalt.wire.stanza.smax.qp;
    exports com.github.auties00.cobalt.wire.stanza.smax.status;
    exports com.github.auties00.cobalt.wire.stanza.smax.support;
    exports com.github.auties00.cobalt.wire.stanza.smax.unifiedsession;
    exports com.github.auties00.cobalt.wire.stanza.smax.usernotice;
    exports com.github.auties00.cobalt.wire.stanza.smax.util;
    exports com.github.auties00.cobalt.wire.stanza.smax.voip;
    exports com.github.auties00.cobalt.wire.stanza.smax.waffle;
    exports com.github.auties00.cobalt.wire.stanza.smax;
    // USync (user-sync) operations
    exports com.github.auties00.cobalt.wire.stanza.usync.protocol;
    exports com.github.auties00.cobalt.wire.stanza.usync.result;
    exports com.github.auties00.cobalt.wire.stanza.usync;
}
