package com.github.auties00.cobalt.calls.engine.mediaplane;

import com.github.auties00.cobalt.calls.engine.LifecycleController;
import com.github.auties00.cobalt.calls.engine.participant.CallMembership;
import com.github.auties00.cobalt.calls.transport.datachannel.AppDataController;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.List;
import java.util.Optional;

/**
 * Brings up and tears down one call's media plane on behalf of the lifecycle controller.
 *
 * <p>Once a call is answered the engine sequences its media plane from the relay credentials to flowing
 * audio and video: it bootstraps the transport session, starts the chosen transport (the relay path or
 * the Web P2P path), derives the per direction SRTP and per participant SFrame keys from the call key,
 * and starts the encode and decode pumps. That whole bring up is owned by the transport and media units;
 * this seam is the single handle the {@link LifecycleController} uses to start it for a call and to
 * tear it down when the call ends, so the controller orchestrates the phase without owning the transport
 * and codec wiring. It is an internal engine collaborator and not part of the public client surface.
 *
 * <p>Bringing the media plane up is keyed by the call's relay block (the {@code <relay>} subtree the offer
 * ack, the offer, or a group update carries) and the call key, plus whether the local side placed the call
 * (the caller is the transport client, the callee the server). The returned {@link Session} is the live
 * media plane; the controller holds it for the call's lifetime and closes it on teardown, which stops the
 * transport and the media pumps.
 *
 * @implNote The production implementation drives the bring up over the call transport controller, the media
 * transport seam, and the audio and video encode and decode pumps, together with the SRTP and SFrame key
 * derivation; this seam carries only the start and close the controller drives.
 */
public interface MediaPlane {
    /**
     * Brings up the media plane for a call from its relay block, engine parameter bundles, call key, and
     * application media streams.
     *
     * <p>The implementer bootstraps the transport, starts it from the relay credentials, derives the
     * SRTP and SFrame keys from the call key, parses the {@code <voip_settings>} bundles into the call's
     * voip param manager and selects the active set for the call's media mode, and starts the media pumps,
     * driving the encode path from the {@link MediaStreams#audioCapture() capture sources} and
     * delivering decoded remote media to the {@link MediaStreams#audioPlayback() playback sinks}. The
     * calling virtual thread blocks for the synchronous part of the bring up (the transport bootstrap
     * round trip); the connection reaching the bidirectional media state is reported asynchronously through
     * the transport and is not awaited here. A bring up that cannot start the transport surfaces as a call
     * exception that is not fatal, so a failed media plane ends only this call rather than the session.
     *
     * <p>The {@code voipSettings} bundles are the {@code <voip_settings>} children the server injected: on
     * the callee side they are the children of the inbound {@code <offer>}, and on the caller side they are
     * the children of the offer acknowledgement. Each carries a {@code type} attribute selecting the
     * settings bucket ({@code default}/{@code audio}/{@code video}); a bring up given an empty list falls
     * back to the implementer's compiled in defaults. The {@code participantCount} is the call's current
     * membership size, used to retune the active set for the call size; a one to one call passes a count of
     * two (or zero when no membership is tracked).
     *
     * <p>The local device's call JID, the keying input for the deterministic media SSRC set this client
     * transmits on, is read by the implementer from its own self JID holder (the same JID the SFrame key
     * derivation uses), so the audio, video, and application data SSRCs the encode path stamps and the
     * {@code StreamLayout} the subscription layer advertises are derived from it and the {@code callId}
     * through the secure SSRC generator, never drawn at random; a sender stamps the exact SSRC its peer
     * registers a receive context for in advance. The {@code membership} is the call's participant manager
     * on a group call, read to attribute an inbound application data packet to the participant whose device
     * transmitted it (by mapping the packet's RTP SSRC to a member device) and threaded into the subscription
     * publisher's participant read seam; it is {@code null} on a one to one call, which has a single fixed
     * peer and tracks no roster.
     *
     * @param callId           the identifier of the call whose media plane is being brought up
     * @param relay            the call's {@code <relay>} block subtree carrying the relay credentials,
     *                         tokens, and endpoints
     * @param voipSettings     the {@code <voip_settings>} bundle nodes the offer (callee) or offer
     *                         acknowledgement (caller) carried, in wire order; never {@code null}, possibly
     *                         empty
     * @param callKey          the raw 32 byte end to end call key the SRTP and SFrame keys derive from
     * @param isCaller         whether the local side placed the call (the transport client) rather than
     *                         answered it (the transport server)
     * @param video            whether the local side participates with video, so a video encode pump is
     *                         started in addition to the audio pumps and the video settings bucket is
     *                         selected; an audio only answer to a video offer passes {@code false}
     * @param participantCount the call's current membership size, used to retune the active voip param set
     *                         for the call size
     * @param membership       the call's participant manager on a group call, read for inbound app data
     *                         attribution and threaded into the subscription publisher, or {@code null} on a
     *                         one to one call
     * @param streams          the application capture sources and playback sinks the encode and decode
     *                         pipelines drive, or {@link MediaStreams#none()} to fall back to platform
     *                         devices
     * @param peerDeviceJid    the peer's device JID on a one to one call, keying the inbound end to end SRTP
     *                         master, or {@code null} when not yet known or on a group call
     * @param electedRelayName the {@linkplain com.github.auties00.cobalt.calls.signaling.relay.RelayEndpoint#relayName()
     *                         name} of the relay the peer aware election chose, restricting the relay endpoint
     *                         selection to that relay so both ends bind the relay they share; an
     *                         {@linkplain Optional#empty() empty} value leaves the implementer on its local
     *                         lowest latency pick, the fallback before the peer's latency report has arrived
     * @param listener         the per call listener the plane reports the media connected and local capture
     *                         interrupted events to; the controller supplies it so the plane holds no
     *                         reference to the controller
     * @return the live media plane session; never {@code null}
     * @throws NullPointerException                                                if {@code callId},
     *                                                                             {@code relay},
     *                                                                             {@code voipSettings},
     *                                                                             {@code callKey},
     *                                                                             {@code streams}, or
     *                                                                             {@code listener} is
     *                                                                             {@code null}
     * @throws com.github.auties00.cobalt.exception.linked.WhatsAppCallException.DataChannel if the transport or
     *                                                                             media plane cannot be
     *                                                                             brought up
     */
    Session bringUp(String callId, Stanza relay, List<Stanza> voipSettings, byte[] callKey, boolean isCaller,
                    boolean video, int participantCount, CallMembership membership,
                    MediaStreams streams, Jid peerDeviceJid, Optional<String> electedRelayName,
                    MediaSessionListener listener);

