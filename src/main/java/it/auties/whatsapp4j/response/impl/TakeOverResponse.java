package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.request.impl.TakeOverRequest;
import it.auties.whatsapp4j.response.model.JsonResponseModel;

import java.util.Objects;


/**
 * A json model that contains information regarding a {@link TakeOverRequest}
 *
 */
public final class TakeOverResponse implements JsonResponseModel {
    private final int status;
    private final String ref;
    private final String challenge;
    private final int ttl;

    /**
     * @param status the http status code for the original request, 0 if challenge != null
     * @param ref a token sent by Whatsapp to login, null if status != 200
     * @param challenge a token sent by Whatsapp to verify ownership of the credentials, null if status != 0
     * @param ttl time to live in seconds for the ref, 0 if status != 200
     */
    public TakeOverResponse(int status, String ref, String challenge, int ttl) {
        this.status = status;
        this.ref = ref;
        this.challenge = challenge;
        this.ttl = ttl;
    }

    public int status() {
        return status;
    }

    public String ref() {
        return ref;
    }

    public String challenge() {
        return challenge;
    }

    public int ttl() {
        return ttl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TakeOverResponse) obj;
        return this.status == that.status &&
                Objects.equals(this.ref, that.ref) &&
                Objects.equals(this.challenge, that.challenge) &&
                this.ttl == that.ttl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, ref, challenge, ttl);
    }

    @Override
    public String toString() {
        return "TakeOverResponse[" +
                "status=" + status + ", " +
                "ref=" + ref + ", " +
                "challenge=" + challenge + ", " +
                "ttl=" + ttl + ']';
    }

}
