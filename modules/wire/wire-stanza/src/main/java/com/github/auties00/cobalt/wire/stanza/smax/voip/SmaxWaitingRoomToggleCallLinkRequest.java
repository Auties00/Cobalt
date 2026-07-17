package com.github.auties00.cobalt.wire.stanza.smax.voip;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;

/**
 * Flips the waiting-room gate on an existing call link.
 *
 * <p>This is the outbound {@code <call><waiting_room_toggle/></call>} request
 * behind the call-link admin's "Require approval to join" toggle on the link
 * details surface. Only the link's creator can issue this RPC; the relay
 * rejects a non-creator caller with a Nack carrying the offending token.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutVoipWaitingRoomToggleCallLinkRequest")
public final class SmaxWaitingRoomToggleCallLinkRequest implements SmaxStanza.Request {
    /**
     * The desired waiting-room state carried by the {@code enabled} attribute.
     *
     * <p>{@code "0"} disables the gate and {@code "1"} enables it. The field is
     * modelled as a raw {@link String} for forward-compat with relay-side enum
     * extensions, even though the toggle UI derives it from a boolean.
     */
    private final String waitingRoomToggleEnabled;

    /**
     * The call-link token whose waiting-room state should be toggled, carried by
     * the {@code link-token} attribute.
     */
    private final String waitingRoomToggleLinkToken;

    /**
     * The media type the link is configured for, carried by the {@code media}
     * attribute.
     *
     * <p>Holds {@code "audio"} or {@code "video"} on the wire so the relay can
     * confirm the toggle targets the correct link.
     */
    private final String waitingRoomToggleMedia;

    /**
     * Constructs a request.
     *
     * @param waitingRoomToggleEnabled   the desired enabled state on the wire; never {@code null}
     * @param waitingRoomToggleLinkToken the call-link token; never {@code null}
     * @param waitingRoomToggleMedia     the media type; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxWaitingRoomToggleCallLinkRequest(String waitingRoomToggleEnabled,
                   String waitingRoomToggleLinkToken,
                   String waitingRoomToggleMedia) {
        this.waitingRoomToggleEnabled = Objects.requireNonNull(waitingRoomToggleEnabled, "waitingRoomToggleEnabled cannot be null");
        this.waitingRoomToggleLinkToken = Objects.requireNonNull(waitingRoomToggleLinkToken, "waitingRoomToggleLinkToken cannot be null");
        this.waitingRoomToggleMedia = Objects.requireNonNull(waitingRoomToggleMedia, "waitingRoomToggleMedia cannot be null");
    }

    /**
     * Returns the desired waiting-room state on the wire.
     *
     * @return the enabled string; never {@code null}
     */
    public String waitingRoomToggleEnabled() {
        return waitingRoomToggleEnabled;
    }

    /**
     * Returns the call-link token.
     *
     * @return the token; never {@code null}
     */
    public String waitingRoomToggleLinkToken() {
        return waitingRoomToggleLinkToken;
    }

    /**
     * Returns the media type.
     *
     * @return the media type; never {@code null}
     */
    public String waitingRoomToggleMedia() {
        return waitingRoomToggleMedia;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation wraps a {@code <waiting_room_toggle/>} child in a
     * {@code <call to="call">} envelope, stamping the {@code enabled},
     * {@code link-token}, and {@code media} attributes.
     *
     * @return a {@link StanzaBuilder} carrying the
     *         {@code <call><waiting_room_toggle/></call>} stanza
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutVoipWaitingRoomToggleCallLinkRequest",
            exports = "makeWaitingRoomToggleCallLinkRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var toggleNode = new StanzaBuilder()
                .description("waiting_room_toggle")
                .attribute("enabled", waitingRoomToggleEnabled)
                .attribute("link-token", waitingRoomToggleLinkToken)
                .attribute("media", waitingRoomToggleMedia)
                .build();
        return new StanzaBuilder()
                .description("call")
                .attribute("to", JidServer.call())
                .content(toggleNode);
    }

    /**
     * Compares this request to another object for value equality.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is a
     *         {@link SmaxWaitingRoomToggleCallLinkRequest} with equal fields,
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxWaitingRoomToggleCallLinkRequest) obj;
        return Objects.equals(this.waitingRoomToggleEnabled, that.waitingRoomToggleEnabled)
                && Objects.equals(this.waitingRoomToggleLinkToken, that.waitingRoomToggleLinkToken)
                && Objects.equals(this.waitingRoomToggleMedia, that.waitingRoomToggleMedia);
    }

    /**
     * Returns a hash code derived from every field of this request.
     *
     * @return the hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(waitingRoomToggleEnabled, waitingRoomToggleLinkToken, waitingRoomToggleMedia);
    }

    /**
     * Returns a debug string listing every field of this request.
     *
     * @return the string representation of this request
     */
    @Override
    public String toString() {
        return "SmaxWaitingRoomToggleCallLinkRequest[waitingRoomToggleEnabled=" + waitingRoomToggleEnabled
                + ", waitingRoomToggleLinkToken=" + waitingRoomToggleLinkToken
                + ", waitingRoomToggleMedia=" + waitingRoomToggleMedia + ']';
    }
}
