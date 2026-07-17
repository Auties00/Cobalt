package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the {@code <native_actions_mixin/>} projection, gathering the 0 to 50
 * {@link SmaxBannerSuggestionNativeAction} entries harvested from a parent
 * stanza's {@code <native_action/>} children.
 * <p>
 * Mixed into the CTWA banner-suggestion {@link SmaxBannerSuggestionBanner} to
 * deliver the per-platform deep-link list; the mixin exists so the 0 to 50
 * cardinality bound is enforced once at this level instead of scattered across
 * every consumer.
 *
 * @implNote This implementation runs the cardinality check before any
 * individual {@link SmaxBannerSuggestionNativeAction#of(Stanza)} call, so a
 * relay-side over-quota notification is rejected without ever touching the
 * individual entries.
 */
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaActionNativeActionsMixinMixin")
public final class SmaxBannerSuggestionNativeActionsMixin {
    /**
     * Holds the harvested {@code <native_action/>} entries (0 to 50).
     */
    private final List<SmaxBannerSuggestionNativeAction> nativeAction;

    /**
     * Constructs a projection from already-validated entries.
     * <p>
     * Callers normally obtain a projection by parsing a parent stanza via
     * {@link #of(Stanza)}; this constructor is exposed for tests and for
     * hand-built fixtures.
     *
     * @param nativeAction the native-action entries; may be {@code null} (treated as empty)
     */
    public SmaxBannerSuggestionNativeActionsMixin(List<SmaxBannerSuggestionNativeAction> nativeAction) {
        this.nativeAction = nativeAction == null ? List.of() : List.copyOf(nativeAction);
    }

    /**
     * Returns the harvested entries.
     * <p>
     * Bounded to 0 to 50 by the parser; consumers filter by
     * {@link SmaxBannerSuggestionNativeAction#platform()} to pick the entry
     * matching their runtime.
     *
     * @return an unmodifiable list of 0 to 50 entries; never {@code null}
     */
    public List<SmaxBannerSuggestionNativeAction> nativeAction() {
        return nativeAction;
    }

    /**
     * Parses the projection from a parent stanza by harvesting its
     * {@code <native_action/>} children.
     * <p>
     * Returns empty when the {@code <native_action/>} cardinality exceeds the
     * 0 to 50 bound or when any individual entry fails to parse. A parent with
     * zero {@code <native_action/>} children succeeds and yields an empty list.
     *
     * @implNote This implementation enforces the upper cardinality bound before
     * walking individual children, so a malformed over-quota stanza is rejected
     * without ever delegating to {@link SmaxBannerSuggestionNativeAction#of(Stanza)}.
     * @param stanza the parent stanza hosting the {@code <native_action/>} children; never {@code null}
     * @return an {@link Optional} carrying the projection, or empty when
     *         parsing fails
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaActionNativeActionsMixinMixin",
            exports = "parseNativeActionsMixinMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxBannerSuggestionNativeActionsMixin> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var nativeActionNodes = stanza.streamChildren("native_action").toList();
        if (nativeActionNodes.size() > 50) {
            return Optional.empty();
        }
        var nativeActions = new ArrayList<SmaxBannerSuggestionNativeAction>(nativeActionNodes.size());
        for (var nativeActionNode : nativeActionNodes) {
            var parsed = SmaxBannerSuggestionNativeAction.of(nativeActionNode);
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            nativeActions.add(parsed.get());
        }
        return Optional.of(new SmaxBannerSuggestionNativeActionsMixin(nativeActions));
    }

    /**
     * Compares this projection to {@code obj} for structural equality on the
     * harvested entries.
     *
     * @param obj the candidate; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxBannerSuggestionNativeActionsMixin}
     *         with matching {@link #nativeAction()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxBannerSuggestionNativeActionsMixin) obj;
        return Objects.equals(this.nativeAction, that.nativeAction);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash of the harvested entries
     */
    @Override
    public int hashCode() {
        return Objects.hash(nativeAction);
    }

    /**
     * Returns a debug-friendly rendering naming the harvested entries.
     *
     * @return a record-style string with the entries list
     */
    @Override
    public String toString() {
        return "SmaxBannerSuggestionNativeActionsMixin[nativeAction=" + nativeAction + ']';
    }
}
