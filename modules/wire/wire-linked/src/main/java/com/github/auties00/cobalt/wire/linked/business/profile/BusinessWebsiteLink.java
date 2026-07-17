package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A website advertised on a WhatsApp Business profile, paired with the safe
 * redirect URL to open instead of the raw address.
 *
 * <p>A WhatsApp Business account can list one or more websites on its profile.
 * To protect users, WhatsApp does not send them straight to the advertised
 * address; it wraps each one in a redirect (a "link shim") that lets WhatsApp
 * vet the destination before the browser follows it. This model carries both
 * forms: the {@link #website() raw advertised address} as the merchant entered
 * it, and the {@link #safeRedirectUrl() redirect URL} an app should actually
 * navigate to.
 */
@ProtobufMessage
public final class BusinessWebsiteLink {
    /**
     * Raw website address as advertised on the business profile, exactly as the
     * merchant entered it. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String website;

    /**
     * Redirect URL wrapping {@link #website} that an app should navigate to so
     * WhatsApp can vet the destination first. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String safeRedirectUrl;

    /**
     * Constructs a new {@code BusinessWebsiteLink}. Both arguments are optional
     * and may be {@code null} when the server omitted the field.
     *
     * @param website         the raw advertised website address, or {@code null}
     * @param safeRedirectUrl the vetting redirect URL, or {@code null}
     */
    BusinessWebsiteLink(String website, String safeRedirectUrl) {
        this.website = website;
        this.safeRedirectUrl = safeRedirectUrl;
    }

    /**
     * Returns the raw website address as advertised on the business profile.
     *
     * @return an {@code Optional} containing the raw address, or empty when the
     *         server omitted it
     */
    public Optional<String> website() {
        return Optional.ofNullable(website);
    }

    /**
     * Returns the redirect URL an app should navigate to so WhatsApp can vet
     * the destination before the browser follows it.
     *
     * @return an {@code Optional} containing the redirect URL, or empty when the
     *         server omitted it
     */
    public Optional<String> safeRedirectUrl() {
        return Optional.ofNullable(safeRedirectUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessWebsiteLink) obj;
        return Objects.equals(this.website, that.website) &&
               Objects.equals(this.safeRedirectUrl, that.safeRedirectUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(website, safeRedirectUrl);
    }

    @Override
    public String toString() {
        return "BusinessWebsiteLink[" +
               "website=" + website + ", " +
               "safeRedirectUrl=" + safeRedirectUrl + ']';
    }
}
