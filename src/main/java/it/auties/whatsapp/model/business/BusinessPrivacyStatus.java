package it.auties.whatsapp.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constants of this enumerated type describe the various types of business privacy
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum BusinessPrivacyStatus {
    E2EE(0),
    BSP(1),
    FB(2),
    BSP_AND_FB(3);

    @Getter
    private final int index;

    @JsonCreator
    public static BusinessPrivacyStatus forIndex(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(null);
    }
}
