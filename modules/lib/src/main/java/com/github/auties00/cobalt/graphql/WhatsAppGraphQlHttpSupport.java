package com.github.auties00.cobalt.graphql;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.regex.Pattern;

/**
 * Shared helpers for WhatsApp Web's same-origin X-Controller POST plumbing.
 *
 * <p>The {@code /graphql/} relay and the {@code /auth/token/} canonical exchange both speak the same FB
 * anti-CSRF dialect: every response is prefixed with the {@code for(;;);} anti-JSON-hijack guard, and
 * every request carries a {@code jazoest} checksum derived from the {@code lsd} token. This class
 * centralises those two computations so {@link WhatsAppWebGraphQlClient} and the credential-acquisition
 * subsystem do not each carry their own copy.
 */
@WhatsAppWebModule(moduleName = "WAWebXControllerFetchUtils")
public final class WhatsAppGraphQlHttpSupport {
    /**
     * Matches the leading {@code for(;;);} anti-JSON-hijack prefix WhatsApp Web prepends to every
     * X-Controller response body.
     */
    private static final Pattern XSSI_PREFIX = Pattern.compile("^for\\s*\\(\\s*;;\\s*\\)\\s*;\\s*");

    /**
     * Prevents instantiation of this stateless helper.
     *
     * <p>All behaviour is exposed through static methods.
     */
    private WhatsAppGraphQlHttpSupport() {
        throw new AssertionError("WhatsAppGraphQlHttpSupport is not instantiable");
    }

    /**
     * Strips the leading {@code for(;;);} anti-JSON-hijack prefix from an X-Controller response body.
     *
     * <p>WhatsApp Web (and Meta's graph endpoints generally) prepend {@code for(;;);} to JSON responses
     * so a {@code <script>}-tag hijack cannot read them; the prefix must be removed before parsing.
     * Bodies without the prefix pass through unchanged.
     *
     * @param body the raw response body text
     * @return the body with any leading {@code for(;;);} prefix removed
     */
    public static String stripXssiPrefix(String body) {
        return XSSI_PREFIX.matcher(body).replaceFirst("");
    }

    /**
     * Derives the {@code jazoest} anti-CSRF checksum from an {@code lsd} token.
     *
     * <p>The checksum is the literal {@code "2"} followed by the decimal sum of the UTF-16 code units
     * of the token, matching WhatsApp Web's well-known computation. For example the captured token of
     * length 22 produced {@code "21795"}.
     *
     * @param token the {@code lsd} token
     * @return the {@code jazoest} value
     */
    @WhatsAppWebExport(moduleName = "WAWebXControllerFetchUtils", exports = "getJazoest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static String jazoest(String token) {
        var sum = 0;
        for (var i = 0; i < token.length(); i++) {
            sum += token.charAt(i);
        }
        return "2" + sum;
    }
}
