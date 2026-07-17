package com.github.auties00.cobalt.wire.linked.business.order;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A single {@code [name, value]} variant property attached to a
 * {@link BusinessOrderItem}.
 *
 * <p>WhatsApp Business catalogs let merchants describe per-item product
 * variations (size, colour, material, etc.). When a customer places an
 * order, each line item carries a list of these variant property pairs
 * so the merchant can identify exactly which variant of the catalog
 * product was bought. Both the property name and value are required and
 * non-empty.
 */
@ProtobufMessage(name = "BusinessOrderItemProperty")
public final class BusinessOrderItemProperty {
    /**
     * The variant property name, for example {@code "size"} or
     * {@code "colour"}. Always populated and never blank.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String name;

    /**
     * The variant property value, for example {@code "L"} or
     * {@code "blue"}. Always populated and never blank.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String value;

    /**
     * Constructs a new {@code BusinessOrderItemProperty} with the given
     * non-{@code null} name and value.
     *
     * @param name  the variant property name; never {@code null}
     * @param value the variant property value; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    BusinessOrderItemProperty(String name, String value) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    /**
     * Returns the variant property name (for example {@code "size"}).
     *
     * @return the name; never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the variant property value (for example {@code "L"}).
     *
     * @return the value; never {@code null}
     */
    public String value() {
        return value;
    }

    /**
     * Sets the variant property name.
     *
     * @param name the name to set
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    /**
     * Sets the variant property value.
     *
     * @param value the value to set
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public void setValue(String value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessOrderItemProperty) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return "BusinessOrderItemProperty[" +
                "name=" + name + ", " +
                "value=" + value + ']';
    }
}
