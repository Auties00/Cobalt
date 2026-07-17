package com.github.auties00.cobalt.wire.linked.setting;

import it.auties.protobuf.annotation.ProtobufEnum;

/**
 * Categories of personal-data export the user can request through the
 * WhatsApp GDPR data-download flow.
 *
 * <p>Each constant identifies one slice of user data the server will package
 * and make available for download (account-wide vs. newsletter-only). The
 * matching {@link #wireValue()} feeds the {@code report_type} attribute on
 * the GDPR control IQ; the account-wide variant is special-cased to omit
 * the attribute entirely.
 */
@ProtobufEnum
public enum GdprReportType {
    /**
     * Full account export: covers messages, profile data, settings and
     * everything else tied to the user's account. The wire form omits the
     * {@code report_type} attribute, signalling "everything".
     */
    ACCOUNT(null),

    /**
     * Newsletter-only export: covers data tied to newsletters the user
     * follows or owns. Wire form sets {@code report_type="newsletters"}.
     */
    NEWSLETTERS("newsletters");

    /**
     * Wire string sent in the {@code report_type} attribute, or {@code null}
     * when the attribute should be omitted for the account-wide variant.
     */
    private final String wireValue;

    /**
     * Constructs a new constant carrying the supplied wire value.
     *
     * @param wireValue the attribute value to emit, or {@code null} when the
     *                  attribute should be omitted entirely
     */
    GdprReportType(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire-side value emitted on the {@code report_type}
     * attribute.
     *
     * @return the wire value, or {@code null} for the account-wide variant
     */
    public String wireValue() {
        return wireValue;
    }
}
