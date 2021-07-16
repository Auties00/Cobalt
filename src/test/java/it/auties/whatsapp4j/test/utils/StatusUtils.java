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
            log.info("The request was successful, but the contact blocked you");
            return;
        }

        Assertions.assertEquals(200, userPresenceResponse.status(), message.formatted(userPresenceResponse));
    }

    public boolean checkStatusCode(GroupModificationResponse changeGroupResponse) {
        return changeGroupResponse.modifications()
                .stream()
                .allMatch(modification -> checkStatusCode(modification.status().code()));
    }

    // This method assumes that we are in a group scope
    // These checks are to handle a test contact that blocked you
    private boolean checkStatusCode(int statusCode) {
        if(statusCode == 401){
            log.info("The request was successful, but the contact blocked you");
            return true;
        }

        if(statusCode == 404){
            log.info("The request had no effect because the contact blocked you");
            return true;
        }

        return statusCode == 200;
    }
}
