package org.example.whatsapp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(String action,
                          @JsonAlias("message") @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY) List<ChatMessage> messages,
                          @JsonProperty("conversation_id") String conversationId,
                          @JsonProperty("parent_message_id") String parentMessageId, String model) {
}
