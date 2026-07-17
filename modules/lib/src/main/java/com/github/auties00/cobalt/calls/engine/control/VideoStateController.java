package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.engine.participant.VideoStreamState;
import com.github.auties00.cobalt.calls.signaling.incall.VideoStateStanza;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.util.Objects;
import com.github.auties00.cobalt.calls.engine.control.event.PeerVideoPermissionChanged;
import com.github.auties00.cobalt.calls.engine.control.event.VideoStateChanged;

/**
 * Drives video control during a call: the local camera lifecycle and the video upgrade negotiation.
 *
 * <p>This controller owns the local user's {@link VideoStreamState} and the actions that change it. The
 * camera lifecycle is {@link #turnCamera(boolean)} (on or off), {@link #pause()}, {@link #resume()}, and the
 * picture in picture toggle {@link #setPictureInPicture(boolean)}. The video upgrade negotiation, which asks
 * a one to one peer to turn an audio only call into a video call, is driven by {@link #requestUpgrade(boolean)}
 * (the v1 or v2 request), {@link #acceptUpgrade()}, {@link #rejectUpgrade()}, and {@link #cancelUpgrade()}.
 * Every action broadcasts the resulting state with a {@code video_state} action and emits a
 * {@link VideoStateChanged} for the local user.
 *
 * <p>The state machine mapping is: the camera lifecycle drives {@link VideoStreamState#ENABLED} and
 * {@link VideoStreamState#DISABLED}; pause and resume drive {@link VideoStreamState#PAUSED} and back to
 * {@link VideoStreamState#ENABLED}; the upgrade negotiation uses {@link VideoStreamState#UPGRADE_REQUEST} or
 * {@link VideoStreamState#UPGRADE_REQUEST_V2}, then {@link VideoStreamState#UPGRADE_ACCEPT},
 * {@link VideoStreamState#UPGRADE_REJECT}, or {@link VideoStreamState#UPGRADE_CANCEL}. The picture in picture
 * flag is a local rendering hint that does not itself change the broadcast state.
 *
 * <p>Inbound peer reports are delivered through {@link #onPeerVideoState(Jid, VideoStreamState)}, which
 * emits a {@link VideoStateChanged} for the peer, and {@link #onPeerVideoPermission(Jid, boolean)}, which
 * emits a {@link PeerVideoPermissionChanged}. The controller holds the local state in volatile fields and is
 * bound to one call's identity and its signaling and event seams at construction; it owns no timers, so it
 * needs no explicit shutdown.
 *
 * @implNote This implementation holds the local video state and picture in picture flag in volatile fields
 * rather than behind a lock, since each is a lone reference or flag with no compound read modify write.
 */
public final class VideoStateController {
    /**
     * The logger for {@link VideoStateController}.
     */
    private static final System.Logger LOGGER = Log.get(VideoStateController.class);

    /**
     * The call identity this controller stamps onto its video actions.
     */
    private final CallControlContext context;

    /**
     * The signaling egress video actions are sent through.
     */
    private final CallSignalingSender sender;

    /**
     * The event sink video events are emitted into.
     */
    private final CallEventSink events;

    /**
     * The local user's current video stream state.
     *
     * <p>Volatile so {@link #transition(VideoStreamState)} can store it and {@link #state()} can read it
     * without a lock: the field is a lone reference with no compound read modify write.
     */
    private volatile VideoStreamState state = VideoStreamState.DISABLED;

    /**
     * Whether the local preview is currently minimized to picture in picture.
     *
     * <p>Volatile so {@link #setPictureInPicture(boolean)} can store it and {@link #isPictureInPicture()}
     * can read it without a lock: the field is a lone flag with no compound read modify write.
     */
    private volatile boolean pictureInPicture;

    /**
     * Constructs a video state controller bound to a call and its signaling and event seams.
     *
     * @param context the call identity to stamp onto video actions; never {@code null}
     * @param sender  the signaling egress to send video actions through; never {@code null}
     * @param events  the event sink to emit video events into; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public VideoStateController(CallControlContext context, CallSignalingSender sender, CallEventSink events) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
        this.events = Objects.requireNonNull(events, "events cannot be null");
    }

    /**
     * Turns the local camera on or off, broadcasting the resulting video state.
     *
     * <p>Transitions the local state to {@link VideoStreamState#ENABLED} or
     * {@link VideoStreamState#DISABLED} and broadcasts it.
     *
     * @param on {@code true} to turn the camera on, {@code false} to turn it off
     */
    public void turnCamera(boolean on) {
        transition(on ? VideoStreamState.ENABLED : VideoStreamState.DISABLED);
    }

