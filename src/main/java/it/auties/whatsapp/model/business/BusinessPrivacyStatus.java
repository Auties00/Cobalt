package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of business privacy
 */
@AllArgsConstructor
@Accessors(fluent = true)
@ProtobufName("BizPrivacyStatus")
public enum BusinessPrivacyStatus implements ProtobufMessage {
    /**
     * End-to-end encryption
     */
    E2EE(0),
    /**
     * Bsp encryption
     */
    BSP(1),
    /**
     * Facebook encryption
     */
    FACEBOOK(2),
    /**
     * Facebook and bsp encryption
     */
    BSP_AND_FB(3);
    @Getter
    private final int index;

    @JsonCreator
    public static BusinessPrivacyStatus of(int index) {
        return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
    }
}