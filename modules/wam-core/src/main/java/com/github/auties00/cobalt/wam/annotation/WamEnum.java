package com.github.auties00.cobalt.wam.annotation;

import com.github.auties00.cobalt.wam.model.WamType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A program element annotated {@code @WamEnum} is an enumeration whose
 * constants carry integer indices used in the WAM binary wire format.
 *
 * <p>Each constant of a {@code @WamEnum}-annotated enum must be annotated
 * with {@link WamEnumConstant} to declare its wire index. On the wire
 * the constant is transmitted as a plain integer; the enum type itself
 * is not encoded.
 *
 * <p>Example usage:
 * <pre>{@code
 *     @WamEnum
 *     public enum DocumentType {
 *         @WamEnumConstant(1) OTHER,
 *         @WamEnumConstant(2) IMAGE,
 *         @WamEnumConstant(3) VIDEO;
 *     }
 * }</pre>
 *
 * @see WamEnumConstant
 * @see WamType#ENUM
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WamEnum {
}
