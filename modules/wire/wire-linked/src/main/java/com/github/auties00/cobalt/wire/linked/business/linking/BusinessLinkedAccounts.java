package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Snapshot of the external Meta-platform identities currently linked to
 * a WhatsApp Business account.
 *
 * <p>A WhatsApp Business account can be tied to four optional external
 * identities, all surfaced through the same Linked Accounts settings
 * panel:
 *
 * <ul>
 *   <li>a {@linkplain #facebookPage() Facebook Page} that the business
 *       publishes content from;</li>
 *   <li>a {@linkplain #facebookBusiness() Facebook Business Manager
 *       account} that owns the page and the catalog;</li>
 *   <li>an {@linkplain #instagramProfessional() Instagram Professional
 *       account} that mirrors the page's publishing surface; and</li>
 *   <li>a {@linkplain #whatsAppAdIdentity() WhatsApp Ad-account identity}
 *       that authorises Click-to-WhatsApp campaigns.</li>
 * </ul>
 *
 * <p>The relay only returns the identities the caller is currently
 * linked to: every accessor on this class is therefore an
 * {@link Optional} that resolves to the populated projection or to
 * empty when that identity is not linked.
 */
@ProtobufMessage(name = "BusinessLinkedAccounts")
public final class BusinessLinkedAccounts {
    /**
     * Optional linked Facebook Page projection.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    BusinessLinkedFacebookPage facebookPage;

    /**
     * Optional linked Facebook Business projection.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    BusinessLinkedFacebookBusiness facebookBusiness;

    /**
     * Optional linked Instagram Professional projection.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    BusinessLinkedInstagramProfessional instagramProfessional;

    /**
     * Optional linked WhatsApp Ad-account identity projection.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    BusinessLinkedWhatsAppAdIdentity whatsAppAdIdentity;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param facebookPage          the optional Facebook Page projection
     * @param facebookBusiness      the optional Facebook Business
     *                              projection
     * @param instagramProfessional the optional Instagram Professional
     *                              projection
     * @param whatsAppAdIdentity    the optional WhatsApp Ad-account
     *                              identity projection
     */
    BusinessLinkedAccounts(BusinessLinkedFacebookPage facebookPage,
                           BusinessLinkedFacebookBusiness facebookBusiness,
                           BusinessLinkedInstagramProfessional instagramProfessional,
                           BusinessLinkedWhatsAppAdIdentity whatsAppAdIdentity) {
        this.facebookPage = facebookPage;
        this.facebookBusiness = facebookBusiness;
        this.instagramProfessional = instagramProfessional;
        this.whatsAppAdIdentity = whatsAppAdIdentity;
    }

    /**
     * Returns the optional linked Facebook Page projection.
     *
     * @return an {@link Optional} carrying the projection, or empty
     *         when the relay reported no linked Facebook Page
     */
    public Optional<BusinessLinkedFacebookPage> facebookPage() {
        return Optional.ofNullable(facebookPage);
    }

    /**
     * Returns the optional linked Facebook Business projection.
     *
     * @return an {@link Optional} carrying the projection, or empty
     *         when the relay reported no linked Facebook Business
     */
    public Optional<BusinessLinkedFacebookBusiness> facebookBusiness() {
        return Optional.ofNullable(facebookBusiness);
    }

    /**
     * Returns the optional linked Instagram Professional projection.
     *
     * @return an {@link Optional} carrying the projection, or empty
     *         when the relay reported no linked Instagram Professional
     *         account
     */
    public Optional<BusinessLinkedInstagramProfessional> instagramProfessional() {
        return Optional.ofNullable(instagramProfessional);
    }

    /**
     * Returns the optional linked WhatsApp Ad-account identity
     * projection.
     *
     * @return an {@link Optional} carrying the projection, or empty
     *         when the relay reported no linked ad identity
     */
    public Optional<BusinessLinkedWhatsAppAdIdentity> whatsAppAdIdentity() {
        return Optional.ofNullable(whatsAppAdIdentity);
    }
}
