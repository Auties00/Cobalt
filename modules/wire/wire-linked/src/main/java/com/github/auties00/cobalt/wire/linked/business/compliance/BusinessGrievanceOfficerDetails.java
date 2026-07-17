package com.github.auties00.cobalt.wire.linked.business.compliance;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Grievance-officer contact block published by a business as part of its
 * merchant-compliance disclosure.
 *
 * <p>Regulations in regions such as India require businesses that operate
 * on WhatsApp to publish the identity and contact details of a designated
 * grievance officer, who is responsible for handling escalated consumer
 * complaints. This block extends the customer-care contact triplet (email,
 * landline, mobile) with the officer's own display name. Each of the four
 * fields is independently optional: a business may publish only the
 * channels and information it actually maintains.
 */
@ProtobufMessage
public final class BusinessGrievanceOfficerDetails {
    /**
     * Display name of the grievance officer designated by the business.
     * Populated when the business has named a specific individual as the
     * point of contact for escalated complaints.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Email address at which customers can reach the grievance officer.
     * Populated when the business has published an email channel for the
     * officer.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String email;

    /**
     * Landline phone number at which customers can reach the grievance
     * officer, in international (E.164-like) format. Populated when the
     * business has published a fixed-line channel for the officer.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String landlineNumber;

    /**
     * Mobile phone number at which customers can reach the grievance
     * officer, in international (E.164-like) format. Populated when the
     * business has published a mobile-line channel for the officer.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String mobileNumber;

    /**
     * Constructs a new {@code BusinessGrievanceOfficerDetails} block. Any
     * argument may be {@code null} when the business has not published
     * the corresponding piece of officer information.
     *
     * @param name           the grievance officer's display name, or {@code null}
     * @param email          the officer's email address, or {@code null}
     * @param landlineNumber the officer's landline number, or {@code null}
     * @param mobileNumber   the officer's mobile number, or {@code null}
     */
    BusinessGrievanceOfficerDetails(String name, String email, String landlineNumber, String mobileNumber) {
        this.name = name;
        this.email = email;
        this.landlineNumber = landlineNumber;
        this.mobileNumber = mobileNumber;
    }

    /**
     * Returns the display name of the grievance officer.
     *
     * @return an {@code Optional} containing the officer name, or empty
     *         when no name has been published
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the email address at which the grievance officer can be
     * reached.
     *
     * @return an {@code Optional} containing the email address, or empty
     *         when no email channel has been published
     */
    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    /**
     * Returns the landline phone number at which the grievance officer
     * can be reached.
     *
     * @return an {@code Optional} containing the landline number, or empty
     *         when no landline channel has been published
     */
    public Optional<String> landlineNumber() {
        return Optional.ofNullable(landlineNumber);
    }

    /**
     * Returns the mobile phone number at which the grievance officer
     * can be reached.
     *
     * @return an {@code Optional} containing the mobile number, or empty
     *         when no mobile channel has been published
     */
    public Optional<String> mobileNumber() {
        return Optional.ofNullable(mobileNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessGrievanceOfficerDetails) obj;
        return Objects.equals(this.name, that.name) &&
               Objects.equals(this.email, that.email) &&
               Objects.equals(this.landlineNumber, that.landlineNumber) &&
               Objects.equals(this.mobileNumber, that.mobileNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, landlineNumber, mobileNumber);
    }

    @Override
    public String toString() {
        return "BusinessGrievanceOfficerDetails[" +
               "name=" + name + ", " +
               "email=" + email + ", " +
               "landlineNumber=" + landlineNumber + ", " +
               "mobileNumber=" + mobileNumber + ']';
    }
}
