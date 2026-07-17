package com.github.auties00.cobalt.wire.push.fcm.mcs;

import com.github.auties00.cobalt.wire.push.fcm.checkin.FcmCheckinResponse;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * Models the first framed packet sent on the MCS stream after the one-byte
 * version preamble.
 *
 * <p>The login request authenticates the device with the
 * {@code androidId}/{@code securityToken} pair from {@link FcmCheckinResponse}
 * and replays any unacked persistent ids from previous sessions, so the server
 * can drop them from its retry queue. The remaining fields are filled with the
 * fixed values the native client advertises.
 *
 * @implNote This implementation carries the MCS frame tag {@code 2}; the tag is
 * written as the frame's length-prefixed type byte by the connection layer, not
 * by this message.
 */
@ProtobufMessage(name = "FcmMcsLoginRequest")
public final class FcmMcsLoginRequest {
    /**
     * Holds the client id.
     *
     * @implNote This implementation always sends {@code "android-30"}, matching
     * the SDK level Cobalt advertises during checkin.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * Holds the MCS domain.
     *
     * @implNote This implementation always sends {@code "mcs.android.com"}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String domain;

    /**
     * Holds the MCS username.
     *
     * <p>This is the decimal {@code androidId} rendered as a string.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String user;

    /**
     * Holds the MCS resource.
     *
     * <p>This is also the decimal {@code androidId} rendered as a string; it
     * matches the value placed in {@link #user}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String resource;

    /**
     * Holds the MCS password.
     *
     * <p>This is the decimal {@code securityToken} rendered as a string.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String authToken;

    /**
     * Holds the device id derived from {@link #user}.
     *
     * <p>This is formatted as {@code "android-" + Long.toHexString(androidId)}.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String deviceId;

    /**
     * Holds the repeated key/value settings.
     *
     * @implNote This implementation always sends a single {@code new_vc=1}
     * entry, mirroring the native client.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    List<Setting> settings;

    /**
     * Holds the persistent ids the client has already acked locally.
     *
     * <p>The server uses this list to skip redelivery of those messages after a
     * reconnect.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    List<String> persistentIds;

    /**
     * Indicates whether to use adaptive heartbeats.
     *
     * @implNote This implementation always sends {@code false}, matching the
     * native client.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    boolean adaptiveHeartbeat;

    /**
     * Indicates whether to use the RMQ2 ack scheme.
     *
     * @implNote This implementation always sends {@code true}, matching the
     * native client.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.BOOL)
    boolean useRmq2;

    /**
     * Holds the auth service id.
     *
     * @implNote This implementation always sends {@code 2}, matching the native
     * client.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.INT64)
    long authService;

    /**
     * Holds the network type id.
     *
     * @implNote This implementation always sends {@code 1}, matching the native
     * client.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.INT64)
    long networkType;

    /**
     * Constructs a new login request with the given values.
     *
     * @param id                the client id
     * @param domain            the MCS domain
     * @param user              the MCS username
     * @param resource          the MCS resource
     * @param authToken         the MCS password
     * @param deviceId          the device id
     * @param settings          the key/value settings
     * @param persistentIds     the persistent ids to replay
     * @param adaptiveHeartbeat whether to negotiate adaptive heartbeats
     * @param useRmq2           whether to negotiate the RMQ2 ack scheme
     * @param authService       the auth service id
     * @param networkType       the network type id
     */
    FcmMcsLoginRequest(String id, String domain, String user, String resource,
                       String authToken, String deviceId, List<Setting> settings,
                       List<String> persistentIds, boolean adaptiveHeartbeat,
                       boolean useRmq2, long authService, long networkType) {
        this.id = id;
        this.domain = domain;
        this.user = user;
        this.resource = resource;
        this.authToken = authToken;
        this.deviceId = deviceId;
        this.settings = settings;
        this.persistentIds = persistentIds;
        this.adaptiveHeartbeat = adaptiveHeartbeat;
        this.useRmq2 = useRmq2;
        this.authService = authService;
        this.networkType = networkType;
    }

    /**
     * Models one key/value setting on the login packet.
     *
     * @implNote This implementation only ever emits {@code new_vc=1}; other
     * settings the native client may include are not modeled.
     */
    @ProtobufMessage(name = "FcmMcsLoginRequest.Setting")
    public static final class Setting {
        /**
         * Holds the setting name.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String name;

        /**
         * Holds the setting value.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String value;

        /**
         * Constructs a new setting with the given name and value.
         *
         * @param name  the setting name
         * @param value the setting value
         */
        Setting(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
