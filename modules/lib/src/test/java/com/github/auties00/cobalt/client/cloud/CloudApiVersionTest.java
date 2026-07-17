package com.github.auties00.cobalt.client.cloud;

import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CloudApiVersion")
class CloudApiVersionTest {
    @Test
    @DisplayName("Known versions sort v19 < v20 < ... < v25")
    void knownVersionsOrdered() {
        var ordered = new CloudApiVersion[]{
                CloudApiVersion.V19_0, CloudApiVersion.V20_0, CloudApiVersion.V21_0,
                CloudApiVersion.V22_0, CloudApiVersion.V23_0, CloudApiVersion.V24_0, CloudApiVersion.V25_0};
        for (var i = 0; i + 1 < ordered.length; i++) {
            assertTrue(ordered[i].compareTo(ordered[i + 1]) < 0,
                    ordered[i].version() + " must sort before " + ordered[i + 1].version());
        }
    }

    @Test
    @DisplayName("LATEST and DEFAULT track v25.0")
    void latestTracksV25() {
        assertEquals(CloudApiVersion.V25_0, CloudApiVersion.LATEST);
        assertEquals(CloudApiVersion.V25_0, CloudApiVersion.DEFAULT);
    }

    @Test
    @DisplayName("major() and minor() parse the version segment")
    void parsesMajorMinor() {
        assertEquals(23, CloudApiVersion.V23_0.major());
        assertEquals(0, CloudApiVersion.V23_0.minor());
        assertEquals(7, new CloudApiVersion("v7.3").major());
        assertEquals(3, new CloudApiVersion("v7.3").minor());
    }

    @Test
    @DisplayName("of() reuses the matching known constant and accepts unknown segments")
    void ofParsesSegments() {
        assertEquals(CloudApiVersion.V23_0, CloudApiVersion.of("v23.0"));
        assertEquals("v18.0", CloudApiVersion.of("v18.0").version());
    }

    @Test
    @DisplayName("isAtLeast() compares against a minimum inclusively")
    void isAtLeastCompares() {
        assertTrue(CloudApiVersion.of("v23.0").isAtLeast(CloudApiVersion.V22_0));
        assertTrue(CloudApiVersion.V22_0.isAtLeast(CloudApiVersion.V22_0));
        assertFalse(CloudApiVersion.V21_0.isAtLeast(CloudApiVersion.V22_0));
        assertFalse(CloudApiVersion.of("v18.0").isAtLeast(CloudApiVersion.V19_0));
    }

    @Test
    @DisplayName("of(null) and isAtLeast(null) reject null")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> CloudApiVersion.of(null));
        assertThrows(NullPointerException.class, () -> CloudApiVersion.V19_0.isAtLeast(null));
    }
}
