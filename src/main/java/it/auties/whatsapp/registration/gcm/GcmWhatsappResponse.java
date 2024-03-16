package it.auties.whatsapp.registration.gcm;

import com.fasterxml.jackson.annotation.JsonProperty;

record GcmWhatsappResponse(
        @JsonProperty("data")
        Data data,
        @JsonProperty("from")
        String from,
        @JsonProperty("priority")
        String priority,
        @JsonProperty("fcmMessageId")
        String fcmMessageId,
        @JsonProperty("collapse_key")
        String collapseKey
) {
    record Data(
            @JsonProperty("push_ts")
            long pushTimestamp,
            @JsonProperty("registration_code")
            String pushCode,
            @JsonProperty("push_id")
            String pushId,
            @JsonProperty("id")
            String id,
            @JsonProperty("push_event_id")
            String pushEventId,
            @JsonProperty("push_server_timestamp")
            long pushServerTimestamp,
            @JsonProperty("ts")
            long timestamp
    ) {

    }
}