    /**
     * A live media plane session the controller holds for a call's lifetime.
     *
     * <p>The session owns the call's transport and media pumps; the controller closes it once when the
     * call tears down, which stops the transport and the encode and decode pumps. {@link #close()} is
     * idempotent so a teardown that races a transport driven close is safe.
     */
    interface Session extends AutoCloseable {
        /**
         * Stops the call's transport and media pumps.
         *
         * <p>Idempotent: a second close, or a close that races the transport's own teardown, does nothing.
         * Unlike {@link AutoCloseable#close()} this declares no checked exception, so the controller can
         * close it from a teardown path without wrapping.
         */
        @Override
        void close();

        /**
         * Returns the live application data controller this session brought up, if it has one.
         *
         * <p>The session constructs the app data controller as part of its bring up and owns its lifecycle,
         * closing it with the session. The controller is exposed so the in call control units, which are
         * built later in the call lifecycle than the media plane, can attach themselves to its inbound
         * observer seams (reactions, transcription) once they exist. A session whose transport carries no
         * app data plane has none. The lifecycle controller reads this after bring up to bind the reaction
         * and transcription control units to the call's app data side channel; it is not part of the public
         * client surface.
         *
         * @implSpec The default implementation returns {@link Optional#empty()}, so a session
         *           implementation that brings up no app data plane needs no override.
         * @return an {@link Optional} holding the session's app data controller, or empty when the session
         * has none
         */
        default Optional<AppDataController> appDataController() {
            return Optional.empty();
        }

        /**
         * Starts the local camera video track on this session, driving the outbound video encode path.
         *
         * <p>This is the media plane half of an in call camera turn on, the counterpart to the
         * {@code video_state} announce the {@link LifecycleController} drives in parallel: it starts the
         * call's camera capture source and the outbound video encode loop so the local picture begins flowing
         * to the peer. It is idempotent, so a session whose outbound video is already running (a call brought
         * up with video) does nothing, and a repeated turn on does not start the camera twice. The lifecycle
         * controller drives this from {@link LifecycleController#startLocalVideo(String)} alongside the
         * {@code video_state} announce; it is not part of the public client surface.
         *
         * @implSpec The default implementation does nothing, so a session that brings up no video plane (an
         *           audio only or test session) needs no override.
         */
        default void startLocalVideo() {
        }

        /**
         * Arms the outbound video encoder so its next encoded picture is a key frame.
         *
         * <p>This is the media plane half of an outbound key frame request: the local application, or a peer
         * relayed picture loss indication the upper layers translate, asks the encoder to emit a fresh intra
         * frame the decoder can resynchronize on. A session whose outbound video is not running does nothing.
         * The lifecycle controller drives this from a
         * {@link com.github.auties00.cobalt.wire.linked.call.CallInteraction.KeyFrameRequest}; it is not part of the
         * public client surface.
         *
         * @implSpec The default implementation does nothing, so a session that brings up no video plane (an
         *           audio only or test session) needs no override.
         */
        default void requestKeyFrame() {
        }
    }
}
