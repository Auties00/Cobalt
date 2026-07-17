package com.github.auties00.cobalt.wam;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Byte-identical known-answer test for the 8-byte WAM buffer header,
 * asserting Cobalt reproduces the {@code "WAM"} magic, protocol version,
 * stream id, little-endian sequence number, and channel byte against
 * vectors captured from {@code WAWebWamLibContext}.
 *
 * <p>Vectors live in {@code fixtures/wam/wam-buffer-headers.json}, pinned
 * to snapshot revision {@code 1039260921}; their ordering covers the
 * {@code 0xFFFF -> 1} lower wrap and the 256-boundary, the two cases a
 * big-endian regression would surface first. The header is reconstructed
 * in-line via {@link DataUtils#putShort} (the same primitive the
 * production writer uses) because that writer is package-private; the
 * end-to-end flush path is covered by {@code WamServiceTest}.
 */
@DisplayName("WAM buffer header KAT against live WhatsApp Web bundle")
class WamBufferHeaderKatTest {
    private static final long PINNED_SNAPSHOT_REVISION = 1039260921L;

    private static final int HEADER_SIZE = 8;

    private static final byte[] WAM_MAGIC = {'W', 'A', 'M'};

    private static final int PROTOCOL_VERSION = 5;

    // Always 1 for the regular client stream.
    private static final int STREAM_ID = 1;

    @TestFactory
    List<DynamicTest> headerBytesAgreeWithLiveBundle() {
        var fixture = WamFixtures.loadOracle("wam-buffer-headers");
        WamFixtures.requireSnapshotRevision(fixture, PINNED_SNAPSHOT_REVISION);
        var protocolVersion = fixture.getIntValue("protocolVersion");
        assertEquals(PROTOCOL_VERSION, protocolVersion,
                "protocol version drift: fixture pinned to " + protocolVersion
                        + " but Cobalt declares " + PROTOCOL_VERSION);

        var vectors = fixture.getJSONArray("vectors");
        var tests = new ArrayList<DynamicTest>(vectors.size());
        for (var entry : vectors) {
            var vector = (JSONObject) entry;
            var label = vector.getString("channelName") + "_seq" + vector.getIntValue("seqNum");
            tests.add(dynamicTest(label, () -> assertVectorAgrees(vector)));
        }
        return tests;
    }

    private static void assertVectorAgrees(JSONObject vector) {
        var channelByte = vector.getIntValue("channelByte");
        var seqNum = vector.getIntValue("seqNum");
        var expectedHex = vector.getString("bytes");

        var headerBytes = new byte[HEADER_SIZE];
        System.arraycopy(WAM_MAGIC, 0, headerBytes, 0, WAM_MAGIC.length);
        headerBytes[3] = (byte) PROTOCOL_VERSION;
        headerBytes[4] = (byte) STREAM_ID;
        DataUtils.putShort(headerBytes, 5, (short) seqNum, ByteOrder.LITTLE_ENDIAN);
        headerBytes[7] = (byte) channelByte;

        var actualHex = HexFormat.of().formatHex(headerBytes);
        assertEquals(expectedHex, actualHex,
                () -> "header bytes mismatch for channelByte=" + channelByte + " seqNum=" + seqNum);
    }
}
