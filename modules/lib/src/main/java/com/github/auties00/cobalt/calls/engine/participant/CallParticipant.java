package com.github.auties00.cobalt.calls.engine.participant;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Aggregates one party's identity, state, devices, media, and crypto material within a
 * call.
 *
 * <p>This is the mutable record the call engine keeps for each party in a call, whether a
 * remote peer or the local self. It composes the party's identity (user JID, active device
 * JID, LID, the server assigned participant id, push and guest names,
 * {@linkplain CallParticipantPlatform platform}, {@linkplain CallParticipantAccountKind
 * account kind}, and {@linkplain CallParticipantUserType user type}), its
 * {@linkplain CallParticipantState membership state}, its list of per device
 * {@linkplain CallDeviceInfo device infos}, its {@linkplain CallParticipantMedia media}
 * object, its {@linkplain CallParticipantCrypto crypto block}, and the transient flags the
 * engine flips during a call (the screenshare flag and the video state code).
 *
 * <p>An instance of this class is never exposed outside the membership layer; the rest of
 * the engine reads a participant only through the immutable {@link ParticipantView}
 * snapshot {@link #toView()} produces under the membership lock. This keeps the mutable
 * aggregate and its lock confined to the membership owner.
 *
 * <p>This class is not thread safe. The membership layer mutates a participant only while
 * holding the call or group membership lock, and snapshots it through {@link #toView()}
 * under that same lock; callers outside the membership layer never see the mutable
 * instance.
 *
 * @implNote This implementation holds the identity, state, devices, media, and crypto as
 * discrete Java objects rather than as one flat structure, so each concern can be locked
 * and snapshotted independently.
 */
public final class CallParticipant {
    /**
     * Holds the sentinel the engine uses for an unassigned server assigned participant id.
     */
    public static final int UNASSIGNED_PID = -1;

    /**
     * Holds this participant's user JID.
     */
    private final Jid userJid;

    /**
     * Holds whether this participant is a call extension (a bot or AI stream) rather than a
     * real user.
     */
    private final boolean extension;

    /**
     * Holds this participant's membership state.
     */
    private CallParticipantState state;

    /**
     * Holds this participant's user type.
     */
    private CallParticipantUserType userType;

    /**
     * Holds this participant's account kind.
     */
    private CallParticipantAccountKind accountKind;

    /**
     * Holds this participant's client platform.
     */
    private CallParticipantPlatform platform;

    /**
     * Holds this participant's Linked Identity JID, or {@code null} if not known.
     */
    private Jid lid;

    /**
     * Holds the server assigned participant id, or {@value #UNASSIGNED_PID} if none has
     * been assigned.
     */
    private int pid;

    /**
     * Holds this participant's push name, or {@code null} if not known.
     */
    private String pushName;

    /**
     * Holds this participant's guest name, or {@code null} if not a named guest.
     */
    private String guestName;

    /**
     * Holds this participant's active device JID, or {@code null} until a device is pinned.
     */
    private Jid activeDeviceJid;

    /**
     * Holds this participant's device infos, keyed positionally with the active device kept
     * first.
     */
    private final List<CallDeviceInfo> devices;

    /**
     * Holds this participant's media object.
     */
    private final CallParticipantMedia media;

    /**
     * Holds this participant's crypto block.
     */
    private final CallParticipantCrypto crypto;

    /**
     * Holds whether this participant is currently screen sharing.
     */
    private boolean screenSharing;

    /**
     * Holds this participant's current video state code.
     */
    private int videoState;

    /**
     * Constructs a participant for the given user JID with the given extension flag, an
     * empty device list, and fresh media and crypto blocks.
     *
     * <p>The participant starts {@linkplain CallParticipantState#INVITED invited} with a
     * {@linkplain CallParticipantUserType#NORMAL normal} user type, a
     * {@linkplain CallParticipantAccountKind#REGULAR regular} account kind, an
     * {@linkplain CallParticipantPlatform#UNKNOWN unknown} platform, no LID, an unassigned
     * participant id, no names, no active device, not screen sharing, and an
     * {@linkplain ParticipantView#VIDEO_STATE_UNKNOWN unknown} video state.
     *
     * @param userJid   this participant's user JID; never {@code null}
     * @param extension whether this participant is a call extension rather than a real user
     * @throws NullPointerException if {@code userJid} is {@code null}
     */
    public CallParticipant(Jid userJid, boolean extension) {
        this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
        this.extension = extension;
        this.state = CallParticipantState.INVITED;
        this.userType = CallParticipantUserType.NORMAL;
        this.accountKind = CallParticipantAccountKind.REGULAR;
        this.platform = CallParticipantPlatform.UNKNOWN;
        this.lid = null;
        this.pid = UNASSIGNED_PID;
        this.pushName = null;
        this.guestName = null;
        this.activeDeviceJid = null;
        this.devices = new ArrayList<>(2);
        this.media = new CallParticipantMedia();
        this.crypto = new CallParticipantCrypto();
        this.screenSharing = false;
        this.videoState = ParticipantView.VIDEO_STATE_UNKNOWN;
    }

    /**
     * Constructs a non extension participant for the given user JID.
     *
     * @param userJid this participant's user JID; never {@code null}
     * @throws NullPointerException if {@code userJid} is {@code null}
     */
    public CallParticipant(Jid userJid) {
        this(userJid, false);
    }

    /**
     * Returns this participant's user JID.
     *
     * @return the user JID; never {@code null}
     */
    public Jid userJid() {
        return userJid;
    }

    /**
     * Returns whether this participant is a call extension rather than a real user.
     *
     * @return {@code true} if this participant is an extension
     */
    public boolean isExtension() {
        return extension;
    }

    /**
     * Returns this participant's membership state.
     *
     * @return the membership state; never {@code null}
     */
    public CallParticipantState state() {
        return state;
    }

    /**
     * Sets this participant's membership state.
     *
     * @param state the membership state; never {@code null}
     * @return this participant
     * @throws NullPointerException if {@code state} is {@code null}
     */
    public CallParticipant state(CallParticipantState state) {
        this.state = Objects.requireNonNull(state, "state cannot be null");
        return this;
    }

    /**
     * Returns whether this participant is connected with media flowing.
     *
     * @return {@code true} if the membership state is
     *         {@link CallParticipantState#CONNECTED}
     */
    public boolean isConnected() {
        return state.isConnected();
    }

    /**
     * Returns this participant's user type.
     *
     * @return the user type; never {@code null}
     */
    public CallParticipantUserType userType() {
        return userType;
    }

    /**
     * Sets this participant's user type.
     *
     * @param userType the user type; never {@code null}
     * @return this participant
     * @throws NullPointerException if {@code userType} is {@code null}
     */
    public CallParticipant userType(CallParticipantUserType userType) {
        this.userType = Objects.requireNonNull(userType, "userType cannot be null");
        return this;
    }

    /**
     * Returns this participant's account kind.
     *
     * @return the account kind; never {@code null}
     */
    public CallParticipantAccountKind accountKind() {
        return accountKind;
    }

    /**
     * Sets this participant's account kind.
     *
     * @param accountKind the account kind; never {@code null}
     * @return this participant
     * @throws NullPointerException if {@code accountKind} is {@code null}
     */
    public CallParticipant accountKind(CallParticipantAccountKind accountKind) {
        this.accountKind = Objects.requireNonNull(accountKind, "accountKind cannot be null");
        return this;
    }

    /**
     * Returns this participant's client platform.
     *
     * @return the platform; never {@code null}
     */
    public CallParticipantPlatform platform() {
        return platform;
    }

    /**
     * Sets this participant's client platform.
     *
     * @param platform the platform; never {@code null}
     * @return this participant
     * @throws NullPointerException if {@code platform} is {@code null}
     */
    public CallParticipant platform(CallParticipantPlatform platform) {
        this.platform = Objects.requireNonNull(platform, "platform cannot be null");
        return this;
    }

    /**
     * Returns this participant's Linked Identity JID, if known.
     *
     * @return an {@code Optional} holding the LID, or empty if not known
     */
    public Optional<Jid> lid() {
        return Optional.ofNullable(lid);
    }

    /**
     * Sets this participant's Linked Identity JID.
     *
     * @param lid the LID, or {@code null} to clear it
     * @return this participant
     */
    public CallParticipant lid(Jid lid) {
        this.lid = lid;
        return this;
    }

    /**
     * Returns the server assigned participant id, if assigned.
     *
     * @return the participant id, or {@link OptionalInt#empty()} if none has been assigned
     */
    public OptionalInt pid() {
        return pid == UNASSIGNED_PID ? OptionalInt.empty() : OptionalInt.of(pid);
    }

    /**
     * Sets the server assigned participant id.
     *
     * @param pid the participant id to record
     * @return this participant
     */
    public CallParticipant pid(int pid) {
        this.pid = pid;
        return this;
    }

    /**
     * Returns this participant's push name, if known.
     *
     * @return an {@code Optional} holding the push name, or empty if not known
     */
    public Optional<String> pushName() {
        return Optional.ofNullable(pushName);
    }

    /**
     * Sets this participant's push name.
     *
     * @param pushName the push name, or {@code null} to clear it
     * @return this participant
     */
    public CallParticipant pushName(String pushName) {
        this.pushName = pushName;
        return this;
    }

    /**
     * Returns this participant's guest name, if it joined as a named guest.
     *
     * @return an {@code Optional} holding the guest name, or empty if not a named guest
     */
    public Optional<String> guestName() {
        return Optional.ofNullable(guestName);
    }

    /**
     * Sets this participant's guest name.
     *
     * @param guestName the guest name, or {@code null} to clear it
     * @return this participant
     */
    public CallParticipant guestName(String guestName) {
        this.guestName = guestName;
        return this;
    }

    /**
     * Returns this participant's active device JID, if one has been pinned.
     *
     * <p>The active device is the device this participant is currently using for media;
     * for a one to one call it is pinned once the first inbound media identifies which
     * device answered.
     *
     * @return an {@code Optional} holding the active device JID, or empty if no device has
     *         been pinned
     */
    public Optional<Jid> activeDeviceJid() {
        return Optional.ofNullable(activeDeviceJid);
    }

    /**
     * Sets this participant's active device JID.
     *
     * @param activeDeviceJid the active device JID, or {@code null} to clear it
     * @return this participant
     */
    public CallParticipant activeDeviceJid(Jid activeDeviceJid) {
        this.activeDeviceJid = activeDeviceJid;
        return this;
    }

    /**
     * Returns the device infos of this participant.
     *
     * <p>The returned list is an unmodifiable snapshot of the current device set; the
     * {@link CallDeviceInfo} elements themselves are the live mutable instances, which the
     * membership layer mutates under its lock.
     *
     * @return an unmodifiable view of the device infos
     */
    public List<CallDeviceInfo> devices() {
        return List.copyOf(devices);
    }

    /**
     * Returns the number of devices this participant has.
     *
     * @return the device count
     */
    public int deviceCount() {
        return devices.size();
    }

    /**
     * Adds a device info to this participant if no device with the same JID is present.
     *
     * <p>If a device info with the same {@linkplain CallDeviceInfo#deviceJid() device JID}
     * already exists, that existing device info is returned unchanged; otherwise the
     * supplied device info is added and returned.
     *
     * @param device the device info to add; never {@code null}
     * @return the device info now held for the JID, either the existing one or the added
     *         one
     * @throws NullPointerException if {@code device} is {@code null}
     */
    public CallDeviceInfo addDevice(CallDeviceInfo device) {
        Objects.requireNonNull(device, "device cannot be null");
        for (var existing : devices) {
            if (existing.deviceJid().equals(device.deviceJid())) {
                return existing;
            }
        }
        devices.add(device);
        return device;
    }

    /**
     * Returns the device info for the given device JID, if present.
     *
     * @param deviceJid the device JID to look up; never {@code null}
     * @return an {@code Optional} holding the matching device info, or empty if no device
     *         matches
     * @throws NullPointerException if {@code deviceJid} is {@code null}
     */
    public Optional<CallDeviceInfo> device(Jid deviceJid) {
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        for (var device : devices) {
            if (device.deviceJid().equals(deviceJid)) {
                return Optional.of(device);
            }
        }
        return Optional.empty();
    }

    /**
     * Removes the device info for the given device JID, if present.
     *
     * @param deviceJid the device JID to remove; never {@code null}
     * @return {@code true} if a device info was removed
     * @throws NullPointerException if {@code deviceJid} is {@code null}
     */
    public boolean removeDevice(Jid deviceJid) {
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        return devices.removeIf(device -> device.deviceJid().equals(deviceJid));
    }

    /**
     * Returns the device info for this participant's active device, if both the active
     * device JID and a matching device info are present.
     *
     * @return an {@code Optional} holding the active device info, or empty if no active
     *         device is pinned or no matching device info exists
     */
    public Optional<CallDeviceInfo> activeDevice() {
        return activeDeviceJid().flatMap(this::device);
    }

    /**
     * Returns this participant's media object.
     *
     * <p>The returned object is the live mutable media instance; the membership layer
     * mutates it under its lock.
     *
     * @return the media object; never {@code null}
     */
    public CallParticipantMedia media() {
        return media;
    }

    /**
     * Returns this participant's crypto block.
     *
     * <p>The returned object is the live mutable crypto instance; the membership and crypto
     * layers mutate it under the membership and stream locks.
     *
     * @return the crypto block; never {@code null}
     */
    public CallParticipantCrypto crypto() {
        return crypto;
    }

    /**
     * Returns whether this participant is currently screen sharing.
     *
     * @return {@code true} if the participant is screen sharing
     */
    public boolean isScreenSharing() {
        return screenSharing;
    }

    /**
     * Sets whether this participant is currently screen sharing.
     *
     * @param screenSharing {@code true} to mark the participant screen sharing
     * @return this participant
     */
    public CallParticipant screenSharing(boolean screenSharing) {
        this.screenSharing = screenSharing;
        return this;
    }

    /**
     * Returns this participant's current video state code.
     *
     * @return the video state code
     */
    public int videoState() {
        return videoState;
    }

    /**
     * Sets this participant's current video state code.
     *
     * @param videoState the video state code to record
     * @return this participant
     */
    public CallParticipant videoState(int videoState) {
        this.videoState = videoState;
        return this;
    }

    /**
     * Returns an immutable snapshot of this participant.
     *
     * <p>This is the only way the wider engine reads a participant: it flattens the
     * participant's identity and state into an immutable {@link ParticipantView}, leaving
     * the mutable aggregate confined to the membership layer. The snapshot reports the
     * participant as connected when its membership state is
     * {@link CallParticipantState#CONNECTED} and as active when its state is allocated. An
     * extension participant always reports an
     * {@linkplain ParticipantView#VIDEO_STATE_ENABLED enabled} video state, overriding its
     * own video state code. The video enabled flag is read from the active device info and
     * the subscribed encoded video stream id from the media object when present, each
     * falling back to its absent default otherwise; the RTCP encoded stream id is not
     * tracked here and is always reported as {@link ParticipantView#NO_SUBSCRIBED_STREAM}.
     *
     * @return an immutable view of this participant; never {@code null}
     */
    public ParticipantView toView() {
        var resolvedVideoState = extension ? ParticipantView.VIDEO_STATE_ENABLED : videoState;
        var deviceVideoEnabled = activeDevice()
                .map(CallDeviceInfo::videoEnabled)
                .orElse(false);
        var subscribedVideoStreamId = media.subscribedVideoStreamId()
                .orElse(ParticipantView.NO_SUBSCRIBED_STREAM);
        return new ParticipantView(
                true,
                extension,
                isConnected(),
                state.isAllocated(),
                platform,
                userJid,
                activeDeviceJid,
                resolvedVideoState,
                deviceVideoEnabled,
                screenSharing,
                subscribedVideoStreamId,
                ParticipantView.NO_SUBSCRIBED_STREAM);
    }

    /**
     * Indicates whether the given object is a participant with the same user JID and
     * extension flag.
     *
     * <p>Equality is decided by identity only, ignoring the mutable state, devices, media,
     * and crypto, so a participant compares equal to itself across the lifetime of a call
     * regardless of how its runtime state evolves.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is a {@link CallParticipant} with the same
     *         {@link #userJid()} and {@link #isExtension()}
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof CallParticipant that
                && this.extension == that.extension
                && this.userJid.equals(that.userJid));
    }

    /**
     * Returns a hash code derived from the user JID and extension flag.
     *
     * @return a hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(userJid, extension);
    }

    /**
     * Returns a string carrying this participant's identity and current membership summary.
     *
     * @return a debug string with the user JID, extension flag, state, platform, device
     *         count, and active device JID
     */
    @Override
    public String toString() {
        return "CallParticipant[userJid=" + userJid
                + ", extension=" + extension
                + ", state=" + state
                + ", platform=" + platform
                + ", deviceCount=" + devices.size()
                + ", activeDeviceJid=" + activeDeviceJid
                + ']';
    }
}
