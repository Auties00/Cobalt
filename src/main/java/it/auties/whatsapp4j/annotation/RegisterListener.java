package it.auties.whatsapp4j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to specify that a {@code WhatsappListener} should be dected automatically by {@code WhatsappAPI}
 * For this annotation to be recognized, the target class should implement {@code WhatsappListener} and provide a no argument constructor
 * If any of those conditions aren't met, a {@code RuntimeException} will be thrown
 * In order for {@code WhatsappAPI} to autodetect listeners remeber to call {@code WhatsappAPI#autodetectListeners}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RegisterListener {

}
