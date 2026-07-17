package com.github.auties00.cobalt.wire.stanza.iq.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;

/**
 * Models the outbound legacy IQ stanza that toggles the user's read-receipts visibility.
 *
 * <p>The serialised stanza is
 * {@code <iq xmlns="privacy" type="set"><privacy><category name="readreceipts" value="all|none"/></privacy></iq>}.
 * It is the single-row dedicated counterpart of the multi-row {@link IqSetPrivacyRequest} and
 * always targets the {@link IqQueryPrivacySettingsCategoryName#READ_RECEIPTS} category. Disabling
 * read receipts also suppresses outbound delivery notifications for the user, because the relay
 * drops receipts on send when the value is {@code "none"}.
 *
 * @implNote
 * This implementation always emits the {@code readreceipts} category and toggles only between
 * {@code "all"} and {@code "none"}; no other category is reachable through this stanza.
 *
 * @deprecated redundant with the multi-row privacy path: {@code editPrivacySetting} writes the same
 * {@code <category name="readreceipts" value="all|none"/>} via {@link IqSetPrivacyRequest}, verified
 * byte-identical to WhatsApp Web's {@code WAWebSetReadReceiptJob}. Prefer that path; this single-row
 * variant is retained only for source-mapping completeness.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WAWebSetReadReceiptJob")
public final class IqSetReadReceiptRequest implements IqStanza.Request {
    /**
     * Holds the new toggle state.
     *
     * <p>A value of {@code true} serialises to the wire value {@code "all"} and {@code false} to
     * {@code "none"}.
     */
    private final boolean enabled;

    /**
     * Constructs a new request for the given toggle state.
     *
     * <p>Pass {@code true} to enable read receipts, so the relay accepts and replays them to
     * peers, or {@code false} to disable them, so the relay drops outbound receipts and the peer's
     * UI no longer shows the double-tick.
     *
     * @param enabled {@code true} to enable read receipts, {@code false} to disable
     */
    public IqSetReadReceiptRequest(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the requested toggle state.
     *
     * @return {@code true} when read receipts are being enabled, {@code false} when disabled
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wraps a single {@code <category name="readreceipts" value="all|none"/>} marker in a
     * {@code <privacy>} envelope inside the canonical
     * {@code <iq xmlns="privacy" to="s.whatsapp.net" type="set">} stanza. The {@code value}
     * attribute is selected by {@link #enabled} as {@code "all"} when enabled and {@code "none"}
     * when disabled.
     *
     * @return the outbound stanza builder, never {@code null}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSetReadReceiptJob",
            exports = "default", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var value = enabled ? "all" : "none";
        var categoryNode = new StanzaBuilder()
                .description("category")
                .attribute("name", "readreceipts")
                .attribute("value", value)
                .build();
        var privacyNode = new StanzaBuilder()
                .description("privacy")
                .content(categoryNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "privacy")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(privacyNode);
    }

    /**
     * Compares this request to another object for equality by toggle state.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an {@link IqSetReadReceiptRequest} with the same
     *         {@link #enabled()} state
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSetReadReceiptRequest) obj;
        return this.enabled == that.enabled;
    }

    /**
     * Returns a hash code derived from the toggle state.
     *
     * <p>The result is consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this request
     */
    @Override
    public int hashCode() {
        return Objects.hash(enabled);
    }

    /**
     * Returns a debug-only string representation of this request.
     *
     * <p>The format is not stable and must not be parsed.
     *
     * @return a debug string describing the toggle state
     */
    @Override
    public String toString() {
        return "IqSetReadReceiptRequest[enabled=" + enabled + ']';
    }
}
