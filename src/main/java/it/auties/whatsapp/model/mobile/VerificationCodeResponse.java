package it.auties.whatsapp.model.mobile;

import com.fasterxml.jackson.annotation.JsonProperty;

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
                                       @JsonProperty("flash_type") long flashType,
                                       @JsonProperty("wa_old_wait") long oldWait,
                                       @JsonProperty("security_code_set") boolean securityCodeSet) {

}