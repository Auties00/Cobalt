package it.auties.whatsapp.util;

public final class GithubActions {
    public static final String CONTACT_NAME = "WHATSAPP_CONTACT";
    public static final String ACCOUNT = "ACCOUNT";
    private static final String GITHUB_ACTIONS = "GITHUB_ACTIONS";

    public static boolean isActionsEnvironment() {
        return Boolean.parseBoolean(System.getenv(GITHUB_ACTIONS));
    }
}
