package com.github.auties00.cobalt.wire.stanza.iq.biz;

import java.util.Objects;
import java.util.Optional;

/**
 * Carries a customer-care or grievance-officer contact triple inside an
 * {@link IqSetMerchantComplianceRequest}.
 *
 * <p>The triple sets or updates the contact email, landline and mobile of the merchant's
 * customer-care surface, part of the regulatory-compliance disclosure required in India and other
 * markets. Every field is independently optional and an absent field is dropped from the wire stanza
 * rather than zeroed out.
 */
public final class IqSetMerchantComplianceContactDetails {
    /**
     * Holds the optional contact email stamped into the {@code <email/>} grandchild.
     */
    private final String email;

    /**
     * Holds the optional landline phone number stamped into the {@code <landline_number/>}
     * grandchild.
     */
    private final String landlineNumber;

    /**
     * Holds the optional mobile phone number stamped into the {@code <mobile_number/>} grandchild.
     */
    private final String mobileNumber;

    /**
     * Constructs a triple from the optional email, landline and mobile.
     *
     * <p>Pass {@code null} for any field that should remain unchanged on the merchant's compliance
     * bundle; that field is dropped from the wire stanza rather than zeroed out.
     *
     * @param email          the email; may be {@code null}
     * @param landlineNumber the landline; may be {@code null}
     * @param mobileNumber   the mobile; may be {@code null}
     */
    public IqSetMerchantComplianceContactDetails(String email, String landlineNumber, String mobileNumber) {
        this.email = email;
        this.landlineNumber = landlineNumber;
        this.mobileNumber = mobileNumber;
    }

    /**
     * Returns the contact email the mutation stamps.
     *
     * <p>An empty optional means the field is dropped from the wire stanza.
     *
     * @return an {@link Optional} carrying the email
     */
    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    /**
     * Returns the landline phone number the mutation stamps.
     *
     * <p>An empty optional means the field is dropped from the wire stanza.
     *
     * @return an {@link Optional} carrying the landline
     */
    public Optional<String> landlineNumber() {
        return Optional.ofNullable(landlineNumber);
    }

    /**
     * Returns the mobile phone number the mutation stamps.
     *
     * <p>An empty optional means the field is dropped from the wire stanza.
     *
     * @return an {@link Optional} carrying the mobile
     */
    public Optional<String> mobileNumber() {
        return Optional.ofNullable(mobileNumber);
    }

    /**
     * Compares this triple with another for value equality across all three contact fields.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal triple
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSetMerchantComplianceContactDetails) obj;
        return Objects.equals(this.email, that.email)
                && Objects.equals(this.landlineNumber, that.landlineNumber)
                && Objects.equals(this.mobileNumber, that.mobileNumber);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(email, landlineNumber, mobileNumber);
    }

    /**
     * Returns a diagnostic string naming the email, landline and mobile.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "IqSetMerchantComplianceContactDetails[email=" + email
                + ", landlineNumber=" + landlineNumber
                + ", mobileNumber=" + mobileNumber + ']';
    }
}
