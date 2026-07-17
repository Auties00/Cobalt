package com.github.auties00.cobalt.wire.stanza.smax.chatstate;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

/**
 * Represents the sealed disjunction over the two outbound chat-state payloads
 * that a {@link SmaxClientNotificationRequest} can carry: composing or paused.
 *
 * <p>The choice mirrors the local user's UI state: an idle state maps to
 * {@link SmaxClientNotificationPaused}, while both typing and voice-note
 * recording map to {@link SmaxClientNotificationComposing}, differing only by
 * {@link SmaxClientNotificationComposing#hasComposingMediaAudio()}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutChatstateStateTypes")
public sealed interface SmaxClientNotificationStateType permits SmaxClientNotificationComposing, SmaxClientNotificationPaused {
    /**
     * Builds the state-type child stanza.
     *
     * <p>This method is invoked by {@link SmaxClientNotificationRequest#toStanza()}
     * to attach the inner child to the {@code <chatstate/>} envelope.
     *
     * @implSpec
     * Implementors must return a {@link StanzaBuilder} whose root description is
     * either {@code "composing"} or {@code "paused"} so the relay can route
     * it. The returned builder must be ready to {@link StanzaBuilder#build()}
     * into the inner child of the {@code <chatstate/>} envelope; implementors
     * must not nest it inside another wrapper.
     *
     * @return a {@link StanzaBuilder} carrying the state-type child
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutChatstateStateTypes",
            exports = "mergeStateTypes", adaptation = WhatsAppAdaptation.ADAPTED)
    StanzaBuilder toStanza();
}
