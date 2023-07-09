package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.Base64;

/**
 * A model that represents a response from Whatsapp regarding the registration of a phone number
 *
 * @param number              the number that was registered
 * @param lid                 the lid of the number that was registered
 * @param status              the status of the registration
 * @param errorReason         the error, if any was thrown
 * @param method              the method used to register, if any was used
 * @param codeLength          the expected length of the code, if a code request was sent
 * @param notifyAfter         the time in seconds after which the app would notify you to try again to register
 * @param retryAfter          the time in seconds after which the app would allow you to try again to register a sms
 * @param voiceLength         unknown
 * @param voiceWait           the time in seconds after which the app would allow you to try again to register using a call
 * @param smsWait             the time in seconds after which the app would allow you to try again to register using a sms
 * @param flashType           unknown
 * @param oldWait             the last wait time in seconds before trying again, if available
 * @param securityCodeSet     whether 2fa is enabled
 * @param whatsappOtpWait     the time in seconds after which the app would allow you to try again to register using Whatsapp
 * @param whatsappOtpEligible whether an otp over whatsapp can be sent
 * @param imageCaptcha        the image captcha to solve, only available for business accounts
 * @param audioCaptcha        the audio captcha to solve, only available for business accounts
 */
@Builder
@Jacksonized
public record VerificationCodeResponse(@JsonProperty("login") PhoneNumber number, @JsonProperty("lid") long lid,
                                       @JsonProperty("status") VerificationCodeStatus status,
                                       @JsonProperty("reason") VerificationCodeError errorReason,
                                       @JsonProperty("method") VerificationCodeMethod method,
                                       @JsonProperty("length") int codeLength,
                                       @JsonProperty("notify_after") int notifyAfter,
                                       @JsonProperty("retry_after") long retryAfter,
                                       @JsonProperty("voice_length") long voiceLength,
                                       @JsonProperty("voice_wait") long voiceWait,
                                       @JsonProperty("sms_wait") long smsWait,
                                       @JsonProperty("email_otp_wait") long whatsappOtpWait,
                                       @JsonProperty("flash_type") long flashType,
                                       @JsonProperty("wa_old_wait") long oldWait,
                                       @JsonProperty("security_code_set") boolean securityCodeSet,
                                       @JsonProperty("email_otp_eligible") boolean whatsappOtpEligible,
                                       @JsonProperty("image_blob") byte[] imageCaptcha,
                                       @JsonProperty("audio_blob") byte[] audioCaptcha
) {
    public static class VerificationCodeResponseBuilder {
        @JsonSetter("email_otp_wait")
        private void whatsappOtpWait(String whatsappOtpWait) {
            try {
                this.whatsappOtpWait = Long.parseLong(whatsappOtpWait);
            } catch (NumberFormatException exception) {
                this.whatsappOtpWait = -1;
            }
        }

        @JsonSetter("voice_wait")
        private void voiceWait(String voiceWait) {
            try {
                this.voiceWait = Long.parseLong(voiceWait);
            } catch (NumberFormatException exception) {
                this.voiceWait = -1;
            }
        }

        @JsonSetter("sms_wait")
        private void smsWait(String smsWait) {
            try {
                this.smsWait = Long.parseLong(smsWait);
            } catch (NumberFormatException exception) {
                this.smsWait = -1;
            }
        }

        @JsonSetter("image_blob")
        private void imageCaptcha(String imageCaptcha) {
            if (imageCaptcha == null) {
                return;
            }

            this.imageCaptcha = Base64.getDecoder().decode(imageCaptcha);
        }

        @JsonSetter("audio_blob")
        private void audioCaptcha(String audioCaptcha) {
            if (imageCaptcha == null) {
                return;
            }

            this.audioCaptcha = Base64.getDecoder().decode(audioCaptcha);
        }
    }
}