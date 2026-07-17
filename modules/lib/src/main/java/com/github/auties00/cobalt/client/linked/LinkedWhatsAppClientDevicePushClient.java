package com.github.auties00.cobalt.client.linked;

import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.registration.push.NoopMobilePushClient;
import com.github.auties00.cobalt.registration.push.apns.ApnsClient;
import com.github.auties00.cobalt.registration.push.fcm.FcmClient;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.util.Set;

/**
 * Bridges Cobalt and an embedder-supplied push-notification service that
 * produces the device-side data the registration body advertises so the
 * WhatsApp registration server can deliver verification codes via silent
 * push.
 *
 * <p>The native WhatsApp mobile clients embed two push-related fields in
 * their registration request bodies:
 * <ol>
 *   <li>a <b>device push token</b> obtained from Firebase Cloud Messaging
 *       on Android or Apple Push Notification Service on iOS, advertised
 *       so the registration server knows where to silent-push the
 *       verification code if the user picks a push-based verification
 *       method;</li>
 *   <li>the <b>verification code</b> received via that silent push,
 *       extracted from the FCM data message or APNS payload and echoed
 *       back to the server on the next code-submission call.</li>
 * </ol>
 * Both values are obtained from an authenticated network session with the
 * vendor's push service, which Cobalt does not have direct access to. This
 * interface is the seam through which an embedding application supplies
 * them.
 *
 * <p>Unlike {@link LinkedWhatsAppClientDeviceAttestor}, this interface is not sealed
 * and not platform-specific at the type level: an FCM-based implementation
 * is used for Android devices and an APNS-based implementation for iOS
 * devices, but both expose the same surface and are kept stateful by the
 * embedder so neither method needs the live
 * {@link LinkedWhatsAppStore} as input.
 *
 * @apiNote
 * Implementations are not required to be stateless or thread-safe: the
 * registration code calls each method sequentially from the thread that
 * drives the registration ceremony and never concurrently.
 */
public interface LinkedWhatsAppClientDevicePushClient extends AutoCloseable {
    /**
     * Returns a fresh, unauthenticated APNS-backed push client.
     *
     * <p>The caller must drive {@link #authenticate(LinkedWhatsAppClientDevice)} with
     * an iOS or iOS-business device before any read-only accessor becomes
     * usable.
     *
     * @return a new unauthenticated APNS-backed push client
     */
    static LinkedWhatsAppClientDevicePushClient apns() {
        return ApnsClient.newSession();
    }

    /**
     * Returns a fresh, unauthenticated FCM-backed push client.
     *
     * <p>The caller must drive {@link #authenticate(LinkedWhatsAppClientDevice)} with
     * an Android or Android-business device before any read-only accessor
     * becomes usable.
     *
     * @return a new unauthenticated FCM-backed push client
     */
    static LinkedWhatsAppClientDevicePushClient fcm() {
        return FcmClient.newSession();
    }

    /**
     * Returns a no-op push client that emits empty token and code values.
     *
     * <p>The push-token and push-code form fields are still emitted but
     * with empty values, which the server tolerates as a low-trust signal.
     *
     * @apiNote
     * Use this as the default when no real push service is available;
     * registration still proceeds, but the account loses the trust signal
     * that a verified push channel would provide.
     *
     * @return a stateless no-op push client
     */
    static LinkedWhatsAppClientDevicePushClient noop() {
        return NoopMobilePushClient.INSTANCE;
    }

    /**
     * Returns the set of {@link ClientPlatformType} values this push
     * client is willing to authenticate against.
     *
     * <p>An APNS-backed implementation returns
     * {@code {IOS, IOS_BUSINESS}}, an FCM-backed implementation returns
     * {@code {ANDROID, ANDROID_BUSINESS}}, and the no-op client returned by
     * {@link #noop()} returns every enum constant since it accepts any
     * device unconditionally.
     *
     * @apiNote
     * Consult this before {@link #authenticate(LinkedWhatsAppClientDevice)} to verify
     * that a chosen client matches the device platform.
     *
     * @return an unmodifiable, non-empty set of supported platforms
     */
    Set<ClientPlatformType> supportedPlatforms();

