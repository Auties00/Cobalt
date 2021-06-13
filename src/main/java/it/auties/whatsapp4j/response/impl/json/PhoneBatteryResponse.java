package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;

/**
 * A json model that contains information the battery status of the phone associated with this session
 *
 * @param value     an unsigned int that represents how much battery percentage is left on the phone
 * @param charging  a flag to determine whether this update is charging
 * @param powerSave a flag to determine whether the phone is on battery save mode or not
 */
public record PhoneBatteryResponse(int value, @JsonProperty("live") boolean charging, boolean powerSave) implements JsonResponseModel {
}
