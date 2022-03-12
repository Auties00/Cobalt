package it.auties.whatsapp;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

@UtilityClass
@Log
public class GithubActions {
    public final String CREDENTIALS_NAME = "WHATSAPP_KEYS";
    private final String GITHUB_ACTIONS = "GITHUB_ACTIONS";

    public boolean isActionsEnvironment(){ 
        return Boolean.parseBoolean(System.getenv(GITHUB_ACTIONS));
    }
}
