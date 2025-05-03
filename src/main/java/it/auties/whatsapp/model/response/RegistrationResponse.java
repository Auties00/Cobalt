package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;
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
@Json
public record RegistrationResponse(@Json.Property("login") PhoneNumber number,
                                   @Json.Property("lid") long lid,
                                   @Json.Property("status") VerificationCodeStatus status,
                                   @Json.Property("reason") VerificationCodeError errorReason,
                                   @Json.Property("method") VerificationCodeMethod method,
                                   @Json.Property("length") int codeLength,
                                   @Json.Property("notify_after") int notifyAfter,
                                   @Json.Property("retry_after") long retryAfter,
                                   @Json.Property("voice_length") long voiceLength,
                                   @Json.Property("voice_wait") long callWait,
                                   @Json.Property("sms_wait") long smsWait,
                                   @Json.Property("flash_type") boolean flashType,
                                   @Json.Property("wa_old_wait") long whatsappWait,
                                   @Json.Property("security_code_set") boolean securityCodeSet,
                                   @Json.Property("image_blob") String imageCaptcha,
                                   @Json.Property("audio_blob") String audioCaptcha,
                                   @Json.Property("cert") String cert,
                                   @Json.Property("wa_old_eligible") boolean otpEligible,
                                   @Json.Property("send_sms_eligible") boolean smsEligible,
                                   @Json.Property("possible_migration") boolean possibleMigration,
                                   @Json.Property("autoconf_type") boolean autoConfigure,
                                   @Json.Property("wipe_token") String wipeToken
) {

}