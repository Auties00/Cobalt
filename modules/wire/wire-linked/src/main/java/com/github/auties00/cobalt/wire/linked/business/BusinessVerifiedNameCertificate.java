package com.github.auties00.cobalt.wire.linked.business;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Signed certificate that attests a business's verified display name on
 * WhatsApp.
 *
 * <p>WhatsApp issues a verified-name certificate to every business that
 * passes identity verification. The certificate is structured as three
 * independent fields: the {@link #details() details} payload (an opaque
 * protobuf-encoded {@link Details} message that carries the business name,
 * issuer, certificate serial, localized name variants, and issuance time),
 * the client-side {@link #signature() signature} produced by the business
 * over the details payload, and the server-side
 * {@link #serverSignature() server signature} produced by WhatsApp to
 * authenticate the certificate. To inspect the certificate's metadata,
 * callers must deserialize the {@link #details()} bytes into a
 * {@link Details} instance.
 *
 * <p>The {@link Details#issuer() issuer} carried in the deserialized
 * payload distinguishes enterprise (Business API) businesses from
 * small/medium businesses (Business App), and the
 * {@link Details#localizedNames() localized names} list supports
 * multi-region businesses that need to advertise translated display names.
 */
@ProtobufMessage(name = "VerifiedNameCertificate")
public final class BusinessVerifiedNameCertificate {
    /**
     * Serialized {@link Details} payload of this certificate. Callers must
     * deserialize the bytes into a {@link Details} instance to access the
     * business name, issuer, certificate serial, localized name variants,
     * and issuance time. May be absent on the wire when the certificate
     * envelope was constructed without a details body.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] details;

    /**
     * Client-side cryptographic signature produced by the business over
     * the {@link #details()} payload. Verifiers use this signature to
     * confirm that the business holding the corresponding key authored
     * the certificate metadata.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] signature;

    /**
     * Server-side cryptographic signature produced by WhatsApp over the
     * {@link #details()} payload. Verifiers use this signature to confirm
     * that WhatsApp has authenticated the business and approved the
     * certificate.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] serverSignature;

    /**
     * Constructs a new {@code BusinessVerifiedNameCertificate} with the
     * given payload and signature components. Any argument may be
     * {@code null} when the corresponding wire field is absent.
     *
     * @param details         the serialized {@link Details} payload, or {@code null}
     * @param signature       the client-side signature bytes, or {@code null}
     * @param serverSignature the server-side signature bytes, or {@code null}
     */
    BusinessVerifiedNameCertificate(byte[] details, byte[] signature, byte[] serverSignature) {
        this.details = details;
        this.signature = signature;
        this.serverSignature = serverSignature;
    }

    /**
     * Returns the serialized {@link Details} payload carried by this
     * certificate.
     *
     * @return an {@code Optional} containing the details bytes, or empty
     *         when no details payload is present
     */
    public Optional<byte[]> details() {
        return Optional.ofNullable(details);
    }

    /**
     * Returns the client-side cryptographic signature over the details
     * payload.
     *
     * @return an {@code Optional} containing the signature bytes, or empty
     *         when no client signature is present
     */
    public Optional<byte[]> signature() {
        return Optional.ofNullable(signature);
    }

    /**
     * Returns the server-side cryptographic signature produced by WhatsApp
     * over the details payload.
     *
     * @return an {@code Optional} containing the server signature bytes,
     *         or empty when no server signature is present
     */
    public Optional<byte[]> serverSignature() {
        return Optional.ofNullable(serverSignature);
    }

    /**
     * Sets the serialized {@link Details} payload for this certificate.
     *
     * @param details the details bytes to set, or {@code null} to clear
     */
    public void setDetails(byte[] details) {
        this.details = details;
    }

    /**
     * Sets the client-side cryptographic signature for this certificate.
     *
     * @param signature the signature bytes to set, or {@code null} to clear
     */
    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    /**
     * Sets the server-side cryptographic signature for this certificate.
     *
     * @param serverSignature the server signature bytes to set, or {@code null} to clear
     */
    public void setServerSignature(byte[] serverSignature) {
        this.serverSignature = serverSignature;
    }

    /**
     * Decoded metadata payload of a {@link BusinessVerifiedNameCertificate}.
     *
     * <p>Once the outer {@link BusinessVerifiedNameCertificate#details()}
     * bytes are deserialized into this message, callers can inspect the
     * canonical verified business name, the certificate serial number,
     * the issuer that distinguishes enterprise-tier from small-business
     * accounts, the optional list of locale-specific name variants, and
     * the moment at which the certificate was issued.
     */
    @ProtobufMessage(name = "VerifiedNameCertificate.Details")
    public static final class Details {
        /**
         * Serial number that uniquely identifies this certificate within
         * the issuer's domain. A change in serial between two updates
         * indicates the business has been issued a new certificate and may
         * trigger re-validation of the business identity.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
        Long serial;

        /**
         * Issuer of this certificate, identifying which WhatsApp Business
         * product the business is registered under (enterprise via
         * {@code "ent:wa"} or small/medium business via {@code "smb:wa"}).
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        CertificateIssuer issuer;

        /**
         * Canonical verified business name approved by WhatsApp. This is
         * the display name shown in the chat header and contact info for
         * verified business contacts.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String verifiedName;

        /**
         * Locale-specific variants of the verified business name. Multi-region
         * businesses use this list to provide translated display names; the
         * list is empty when the business only advertises the canonical
         * {@link #verifiedName}.
         */
        @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
        List<LocalizedName> localizedNames;

        /**
         * Moment at which this certificate was issued by WhatsApp. Wire
         * encoding is seconds since the Unix epoch, converted to
         * {@link Instant} via {@link InstantSecondsMixin}.
         */
        @ProtobufProperty(index = 10, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
        Instant issueTime;

        /**
         * Constructs a new {@code Details} payload with the given
         * certificate metadata. Any argument may be {@code null} when the
         * corresponding wire field is absent.
         *
         * @param serial         the certificate serial number, or {@code null}
         * @param issuer         the certificate issuer, or {@code null}
         * @param verifiedName   the canonical verified business name, or {@code null}
         * @param localizedNames the locale-specific name variants, or {@code null}
         * @param issueTime      the certificate issuance time, or {@code null}
         */
        Details(Long serial, CertificateIssuer issuer, String verifiedName, List<LocalizedName> localizedNames, Instant issueTime) {
            this.serial = serial;
            this.issuer = issuer;
            this.verifiedName = verifiedName;
            this.localizedNames = localizedNames;
            this.issueTime = issueTime;
        }

        /**
         * Returns the serial number of this certificate.
         *
         * @return an {@code OptionalLong} containing the serial number,
         *         or empty when the field is absent
         */
        public OptionalLong serial() {
            return serial == null ? OptionalLong.empty() : OptionalLong.of(serial);
        }

        /**
         * Returns the issuer of this certificate.
         *
         * @return an {@code Optional} containing the {@link CertificateIssuer},
         *         or empty when the field is absent
         */
        public Optional<CertificateIssuer> issuer() {
            return Optional.ofNullable(issuer);
        }

        /**
         * Returns the canonical verified business name approved by
         * WhatsApp.
         *
         * @return an {@code Optional} containing the verified name, or
         *         empty when the field is absent
         */
        public Optional<String> verifiedName() {
            return Optional.ofNullable(verifiedName);
        }

        /**
         * Returns the locale-specific variants of the verified business
         * name.
         *
         * @return an unmodifiable list of {@link LocalizedName} entries;
         *         never {@code null}, empty when no variants exist
         */
        public List<LocalizedName> localizedNames() {
            return localizedNames == null ? List.of() : Collections.unmodifiableList(localizedNames);
        }

        /**
         * Returns the moment at which this certificate was issued.
         *
         * @return an {@code Optional} containing the issuance time, or
         *         empty when the field is absent
         */
        public Optional<Instant> issueTime() {
            return Optional.ofNullable(issueTime);
        }

        /**
         * Sets the serial number for this certificate.
         *
         * @param serial the serial number to set, or {@code null} to clear
         */
        public void setSerial(Long serial) {
            this.serial = serial;
        }

        /**
         * Sets the issuer for this certificate.
         *
         * @param issuer the {@link CertificateIssuer} to set, or {@code null} to clear
         */
        public void setIssuer(CertificateIssuer issuer) {
            this.issuer = issuer;
        }

        /**
         * Sets the canonical verified business name.
         *
         * @param verifiedName the verified name to set, or {@code null} to clear
         */
        public void setVerifiedName(String verifiedName) {
            this.verifiedName = verifiedName;
        }

        /**
         * Sets the locale-specific name variants for this certificate.
         *
         * @param localizedNames the list of {@link LocalizedName} entries
         *                       to set, or {@code null} to clear
         */
        public void setLocalizedNames(List<LocalizedName> localizedNames) {
            this.localizedNames = localizedNames;
        }

        /**
         * Sets the issuance time for this certificate.
         *
         * @param issueTime the issuance time to set, or {@code null} to clear
         */
        public void setIssueTime(Instant issueTime) {
            this.issueTime = issueTime;
        }
    }

    /**
     * Locale-specific variant of a verified business name carried inside
     * a {@link Details} payload.
     *
     * <p>Each instance pairs a verified business name with a locale
     * identified by an ISO 639-1 language code and an ISO 3166-1 alpha-2
     * country code, enabling a business that operates in multiple regions
     * to advertise translated display names. For example, a business
     * operating in both the United States and Brazil may publish two
     * localized names with locales {@code "en"/"US"} and {@code "pt"/"BR"}.
     */
    @ProtobufMessage(name = "LocalizedName")
    public static final class LocalizedName {
        /**
         * ISO 639-1 language code identifying the language of this
         * localized name (for example {@code "en"}, {@code "es"}, or
         * {@code "pt"}).
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String languageCode;

        /**
         * ISO 3166-1 alpha-2 country code identifying the country of this
         * localized name (for example {@code "US"}, {@code "BR"}, or
         * {@code "GB"}).
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String countryCode;

        /**
         * Verified business name expressed in the locale identified by
         * {@link #languageCode()} and {@link #countryCode()}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String verifiedName;

        /**
         * Constructs a new {@code LocalizedName} for the given locale and
         * translated business name. Any argument may be {@code null} when
         * the corresponding wire field is absent.
         *
         * @param languageCode the ISO 639-1 language code, or {@code null}
         * @param countryCode  the ISO 3166-1 alpha-2 country code, or {@code null}
         * @param verifiedName the localized verified business name, or {@code null}
         */
        LocalizedName(String languageCode, String countryCode, String verifiedName) {
            this.languageCode = languageCode;
            this.countryCode = countryCode;
            this.verifiedName = verifiedName;
        }

        /**
         * Returns the ISO 639-1 language code for this localized name.
         *
         * @return an {@code Optional} containing the language code (for
         *         example {@code "en"}), or empty when the field is absent
         */
        public Optional<String> languageCode() {
            return Optional.ofNullable(languageCode);
        }

        /**
         * Returns the ISO 3166-1 alpha-2 country code for this localized
         * name.
         *
         * @return an {@code Optional} containing the country code (for
         *         example {@code "US"}), or empty when the field is absent
         */
        public Optional<String> countryCode() {
            return Optional.ofNullable(countryCode);
        }

        /**
         * Returns the verified business name in this locale.
         *
         * @return an {@code Optional} containing the localized verified
         *         name, or empty when the field is absent
         */
        public Optional<String> verifiedName() {
            return Optional.ofNullable(verifiedName);
        }

        /**
         * Sets the ISO 639-1 language code for this localized name.
         *
         * @param languageCode the language code to set (for example
         *                     {@code "en"}), or {@code null} to clear
         */
        public void setLanguageCode(String languageCode) {
            this.languageCode = languageCode;
        }

        /**
         * Sets the ISO 3166-1 alpha-2 country code for this localized name.
         *
         * @param countryCode the country code to set (for example
         *                    {@code "US"}), or {@code null} to clear
         */
        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        /**
         * Sets the verified business name in this locale.
         *
         * @param verifiedName the localized verified name to set, or
         *                     {@code null} to clear
         */
        public void setVerifiedName(String verifiedName) {
            this.verifiedName = verifiedName;
        }
    }

    /**
     * Identifies the issuer of a {@link BusinessVerifiedNameCertificate},
     * which determines the WhatsApp Business product the business is
     * registered under.
     *
     * <p>The issuer is serialized as a string on the wire and resolves to
     * one of two singleton variants:
     * <ul>
     *   <li>{@link Enterprise} (wire value {@code "ent:wa"}) for businesses
     *       using the WhatsApp Business API (Cloud API or On-Premises API).
     *   <li>{@link SmallBusiness} (wire value {@code "smb:wa"}) for
     *       businesses using the WhatsApp Business App.
     * </ul>
     *
     * <p>Use {@link #ENTERPRISE} and {@link #SMALL_BUSINESS} to obtain the
     * canonical instances; deserialization from wire format is handled by
     * {@link #deserialize(String)}.
     */
    public sealed interface CertificateIssuer {
        /**
         * Singleton instance of the enterprise (Business API) issuer.
         */
        Enterprise ENTERPRISE = new Enterprise();

        /**
         * Singleton instance of the small/medium-business (Business App)
         * issuer.
         */
        SmallBusiness SMALL_BUSINESS = new SmallBusiness();

        /**
         * Returns the wire-format string value used to serialize this
         * issuer (for example {@code "ent:wa"} for {@link Enterprise} or
         * {@code "smb:wa"} for {@link SmallBusiness}).
         *
         * @return the wire string for this issuer
         */
        @ProtobufSerializer
        String value();

        /**
         * Deserializes the wire-format string for an issuer into the
         * corresponding canonical {@code CertificateIssuer} instance.
         *
         * @param value the wire string to deserialize, or {@code null}
         * @return the matching issuer instance, or {@code null} if the
         *         input is {@code null}
         * @throws IllegalArgumentException if the value is not a recognised
         *         issuer string ({@code "ent:wa"} or {@code "smb:wa"})
         */
        @ProtobufDeserializer
        static CertificateIssuer deserialize(String value) {
            if (value == null) {
                return null;
            }
            return switch (value) {
                case "ent:wa" -> new Enterprise();
                case "smb:wa" -> new SmallBusiness();
                default -> throw new IllegalArgumentException("Unknown certificate issuer: " + value);
            };
        }

        /**
         * Issuer variant for businesses registered through the WhatsApp
         * Business API (Cloud API or On-Premises API), serialized on the
         * wire as the string {@code "ent:wa"}.
         */
        final class Enterprise implements CertificateIssuer {
            /**
             * Constructs the canonical {@code Enterprise} issuer instance.
             * Reserved for the singleton initialisation; callers use
             * {@link CertificateIssuer#ENTERPRISE} instead.
             */
            private Enterprise() {

            }

            /**
             * Returns the wire-format string {@code "ent:wa"}.
             *
             * @return the string {@code "ent:wa"}
             */
            @Override
            public String value() {
                return "ent:wa";
            }

            /**
             * Returns the wire-format string representation of this issuer.
             *
             * @return the string {@code "ent:wa"}
             */
            @Override
            public String toString() {
                return value();
            }
        }

        /**
         * Issuer variant for businesses registered through the WhatsApp
         * Business App (small/medium-business tier), serialized on the
         * wire as the string {@code "smb:wa"}.
         */
        final class SmallBusiness implements CertificateIssuer {
            /**
             * Constructs the canonical {@code SmallBusiness} issuer
             * instance. Reserved for the singleton initialisation; callers
             * use {@link CertificateIssuer#SMALL_BUSINESS} instead.
             */
            private SmallBusiness() {

            }

            /**
             * Returns the wire-format string {@code "smb:wa"}.
             *
             * @return the string {@code "smb:wa"}
             */
            @Override
            public String value() {
                return "smb:wa";
            }

            /**
             * Returns the wire-format string representation of this issuer.
             *
             * @return the string {@code "smb:wa"}
             */
            @Override
            public String toString() {
                return value();
            }
        }
    }
}
