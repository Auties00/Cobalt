package com.github.auties00.cobalt.client.linked;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Defers the creation of a passkey authenticator until a ceremony actually needs it.
 *
 * <p>The system authenticator loads a native library (e.g. {@code libfido2} on Linux) when it is
 * created. The default verification handlers are built eagerly, so creating the native backend there
 * made every client fail on hosts without the library, even when no passkey ceremony ever runs. The
 * native backend is now created on the first {@link #assertCredential(Request)}; if it is unavailable,
 * that ceremony fails instead of the whole client.
 */
final class LazyPasskeyAuthenticator implements LinkedWhatsAppClientPasskeyAuthenticator {

    private final Supplier<LinkedWhatsAppClientPasskeyAuthenticator> factory;
    private volatile LinkedWhatsAppClientPasskeyAuthenticator delegate;

    LazyPasskeyAuthenticator(Supplier<LinkedWhatsAppClientPasskeyAuthenticator> factory) {
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
    }

    @Override
    public Assertion assertCredential(Request request) {
        var current = delegate;
        if (current == null) {
            synchronized (this) {
                current = delegate;
                if (current == null) {
                    current = factory.get();
                    delegate = current;
                }
            }
        }
        return current.assertCredential(request);
    }
}
