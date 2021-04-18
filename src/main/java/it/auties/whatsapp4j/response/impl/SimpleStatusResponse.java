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
 * @param status the http status code for the original request
 */
public record SimpleStatusResponse(int status) implements JsonResponseModel {
}
