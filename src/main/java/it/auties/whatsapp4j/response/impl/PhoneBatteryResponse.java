package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.Builder;

/**
 * A json model that contains information the battery status of the phone associated with this session
 *
 * @param value an unsigned int that represents how much battery percentage is left on the phone
 * @param live a flag to determine whether this update is in real time or not???
 * @param powerSave a flag to determine whether the phone is on battery save mode or not
 */
public record PhoneBatteryResponse(int value, boolean live, boolean powerSave) implements JsonResponseModel {
}
