package it.auties.whatsapp.model.phone;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PhoneNumberResponse(boolean ok,
                                  String reason,
                                  String login,
                                  String status, @JsonProperty("voice_length") long voiceLength,
                                  @JsonProperty("voice_wait") long voiceWait,
                                  @JsonProperty("sms_wait") long smsWait,
                                  @JsonProperty("flash_type") long flashType) {
}
