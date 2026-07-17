package com.github.auties00.cobalt.registration.push.fcm;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevice;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Immutable Firebase project descriptor identifying the Android app that {@link FcmClient} impersonates while running
 * the FCM registration handshake.
 *
 * <p>Two pre-built constants cover the WhatsApp Android variants the registration server expects:
 * {@link #WHATSAPP_PERSONAL} for {@code com.whatsapp} and {@link #WHATSAPP_BUSINESS} for {@code com.whatsapp.w4b}. The
 * config is selected indirectly by the device-platform switch inside
 * {@link FcmClient#authenticate(LinkedWhatsAppClientDevice)} and supplies the project id, app
 * id, API key, sender id, package name and signing-certificate hash that the FIS install and register3 calls send so
 * the requests look like a real install.
 *
 * @implNote
 * This implementation is bundled inside {@link FcmSession} so a saved session round-trips its configuration along with
 * the credentials it acquired; callers reloading via {@link FcmClient#loadSession(FcmSession)} do not need to remember
 * which config the original session was created with.
 */
@ProtobufMessage(name = "FcmConfig")
public final class FcmConfig {
    /**
     * Configuration impersonating the WhatsApp consumer Android app ({@code com.whatsapp}).
     *
     * <p>Selected by {@link FcmClient#authenticate(LinkedWhatsAppClientDevice)} for
     * {@link com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType#ANDROID}; the values mirror the
     * {@code google-services.json} bundled with the Play Store APK so the FIS and register3 calls look like a real
     * install.
     */
    public static final FcmConfig WHATSAPP_PERSONAL = new FcmConfig(
            "whatsapp-messenger",
            "1:293955441834:android:7373a2d0bdfa3228",
            "AIzaSyCGOJbGQ95SWrXxl8wk-_cRQZcJl42bvDU",
            "293955441834",
            "com.whatsapp",
            "38a0f7d505fe18fec64fbf343ecaaaf310dbd799",
            true);

    /**
     * Configuration impersonating the WhatsApp Business Android app ({@code com.whatsapp.w4b}).
     *
     * <p>Selected by {@link FcmClient#authenticate(LinkedWhatsAppClientDevice)} for
     * {@link com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType#ANDROID_BUSINESS}; every Firebase
     * resource matches {@link #WHATSAPP_PERSONAL} (same project id, app id, API key, sender id and signing
     * certificate). Only the package name differs.
     */
    public static final FcmConfig WHATSAPP_BUSINESS = new FcmConfig(
            "whatsapp-messenger",
            "1:293955441834:android:7373a2d0bdfa3228",
            "AIzaSyCGOJbGQ95SWrXxl8wk-_cRQZcJl42bvDU",
            "293955441834",
            "com.whatsapp.w4b",
            "38a0f7d505fe18fec64fbf343ecaaaf310dbd799",
            true);

    /**
     * Firebase project id, sent as the path segment of the FIS endpoint URL.
     *
     * <p>Both WhatsApp variants share the same project id ({@code "whatsapp-messenger"}).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String projectId;

    /**
     * Firebase application id.
     *
     * <p>Sent as the {@code appId} field on the FIS install request and as the {@code X-gmp_app_id} header on the GCM
     * register3 call.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String appId;

    /**
     * Firebase API key.
     *
     * <p>Sent as the {@code x-goog-api-key} header on the FIS install request.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String apiKey;

    /**
     * Numeric GCM sender id, also called the Firebase project number.
     *
     * <p>Sent as the {@code sender}, {@code X-subtype} and {@code X-subscription} fields on the GCM register3 call.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String senderId;

    /**
     * Android package name to impersonate.
     *
     * <p>Sent as the {@code app} form field, the {@code X-Android-Package} header, and the {@code app} header on
     * register3.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String packageName;

    /**
     * Hex-encoded SHA-1 of the Android signing certificate.
     *
     * <p>Sent as the {@code cert} form field and the {@code X-Android-Cert} header; the FIS step normalises the value
     * to uppercase, the register3 step normalises it to lowercase, and both strip any colons.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String certSha1;

    /**
     * Whether to run the Firebase Installations step before register3.
     *
     * <p>Modern Firebase-backed apps require the FIS step; legacy pre-Firebase projects registered against the bare
     * GCM endpoint must skip it. {@code true} is the right default for any project created after 2019.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    boolean useFis;

    /**
     * Constructs a new immutable config with the given values.
     *
     * <p>Callers obtain configs through {@link #WHATSAPP_PERSONAL} or {@link #WHATSAPP_BUSINESS}, or via the protobuf
     * decoder when reloading an {@link FcmSession}.
     *
     * @param projectId   the Firebase project id
     * @param appId       the Firebase app id
     * @param apiKey      the Firebase API key
     * @param senderId    the GCM sender id
     * @param packageName the Android package name
     * @param certSha1    hex-encoded SHA-1 of the signing certificate
     * @param useFis      whether to call the FIS endpoint
     */
    FcmConfig(String projectId, String appId, String apiKey, String senderId,
              String packageName, String certSha1, boolean useFis) {
        this.projectId = projectId;
        this.appId = appId;
        this.apiKey = apiKey;
        this.senderId = senderId;
        this.packageName = packageName;
        this.certSha1 = certSha1;
        this.useFis = useFis;
    }

    /**
     * Returns the Firebase project id.
     *
     * @return the project id
     */
    public String projectId() {
        return projectId;
    }

    /**
     * Returns the Firebase application id.
     *
     * @return the app id
     */
    public String appId() {
        return appId;
    }

    /**
     * Returns the Firebase API key.
     *
     * @return the API key
     */
    public String apiKey() {
        return apiKey;
    }

    /**
     * Returns the numeric GCM sender id.
     *
     * @return the sender id
     */
    public String senderId() {
        return senderId;
    }

    /**
     * Returns the Android package name to impersonate.
     *
     * @return the package name
     */
    public String packageName() {
        return packageName;
    }

    /**
     * Returns the hex-encoded SHA-1 of the signing certificate.
     *
     * @return the certificate hash
     */
    public String certSha1() {
        return certSha1;
    }

    /**
     * Reports whether the FIS install step is required before register3.
     *
     * @return {@code true} if the FIS step must be run
     */
    public boolean useFis() {
        return useFis;
    }
}
