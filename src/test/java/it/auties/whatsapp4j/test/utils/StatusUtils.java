package it.auties.whatsapp4j.test.utils;

import it.auties.whatsapp4j.response.impl.json.GroupModificationResponse;
import it.auties.whatsapp4j.response.impl.json.SimpleStatusResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Assertions;

@UtilityClass
@Log
public class StatusUtils {
    public void checkStatusCode(SimpleStatusResponse userPresenceResponse, String message) {
        if(userPresenceResponse.status() == 401){
            log.info("Successful but unauthorized request");
            return;
        }

        Assertions.assertEquals(200, userPresenceResponse.status(), message.formatted(userPresenceResponse));
    }

    public boolean checkStatusCode(GroupModificationResponse changeGroupResponse) {
        return changeGroupResponse.modifications()
                .stream()
                .allMatch(modification -> checkStatusCode(modification.status().code()));
    }

    private boolean checkStatusCode(int statusCode) {
        return statusCode == 200 || statusCode == 404;
    }
}
