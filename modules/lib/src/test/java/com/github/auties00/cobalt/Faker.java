package com.github.auties00.cobalt;

import java.security.SecureRandom;
import java.util.List;

/**
 * Test-harness generator of random fake data, used to feed plausible inputs to tests that need a
 * throwaway phone number.
 */
public final class Faker {
    // Italian mobile prefixes. The country code is always 39.
    private static final List<String> ITALIAN_MOBILE_PREFIXES = List.of(
            "320", "327", "328", "329",
            "330", "331", "333", "334", "335", "336", "338", "339",
            "340", "342", "345", "346", "347", "348", "349",
            "350", "351",
            "366", "368",
            "380", "388", "389",
            "391", "392", "393"
    );

    private static final SecureRandom RNG = new SecureRandom();

    private Faker() {
    }

    public static long randomItalianMobile() {
        var prefix = ITALIAN_MOBILE_PREFIXES.get(RNG.nextInt(ITALIAN_MOBILE_PREFIXES.size()));
        var subscriber = String.format("%07d", RNG.nextInt(10_000_000));
        return Long.parseLong("39" + prefix + subscriber);
    }
}
