package com.github.auties00.cobalt.wire.stanza.smax.pushconfig;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;

/**
 * Represents the outbound {@code <iq xmlns="urn:xmpp:whatsapp:push" type="set">} stanza that
 * registers or clears a push-notification channel.
 *
 * <p>The request carries a single {@link SmaxPushConfigSetSetVariant}: a
 * {@link SmaxPushConfigSetSetVariant.Config} payload to register a platform-specific push channel
 * (W3C Push API endpoint for web, APNs for iOS, FCM-style for Android, WNS for Windows, and so on)
 * or a {@link SmaxPushConfigSetSetVariant.Clear} payload to de-register. Callers wiring server-side
 * push to their own application build this stanza after obtaining a push subscription and dispatch
 * it through the SMAX layer; the relay replies with a {@link SmaxPushConfigSetResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutPushConfigSetRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutPushConfigBaseIQSetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutPushConfigSetSetConfigOrSetClearMixinGroup")
public final class SmaxPushConfigSetRequest implements SmaxStanza.Request {
    /**
     * Holds the exclusive payload variant, either {@code <config>} or {@code <clear>}.
     */
    private final SmaxPushConfigSetSetVariant variant;

    /**
     * Constructs a push-config request around the given payload variant.
     *
     * <p>Pass a {@link SmaxPushConfigSetSetVariant.Config} to register a platform-specific push
     * channel or a {@link SmaxPushConfigSetSetVariant.Clear} to drop the registration.
     *
     * @param variant the payload variant
     * @throws NullPointerException if {@code variant} is {@code null}
     */
    public SmaxPushConfigSetRequest(SmaxPushConfigSetSetVariant variant) {
        this.variant = Objects.requireNonNull(variant, "variant cannot be null");
    }

    /**
     * Returns the payload variant carried by this request.
     *
     * @return the {@link SmaxPushConfigSetSetVariant}
     */
    public SmaxPushConfigSetSetVariant variant() {
        return variant;
    }

    /**
     * Builds the {@code <iq>} envelope and nests the payload variant as its sole child.
     *
     * <p>The envelope is addressed to the user server with {@code xmlns="urn:xmpp:whatsapp:push"}
     * and {@code type="set"}; the payload is produced by {@link SmaxPushConfigSetSetVariant#toStanza()}.
     *
     * @implNote This implementation addresses the stanza to {@link Jid#userServer()} rather than a
     * literal {@code s.whatsapp.net} string so the destination tracks the shared server constant.
     * @return the {@link StanzaBuilder} for the request envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutPushConfigSetRequest",
            exports = "makeSetRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "urn:xmpp:whatsapp:push")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .content(variant.toStanza());
    }

    /**
     * Compares this request to another object for equality on the carried variant.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a {@link SmaxPushConfigSetRequest} with an equal
     *         variant
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxPushConfigSetRequest) obj;
        return Objects.equals(this.variant, that.variant);
    }

    /**
     * Returns a hash code derived from the carried variant.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(variant);
    }

    /**
     * Returns a debug rendering of this request.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxPushConfigSetRequest[variant=" + variant + ']';
    }
}
