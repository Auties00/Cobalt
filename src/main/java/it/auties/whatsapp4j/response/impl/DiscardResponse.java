package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * A json model that ignores any data provided by a response
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class DiscardResponse implements JsonResponseModel {
}
