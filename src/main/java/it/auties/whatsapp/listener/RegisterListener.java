package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to specify that a {@link Listener} should be dected automatically by
 * {@link Whatsapp}. For this annotation to be recognized, the target class should implement
 * {@link Listener} and provide a no argument constructor. If any of those conditions aren't met, a
 * {@link RuntimeException} will be thrown.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RegisterListener {

}
