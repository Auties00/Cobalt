package com.github.auties00.cobalt.wire.linked.business.promotion;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Context describing where and on which app version a WhatsApp
 * quick-promotion request is being triggered, so the server can decide
 * which promotional banners are eligible to surface.
 *
 * <p>Quick promotions are the in-app banners WhatsApp surfaces on
 * onboarding-like screens (the chat list, the call tab, the status tab,
 * and so on) to advertise new features or upsell to the Business app.
 * The server keys eligibility off four signals carried by this context:
 * whether the caller is the consumer or the Business build of the
 * WhatsApp client ({@linkplain #fromBusinessApp() from-business-app}
 * flag), the {@linkplain #appVersion() app version} the caller is running,
 * the caller's {@linkplain #country() country code}, and the caller's
 * user-interface {@linkplain #locale() locale}.
 *
 * <p>Every field is optional; a {@code null} field is omitted from the
 * request the WhatsApp client sends to the server.
 */
@ProtobufMessage(name = "QuickPromotionTriggerContext")
public final class QuickPromotionTriggerContext {
    /**
     * Whether the request originates from the WhatsApp Business client.
     * {@code true} when the caller is the Business app, {@code false} when
     * the caller is the consumer app, {@code null} when the flag was not
     * supplied.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final Boolean fromBusinessApp;

    /**
     * Base string of the WhatsApp app version the caller is running, or
     * {@code null} when the field was not supplied.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String appVersion;

    /**
     * Caller's ISO country short-code, or {@code null} when the field was
     * not supplied.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String country;

    /**
     * Caller's user-interface locale, or {@code null} when the field was
     * not supplied.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String locale;

    /**
     * Constructs a new {@code QuickPromotionTriggerContext}. Every argument
     * may be {@code null}; a {@code null} argument is omitted from the
     * request the WhatsApp client sends.
     *
     * @param fromBusinessApp whether the caller is the Business app, or
     *                        {@code null}
     * @param appVersion      the app version base string, or {@code null}
     * @param country         the caller's ISO country short-code, or
     *                        {@code null}
     * @param locale          the caller's locale, or {@code null}
     */
    QuickPromotionTriggerContext(Boolean fromBusinessApp, String appVersion, String country, String locale) {
        this.fromBusinessApp = fromBusinessApp;
        this.appVersion = appVersion;
        this.country = country;
        this.locale = locale;
    }

    /**
     * Returns whether the request originates from the WhatsApp Business
     * client.
     *
     * @return an {@code Optional} carrying {@code true} when the caller is
     *         the Business app and {@code false} when the caller is the
     *         consumer app, or empty when the flag was not supplied
     */
    public Optional<Boolean> fromBusinessApp() {
        return Optional.ofNullable(fromBusinessApp);
    }

    /**
     * Returns the base string of the WhatsApp app version the caller is
     * running.
     *
     * @return an {@code Optional} carrying the app version, or empty when
     *         the field was not supplied
     */
    public Optional<String> appVersion() {
        return Optional.ofNullable(appVersion);
    }

    /**
     * Returns the caller's ISO country short-code.
     *
     * @return an {@code Optional} carrying the country code, or empty when
     *         the field was not supplied
     */
    public Optional<String> country() {
        return Optional.ofNullable(country);
    }

    /**
     * Returns the caller's user-interface locale.
     *
     * @return an {@code Optional} carrying the locale, or empty when the
     *         field was not supplied
     */
    public Optional<String> locale() {
        return Optional.ofNullable(locale);
    }
}
