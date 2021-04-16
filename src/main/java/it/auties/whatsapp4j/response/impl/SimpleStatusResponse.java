package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;

import java.util.Objects;

/**
 * A json model that contains information only about the http status code for the original request
 *
 */
public final class SimpleStatusResponse implements JsonResponseModel<SimpleStatusResponse> {
    private final int status;

    /**
     * @param status the http status code for the original request
     */
    public SimpleStatusResponse(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SimpleStatusResponse) obj;
        return this.status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status);
    }

    @Override
    public String toString() {
        return "SimpleStatusResponse[" +
                "status=" + status + ']';
    }

}
