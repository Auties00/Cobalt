package com.github.auties00.cobalt.passkey;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientPasskeyAuthenticator;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.warden.HybridAssertion;
import com.github.auties00.warden.HybridAssertionListener;
import com.github.auties00.warden.PasskeyAssertion;
import com.github.auties00.warden.PasskeyRequest;
import com.github.auties00.warden.WardenAuthenticator;
import com.github.auties00.warden.exception.PasskeyException;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Authenticator that asserts a passkey over Warden's cross-device hybrid transport, driving the FIDO
 * "sign in with a phone" flow so the user approves the ceremony on a nearby device.
 *
 * <p>Warden's native platform path (Windows Hello, macOS AuthenticationServices, libfido2) needs a
 * code-signing entitlement on macOS that a plain JVM cannot carry, so it cannot serve every desktop.
 * The hybrid transport is entitlement-free on every operating system: Warden shows a {@code FIDO:/...}
 * URL, the user scans it as a QR code with a phone that holds the {@code whatsapp.com} passkey, a
 * Bluetooth proximity check and an encrypted tunnel are established, and the phone produces the
 * assertion. This bridge drives that flow through {@link WardenAuthenticator#getHybridAssertion} and
 * maps its result onto the {@link LinkedWhatsAppClientPasskeyAuthenticator} contract, so a desktop
 * application can satisfy WhatsApp's passkey ceremonies without a browser or a platform entitlement.
 *
 * @implNote The hybrid ceremony is asynchronous (it reports the QR payload, progress, and outcome
 * through a {@link HybridAssertionListener}), whereas {@link #assertCredential(Request)} is
 * synchronous, so this implementation blocks the calling virtual thread on a single-slot handoff until
 * the listener delivers an assertion or a failure, cancelling the in-flight ceremony (through the
 * {@link HybridAssertion} handle) on timeout or interruption. Warden's Bluetooth scan goes through the
 * foreign-function and memory API, so an application running Cobalt as a named module must grant native
 * access to the {@code com.github.auties00.warden} module.
 */
public final class SystemPasskeyAuthenticator implements LinkedWhatsAppClientPasskeyAuthenticator {
    /**
     * The logger for {@link SystemPasskeyAuthenticator}.
     */
    private static final System.Logger LOGGER = Log.get(SystemPasskeyAuthenticator.class);

    /**
     * The slack added to the request timeout when waiting for a terminal callback, letting Warden's own
     * ceremony timeout fire first and deliver its failure rather than tripping this safety net.
     */
    private static final long TIMEOUT_SLACK_MILLIS = 5_000L;

    /**
     * The Warden authenticator driving the cross-device hybrid transport.
     */
    private final WardenAuthenticator authenticator;

    /**
     * The consumer the {@code FIDO:/...} QR payload is handed to for rendering.
     */
    private final Consumer<String> onQrCode;

    /**
     * Constructs a bridge over the given Warden authenticator and QR consumer.
     *
     * @param authenticator the host-platform Warden authenticator
     * @param onQrCode      the consumer the {@code FIDO:/...} QR payload is handed to
     */
    private SystemPasskeyAuthenticator(WardenAuthenticator authenticator, Consumer<String> onQrCode) {
        this.authenticator = authenticator;
        this.onQrCode = onQrCode;
    }

    /**
     * Returns an authenticator that drives Warden's cross-device hybrid transport, handing every
     * ceremony's {@code FIDO:/...} QR payload to the given consumer.
     *
     * <p>The Warden authenticator is resolved eagerly, so an unsupported platform or an uninitialisable
     * service surfaces here rather than on the first ceremony.
     *
     * @param onQrCode the consumer the {@code FIDO:/...} QR payload is handed to for rendering; never
     *                 {@code null}
     * @return a hybrid-transport authenticator
     * @throws NullPointerException          if {@code onQrCode} is {@code null}
     * @throws UnsupportedOperationException if the host platform has no supported native passkey service
     * @throws IllegalStateException         if the platform service is present but cannot be initialised
     */
    public static SystemPasskeyAuthenticator create(Consumer<String> onQrCode) {
        Objects.requireNonNull(onQrCode, "onQrCode must not be null");
        try {
            return new SystemPasskeyAuthenticator(WardenAuthenticator.create(), onQrCode);
        } catch (PasskeyException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "host passkey service could not be initialised", exception);
            throw new IllegalStateException("The host passkey service could not be initialised", exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation maps the request onto a Warden {@link PasskeyRequest}, launches the
     * hybrid ceremony, and blocks on a single-slot handoff fed by a {@link HybridAssertionListener}: the
     * QR payload is forwarded to the configured consumer, device-detection and tunnel progress are
     * logged, and the terminal {@link PasskeyAssertion} or {@link PasskeyException} is handed back. A
     * failure, an interruption, or a wait that outlasts the request timeout plus a small slack cancels
     * the in-flight ceremony and is rethrown as an {@link IllegalStateException} so the caller aborts the
     * ceremony or logs the session out.
     */
    @Override
    public Assertion assertCredential(Request request) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting hybrid passkey ceremony for {0}", request.relyingPartyId());
        var passkeyRequest = new PasskeyRequest(
                request.relyingPartyId(),
                request.challenge(),
                request.allowedCredentialIds(),
                toWarden(request.userVerification()),
                request.timeout(),
                request.prfEvalFirst(),
                request.origin());
        var handoff = new ArrayBlockingQueue<Object>(1);
        var listener = new HybridAssertionListener() {
            @Override
            public void onQrCode(String fidoUrl) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "hybrid passkey ceremony: qr code issued");
                onQrCode.accept(fidoUrl);
            }

            @Override
            public void onDeviceDetected() {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "hybrid passkey ceremony: nearby device detected");
            }

            @Override
            public void onConnecting() {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "hybrid passkey ceremony: establishing encrypted tunnel");
            }

            @Override
            public void onAssertion(PasskeyAssertion assertion) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "hybrid passkey ceremony: assertion received");
                handoff.offer(assertion);
            }

            @Override
            public void onFailure(PasskeyException failure) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "hybrid passkey ceremony failed", failure);
                handoff.offer(failure);
            }
        };

        HybridAssertion ceremony;
        try {
            ceremony = authenticator.getHybridAssertion(passkeyRequest, listener);
        } catch (PasskeyException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "hybrid passkey ceremony could not be started", exception);
            throw new IllegalStateException("The host passkey ceremony could not be started", exception);
        }
        try (ceremony) {
            var outcome = handoff.poll(request.timeout().toMillis() + TIMEOUT_SLACK_MILLIS, TimeUnit.MILLISECONDS);
            return switch (outcome) {
                case PasskeyAssertion assertion -> {
                    if (Log.INFO) LOGGER.log(Level.INFO, "hybrid passkey ceremony completed");
                    yield new Assertion(
                            assertion.credentialId(),
                            assertion.authenticatorData(),
                            assertion.clientDataJson(),
                            assertion.signature(),
                            assertion.userHandle(),
                            assertion.prfOutput());
                }
                case PasskeyException failure -> {
                    if (Log.ERROR) LOGGER.log(Level.ERROR, "hybrid passkey ceremony did not produce an assertion", failure);
                    throw new IllegalStateException("The host passkey ceremony did not produce an assertion", failure);
                }
                case null -> {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "hybrid passkey ceremony timed out");
                    throw new IllegalStateException("The host passkey ceremony timed out before producing an assertion");
                }
                default -> {
                    if (Log.ERROR) LOGGER.log(Level.ERROR, "hybrid passkey ceremony produced an unexpected result");
                    throw new IllegalStateException("The host passkey ceremony produced an unexpected result");
                }
            };
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (Log.WARNING) LOGGER.log(Level.WARNING, "hybrid passkey ceremony interrupted");
            throw new IllegalStateException("The host passkey ceremony was interrupted", exception);
        }
    }

    /**
     * Maps Cobalt's user-verification preference onto Warden's equivalent.
     *
     * @param userVerification the Cobalt preference
     * @return the matching Warden preference
     */
    private static com.github.auties00.warden.UserVerification toWarden(UserVerification userVerification) {
        return switch (userVerification) {
            case REQUIRED -> com.github.auties00.warden.UserVerification.REQUIRED;
            case PREFERRED -> com.github.auties00.warden.UserVerification.PREFERRED;
            case DISCOURAGED -> com.github.auties00.warden.UserVerification.DISCOURAGED;
        };
    }
}
