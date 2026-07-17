package com.github.auties00.cobalt.wire.cloud.commerce;

/**
 * A monetary amount expressed in minor units with a decimal offset.
 *
 * <p>Cloud API payment amounts are sent as an integer {@code value} in the currency's minor unit
 * together with an {@code offset} that fixes the decimal position. With the conventional offset of
 * {@code 100} a {@code value} of {@code 21000} represents {@code 210.00}.
 *
 * @param value  the amount in minor units
 * @param offset the decimal offset, conventionally {@code 100}
 */
public record CloudOrderAmount(long value, int offset) {
}
