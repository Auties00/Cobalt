package com.github.auties00.cobalt.wire.linked.business.compliance;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Customer-care contact block published by a business as part of its
 * merchant-compliance disclosure.
 *
 * <p>Regulators in several regions (most prominently India) require
 * businesses that operate on WhatsApp to publish a customer-care contact
 * point that consumers can use to raise issues with the merchant directly.
 * WhatsApp surfaces this block under the merchant-info IQ and accepts an
 * email address, a landline number, and a mobile number, in international
 * format. Each of the three fields is independently optional: a business
 * may publish only the channels it actually monitors.
 */
@ProtobufMessage
public final class BusinessContactDetails {
    /**
     * Customer-care email address advertised by the business. Customers
     * can use this address to send written enquiries that are not part of
     * the WhatsApp conversation. Populated when the business has published
     * an email channel for customer support.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String email;

    /**
     * Customer-care landline phone number in international (E.164-like)
     * format. Populated when the business has published a fixed-line
     * channel for customer support.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String landlineNumber;

    /**
     * Customer-care mobile phone number in international (E.164-like)
     * format. Populated when the business has published a mobile-line
     * channel for customer support.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String mobileNumber;

    /**
     * Constructs a new {@code BusinessContactDetails} block describing a
     * customer-care contact point. Any argument may be {@code null} when
     * the business has not published the corresponding channel.
     *
     * @param email          the customer-care email address, or {@code null}
     * @param landlineNumber the customer-care landline number, or {@code null}
     * @param mobileNumber   the customer-care mobile number, or {@code null}
     */
    BusinessContactDetails(String email, String landlineNumber, String mobileNumber) {
        this.email = email;
        this.landlineNumber = landlineNumber;
        this.mobileNumber = mobileNumber;
    }

    /**
     * Returns the customer-care email address.
     *
     * @return an {@code Optional} containing the email address, or empty
     *         when the business has not published an email channel
     */
    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    /**
     * Returns the customer-care landline phone number.
     *
     * @return an {@code Optional} containing the landline number, or empty
     *         when the business has not published a landline channel
     */
    public Optional<String> landlineNumber() {
        return Optional.ofNullable(landlineNumber);
    }

    /**
     * Returns the customer-care mobile phone number.
     *
     * @return an {@code Optional} containing the mobile number, or empty
     *         when the business has not published a mobile channel
     */
    public Optional<String> mobileNumber() {
        return Optional.ofNullable(mobileNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessContactDetails) obj;
        return Objects.equals(this.email, that.email) &&
               Objects.equals(this.landlineNumber, that.landlineNumber) &&
               Objects.equals(this.mobileNumber, that.mobileNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, landlineNumber, mobileNumber);
    }

    @Override
    public String toString() {
        return "BusinessContactDetails[" +
               "email=" + email + ", " +
               "landlineNumber=" + landlineNumber + ", " +
               "mobileNumber=" + mobileNumber + ']';
    }
}
