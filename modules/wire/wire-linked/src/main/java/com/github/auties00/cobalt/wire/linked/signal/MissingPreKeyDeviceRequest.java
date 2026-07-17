package com.github.auties00.cobalt.wire.linked.signal;

import java.util.Arrays;
import java.util.Objects;

/**
 * Carries one per-device entry of a Signal missing-pre-key fetch
 * request.
 *
 * <p>Pairs a numeric device id with the 4-byte registration id whose
 * stale state needs refreshing. The relay uses the registration id to
 * decide whether the device's locally cached bundle is still fresh:
 * when it matches the relay's view, no fresh pre-key is dispensed for
 * that device.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it travels
 * from caller code to the wire-encoder and is never serialized as
 * protobuf itself.
 */
public final class MissingPreKeyDeviceRequest {
    /**
     * The numeric device id ({@code 0..99}).
     */
    private final int deviceId;

    /**
     * The 4-byte registration id whose freshness needs validating.
     */
    private final byte[] registrationId;

    /**
     * Constructs a per-device entry.
     *
     * @param deviceId       the device id ({@code 0..99})
     * @param registrationId the 4-byte registration id; never
     *                       {@code null}
     * @throws NullPointerException if {@code registrationId} is
     *                              {@code null}
     */
    public MissingPreKeyDeviceRequest(int deviceId, byte[] registrationId) {
        this.deviceId = deviceId;
        this.registrationId = Objects.requireNonNull(registrationId, "registrationId cannot be null").clone();
    }

    /**
     * Returns the device id.
     *
     * @return the device id
     */
    public int deviceId() {
        return deviceId;
    }

    /**
     * Returns a defensive copy of the 4-byte registration id.
     *
     * @return the registration bytes; never {@code null}
     */
    public byte[] registrationId() {
        return registrationId.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (MissingPreKeyDeviceRequest) obj;
        return this.deviceId == that.deviceId
                && Arrays.equals(this.registrationId, that.registrationId);
    }

    @Override
    public int hashCode() {
        var result = Integer.hashCode(deviceId);
        result = 31 * result + Arrays.hashCode(registrationId);
        return result;
    }

    @Override
    public String toString() {
        return "MissingPreKeyDeviceRequest[deviceId=" + deviceId
                + ", registrationId=" + (registrationId.length + " bytes") + ']';
    }
}
