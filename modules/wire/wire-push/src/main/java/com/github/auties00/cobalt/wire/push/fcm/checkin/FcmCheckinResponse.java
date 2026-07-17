package com.github.auties00.cobalt.wire.push.fcm.checkin;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Models the wire shape of the response to the checkin POST, decoded after the body has been gunzipped.
 *
 * <p>The message carries the credentials Cobalt needs to authenticate against the FCM message connection
 * service: the server-assigned Android id and its paired security token. The matching request is built from
 * {@link FcmCheckinRequest}.
 *
 * @implNote This implementation maps only the two fields Cobalt consumes; every other field the server
 * returns is left out of the schema and discarded during decoding.
 */
@ProtobufMessage(name = "FcmCheckinResponse")
public final class FcmCheckinResponse {
    /**
     * Holds the server-assigned 64-bit Android device id.
     *
     * <p>This value is used as the username when logging in to the FCM message connection service.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.FIXED64)
    long androidId;

    /**
     * Holds the server-assigned 64-bit security token paired with {@link #androidId}.
     *
     * <p>This value is used as the password when logging in to the FCM message connection service.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.FIXED64)
    long securityToken;

    /**
     * Constructs a new checkin response with the given values.
     *
     * @param androidId     the server-assigned Android id
     * @param securityToken the paired security token
     */
    FcmCheckinResponse(long androidId, long securityToken) {
        this.androidId = androidId;
        this.securityToken = securityToken;
    }

    /**
     * Returns the server-assigned Android device id.
     *
     * @return the Android id
     */
    public long androidId() {
        return androidId;
    }

    /**
     * Returns the server-assigned security token.
     *
     * @return the security token
     */
    public long securityToken() {
        return securityToken;
    }
}
