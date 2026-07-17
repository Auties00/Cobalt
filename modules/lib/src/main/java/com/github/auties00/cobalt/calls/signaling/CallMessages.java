package com.github.auties00.cobalt.calls.signaling;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

/**
 * Holds the shared wire constants and common header helper the call message payload records use.
 *
 * <p>Every call action element inside the top level {@code <call>} stanza carries two universal
 * attributes stamped on every outgoing action and required on every inbound one: the call identifier
 * ({@code call-id}) and the call creator's device {@link Jid} ({@code call-creator}). This holder
 * centralizes those two attribute names and the {@link #stampHeader(StanzaBuilder, String, Jid)}
 * helper so each payload record applies them identically, keeping the per record builders to their
 * own type specific attributes and children. It holds no state and is not instantiable.
 */
public final class CallMessages {
    /**
     * The wire attribute naming the call identifier, stamped on every action element.
     */
    public static final String CALL_ID_ATTRIBUTE = "call-id";

    /**
     * The wire attribute naming the call creator's device {@link Jid}, stamped on every action element.
     */
    public static final String CALL_CREATOR_ATTRIBUTE = "call-creator";

    /**
     * Prevents instantiation of this holder.
     *
     * @throws AssertionError always, since this class is not instantiable
     */
    private CallMessages() {
        throw new AssertionError("CallMessages is not instantiable");
    }

    /**
     * Stamps the universal call header onto an action element builder.
     *
     * <p>Applies the {@link #CALL_ID_ATTRIBUTE} and {@link #CALL_CREATOR_ATTRIBUTE} attributes in
     * order, returning the same builder so the caller can chain its type specific attributes and
     * content.
     *
     * @param builder     the action element builder to stamp
     * @param callId      the call identifier
     * @param callCreator the call creator's device {@link Jid}
     * @return the supplied {@code builder}, with the common header applied
     */
    public static StanzaBuilder stampHeader(StanzaBuilder builder, String callId, Jid callCreator) {
        return builder
                .attribute(CALL_ID_ATTRIBUTE, callId)
                .attribute(CALL_CREATOR_ATTRIBUTE, callCreator);
    }
}
