package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Parses the {@code BizOptOutJid} arm of an opt-out item's {@code biz_opt_out_ids} field.
 *
 * <p>The arm reads the required {@code biz_opt_out_jid} attribute and projects it as a business JID.
 * {@link BizOptOutId#parse(Stanza)} invokes this parser as the fall-through after
 * {@link SmaxBizOptOutBrandIdMixin#parse(Stanza)}; on a match it lifts the JID into a {@link BizOptOutId.UserJid}
 * that is addressable directly without any brand-id expansion.
 *
 * @implNote This implementation collapses the WA Web result-or-error singleton to a plain {@link Optional}:
 * empty means the required JID attribute is missing or fails user-JID validation.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsBizOptOutJidMixin")
public final class SmaxBizOptOutJidMixin {
    /**
     * Prevents instantiation of this static-only parser holder.
     *
     * @throws AssertionError on every invocation
     */
    private SmaxBizOptOutJidMixin() {
        throw new AssertionError("SmaxBizOptOutJidMixin cannot be instantiated");
    }

    /**
     * The decoded {@code BizOptOutJid} singleton wrapping the parsed business user JID.
     *
     * <p>Returned by {@link SmaxBizOptOutJidMixin#parse(Stanza)} when the {@code biz_opt_out_jid} attribute is
     * present and validates as a user JID, then lifted into the {@link BizOptOutId.UserJid} variant by
     * {@link BizOptOutId#parse(Stanza)}.
     *
     * @param bizOptOutJid the parsed business user JID; never {@code null}
     */
    public record Projection(Jid bizOptOutJid) {
        /**
         * Validates the projection payload, rejecting a missing JID.
         *
         * <p>The calling parser has already validated the server component, so this is a defensive check against
         * incorrect builder use.
         *
         * @param bizOptOutJid the business JID; never {@code null}
         * @throws NullPointerException if {@code bizOptOutJid} is {@code null}
         */
        public Projection {
            Objects.requireNonNull(bizOptOutJid, "bizOptOutJid cannot be null");
        }
    }

    /**
     * Parses an {@code <item>} stanza as a {@code BizOptOutJid} projection.
     *
     * <p>The result is empty when the required {@code biz_opt_out_jid} attribute is missing or fails
     * {@link Jid#hasUserServer()}. Callers in the {@code biz_opt_out_ids} disjunction lift the projection into the
     * {@link BizOptOutId.UserJid} arm when present.
     *
     * @implNote This implementation rejects a present-but-non-user JID so the disjunction can fail correctly
     * rather than silently accepting a malformed value.
     *
     * @param item the source {@code <item>} stanza; never {@code null}
     * @return an {@link Optional} carrying the projection, or empty when the schema does not match
     * @throws NullPointerException if {@code item} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsBizOptOutJidMixin",
            exports = "parseBizOptOutJidMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<Projection> parse(Stanza item) {
        Objects.requireNonNull(item, "item cannot be null");
        var parsed = item.getAttributeAsJid("biz_opt_out_jid").orElse(null);
        if (parsed == null || !parsed.hasUserServer()) {
            return Optional.empty();
        }
        return Optional.of(new Projection(parsed));
    }
}
