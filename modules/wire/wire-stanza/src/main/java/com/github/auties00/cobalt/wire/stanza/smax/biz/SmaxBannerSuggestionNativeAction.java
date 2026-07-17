package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Models a single {@code <native_action/>} entry under the CTWA
 * banner-suggestion {@link SmaxBannerSuggestionNativeActionsMixin native-actions
 * mixin}, describing one platform's deep-link target.
 * <p>
 * Each entry is keyed by a {@link #platform()} string (one of {@code "web"},
 * {@code "ios"}, {@code "android"}); the consumer picks the entry matching its
 * runtime and uses {@link #localLink()} as the banner call-to-action's
 * navigation target. {@link #minAppVersion()} gates rendering when the client
 * app is too old for the deep link.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaActionNativeActionsMixinMixin")
public final class SmaxBannerSuggestionNativeAction {
    /**
     * Holds the mandatory {@code platform} attribute, for example
     * {@code "web"}, {@code "ios"}, or {@code "android"}.
     */
    private final String platform;

    /**
     * Holds the mandatory {@code min_app_version} attribute.
     */
    private final String minAppVersion;

    /**
     * Holds the mandatory {@code local_link} attribute, the deep-link URL.
     */
    private final String localLink;

    /**
     * Holds the optional {@code universal_link} attribute.
     */
    private final String universalLink;

    /**
     * Constructs an entry from already-validated wire values.
     * <p>
     * Callers normally obtain an entry by parsing a stanza via {@link #of(Stanza)};
     * this constructor is exposed for tests and for hand-built fixtures.
     *
     * @param platform      the platform identifier; never {@code null}
     * @param minAppVersion the minimum client version; never {@code null}
     * @param localLink     the deep-link URL; never {@code null}
     * @param universalLink the optional universal-link URL; may be {@code null}
     * @throws NullPointerException if any of {@code platform},
     *                              {@code minAppVersion}, or
     *                              {@code localLink} is {@code null}
     */
    public SmaxBannerSuggestionNativeAction(String platform, String minAppVersion,
                        String localLink, String universalLink) {
        this.platform = Objects.requireNonNull(platform, "platform cannot be null");
        this.minAppVersion = Objects.requireNonNull(minAppVersion, "minAppVersion cannot be null");
        this.localLink = Objects.requireNonNull(localLink, "localLink cannot be null");
        this.universalLink = universalLink;
    }

    /**
     * Returns the platform identifier.
     * <p>
     * Used by consumers to filter for the entry matching their runtime.
     *
     * @return the platform identifier; never {@code null}
     */
    public String platform() {
        return platform;
    }

    /**
     * Returns the minimum client version.
     * <p>
     * Consumers compare this against the sanitised client version and skip
     * rendering when the client is too old; the value uses the same
     * dotted-component format as the sanitised version string.
     *
     * @return the version string; never {@code null}
     */
    public String minAppVersion() {
        return minAppVersion;
    }

    /**
     * Returns the deep-link URL.
     * <p>
     * Used as the navigation target when the entry matches the client platform;
     * the value supports both API-style deep links (such as {@code "MANAGE_ADS"})
     * and bare HTTPS URLs.
     *
     * @return the local link; never {@code null}
     */
    public String localLink() {
        return localLink;
    }

    /**
     * Returns the optional universal-link URL.
     * <p>
     * Cross-platform deep link kept as a fallback alongside
     * {@link #localLink()}; not consumed by the Web banner pipeline today.
     *
     * @return an {@link Optional} carrying the universal link, or empty when the
     *         relay omitted the attribute
     */
    public Optional<String> universalLink() {
        return Optional.ofNullable(universalLink);
    }

    /**
     * Parses the entry from a {@code <native_action/>} stanza.
     * <p>
     * Returns empty when the stanza tag is wrong or any of the three mandatory
     * attributes is missing. The optional {@code universal_link} attribute may
     * legally be absent.
     *
     * @param stanza the candidate {@code <native_action/>} stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed entry, or empty when
     *         parsing fails
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaActionNativeActionsMixinMixin",
            exports = "parseNativeActionsMixinNativeAction",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxBannerSuggestionNativeAction> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("native_action")) {
            return Optional.empty();
        }
        var platform = stanza.getAttributeAsString("platform").orElse(null);
        if (platform == null) {
            return Optional.empty();
        }
        var minAppVersion = stanza.getAttributeAsString("min_app_version").orElse(null);
        if (minAppVersion == null) {
            return Optional.empty();
        }
        var localLink = stanza.getAttributeAsString("local_link").orElse(null);
        if (localLink == null) {
            return Optional.empty();
        }
        var universalLink = stanza.getAttributeAsString("universal_link").orElse(null);
        return Optional.of(new SmaxBannerSuggestionNativeAction(platform, minAppVersion, localLink, universalLink));
    }

    /**
     * Compares this entry to {@code obj} for structural equality on all four
     * slots.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxBannerSuggestionNativeAction}
     *         with matching {@link #platform()}, {@link #minAppVersion()},
     *         {@link #localLink()}, and {@link #universalLink()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxBannerSuggestionNativeAction) obj;
        return Objects.equals(this.platform, that.platform)
                && Objects.equals(this.minAppVersion, that.minAppVersion)
                && Objects.equals(this.localLink, that.localLink)
                && Objects.equals(this.universalLink, that.universalLink);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash of all four slots
     */
    @Override
    public int hashCode() {
        return Objects.hash(platform, minAppVersion, localLink, universalLink);
    }

    /**
     * Returns a debug-friendly rendering naming all four slots.
     *
     * @return a record-style string with the four slot values
     */
    @Override
    public String toString() {
        return "SmaxBannerSuggestionNativeAction[platform=" + platform
                + ", minAppVersion=" + minAppVersion
                + ", localLink=" + localLink
                + ", universalLink=" + universalLink + ']';
    }
}
