package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * A json model that contains information only about the http status code for the original request
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class SimpleStatusResponse implements JsonResponseModel {
    private final int status;

    /**
     * @param status the http status code for the original request
     */
    public SimpleStatusResponse(int status) {
        this.status = status;
    }
}
