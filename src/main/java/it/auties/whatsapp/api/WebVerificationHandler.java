package it.auties.whatsapp.api;

/**
 * A utility sealed interface to represent methods that can be used to verify a WhatsappWeb Client
 */
public sealed interface WebVerificationHandler permits QrHandler, PairingCodeHandler {
}
