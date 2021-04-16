package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;


import java.util.Objects;

/**
 * A json model that contains information about a requested profile picture
 *
 */
public final class ChatPictureResponse implements JsonResponseModel<ChatPictureResponse> {
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

    public int status() {
        return status;
    }

    public String url() {
        return url;
    }

    public String tag() {
        return tag;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ChatPictureResponse) obj;
        return this.status == that.status &&
                Objects.equals(this.url, that.url) &&
                Objects.equals(this.tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, url, tag);
    }

    @Override
    public String toString() {
        return "ChatPictureResponse[" +
                "status=" + status + ", " +
                "url=" + url + ", " +
                "tag=" + tag + ']';
    }

}
