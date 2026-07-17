package com.github.auties00.cobalt.wire.stanza.smax.coexistence;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the inbound projection of {@code <notification type="hosted"><offboarding/></notification>}.
 *
 * <p>This response is surfaced when the relay tears down a third-party-coexistence link, that is,
 * when the {@code ai_from_meta}, {@code automation}, or {@code business_platform} surface stops
 * handling messages on behalf of the user. It carries the notification id used to acknowledge
 * delivery, the {@code from} JID (always the WA server), the {@code product_surface} the teardown
 * applies to, and the {@code <provider_info/>} block identifying the integration that ended.
 * Consumers wait on this projection to clear local provider state and surface a user-visible
 * "coexistence ended" notice.
 */
@WhatsAppWebModule(moduleName = "WASmaxInCoexistenceOffboardingNotificationRequest")
@WhatsAppWebModule(moduleName = "WASmaxInCoexistenceServerNotificationMixin")
@WhatsAppWebModule(moduleName = "WASmaxInCoexistenceProductSurfaceMixin")
@WhatsAppWebModule(moduleName = "WASmaxInCoexistenceEnums")
public final class SmaxCoexistenceOffboardingNotificationResponse implements SmaxStanza.Response {
    /**
     * Holds the notification id used for delivery acknowledgement.
     *
     * <p>Routed back to the relay in the {@code <ack/>} the receiver sends after handling the
     * notification.
     */
    private final String notificationId;

    /**
     * Holds the notification {@code from} JID, always the WA server JID.
     *
     * <p>Validated against the WA server domain in {@link #of(Stanza)} so the dispatch layer can
     * route the projection through the same {@code from}-keyed handler registry as other server
     * notifications.
     */
    private final Jid notificationFrom;

    /**
     * Holds the {@code product_surface} attribute on {@code <offboarding/>}.
     *
     * <p>One of {@code "ai_from_meta"}, {@code "automation"}, or {@code "business_platform"}. WA Web
     * branches only on the {@code "automation"} case to call its CTWA outcome handler; consumers may
     * branch on any value to choose which surface-specific cleanup to run.
     */
    private final String offboardingProductSurface;

    /**
     * Holds the {@code <provider_info/>} sub-child.
     *
     * <p>Carries the provider's optional display name, logo URL, and stable id so consumers can
     * disambiguate which third-party integration ended; see
     * {@link SmaxCoexistenceOffboardingNotificationProviderInfo} for the per-field semantics.
     */
    private final SmaxCoexistenceOffboardingNotificationProviderInfo offboardingProviderInfo;

    /**
     * Constructs a new offboarding-notification projection.
     *
     * <p>Invoked by {@link #of(Stanza)} after the inbound stanza passes the type, surface, and
     * provider-info validation. Every argument is required.
     *
     * @param notificationId            the notification id; never {@code null}
     * @param notificationFrom          the from JID; never {@code null}
     * @param offboardingProductSurface the product-surface enum literal; never {@code null}
     * @param offboardingProviderInfo   the provider-info projection; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SmaxCoexistenceOffboardingNotificationResponse(String notificationId,
                    Jid notificationFrom,
                    String offboardingProductSurface,
                    SmaxCoexistenceOffboardingNotificationProviderInfo offboardingProviderInfo) {
        this.notificationId = Objects.requireNonNull(notificationId, "notificationId cannot be null");
        this.notificationFrom = Objects.requireNonNull(notificationFrom, "notificationFrom cannot be null");
        this.offboardingProductSurface = Objects.requireNonNull(offboardingProductSurface, "offboardingProductSurface cannot be null");
        this.offboardingProviderInfo = Objects.requireNonNull(offboardingProviderInfo, "offboardingProviderInfo cannot be null");
    }

    /**
     * Returns the notification id.
     *
     * <p>Used to build the corresponding {@code <ack/>} stanza.
     *
     * @return the id; never {@code null}
     */
    public String notificationId() {
        return notificationId;
    }

    /**
     * Returns the notification {@code from} JID.
     *
     * <p>Always the WA server JID; exposed so dispatch can key handlers by sender.
     *
     * @return the JID; never {@code null}
     */
    public Jid notificationFrom() {
        return notificationFrom;
    }

