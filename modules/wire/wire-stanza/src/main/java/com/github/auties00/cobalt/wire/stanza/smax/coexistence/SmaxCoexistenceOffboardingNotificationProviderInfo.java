package com.github.auties00.cobalt.wire.stanza.smax.coexistence;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the {@code <provider_info/>} sub-child carried by a coexistence notification.
 *
 * <p>This projection identifies the third-party provider behind a hosted onboarding or
 * offboarding event (for example, the {@code ai_from_meta}, {@code automation}, or
 * {@code business_platform} surface). It exposes the provider's logo URL, display name, and
 * stable id so consumers can disambiguate which integration the coexistence link refers to and
 * render a logo and label when notifying the user. All three fields are optional because the
 * relay populates only the children it has for a given provider.
 */
@WhatsAppWebModule(moduleName = "WASmaxInCoexistenceProviderInfoMixin")
public final class SmaxCoexistenceOffboardingNotificationProviderInfo {
    /**
     * Holds the optional logo-URL bytes for the provider.
     *
     * <p>Is {@code null} when the relay did not include the {@code <logo_url/>} child; otherwise
     * the raw bytes of the URL a consumer loads into the notification.
     */
    private final byte[] logoUrl;

    /**
     * Holds the optional human-readable name bytes for the provider.
     *
     * <p>Is {@code null} when the relay omitted the {@code <name/>} child; otherwise the raw bytes
     * of the provider's display name.
     */
    private final byte[] name;

    /**
     * Holds the optional provider id.
     *
     * <p>Is {@code null} when the relay omitted the {@code <id/>} child or when its content cannot
     * be parsed as a base-10 integer. The value acts as a stable provider key when
     * cross-referencing the coexistence link against external records.
     */
    private final Integer id;

    /**
     * Constructs a new provider-info projection.
     *
     * <p>Invoked by {@link #of(Stanza)} from the parsed children of the {@code <provider_info/>}
     * stanza. Each argument may be {@code null}, reflecting the corresponding child being absent.
     *
     * @param logoUrl the optional logo-URL bytes; may be {@code null}
     * @param name    the optional name bytes; may be {@code null}
     * @param id      the optional provider id; may be {@code null}
     */
    public SmaxCoexistenceOffboardingNotificationProviderInfo(byte[] logoUrl, byte[] name, Integer id) {
        this.logoUrl = logoUrl;
        this.name = name;
        this.id = id;
    }

    /**
     * Returns the optional logo-URL bytes.
     *
     * <p>The returned {@link Optional} is empty when no {@code <logo_url/>} child was present. When
     * present, it wraps the underlying mutable buffer; callers must not mutate it.
     *
     * @return an {@link Optional} carrying the logo-URL bytes
     */
    public Optional<byte[]> logoUrl() {
        return Optional.ofNullable(logoUrl);
    }

    /**
     * Returns the optional name bytes.
     *
     * <p>The returned {@link Optional} is empty when no {@code <name/>} child was present. When
     * present, it wraps the underlying mutable buffer; callers must not mutate it.
     *
     * @return an {@link Optional} carrying the name bytes
     */
    public Optional<byte[]> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the optional provider id.
     *
     * <p>The returned {@link Optional} is empty when the relay omitted the {@code <id/>} child or
     * when its content failed the base-10 integer parse.
     *
     * @return an {@link Optional} carrying the id
     */
    public Optional<Integer> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Parses a {@code <provider_info/>} sub-child from the given parent stanza.
     *
     * <p>Resolves the {@code <provider_info/>} child of {@code parent} via
     * {@link Stanza#getChild(String)} and, when present, lifts its {@code <logo_url/>} and
     * {@code <name/>} contents through {@link Stanza#toContentBytes()} and its {@code <id/>} content
     * through {@link Stanza#toContentString()}. Returns {@link Optional#empty()} when no
     * {@code <provider_info/>} child exists. Consumed by
     * {@link SmaxCoexistenceOffboardingNotificationResponse#of(Stanza)} and
     * {@link SmaxCoexistenceOnboardingStatusNotificationResponse#of(Stanza)} to lift the nested
     * provider block.
     *
     * @implNote
     * This implementation collapses a malformed {@code <id/>} content (non-numeric or out of range
     * for {@code int}) to {@link Optional#empty()}, matching the {@code contentInt} parser which
     * signals a parse error on a non-numeric value; WA Web propagates the underlying error envelope
     * instead.
     *
     * @param parent the stanza carrying the {@code <provider_info/>} child; never {@code null}
     * @return an {@link Optional} carrying the projection
     * @throws NullPointerException if {@code parent} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInCoexistenceProviderInfoMixin",
            exports = {
                    "parseProviderInfoMixin",
                    "parseProviderInfoProviderInfoLogoUrl",
                    "parseProviderInfoProviderInfoName",
                    "parseProviderInfoProviderInfoId"
            },
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxCoexistenceOffboardingNotificationProviderInfo> of(Stanza parent) {
        Objects.requireNonNull(parent, "parent cannot be null");
        var providerInfoNode = parent.getChild("provider_info").orElse(null);
        if (providerInfoNode == null) {
            return Optional.empty();
        }
        var logoUrl = providerInfoNode.getChild("logo_url")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        var name = providerInfoNode.getChild("name")
                .flatMap(Stanza::toContentBytes)
                .orElse(null);
        var idBytes = providerInfoNode.getChild("id")
                .flatMap(Stanza::toContentString)
                .orElse(null);
        Integer id = null;
        if (idBytes != null) {
            try {
                id = Integer.parseInt(idBytes);
            } catch (NumberFormatException _) {
                return Optional.empty();
            }
        }
        return Optional.of(new SmaxCoexistenceOffboardingNotificationProviderInfo(logoUrl, name, id));
    }

    /**
     * Compares this projection with another for value equality.
     *
     * <p>Two projections are equal when their logo-URL bytes, name bytes, and provider ids are
     * pairwise equal; the byte arrays are compared by content via {@link Arrays#equals(byte[], byte[])}.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} if {@code obj} is an equal projection
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxCoexistenceOffboardingNotificationProviderInfo) obj;
        return Arrays.equals(this.logoUrl, that.logoUrl)
                && Arrays.equals(this.name, that.name)
                && Objects.equals(this.id, that.id);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>Combines the content hashes of the logo-URL and name byte arrays with the hash of the
     * provider id.
     *
     * @return the hash code for this projection
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(id);
        result = 31 * result + Arrays.hashCode(logoUrl);
        result = 31 * result + Arrays.hashCode(name);
        return result;
    }

    /**
     * Returns a debug string describing this projection.
     *
     * <p>Renders the logo-URL and name byte arrays element by element via
     * {@link Arrays#toString(byte[])} together with the provider id.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxCoexistenceOffboardingNotificationProviderInfo[logoUrl=" + Arrays.toString(logoUrl)
                + ", name=" + Arrays.toString(name)
                + ", id=" + id + ']';
    }
}
