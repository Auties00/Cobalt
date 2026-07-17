/**
 * Defines the stanza node model and its binary-XMPP codec for Cobalt.
 *
 * <p>This module is used by the whole binary-XMPP transport: the {@code Stanza} node tree (its builder,
 * attribute and boolean-format types, plus the {@code SizedInputStream} sized-payload leaf that streams
 * length-prefixed binary content straight to the wire) and the {@code stanza.binary} tokenization codec that
 * serialises those nodes to and from socket bytes. A captured node renders to a redacted structural summary
 * through the {@code cobalt-telemetry-core} facade, so it never leaks message content. The typed IQ/MEX/SMAX/USync
 * operation models build on this layer in {@code cobalt-wire-stanza}.
 */
module com.github.auties00.cobalt.stanza {
    // transitive: Jid and the protobuf mixins appear on the Stanza and SizedInputStream API
    requires transitive com.github.auties00.cobalt.wire.core;
    // source-provenance annotations on the stanza model; SOURCE retention, so static
    requires static com.github.auties00.cobalt.meta;
    // the codec renders captured nodes through the logging facade
    requires com.github.auties00.cobalt.telemetry;

    // Stanza node model, its builder/attribute types and the SizedInputStream leaf
    exports com.github.auties00.cobalt.stanza.model;
    // Binary-XMPP tokenization codec (reader, writer, sizer)
    exports com.github.auties00.cobalt.stanza.binary;
}
