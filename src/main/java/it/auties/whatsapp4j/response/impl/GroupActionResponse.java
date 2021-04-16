package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GroupActionResponse(@NotNull List<String> participants) implements JsonResponseModel {

}
