package com.github.auties00.cobalt.wam.annotation;

import com.github.auties00.cobalt.wam.model.WamType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A program element annotated {@code @WamProperty} is a method within a
 * {@link WamEvent}-annotated interface that maps to one TLV-encoded field
 * in the WAM binary protocol.
 *
 * <p>Each annotated method must return an {@code Optional}, {@code OptionalLong},
 * or {@code OptionalDouble} wrapping the property value. The annotation
 * processor generates an implementation that stores raw nullable values
 * internally and wraps them at call time. Only non-{@code null} values are
 * written to the buffer during encoding.
 *
 * <p>Field identifiers below 256 are encoded as a single byte in the
 * wire format; identifiers 256 and above use a two-byte (uint16)
 * encoding with the wide-ID flag set.
 *
 * @see WamEvent
 * @see WamType
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WamProperty {
    /**
     * Returns the numeric field identifier assigned by WhatsApp.
     *
     * @return the field id
     */
    int index();

    /**
     * Returns the wire type that controls how this method's value is
     * encoded in the WAM binary protocol.
     *
     * @return the WAM type
     */
    WamType type();
}
