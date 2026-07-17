package com.github.auties00.cobalt.listener.linked;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;

/**
 * A functional interface for the {@link LinkedWhatsAppClientListener#onCallTranscript onCallTranscript} event.
 *
 * <p>{@link LinkedWhatsAppClientListener} extends this interface and supplies an empty
 * default implementation, so the event can also be observed in isolation as a
 * lambda.
 *
 * @see LinkedWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface LinkedCallTranscriptListener extends LinkedListener {
    /**
     * Notifies the listener that a live transcription fragment was received for a
     * call participant's speech.
     *
     * @param whatsapp   the client emitting the event
     * @param callId     the identifier of the call
     * @param speakerJid the JID of the participant whose speech was transcribed
     * @param text       the transcribed text fragment
     * @param language   the {@code BCP-47} language tag of the text, or {@code null} when the engine
     *                   did not supply one
     * @param sequence   the monotonic sequence number of this fragment within the speaker's stream
     */
    void onCallTranscript(LinkedWhatsAppClient whatsapp, String callId, Jid speakerJid, String text, String language, long sequence);
}