    /**
     * Authenticates this push client with its underlying push service
     * using the given device profile.
     *
     * <p>For FCM-based implementations this typically completes a checkin
     * with Google Play Services using the device's model, manufacturer, and
     * OS version to register a fresh push registration ID. For APNS-based
     * implementations this opens an authenticated session with Apple's APNS
     * gateway.
     *
     * @implSpec
     * Implementations must be idempotent: a call made while
     * {@link #isAuthenticated} already returns {@code true} is a no-op (or a
     * cheap re-check) rather than a re-run of the full authentication
     * ceremony.
     *
     * @param device the device profile to authenticate as; never
     *               {@code null}
     * @throws RuntimeException if the underlying push service refuses to
     *                          authenticate the given device profile
     */
    void authenticate(LinkedWhatsAppClientDevice device);

    /**
     * Returns whether this push client currently holds an active
     * authenticated session with its underlying push service.
     *
     * <p>The client returned by {@link #noop()} reports {@code true}
     * unconditionally, because the empty token and code values it produces
     * are valid even without any real authentication.
     *
     * @apiNote
     * Consult this to decide whether {@link #authenticate(LinkedWhatsAppClientDevice)}
     * must run before {@link #getPushToken()} or {@link #getPushCode()} can
     * be safely read.
     *
     * @return {@code true} if an authenticated session is in place
     */
    boolean isAuthenticated();

    /**
     * Returns the device push registration token advertised in the
     * push-token form field.
     *
     * <p>On Android this is the long token returned by
     * {@code FirebaseMessaging.getInstance().getToken()} on a real
     * Play-Services device. On iOS this is the hex-encoded device token the
     * app receives via {@code -[UIApplication
     * application:didRegisterForRemoteNotificationsWithDeviceToken:]}. The
     * token is embedded once at the start of the registration ceremony so
     * the server knows where to silent-push the verification code if the
     * user later picks a push-based verification method; its lifecycle is
     * independent from {@link #getPushCode()}, which surrenders the code
     * extracted from that later push.
     *
     * @implSpec
     * Implementations return the empty string when push is unavailable
     * (Huawei-style or sideloaded Android, simulator or jailbroken iOS); the
     * field is still emitted, and the server tolerates the empty value as a
     * low-trust signal.
     *
     * @return the push device token, or empty string when push is
     *         unavailable; never {@code null}
     */
    String getPushToken();

    /**
     * Releases any resources held by this push client.
     *
     * <p>Narrows {@link AutoCloseable#close()}'s {@code throws Exception} to
     * no checked exceptions so callers may use the client in plain
     * try-with-resources blocks without a wrapping {@code try}/{@code catch}.
     * Vendor-backed implementations (FCM, APNS) override it to tear down
     * their network sockets, threads, and decryption material; the default
     * implementation is a no-op for stateless clients such as the one
     * returned by {@link #noop()}.
     */
    @Override
    default void close() {
    }

    /**
     * Returns the verification code received via a silent push message,
     * embedded as the push-code form field on the code-submission request.
     *
     * <p>The flow is: a prior request advertised the device's push token via
     * {@link #getPushToken()}, the server silently pushed a payload carrying
     * a verification code, the embedder's push handler extracted that code,
     * and the next code-submission request surrenders it back here. Cobalt's
     * built-in flow does not drive this path (it offers SMS, voice, and
     * existing-install verification instead), so this is rarely consulted
     * unless the embedder has wired a push listener.
     *
     * @implSpec
     * Implementations return the empty string when no push verification is
     * in flight; the server treats an empty push code the same as the field
     * being absent.
     *
     * @return the push-delivered verification code, or empty string when no
     *         push verification is in flight; never {@code null}
     */
    String getPushCode();
}
