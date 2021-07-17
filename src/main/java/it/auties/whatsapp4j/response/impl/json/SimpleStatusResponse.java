package it.auties.whatsapp4j.response.impl.json;

import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import it.auties.whatsapp4j.utils.internal.Validate;
import lombok.NonNull;

/**
 * A json model that contains information only about the http status code for the original request
 *
 * @param status the http status code for the original request
 */
public final record SimpleStatusResponse(int status) implements JsonResponseModel {
    /**
     * Returns whether the request associated with this response has a 200 status code
     *
     * @return true if {@code status == 200}
     */
    public boolean isSuccessful(){
        return status == 200;
    }

    /**
     * If the request associated with this response was not successful,
     * {@link IllegalStateException} is thrown.
     */
    public void orElseThrow(){
        orElseThrow("Erroneous status code, expected 200 got %s", status);
    }

    /**
     * If the request associated with this response was not successful,
     * {@link IllegalStateException} is thrown.
     *
     * @param message the error message
     * @param params  the parameters for {@code message}
     */
    public void orElseThrow(@NonNull String message, Object... params){
        Validate.isTrue(isSuccessful(), message, params, IllegalStateException.class);
    }
}
