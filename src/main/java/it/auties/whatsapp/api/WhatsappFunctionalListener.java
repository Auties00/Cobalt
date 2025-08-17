package it.auties.whatsapp.api;

/**
 * A convenient interface to provide functional overloads for {@link WhatsappListener}
 */
public sealed interface WhatsappFunctionalListener {
    /**
     * A functional listener that takes no parameters
     */
    non-sealed interface Empty extends WhatsappFunctionalListener {
        void accept();
    }

    /**
     * A functional listener that takes one parameter
     */
    non-sealed interface Unary<F> extends WhatsappFunctionalListener {
        void accept(F value);
    }

    /**
     * A functional listener that takes two parameters
     */
    non-sealed interface Binary<F, S> extends WhatsappFunctionalListener {
        void accept(F first, S second);
    }

    /**
     * A functional listener that takes three parameters
     */
    non-sealed interface Ternary<F, S, T> extends WhatsappFunctionalListener {
        void accept(F first, S second, T third);
    }
}
