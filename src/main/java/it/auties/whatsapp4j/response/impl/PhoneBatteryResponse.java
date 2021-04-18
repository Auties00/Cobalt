package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;

import java.util.Objects;

/**
 * A json model that contains information the battery status of the phone associated with this session
 *
 */
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

    public int value() {
        return value;
    }

    public boolean live() {
        return live;
    }

    public boolean powerSave() {
        return powerSave;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PhoneBatteryResponse) obj;
        return this.value == that.value &&
                this.live == that.live &&
                this.powerSave == that.powerSave;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, live, powerSave);
    }

    @Override
    public String toString() {
        return "PhoneBatteryResponse[" +
                "value=" + value + ", " +
                "live=" + live + ", " +
                "powerSave=" + powerSave + ']';
    }

}
