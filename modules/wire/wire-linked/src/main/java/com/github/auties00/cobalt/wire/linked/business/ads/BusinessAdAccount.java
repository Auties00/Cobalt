package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * The advertising account that funds a merchant's WhatsApp Business
 * advertisements.
 *
 * <p>When a merchant runs a "Click-to-WhatsApp" ad (a paid promotion that opens
 * a chat with the business when tapped), the spend is billed to an advertising
 * account. That account fixes the currency the budget is expressed in and is
 * linked to a payment method that money is drawn from. The ad-creation surface
 * shows these details so the merchant can confirm which account and currency
 * the ad will use before committing a budget.
 *
 * <p>This model is that account as the server reports it: its identifiers, its
 * display name, the currency it bills in, whether it can use the newer
 * spend-attribution reporting, and the linked {@link PaymentMethod}.
 */
@ProtobufMessage(name = "BusinessAdAccount")
public final class BusinessAdAccount {
    /**
     * Server-issued identifier of the advertising account. A numeric
     * advertising identifier, not a WhatsApp address. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Legacy numeric identifier of the same advertising account, retained for
     * compatibility with older advertising surfaces. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String legacyAccountId;

    /**
     * Currency the account bills budgets in, as a server-defined currency
     * marker. The full marker set is not recoverable from the WhatsApp client,
     * so the raw marker is exposed as a string. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String currencyCode;

    /**
     * Display name of the advertising account shown to the merchant. Empty when
     * the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String name;

    /**
     * Whether the account can use the newer spend-attribution reporting
     * surface. Reported by the server as a capability flag; {@code false} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean spendAttributionReportingEnabled;

    /**
     * Payment method money is drawn from for this account, or {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final PaymentMethod paymentMethod;

    /**
     * Constructs a new {@code BusinessAdAccount}. The reference arguments may be
     * {@code null} when the server omitted them.
     *
     * @param id                               the account identifier, or {@code null}
     * @param legacyAccountId                  the legacy account identifier, or {@code null}
     * @param currencyCode                     the billing currency marker, or {@code null}
     * @param name                             the display name, or {@code null}
     * @param spendAttributionReportingEnabled whether spend-attribution reporting is enabled
     * @param paymentMethod                    the linked payment method, or {@code null}
     */
    BusinessAdAccount(String id,
                      String legacyAccountId,
                      String currencyCode,
                      String name,
                      boolean spendAttributionReportingEnabled,
                      PaymentMethod paymentMethod) {
        this.id = id;
        this.legacyAccountId = legacyAccountId;
        this.currencyCode = currencyCode;
        this.name = name;
        this.spendAttributionReportingEnabled = spendAttributionReportingEnabled;
        this.paymentMethod = paymentMethod;
    }

    /**
     * Returns the server-issued identifier of the advertising account.
     *
     * @return the account id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the legacy numeric identifier of the advertising account.
     *
     * @return the legacy account id, or empty when the server omitted it
     */
    public Optional<String> legacyAccountId() {
        return Optional.ofNullable(legacyAccountId);
    }

    /**
     * Returns the currency the account bills budgets in.
     *
     * @return the billing currency marker, or empty when the server omitted it
     */
    public Optional<String> currencyCode() {
        return Optional.ofNullable(currencyCode);
    }

    /**
     * Returns the display name of the advertising account.
     *
     * @return the display name, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns whether the account can use the newer spend-attribution reporting
     * surface.
     *
     * @return {@code true} when the capability is granted, {@code false}
     *         otherwise
     */
    public boolean spendAttributionReportingEnabled() {
        return spendAttributionReportingEnabled;
    }

    /**
     * Returns the payment method money is drawn from for this account.
     *
     * @return the linked payment method, or empty when the server omitted it
     */
    public Optional<PaymentMethod> paymentMethod() {
        return Optional.ofNullable(paymentMethod);
    }

    /**
     * A payment instrument linked to an advertising account.
     *
     * <p>This is the source the ad spend is charged to. The server identifies
     * the instrument by an identifier and a server-defined kind marker (for
     * example a card or a prepaid balance).
     */
    @ProtobufMessage(name = "BusinessAdAccount.PaymentMethod")
    public static final class PaymentMethod {
        /**
         * Server-defined kind of the payment instrument (for example a card or
         * a prepaid balance). The full marker set is not recoverable from the
         * WhatsApp client, so the raw marker is exposed as a string. Empty when
         * the server omitted it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String kind;

        /**
         * Server-issued identifier of the payment instrument. Empty when the
         * server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String id;

        /**
         * Constructs a new {@code PaymentMethod}. The reference arguments may be
         * {@code null} when the server omitted them.
         *
         * @param kind the payment-instrument kind marker, or {@code null}
         * @param id   the payment-instrument identifier, or {@code null}
         */
        PaymentMethod(String kind, String id) {
            this.kind = kind;
            this.id = id;
        }

        /**
         * Returns the server-defined kind of the payment instrument.
         *
         * @return the kind marker, or empty when the server omitted it
         */
        public Optional<String> kind() {
            return Optional.ofNullable(kind);
        }

        /**
         * Returns the server-issued identifier of the payment instrument.
         *
         * @return the instrument id, or empty when the server omitted it
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }
    }
}
