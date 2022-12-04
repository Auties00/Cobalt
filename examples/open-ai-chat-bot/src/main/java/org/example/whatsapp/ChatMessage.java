package org.example.whatsapp;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(String id, String role, Map<String, Object> content) {

}
