package com.github.auties00.cobalt.wire.graphql.whatsapp;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Enumerates the {@code graph.whatsapp.com} environments a {@link WhatsAppGraphQlOperation.Request} can
 * target.
 *
 * <p>Each constant maps to one of the {@code environmentType} values WhatsApp Web's
 * {@code WAWebRelayEnvironment.getEnvironment} routes to the {@code graph.whatsapp.com} host, and it
 * carries the three per-environment concerns that transport diverges on: the request
 * {@link #endpoint() endpoint}, the {@link #localeParameterName() locale parameter name}, and the
 * shared app-level {@link #defaultAccessToken() default access token}. All three environments are
 * session-independent; they differ only in which endpoint they hit, whether the locale rides under
 * {@code locale} or {@code lang}, and which app token (if any) authenticates them.
 *
 * <ul>
 * <li>{@link #WWW} maps to {@code whatsapp_www}: the {@code DEFAULT_ENDPOINT}
 * endpoint, the {@code locale} parameter, and the {@code WWW_ACCESS_TOKEN}
 * default token.</li>
 * <li>{@link #GUEST} maps to {@code whatsapp_guest}: the {@code DEFAULT_ENDPOINT}
 * endpoint, the {@code lang} parameter, and no default token (the caller must supply one via
 * {@link WhatsAppGraphQlOperation.Request#accessToken()}).</li>
 * <li>{@link #CATALOG} maps to {@code whatsapp_catalog} (WhatsApp Web's default environment): the
 * {@code CATALOG_ENDPOINT} endpoint, the {@code lang} parameter, and the
 * {@code CATALOG_ACCESS_TOKEN} default token.</li>
 * </ul>
 */
@WhatsAppWebModule(moduleName = "WAWebRelayEnvironment")
@WhatsAppWebModule(moduleName = "WAWebGraphQLConstants")
public enum WhatsAppGraphQlEnvironment {
    /**
     * The {@code whatsapp_www} environment: signup and pre-login {@code graph.whatsapp.com} reads keyed
     * by the {@code WWW_ACCESS_TOKEN} shared app token, with the locale sent under
     * the {@code locale} parameter.
     */
    WWW(WhatsAppGraphQlEnvironment.DEFAULT_ENDPOINT, "locale", WhatsAppGraphQlEnvironment.WWW_ACCESS_TOKEN),

    /**
     * The {@code whatsapp_guest} environment: guest {@code graph.whatsapp.com} reads that carry no
     * shared app token, so the caller must supply one through
     * {@link WhatsAppGraphQlOperation.Request#accessToken()}; the locale is sent under the {@code lang}
     * parameter.
     */
    GUEST(WhatsAppGraphQlEnvironment.DEFAULT_ENDPOINT, "lang", null),

    /**
     * The {@code whatsapp_catalog} environment (WhatsApp Web's default): public catalog reads served by
     * the {@code CATALOG_ENDPOINT} endpoint and keyed by the
     * {@code CATALOG_ACCESS_TOKEN} shared app token, with the locale sent under
     * the {@code lang} parameter.
     */
    CATALOG(WhatsAppGraphQlEnvironment.CATALOG_ENDPOINT, "lang", WhatsAppGraphQlEnvironment.CATALOG_ACCESS_TOKEN);

    /**
     * The default {@code graph.whatsapp.com} GraphQL endpoint used by the {@link #WWW} and
     * {@link #GUEST} environments.
     */
    public static final String DEFAULT_ENDPOINT = "https://graph.whatsapp.com/graphql/";

    /**
     * The catalog {@code graph.whatsapp.com} GraphQL endpoint used by the {@link #CATALOG} environment.
     */
    public static final String CATALOG_ENDPOINT = "https://graph.whatsapp.com/graphql/catalog";

    /**
     * The shared app-level access token authenticating the {@link #WWW} environment.
     */
    public static final String WWW_ACCESS_TOKEN = "WA|368348580915920|f740dc6ab59f4466ba09052010768cc1";

    /**
     * The shared app-level access token authenticating the {@link #CATALOG} environment.
     */
    public static final String CATALOG_ACCESS_TOKEN = "WA|787118555984857|7bb1544a3599aa180ac9a3f7688ba243";

    /**
     * The {@code graph.whatsapp.com} endpoint this environment targets.
     */
    private final String endpoint;

    /**
     * The JSON body key under which the remapped locale is sent ({@code locale} or {@code lang}).
     */
    private final String localeParameterName;

    /**
     * The shared app-level {@code access_token} that authenticates this environment, or {@code null}
     * when the environment carries no default and the caller must supply one.
     */
    private final String defaultAccessToken;

    /**
     * Constructs an environment constant.
     *
     * @param endpoint            the {@code graph.whatsapp.com} endpoint this environment targets
     * @param localeParameterName the JSON body key under which the locale is sent
     * @param defaultAccessToken  the shared app-level access token, or {@code null} when none applies
     */
    WhatsAppGraphQlEnvironment(String endpoint, String localeParameterName, String defaultAccessToken) {
        this.endpoint = endpoint;
        this.localeParameterName = localeParameterName;
        this.defaultAccessToken = defaultAccessToken;
    }

    /**
     * Returns the {@code graph.whatsapp.com} endpoint this environment targets.
     *
     * @return the request endpoint, never {@code null}
     */
    public String endpoint() {
        return endpoint;
    }

    /**
     * Returns the JSON body key under which the remapped locale is sent for this environment.
     *
     * <p>The key is {@code "locale"} for {@link #WWW} and {@code "lang"} for {@link #GUEST} and
     * {@link #CATALOG}.
     *
     * @return the locale parameter name, never {@code null}
     */
    public String localeParameterName() {
        return localeParameterName;
    }

    /**
     * Returns the shared app-level {@code access_token} that authenticates this environment by default.
     *
     * <p>The token is present for {@link #WWW} and {@link #CATALOG} and empty for {@link #GUEST}, whose
     * requests must supply a token through {@link WhatsAppGraphQlOperation.Request#accessToken()}.
     *
     * @return an {@link Optional} wrapping the default app token, or {@link Optional#empty()} when the
     *         environment carries none
     */
    public Optional<String> defaultAccessToken() {
        return Optional.ofNullable(defaultAccessToken);
    }
}
