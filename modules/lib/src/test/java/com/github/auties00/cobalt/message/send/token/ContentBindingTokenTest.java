package com.github.auties00.cobalt.message.send.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Branch-coverage tests for {@link ContentBindingToken#getContentIdString(String, boolean)}
 * and {@link ContentBindingToken#resolveContentId(String)}, exercising the
 * canonical YouTube URL shapes, the {@code youtubeOnly} flag branches, the
 * null/empty input paths, and UTF-8 encoding of the resolved id. The
 * assertions use the stable canonical YouTube id {@code dQw4w9WgXcQ} so they
 * read as content-equality checks rather than length-or-shape checks.
 */
@DisplayName("ContentBindingToken")
class ContentBindingTokenTest {

    @Test
    @DisplayName("getContentIdString: YouTube watch URL resolves to 11-char video id")
    void youtubeWatchUrl() {
        assertEquals("dQw4w9WgXcQ",
                ContentBindingToken.getContentIdString("https://www.youtube.com/watch?v=dQw4w9WgXcQ", true));
        assertEquals("dQw4w9WgXcQ",
                ContentBindingToken.getContentIdString("https://youtube.com/watch?v=dQw4w9WgXcQ", true));
    }

    @Test
    @DisplayName("getContentIdString: youtu.be short URL resolves to 11-char video id")
    void youtubeShortUrl() {
        assertEquals("dQw4w9WgXcQ",
                ContentBindingToken.getContentIdString("https://youtu.be/dQw4w9WgXcQ", true));
    }

    @Test
    @DisplayName("getContentIdString: youtube.com/shorts URL resolves to 11-char video id")
    void youtubeShortsUrl() {
        assertEquals("dQw4w9WgXcQ",
                ContentBindingToken.getContentIdString("https://youtube.com/shorts/dQw4w9WgXcQ", true));
    }

    @Test
    @DisplayName("getContentIdString(youtubeOnly=true) on non-YouTube URL returns null")
    void nonYoutubeWithYoutubeOnly() {
        assertNull(ContentBindingToken.getContentIdString("https://example.com/page", true));
    }

    @Test
    @DisplayName("getContentIdString(youtubeOnly=false) on non-YouTube URL returns matched text verbatim")
    void nonYoutubeFallsBackToMatched() {
        assertEquals("https://example.com/page",
                ContentBindingToken.getContentIdString("https://example.com/page", false));
    }

    @Test
    @DisplayName("getContentIdString: null and empty input return null")
    void nullEmptyInput() {
        assertNull(ContentBindingToken.getContentIdString(null, false));
        assertNull(ContentBindingToken.getContentIdString("", false));
    }

    @Test
    @DisplayName("resolveContentId: returns UTF-8 bytes of the resolved id")
    void resolveContentIdReturnsBytes() {
        var bytes = ContentBindingToken.resolveContentId("https://example.com/page");
        assertArrayEquals("https://example.com/page".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    @DisplayName("resolveContentId: null input throws NullPointerException")
    void resolveContentIdNullThrows() {
        assertThrows(NullPointerException.class, () -> ContentBindingToken.resolveContentId(null));
    }
}
