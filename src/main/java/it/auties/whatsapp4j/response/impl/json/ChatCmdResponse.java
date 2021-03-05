package it.auties.whatsapp4j.response.impl.json;

import it.auties.whatsapp4j.response.model.JsonResponseModel;

import java.util.List;

public record ChatCmdResponse(String id, String cmd, List<Object> data) implements JsonResponseModel {
}
