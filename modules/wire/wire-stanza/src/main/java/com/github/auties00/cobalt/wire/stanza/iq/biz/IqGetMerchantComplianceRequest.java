package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Models the typed outbound {@code <iq xmlns="w:biz:merchant_info" type="get">} stanza that requests the regulatory-compliance bundle for one or more merchants.
 *
 * <p>The India e-commerce surfaces (compliance entry-point banner, post-thread legal-entity row,
 * grievance officer details) use this request to fetch the merchant's compliance bundle for
 * rendering. One stanza can carry multiple merchant JIDs so a list view fetches all entries at
 * once. The matching {@link IqGetMerchantComplianceResponse} surfaces the entity name, the entity
 * type, the registered flag, the customer-care contact triple and the grievance-officer block per
 * merchant.
 *
 * @implNote
 * This implementation targets the deprecated WAP path of {@code WAWebMerchantComplianceJob.getMerchantCompliance};
 * WA Web routes through {@code WAWebBizGetMerchantCompliance} first when {@code graphQLForGetComplianceInfo}
 * is set and only falls back to this stanza shape when the GraphQL path is disabled.
 */
@WhatsAppWebModule(moduleName = "WAWebMerchantComplianceJob")
public final class IqGetMerchantComplianceRequest implements IqStanza.Request {
    /**
     * Holds the merchant JIDs whose compliance bundles are being queried.
     *
     * <p>Each entry is emitted as the {@code jid} attribute of one {@code <merchant_info/>} child.
     */
    private final List<Jid> businessJids;

    /**
     * Constructs a typed request over the given merchant JIDs.
     *
     * <p>The list must contain at least one entry because the relay rejects an empty fan-out.
     *
     * @param businessJids the merchant JIDs; never {@code null} and must be non-empty
     * @throws NullPointerException     if {@code businessJids} is {@code null}
     * @throws IllegalArgumentException if {@code businessJids} is empty
     */
    public IqGetMerchantComplianceRequest(List<Jid> businessJids) {
        Objects.requireNonNull(businessJids, "businessJids cannot be null");
        if (businessJids.isEmpty()) {
            throw new IllegalArgumentException("businessJids cannot be empty");
        }
        this.businessJids = List.copyOf(businessJids);
    }

    /**
     * Returns the queried merchant JIDs.
     *
     * <p>The list preserves the caller-supplied order, which the fan-out mirrors on the wire.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<Jid> businessJids() {
        return businessJids;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation materialises the WAP envelope produced by the legacy fallback branch of
     * {@code WAWebMerchantComplianceJob.getMerchantCompliance}: one {@code <merchant_info jid/>}
     * child per queried JID, wrapped in an {@code <iq xmlns="w:biz:merchant_info" type="get"/>}
     * envelope routed to the WhatsApp service via {@link JidServer#user()}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebMerchantComplianceJob",
            exports = "getMerchantCompliance", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>();
        for (var jid : businessJids) {
            children.add(new StanzaBuilder()
                    .description("merchant_info")
                    .attribute("jid", jid)
                    .build());
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz:merchant_info")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .content(children);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqGetMerchantComplianceRequest) obj;
        return Objects.equals(this.businessJids, that.businessJids);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessJids);
    }

    @Override
    public String toString() {
        return "IqGetMerchantComplianceRequest[businessJids=" + businessJids + ']';
    }
}
