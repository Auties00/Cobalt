package com.github.auties00.cobalt.util;

import net.dongliu.apk.parser.ByteArrayApkFile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration smoke tests for {@link PlayStoreUtils} that download the live WhatsApp and WhatsApp
 * Business APKs from the production Google Play backend, then assert the base APK and every split
 * parse as APKs whose package name matches the request. These tests require network access. The
 * synthetic device profile reports an x86_64 emulator, so any ABI-keyed split must resolve to
 * {@code x86} or {@code x86_64}; an ARM split would mean the dispenser fed through the wrong profile.
 */
public class PlayStoreUtilsTest {
    private static final Pattern ABI_SPLIT = Pattern.compile(
            "^config\\.(armeabi(?:_v7a)?|arm64_v8a|x86|x86_64|mips|mips64)$"
    );

    @Test
    public void downloadsWhatsApp() throws IOException {
        assertDownloadsValidApk("com.whatsapp");
    }

    @Test
    public void downloadsWhatsAppBusiness() throws IOException {
        assertDownloadsValidApk("com.whatsapp.w4b");
    }

    private static void assertDownloadsValidApk(String packageName) throws IOException {
        try (var downloaded = PlayStoreUtils.downloadApk(packageName)) {
            assertEquals(packageName, downloaded.packageName());
            assertApkPackage(packageName, "base", downloaded.baseApk());
            for (var entry : downloaded.splits().entrySet()) {
                var splitName = entry.getKey();
                assertFalse(splitName.isBlank(),
                        () -> packageName + " split has a blank name");
                var abiMatch = ABI_SPLIT.matcher(splitName);
                if (abiMatch.matches()) {
                    var abi = abiMatch.group(1);
                    assertTrue(abi.equals("x86") || abi.equals("x86_64"),
                            () -> packageName + " received non-x86 ABI split '" + splitName
                                    + "' despite x86_64 device profile");
                }
                assertApkPackage(packageName, splitName, entry.getValue());
            }
        }
    }

    private static void assertApkPackage(String expectedPackage, String label, InputStream stream) throws IOException {
        try (var parsed = new ByteArrayApkFile(stream.readAllBytes())) {
            var meta = parsed.getApkMeta();
            assertNotNull(meta, () -> expectedPackage + "/" + label + " has no parseable ApkMeta");
            assertEquals(expectedPackage, meta.getPackageName(),
                    () -> expectedPackage + "/" + label + " has unexpected package name");
        }
    }
}
