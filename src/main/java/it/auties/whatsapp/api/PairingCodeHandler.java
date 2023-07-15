package it.auties.whatsapp.api;

import java.util.function.Consumer;

/**
 * This interface allows to consume a pairing code sent by WhatsappWeb
 */
@FunctionalInterface
@SuppressWarnings("unused")
public non-sealed interface PairingCodeHandler extends Consumer<String>, WebVerificationSupport {
    /**
     * Prints the pairing code to the terminal
     */
    static PairingCodeHandler toTerminal() {
        return System.out::println;
    }

    /**
     * Discards the pairing code
     */
    static PairingCodeHandler discarding() {
        return (ignored) -> {};
    }
}