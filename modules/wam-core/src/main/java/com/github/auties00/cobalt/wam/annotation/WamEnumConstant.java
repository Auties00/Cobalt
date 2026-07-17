package com.github.auties00.cobalt.wam.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A program element annotated {@code @WamEnumConstant} is an enum
 * constant within a {@link WamEnum}-annotated enumeration that declares
 * the integer index transmitted on the WAM binary wire.
 *
 * <p>The annotation processor reads this value at compile time and
 * generates a direct switch expression in the encoder, avoiding any
 * runtime reflection or virtual dispatch.
 *
 * <p>Example usage:
 * <pre>{@code
 *     @WamEnum
 *     public enum MediaType {
 *         @WamEnumConstant(1)  NONE,
 *         @WamEnumConstant(2)  PHOTO,
 *         @WamEnumConstant(3)  VIDEO,
 *         @WamEnumConstant(4)  AUDIO;
 *     }
 * }</pre>
 *
 * @see WamEnum
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WamEnumConstant {
    /**
     * Returns the numeric index transmitted on the wire for this enum
     * constant.
     *
     * @return the wire index
     */
    int value();
}
