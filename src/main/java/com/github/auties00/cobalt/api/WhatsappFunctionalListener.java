package com.github.auties00.cobalt.api;

/**
 * A convenient interface to provide functional overloads for {@link WhatsappListener}
 */
public sealed interface WhatsappFunctionalListener {
    /**
     * A functional listener that takes no parameters
     */
    @FunctionalInterface
    non-sealed interface Empty extends WhatsappFunctionalListener {
        void accept();
    }

    /**
     * A functional listener that takes one parameter
     */
    @FunctionalInterface
    non-sealed interface Unary<F> extends WhatsappFunctionalListener {
        void accept(F value);
    }

    /**
     * A functional listener that takes two parameters
     */
    @FunctionalInterface
    non-sealed interface Binary<F, S> extends WhatsappFunctionalListener {
        void accept(F first, S second);
    }

    /**
     * A functional listener that takes three parameters
     */
    @FunctionalInterface
    non-sealed interface Ternary<F, S, T> extends WhatsappFunctionalListener {
        void accept(F first, S second, T third);
    }
}
