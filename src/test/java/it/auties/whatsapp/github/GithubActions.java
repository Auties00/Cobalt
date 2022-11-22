package it.auties.whatsapp.github;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GithubActions {
    public final String CREDENTIALS_NAME = "WHATSAPP_KEYS";
    public final String STORE_NAME = "WHATSAPP_STORE";
    public final String CONTACT_NAME = "WHATSAPP_CONTACT";
    public final String GPG_PASSWORD = "GPG_PASSWORD";
    private final String GITHUB_ACTIONS = "GITHUB_ACTIONS";

    public boolean isActionsEnvironment() {
        return Boolean.parseBoolean(System.getenv(GITHUB_ACTIONS));
    }
}
