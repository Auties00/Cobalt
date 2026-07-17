package com.github.auties00.cobalt.device.icdc;

import com.github.auties00.cobalt.wire.linked.device.identity.ADVEncryptionType;

import java.time.Instant;
import java.util.List;

/**
 * Test-only fixture mint that exposes the package-private {@link IcdcResult} constructor to
 * test code in other packages. Lives in {@code com.github.auties00.cobalt.device.icdc} so it can
 * reach the package-private constructor, and stays on the test classpath only.
 */
public final class TestIcdcResults {
    private TestIcdcResults() {
        throw new AssertionError("TestIcdcResults is not instantiable");
    }

    /**
     * Builds an {@link IcdcResult} from the supplied components, where any argument may be
     * {@code null} to model an absent field.
     *
     * @param keyHash     the truncated identity key hash, or {@code null}
     * @param timestamp   the device-list snapshot timestamp, or {@code null}
     * @param keyIndexes  the indexes of included devices, or {@code null}
     * @param accountType the hosted account encryption type, or {@code null}
     * @return the constructed {@link IcdcResult}
     */
    public static IcdcResult create(
            byte[] keyHash,
            Instant timestamp,
            List<Integer> keyIndexes,
            ADVEncryptionType accountType
    ) {
        return new IcdcResult(keyHash, timestamp, keyIndexes, accountType);
    }

    /**
     * Builds an {@link IcdcResult} with all fields set to {@code null}.
     *
     * @return the empty {@link IcdcResult}
     */
    public static IcdcResult empty() {
        return new IcdcResult(null, null, null, null);
    }
}
