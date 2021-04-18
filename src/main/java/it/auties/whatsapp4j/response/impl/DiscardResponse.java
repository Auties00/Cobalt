package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;

/**
 * A json model that ignores any data provided by a response
 */
public final class DiscardResponse implements JsonResponseModel {
    /**
     */
    public DiscardResponse() {
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null && obj.getClass() == this.getClass();
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return "DiscardResponse[]";
    }


}
