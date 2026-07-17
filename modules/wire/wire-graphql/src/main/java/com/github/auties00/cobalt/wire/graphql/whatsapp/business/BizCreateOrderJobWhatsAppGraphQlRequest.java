package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the graph.whatsapp.com GraphQL mutation that places a WhatsApp Business catalog order on behalf of a buyer.
 *
 * <p>The single {@code input} GraphQL variable wraps an {@code order} object. WhatsApp Web's
 * {@code WAWebBizCreateOrderJob.createOrderMD} fills it with the seller {@code jid}, a
 * {@code products} array of {@link Product} line items, and the optional
 * {@code direct_connection_encrypted_info} blob carried for direct-connection checkouts. Each line
 * item names the product {@code id}, {@code name} and {@code quantity}, and optionally its
 * {@code currency}, unit {@code price} (the {@code priceAmount1000} thousandths serialized as a
 * decimal string) and {@code variant_info} attributes. The graph.whatsapp.com endpoint returns the placed order under
 * {@code xwa_checkout_place_order}; the reply is consumed through
 * {@link BizCreateOrderJobWhatsAppGraphQlResponse}.
 *
 * @see BizCreateOrderJobWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCreateOrderJobMutation")
public final class BizCreateOrderJobWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCreateOrderJobMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26486627094287046";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCreateOrderJobMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCreateOrderJobMutation";

    /**
     * The {@code order.jid} field naming the seller the order is placed with, or {@code null} to omit
     * it.
     */
    private final Jid jid;

    /**
     * The {@code order.products} line items composing the order, or {@code null} to omit the
     * {@code products} array.
     */
    private final List<Product> products;

    /**
     * The {@code order.direct_connection_encrypted_info} blob carried for direct-connection checkouts,
     * or {@code null} to omit it.
     *
     * <p>This is an opaque server-defined encrypted payload, kept as a plain {@link String}.
     */
    private final String directConnectionEncryptedInfo;

    /**
     * Constructs a create-order mutation request carrying the seller, the order line items, and the
     * optional direct-connection encrypted info.
     *
     * <p>All values populate the {@code input.order} GraphQL object; a {@code null} {@code jid} omits
     * the {@code jid} field, a {@code null} {@code products} omits the {@code products} array, an empty
     * {@code products} serializes as an empty array, and a {@code null}
     * {@code directConnectionEncryptedInfo} omits the {@code direct_connection_encrypted_info} field.
     *
     * @param jid                           the seller {@link Jid}, or {@code null} to omit the field
     * @param products                      the order line items, or {@code null} to omit the
     *                                      {@code products} array
     * @param directConnectionEncryptedInfo the direct-connection encrypted info, or {@code null} to
     *                                      omit the field
     */
    public BizCreateOrderJobWhatsAppGraphQlRequest(Jid jid, List<Product> products, String directConnectionEncryptedInfo) {
        this.jid = jid;
        this.products = products;
        this.directConnectionEncryptedInfo = directConnectionEncryptedInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits {@code {"input": {"order": {"jid": ..., "products": [...],
     * "direct_connection_encrypted_info": ...}}}}, writing each field only when its value is non-null;
     * an empty {@code order} object is emitted when every field is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCreateOrderJob", exports = "createOrderMD",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            writer.writeName("order");
            writer.writeColon();
            writer.startObject();
            if (jid != null) {
                writer.writeName("jid");
                writer.writeColon();
                writer.writeString(jid.toString());
            }

            if (products != null) {
                writer.writeName("products");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < products.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    products.get(i).write(writer);
                }
                writer.endArray();
            }

            if (directConnectionEncryptedInfo != null) {
                writer.writeName("direct_connection_encrypted_info");
                writer.writeColon();
                writer.writeString(directConnectionEncryptedInfo);
            }
            writer.endObject();
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Describes a single line item within the {@code order.products} array.
     *
     * <p>Each line item names the product {@code id}, its display {@code name} and the ordered
     * {@code quantity}, and optionally its {@code currency}, unit {@code price} and
     * {@code variant_info} attribute selections.
     */
    public static final class Product {
        /**
         * The {@code id} of the ordered product.
         *
         * <p>This is an opaque catalog product identifier rather than a WhatsApp address, so it is kept
         * as a plain {@link String}.
         */
        private final String id;

        /**
         * The display {@code name} of the ordered product.
         */
        private final String name;

        /**
         * The ordered {@code quantity} of the product.
         */
        private final int quantity;

        /**
         * The {@code currency} of the unit price, or {@code null} to omit the field.
         */
        private final String currency;

        /**
         * The unit {@code price} in thousandths of the currency unit, or {@code null} to omit the
         * field.
         *
         * <p>WhatsApp Web serializes this as a decimal string of the thousandths value; Cobalt keeps
         * the raw thousandths and renders it the same way.
         */
        private final Long priceAmount1000;

        /**
         * The {@code variant_info} attribute selections, or {@code null} to omit the {@code variant_info}
         * object.
         */
        private final List<VariantProperty> variantProperties;

        /**
         * Constructs a line item from the product identity, the ordered quantity, and the optional
         * currency, price and variant attributes.
         *
         * @param id                the product identifier
         * @param name              the display name of the product
         * @param quantity          the ordered quantity
         * @param currency          the currency of the unit price, or {@code null} to omit the field
         * @param priceAmount1000   the unit price in thousandths of the currency unit, or {@code null}
         *                          to omit the field
         * @param variantProperties the variant attribute selections, or {@code null} to omit the
         *                          {@code variant_info} object
         */
        public Product(String id, String name, int quantity, String currency, Long priceAmount1000, List<VariantProperty> variantProperties) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.currency = currency;
            this.priceAmount1000 = priceAmount1000;
            this.variantProperties = variantProperties;
        }

        /**
         * Returns the product identifier.
         *
         * @return the product identifier, may be {@code null}
         */
        public String id() {
            return id;
        }

        /**
         * Returns the display name of the product.
         *
         * @return the product name, may be {@code null}
         */
        public String name() {
            return name;
        }

        /**
         * Returns the ordered quantity.
         *
         * @return the quantity
         */
        public int quantity() {
            return quantity;
        }

        /**
         * Returns the currency of the unit price.
         *
         * @return the currency, may be {@code null}
         */
        public String currency() {
            return currency;
        }

        /**
         * Returns the unit price in thousandths of the currency unit.
         *
         * @return the price in thousandths, may be {@code null}
         */
        public Long priceAmount1000() {
            return priceAmount1000;
        }

        /**
         * Returns the variant attribute selections.
         *
         * @return the variant properties, may be {@code null}
         */
        public List<VariantProperty> variantProperties() {
            return variantProperties;
        }

        /**
         * Writes this line item as an order-product object onto the given JSON writer.
         *
         * <p>The {@code id}, {@code name}, {@code currency}, {@code price} and {@code variant_info}
         * members are written only when their values are non-null; the {@code quantity} is always
         * written; the {@code price} is rendered as the decimal string of the thousandths value; and
         * {@code variant_info} is emitted only when at least one variant property is present, as
         * {@code {"properties": {"properties": [{"nameAttr": ..., "valueAttr": ...}, ...]}}}.
         *
         * @param writer the JSON writer to append to
         */
        private void write(JSONWriter writer) {
            writer.startObject();
            if (id != null) {
                writer.writeName("id");
                writer.writeColon();
                writer.writeString(id);
            }

            if (name != null) {
                writer.writeName("name");
                writer.writeColon();
                writer.writeString(name);
            }

            writer.writeName("quantity");
            writer.writeColon();
            writer.writeInt32(quantity);
            if (currency != null) {
                writer.writeName("currency");
                writer.writeColon();
                writer.writeString(currency);
            }

            if (priceAmount1000 != null) {
                writer.writeName("price");
                writer.writeColon();
                writer.writeString(String.valueOf(priceAmount1000.longValue()));
            }

            if (variantProperties != null && !variantProperties.isEmpty()) {
                writer.writeName("variant_info");
                writer.writeColon();
                writer.startObject();
                writer.writeName("properties");
                writer.writeColon();
                writer.startObject();
                writer.writeName("properties");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < variantProperties.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    variantProperties.get(i).write(writer);
                }
                writer.endArray();
                writer.endObject();
                writer.endObject();
            }
            writer.endObject();
        }
    }

    /**
     * Describes a single variant attribute selection within a line item's {@code variant_info}.
     *
     * <p>WhatsApp Web emits each selection as {@code {nameAttr, valueAttr}}, defaulting either to the
     * empty string when the corresponding attribute is absent.
     */
    public static final class VariantProperty {
        /**
         * The {@code nameAttr} of the variant attribute, or {@code null} to emit the empty string.
         */
        private final String name;

        /**
         * The {@code valueAttr} of the variant attribute, or {@code null} to emit the empty string.
         */
        private final String value;

        /**
         * Constructs a variant property from the attribute name and value.
         *
         * @param name  the variant attribute name, or {@code null} to emit the empty string
         * @param value the variant attribute value, or {@code null} to emit the empty string
         */
        public VariantProperty(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Returns the variant attribute name.
         *
         * @return the attribute name, may be {@code null}
         */
        public String name() {
            return name;
        }

        /**
         * Returns the variant attribute value.
         *
         * @return the attribute value, may be {@code null}
         */
        public String value() {
            return value;
        }

        /**
         * Writes this variant property as a {@code {"nameAttr": ..., "valueAttr": ...}} object onto the
         * given JSON writer.
         *
         * <p>A {@code null} name or value is written as the empty string, mirroring WhatsApp Web's
         * {@code nameAttr}/{@code valueAttr} defaulting.
         *
         * @param writer the JSON writer to append to
         */
        private void write(JSONWriter writer) {
            writer.startObject();
            writer.writeName("nameAttr");
            writer.writeColon();
            writer.writeString(name != null ? name : "");
            writer.writeName("valueAttr");
            writer.writeColon();
            writer.writeString(value != null ? value : "");
            writer.endObject();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WhatsAppGraphQlEnvironment environment() {
        return WhatsAppGraphQlEnvironment.CATALOG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}
