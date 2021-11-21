package it.auties.whatsapp.api;

import io.github.classgraph.ClassGraph;
import it.auties.whatsapp.utils.Validate;
import lombok.experimental.UtilityClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An annotation used to specify that a {@link WhatsappListener} should be dected automatically by {@link Whatsapp}.
 * For this annotation to be recognized, the target class should implement {@link WhatsappListener} and provide a no argument constructor.
 * If any of those conditions aren't met, a {@link RuntimeException} will be thrown.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RegisterListener {
    @UtilityClass
    class Scanner {
        public List<WhatsappListener> scan(Whatsapp whatsapp){
            return new ClassGraph()
                    .enableClassInfo()
                    .enableAnnotationInfo()
                    .scan()
                    .getClassesWithAnnotation(RegisterListener.class)
                    .loadClasses()
                    .stream()
                    .map(clazz -> initialize(clazz, whatsapp))
                    .toList();
        }

        private WhatsappListener initialize(Class<?> listener, Whatsapp whatsapp){
            Validate.isTrue(WhatsappListener.class.isAssignableFrom(listener),
                    "Cannot initialize listener at %s: cannot register classes that don't implement WhatsappListener", listener.getName(), IllegalArgumentException.class);
            try {
                return (WhatsappListener) listener.getConstructor(createParameters(whatsapp))
                        .newInstance(createArguments(whatsapp));
            }catch (NoSuchMethodException noArgsConstructorException){
                if(whatsapp != null){
                    return initialize(listener, null);
                }

                throw new NoSuchElementException("Cannot initialize listener at %s: no applicable constructor was found. Create a public no args constructor or Whatsapp constructor"
                        .formatted(listener.getName()), noArgsConstructorException);
            }catch (IllegalAccessException accessException){
                throw new IllegalArgumentException("Cannot initialize listener at %s: inaccessible module. Mark %s as open in order to allow registration"
                        .formatted(listener.getName(), listener.getModule().getName()), accessException);
            }catch (Throwable invocationException){
                throw new RuntimeException("Cannot initialize listener at %s: an unknown exception was thrown"
                        .formatted(listener.getName()), invocationException);
            }
        }

        private Object[] createArguments(Whatsapp whatsapp) {
            return whatsapp == null ? new Object[0]
                    : new Object[]{whatsapp};
        }

        private Class<?>[] createParameters(Whatsapp whatsapp) {
            return whatsapp == null ? new Class[0]
                    : new Class[]{Whatsapp.class};
        }
    }
}
