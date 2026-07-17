package com.github.auties00.cobalt.wam;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;
import com.github.auties00.cobalt.wam.binary.WamGlobalEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Byte-identical known-answer test for {@link WamGlobalEncoder} against
 * vectors captured from {@code WAWebWamLibProtocol.writeGlobalAttribute},
 * exercising each named global writer (for example
 * {@link WamGlobalEncoder#writePlatform},
 * {@link WamGlobalEncoder#writeDeviceName}) on a captured (fieldId, value)
 * pair and the two generic boundary writers
 * ({@link WamGlobalEncoder#writeNullGlobal},
 * {@link WamGlobalEncoder#writeDynamicGlobal}) on synthetic boundary rows.
 *
 * <p>Agreement here is the strongest validation that Cobalt's globals
 * table stays bit-aligned with {@code WAWebWamGlobals}. Vectors live in
 * {@code fixtures/wam/wam-global-encoder.json}, pinned to snapshot
 * revision {@code 1039260921}.
 */
@DisplayName("WamGlobalEncoder KAT against live WhatsApp Web bundle")
class WamGlobalEncoderKatTest {
    private static final long PINNED_SNAPSHOT_REVISION = 1039260921L;

    // Comfortable for the longest captured global value (UUID-shaped tab id at ~38 bytes).
    private static final int MAX_BUFFER = 1_024;

    @TestFactory
    List<DynamicTest> globalBytesAgreeWithLiveBundle() {
        var fixture = WamFixtures.loadOracle("wam-global-encoder");
        WamFixtures.requireSnapshotRevision(fixture, PINNED_SNAPSHOT_REVISION);
        var vectors = fixture.getJSONArray("vectors");
        var tests = new ArrayList<DynamicTest>(vectors.size());
        for (var entry : vectors) {
            var vector = (JSONObject) entry;
            tests.add(dynamicTest(vector.getString("name"), () -> assertVectorAgrees(vector)));
        }
        return tests;
    }

    private static void assertVectorAgrees(JSONObject vector) {
        var name = vector.getString("name");
        var fieldId = vector.getIntValue("fieldId");
        var type = vector.getString("type");
        var sampleValue = vector.get("sampleValue");
        var expectedHex = vector.getString("bytes");

        var buffer = new byte[MAX_BUFFER];
        var encoder = WamEventEncoder.toBytes(buffer);

        invokeWriter(encoder, name, fieldId, type, sampleValue);

        var written = encoder.written();
        var actualHex = HexFormat.of().formatHex(buffer, 0, written);
        assertEquals(expectedHex, actualHex,
                () -> "global bytes mismatch for " + name + " (fieldId=" + fieldId + ", type=" + type + ")");
    }

    // Named rows exercise their matching WamGlobalEncoder helper directly so a
    // single-global regression fails that named row, not the catch-all; synthetic
    // boundary names (null_tinyId, int_zero, str_empty) fall through to invokeBoundaryWriter.
    private static void invokeWriter(WamEventEncoder encoder, String name, int fieldId, String type, Object sampleValue) {
        switch (name) {
            case "mnc" -> WamGlobalEncoder.writeMnc(((Number) sampleValue).longValue(), encoder);
            case "mcc" -> WamGlobalEncoder.writeMcc(((Number) sampleValue).longValue(), encoder);
            case "platform" -> WamGlobalEncoder.writePlatform(((Number) sampleValue).longValue(), encoder);
            case "deviceName" -> WamGlobalEncoder.writeDeviceName((String) sampleValue, encoder);
            case "osVersion" -> WamGlobalEncoder.writeOsVersion((String) sampleValue, encoder);
            case "appVersion" -> WamGlobalEncoder.writeAppVersion((String) sampleValue, encoder);
            case "appIsBetaRelease" -> WamGlobalEncoder.writeAppIsBetaRelease((Boolean) sampleValue, encoder);
            case "networkIsWifi" -> WamGlobalEncoder.writeNetworkIsWifi((Boolean) sampleValue, encoder);
            case "commitTime" -> WamGlobalEncoder.writeCommitTime(((Number) sampleValue).longValue(), encoder);
            case "browserVersion" -> WamGlobalEncoder.writeBrowserVersion((String) sampleValue, encoder);
            case "webcEnv" -> WamGlobalEncoder.writeWebcEnv(((Number) sampleValue).longValue(), encoder);
            case "memClass" -> WamGlobalEncoder.writeMemClass(((Number) sampleValue).longValue(), encoder);
            case "yearClass" -> WamGlobalEncoder.writeYearClass(((Number) sampleValue).longValue(), encoder);
            case "webcPhonePlatform" -> WamGlobalEncoder.writeWebcPhonePlatform(((Number) sampleValue).longValue(), encoder);
            case "browser" -> WamGlobalEncoder.writeBrowser((String) sampleValue, encoder);
            case "webcPhoneCharging" -> WamGlobalEncoder.writeWebcPhoneCharging((Boolean) sampleValue, encoder);
            case "webcPhoneDeviceManufacturer" -> WamGlobalEncoder.writeWebcPhoneDeviceManufacturer((String) sampleValue, encoder);
            case "webcPhoneDeviceModel" -> WamGlobalEncoder.writeWebcPhoneDeviceModel((String) sampleValue, encoder);
            case "webcPhoneOsBuildNumber" -> WamGlobalEncoder.writeWebcPhoneOsBuildNumber((String) sampleValue, encoder);
            case "webcPhoneOsVersion" -> WamGlobalEncoder.writeWebcPhoneOsVersion((String) sampleValue, encoder);
            case "webcBucket" -> WamGlobalEncoder.writeWebcBucket((String) sampleValue, encoder);
            case "webcWebPlatform" -> WamGlobalEncoder.writeWebcWebPlatform(((Number) sampleValue).longValue(), encoder);
            case "webcPhoneAppVersion" -> WamGlobalEncoder.writeWebcPhoneAppVersion((String) sampleValue, encoder);
            case "webcNativeBetaUpdates" -> WamGlobalEncoder.writeWebcNativeBetaUpdates((Boolean) sampleValue, encoder);
            case "webcNativeAutolaunch" -> WamGlobalEncoder.writeWebcNativeAutolaunch((Boolean) sampleValue, encoder);
            case "appBuild" -> WamGlobalEncoder.writeAppBuild(((Number) sampleValue).longValue(), encoder);
            case "yearClass2016" -> WamGlobalEncoder.writeYearClass2016(((Number) sampleValue).longValue(), encoder);
            case "datacenter" -> WamGlobalEncoder.writeDatacenter((String) sampleValue, encoder);
            case "beaconSessionId" -> WamGlobalEncoder.writeBeaconSessionId(((Number) sampleValue).longValue(), encoder);
            case "streamId" -> WamGlobalEncoder.writeStreamId(((Number) sampleValue).longValue(), encoder);
            case "webcTabId" -> WamGlobalEncoder.writeWebcTabId((String) sampleValue, encoder);
            case "abKey2" -> WamGlobalEncoder.writeAbKey2((String) sampleValue, encoder);
            case "deviceVersion" -> WamGlobalEncoder.writeDeviceVersion((String) sampleValue, encoder);
            case "expoKey" -> WamGlobalEncoder.writeExpoKey((String) sampleValue, encoder);
            case "psId" -> WamGlobalEncoder.writePsId((String) sampleValue, encoder);
            case "ocVersion" -> WamGlobalEncoder.writeOcVersion(((Number) sampleValue).longValue(), encoder);
            case "webcWebDeviceManufacturer" -> WamGlobalEncoder.writeWebcWebDeviceManufacturer((String) sampleValue, encoder);
            case "webcWebDeviceModel" -> WamGlobalEncoder.writeWebcWebDeviceModel((String) sampleValue, encoder);
            case "webcWebOsReleaseNumber" -> WamGlobalEncoder.writeWebcWebOsReleaseNumber((String) sampleValue, encoder);
            case "webcWebArch" -> WamGlobalEncoder.writeWebcWebArch((String) sampleValue, encoder);
            case "psCountryCode" -> WamGlobalEncoder.writePsCountryCode((String) sampleValue, encoder);
            case "numCpu" -> WamGlobalEncoder.writeNumCpu(((Number) sampleValue).longValue(), encoder);
            case "serviceImprovementOptOut" -> WamGlobalEncoder.writeServiceImprovementOptOut((Boolean) sampleValue, encoder);
            case "deviceClassification" -> WamGlobalEncoder.writeDeviceClassification(((Number) sampleValue).longValue(), encoder);
            case "wametaLoggerTestFilter" -> WamGlobalEncoder.writeWametaLoggerTestFilter((String) sampleValue, encoder);
            case "webcRevision" -> WamGlobalEncoder.writeWebcRevision(((Number) sampleValue).longValue(), encoder);
            case "isInCohort" -> WamGlobalEncoder.writeIsInCohort((Boolean) sampleValue, encoder);
            default -> invokeBoundaryWriter(encoder, fieldId, type, sampleValue);
        }
    }

    private static void invokeBoundaryWriter(WamEventEncoder encoder, int fieldId, String type, Object sampleValue) {
        if ("null".equals(type) || sampleValue == null) {
            WamGlobalEncoder.writeNullGlobal(fieldId, encoder);
            return;
        }
        Object boxed = switch (type) {
            case "int" -> ((Number) sampleValue).longValue();
            case "bool" -> (Boolean) sampleValue;
            case "str" -> (String) sampleValue;
            case "float" -> ((Number) sampleValue).doubleValue();
            default -> throw new IllegalStateException("unsupported boundary type: " + type);
        };
        WamGlobalEncoder.writeDynamicGlobal(fieldId, boxed, encoder);
    }
}
