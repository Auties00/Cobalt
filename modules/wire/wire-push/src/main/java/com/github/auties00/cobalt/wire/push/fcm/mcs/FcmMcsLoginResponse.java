package com.github.auties00.cobalt.wire.push.fcm.mcs;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Models the server reply to {@link FcmMcsLoginRequest} on the MCS stream.
 *
 * <p>On a successful login {@link #error()} is {@code null} and
 * {@link #serverTimestamp()} carries the server's clock; on a rejected login
 * {@link #error()} carries the code and message pair describing the failure.
 *
 * @implNote This implementation carries the MCS frame tag {@code 3}; the tag is
 * read from the frame's length-prefixed type byte by the connection layer, not
 * from this message.
 */
@ProtobufMessage(name = "FcmMcsLoginResponse")
public final class FcmMcsLoginResponse {
    /**
     * Holds the optional error block, present only when the login is rejected.
     *
     * <p>This is {@code null} if and only if the login succeeded.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    ErrorInfo error;

    /**
     * Holds the server's wall-clock at login time, in milliseconds since the
     * epoch.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.INT64)
    long serverTimestamp;

    /**
     * Constructs a new login response with the given values.
     *
     * @param error           the optional error block
     * @param serverTimestamp the server timestamp in milliseconds
     */
    FcmMcsLoginResponse(ErrorInfo error, long serverTimestamp) {
        this.error = error;
        this.serverTimestamp = serverTimestamp;
    }

    /**
     * Returns the optional error block.
     *
     * @return the error info, or {@code null} on success
     */
    public ErrorInfo error() {
        return error;
    }

    /**
     * Returns the server timestamp in milliseconds since the epoch.
     *
     * @return the server timestamp
     */
    public long serverTimestamp() {
        return serverTimestamp;
    }

    /**
     * Models the non-zero error code plus human-readable message returned when
     * the login is rejected.
     *
     * <p>Common codes include "Expired security token", which requires forcing a
     * fresh checkin, and "Invalid credentials", which indicates the
     * {@code androidId}/{@code securityToken} pair is wrong.
     */
    @ProtobufMessage(name = "FcmMcsLoginResponse.ErrorInfo")
    public static final class ErrorInfo {
        /**
         * Holds the numeric error code.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.INT64)
        long code;

        /**
         * Holds the human-readable error message.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String message;

        /**
         * Constructs a new error block with the given code and message.
         *
         * @param code    the numeric error code
         * @param message the human-readable message
         */
        ErrorInfo(long code, String message) {
            this.code = code;
            this.message = message;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the code
         */
        public long code() {
            return code;
        }

        /**
         * Returns the human-readable error message.
         *
         * @return the message
         */
        public String message() {
            return message;
        }
    }
}
