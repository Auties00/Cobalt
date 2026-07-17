package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Snapshot of the Meta ad-platform identities currently linked to a WhatsApp
 * Business account, projected by the relay GraphQL view.
 *
 * <p>This projection carries the linked Facebook page and the linked WhatsApp
 * ad identity, each with its identifier and a click-to-WhatsApp advertising
 * state report. Both identities are optional; an absent identity means it is
 * not linked.
 *
 * <p>This is the slimmer relay-side view of the linked-accounts snapshot. The
 * stanza-based {@link BusinessLinkedAccounts} carries the broader four-identity
 * projection (with picture URLs, display names and the profile-sync toggles).
 */
@ProtobufMessage(name = "BusinessLinkedAdAccounts")
public final class BusinessLinkedAdAccounts {
    /**
     * Linked Facebook page identity, or {@code null} when the relay reported
     * no linked Facebook page.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final BusinessLinkedAdAccount facebookPage;

    /**
     * Linked WhatsApp ad identity, or {@code null} when the relay reported no
     * linked WhatsApp ad identity.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final BusinessLinkedAdAccount whatsAppAdIdentity;

    /**
     * Constructs a new {@code BusinessLinkedAdAccounts}. The reference
     * arguments may be {@code null} when the relay omitted them.
     *
     * @param facebookPage       the linked Facebook page identity, or {@code null}
     * @param whatsAppAdIdentity the linked WhatsApp ad identity, or {@code null}
     */
    BusinessLinkedAdAccounts(BusinessLinkedAdAccount facebookPage, BusinessLinkedAdAccount whatsAppAdIdentity) {
        this.facebookPage = facebookPage;
        this.whatsAppAdIdentity = whatsAppAdIdentity;
    }

    /**
     * Returns the linked Facebook page identity.
     *
     * @return the linked Facebook page, or empty when the relay reported no
     *         linked Facebook page
     */
    public Optional<BusinessLinkedAdAccount> facebookPage() {
        return Optional.ofNullable(facebookPage);
    }

    /**
     * Returns the linked WhatsApp ad identity.
     *
     * @return the linked WhatsApp ad identity, or empty when the relay reported
     *         no linked WhatsApp ad identity
     */
    public Optional<BusinessLinkedAdAccount> whatsAppAdIdentity() {
        return Optional.ofNullable(whatsAppAdIdentity);
    }
}