    /**
     * Returns the product-surface enum literal.
     *
     * <p>One of {@code "ai_from_meta"}, {@code "automation"}, or {@code "business_platform"};
     * consumers branch on the value to decide which surface-specific cleanup to run.
     *
     * @return the literal; never {@code null}
     */
    public String offboardingProductSurface() {
        return offboardingProductSurface;
    }

    /**
     * Returns the provider-info projection.
     *
     * <p>Carries the optional logo URL, name bytes, and provider id; see
     * {@link SmaxCoexistenceOffboardingNotificationProviderInfo} for the per-field semantics.
     *
     * @return the projection; never {@code null}
     */
    public SmaxCoexistenceOffboardingNotificationProviderInfo offboardingProviderInfo() {
        return offboardingProviderInfo;
    }

    /**
     * Parses a {@link SmaxCoexistenceOffboardingNotificationResponse} projection from the given stanza.
     *
     * <p>Returns {@link Optional#empty()} unless {@code stanza} is a {@code <notification>} with an
     * {@code <offboarding/>} child, a {@code from} attribute that resolves to the WA server JID via
     * {@link Jid#isServerJid(JidServer)} against {@link JidServer#user()}, a {@code type} attribute
     * equal to {@code "hosted"}, an {@code <offboarding/>} {@code product_surface} that is one of
     * {@code "ai_from_meta"}, {@code "automation"}, or {@code "business_platform"}, a parseable
     * {@code <provider_info/>} block, and a non-null {@code id} attribute. When all checks pass, a
     * fully populated projection is returned.
     *
     * @implNote
     * This implementation only lifts the notification {@code id} from the server-notification mixin;
     * the server timestamp {@code t} and the optional {@code offline} batch index (0 to 1024) are
     * dropped because Cobalt's notification dispatch keys solely on the id, whereas WA Web's
     * {@code parseServerNotificationMixin} returns all three fields.
     *
     * @param stanza the inbound notification stanza; never {@code null}
     * @return an {@link Optional} carrying the projection
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxCoexistenceOffboardingNotificationRPC",
            exports = "receiveOffboardingNotificationRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInCoexistenceOffboardingNotificationRequest",
            exports = "parseOffboardingNotificationRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxCoexistenceOffboardingNotificationResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("notification")) {
            return Optional.empty();
        }
        var offboarding = stanza.getChild("offboarding").orElse(null);
        if (offboarding == null) {
            return Optional.empty();
        }
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (from == null || !from.isServerJid(JidServer.user())) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("type", "hosted")) {
            return Optional.empty();
        }
        var productSurface = offboarding.getAttributeAsString("product_surface").orElse(null);
        if (productSurface == null
                || (!"ai_from_meta".equals(productSurface)
                && !"automation".equals(productSurface)
                && !"business_platform".equals(productSurface))) {
            return Optional.empty();
        }
        var providerInfo = SmaxCoexistenceOffboardingNotificationProviderInfo.of(offboarding).orElse(null);
        if (providerInfo == null) {
            return Optional.empty();
        }
        var id = stanza.getAttributeAsString("id").orElse(null);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.of(new SmaxCoexistenceOffboardingNotificationResponse(id, from, productSurface, providerInfo));
    }

    /**
     * Compares this projection with another for value equality.
     *
     * <p>Two projections are equal when their notification id, from JID, product surface, and
     * provider-info block are pairwise equal.
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
        var that = (SmaxCoexistenceOffboardingNotificationResponse) obj;
        return Objects.equals(this.notificationId, that.notificationId)
                && Objects.equals(this.notificationFrom, that.notificationFrom)
                && Objects.equals(this.offboardingProductSurface, that.offboardingProductSurface)
                && Objects.equals(this.offboardingProviderInfo, that.offboardingProviderInfo);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>Combines the hashes of the notification id, from JID, product surface, and provider-info
     * block.
     *
     * @return the hash code for this projection
     */
    @Override
    public int hashCode() {
        return Objects.hash(notificationId, notificationFrom, offboardingProductSurface, offboardingProviderInfo);
    }

    /**
     * Returns a debug string describing this projection.
     *
     * <p>Renders the notification id, from JID, product surface, and provider-info block.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxCoexistenceOffboardingNotificationResponse[notificationId=" + notificationId
                + ", notificationFrom=" + notificationFrom
                + ", offboardingProductSurface=" + offboardingProductSurface
                + ", offboardingProviderInfo=" + offboardingProviderInfo + ']';
    }
}
