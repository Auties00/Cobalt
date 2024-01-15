package it.auties.whatsapp.model;

public final class GithubActions {
    public static final String CREDENTIALS_NAME = "WHATSAPP_KEYS";
    public static final String STORE_NAME = "WHATSAPP_STORE";
    public static final String CONTACT_NAME = "WHATSAPP_CONTACT";
    public static final String GPG_PASSWORD = "GPG_PASSWORD";
    private static final String GITHUB_ACTIONS = "GITHUB_ACTIONS";
    private static final String RELEASE_ENV = "RELEASE";

    public static boolean isActionsEnvironment() {
        return Boolean.parseBoolean(System.getenv(GITHUB_ACTIONS));
    }

    public static boolean isReleaseEnv() {
        return Boolean.parseBoolean(System.getenv(RELEASE_ENV));
    }
}
