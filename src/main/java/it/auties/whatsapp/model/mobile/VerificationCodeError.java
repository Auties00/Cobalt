package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum VerificationCodeError {
    NETWORK_ERROR("network_error"),
    GENERAL_ERROR("error"),
    TOKEN_ERROR("token-error"),
    BAD_PARAM("bad_param"),
    BAD_TOKEN("bad_token"),
    MISSING_PARAM("missing_param"),
    TOO_MANY_GUESSES("too_many_guesses"),
    UNKNOWN("unknown"),
    TOO_MANY_ALL_METHODS("too_many_all_methods"),
    BLOCKED("blocked"),
    TEMPORARILY_UNAVAILABLE("temporarily_unavailable"),
    TOO_RECENT("too_recent"),
    PROVIDER_TIMEOUT("provider_timeout"),
    PROVIDER_UNROUTABLE("provider_unroutable"),
    GUESSED_TOO_FAST("guessed_too_fast"),
    RESET_TOO_SOON("reset_too_soon"),
    EMAIL_SENT("email_sent"),
    NO_ROUTES("no_routes"),
    TOO_MANY("too_many"),
    NEXT_METHOD("next_method"),
    WRONG_CODE("mismatch"),
    OLD_VERSION("old_version"),
    CAPTCHA("code_checkpoint"),
    INVALID_SIGNED_KEY("invalid_skey"),
    FORMAT_WRONG("format_wrong"),
    INCORRECT("incorrect"),
    DEVICE_CONFIRM_OR_SECOND_CODE("device_confirm_or_second_code"),
    SECURITY_CODE("security_code"),
    STALE("stale"),
    SUCCESS("");

    private final String data;

    VerificationCodeError(String data) {
        this.data = data;
    }

    @JsonCreator
    public static VerificationCodeError of(String name) {
        return Arrays.stream(values())
                .filter(entry -> entry.data.equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public String data() {
        return data;
    }
}
