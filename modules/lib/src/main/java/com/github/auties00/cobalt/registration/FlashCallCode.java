package com.github.auties00.cobalt.registration;

import com.alibaba.fastjson2.JSONObject;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The instructions a {@code /v2/code} reply carries for reading a
 * verification code out of the number a flash call rings from.
 *
 * <p>A flash call delivers no code of its own. WhatsApp rings the
 * number being registered from a one-time caller id and drops the call
 * before it can be answered, and the verification code is embedded in
 * that caller id. The reply to the {@code /v2/code} request that asked
 * for the call states exactly how to read it: {@code cli_filter} is a
 * regular expression isolating the code after {@code cli_prefix},
 * {@code length} is how many digits the code is, {@code cli_cc} is the
 * country code the call originates from, and {@code flash_timeout} is
 * how long the code stays valid after the call.
 *
 * <p>Reading the code is a two-step reduction of whatever the caller
 * hands over. The digits are taken first, so the same code is reached
 * whether the number was typed as {@code "+39 373 9799312"},
 * {@code "00393739799312"} or already trimmed to its tail. The filter
 * is then applied to isolate the part that follows the prefix, and the
 * last {@link #codeLength()} digits of that part are the code. Without
 * a usable filter the trailing digits of the whole caller id are taken
 * instead, which is right whenever the code sits flush at the end.
 *
 * @param cliCc          the country code the call originates from, or
 *                       {@code null} if the server did not state one
 * @param cliPrefix      the dialling prefix that precedes the code, or
 *                       {@code null} if the server did not state one
 * @param cliFilter      the regular expression isolating the code, or
 *                       {@code null} if the server did not state one
 * @param codeLength     the number of digits the code is
 * @param timeoutSeconds how long the code stays valid after the call,
 *                       in seconds, or {@code 0} if the server did not
 *                       state it
 */
record FlashCallCode(String cliCc, String cliPrefix, String cliFilter, int codeLength, int timeoutSeconds) {
    /**
     * The code length assumed when the server names none.
     *
     * <p>Six digits is what the native Android client defaults to.
     * Flash-call providers vary between four and six, which is why the
     * server's own {@code length} wins over this value whenever it is
     * present.
     */
    static final int DEFAULT_CODE_LENGTH = 6;

    /**
     * Matches every character that is not an ASCII digit.
     */
    private static final Pattern NON_DIGIT = Pattern.compile("\\D");

    /**
     * Normalises the parsed fields so the record never carries a blank
     * string or a nonsensical length.
     *
     * <p>Blank strings become {@code null} and a non-positive length
     * becomes {@link #DEFAULT_CODE_LENGTH}, so {@link #read(String)}
     * can treat every field as either absent or usable.
     */
    FlashCallCode {
        cliCc = emptyToNull(cliCc);
        cliPrefix = emptyToNull(cliPrefix);
        cliFilter = emptyToNull(cliFilter);
        if (codeLength <= 0) {
            codeLength = DEFAULT_CODE_LENGTH;
        }
        if (timeoutSeconds < 0) {
            timeoutSeconds = 0;
        }
    }

    /**
     * Returns the given string, or {@code null} when it is
     * {@code null} or blank.
     *
     * <p>Backs the canonical constructor's normalisation of the three
     * optional {@code cli_*} fields.
     *
     * @param value the string to normalise
     * @return the string, or {@code null} if it carries nothing
     */
    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * Reads the flash-call instructions out of a successful
     * {@code /v2/code} reply.
     *
     * <p>Every field is optional: a reply that states none of them
     * yields a record that falls back to taking the last
     * {@link #DEFAULT_CODE_LENGTH} digits of the caller id.
     *
     * @param response the parsed {@code /v2/code} JSON response; never
     *                 {@code null}
     * @return the instructions carried by the reply
     * @throws NullPointerException if {@code response} is {@code null}
     */
    static FlashCallCode of(JSONObject response) {
        Objects.requireNonNull(response, "response cannot be null");
        var length = response.getInteger("length");
        var timeout = response.getInteger("flash_timeout");
        return new FlashCallCode(
                response.getString("cli_cc"),
                response.getString("cli_prefix"),
                response.getString("cli_filter"),
                length == null ? 0 : length,
                timeout == null ? 0 : timeout
        );
    }

    /**
     * Returns a copy of these instructions with the caller's own code
     * length and filter substituted for the server's.
     *
     * <p>An empty override leaves the corresponding server value in
     * place, so a caller can state one without discarding the other.
     *
     * @param length the code length to use instead of the server's, or
     *               empty to keep the server's
     * @param filter the filter to use instead of the server's, or
     *               empty to keep the server's
     * @return the overridden instructions
     * @throws NullPointerException if {@code length} or {@code filter}
     *                              is {@code null}
     */
    FlashCallCode withOverrides(OptionalInt length, Optional<String> filter) {
        Objects.requireNonNull(length, "length cannot be null");
        Objects.requireNonNull(filter, "filter cannot be null");
        return new FlashCallCode(
                cliCc,
                cliPrefix,
                filter.orElse(cliFilter),
                length.orElse(codeLength),
                timeoutSeconds
        );
    }

    /**
     * Reads the verification code out of the caller id of the flash
     * call.
     *
     * <p>The input is reduced to its digits, {@link #cliFilter()} is
     * applied to isolate the part that follows the prefix, and the last
     * {@link #codeLength()} digits of that part are returned. A part
     * shorter than the code length is returned whole: the caller has
     * already typed just the tail, or the handset showed a short
     * number, and trimming further would only corrupt it.
     *
     * {@snippet :
     * // cli_filter "(.*)373(.*)", length 6
     * var code = new FlashCallCode("39", "373", "(.*)373(.*)", 6, 60)
     *         .read("+39 373 9799312"); // "799312"
     * }
     *
     * @param callerId the number the flash call rang from, in whatever
     *                 form the user read it off the handset
     * @return the verification code to submit to {@code /v2/register}
     */
    String read(String callerId) {
        var digits = digits(callerId);
        var filtered = applyFilter(digits, cliFilter);
        var base = filtered == null ? digits : filtered;
        return base.length() > codeLength ? base.substring(base.length() - codeLength) : base;
    }

    /**
     * Returns the digits carried by the given string, in order.
     *
     * <p>A {@code null} input yields an empty string, so a caller that
     * never obtained a caller id reaches the server with an empty code
     * rather than a {@link NullPointerException}.
     *
     * @param raw the string to reduce
     * @return the digits of {@code raw}, possibly empty
     */
    static String digits(String raw) {
        return raw == null ? "" : NON_DIGIT.matcher(raw).replaceAll("");
    }

    /**
     * Applies the server's {@code cli_filter} to the digits of a caller
     * id and returns the group holding the code.
     *
     * <p>The code follows the prefix, so it is the last capture group
     * that carries any digits; earlier groups hold the country code and
     * the prefix. Returns {@code null} when there is no filter, the
     * filter does not compile, the filter does not match, or no group
     * carries digits, which leaves the caller free to fall back to the
     * trailing digits of the whole caller id.
     *
     * @implNote
     * This implementation exists because the trailing digits of a
     * caller id are the code only by coincidence. The national part of
     * an Italian {@code +39 373 XXXXXXX} caller id carries seven digits
     * after the {@code 373} prefix while the code is six, so taking the
     * last six of the whole number slices one digit into the code and
     * the server answers {@code mismatch} for a caller id that was read
     * off the handset correctly.
     *
     * @param digits the digits of the caller id
     * @param filter the regular expression the server supplied, or
     *               {@code null}
     * @return the digits of the group holding the code, or
     *         {@code null} if the filter yielded nothing usable
     */
    static String applyFilter(String digits, String filter) {
        if (filter == null || filter.isBlank()) {
            return null;
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(filter);
        } catch (PatternSyntaxException exception) {
            return null;
        }

        var matcher = pattern.matcher(digits);
        if (!matcher.find()) {
            return null;
        }

        for (var group = matcher.groupCount(); group >= 1; group--) {
            var value = digits(matcher.group(group));
            if (!value.isEmpty()) {
                return value;
            }
        }

        return null;
    }
}
