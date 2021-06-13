package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.NonNull;

/**
 * A json model that contains information about a change in a WhatsappGroup's description
 *
 * @param description   the new description
 * @param descriptionId the id of the new description
 */
public final record DescriptionChangeResponse(@JsonProperty("desc") @NonNull String description,
                                              @JsonProperty("descId") String descriptionId) implements JsonResponseModel {
}
