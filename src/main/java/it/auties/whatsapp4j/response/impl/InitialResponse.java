package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.request.impl.TakeOverRequest;
import it.auties.whatsapp4j.response.model.JsonResponseModel;

import java.util.Objects;


/**
 * A json model that contains information regarding a {@link TakeOverRequest}
 *
 */
public final class InitialResponse implements JsonResponseModel {
    private final int status;
    private final String ref;
    private final int ttl;

    /**
     * @param status the http status code for the original request, 0 if challenge != null
     * @param ref a token sent by Whatsapp to login, might be null
     * @param ttl time to live in seconds for the ref
     */
    public InitialResponse(int status, String ref, int ttl) {
        this.status = status;
        this.ref = ref;
        this.ttl = ttl;
    }

    public int status() {
        return status;
    }

    public String ref() {
        return ref;
    }

    public int ttl() {
        return ttl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (InitialResponse) obj;
        return this.status == that.status &&
                Objects.equals(this.ref, that.ref) &&
                this.ttl == that.ttl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, ref, ttl);
    }

    @Override
    public String toString() {
        return "InitialResponse[" +
                "status=" + status + ", " +
                "ref=" + ref + ", " +
                "ttl=" + ttl + ']';
    }

}
