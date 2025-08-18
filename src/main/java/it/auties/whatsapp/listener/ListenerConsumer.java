package it.auties.whatsapp.listener;

public sealed interface ListenerConsumer {
    non-sealed interface Empty extends ListenerConsumer {
        void accept();
    }

    non-sealed interface Unary<F> extends ListenerConsumer {
        void accept(F value);
    }

    non-sealed interface Binary<F, S> extends ListenerConsumer {
        void accept(F first, S second);
    }

    non-sealed interface Ternary<F, S, T> extends ListenerConsumer {
        void accept(F first, S second, T third);
    }
}
