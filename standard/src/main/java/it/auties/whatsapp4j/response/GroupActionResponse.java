package it.auties.whatsapp4j.response;

import it.auties.whatsapp4j.common.response.JsonResponseModel;
import lombok.NonNull;

import java.util.List;

/**
 * A json model that contains information about an action executed in a WhatsappGroup
 *
 * @param participants the jids of the participants on which the action was executed
 */
public final record GroupActionResponse(@NonNull List<String> participants) implements JsonResponseModel {
}
