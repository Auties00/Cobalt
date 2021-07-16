package it.auties.whatsapp4j.test.github;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

@UtilityClass
@Log
public class GithubActions {
    private final boolean ACTIONS_ENV = Boolean.parseBoolean(System.getenv("GITHUB_ACTIONS"));
    public final String CONTACT_NAME = "WHATSAPP_CONTACT";
    public final String CREDENTIALS_NAME = "WHATSAPP_KEYS";

    public boolean isActionsEnvironment(){ 
        return ACTIONS_ENV;
    }
}
