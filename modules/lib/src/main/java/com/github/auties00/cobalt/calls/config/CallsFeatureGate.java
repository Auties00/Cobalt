package com.github.auties00.cobalt.calls.config;

import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;

import java.util.Objects;

/**
 * Typed read facade over the server pushed AB props that gate wa voip calling behaviour.
 *
 * <p>WhatsApp ships its calling capabilities behind server pushed feature flags rather than a
 * static client build: each capability (one to one calling, group calling, call links, screen
 * share) and each negotiated protocol version (admin control, LID addressing, audio sharing) is
 * keyed by a numeric {@link ABProp} the relay syncs over the binary XMPP socket. The calls engine
 * reads these flags to decide whether to spin up at all, what to advertise in an {@code <offer>},
 * and which protocol versions to negotiate.
 *
 * <p>This facade owns no flag storage. It holds the same {@link ABPropsService} instance the owning
 * client already owns and forwards each read to that service's typed accessor keyed by the matching
 * {@link ABProp} constant; the service caches the synced values, applies the production vs beta
 * default, and coerces the raw string. Boolean predicates return the coerced flag; the integer reads
 * (versions, counts, and intervals) return the value the relay synced, or the prop default
 * (ultimately {@code 0}).
 *
 * <p>Reads block on the first AB props sync by default, matching {@link ABPropsService}'s
 * {@code waitForSync = true} accessors, so a gate query made before the cache is warm waits up to
 * the service's sync timeout and then falls back to the prop default. The per call
 * {@code <voip_settings>} parameter bundle is a separate engine side channel (see
 * {@link VoipSettings}) and is never routed through this gate; the engine booleans carried there are
 * not server AB props and are not read here.
 *
 * <p>This is the single seam through which calls reads calling feature flags: engine components take
 * an injected {@link CallsFeatureGate} rather than reaching for {@link ABPropsService} directly, so
 * the call relevant flag subset is named once here instead of scattering raw {@link ABProp}
 * constants across the call path.
 */
public final class CallsFeatureGate {
    /**
     * The AB props service whose cached flags back every read.
     *
     * <p>This is the same {@link ABPropsService} instance the owning client uses for all other
     * AB prop reads; the gate never creates a second service or cache.
     */
    private final ABPropsService abPropsService;

    /**
     * Constructs a feature gate backed by the given AB props service.
     *
     * @param abPropsService the AB props service whose cached flags back every read
     * @throws NullPointerException if {@code abPropsService} is {@code null}
     */
    public CallsFeatureGate(ABPropsService abPropsService) {
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
    }

    /**
     * Returns whether one to one web calling is enabled for this account.
     *
     * <p>This is the master gate for the web/desktop one to one call surface: when it is
     * {@code false} the engine offers and advertises no one to one calling capability. Reads
     * {@link ABProp#ENABLE_WEB_CALLING}.
     *
     * @return {@code true} when one to one web calling is enabled
     */
    public boolean isWebCallingEnabled() {
        return abPropsService.getBool(ABProp.ENABLE_WEB_CALLING);
    }

    /**
     * Returns whether web group calling is enabled for this account.
     *
     * <p>This is the master gate for the web group call surface, which also covers the call link
     * join path because joining a call link enters a group call. When it is {@code false} the
     * engine neither starts nor joins group calls. Reads {@link ABProp#ENABLE_WEB_GROUP_CALLING}.
     *
     * @return {@code true} when web group calling is enabled
     */
    public boolean isWebGroupCallingEnabled() {
        return abPropsService.getBool(ABProp.ENABLE_WEB_GROUP_CALLING);
    }

    /**
     * Returns whether group calls are available, the semantic gate the call path checks before
     * starting or joining a multi party call.
     *
     * <p>Group calling requires both the one to one calling master gate and the group calling master
     * gate, because the group call path builds on the same engine the one to one path spins up; this
     * predicate is the conjunction of {@link #isWebCallingEnabled()} and
     * {@link #isWebGroupCallingEnabled()}.
     *
     * @return {@code true} when group calls may be started or joined
     */
    public boolean isGroupCallsEnabled() {
        return isWebCallingEnabled() && isWebGroupCallingEnabled();
    }

