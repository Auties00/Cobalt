package com.github.auties00.cobalt.wire.stanza.iq.biz;

import java.util.Objects;
import java.util.Optional;

/**
 * Carries a grievance-officer block inside an {@link IqSetMerchantComplianceRequest}.
 *
 * <p>The block sets or updates the grievance officer's display name and contact triple, a regulatory
 * disclosure required for India sellers under the Consumer Protection rules and similar markets. The
 * name is optional and an absent name is dropped from the wire stanza rather than zeroed out.
 */
public final class IqSetMerchantComplianceGrievanceOfficerDetails {
    /**
     * Holds the optional officer name stamped into the {@code <name/>} grandchild.
     */
    private final String name;

    /**
     * Holds the contact triple flattened into the {@code <email/>}, {@code <landline_number/>} and
     * {@code <mobile_number/>} grandchildren.
     */
    private final IqSetMerchantComplianceContactDetails contact;

    /**
     * Constructs a block from the optional officer name and the contact triple.
     *
     * <p>The name may be left {@code null} when the merchant only wants to update the contact
     * channels.
     *
     * @param name    the name; may be {@code null}
     * @param contact the contact triple; never {@code null}
     * @throws NullPointerException if {@code contact} is {@code null}
     */
    public IqSetMerchantComplianceGrievanceOfficerDetails(String name, IqSetMerchantComplianceContactDetails contact) {
        this.name = name;
        this.contact = Objects.requireNonNull(contact, "contact cannot be null");
    }

    /**
     * Returns the officer name the mutation stamps.
     *
     * <p>An empty optional means the field is dropped from the wire stanza.
     *
     * @return an {@link Optional} carrying the name
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the officer's contact triple.
     *
     * <p>The triple itself follows the same field-by-field optionality, where each absent field is
     * dropped from the wire stanza.
     *
     * @return the triple; never {@code null}
     */
    public IqSetMerchantComplianceContactDetails contact() {
        return contact;
    }

    /**
     * Compares this block with another for value equality on the name and contact triple.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal block
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSetMerchantComplianceGrievanceOfficerDetails) obj;
        return Objects.equals(this.name, that.name)
                && Objects.equals(this.contact, that.contact);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, contact);
    }

    /**
     * Returns a diagnostic string naming the officer name and contact triple.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "IqSetMerchantComplianceGrievanceOfficerDetails[name=" + name
                + ", contact=" + contact + ']';
    }
}
