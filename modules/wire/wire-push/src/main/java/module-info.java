/**
 * Defines the third-party push-protocol wire models the mobile registration flow speaks.
 *
 * <p>This module is used by the client library's push clients to encode and decode the Google FCM Mobile
 * Connection Server (MCS) stanzas and the Android check-in request/response. These are third-party Google
 * protocols with no WhatsApp counterpart, so they carry no source-provenance annotations and reference no domain
 * model; the push socket connections and crypto that drive these stanzas remain in the client library.
 */
module com.github.auties00.cobalt.wire.push {
    // protobuf runtime for the FCM and check-in message types
    requires it.auties.protobuf.base;

    // FCM Mobile Connection Server stanzas
    exports com.github.auties00.cobalt.wire.push.fcm.mcs;
    // Android check-in request/response
    exports com.github.auties00.cobalt.wire.push.fcm.checkin;
}
