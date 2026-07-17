package com.github.auties00.cobalt.wire.cloud.template.library;

import com.github.auties00.cobalt.wire.cloud.template.CloudOtpType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * The caller-bound value for one button when adopting a WhatsApp Cloud API Template Library entry.
 *
 * <p>A library entry declares its buttons abstractly; adopting it requires binding the concrete value
 * each button carries. The {@link #type() type} selects which value is required: a {@link #url() url} for
 * a {@link CloudTemplateLibraryButtonType#URL} button, a {@link #phoneNumber() phone number} for a
 * {@link CloudTemplateLibraryButtonType#PHONE_NUMBER} button, or an {@link #otpType() OTP type} for a
 * {@link CloudTemplateLibraryButtonType#OTP} button; a {@link CloudTemplateLibraryButtonType#QUICK_REPLY}
 * button carries none of them. This model is a nested input on {@link CloudTemplateLibraryAdoption}; the
 * type is required.
 *
 * <p>The flat {@link #type()}, {@link #url()}, {@link #phoneNumber()}, and {@link #otpType()} accessors
 * are the wire shape; {@link #value()} projects them onto the closed-type
 * {@link CloudTemplateLibraryButtonValue} for type-safe matching on the bound value.
 */
@ProtobufMessage
public final class CloudTemplateLibraryButtonInput {
    /**
     * The button type, which selects which value field is required.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    final CloudTemplateLibraryButtonType type;

    /**
     * The URL for a {@link CloudTemplateLibraryButtonType#URL} button, or {@code null} otherwise.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String url;

    /**
     * The phone number for a {@link CloudTemplateLibraryButtonType#PHONE_NUMBER} button, or {@code null}
     * otherwise.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String phoneNumber;

    /**
     * The OTP type for a {@link CloudTemplateLibraryButtonType#OTP} button, or {@code null} otherwise.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String otpType;

    /**
     * Constructs a new library-button input.
     *
     * @param type        the button type, which selects which value field is required
     * @param url         the URL for a URL button, or {@code null} otherwise
     * @param phoneNumber the phone number for a phone-number button, or {@code null} otherwise
     * @param otpType     the OTP type for an OTP button, or {@code null} otherwise
     * @throws NullPointerException if {@code type} is {@code null}
     */
    CloudTemplateLibraryButtonInput(CloudTemplateLibraryButtonType type, String url, String phoneNumber,
                                    String otpType) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.url = url;
        this.phoneNumber = phoneNumber;
        this.otpType = otpType;
    }

    /**
     * Returns the button type.
     *
     * @return the type
     */
    public CloudTemplateLibraryButtonType type() {
        return type;
    }

    /**
     * Returns the URL for a URL button.
     *
     * @return an {@link Optional} carrying the URL, or empty when not a URL button
     */
    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    /**
     * Returns the phone number for a phone-number button.
     *
     * @return an {@link Optional} carrying the phone number, or empty when not a phone-number button
     */
    public Optional<String> phoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    /**
     * Returns the OTP type for an OTP button.
     *
     * @return an {@link Optional} carrying the OTP type, or empty when not an OTP button
     */
    public Optional<String> otpType() {
        return Optional.ofNullable(otpType);
    }

    /**
     * Returns the bound value projected onto the closed-type {@link CloudTemplateLibraryButtonValue}.
     *
     * <p>The {@link #type()} selects the variant: a {@link CloudTemplateLibraryButtonType#URL} type
     * yields a {@link CloudTemplateLibraryButtonValue.Url} (empty when no {@link #url()} is bound), a
     * {@link CloudTemplateLibraryButtonType#PHONE_NUMBER} type yields a
     * {@link CloudTemplateLibraryButtonValue.PhoneNumber} (empty when no {@link #phoneNumber()} is
     * bound), a {@link CloudTemplateLibraryButtonType#OTP} type yields a
     * {@link CloudTemplateLibraryButtonValue.Otp} carrying the parsed {@link CloudOtpType}, and a
     * {@link CloudTemplateLibraryButtonType#QUICK_REPLY} type yields a
     * {@link CloudTemplateLibraryButtonValue.QuickReply}. This is a computed projection over the flat
     * fields and is not a serialized property.
     *
     * @return an {@link Optional} carrying the bound value, or empty when the type is
     *         {@link CloudTemplateLibraryButtonType#UNKNOWN} or the required field is unset
     */
    public Optional<CloudTemplateLibraryButtonValue> value() {
        return switch (type) {
            case URL -> Optional.ofNullable(url).map(CloudTemplateLibraryButtonValue.Url::new);
            case PHONE_NUMBER -> Optional.ofNullable(phoneNumber)
                    .map(CloudTemplateLibraryButtonValue.PhoneNumber::new);
            case QUICK_REPLY -> Optional.of(new CloudTemplateLibraryButtonValue.QuickReply());
            case OTP -> Optional.of(new CloudTemplateLibraryButtonValue.Otp(CloudOtpType.of(otpType)));
            case UNKNOWN -> Optional.empty();
        };
    }
}
