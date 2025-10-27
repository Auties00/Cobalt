package com.github.auties00.cobalt.client.listener;

/**
 * A convenient interface to provide functional overloads for {@link WhatsAppClientListener}
 */
public sealed interface WhatsappClientListenerConsumer {
    /**
     * A functional listener that takes no parameters
     */
    @FunctionalInterface
    non-sealed interface Empty extends WhatsappClientListenerConsumer {
        void accept();
    }

    /**
     * A functional listener that takes one parameter
     */
    @FunctionalInterface
    non-sealed interface Unary<F> extends WhatsappClientListenerConsumer {
        void accept(F value);
    }

    /**
     * A functional listener that takes two parameters
     */
    @FunctionalInterface
    non-sealed interface Binary<F, S> extends WhatsappClientListenerConsumer {
        void accept(F first, S second);
    }

    /**
     * A functional listener that takes three parameters
     */
    @FunctionalInterface
    non-sealed interface Ternary<F, S, T> extends WhatsappClientListenerConsumer {
        void accept(F first, S second, T third);
    }
}
