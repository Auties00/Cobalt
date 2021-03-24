package it.auties.whatsapp4j.listener;

import it.auties.whatsapp4j.api.WhatsappAPI;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to specify that a {@link WhatsappListener} should be dected automatically by {@link WhatsappAPI}.
 * For this annotation to be recognized, the target class should implement {@link WhatsappListener} and provide a no argument constructor.
 * If any of those conditions aren't met, a {@link RuntimeException} will be thrown.
 * In order for {@link WhatsappAPI} to autodetect listeners remeber to call {@link WhatsappAPI#autodetectListeners()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RegisterListener {

}
