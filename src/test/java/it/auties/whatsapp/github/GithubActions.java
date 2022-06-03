package it.auties.whatsapp.github;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

@UtilityClass
@Log
public class GithubActions {
    public final String CREDENTIALS_NAME = "WHATSAPP_KEYS";
    public final String STORE_NAME = "WHATSAPP_STORE";
    public final String CONTACT_NAME = "WHATSAPP_CONTACT";
    private final String GITHUB_ACTIONS = "GITHUB_ACTIONS";

    public boolean isActionsEnvironment(){ 
        return Boolean.parseBoolean(System.getenv(GITHUB_ACTIONS));
    }
}
