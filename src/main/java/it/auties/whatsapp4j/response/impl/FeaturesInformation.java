package it.auties.whatsapp4j.response.impl;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * A json model that contains information about the features available to the user linked with this session
 * This record should only be used by {@link UserInformationResponse}
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class FeaturesInformation {
    private final boolean url;
    private final String flags;

    /**
     * @param url a flag used to determine whether URLs can be shown???
     * @param flags unknown
     */
    public FeaturesInformation(boolean url, String flags) {
        this.url = url;
        this.flags = flags;
    }
}
