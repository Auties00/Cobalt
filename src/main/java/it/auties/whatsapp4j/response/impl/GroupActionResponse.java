package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A json model that contains information about an action executed in a WhatsappGroup
 *
 * @param participants the jids of the participants on which the action was executed
 */
public record GroupActionResponse(@NotNull List<String> participants) implements JsonResponseModel {

}
