package com.github.auties00.cobalt.calls.signaling;

import com.github.auties00.cobalt.calls.engine.participant.VideoStreamState;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;

import java.util.List;
import java.util.function.BiFunction;
import com.github.auties00.cobalt.calls.signaling.incall.DtmfStanza;
import com.github.auties00.cobalt.calls.signaling.incall.FlowControlStanza;
import com.github.auties00.cobalt.calls.signaling.incall.InterruptionStanza;
import com.github.auties00.cobalt.calls.signaling.incall.MuteV2Stanza;
import com.github.auties00.cobalt.calls.signaling.incall.NotifyStanza;
import com.github.auties00.cobalt.calls.signaling.incall.PeerStateStanza;
import com.github.auties00.cobalt.calls.signaling.incall.ReconfigureBotStanza;
import com.github.auties00.cobalt.calls.signaling.incall.ScreenShareStanza;
import com.github.auties00.cobalt.calls.signaling.incall.VideoStateStanza;
import com.github.auties00.cobalt.calls.signaling.receive.CallSignalingRouter;
import com.github.auties00.cobalt.calls.signaling.session.AcceptStanza;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.calls.signaling.session.PreacceptStanza;
import com.github.auties00.cobalt.calls.signaling.session.RejectStanza;
import com.github.auties00.cobalt.calls.signaling.session.TerminateStanza;

/**
 * Representative {@link CallMessage} fixtures and an enum of inbound actions that DO carry a
 * {@link SignalingType} taxonomy ordinal, so each must classify to {@link CallSignalingRouter.Disposition#PROCESS}
 * with a present {@link CallSignalingRouter.Verdict#type()} and parse back to its own record. The
 * ordinal-less {@code <ringing>} and {@code <raise_hand>} actions are deliberately excluded from this
 * enum because they route via the parser-known tag fallback with an empty verdict type; they are
 * covered directly in the routing tests.
 */
final class SignalingFixtures {
    private SignalingFixtures() {
    }

    static OfferStanza minimalOffer(String callId, Jid callCreator) {
        return new OfferStanza(callId, callCreator, null, null, null, null, null, null,
                false, false, null, -1, -1, List.of(), List.of(), List.of(), List.of(), null,
                null, null, null, null, null, null, List.of(), null);
    }

    /**
     * One representative tag-routable inbound action per record, each pinned to the
     * {@link SignalingType} the router must classify it to and the record the parser must yield.
     */
    enum Kind {
        OFFER(SignalingType.OFFER, OfferStanza.class,
                SignalingFixtures::minimalOffer),
        ACCEPT(SignalingType.ACCEPT, AcceptStanza.class,
                (id, creator) -> new AcceptStanza(id, creator, 2, List.of(), List.of(), List.of(), List.of(),
                        null, null, null, null)),
        PREACCEPT(SignalingType.PREACCEPT, PreacceptStanza.class,
                (id, creator) -> new PreacceptStanza(id, creator, List.of(), List.of(), List.of(), null, null)),
        REJECT(SignalingType.REJECT, RejectStanza.class,
                (id, creator) -> RejectStanza.of(id, creator, CallEndReason.REJECT_DO_NOT_DISTURB)),
        TERMINATE(SignalingType.TERMINATE, TerminateStanza.class,
                (id, creator) -> TerminateStanza.of(id, creator, CallEndReason.HANGUP, List.of())),
        MUTE_V2(SignalingType.MUTE_V2, MuteV2Stanza.class,
                (id, creator) -> MuteV2Stanza.ofSelfState(id, creator, true, false)),
        DTMF(SignalingType.DTMF_TONE, DtmfStanza.class,
                (id, creator) -> new DtmfStanza(id, creator, "5")),
        INTERRUPTION(SignalingType.INTERRUPTION, InterruptionStanza.class,
                (id, creator) -> new InterruptionStanza(id, creator, true, 1)),
        NOTIFY(SignalingType.NOTIFY, NotifyStanza.class,
                (id, creator) -> new NotifyStanza(id, creator, 80)),
        SCREEN_SHARE(SignalingType.SCREEN_SHARE, ScreenShareStanza.class,
                (id, creator) -> new ScreenShareStanza(id, creator, 1, 3)),
        VIDEO_STATE(SignalingType.VIDEO_STATE, VideoStateStanza.class,
                (id, creator) -> new VideoStateStanza(id, creator, VideoStreamState.ENABLED)),
        PEER_STATE(SignalingType.PEER_STATE, PeerStateStanza.class,
                (id, creator) -> new PeerStateStanza(id, creator,
                        Jid.of("55555555", JidServer.lid(), 3, 0), 1)),
        FLOW_CONTROL(SignalingType.FLOW_CONTROL, FlowControlStanza.class,
                (id, creator) -> new FlowControlStanza(id, creator, 7, 300000, 640, 30)),
        RECONFIGURE_BOT(SignalingType.RECONFIGURE_BOT, ReconfigureBotStanza.class,
                (id, creator) -> new ReconfigureBotStanza(id, creator, 9));

        private final SignalingType expectedType;
        private final Class<? extends CallMessage> recordClass;
        private final BiFunction<String, Jid, ? extends CallMessage> factory;

        Kind(SignalingType expectedType, Class<? extends CallMessage> recordClass,
             BiFunction<String, Jid, ? extends CallMessage> factory) {
            this.expectedType = expectedType;
            this.recordClass = recordClass;
            this.factory = factory;
        }

        CallMessage build(String callId, Jid callCreator) {
            return factory.apply(callId, callCreator);
        }

        SignalingType expectedType() {
            return expectedType;
        }

        Class<? extends CallMessage> recordClass() {
            return recordClass;
        }
    }
}
