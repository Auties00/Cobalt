package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.wire.linked.call.JoinableCallLink;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallLinkLobbyJoinRequest onCallLinkLobbyJoinRequest} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallLinkLobbyJoinRequestListener extends LinkedListener {
    /**
     * Notifies the listener that someone has clicked a call-link the
     * local user owns and is now waiting in the lobby for the host to
     * admit them.
     *
     * <p>The host is expected to admit or deny the waiting joiner in
     * response.
     *
     * @param whatsapp the client emitting the event
     * @param link     the link the joiner is waiting on
     * @param peer     the JID of the joiner waiting in the lobby
     */
    void onCallLinkLobbyJoinRequest(LinkedWhatsAppClient whatsapp, JoinableCallLink link, Jid peer);
}