    /**
     * Returns whether call links are available, the semantic gate the call path checks before
     * creating, querying, or joining a joinable call link.
     *
     * <p>A call link is a joinable entry point into a group call, so call link support is gated on
     * group calling being available; this predicate is equivalent to {@link #isGroupCallsEnabled()}.
     * There is no separate host AB prop that enables call links independently of group calling.
     *
     * @return {@code true} when call links may be created or joined
     */
    public boolean isCallLinkEnabled() {
        return isGroupCallsEnabled();
    }

    /**
     * Returns whether in call screen sharing is available.
     *
     * <p>Screen sharing is gated by a milestone version rather than a boolean: a positive
     * {@linkplain #screenShareMilestoneVersion() milestone version} means the capability is enabled,
     * and screen sharing additionally requires the one to one calling master gate since the
     * screen share surface rides an active call. This predicate is {@code true} when
     * {@link #isWebCallingEnabled()} holds and the milestone version is greater than {@code 0}.
     *
     * @return {@code true} when in call screen sharing may be started
     */
    public boolean isScreenShareEnabled() {
        return isWebCallingEnabled() && screenShareMilestoneVersion() > 0;
    }

    /**
     * Returns whether username addressed calling is enabled.
     *
     * <p>When enabled the engine may place and accept calls addressed by username rather than by
     * phone number. Reads {@link ABProp#ENABLE_CALLING_USERNAME}.
     *
     * @return {@code true} when username addressed calling is enabled
     */
    public boolean isCallingUsernameEnabled() {
        return abPropsService.getBool(ABProp.ENABLE_CALLING_USERNAME);
    }

    /**
     * Returns whether phone number privacy is enabled for calling.
     *
     * <p>When enabled the engine withholds the caller's phone number where the calling privacy
     * surface allows it. Reads {@link ABProp#ENABLE_CALLING_PHONE_NUMBER_PRIVACY}.
     *
     * @return {@code true} when calling phone number privacy is enabled
     */
    public boolean isPhoneNumberPrivacyEnabled() {
        return abPropsService.getBool(ABProp.ENABLE_CALLING_PHONE_NUMBER_PRIVACY);
    }

    /**
     * Returns whether initial bandwidth estimation is enabled for group calls.
     *
     * <p>When enabled the engine seeds an initial BWE estimate when a group call starts rather than
     * ramping from cold. Reads {@link ABProp#ENABLE_INIT_BWE_FOR_GROUP_CALL}.
     *
     * @return {@code true} when initial group call bandwidth estimation is enabled
     */
    public boolean isInitBweForGroupCallEnabled() {
        return abPropsService.getBool(ABProp.ENABLE_INIT_BWE_FOR_GROUP_CALL);
    }

    /**
     * Returns the group call membership heartbeat cadence, in seconds.
     *
     * <p>A placed group call arms its membership heartbeat timer at this period; WhatsApp reads it
     * from this server setting at call bring up rather than from a fixed build constant, which is why
     * the engine resolves it through this gate instead of a Java literal. Reads
     * {@link ABProp#HEARTBEAT_INTERVAL_S}, whose default is {@code 10}.
     *
     * @return the heartbeat interval, in seconds
     */
    public int heartbeatIntervalSeconds() {
        return abPropsService.getInt(ABProp.HEARTBEAT_INTERVAL_S);
    }

    /**
     * Returns whether a one to one {@code <terminate>} is ignored while a group call is active.
     *
     * <p>When enabled the engine drops a one to one terminate that arrives during a group call, so a
     * stale one to one leg does not tear down the group call. Reads
     * {@link ABProp#IGNORE_ONE_TO_ONE_TERMINATE_IN_GROUP_CALL}.
     *
     * @return {@code true} when a one to one terminate is ignored during a group call
     */
    public boolean isIgnoreOneToOneTerminateInGroupCall() {
        return abPropsService.getBool(ABProp.IGNORE_ONE_TO_ONE_TERMINATE_IN_GROUP_CALL);
    }

