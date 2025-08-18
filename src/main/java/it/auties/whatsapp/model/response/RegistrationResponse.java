package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.VerificationCodeError;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeStatus;

/**
 * A model that represents a newsletters from Whatsapp regarding the registration of a phone number
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
 * @param callWait            the time in seconds after which the app would allow you to try again to register using a call
 * @param smsWait             the time in seconds after which the app would allow you to try again to register using a sms
 * @param flashType           unknown
 * @param whatsappWait        the last wait time in seconds before trying again, if available
 * @param securityCodeSet     whether 2fa is enabled
 * @param imageCaptcha        the image captcha to solve, only available for business accounts
 * @param audioCaptcha        the audio captcha to solve, only available for business accounts
 * @param otpEligible if requested, whether the phone number was already registered on Whatsapp
 */
public record RegistrationResponse(@JsonProperty("login") PhoneNumber number,
                                   @JsonProperty("lid") long lid,
                                   @JsonProperty("status") VerificationCodeStatus status,
                                   @JsonProperty("reason") VerificationCodeError errorReason,
                                   @JsonProperty("method") VerificationCodeMethod method,
                                   @JsonProperty("length") int codeLength,
                                   @JsonProperty("notify_after") int notifyAfter,
                                   @JsonProperty("retry_after") long retryAfter,
                                   @JsonProperty("voice_length") long voiceLength,
                                   @JsonProperty(value = "voice_wait", defaultValue = "-1") long callWait,
                                   @JsonProperty(value = "sms_wait", defaultValue = "-1") long smsWait,
                                   @JsonProperty(value = "flash_type", defaultValue = "0") boolean flashType,
                                   @JsonProperty(value = "wa_old_wait", defaultValue = "-1") long whatsappWait,
                                   @JsonProperty("security_code_set") boolean securityCodeSet,
                                   @JsonProperty("image_blob") String imageCaptcha,
                                   @JsonProperty("audio_blob") String audioCaptcha,
                                   @JsonProperty("cert") String cert,
                                   @JsonProperty("wa_old_eligible") boolean otpEligible,
                                   @JsonProperty(value = "send_sms_eligible", defaultValue = "true") boolean smsEligible,
                                   @JsonProperty("possible_migration") boolean possibleMigration,
                                   @JsonProperty(value = "autoconf_type", defaultValue = "0") boolean autoConfigure,
                                   @JsonProperty("wipe_token") String wipeToken
) {

}