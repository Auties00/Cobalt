package com.github.auties00.cobalt.wire.cloud.template;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A single authentication-template definition to upsert across many languages in one request.
 *
 * <p>The upsert edge takes one template body, fixed to the {@code AUTHENTICATION} category, and fans it
 * out across every listed language: an existing template matching a name and language is updated, and a
 * missing one is created. The definition carries the template name, the target languages, the OTP
 * button to render, and three optional fields: whether the body shows the security recommendation, the
 * code expiration carried on the footer, and the top-level message send time-to-live.
 *
 * <p>Unlike a per-language template, no single language field is carried; the {@link #languages()} list
 * drives the fan-out. The OTP button's text and autofill label are intentionally not sent: the server
 * localizes them per language.
 */
public final class CloudAuthenticationTemplateUpsert {
    /**
     * The unique template name, at most 512 characters.
     */
    private final String name;

    /**
     * The target language codes, for example {@code en_US}.
     */
    private final List<String> languages;

    /**
     * The OTP button to render in the buttons component.
     */
    private final CloudOtpButton otpButton;

    /**
     * Whether the body shows the security recommendation, or {@code null} to omit the field.
     */
    private final Boolean addSecurityRecommendation;

    /**
     * The code expiration carried on the footer, in minutes, or {@code null} to omit the field.
     */
    private final Integer codeExpirationMinutes;

    /**
     * The top-level message send time-to-live, in seconds, or {@code null} to omit the field.
     */
    private final Integer messageSendTtlSeconds;

    /**
     * Constructs a new authentication-template upsert definition.
     *
     * @param name                      the unique template name, at most 512 characters
     * @param languages                 the target language codes
     * @param otpButton                 the OTP button to render
     * @param addSecurityRecommendation whether the body shows the security recommendation, or {@code null}
     * @param codeExpirationMinutes     the footer code expiration in minutes, or {@code null}
     * @param messageSendTtlSeconds     the message send time-to-live in seconds, or {@code null}
     * @throws NullPointerException     if {@code name}, {@code languages}, or {@code otpButton} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code languages} is empty
     */
    public CloudAuthenticationTemplateUpsert(String name, List<String> languages, CloudOtpButton otpButton,
                                             Boolean addSecurityRecommendation, Integer codeExpirationMinutes,
                                             Integer messageSendTtlSeconds) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(languages, "languages must not be null");
        if (languages.isEmpty()) {
            throw new IllegalArgumentException("languages must not be empty");
        }
        this.languages = List.copyOf(languages);
        this.otpButton = Objects.requireNonNull(otpButton, "otpButton must not be null");
        this.addSecurityRecommendation = addSecurityRecommendation;
        this.codeExpirationMinutes = codeExpirationMinutes;
        this.messageSendTtlSeconds = messageSendTtlSeconds;
    }

    /**
     * Returns the unique template name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the target language codes.
     *
     * @return an unmodifiable, non-empty list of language codes
     */
    public List<String> languages() {
        return languages;
    }

    /**
     * Returns the OTP button to render.
     *
     * @return the OTP button
     */
    public CloudOtpButton otpButton() {
        return otpButton;
    }

    /**
     * Returns whether the body shows the security recommendation.
     *
     * @return an {@link Optional} carrying the flag, or empty to omit the field
     */
    public Optional<Boolean> addSecurityRecommendation() {
        return Optional.ofNullable(addSecurityRecommendation);
    }

    /**
     * Returns the footer code expiration, in minutes.
     *
     * @return an {@link Optional} carrying the expiration, or empty to omit the field
     */
    public Optional<Integer> codeExpirationMinutes() {
        return Optional.ofNullable(codeExpirationMinutes);
    }

    /**
     * Returns the message send time-to-live, in seconds.
     *
     * @return an {@link Optional} carrying the time-to-live, or empty to omit the field
     */
    public Optional<Integer> messageSendTtlSeconds() {
        return Optional.ofNullable(messageSendTtlSeconds);
    }
}
