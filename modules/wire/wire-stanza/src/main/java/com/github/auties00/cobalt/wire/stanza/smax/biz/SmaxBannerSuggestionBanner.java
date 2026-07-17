package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the {@code <banner/>} grandchild of the CTWA banner-suggestion
 * notification, bundling the {@link SmaxBannerSuggestionConfig},
 * {@link SmaxBannerSuggestionContent}, optional
 * {@link SmaxBannerSuggestionAction}, and the
 * {@link SmaxBannerSuggestionNativeAction native-action} list.
 * <p>
 * Carries the full rich-content payload that drives the WhatsApp Business
 * "suggested banner" panel: expiry, visual style, headline, body, highlight
 * copy, deep-link target, and per-platform deep-link overrides.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaActionBannerSuggestionRequest")
public final class SmaxBannerSuggestionBanner {
    /**
     * Holds the mandatory {@code <config/>} projection carrying the banner
     * lifecycle attributes.
     */
    private final SmaxBannerSuggestionConfig config;

    /**
     * Holds the mandatory {@code <content/>} projection carrying the banner
     * copy.
     */
    private final SmaxBannerSuggestionContent content;

    /**
     * Holds the optional {@code <action/>} projection carrying the deep-link
     * triple.
     */
    private final SmaxBannerSuggestionAction action;

    /**
     * Holds the optional native-actions list of 0 to 50 entries.
     */
    private final List<SmaxBannerSuggestionNativeAction> nativeActions;

    /**
     * Constructs a banner from already-validated sub-projections.
     * <p>
     * Callers normally obtain a banner by parsing a stanza via
     * {@link #of(Stanza)}; this constructor is exposed for tests and for
     * hand-built fixtures.
     *
     * @param config        the {@link SmaxBannerSuggestionConfig} projection; never {@code null}
     * @param content       the {@link SmaxBannerSuggestionContent} projection; never {@code null}
     * @param action        the optional {@link SmaxBannerSuggestionAction} projection; may be {@code null}
     * @param nativeActions the native actions list (0 to 50 entries); may be {@code null} (treated as empty)
     * @throws NullPointerException if {@code config} or {@code content} is {@code null}
     */
    public SmaxBannerSuggestionBanner(SmaxBannerSuggestionConfig config, SmaxBannerSuggestionContent content, SmaxBannerSuggestionAction action, List<SmaxBannerSuggestionNativeAction> nativeActions) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.action = action;
        this.nativeActions = nativeActions == null ? List.of() : List.copyOf(nativeActions);
    }

    /**
     * Returns the {@link SmaxBannerSuggestionConfig} projection.
     * <p>
     * Carries the {@code expires_at}, {@code display}, and {@code revoked}
     * attributes; consumers check {@link SmaxBannerSuggestionConfig#revoked()}
     * first to short-circuit rendering when the banner has been pulled.
     *
     * @return the config projection; never {@code null}
     */
    public SmaxBannerSuggestionConfig config() {
        return config;
    }

    /**
     * Returns the {@link SmaxBannerSuggestionContent} projection.
     * <p>
     * Carries the {@code locale} attribute, the {@code <heading>},
     * {@code <body>}, and {@code <highlight>} mandatory copy, and the three
     * optional localised parallels.
     *
     * @return the content projection; never {@code null}
     */
    public SmaxBannerSuggestionContent content() {
        return content;
    }

    /**
     * Returns the optional {@link SmaxBannerSuggestionAction} projection.
     * <p>
     * Empty when the banner ships no {@code <action/>} element; consumers fall
     * back to the matching {@link SmaxBannerSuggestionNativeAction native action}
     * entry for the client platform.
     *
     * @return an {@link Optional} carrying the action projection, or empty
     *         when the {@code <action/>} element is absent
     */
    public Optional<SmaxBannerSuggestionAction> action() {
        return Optional.ofNullable(action);
    }

    /**
     * Returns the {@link SmaxBannerSuggestionNativeAction native-action}
     * entries.
     * <p>
     * Each entry pairs a {@code platform} string with a deep link plus a
     * minimum app version; consumers pick the entry matching the local
     * platform.
     *
     * @return an unmodifiable list of 0 to 50 entries; never {@code null}
     */
    public List<SmaxBannerSuggestionNativeAction> nativeActions() {
        return nativeActions;
    }

    /**
     * Parses the projection from a {@code <banner/>} stanza.
     * <p>
     * Returns empty whenever the stanza tag is wrong, either mandatory child is
     * missing, the optional {@code <action/>} child fails to parse, or the
     * {@code <native_action/>} cardinality bound (0 to 50) is breached.
     *
     * @implNote This implementation walks the children in order:
     * {@code config}, {@code content}, optional {@code action}, then the
     * {@link SmaxBannerSuggestionNativeActionsMixin} bound check. The
     * cardinality is enforced by the mixin so it short-circuits before any
     * {@code <native_action/>} child is parsed.
     * @param stanza the candidate {@code <banner/>} stanza; never {@code null}
     * @return an {@link Optional} carrying the projection, or empty when
     *         parsing fails at any step
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaActionBannerSuggestionRequest",
            exports = "parseBannerSuggestionRequestCtwaSuggestionBanner",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxBannerSuggestionBanner> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("banner")) {
            return Optional.empty();
        }
        var configNode = stanza.getChild("config").orElse(null);
        if (configNode == null) {
            return Optional.empty();
        }
        var config = SmaxBannerSuggestionConfig.of(configNode).orElse(null);
        if (config == null) {
            return Optional.empty();
        }
        var contentNode = stanza.getChild("content").orElse(null);
        if (contentNode == null) {
            return Optional.empty();
        }
        var content = SmaxBannerSuggestionContent.of(contentNode).orElse(null);
        if (content == null) {
            return Optional.empty();
        }
        SmaxBannerSuggestionAction action = null;
        var actionNode = stanza.getChild("action").orElse(null);
        if (actionNode != null) {
            var parsed = SmaxBannerSuggestionAction.of(actionNode);
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            action = parsed.get();
        }
        var nativeActionsMixin = SmaxBannerSuggestionNativeActionsMixin.of(stanza).orElse(null);
        if (nativeActionsMixin == null) {
            return Optional.empty();
        }
        return Optional.of(new SmaxBannerSuggestionBanner(config, content, action,
                nativeActionsMixin.nativeAction()));
    }

    /**
     * Compares this banner to {@code obj} for structural equality on all four
     * sub-projections.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxBannerSuggestionBanner}
     *         with matching {@link #config()}, {@link #content()},
     *         {@link #action()}, and {@link #nativeActions()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxBannerSuggestionBanner) obj;
        return Objects.equals(this.config, that.config)
                && Objects.equals(this.content, that.content)
                && Objects.equals(this.action, that.action)
                && Objects.equals(this.nativeActions, that.nativeActions);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash of all four sub-projections
     */
    @Override
    public int hashCode() {
        return Objects.hash(config, content, action, nativeActions);
    }

    /**
     * Returns a debug-friendly rendering naming all four sub-projections.
     *
     * @return a record-style string with the four sub-projection values
     */
    @Override
    public String toString() {
        return "SmaxBannerSuggestionBanner[config=" + config
                + ", content=" + content
                + ", action=" + action
                + ", nativeActions=" + nativeActions + ']';
    }
}
