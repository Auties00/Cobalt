package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import com.github.auties00.cobalt.wire.linked.integrity.IntegrityChallenge;

/**
 * A functional interface for the
 * {@link LinkedWhatsAppClientListener#onIntegrityChallenge onIntegrityChallenge} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised
 * whenever the server pushes an integrity checkpoint to the session, before the client attempts to
 * satisfy it with the configured passkey authenticator. It lets an application observe the
 * checkpoint (for example to alert an operator) independently of whether the client can answer it.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedIntegrityChallengeListener extends LinkedListener {
    /**
     * Notifies the listener that the server pushed an integrity checkpoint.
     *
     * @param whatsapp  the client emitting the event
     * @param challenge the challenge the server is demanding
     */
    void onIntegrityChallenge(LinkedWhatsAppClient whatsapp, IntegrityChallenge challenge);
}
