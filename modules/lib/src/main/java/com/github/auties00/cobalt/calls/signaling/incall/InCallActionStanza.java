package com.github.auties00.cobalt.calls.signaling.incall;

import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallStanza;

/**
 * Represents an in call control action of the {@code <call>} signaling plane.
 *
 * <p>An in call action mutates a call that is already connected rather than driving its setup or
 * teardown: muting a participant, raising a hand, toggling video or screen share, reporting a peer
 * state, flow controlling the video bitrate, sending a DTMF tone, reconfiguring a bot, or attaching a
 * call extension. These actions form a distinct family from the lifecycle messages (offer, accept,
 * reject, terminate) and the control plane messages (group, link, waiting room), and this sealed
 * interface groups them so a consumer can exhaustively switch over the in call actions alone.
 *
 * <p>This interface extends {@link CallMessage} and adds no operations of its own: an in call action is
 * a {@link CallMessage} like any other action element, carried as one child of the {@code <call>}
 * envelope, decoded and encoded through {@link CallStanza}, and forwarded through the same receiver
 * sink. The separation exists purely for exhaustive pattern matching over the in call subset; the
 * {@link CallMessage#type()} and {@link CallMessage#toStanza()} contract is inherited unchanged.
 *
 * <p>Several in call actions do not ride a dedicated {@code <call>} child of their own but are carried
 * inside a shared message container (the {@code 0x68}, {@code 0x6c}, {@code 0x70}, and {@code 0x74}
 * containers), and a couple ({@link RaiseHandStanza}) carry no entry in the numeric
 * {@code voip_signaling_message_type} table at all, so their {@link CallMessage#type()} is {@code null}
 * and they are routed by their wire tag. The grouping here is by control semantics, not by container or
 * taxonomy presence.
 *
 * @see CallMessage
 * @see CallStanza
 */
public sealed interface InCallActionStanza extends CallMessage permits
        DtmfStanza,
        ExtensionStanza,
        FlowControlStanza,
        InterruptionStanza,
        MuteV2Stanza,
        NotifyStanza,
        PeerStateStanza,
        RaiseHandStanza,
        ReconfigureBotStanza,
        ScreenShareStanza,
        VideoStateStanza {
}
