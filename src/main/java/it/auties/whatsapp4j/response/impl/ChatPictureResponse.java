package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;


import java.util.Objects;

/**
 * A json model that contains information about a requested profile picture
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class ChatPictureResponse implements JsonResponseModel {
    private final int status;
    private final String url;
    private final String tag;

    /**
     * @param status the http status code for the original request
     * @param url the url for the requested profile picture
     * @param tag a tag for this response
     */
    public ChatPictureResponse(int status, String url, String tag) {
        this.status = status;
        this.url = url;
        this.tag = tag;
    }

    @JsonCreator
    public ChatPictureResponse(@JsonProperty("eurl") String url, @JsonProperty("tag") String tag, @JsonProperty("status") Integer status) {
        this(Objects.requireNonNullElse(status, 200), url, tag);
    }
}
