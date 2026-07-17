package com.github.auties00.cobalt.registration.push;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevicePushClient;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Provides a {@link LinkedWhatsAppClientDevicePushClient} that emits empty token and code values without
 * contacting any push provider.
 *
 * <p>This client is the low-trust default returned by {@link LinkedWhatsAppClientDevicePushClient#noop()} when
 * the embedder has not wired a real FCM or APNS client. The {@code push_token} and {@code push_code}
 * form fields are still sent on the registration request, but with empty values that the WhatsApp
 * registration server tolerates as a low-trust signal. It fits device profiles that are not
 * push-capable, such as registering through SMS or voice verification only.
 *
 * @implNote
 * This implementation is stateless and therefore inherently thread-safe; a single shared
 * {@link #INSTANCE} is exposed instead of allocating one per call.
 */
public final class NoopMobilePushClient implements LinkedWhatsAppClientDevicePushClient {
    /**
     * Holds the cached unmodifiable view of every {@link ClientPlatformType} entry returned by
     * {@link #supportedPlatforms()}.
     *
     * <p>The no-op client accepts any device unconditionally, so the supported set spans every
     * platform the enum defines.
     *
     * @implNote
     * This implementation keeps a single immutable wrapper around an {@link EnumSet} so
     * {@link #supportedPlatforms()} avoids reallocating on every call.
     */
    private static final Set<ClientPlatformType> ALL_PLATFORMS =
            Collections.unmodifiableSet(EnumSet.allOf(ClientPlatformType.class));

    /**
     * Holds the process-wide shared instance of this client.
     *
     * <p>This field gives {@link LinkedWhatsAppClientDevicePushClient#noop()} a stable reference to return so
     * that the stateless no-op client is never reallocated.
     */
    public static final NoopMobilePushClient INSTANCE = new NoopMobilePushClient();

    /**
     * Constructs the singleton no-op push client.
     *
     * <p>The constructor is private because the class is a singleton; the only instance is reached
     * through {@link LinkedWhatsAppClientDevicePushClient#noop()} or {@link #INSTANCE}.
     */
    private NoopMobilePushClient() {

    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns every {@link ClientPlatformType} entry so the no-op client can be
     * paired with any device profile the registration code might pass in.
     */
    @Override
    public Set<ClientPlatformType> supportedPlatforms() {
        return ALL_PLATFORMS;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation does nothing: there is no underlying push service to authenticate against,
     * and {@link #isAuthenticated()} already reports {@code true} unconditionally.
     */
    @Override
    public void authenticate(LinkedWhatsAppClientDevice device) {
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@code true} so callers do not need to special-case the
     * no-op client; the empty token and code values it produces are valid even without any real
     * authentication.
     */
    @Override
    public boolean isAuthenticated() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the empty string so the {@code push_token} form field is still
     * emitted on the wire but with an empty value, matching the low-trust contract of the no-op
     * client.
     */
    @Override
    public String getPushToken() {
        return "";
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the empty string so the {@code push_code} form field is still
     * emitted on the wire but with an empty value, matching the low-trust contract of the no-op
     * client.
     */
    @Override
    public String getPushCode() {
        return "";
    }
}
