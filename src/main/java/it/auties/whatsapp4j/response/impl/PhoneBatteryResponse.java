package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * A json model that contains information the battery status of the phone associated with this session
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class PhoneBatteryResponse implements JsonResponseModel {
    private final int value;
    private final boolean live;
    private final boolean powerSave;

    /**
     * @param value an unsigned int that represents how much battery percentage is left on the phone
     * @param live a flag to determine whether this update is in real time or not???
     * @param powerSave a flag to determine whether the phone is on battery save mode or not
     */
    public PhoneBatteryResponse(int value, boolean live, boolean powerSave) {
        this.value = value;
        this.live = live;
        this.powerSave = powerSave;
    }
}
