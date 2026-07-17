package com.github.auties00.cobalt.calls.engine.participant;

/**
 * Enumerates the account kind the engine assigns to a call participant.
 *
 * <p>Every participant is classified as either a regular account ({@link #REGULAR}) or a
 * guest account ({@link #GUEST}) based on the {@code account_kind} attribute on its
 * membership stanza. A guest is a participant who joined through a call link without a
 * full account; every other participant is regular.
 *
 * <p>Each constant carries the {@link #code() integer code} the engine assigns. The token
 * {@code "guest"} maps to {@link #GUEST} and every other token, including an absent one,
 * maps to {@link #REGULAR}; there is therefore no error sentinel and
 * {@link #ofToken(String)} always resolves.
 */
public enum CallParticipantAccountKind {
    /**
     * A regular account participant.
     *
     * <p>This is the classification for any token other than {@code "guest"}, including
     * an absent token.
     */
    REGULAR(0, "regular"),

    /**
     * A guest account participant, typically one who joined through a call link.
     */
    GUEST(1, "guest");

    /**
     * The integer code the engine assigns to this account kind.
     */
    private final int code;

    /**
     * The wire token that selects this account kind.
     */
    private final String token;

    /**
     * Constructs an account kind constant bound to its engine code and wire token.
     *
     * @param code  the integer code the engine assigns
     * @param token the wire token that selects this kind
     */
    CallParticipantAccountKind(int code, String token) {
        this.code = code;
        this.token = token;
    }

    /**
     * Returns the integer code the engine assigns to this account kind.
     *
     * @return the engine code
     */
    public int code() {
        return code;
    }

    /**
     * Returns the wire token that selects this account kind.
     *
     * <p>Only {@link #GUEST} carries a token that is significant on the wire;
     * {@link #REGULAR} is the default for any other token.
     *
     * @return the wire token
     */
    public String token() {
        return token;
    }

    /**
     * Returns the account kind selected by the given wire token.
     *
     * <p>The token {@code "guest"} resolves to {@link #GUEST}; every other token,
     * including {@code null}, resolves to {@link #REGULAR}, matching the engine's
     * default.
     *
     * @param token the wire token to classify, may be {@code null}
     * @return the matching account kind, never {@code null}
     */
    public static CallParticipantAccountKind ofToken(String token) {
        return GUEST.token.equals(token) ? GUEST : REGULAR;
    }

    /**
     * Returns the account kind whose {@linkplain #code() code} equals the given value.
     *
     * <p>Any value other than {@code 1} resolves to {@link #REGULAR}, matching the
     * engine's default.
     *
     * @param code the engine code to resolve
     * @return the matching account kind, never {@code null}
     */
    public static CallParticipantAccountKind ofCode(int code) {
        return code == GUEST.code ? GUEST : REGULAR;
    }
}
