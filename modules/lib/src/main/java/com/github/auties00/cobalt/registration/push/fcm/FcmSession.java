package com.github.auties00.cobalt.registration.push.fcm;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable state of an {@link FcmClient}: the immutable {@link FcmConfig} plus every credential and stream cursor
 * accumulated across the registration handshake and the live MCS stream.
 *
 * <p>Round-tripping the value returned by {@link FcmClient#getSession()} through
 * {@link FcmClient#loadSession(FcmSession)} reuses the same FCM registration token across process restarts; the
 * protobuf codec on this class encodes the full session in one blob.
 *
 * @implNote
 * This implementation is mutated in place by both {@link FcmRegistration} (during the three-step handshake) and
 * {@link FcmMcsConnection} (as persistent ids accumulate); callers that snapshot the session concurrently with an
 * active MCS connection should copy {@link #persistentIds()} first.
 */
@ProtobufMessage(name = "FcmSession")
public final class FcmSession {
    /**
     * Configuration the session was created against.
     *
     * <p>Bundled in the serialized output so a saved session loads back without the caller having to remember which
     * {@link FcmConfig} it was originally created against.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    FcmConfig config;

    /**
     * Server-assigned 64-bit Android device id from the {@code /checkin} step.
     *
     * <p>Becomes the username on the MCS login. {@code 0} means no checkin has been performed yet, which is the
     * trigger {@link FcmRegistration#ensureCredentials(FcmSession)} uses to decide it must run the checkin step.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    long androidId;

    /**
     * Server-assigned 64-bit security token paired with {@link #androidId}.
     *
     * <p>Becomes the password on the MCS login. {@code 0} means no checkin has been performed yet.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    long securityToken;

    /**
     * Firebase Installation Id returned by the FIS endpoint.
     *
     * <p>Sent as the {@code X-appid} header on GCM register3. Empty when the FIS step has not been performed, or when
     * {@link FcmConfig#useFis()} is {@code false}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String fid;

    /**
     * FIS auth token returned alongside {@link #fid}.
     *
     * <p>Sent as the {@code X-Goog-Firebase-Installations-Auth} header on GCM register3;
     * {@link FcmRegistration#ensureCredentials(FcmSession)} re-runs the FIS step when {@link #fisExpiresAt} is within
     * 60 s of the wall clock.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String fisAuthToken;

    /**
     * FIS refresh token returned alongside {@link #fid}.
     *
     * <p>Currently stored but not consumed; the client just re-runs the full FIS install when the auth token expires.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String fisRefreshToken;

    /**
     * Wall-clock second at which {@link #fisAuthToken} expires.
     *
     * <p>Compared against {@code System.currentTimeMillis() / 1000 + 60} to decide whether the FIS step needs a
     * refresh on the next {@link FcmRegistration#ensureCredentials(FcmSession)} call.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT64)
    long fisExpiresAt;

    /**
     * FCM registration token, the public output of the three-step handshake.
     *
     * <p>Empty until GCM register3 succeeds; once populated it is the value the WhatsApp registration server pushes
     * verification codes to via {@link FcmClient#getPushToken()}.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String fcmToken;

    /**
     * Per-message persistent ids the server has delivered.
     *
     * <p>Replayed on the next MCS login so the server stops redelivering messages this client has already acked
     * locally; bounded to the most-recent 50 entries by {@link FcmMcsConnection} so the serialized session stays
     * compact.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    List<String> persistentIds;

    /**
     * Constructs a new session with the given values.
     *
     * <p>Used by the protobuf codec on decode and by {@link #newSession(FcmConfig)} to produce an empty starting
     * state; an explicit {@code null} {@code persistentIds} is normalised to an empty mutable {@link ArrayList}.
     *
     * @param config          the session configuration
     * @param androidId       the server-assigned Android device id
     * @param securityToken   the server-assigned security token
     * @param fid             the Firebase Installation Id
     * @param fisAuthToken    the FIS auth token
     * @param fisRefreshToken the FIS refresh token
     * @param fisExpiresAt    epoch second of FIS token expiry
     * @param fcmToken        the FCM registration token
     * @param persistentIds   the persistent ids accumulated from MCS
     */
    FcmSession(FcmConfig config, long androidId, long securityToken, String fid,
               String fisAuthToken, String fisRefreshToken, long fisExpiresAt,
               String fcmToken, List<String> persistentIds) {
        this.config = config;
        this.androidId = androidId;
        this.securityToken = securityToken;
        this.fid = fid;
        this.fisAuthToken = fisAuthToken;
        this.fisRefreshToken = fisRefreshToken;
        this.fisExpiresAt = fisExpiresAt;
        this.fcmToken = fcmToken;
        this.persistentIds = persistentIds == null ? new ArrayList<>() : persistentIds;
    }

    /**
     * Creates an empty session bound to {@code config}.
     *
     * <p>Every credential field is zero or empty; used by
     * {@link FcmClient#authenticate(LinkedWhatsAppClientDevice)} before
     * {@link FcmRegistration#ensureCredentials(FcmSession)} runs the three-step handshake.
     *
     * @param config the configuration to bind
     * @return a fresh empty session
     */
    static FcmSession newSession(FcmConfig config) {
        return new FcmSession(config, 0L, 0L, "", "", "", 0L, "", new ArrayList<>());
    }

    /**
     * Returns the bound configuration.
     *
     * @return the configuration
     */
    public FcmConfig config() {
        return config;
    }

    /**
     * Returns the server-assigned Android device id.
     *
     * @return the Android id, or {@code 0} when no checkin has run yet
     */
    public long androidId() {
        return androidId;
    }

    /**
     * Returns the server-assigned security token.
     *
     * @return the security token, or {@code 0} when no checkin has run yet
     */
    public long securityToken() {
        return securityToken;
    }

    /**
     * Returns the Firebase Installation Id.
     *
     * @return the FID, or empty when the FIS step has not run
     */
    public String fid() {
        return fid;
    }

    /**
     * Returns the FIS auth token.
     *
     * @return the FIS auth token, or empty when the FIS step has not run
     */
    public String fisAuthToken() {
        return fisAuthToken;
    }

    /**
     * Returns the FIS refresh token.
     *
     * @return the FIS refresh token, or empty when the FIS step has not run
     */
    public String fisRefreshToken() {
        return fisRefreshToken;
    }

    /**
     * Returns the epoch second at which {@link #fisAuthToken} expires.
     *
     * @return the expiry second, or {@code 0} when the FIS step has not run
     */
    public long fisExpiresAt() {
        return fisExpiresAt;
    }

    /**
     * Returns the FCM registration token.
     *
     * @return the FCM token, or empty when register3 has not run
     */
    public String fcmToken() {
        return fcmToken;
    }

    /**
     * Returns the live mutable list of replayable persistent ids.
     *
     * <p>The list is mutated in place by {@link FcmMcsConnection}; callers that need a stable view should copy it
     * under external synchronisation.
     *
     * @return the persistent ids
     */
    public List<String> persistentIds() {
        return persistentIds;
    }

    /**
     * Stores the server-assigned {@link #androidId}.
     *
     * <p>Called by {@link FcmRegistration} after parsing the {@code /checkin} response.
     *
     * @param androidId the Android device id
     */
    void setAndroidId(long androidId) {
        this.androidId = androidId;
    }

    /**
     * Stores the server-assigned {@link #securityToken}.
     *
     * <p>Called by {@link FcmRegistration} after parsing the {@code /checkin} response.
     *
     * @param securityToken the security token
     */
    void setSecurityToken(long securityToken) {
        this.securityToken = securityToken;
    }

    /**
     * Stores the {@link #fid}.
     *
     * <p>Called by {@link FcmRegistration} after the FIS install succeeds; the value either echoes the candidate FID
     * submitted by the client or replaces it with the server-confirmed one.
     *
     * @param fid the Firebase Installation Id
     */
    void setFid(String fid) {
        this.fid = fid;
    }

    /**
     * Stores the {@link #fisAuthToken}.
     *
     * @param fisAuthToken the FIS auth token
     */
    void setFisAuthToken(String fisAuthToken) {
        this.fisAuthToken = fisAuthToken;
    }

    /**
     * Stores the {@link #fisRefreshToken}.
     *
     * @param fisRefreshToken the FIS refresh token
     */
    void setFisRefreshToken(String fisRefreshToken) {
        this.fisRefreshToken = fisRefreshToken;
    }

    /**
     * Stores the absolute expiry of {@link #fisAuthToken}.
     *
     * @param fisExpiresAt the epoch second at which the FIS token expires
     */
    void setFisExpiresAt(long fisExpiresAt) {
        this.fisExpiresAt = fisExpiresAt;
    }

    /**
     * Stores the {@link #fcmToken}.
     *
     * <p>Called by {@link FcmRegistration} after register3 returns; the value is the public output of the whole
     * three-step handshake.
     *
     * @param fcmToken the FCM registration token
     */
    void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
