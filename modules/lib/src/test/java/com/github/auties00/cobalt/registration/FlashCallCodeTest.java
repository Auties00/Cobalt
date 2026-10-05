package com.github.auties00.cobalt.registration;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers reading a flash-call verification code out of the number the call rang from.
 *
 * <p>The fixture values are the ones a live {@code /v2/code} reply carried for an Italian number:
 * the caller id {@code +39 373 9799312}, the filter {@code (.*)373(.*)} and a six-digit code. That
 * case is the one a blind last-six slice gets wrong, because the national part after the
 * {@code 373} prefix is seven digits long.
 */
@DisplayName("Flash call code reading")
class FlashCallCodeTest {
    private static final String CALLER_ID = "+39 373 9799312";
    private static final String FILTER = "(.*)373(.*)";
    private static final int LENGTH = 6;
    private static final String CODE = "799312";

    @Nested
    @DisplayName("applying the server's cli_filter")
    class ApplyFilter {
        @Test
        @DisplayName("isolates the digits that follow the prefix")
        void isolatesTailAfterPrefix() {
            assertEquals("9799312", FlashCallCode.applyFilter("393739799312", FILTER));
        }

        @Test
        @DisplayName("takes the last capturing group that holds digits, not an earlier one")
        void takesLastDigitBearingGroup() {
            assertEquals("9799312", FlashCallCode.applyFilter("393739799312", "(39)(.*)373(.*)"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"(", "(.*)999(.*)"})
        @DisplayName("yields nothing for a filter that does not compile or does not match")
        void unusableFilterYieldsNothing(String filter) {
            assertNull(FlashCallCode.applyFilter("393739799312", filter));
        }

        @Test
        @DisplayName("yields nothing when the server stated no filter")
        void absentFilterYieldsNothing() {
            assertNull(FlashCallCode.applyFilter("393739799312", null));
            assertNull(FlashCallCode.applyFilter("393739799312", "  "));
        }
    }

    @Nested
    @DisplayName("reading the code")
    class Read {
        @Test
        @DisplayName("lands on the code the server expects rather than a blind last six")
        void liveCase() {
            assertEquals(CODE, flash(FILTER, LENGTH).read(CALLER_ID));
        }

        @ParameterizedTest
        @CsvSource({
                "+39 373 9799312, 799312",
                "+393739799312,   799312",
                "00393739799312,  799312",
                "799312,          799312"
        })
        @DisplayName("reaches the same code however the caller id was typed")
        void formatIsIrrelevant(String typed, String expected) {
            assertEquals(expected, flash(FILTER, LENGTH).read(typed));
        }

        @Test
        @DisplayName("falls back to the trailing digits when the server stated no filter")
        void noFilterTakesTrailingDigits() {
            assertEquals("799312", flash(null, LENGTH).read("393739799312"));
        }

        @Test
        @DisplayName("keeps a tail shorter than the code length rather than guessing at it")
        void shortTailIsKept() {
            assertEquals("1234", flash(FILTER, LENGTH).read("3731234"));
        }

        @Test
        @DisplayName("defaults to six digits when the server stated no length")
        void defaultsToSixDigits() {
            assertEquals(FlashCallCode.DEFAULT_CODE_LENGTH, flash(FILTER, 0).codeLength());
            assertEquals(CODE, flash(FILTER, 0).read(CALLER_ID));
        }
    }

    @Nested
    @DisplayName("parsing the /v2/code reply")
    class Parsing {
        @Test
        @DisplayName("carries every field the reply stated")
        void readsEveryField() {
            var response = new JSONObject();
            response.put("cli_cc", "39");
            response.put("cli_prefix", "373");
            response.put("cli_filter", FILTER);
            response.put("length", LENGTH);
            response.put("flash_timeout", 60);

            var parsed = FlashCallCode.of(response);
            assertEquals("39", parsed.cliCc());
            assertEquals("373", parsed.cliPrefix());
            assertEquals(FILTER, parsed.cliFilter());
            assertEquals(LENGTH, parsed.codeLength());
            assertEquals(60, parsed.timeoutSeconds());
            assertEquals(CODE, parsed.read(CALLER_ID));
        }

        @Test
        @DisplayName("tolerates a reply that stated none of them")
        void toleratesEmptyReply() {
            var parsed = FlashCallCode.of(new JSONObject());
            assertNull(parsed.cliCc());
            assertNull(parsed.cliPrefix());
            assertNull(parsed.cliFilter());
            assertEquals(FlashCallCode.DEFAULT_CODE_LENGTH, parsed.codeLength());
            assertEquals(0, parsed.timeoutSeconds());
            assertEquals("799312", parsed.read(CALLER_ID));
        }
    }

    @Nested
    @DisplayName("overriding what the server stated")
    class Overrides {
        @Test
        @DisplayName("a stated length wins over the server's")
        void lengthWins() {
            // the tail after the prefix is "9799312"; its last four digits
            var overridden = flash(FILTER, LENGTH).withOverrides(OptionalInt.of(4), Optional.empty());
            assertEquals("9312", overridden.read(CALLER_ID));
        }

        @Test
        @DisplayName("a stated filter wins over the server's")
        void filterWins() {
            var overridden = flash(null, LENGTH).withOverrides(OptionalInt.empty(), Optional.of("(.*)373(.*)"));
            assertEquals(CODE, overridden.read(CALLER_ID));
        }

        @Test
        @DisplayName("stating neither keeps both of the server's")
        void emptyOverridesChangeNothing() {
            var instructions = flash(FILTER, LENGTH);
            assertEquals(instructions, instructions.withOverrides(OptionalInt.empty(), Optional.empty()));
        }
    }

    private static FlashCallCode flash(String filter, int length) {
        return new FlashCallCode("39", "373", filter, length, 60);
    }
}
