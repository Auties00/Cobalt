package com.github.auties00.cobalt.wire.stanza.smax.account;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the outbound {@code <iq xmlns="w:pay">} stanza for registering a
 * Brazilian custom payment method against the connected device.
 *
 * <p>The method being registered is either pay-on-delivery or PIX. The relay
 * either persists the method and replies with a
 * {@link SmaxBrPaymentCreateCustomPaymentMethodResponse.Success}, or rejects
 * with a {@link SmaxBrPaymentCreateCustomPaymentMethodResponse.IqError}.
 *
 * @implNote
 * This implementation collapses the WA Web smax mixin chain (set-IQ +
 * metadata-info + metadata-mixin) into a single {@link #toStanza()} call. The
 * optional {@code <metadata_info>} child is folded in only when the caller
 * passed a non-{@code null} metadata map.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBrPaymentCreateCustomPaymentMethodRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutBrPaymentSetIQMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBrPaymentCustomPaymentMethodMetaDataInfoMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBrPaymentCustomPaymentMethodMetaDataMixin")
public final class SmaxBrPaymentCreateCustomPaymentMethodRequest implements SmaxStanza.Request {
    /**
     * Holds the opaque device-id routed into {@code <account device_id=...>}.
     *
     * <p>Carries the device value passed by the Brazilian-payments
     * registration flow.
     */
    private final String accountDeviceId;

    /**
     * Holds the custom-payment-method type literal; one of
     * {@code "pay_on_delivery"} or {@code "pix_key"}.
     *
     * <p>The relay validates this against its method-type enum; the request
     * side trusts the caller.
     */
    private final String customPaymentMethodType;

    /**
     * Holds the optional {@code update} marker on
     * {@code <custom_payment_method/>}.
     *
     * <p>Set when re-registering an existing method to refresh server-side
     * metadata; {@code null} for a brand-new registration.
     */
    private final String customPaymentMethodUpdate;

    /**
     * Holds the optional {@code flow} marker on
     * {@code <custom_payment_method/>}; one of {@code "p2m"} or {@code "p2p"}.
     *
     * <p>The relay validates this against its flow enum.
     */
    private final String customPaymentMethodFlow;

    /**
     * Holds the optional 1..5 metadata key-value pairs, preserved in insertion
     * order; {@code null} omits the {@code <metadata_info>} child entirely.
     *
     * <p>A non-{@code null} value opts in to the metadata-info payload; the
     * relay applies the entries as method-specific fields (e.g. PIX-key
     * contents).
     */
    private final Map<String, String> metadata;

    /**
     * Constructs a Brazilian-payments method-registration request for dispatch
     * through the smax send pipeline.
     *
     * @implNote
     * This implementation defensively copies the metadata map into a
     * {@link LinkedHashMap} wrapped via {@link Collections#unmodifiableMap(Map)}
     * so insertion order is preserved on subsequent {@link #toStanza()} fanout.
     *
     * @param accountDeviceId           the device-id; never {@code null}
     * @param customPaymentMethodType   the method-type literal; never
     *                                  {@code null}
     * @param customPaymentMethodUpdate the optional update marker; may be
     *                                  {@code null}
     * @param customPaymentMethodFlow   the optional flow marker; may be
     *                                  {@code null}
     * @param metadata                  the optional metadata map; must contain
     *                                  1..5 entries when non-{@code null};
     *                                  {@code null} omits the
     *                                  {@code <metadata_info>} child
     * @throws NullPointerException     if {@code accountDeviceId} or
     *                                  {@code customPaymentMethodType} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code metadata} is non-{@code null}
     *                                  and empty or exceeds {@code 5} entries
     */
    public SmaxBrPaymentCreateCustomPaymentMethodRequest(String accountDeviceId,
                   String customPaymentMethodType,
                   String customPaymentMethodUpdate,
                   String customPaymentMethodFlow,
                   Map<String, String> metadata) {
        this.accountDeviceId = Objects.requireNonNull(accountDeviceId, "accountDeviceId cannot be null");
        this.customPaymentMethodType = Objects.requireNonNull(customPaymentMethodType, "customPaymentMethodType cannot be null");
        this.customPaymentMethodUpdate = customPaymentMethodUpdate;
        this.customPaymentMethodFlow = customPaymentMethodFlow;
        if (metadata == null) {
            this.metadata = null;
        } else {
            if (metadata.isEmpty() || metadata.size() > 5) {
                throw new IllegalArgumentException("metadata must contain 1..5 entries");
            }
            this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
        }
    }

    /**
     * Returns the account device-id.
     *
     * <p>Read by {@link #toStanza()} when stamping the
     * {@code <account device_id=...>} attribute.
     *
     * @return the id; never {@code null}
     */
    public String accountDeviceId() {
        return accountDeviceId;
    }

    /**
     * Returns the custom-payment-method type literal.
     *
     * <p>Read by {@link #toStanza()} when stamping the
     * {@code <custom_payment_method type=...>} attribute.
     *
     * @return the type; never {@code null}
     */
    public String customPaymentMethodType() {
        return customPaymentMethodType;
    }

    /**
     * Returns the optional {@code update} marker.
     *
     * <p>Read by {@link #toStanza()} to decide whether to stamp
     * {@code <custom_payment_method update=...>}.
     *
     * @return an {@link Optional} carrying the marker, or
     *         {@link Optional#empty()} when omitted
     */
    public Optional<String> customPaymentMethodUpdate() {
        return Optional.ofNullable(customPaymentMethodUpdate);
    }

    /**
     * Returns the optional {@code flow} marker.
     *
     * <p>Read by {@link #toStanza()} to decide whether to stamp
     * {@code <custom_payment_method flow=...>}.
     *
     * @return an {@link Optional} carrying the marker, or
     *         {@link Optional#empty()} when omitted
     */
    public Optional<String> customPaymentMethodFlow() {
        return Optional.ofNullable(customPaymentMethodFlow);
    }

    /**
     * Returns the optional metadata map.
     *
     * <p>A present value tells {@link #toStanza()} to emit a
     * {@code <metadata_info>} child with one
     * {@code <metadata key=" " value=" "/>} per entry, in insertion order.
     *
     * @return an {@link Optional} carrying the unmodifiable map, or
     *         {@link Optional#empty()} when the caller omitted the metadata
     *         child entirely
     */
    public Optional<Map<String, String>> metadata() {
        return Optional.ofNullable(metadata);
    }

    /**
     * Builds the outbound {@code <iq>} stanza ready for dispatch.
     *
     * <p>The stanza has shape
     * {@snippet lang=xml :
     * <iq xmlns="w:pay" type="set" to="s.whatsapp.net">
     *   <account action="create-custom-payment-method" device_id="..." country="BR">
     *     <custom_payment_method type="pay_on_delivery|pix_key" update? flow?>
     *       <metadata_info>?
     *         <metadata key=" " value=" "/>
     *         ...
     *       </metadata_info>
     *     </custom_payment_method>
     *   </account>
     * </iq>
     * }
     *
     * @implNote
     * This implementation folds the WA Web optional merge of
     * {@code <metadata_info>} into a single {@code if (metadata != null)}
     * branch; the metadata map's {@link LinkedHashMap} backing preserves the
     * caller's insertion order in the emitted children.
     *
     * @return a {@link StanzaBuilder} carrying the partially-built IQ envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBrPaymentCreateCustomPaymentMethodRequest",
            exports = "makeCreateCustomPaymentMethodRequest",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBrPaymentSetIQMixin",
            exports = "mergeSetIQMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBrPaymentCustomPaymentMethodMetaDataInfoMixin",
            exports = "mergeCustomPaymentMethodMetaDataInfoMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBrPaymentCustomPaymentMethodMetaDataMixin",
            exports = "makeCustomPaymentMethodMetaDataMetadata",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBrPaymentCustomPaymentMethodMetaDataMixin",
            exports = "mergeCustomPaymentMethodMetaDataMixin",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBrPaymentEnums",
            exports = "ENUM_PAYONDELIVERY_PIXKEY",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxInBrPaymentEnums",
            exports = "ENUM_P2M_P2P",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var cpmBuilder = new StanzaBuilder()
                .description("custom_payment_method")
                .attribute("type", customPaymentMethodType);
        if (customPaymentMethodUpdate != null) {
            cpmBuilder.attribute("update", customPaymentMethodUpdate);
        }
        if (customPaymentMethodFlow != null) {
            cpmBuilder.attribute("flow", customPaymentMethodFlow);
        }
        if (metadata != null) {
            var metadataChildren = new Stanza[metadata.size()];
            var i = 0;
            for (var entry : metadata.entrySet()) {
                metadataChildren[i++] = new StanzaBuilder()
                        .description("metadata")
                        .attribute("key", entry.getKey())
                        .attribute("value", entry.getValue())
                        .build();
            }
            var metadataInfo = new StanzaBuilder()
                    .description("metadata_info")
                    .content(metadataChildren)
                    .build();
            cpmBuilder.content(metadataInfo);
        }
        var customPaymentMethod = cpmBuilder.build();
        var account = new StanzaBuilder()
                .description("account")
                .attribute("action", "create-custom-payment-method")
                .attribute("device_id", accountDeviceId)
                .attribute("country", "BR")
                .content(customPaymentMethod)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:pay")
                .attribute("type", "set")
                .attribute("to", JidServer.user())
                .content(account);
    }

    /**
     * Compares this request to another for value equality on every payload
     * field.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a
     *         {@link SmaxBrPaymentCreateCustomPaymentMethodRequest} with
     *         identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxBrPaymentCreateCustomPaymentMethodRequest) obj;
        return Objects.equals(this.accountDeviceId, that.accountDeviceId)
                && Objects.equals(this.customPaymentMethodType, that.customPaymentMethodType)
                && Objects.equals(this.customPaymentMethodUpdate, that.customPaymentMethodUpdate)
                && Objects.equals(this.customPaymentMethodFlow, that.customPaymentMethodFlow)
                && Objects.equals(this.metadata, that.metadata);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountDeviceId, customPaymentMethodType, customPaymentMethodUpdate,
                customPaymentMethodFlow, metadata);
    }

    /**
     * Returns a debug-friendly representation of this request.
     *
     * <p>The format is intended for logging and is not part of any contract.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxBrPaymentCreateCustomPaymentMethodRequest[accountDeviceId=" + accountDeviceId
                + ", customPaymentMethodType=" + customPaymentMethodType
                + ", customPaymentMethodUpdate=" + customPaymentMethodUpdate
                + ", customPaymentMethodFlow=" + customPaymentMethodFlow
                + ", metadata=" + metadata + ']';
    }
}
