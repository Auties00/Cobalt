package it.auties.whatsapp4j.common.listener;

import it.auties.whatsapp4j.common.api.AbstractWhatsappAPI;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to specify that a {@link IWhatsappListener} should be dected automatically by {@link AbstractWhatsappAPI}.
 * For this annotation to be recognized, the target class should implement {@link IWhatsappListener} and provide a no argument constructor.
 * If any of those conditions aren't met, a {@link RuntimeException} will be thrown.
 * In order for {@link AbstractWhatsappAPI} to autodetect listeners remeber to call {@link AbstractWhatsappAPI#autodetectListeners()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RegisterListener {
}