    /**
     * Pauses the local video stream, broadcasting the paused state.
     *
     * <p>Transitions the local state to {@link VideoStreamState#PAUSED}; a paused stream can be resumed
     * without a fresh upgrade negotiation.
     */
    public void pause() {
        transition(VideoStreamState.PAUSED);
    }

    /**
     * Resumes a paused local video stream, broadcasting the enabled state.
     *
     * <p>Transitions the local state back to {@link VideoStreamState#ENABLED}.
     */
    public void resume() {
        transition(VideoStreamState.ENABLED);
    }

    /**
     * Issues a request to upgrade an audio only call to video, broadcasting the request state.
     *
     * <p>Transitions the local state to {@link VideoStreamState#UPGRADE_REQUEST_V2} when {@code v2} is
     * requested (for peers that advertise the v2 capability) or {@link VideoStreamState#UPGRADE_REQUEST}
     * otherwise, and broadcasts it. The peer answers with an accept or a reject.
     *
     * @param v2 {@code true} to use the newer v2 upgrade request, {@code false} for the v1 request
     */
    public void requestUpgrade(boolean v2) {
        transition(v2 ? VideoStreamState.UPGRADE_REQUEST_V2 : VideoStreamState.UPGRADE_REQUEST);
    }

    /**
     * Accepts a pending video upgrade request, broadcasting the accept state.
     *
     * <p>Transitions the local state to {@link VideoStreamState#UPGRADE_ACCEPT} (engine code {@code 4}).
     */
    public void acceptUpgrade() {
        transition(VideoStreamState.UPGRADE_ACCEPT);
    }

    /**
     * Rejects a pending video upgrade request, broadcasting the reject state.
     *
     * <p>Transitions the local state to {@link VideoStreamState#UPGRADE_REJECT} (reason {@code 5}).
     */
    public void rejectUpgrade() {
        transition(VideoStreamState.UPGRADE_REJECT);
    }

    /**
     * Cancels a video upgrade request the local user issued, broadcasting the cancel state.
     *
     * <p>Transitions the local state to {@link VideoStreamState#UPGRADE_CANCEL} (reason {@code 8}).
     */
    public void cancelUpgrade() {
        transition(VideoStreamState.UPGRADE_CANCEL);
    }

    /**
     * Sets whether the local preview is minimized to picture in picture.
     *
     * <p>This is a local rendering hint and does not change or broadcast the video stream state; it is
     * retained so a caller can query it through {@link #isPictureInPicture()}.
     *
     * @param pictureInPicture {@code true} when the local preview is minimized to picture in picture
     */
    public void setPictureInPicture(boolean pictureInPicture) {
        this.pictureInPicture = pictureInPicture;
    }

    /**
     * Returns whether the local preview is currently minimized to picture in picture.
     *
     * @return {@code true} when the local preview is minimized to picture in picture
     */
    public boolean isPictureInPicture() {
        return pictureInPicture;
    }

    /**
     * Returns the local user's current video stream state.
     *
     * @return the current local video stream state; never {@code null}
     */
    public VideoStreamState state() {
        return state;
    }

    /**
     * Handles an inbound peer video state report, emitting the peer's change.
     *
     * <p>Emits a {@link VideoStateChanged} for the reporting peer; it does not change the local state.
     *
     * @param peer  the device JID of the reporting peer; never {@code null}
     * @param state the peer's reported video stream state; never {@code null}
     * @throws NullPointerException if {@code peer} or {@code state} is {@code null}
     */
    public void onPeerVideoState(Jid peer, VideoStreamState state) {
        Objects.requireNonNull(peer, "peer cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "peer {0} video state -> {1}", peer, state);
        events.emit(new VideoStateChanged(peer, state, false));
    }

    /**
     * Handles an inbound change to a peer's permission to send video, emitting the change.
     *
     * <p>Emits a {@link PeerVideoPermissionChanged} for the affected peer.
     *
     * @param peer    the device JID whose video permission changed; never {@code null}
     * @param allowed {@code true} when the peer is now permitted to send video
     * @throws NullPointerException if {@code peer} is {@code null}
     */
    public void onPeerVideoPermission(Jid peer, boolean allowed) {
        Objects.requireNonNull(peer, "peer cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "peer {0} video permission allowed={1}", peer, allowed);
        events.emit(new PeerVideoPermissionChanged(peer, allowed));
    }

    /**
     * Sets the local video state, broadcasts it, and emits the local change.
     *
     * <p>Records the new state, sends a {@code video_state} action carrying it, and emits a
     * {@link VideoStateChanged} for the local user.
     *
     * @param next the new local video stream state
     */
    private void transition(VideoStreamState next) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "local video state {0} -> {1}", state, next);
        this.state = next;
        sender.send(new VideoStateStanza(context.callId(), context.callCreator(), next));
        events.emit(new VideoStateChanged(context.selfJid(), next, true));
    }
}
