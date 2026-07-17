package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Parses the {@code BizOptOutBrandID} arm of an opt-out item's {@code biz_opt_out_ids} field.
 *
 * <p>The arm reads the {@code biz_opt_out_brand_id} string identifying a marketing brand together with the
 * optional {@code biz_jid} echo of the business JID paired with that brand. {@link BizOptOutId#parse(Stanza)}
 * invokes this parser first; on a match it lifts the resulting pair into a {@link BizOptOutId.BrandId}.
 *
 * @implNote This implementation collapses the WA Web result-or-error envelope to a plain {@link Optional}: an
 * empty value covers both the required-attribute-missing case and the optional-jid-fails-user-validation case,
 * since no caller introspects the failure shape further.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsBizOptOutBrandIDMixin")
public final class SmaxBizOptOutBrandIdMixin {
    /**
     * Prevents instantiation of this static-only parser holder.
     *
     * @throws AssertionError on every invocation
     */
    private SmaxBizOptOutBrandIdMixin() {
        throw new AssertionError("SmaxBizOptOutBrandIdMixin cannot be instantiated");
    }

    /**
     * The decoded {@code BizOptOutBrandID} pair of brand id and optional business JID.
     *
     * <p>Returned by {@link SmaxBizOptOutBrandIdMixin#parse(Stanza)} when both attributes parse cleanly and lifted
     * into the {@link BizOptOutId.BrandId} variant by {@link BizOptOutId#parse(Stanza)}.
     *
     * @param bizOptOutBrandId the marketing-brand identifier; never {@code null}
     * @param bizJid           the optional paired business JID; may be {@code null}
     */
    public record Projection(String bizOptOutBrandId, Jid bizJid) {
        /**
         * Validates the projection payload, rejecting a missing brand id.
         *
         * @param bizOptOutBrandId the brand identifier; never {@code null}
         * @param bizJid           the optional business JID; may be {@code null}
         * @throws NullPointerException if {@code bizOptOutBrandId} is {@code null}
         */
        public Projection {
            Objects.requireNonNull(bizOptOutBrandId, "bizOptOutBrandId cannot be null");
        }

        /**
         * Returns the business JID when present.
         *
         * <p>Detects whether the relay echoed a business JID alongside the brand id; consumers without an
         * {@link Optional} preference may read the raw field via the record accessor instead.
         *
         * @return an {@link Optional} carrying the JID, or empty when the relay omitted {@code biz_jid}
         */
        public Optional<Jid> bizJidAsOptional() {
            return Optional.ofNullable(bizJid);
        }
    }

    /**
     * Parses an {@code <item>} stanza as a {@code BizOptOutBrandID} projection.
     *
     * <p>The result is empty when the required {@code biz_opt_out_brand_id} attribute is missing or when an
     * optional {@code biz_jid} is present but is not a user JID (no user server, as enforced by
     * {@link Jid#hasUserServer()}). Callers in the {@code biz_opt_out_ids} disjunction fall through to
     * {@link SmaxBizOptOutJidMixin#parse(Stanza)} on empty.
     *
     * @implNote This implementation rejects the whole projection when a present {@code biz_jid} fails user-JID
     * validation rather than degrading silently, which is load-bearing for the {@link BizOptOutId#parse(Stanza)}
     * priority chain.
     *
     * @param item the source {@code <item>} stanza; never {@code null}
     * @return an {@link Optional} carrying the projection, or empty when the schema does not match
     * @throws NullPointerException if {@code item} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsBizOptOutBrandIDMixin",
            exports = "parseBizOptOutBrandIDMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<Projection> parse(Stanza item) {
        Objects.requireNonNull(item, "item cannot be null");
        var brandId = item.getAttributeAsString("biz_opt_out_brand_id").orElse(null);
        if (brandId == null) {
            return Optional.empty();
        }
        Jid bizJid = null;
        if (item.hasAttribute("biz_jid")) {
            var parsed = item.getAttributeAsJid("biz_jid").orElse(null);
            if (parsed == null || !parsed.hasUserServer()) {
                return Optional.empty();
            }
            bizJid = parsed;
        }
        return Optional.of(new Projection(brandId, bizJid));
    }
}
