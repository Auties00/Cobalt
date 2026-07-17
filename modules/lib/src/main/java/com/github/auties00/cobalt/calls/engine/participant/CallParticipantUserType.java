package com.github.auties00.cobalt.calls.engine.participant;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enumerates the user type classification the engine assigns to a call participant.
 *
 * <p>Every participant carries a user type derived from the {@code type} attribute on
 * its membership stanza. The type distinguishes an ordinary human participant
 * ({@link #NORMAL}) from one that has an associated bot ({@link #HAS_BOT}) and from a
 * participant that is itself a bot ({@link #BOT}). The classification is read from the
 * wire token: an absent or empty token means {@link #NORMAL}, {@code "has-bot"} means
 * {@link #HAS_BOT}, and {@code "bot"} means {@link #BOT}.
 *
 * <p>Each constant carries the {@link #code() integer code} the engine assigns:
 * {@link #NORMAL} is {@code 1}, {@link #HAS_BOT} is {@code 2}, and {@link #BOT} is
 * {@code 3}. Code {@code 0} is reserved as an error sentinel for an unrecognized token;
 * this enum has no constant for that value, and {@link #ofToken(String)} returns
 * {@link Optional#empty()} for any token it cannot classify.
 */
public enum CallParticipantUserType {
    /**
     * An ordinary human participant with no associated bot.
     *
     * <p>This is the classification for an absent or empty {@code type} token.
     */
    NORMAL(1, ""),

    /**
     * A participant that has an associated bot.
     */
    HAS_BOT(2, "has-bot"),

    /**
     * A participant that is itself a bot.
     */
    BOT(3, "bot");

    /**
     * Resolves a wire token to its user type, backing {@link #ofToken(String)}.
     *
     * <p>Built once at class initialization from each constant's {@link #token}, so a token resolves to
     * its type in constant time rather than by scanning {@link #values()}. Keys are the raw tokens, so
     * matching is case sensitive, preserving the {@link String#equals(Object)} semantics this lookup
     * replaces. The empty token of {@link #NORMAL} is handled by {@link #ofToken(String)} before this map
     * is consulted.
     */
    private static final Map<String, CallParticipantUserType> BY_TOKEN;

    /**
     * Resolves an engine code to its user type, backing {@link #ofCode(int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #code}, so a code resolves to its
     * type in constant time rather than by scanning {@link #values()}.
     */
    private static final Map<Integer, CallParticipantUserType> BY_CODE;

    static {
        var byToken = new HashMap<String, CallParticipantUserType>();
        var byCode = new HashMap<Integer, CallParticipantUserType>();
        for (var type : values()) {
            if (byToken.put(type.token, type) != null) {
                throw new AssertionError("Conflict");
            }
            if (byCode.put(type.code, type) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_TOKEN = Map.copyOf(byToken);
        BY_CODE = Map.copyOf(byCode);
    }

    /**
     * The integer code reserved by the engine for an unrecognized user type token.
     */
    private static final int ERROR_CODE = 0;

    /**
     * The integer code the engine assigns to this user type.
     */
    private final int code;

    /**
     * The wire token that selects this user type, empty for {@link #NORMAL}.
     */
    private final String token;

    /**
     * Constructs a user type constant bound to its engine code and wire token.
     *
     * @param code  the integer code the engine assigns
     * @param token the wire token that selects this type
     */
    CallParticipantUserType(int code, String token) {
        this.code = code;
        this.token = token;
    }

    /**
     * Returns the integer code the engine assigns to this user type.
     *
     * @return the engine code
     */
    public int code() {
        return code;
    }

    /**
     * Returns the wire token that selects this user type.
     *
     * @return the wire token, empty for {@link #NORMAL}
     */
    public String token() {
        return token;
    }

    /**
     * Returns the user type selected by the given wire token.
     *
     * <p>A {@code null} or empty token resolves to {@link #NORMAL}; {@code "has-bot"} and
     * {@code "bot"} resolve to their constants; any other token yields
     * {@link Optional#empty()}, matching the engine's error sentinel.
     *
     * @implNote This implementation resolves a non-empty token through the prebuilt {@link #BY_TOKEN} map
     * rather than scanning {@link #values()}; the {@code null} and empty cases are still short circuited to
     * {@link #NORMAL} first.
     * @param token the wire token to classify, may be {@code null}
     * @return the matching user type, or {@link Optional#empty()} if the token is
     *         unrecognized
     */
    public static Optional<CallParticipantUserType> ofToken(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.of(NORMAL);
        }
        return Optional.ofNullable(BY_TOKEN.get(token));
    }

    /**
     * Returns the user type whose {@linkplain #code() code} equals the given value.
     *
     * <p>The engine's error sentinel ({@code 0}) and any other unmapped value yield
     * {@link Optional#empty()}.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_CODE} map rather than
     * scanning {@link #values()}; the {@link #ERROR_CODE} sentinel is still short circuited to
     * {@link Optional#empty()} first.
     * @param code the engine code to resolve
     * @return the matching user type, or {@link Optional#empty()} if no type matches
     */
    public static Optional<CallParticipantUserType> ofCode(int code) {
        if (code == ERROR_CODE) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
