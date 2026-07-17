package com.github.auties00.cobalt.calls.engine.participant;

import com.github.auties00.cobalt.calls.capability.VideoDecoderCapability;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.EnumSet;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Holds the per device state the engine tracks for one device of a call participant with
 * multiple devices.
 *
 * <p>A participant may join a call from several devices at once; the engine keeps one of
 * these records per device. Each record carries the device's {@link Jid}, the
 * numeric participant id the server assigns ({@code pid}), the device's advertised video
 * decode capabilities, a per device video enabled flag the engine toggles as the device
 * starts and stops its camera, the device's generated SSRC set, and a per device server
 * state code used when the device is serialized back into a membership stanza.
 *
 * <p>This class is mutable: the engine flips the {@linkplain #videoEnabled() video enabled
 * flag}, refreshes the {@linkplain #decoderCapabilities() decode capabilities} when a new
 * capability descriptor arrives, and fills the {@linkplain #mainSsrc() SSRC set} once it is
 * generated. The SSRC set is not generated here; it is computed by the secure SSRC
 * generator from this device's JID bound identifiers and installed through
 * {@link #ssrcs(int, int, int, int, int)}. Until the SSRC set is installed the SSRC
 * accessors report absent.
 *
 * <p>This class is not thread safe; the engine mutates a device info only while holding the
 * membership lock that guards the owning {@link CallParticipant}.
 */
public final class CallDeviceInfo {
    /**
     * Holds the sentinel the engine uses for an unassigned SSRC.
     *
     * <p>An SSRC field holds this value until the secure SSRC generator fills it; the SSRC
     * accessors report absent while a field equals this sentinel.
     */
    public static final int UNASSIGNED_SSRC = 0;

    /**
     * Holds the sentinel the engine uses for a participant id the server has not yet
     * assigned.
     *
     * <p>The {@code pid} field holds this value until the server assigns a participant id.
     */
    public static final int UNASSIGNED_PID = -1;

    /**
     * Holds this device's JID.
     */
    private final Jid deviceJid;

    /**
     * Holds the numeric participant/device id the server assigns, or {@value #UNASSIGNED_PID}
     * when none has been assigned.
     */
    private int pid;

    /**
     * Holds the video decode codecs this device has advertised.
     */
    private Set<VideoDecoderCapability> decoderCapabilities;

    /**
     * Holds whether this device currently has its outbound video enabled.
     */
    private boolean videoEnabled;

    /**
     * Holds the per device server state code used when this device is serialized into a
     * membership stanza.
     */
    private int serverState;

    /**
     * Holds this device's main RTP SSRC, or {@value #UNASSIGNED_SSRC} until generated.
     */
    private int mainSsrc;

    /**
     * Holds this device's app data DataChannel SSRC, or {@value #UNASSIGNED_SSRC} until
     * generated.
     */
    private int appDataSsrc;

    /**
     * Holds this device's IMU data SSRC, or {@value #UNASSIGNED_SSRC} until generated.
     */
    private int imuDataSsrc;

    /**
     * Holds this device's hop by hop FEC transmit SSRC, or {@value #UNASSIGNED_SSRC} until
     * generated.
     */
    private int hopByHopFecTxSsrc;

    /**
     * Holds this device's hop by hop FEC receive SSRC, or {@value #UNASSIGNED_SSRC} until
     * generated.
     */
    private int hopByHopFecRxSsrc;

    /**
     * Constructs a device info for the given device JID with no capabilities, video
     * disabled, an unassigned participant id and server state, and an unassigned SSRC set.
     *
     * @param deviceJid this device's JID; never {@code null}
     * @throws NullPointerException if {@code deviceJid} is {@code null}
     */
    public CallDeviceInfo(Jid deviceJid) {
        this.deviceJid = Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        this.pid = UNASSIGNED_PID;
        this.decoderCapabilities = EnumSet.noneOf(VideoDecoderCapability.class);
        this.videoEnabled = false;
        this.serverState = CallParticipantState.INVALID.code();
        this.mainSsrc = UNASSIGNED_SSRC;
        this.appDataSsrc = UNASSIGNED_SSRC;
        this.imuDataSsrc = UNASSIGNED_SSRC;
        this.hopByHopFecTxSsrc = UNASSIGNED_SSRC;
        this.hopByHopFecRxSsrc = UNASSIGNED_SSRC;
    }

    /**
     * Returns this device's JID.
     *
     * @return the device JID; never {@code null}
     */
    public Jid deviceJid() {
        return deviceJid;
    }

    /**
     * Returns the numeric participant/device id the server assigned to this device.
     *
     * @return the assigned participant id, or {@link OptionalInt#empty()} if none has been
     *         assigned
     */
    public OptionalInt pid() {
        return pid == UNASSIGNED_PID ? OptionalInt.empty() : OptionalInt.of(pid);
    }

    /**
     * Sets the numeric participant/device id the server assigned to this device.
     *
     * @param pid the participant id to record
     * @return this device info
     */
    public CallDeviceInfo pid(int pid) {
        this.pid = pid;
        return this;
    }

    /**
     * Returns the video decode codecs this device has advertised.
     *
     * <p>The returned set is an unmodifiable snapshot; it is empty until a capability
     * descriptor has been recorded through {@link #decoderCapabilities(Set)}.
     *
     * @return an unmodifiable view of the advertised decode codecs
     */
    public Set<VideoDecoderCapability> decoderCapabilities() {
        return Set.copyOf(decoderCapabilities);
    }

    /**
     * Records the video decode codecs this device has advertised.
     *
     * <p>The supplied set is copied defensively; callers typically pass the result of
     * {@link VideoDecoderCapability#parse(String)} over the device's capability descriptor,
     * which already applies the H264 fallback on a malformed descriptor.
     *
     * @param decoderCapabilities the advertised decode codecs; never {@code null}
     * @return this device info
     * @throws NullPointerException if {@code decoderCapabilities} is {@code null}
     */
    public CallDeviceInfo decoderCapabilities(Set<VideoDecoderCapability> decoderCapabilities) {
        Objects.requireNonNull(decoderCapabilities, "decoderCapabilities cannot be null");
        this.decoderCapabilities = decoderCapabilities.isEmpty()
                ? EnumSet.noneOf(VideoDecoderCapability.class)
                : EnumSet.copyOf(decoderCapabilities);
        return this;
    }

    /**
     * Returns whether this device currently has its outbound video enabled.
     *
     * @return {@code true} if the device's video is enabled
     */
    public boolean videoEnabled() {
        return videoEnabled;
    }

    /**
     * Sets whether this device currently has its outbound video enabled.
     *
     * @param videoEnabled {@code true} to mark the device's video enabled
     * @return this device info
     */
    public CallDeviceInfo videoEnabled(boolean videoEnabled) {
        this.videoEnabled = videoEnabled;
        return this;
    }

    /**
     * Returns the per device server state code used when this device is serialized into a
     * membership stanza.
     *
     * @return the server state code
     */
    public int serverState() {
        return serverState;
    }

    /**
     * Sets the per device server state code used when this device is serialized into a
     * membership stanza.
     *
     * <p>The membership remove path sets this to {@link CallParticipantState#INVALID}'s
     * code to mark the device removed.
     *
     * @param serverState the server state code to record
     * @return this device info
     */
    public CallDeviceInfo serverState(int serverState) {
        this.serverState = serverState;
        return this;
    }

    /**
     * Returns this device's main RTP SSRC.
     *
     * @return the main SSRC, or {@link OptionalInt#empty()} if the SSRC set has not been
     *         generated
     */
    public OptionalInt mainSsrc() {
        return mainSsrc == UNASSIGNED_SSRC ? OptionalInt.empty() : OptionalInt.of(mainSsrc);
    }

    /**
     * Returns this device's app data DataChannel SSRC.
     *
     * @return the app data SSRC, or {@link OptionalInt#empty()} if the SSRC set has not
     *         been generated
     */
    public OptionalInt appDataSsrc() {
        return appDataSsrc == UNASSIGNED_SSRC ? OptionalInt.empty() : OptionalInt.of(appDataSsrc);
    }

    /**
     * Returns this device's IMU data SSRC.
     *
     * @return the IMU data SSRC, or {@link OptionalInt#empty()} if the SSRC set has not
     *         been generated
     */
    public OptionalInt imuDataSsrc() {
        return imuDataSsrc == UNASSIGNED_SSRC ? OptionalInt.empty() : OptionalInt.of(imuDataSsrc);
    }

    /**
     * Returns this device's hop by hop FEC transmit SSRC.
     *
     * @return the hop by hop FEC transmit SSRC, or {@link OptionalInt#empty()} if the SSRC
     *         set has not been generated
     */
    public OptionalInt hopByHopFecTxSsrc() {
        return hopByHopFecTxSsrc == UNASSIGNED_SSRC ? OptionalInt.empty() : OptionalInt.of(hopByHopFecTxSsrc);
    }

    /**
     * Returns this device's hop by hop FEC receive SSRC.
     *
     * @return the hop by hop FEC receive SSRC, or {@link OptionalInt#empty()} if the SSRC
     *         set has not been generated
     */
    public OptionalInt hopByHopFecRxSsrc() {
        return hopByHopFecRxSsrc == UNASSIGNED_SSRC ? OptionalInt.empty() : OptionalInt.of(hopByHopFecRxSsrc);
    }

    /**
     * Installs the device wide SSRC set generated for this device.
     *
     * <p>This records the main RTP SSRC plus the app data, IMU data, and hop by hop FEC
     * transmit and receive SSRCs. The per stream video and screenshare SSRCs are held by
     * the participant media object rather than per device, so they are not installed here.
     * The SSRC values are produced by the secure SSRC generator owned by the call crypto
     * layer, not by this holder.
     *
     * @param mainSsrc           the main RTP SSRC
     * @param appDataSsrc        the app data DataChannel SSRC
     * @param imuDataSsrc        the IMU data SSRC
     * @param hopByHopFecTxSsrc  the hop by hop FEC transmit SSRC
     * @param hopByHopFecRxSsrc  the hop by hop FEC receive SSRC
     * @return this device info
     */
    public CallDeviceInfo ssrcs(int mainSsrc, int appDataSsrc, int imuDataSsrc, int hopByHopFecTxSsrc, int hopByHopFecRxSsrc) {
        this.mainSsrc = mainSsrc;
        this.appDataSsrc = appDataSsrc;
        this.imuDataSsrc = imuDataSsrc;
        this.hopByHopFecTxSsrc = hopByHopFecTxSsrc;
        this.hopByHopFecRxSsrc = hopByHopFecRxSsrc;
        return this;
    }

    /**
     * Indicates whether another object is a device info for the same device.
     *
     * <p>Two device infos are equal when their {@link #deviceJid()} are equal; the mutable
     * state (participant id, capabilities, video flag, server state, and SSRCs) is ignored,
     * so a device retains its identity across mutation.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is a {@code CallDeviceInfo} with an equal device
     *         JID
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof CallDeviceInfo that && this.deviceJid.equals(that.deviceJid));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}, derived solely from the
     * {@link #deviceJid()}.
     *
     * @return the device JID hash code
     */
    @Override
    public int hashCode() {
        return deviceJid.hashCode();
    }

    /**
     * Returns a string describing this device's JID, participant id, video flag, server
     * state, and advertised decode capabilities.
     *
     * @return a diagnostic string for this device info
     */
    @Override
    public String toString() {
        return "CallDeviceInfo[deviceJid=" + deviceJid
                + ", pid=" + pid
                + ", videoEnabled=" + videoEnabled
                + ", serverState=" + serverState
                + ", decoderCapabilities=" + decoderCapabilities
                + ']';
    }
}
