package com.github.auties00.cobalt.wire.stanza.iq.status;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;

/**
 * Models the outbound legacy-IQ stanza that sets the calling user's "about" text.
 *
 * <p>The about text is the short bio shown under the contact name in the profile screen.
 * Dispatching this request as an {@link IqStanza.Request} produces a
 * {@code <iq xmlns="status" type="set">} envelope addressed to {@link JidServer#user()} and
 * wrapping a single {@code <status>} child whose content is the new about text. The relay
 * acknowledges the change with one of the {@link IqSetAboutResponse} variants.
 */
@WhatsAppWebModule(moduleName = "WAWebSetAboutJob")
public final class IqSetAboutRequest implements IqStanza.Request {
    /**
     * Holds the new about-text value routed verbatim into the {@code <status>} child content.
     *
     * <p>An empty string clears the about field. The relay enforces its own length cap and
     * rejects an over-long value with an {@link IqSetAboutResponse.ClientError}; Cobalt does
     * not pre-validate the length client-side.
     */
    private final String about;

    /**
     * Constructs a new set-about request from the given about text.
     *
     * @param about the new about text
     * @throws NullPointerException if {@code about} is {@code null}
     */
    public IqSetAboutRequest(String about) {
        this.about = Objects.requireNonNull(about, "about cannot be null");
    }

    /**
     * Returns the new about text carried by this request.
     *
     * @return the about-text string, never {@code null}
     */
    public String about() {
        return about;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code <iq xmlns="status" type="set">} envelope addressed to
     * {@link JidServer#user()} wrapping a single {@code <status>} child whose content is the
     * about text held by this request.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the {@code <status>}
     *         payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSetAboutJob",
            exports = "setAbout", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var statusNode = new StanzaBuilder()
                .description("status")
                .content(about)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "status")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(statusNode);
    }

    /**
     * Compares this request with another object for value equality on the about text.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is an {@link IqSetAboutRequest} carrying an equal
     *         about text, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSetAboutRequest) obj;
        return Objects.equals(this.about, that.about);
    }

    /**
     * Returns a hash code derived from the about text.
     *
     * @return the hash code for this request
     */
    @Override
    public int hashCode() {
        return Objects.hash(about);
    }

    /**
     * Returns a debug string rendering the about text.
     *
     * @return the string representation of this request
     */
    @Override
    public String toString() {
        return "IqSetAboutRequest[about=" + about + ']';
    }
}
