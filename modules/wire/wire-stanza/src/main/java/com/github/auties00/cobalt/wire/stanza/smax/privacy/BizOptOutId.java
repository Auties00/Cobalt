package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Discriminates an opt-out list entry by either a marketing brand id or a business user JID.
 *
 * <p>The {@link BrandId} arm carries a marketing-brand id that must be expanded into the full set of business
 * numbers before the business can be contacted. The {@link UserJid} arm carries a concrete business JID that is
 * rendered directly without any further resolution. Each entry of {@link SmaxGetOptOutListResponse.Item} exposes
 * one of these arms through {@link SmaxGetOptOutListResponse.Item#bizOptOutIds()}.
 *
 * @implNote This implementation collapses the wire tagged-union (a {@code name} discriminator of
 * {@code "BizOptOutBrandID"} or {@code "BizOptOutJid"}) into a sealed interface whose variant type is the
 * discriminator, so pattern matching on {@link BrandId} versus {@link UserJid} replaces a name comparison.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBlocklistsBizOptOutIds")
public sealed interface BizOptOutId permits BizOptOutId.BrandId, BizOptOutId.UserJid {
    /**
     * The {@code BizOptOutBrandID} arm of the disjunction, carrying a marketing-brand id with an optional business JID.
     *
     * <p>The brand ids are collected and resolved to business phone numbers and LIDs in a single batch; the
     * optional {@link #bizJid()} is the business JID echoed by the relay when it already knows the pairing.
     *
     * @param bizOptOutBrandId the marketing brand identifier; never {@code null}
     * @param bizJid           the optional business user JID echoed by the relay; may be {@code null}
     */
    record BrandId(String bizOptOutBrandId, Jid bizJid) implements BizOptOutId {
        /**
         * Validates the {@link BrandId} payload, rejecting a missing brand id.
         *
         * @param bizOptOutBrandId the brand id; never {@code null}
         * @param bizJid           the optional business JID; may be {@code null}
         * @throws NullPointerException if {@code bizOptOutBrandId} is {@code null}
         */
        public BrandId {
            Objects.requireNonNull(bizOptOutBrandId, "bizOptOutBrandId cannot be null");
        }

        /**
         * Returns the business JID when present.
         *
         * <p>Absence means the relay did not pair the brand id with a concrete business JID, so the caller must
         * run brand-id expansion before contacting the business.
         *
         * @return an {@link Optional} carrying the JID, or empty when the relay omitted {@code biz_jid}
         */
        public Optional<Jid> bizJidAsOptional() {
            return Optional.ofNullable(bizJid);
        }
    }

    /**
     * The {@code BizOptOutJid} arm of the disjunction, carrying a concrete business user JID.
     *
     * <p>The JID is addressable as-is, so consumers render it without any brand-id lookup.
     *
     * @param bizOptOutJid the business user JID; never {@code null}
     */
    record UserJid(Jid bizOptOutJid) implements BizOptOutId {
        /**
         * Validates the {@link UserJid} payload, rejecting a missing JID.
         *
         * @param bizOptOutJid the business user JID; never {@code null}
         * @throws NullPointerException if {@code bizOptOutJid} is {@code null}
         */
        public UserJid {
            Objects.requireNonNull(bizOptOutJid, "bizOptOutJid cannot be null");
        }
    }

    /**
     * Projects an {@code <item>} stanza onto a {@link BizOptOutId} variant.
     *
     * <p>The {@link BrandId} arm is tried first and the {@link UserJid} arm second. An empty result signals that
     * neither arm matched; callers then reject the enclosing item, preserving the fail-the-parent semantics of the
     * disjunction.
     *
     * @implNote This implementation tries {@link SmaxBizOptOutBrandIdMixin#parse(Stanza)} before
     * {@link SmaxBizOptOutJidMixin#parse(Stanza)} and collapses the no-match envelope to an empty {@link Optional},
     * since no caller introspects the disjunction-failure detail.
     *
     * @param item the source {@code <item>} stanza; never {@code null}
     * @return an {@link Optional} carrying the projected variant, or empty when neither arm parses
     * @throws NullPointerException if {@code item} is {@code null}
     * @see SmaxBizOptOutBrandIdMixin#parse(Stanza)
     * @see SmaxBizOptOutJidMixin#parse(Stanza)
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBlocklistsBizOptOutIds",
            exports = "parseBizOptOutIds", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<BizOptOutId> parse(Stanza item) {
        Objects.requireNonNull(item, "item cannot be null");
        var brand = SmaxBizOptOutBrandIdMixin.parse(item);
        if (brand.isPresent()) {
            var p = brand.get();
            return Optional.of(new BrandId(p.bizOptOutBrandId(), p.bizJid()));
        }
        var jid = SmaxBizOptOutJidMixin.parse(item);
        if (jid.isPresent()) {
            return Optional.of(new UserJid(jid.get().bizOptOutJid()));
        }
        return Optional.empty();
    }
}