    /**
     * Returns whether a {@code <terminate>} is ignored for a joinable call link whose offer has
     * expired.
     *
     * <p>When enabled the engine drops a terminate for a joinable (call link) offer that has already
     * expired, so a late terminate does not abort an otherwise valid join. Reads
     * {@link ABProp#IGNORE_JOINABLE_TERMINATE_ON_EXPIRED_OFFER}.
     *
     * @return {@code true} when a joinable terminate is ignored on an expired offer
     */
    public boolean isIgnoreJoinableTerminateOnExpiredOffer() {
        return abPropsService.getBool(ABProp.IGNORE_JOINABLE_TERMINATE_ON_EXPIRED_OFFER);
    }

    /**
     * Returns whether coexistence calling is enabled.
     *
     * <p>Coexistence calling lets a linked client place calls while a primary device is also
     * connected. This prop is off in production and on in the beta programme, so the resolved value
     * depends on whether the session is on WhatsApp Web Beta. Reads
     * {@link ABProp#COEX_CALLING_ENABLED}.
     *
     * @return {@code true} when coexistence calling is enabled
     */
    public boolean isCoexCallingEnabled() {
        // TODO: linked while primary coexistence calling is not implemented yet; no engine site consumes this flag
        return abPropsService.getBool(ABProp.COEX_CALLING_ENABLED);
    }

    /**
     * Returns the maximum number of participants allowed in a group call.
     *
     * <p>The engine caps group call membership at this value. Reads
     * {@link ABProp#GROUP_CALL_MAX_PARTICIPANTS}, whose default is {@code 32}.
     *
     * @return the group call participant ceiling
     */
    public int groupCallMaxParticipants() {
        return abPropsService.getInt(ABProp.GROUP_CALL_MAX_PARTICIPANTS);
    }

    /**
     * Returns the negotiated group call admin control protocol version.
     *
     * <p>The engine negotiates the admin control protocol at this version; {@code 0} disables admin
     * control. Reads {@link ABProp#CALL_ADMIN_VERSION}.
     *
     * @return the admin control protocol version
     */
    public int callAdminVersion() {
        // TODO: negotiate the group call admin control protocol; no engine site consumes this version yet
        return abPropsService.getInt(ABProp.CALL_ADMIN_VERSION);
    }

    /**
     * Returns the negotiated LID addressing version for calls.
     *
     * <p>The engine uses this version to decide whether and how to address call participants by LID
     * rather than phone number; {@code 0} disables LID addressing. Reads
     * {@link ABProp#CALLING_LID_VERSION}.
     *
     * @return the calling LID addressing version
     */
    public int callingLidVersion() {
        return abPropsService.getInt(ABProp.CALLING_LID_VERSION);
    }

    /**
     * Returns the negotiated in call audio sharing protocol version.
     *
     * <p>The engine negotiates in call audio sharing at this version; {@code 0} disables audio
     * sharing. Reads {@link ABProp#CALLING_AUDIO_SHARE_VERSION}.
     *
     * @return the in call audio sharing version
     */
    public int callingAudioShareVersion() {
        // TODO: negotiate in call audio sharing; no engine site consumes this version yet
        return abPropsService.getInt(ABProp.CALLING_AUDIO_SHARE_VERSION);
    }

    /**
     * Returns the screen share milestone version.
     *
     * <p>This is the host side screen share capability gate expressed as a milestone version: a value
     * greater than {@code 0} enables in call screen sharing and selects its protocol milestone, and
     * {@link #isScreenShareEnabled()} folds this together with the calling master gate. Reads
     * {@link ABProp#CALLING_SCREEN_SHARE_MILESTONE_VERSION}, whose default is {@code 2}.
     *
     * @return the screen share milestone version
     */
    public int screenShareMilestoneVersion() {
        return abPropsService.getInt(ABProp.CALLING_SCREEN_SHARE_MILESTONE_VERSION);
    }
}
