package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the outbound {@code <iq xmlns="blocklist" type="set">} stanza that applies a block or unblock action to a
 * user JID.
 *
 * <p>This request drives the chat-info Block / Unblock action and the report-and-block flow. The {@link #action()}
 * selects block versus unblock, {@link #itemJid()} names the target user, and {@link #itemDhash()} carries the
 * optional cached blocklist digest used by the relay for its cache-match shortcut. The optional
 * {@link #entryPointSource()} marks a block originating from "report and block", and the optional
 * {@link #bizOptOut()} payload attaches a marketing-message opt-out reason to the block.
 *
 * @implNote This implementation collapses WA Web's per-action and migrated-versus-non-migrated mixin-group
 * dispatchers into a single field: {@link SmaxUpdateBlockListAction#wire()} is the per-action selector, and the
 * migrated-versus-non-migrated dispatcher reduces to the non-migrated branch since Cobalt does not yet generate the
 * migrated branch's {@code blocklist_ids} payload. The {@code id} attribute is generated downstream by the central
 * client dispatcher.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBlocklistsUpdateBlockListRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutBlocklistsUpdateBlockListBlockOrUpdateBlockListUnblockItemMixinGroup")
@WhatsAppWebModule(moduleName = "WASmaxOutBlocklistsUpdateBlockListBlockItemMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBlocklistsUpdateBlockListUnblockItemMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBlocklistsUpdateBlockListOrUpdateBlockListNonMigratedBlockItemMixinGroup")
@WhatsAppWebModule(moduleName = "WASmaxOutBlocklistsUpdateBlockListNonMigratedBlockItemMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBlocklistsUpdateBlockListReportBlockEntryPointMixin")
public final class SmaxUpdateBlockListRequest implements SmaxStanza.Request {
    /**
     * The action to apply to {@link #itemJid}.
     */
    private final SmaxUpdateBlockListAction action;

    /**
     * The target user JID being blocked or unblocked.
     */
    private final Jid itemJid;

    /**
     * The cached digest of the local blocklist or {@code null} to skip the cache-match shortcut.
     */
    private final String itemDhash;

    /**
     * The optional report-block entry-point source when the action originates from "report and block".
     */
    private final String entryPointSource;

    /**
     * The optional marketing-message opt-out payload attached to the block action.
     */
    private final BizOptOut bizOptOut;

    /**
     * Constructs a block/unblock request with every optional field.
     *
     * <p>The {@code entryPointSource} and {@code bizOptOut} payload are only meaningful for block actions
     * originating from the report-and-block or marketing-messages opt-out flows; the vanilla block/unblock case
     * passes {@code null} for both, which makes {@link #toStanza()} omit the corresponding children.
     *
     * @param action           the block-or-unblock action; never {@code null}
     * @param itemJid          the target user JID; never {@code null}
     * @param itemDhash        the cached digest; may be {@code null}
     * @param entryPointSource the report entry-point source; may be {@code null}
     * @param bizOptOut        the optional marketing-messages opt-out payload; may be {@code null}
     * @throws NullPointerException if {@code action} or {@code itemJid} is {@code null}
     */
    public SmaxUpdateBlockListRequest(SmaxUpdateBlockListAction action, Jid itemJid, String itemDhash, String entryPointSource, BizOptOut bizOptOut) {
        this.action = Objects.requireNonNull(action, "action cannot be null");
        this.itemJid = Objects.requireNonNull(itemJid, "itemJid cannot be null");
        this.itemDhash = itemDhash;
        this.entryPointSource = entryPointSource;
        this.bizOptOut = bizOptOut;
    }

    /**
     * Constructs a block/unblock request without a marketing-messages opt-out payload.
     *
     * <p>This is the convenience overload for the dominant call-pattern where no {@code <biz_opt_out>} child is
     * attached; the underlying {@link #bizOptOut} field defaults to {@code null}.
     *
     * @param action           the block-or-unblock action; never {@code null}
     * @param itemJid          the target user JID; never {@code null}
     * @param itemDhash        the cached digest; may be {@code null}
     * @param entryPointSource the report entry-point source; may be {@code null}
     * @throws NullPointerException if {@code action} or {@code itemJid} is {@code null}
     */
    public SmaxUpdateBlockListRequest(SmaxUpdateBlockListAction action, Jid itemJid, String itemDhash, String entryPointSource) {
        this(action, itemJid, itemDhash, entryPointSource, null);
    }

    /**
     * Returns the block-or-unblock action.
     *
     * @return the action; never {@code null}
     */
    public SmaxUpdateBlockListAction action() {
        return action;
    }

    /**
     * Returns the target user JID.
     *
     * @return the JID; never {@code null}
     */
    public Jid itemJid() {
        return itemJid;
    }

    /**
     * Returns the cached digest when set.
     *
     * <p>Absence means the request carries no cache-match shortcut, so the relay returns a full mismatch reply
     * rather than a cache-match confirmation.
     *
     * @return an {@link Optional} carrying the digest, or empty when no cached digest was supplied
     */
    public Optional<String> itemDhash() {
        return Optional.ofNullable(itemDhash);
    }

    /**
     * Returns the report entry-point source when set.
     *
     * @return an {@link Optional} carrying the entry-point source, or empty when the action did not originate
     *         from report-and-block
     */
    public Optional<String> entryPointSource() {
        return Optional.ofNullable(entryPointSource);
    }

    /**
     * Returns the marketing-messages opt-out payload when set.
     *
     * @return an {@link Optional} carrying the payload, or empty when no payload was attached
     */
    public Optional<BizOptOut> bizOptOut() {
        return Optional.ofNullable(bizOptOut);
    }

    /**
     * Builds the outbound {@code <iq>} stanza ready for dispatch.
     *
     * <p>The returned {@link StanzaBuilder} addresses {@link JidServer#user()} with {@code xmlns="blocklist"} and
     * {@code type="set"}; the {@code id} attribute is filled in downstream by the central client dispatcher. The
     * {@code <item>} child always carries the action and target JID; the {@code dhash} attribute, the
     * {@code <biz_opt_out>} child, and the {@code <entry_point>} child are added only when their respective fields
     * are set.
     *
     * @implNote This implementation collapses WA Web's
     * {@code mergeUpdateBlockListBlockOrUpdateBlockListUnblockItemMixinGroup} dispatcher (block versus unblock) and
     * {@code mergeUpdateBlockListOrUpdateBlockListNonMigratedBlockItemMixinGroup} dispatcher (migrated versus
     * non-migrated) into a single emission path; the {@code SmaxMixinGroupExhaustiveError} throw becomes
     * unreachable because the enum closes the variant set, and the migrated branch's {@code blocklist_ids} payload
     * is not generated because no Cobalt consumer needs it yet.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsUpdateBlockListRequest",
            exports = "makeUpdateBlockListRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsUpdateBlockListBlockOrUpdateBlockListUnblockItemMixinGroup",
            exports = "mergeUpdateBlockListBlockOrUpdateBlockListUnblockItemMixinGroup",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsUpdateBlockListBlockItemMixin",
            exports = "mergeUpdateBlockListBlockItemMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsUpdateBlockListUnblockItemMixin",
            exports = "mergeUpdateBlockListUnblockItemMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsUpdateBlockListOrUpdateBlockListNonMigratedBlockItemMixinGroup",
            exports = "mergeUpdateBlockListOrUpdateBlockListNonMigratedBlockItemMixinGroup",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsUpdateBlockListNonMigratedBlockItemMixin",
            exports = "mergeUpdateBlockListNonMigratedBlockItemMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsUpdateBlockListReportBlockEntryPointMixin",
            exports = "mergeUpdateBlockListReportBlockEntryPointMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var itemBuilder = new StanzaBuilder()
                .description("item")
                .attribute("action", action.wire())
                .attribute("jid", itemJid);
        if (itemDhash != null) {
            itemBuilder.attribute("dhash", itemDhash);
        }
        if (bizOptOut != null) {
            itemBuilder.content(bizOptOut.toStanza());
        }
        var iqBuilder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "blocklist")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(itemBuilder.build());
        if (entryPointSource != null) {
            var entryPointNode = new StanzaBuilder()
                    .description("entry_point")
                    .attribute("source", entryPointSource)
                    .build();
            iqBuilder.content(entryPointNode);
        }
        return iqBuilder;
    }

    /**
     * Compares this request to another object for value equality across every field.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxUpdateBlockListRequest} with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxUpdateBlockListRequest) obj;
        return this.action == that.action
                && Objects.equals(this.itemJid, that.itemJid)
                && Objects.equals(this.itemDhash, that.itemDhash)
                && Objects.equals(this.entryPointSource, that.entryPointSource)
                && Objects.equals(this.bizOptOut, that.bizOptOut);
    }

    /**
     * Returns a hash code derived from every field.
     *
     * @return the hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(action, itemJid, itemDhash, entryPointSource, bizOptOut);
    }

    /**
     * Returns a debug rendering listing every field.
     *
     * @return a diagnostic string; never {@code null}
     */
    @Override
    public String toString() {
        return "SmaxUpdateBlockListRequest[action=" + action
                + ", itemJid=" + itemJid
                + ", itemDhash=" + itemDhash
                + ", entryPointSource=" + entryPointSource
                + ", bizOptOut=" + bizOptOut + ']';
    }

    /**
     * The {@code <biz_opt_out>} child attached to a block request when the action records a marketing-message
     * opt-out reason.
     *
     * <p>This payload is produced when a block originates from a marketing-message complaint flow. The seven
     * attributes are independently optional and serialise as bare attributes on the {@code <biz_opt_out/>} child of
     * {@code <item>}; an attribute is emitted only when its component is non-{@code null}.
     *
     * @param reason                      the optional reason marker; may be {@code null}
     * @param reasonDescription           the optional free-form reason description; may be {@code null}
     * @param entryPoint                  the optional entry-point marker; may be {@code null}
     * @param firstMessage                the optional first-message hint; may be {@code null}
     * @param businessDiscoveryEntryPoint the optional discovery entry-point marker; may be {@code null}
     * @param businessDiscoveryTimestamp  the optional discovery timestamp in seconds; may be {@code null}
     * @param businessDiscoveryId         the optional discovery id; may be {@code null}
     */
    public record BizOptOut(String reason, String reasonDescription, String entryPoint,
                            String firstMessage, String businessDiscoveryEntryPoint,
                            Long businessDiscoveryTimestamp, String businessDiscoveryId) {
        /**
         * Builds the {@code <biz_opt_out>} stanza for this payload.
         *
         * <p>The resulting stanza is attached as a child of the {@code <item>} element by
         * {@link SmaxUpdateBlockListRequest#toStanza()}. Each attribute is emitted only when its component is
         * non-{@code null}.
         *
         * @implNote This implementation mirrors WA Web's {@code WASmaxAttrs.OPTIONAL} contract by emitting an
         * attribute only when its component is non-{@code null}, since {@code null} attributes would collapse to
         * {@code DROP_ATTR} on the wire anyway.
         *
         * @return the built {@link Stanza}; never {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxOutBlocklistsUpdateBlockListRequest",
                exports = "makeUpdateBlockListRequestItemBizOptOut", adaptation = WhatsAppAdaptation.DIRECT)
        public Stanza toStanza() {
            var builder = new StanzaBuilder()
                    .description("biz_opt_out");
            if (reason != null) {
                builder.attribute("reason", reason);
            }
            if (reasonDescription != null) {
                builder.attribute("reason_description", reasonDescription);
            }
            if (entryPoint != null) {
                builder.attribute("entry_point", entryPoint);
            }
            if (firstMessage != null) {
                builder.attribute("first_message", firstMessage);
            }
            if (businessDiscoveryEntryPoint != null) {
                builder.attribute("business_discovery_entry_point", businessDiscoveryEntryPoint);
            }
            if (businessDiscoveryTimestamp != null) {
                builder.attribute("business_discovery_timestamp", businessDiscoveryTimestamp);
            }
            if (businessDiscoveryId != null) {
                builder.attribute("business_discovery_id", businessDiscoveryId);
            }
            return builder.build();
        }

        /**
         * Returns the reason marker when present.
         *
         * @return an {@link Optional} carrying the reason, or empty when the field is unset
         */
        public Optional<String> reasonAsOptional() {
            return Optional.ofNullable(reason);
        }

        /**
         * Returns the reason description when present.
         *
         * @return an {@link Optional} carrying the description, or empty when the field is unset
         */
        public Optional<String> reasonDescriptionAsOptional() {
            return Optional.ofNullable(reasonDescription);
        }

        /**
         * Returns the entry-point marker when present.
         *
         * @return an {@link Optional} carrying the entry-point, or empty when the field is unset
         */
        public Optional<String> entryPointAsOptional() {
            return Optional.ofNullable(entryPoint);
        }

        /**
         * Returns the first-message hint when present.
         *
         * @return an {@link Optional} carrying the hint, or empty when the field is unset
         */
        public Optional<String> firstMessageAsOptional() {
            return Optional.ofNullable(firstMessage);
        }

        /**
         * Returns the business-discovery entry-point when present.
         *
         * @return an {@link Optional} carrying the entry-point, or empty when the field is unset
         */
        public Optional<String> businessDiscoveryEntryPointAsOptional() {
            return Optional.ofNullable(businessDiscoveryEntryPoint);
        }

        /**
         * Returns the business-discovery timestamp when present.
         *
         * @return an {@link Optional} carrying the timestamp in seconds, or empty when the field is unset
         */
        public Optional<Long> businessDiscoveryTimestampAsOptional() {
            return Optional.ofNullable(businessDiscoveryTimestamp);
        }

        /**
         * Returns the business-discovery id when present.
         *
         * @return an {@link Optional} carrying the id, or empty when the field is unset
         */
        public Optional<String> businessDiscoveryIdAsOptional() {
            return Optional.ofNullable(businessDiscoveryId);
        }
    }
}
