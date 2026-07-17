package com.github.auties00.cobalt.wam.binary;

import com.github.auties00.cobalt.wam.model.WamChannel;

import static com.github.auties00.cobalt.wam.binary.WamTags.GLOBAL;

/**
 * A stateless encoding facade for the known global attribute entries
 * in the WAM binary protocol.
 *
 * <p>Each WhatsApp-defined global field has a dedicated pair of methods:
 * a {@code xxxSize} method that returns the exact byte count, and a
 * {@code writeXxx} method that pushes the encoded bytes into a
 * {@link WamEventEncoder}. Field identifiers are hardcoded inside each
 * method so callers never deal with raw numeric IDs.
 *
 * <p>This class is thread-safe as all methods are static and operate
 * on provided parameters without shared mutable state.
 *
 * @see WamEventEncoder
 * @see WamEventSizes
 * @see WamTags#GLOBAL
 */
public final class WamGlobalEncoder {
    private static final int MNC = 3;
    private static final int MCC = 5;
    private static final int PLATFORM = 11;
    private static final int DEVICE_NAME = 13;
    private static final int OS_VERSION = 15;
    private static final int APP_VERSION = 17;
    private static final int APP_IS_BETA_RELEASE = 21;
    private static final int NETWORK_IS_WIFI = 23;
    private static final int COMMIT_TIME = 47;
    private static final int BROWSER_VERSION = 295;
    private static final int WEBC_ENV = 633;
    private static final int MEM_CLASS = 655;
    private static final int YEAR_CLASS = 689;
    private static final int WEBC_PHONE_PLATFORM = 707;
    private static final int BROWSER = 779;
    private static final int WEBC_PHONE_CHARGING = 783;
    private static final int WEBC_PHONE_DEVICE_MANUFACTURER = 829;
    private static final int WEBC_PHONE_DEVICE_MODEL = 831;
    private static final int WEBC_PHONE_OS_BUILD_NUMBER = 833;
    private static final int WEBC_PHONE_OS_VERSION = 835;
    private static final int WEBC_BUCKET = 875;
    private static final int WEBC_WEB_PLATFORM = 899;
    private static final int WEBC_PHONE_APP_VERSION = 1005;
    private static final int WEBC_NATIVE_BETA_UPDATES = 1007;
    private static final int WEBC_NATIVE_AUTOLAUNCH = 1009;
    private static final int APP_BUILD = 1657;
    private static final int YEAR_CLASS_2016 = 2617;
    private static final int DATACENTER = 2795;
    private static final int BEACON_SESSION_ID = 3433;
    private static final int STREAM_ID = 3543;
    private static final int WEBC_TAB_ID = 3727;
    private static final int AB_KEY_2 = 4473;
    private static final int DEVICE_VERSION = 4505;
    private static final int EXPO_KEY = 5029;
    private static final int PS_ID = 6005;
    private static final int OC_VERSION = 6251;
    private static final int WEBC_WEB_DEVICE_MANUFACTURER = 6599;
    private static final int WEBC_WEB_DEVICE_MODEL = 6601;
    private static final int WEBC_WEB_OS_RELEASE_NUMBER = 6603;
    private static final int WEBC_WEB_ARCH = 6605;
    private static final int PS_COUNTRY_CODE = 6833;
    private static final int NUM_CPU = 10317;
    private static final int SERVICE_IMPROVEMENT_OPT_OUT = 13293;
    private static final int DEVICE_CLASSIFICATION = 14507;
    private static final int WAMETA_LOGGER_TEST_FILTER = 15881;
    private static final int WEBC_REVISION = 18491;
    private static final int IS_IN_COHORT = 19129;

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private WamGlobalEncoder() {
        throw new AssertionError();
    }

    // mnc (3, int)
    /**
     * Returns the number of bytes required to encode the mobile network
     * code global.
     *
     * @param value the mobile network code
     * @return the encoded size in bytes
     */
    public static int mncSize(long value) {
        return WamEventSizes.intSize(MNC, value);
    }

    /**
     * Writes the mobile network code global attribute.
     *
     * @param value   the mobile network code
     * @param encoder the destination encoder
     */
    public static void writeMnc(long value, WamEventEncoder encoder) {
        encoder.writeInt(MNC, GLOBAL, value);
    }

    // mcc (5, int)
    /**
     * Returns the number of bytes required to encode the mobile country
     * code global.
     *
     * @param value the mobile country code
     * @return the encoded size in bytes
     */
    public static int mccSize(long value) {
        return WamEventSizes.intSize(MCC, value);
    }

    /**
     * Writes the mobile country code global attribute.
     *
     * @param value   the mobile country code
     * @param encoder the destination encoder
     */
    public static void writeMcc(long value, WamEventEncoder encoder) {
        encoder.writeInt(MCC, GLOBAL, value);
    }

    // generic null global
    /**
     * Returns the number of bytes required to encode a null global
     * entry for the given field identifier.
     *
     * @param fieldId the numeric field identifier
     * @return the encoded size in bytes
     */
    public static int nullGlobalSize(int fieldId) {
        return WamEventSizes.nullSize(fieldId);
    }

    /**
     * Writes a null global entry.
     *
     * @param fieldId the numeric field identifier
     * @param encoder the destination encoder
     */
    public static void writeNullGlobal(int fieldId, WamEventEncoder encoder) {
        encoder.writeNull(fieldId, GLOBAL);
    }

    // platform (11, int)
    /**
     * Returns the number of bytes required to encode the platform global.
     *
     * @param value the platform identifier
     * @return the encoded size in bytes
     */
    public static int platformSize(long value) {
        return WamEventSizes.intSize(PLATFORM, value);
    }

    /**
     * Writes the platform global attribute.
     *
     * @param value   the platform identifier
     * @param encoder the destination encoder
     */
    public static void writePlatform(long value, WamEventEncoder encoder) {
        encoder.writeInt(PLATFORM, GLOBAL, value);
    }

    // deviceName (13, string)
    /**
     * Returns the number of bytes required to encode the device name
     * global.
     *
     * @param value the device name, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int deviceNameSize(String value) {
        return WamEventSizes.stringSize(DEVICE_NAME, value);
    }

    /**
     * Writes the device name global attribute.
     *
     * @param value   the device name, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeDeviceName(String value, WamEventEncoder encoder) {
        encoder.writeString(DEVICE_NAME, GLOBAL, value);
    }

    // osVersion (15, string)
    /**
     * Returns the number of bytes required to encode the OS version
     * global.
     *
     * @param value the OS version string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int osVersionSize(String value) {
        return WamEventSizes.stringSize(OS_VERSION, value);
    }

    /**
     * Writes the OS version global attribute.
     *
     * @param value   the OS version string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeOsVersion(String value, WamEventEncoder encoder) {
        encoder.writeString(OS_VERSION, GLOBAL, value);
    }

    // appVersion (17, string)
    /**
     * Returns the number of bytes required to encode the app version
     * global.
     *
     * @param value the version string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int appVersionSize(String value) {
        return WamEventSizes.stringSize(APP_VERSION, value);
    }

    /**
     * Writes the app version global attribute.
     *
     * @param value   the version string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeAppVersion(String value, WamEventEncoder encoder) {
        encoder.writeString(APP_VERSION, GLOBAL, value);
    }

    // appIsBetaRelease (21, bool as int)
    /**
     * Returns the number of bytes required to encode the app-is-beta
     * global.
     *
     * @param value {@code true} if this is a beta release
     * @return the encoded size in bytes
     */
    public static int appIsBetaReleaseSize(boolean value) {
        return WamEventSizes.intSize(APP_IS_BETA_RELEASE, value ? 1 : 0);
    }

    /**
     * Writes the app-is-beta global attribute.
     *
     * @param value   {@code true} if this is a beta release
     * @param encoder the destination encoder
     */
    public static void writeAppIsBetaRelease(boolean value, WamEventEncoder encoder) {
        encoder.writeInt(APP_IS_BETA_RELEASE, GLOBAL, value ? 1 : 0);
    }

    // networkIsWifi (23, bool as int)
    /**
     * Returns the number of bytes required to encode the network-is-wifi
     * global.
     *
     * @param value {@code true} if the device is connected via Wi-Fi
     * @return the encoded size in bytes
     */
    public static int networkIsWifiSize(boolean value) {
        return WamEventSizes.intSize(NETWORK_IS_WIFI, value ? 1 : 0);
    }

    /**
     * Writes the network-is-wifi global attribute.
     *
     * @param value   {@code true} if the device is connected via Wi-Fi
     * @param encoder the destination encoder
     */
    public static void writeNetworkIsWifi(boolean value, WamEventEncoder encoder) {
        encoder.writeInt(NETWORK_IS_WIFI, GLOBAL, value ? 1 : 0);
    }

    // commitTime (47, int)
    /**
     * Returns the number of bytes required to encode the commit-time
     * global written before each event.
     *
     * @param epochSeconds the Unix epoch seconds when the event was
     *                     committed
     * @return the encoded size in bytes
     */
    public static int commitTimeSize(long epochSeconds) {
        return WamEventSizes.intSize(COMMIT_TIME, epochSeconds);
    }

    /**
     * Writes the commit-time global attribute.
     *
     * @param epochSeconds the Unix epoch seconds
     * @param encoder      the destination encoder
     */
    public static void writeCommitTime(long epochSeconds, WamEventEncoder encoder) {
        encoder.writeInt(COMMIT_TIME, GLOBAL, epochSeconds);
    }

    // browserVersion (295, string)
    /**
     * Returns the number of bytes required to encode the browser version
     * global.
     *
     * @param value the browser version string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int browserVersionSize(String value) {
        return WamEventSizes.stringSize(BROWSER_VERSION, value);
    }

    /**
     * Writes the browser version global attribute.
     *
     * @param value   the browser version string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeBrowserVersion(String value, WamEventEncoder encoder) {
        encoder.writeString(BROWSER_VERSION, GLOBAL, value);
    }

    // webcEnv (633, int enum)
    /**
     * Returns the number of bytes required to encode the web client
     * environment global.
     *
     * @param value the environment code enum value
     * @return the encoded size in bytes
     */
    public static int webcEnvSize(long value) {
        return WamEventSizes.intSize(WEBC_ENV, value);
    }

    /**
     * Writes the web client environment global attribute.
     *
     * @param value   the environment code enum value
     * @param encoder the destination encoder
     */
    public static void writeWebcEnv(long value, WamEventEncoder encoder) {
        encoder.writeInt(WEBC_ENV, GLOBAL, value);
    }

    // memClass (655, int)
    /**
     * Returns the number of bytes required to encode the memory class
     * global.
     *
     * @param value the device memory class in megabytes
     * @return the encoded size in bytes
     */
    public static int memClassSize(long value) {
        return WamEventSizes.intSize(MEM_CLASS, value);
    }

    /**
     * Writes the memory class global attribute.
     *
     * @param value   the device memory class in megabytes
     * @param encoder the destination encoder
     */
    public static void writeMemClass(long value, WamEventEncoder encoder) {
        encoder.writeInt(MEM_CLASS, GLOBAL, value);
    }

    // yearClass (689, int)
    /**
     * Returns the number of bytes required to encode the year class
     * global.
     *
     * @param value the year class
     * @return the encoded size in bytes
     */
    public static int yearClassSize(long value) {
        return WamEventSizes.intSize(YEAR_CLASS, value);
    }

    /**
     * Writes the year class global attribute.
     *
     * @param value   the year class
     * @param encoder the destination encoder
     */
    public static void writeYearClass(long value, WamEventEncoder encoder) {
        encoder.writeInt(YEAR_CLASS, GLOBAL, value);
    }

    // webcPhonePlatform (707, int enum)
    /**
     * Returns the number of bytes required to encode the phone platform
     * global.
     *
     * @param value the platform type enum value
     * @return the encoded size in bytes
     */
    public static int webcPhonePlatformSize(long value) {
        return WamEventSizes.intSize(WEBC_PHONE_PLATFORM, value);
    }

    /**
     * Writes the phone platform global attribute.
     *
     * @param value   the platform type enum value
     * @param encoder the destination encoder
     */
    public static void writeWebcPhonePlatform(long value, WamEventEncoder encoder) {
        encoder.writeInt(WEBC_PHONE_PLATFORM, GLOBAL, value);
    }

    // browser (779, string)
    /**
     * Returns the number of bytes required to encode the browser global.
     *
     * @param value the browser name, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int browserSize(String value) {
        return WamEventSizes.stringSize(BROWSER, value);
    }

    /**
     * Writes the browser global attribute.
     *
     * @param value   the browser name, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeBrowser(String value, WamEventEncoder encoder) {
        encoder.writeString(BROWSER, GLOBAL, value);
    }

    // webcPhoneCharging (783, bool as int)
    /**
     * Returns the number of bytes required to encode the phone-charging
     * global.
     *
     * @param value {@code true} if the paired phone is charging
     * @return the encoded size in bytes
     */
    public static int webcPhoneChargingSize(boolean value) {
        return WamEventSizes.intSize(WEBC_PHONE_CHARGING, value ? 1 : 0);
    }

    /**
     * Writes the phone-charging global attribute.
     *
     * @param value   {@code true} if the paired phone is charging
     * @param encoder the destination encoder
     */
    public static void writeWebcPhoneCharging(boolean value, WamEventEncoder encoder) {
        encoder.writeInt(WEBC_PHONE_CHARGING, GLOBAL, value ? 1 : 0);
    }

    // webcPhoneDeviceManufacturer (829, string)
    /**
     * Returns the number of bytes required to encode the phone device
     * manufacturer global.
     *
     * @param value the manufacturer name, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int webcPhoneDeviceManufacturerSize(String value) {
        return WamEventSizes.stringSize(WEBC_PHONE_DEVICE_MANUFACTURER, value);
    }

    /**
     * Writes the phone device manufacturer global attribute.
     *
     * @param value   the manufacturer name, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWebcPhoneDeviceManufacturer(String value, WamEventEncoder encoder) {
        encoder.writeString(WEBC_PHONE_DEVICE_MANUFACTURER, GLOBAL, value);
    }

    // webcPhoneDeviceModel (831, string)
    /**
     * Returns the number of bytes required to encode the phone device
     * model global.
     *
     * @param value the model name, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int webcPhoneDeviceModelSize(String value) {
        return WamEventSizes.stringSize(WEBC_PHONE_DEVICE_MODEL, value);
    }

    /**
     * Writes the phone device model global attribute.
     *
     * @param value   the model name, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWebcPhoneDeviceModel(String value, WamEventEncoder encoder) {
        encoder.writeString(WEBC_PHONE_DEVICE_MODEL, GLOBAL, value);
    }

    // webcPhoneOsBuildNumber (833, string)
    /**
     * Returns the number of bytes required to encode the phone OS build
     * number global.
     *
     * @param value the build number string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int webcPhoneOsBuildNumberSize(String value) {
        return WamEventSizes.stringSize(WEBC_PHONE_OS_BUILD_NUMBER, value);
    }

    /**
     * Writes the phone OS build number global attribute.
     *
     * @param value   the build number string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWebcPhoneOsBuildNumber(String value, WamEventEncoder encoder) {
        encoder.writeString(WEBC_PHONE_OS_BUILD_NUMBER, GLOBAL, value);
    }

    // webcPhoneOsVersion (835, string)
    /**
     * Returns the number of bytes required to encode the phone OS
     * version global.
     *
     * @param value the version string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int webcPhoneOsVersionSize(String value) {
        return WamEventSizes.stringSize(WEBC_PHONE_OS_VERSION, value);
    }

    /**
     * Writes the phone OS version global attribute.
     *
     * @param value   the version string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWebcPhoneOsVersion(String value, WamEventEncoder encoder) {
        encoder.writeString(WEBC_PHONE_OS_VERSION, GLOBAL, value);
    }

    // webcBucket (875, string)
    /**
     * Returns the number of bytes required to encode the web bucket
     * global.
     *
     * @param value the experiment bucket string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int webcBucketSize(String value) {
        return WamEventSizes.stringSize(WEBC_BUCKET, value);
    }

    /**
     * Writes the web bucket global attribute.
     *
     * @param value   the experiment bucket string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWebcBucket(String value, WamEventEncoder encoder) {
        encoder.writeString(WEBC_BUCKET, GLOBAL, value);
    }

    // webcWebPlatform (899, int enum)
    /**
     * Returns the number of bytes required to encode the web platform
     * type global.
     *
     * @param value the web platform enum value
     * @return the encoded size in bytes
     */
    public static int webcWebPlatformSize(long value) {
        return WamEventSizes.intSize(WEBC_WEB_PLATFORM, value);
    }

    /**
     * Writes the web platform type global attribute.
     *
     * @param value   the web platform enum value
     * @param encoder the destination encoder
     */
    public static void writeWebcWebPlatform(long value, WamEventEncoder encoder) {
        encoder.writeInt(WEBC_WEB_PLATFORM, GLOBAL, value);
    }

    // webcPhoneAppVersion (1005, string)
    /**
     * Returns the number of bytes required to encode the phone app
     * version global.
     *
     * @param value the version string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int webcPhoneAppVersionSize(String value) {
        return WamEventSizes.stringSize(WEBC_PHONE_APP_VERSION, value);
    }

    /**
     * Writes the phone app version global attribute.
     *
     * @param value   the version string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWebcPhoneAppVersion(String value, WamEventEncoder encoder) {
        encoder.writeString(WEBC_PHONE_APP_VERSION, GLOBAL, value);
    }

    // webcNativeBetaUpdates (1007, bool as int)
    /**
     * Returns the number of bytes required to encode the native beta
     * updates global.
     *
     * @param value {@code true} if the native app is configured for
     *              beta updates
     * @return the encoded size in bytes
     */
    public static int webcNativeBetaUpdatesSize(boolean value) {
        return WamEventSizes.intSize(WEBC_NATIVE_BETA_UPDATES, value ? 1 : 0);
    }

    /**
     * Writes the native beta updates global attribute.
     *
     * @param value   {@code true} if configured for beta updates
     * @param encoder the destination encoder
     */
    public static void writeWebcNativeBetaUpdates(boolean value, WamEventEncoder encoder) {
        encoder.writeInt(WEBC_NATIVE_BETA_UPDATES, GLOBAL, value ? 1 : 0);
    }

    // webcNativeAutolaunch (1009, bool as int)
    /**
     * Returns the number of bytes required to encode the native
     * autolaunch global.
     *
     * @param value {@code true} if the native app is configured for
     *              autolaunch
     * @return the encoded size in bytes
     */
    public static int webcNativeAutolaunchSize(boolean value) {
        return WamEventSizes.intSize(WEBC_NATIVE_AUTOLAUNCH, value ? 1 : 0);
    }

    /**
     * Writes the native autolaunch global attribute.
     *
     * @param value   {@code true} if configured for autolaunch
     * @param encoder the destination encoder
     */
    public static void writeWebcNativeAutolaunch(boolean value, WamEventEncoder encoder) {
        encoder.writeInt(WEBC_NATIVE_AUTOLAUNCH, GLOBAL, value ? 1 : 0);
    }

    // appBuild (1657, int enum)
    /**
     * Returns the number of bytes required to encode the app build type
     * global.
     *
     * @param value the app build type enum value
     * @return the encoded size in bytes
     */
    public static int appBuildSize(long value) {
        return WamEventSizes.intSize(APP_BUILD, value);
    }

    /**
     * Writes the app build type global attribute.
     *
     * @param value   the app build type enum value
     * @param encoder the destination encoder
     */
    public static void writeAppBuild(long value, WamEventEncoder encoder) {
        encoder.writeInt(APP_BUILD, GLOBAL, value);
    }

    // yearClass2016 (2617, int)
    /**
     * Returns the number of bytes required to encode the year class 2016
     * global.
     *
     * @param value the year class value
     * @return the encoded size in bytes
     */
    public static int yearClass2016Size(long value) {
        return WamEventSizes.intSize(YEAR_CLASS_2016, value);
    }

    /**
     * Writes the year class 2016 global attribute.
     *
     * @param value   the year class value
     * @param encoder the destination encoder
     */
    public static void writeYearClass2016(long value, WamEventEncoder encoder) {
        encoder.writeInt(YEAR_CLASS_2016, GLOBAL, value);
    }

    // datacenter (2795, string)
    /**
     * Returns the number of bytes required to encode the datacenter
     * global.
     *
     * @param value the datacenter identifier, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int datacenterSize(String value) {
        return WamEventSizes.stringSize(DATACENTER, value);
    }

    /**
     * Writes the datacenter global attribute.
     *
     * @param value   the datacenter identifier, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeDatacenter(String value, WamEventEncoder encoder) {
        encoder.writeString(DATACENTER, GLOBAL, value);
    }

    // beaconSessionId (3433, int)
    /**
     * Returns the number of bytes required to encode the beacon session
     * ID global written per-event when beaconing is active.
     *
     * @param value the beaconing sequence number
     * @return the encoded size in bytes
     */
    public static int beaconSessionIdSize(long value) {
        return WamEventSizes.intSize(BEACON_SESSION_ID, value);
    }

    /**
     * Writes the beacon session ID global attribute.
     *
     * @param value   the beaconing sequence number
     * @param encoder the destination encoder
     */
    public static void writeBeaconSessionId(long value, WamEventEncoder encoder) {
        encoder.writeInt(BEACON_SESSION_ID, GLOBAL, value);
    }

    // streamId (3543, int)
    /**
     * Returns the number of bytes required to encode the stream ID
     * global.
     *
     * @param value the stream identifier
     * @return the encoded size in bytes
     */
    public static int streamIdSize(long value) {
        return WamEventSizes.intSize(STREAM_ID, value);
    }

    /**
     * Writes the stream ID global attribute.
     *
     * @param value   the stream identifier
     * @param encoder the destination encoder
     */
    public static void writeStreamId(long value, WamEventEncoder encoder) {
        encoder.writeInt(STREAM_ID, GLOBAL, value);
    }

    // webcTabId (3727, string)
    /**
     * Returns the number of bytes required to encode the tab ID global.
     *
     * @param value the tab identifier string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int webcTabIdSize(String value) {
        return WamEventSizes.stringSize(WEBC_TAB_ID, value);
    }

    /**
     * Writes the tab ID global attribute.
     *
     * @param value   the tab identifier string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWebcTabId(String value, WamEventEncoder encoder) {
        encoder.writeString(WEBC_TAB_ID, GLOBAL, value);
    }

    // abKey2 (4473, string)
    /**
     * Returns the number of bytes required to encode the AB key 2
     * global.
     *
     * @param value the AB key string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int abKey2Size(String value) {
        return WamEventSizes.stringSize(AB_KEY_2, value);
    }

    /**
     * Writes the AB key 2 global attribute.
     *
     * @param value   the AB key string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeAbKey2(String value, WamEventEncoder encoder) {
        encoder.writeString(AB_KEY_2, GLOBAL, value);
    }

    // deviceVersion (4505, string)
    /**
     * Returns the number of bytes required to encode the device version
     * global.
     *
     * @param value the device version string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int deviceVersionSize(String value) {
        return WamEventSizes.stringSize(DEVICE_VERSION, value);
    }

    /**
     * Writes the device version global attribute.
     *
     * @param value   the device version string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeDeviceVersion(String value, WamEventEncoder encoder) {
        encoder.writeString(DEVICE_VERSION, GLOBAL, value);
    }

    // expoKey (5029, string)
    /**
     * Returns the number of bytes required to encode the exposure key
     * global.
     *
     * @param value the exposure key string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int expoKeySize(String value) {
        return WamEventSizes.stringSize(EXPO_KEY, value);
    }

    /**
     * Writes the exposure key global attribute.
     *
     * @param value   the exposure key string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeExpoKey(String value, WamEventEncoder encoder) {
        encoder.writeString(EXPO_KEY, GLOBAL, value);
    }

    // psId (6005, string)
    /**
     * Returns the number of bytes required to encode the private stats
     * identifier global.
     *
     * <p>This global is only written for {@link WamChannel#PRIVATE
     * PRIVATE} channel buffers.
     *
     * @param value the PS identifier string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int psIdSize(String value) {
        return WamEventSizes.stringSize(PS_ID, value);
    }

    /**
     * Writes the private stats identifier global attribute.
     *
     * @param value   the PS identifier string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writePsId(String value, WamEventEncoder encoder) {
        encoder.writeString(PS_ID, GLOBAL, value);
    }

    // ocVersion (6251, int)
    /**
     * Returns the number of bytes required to encode the official client
     * version global.
     *
     * @param value the official client version number
     * @return the encoded size in bytes
     */
    public static int ocVersionSize(long value) {
        return WamEventSizes.intSize(OC_VERSION, value);
    }

    /**
     * Writes the official client version global attribute.
     *
     * @param value   the official client version number
     * @param encoder the destination encoder
     */
    public static void writeOcVersion(long value, WamEventEncoder encoder) {
        encoder.writeInt(OC_VERSION, GLOBAL, value);
    }

    // webcWebDeviceManufacturer (6599, string)
    /**
     * Returns the number of bytes required to encode the web device
     * manufacturer global.
     *
     * @param value the manufacturer name, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int webcWebDeviceManufacturerSize(String value) {
        return WamEventSizes.stringSize(WEBC_WEB_DEVICE_MANUFACTURER, value);
    }

    /**
     * Writes the web device manufacturer global attribute.
     *
     * @param value   the manufacturer name, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWebcWebDeviceManufacturer(String value, WamEventEncoder encoder) {
        encoder.writeString(WEBC_WEB_DEVICE_MANUFACTURER, GLOBAL, value);
    }

    // webcWebDeviceModel (6601, string)
    /**
     * Returns the number of bytes required to encode the web device
     * model global.
     *
     * @param value the model name, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int webcWebDeviceModelSize(String value) {
        return WamEventSizes.stringSize(WEBC_WEB_DEVICE_MODEL, value);
    }

    /**
     * Writes the web device model global attribute.
     *
     * @param value   the model name, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWebcWebDeviceModel(String value, WamEventEncoder encoder) {
        encoder.writeString(WEBC_WEB_DEVICE_MODEL, GLOBAL, value);
    }

    // webcWebOsReleaseNumber (6603, string)
    /**
     * Returns the number of bytes required to encode the web OS release
     * number global.
     *
     * @param value the release number string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int webcWebOsReleaseNumberSize(String value) {
        return WamEventSizes.stringSize(WEBC_WEB_OS_RELEASE_NUMBER, value);
    }

    /**
     * Writes the web OS release number global attribute.
     *
     * @param value   the release number string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWebcWebOsReleaseNumber(String value, WamEventEncoder encoder) {
        encoder.writeString(WEBC_WEB_OS_RELEASE_NUMBER, GLOBAL, value);
    }

    // webcWebArch (6605, string)
    /**
     * Returns the number of bytes required to encode the web
     * architecture global.
     *
     * @param value the architecture string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int webcWebArchSize(String value) {
        return WamEventSizes.stringSize(WEBC_WEB_ARCH, value);
    }

    /**
     * Writes the web architecture global attribute.
     *
     * @param value   the architecture string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWebcWebArch(String value, WamEventEncoder encoder) {
        encoder.writeString(WEBC_WEB_ARCH, GLOBAL, value);
    }

    // psCountryCode (6833, string)
    /**
     * Returns the number of bytes required to encode the private stats
     * country code global.
     *
     * @param value the country code string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int psCountryCodeSize(String value) {
        return WamEventSizes.stringSize(PS_COUNTRY_CODE, value);
    }

    /**
     * Writes the private stats country code global attribute.
     *
     * @param value   the country code string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writePsCountryCode(String value, WamEventEncoder encoder) {
        encoder.writeString(PS_COUNTRY_CODE, GLOBAL, value);
    }

    // numCpu (10317, int)
    /**
     * Returns the number of bytes required to encode the CPU count
     * global.
     *
     * @param value the number of available processors
     * @return the encoded size in bytes
     */
    public static int numCpuSize(long value) {
        return WamEventSizes.intSize(NUM_CPU, value);
    }

    /**
     * Writes the CPU count global attribute.
     *
     * @param value   the number of available processors
     * @param encoder the destination encoder
     */
    public static void writeNumCpu(long value, WamEventEncoder encoder) {
        encoder.writeInt(NUM_CPU, GLOBAL, value);
    }

    // serviceImprovementOptOut (13293, bool as int)
    /**
     * Returns the number of bytes required to encode the service
     * improvement opt-out global.
     *
     * @param value {@code true} if the user has opted out
     * @return the encoded size in bytes
     */
    public static int serviceImprovementOptOutSize(boolean value) {
        return WamEventSizes.intSize(SERVICE_IMPROVEMENT_OPT_OUT, value ? 1 : 0);
    }

    /**
     * Writes the service improvement opt-out global attribute.
     *
     * @param value   {@code true} if opted out
     * @param encoder the destination encoder
     */
    public static void writeServiceImprovementOptOut(boolean value, WamEventEncoder encoder) {
        encoder.writeInt(SERVICE_IMPROVEMENT_OPT_OUT, GLOBAL, value ? 1 : 0);
    }

    // deviceClassification (14507, int)
    /**
     * Returns the number of bytes required to encode the device
     * classification global.
     *
     * @param value the device classification enum value
     * @return the encoded size in bytes
     */
    public static int deviceClassificationSize(long value) {
        return WamEventSizes.intSize(DEVICE_CLASSIFICATION, value);
    }

    /**
     * Writes the device classification global attribute.
     *
     * @param value   the device classification enum value
     * @param encoder the destination encoder
     */
    public static void writeDeviceClassification(long value, WamEventEncoder encoder) {
        encoder.writeInt(DEVICE_CLASSIFICATION, GLOBAL, value);
    }

    // wametaLoggerTestFilter (15881, string)
    /**
     * Returns the number of bytes required to encode the WAMeta logger
     * test filter global.
     *
     * @param value the test filter string, must not be {@code null}
     * @return the encoded size in bytes
     */
    public static int wametaLoggerTestFilterSize(String value) {
        return WamEventSizes.stringSize(WAMETA_LOGGER_TEST_FILTER, value);
    }

    /**
     * Writes the WAMeta logger test filter global attribute.
     *
     * @param value   the test filter string, must not be {@code null}
     * @param encoder the destination encoder
     */
    public static void writeWametaLoggerTestFilter(String value, WamEventEncoder encoder) {
        encoder.writeString(WAMETA_LOGGER_TEST_FILTER, GLOBAL, value);
    }

    // webcRevision (18491, int)
    /**
     * Returns the number of bytes required to encode the web client
     * revision global.
     *
     * @param value the client revision number
     * @return the encoded size in bytes
     */
    public static int webcRevisionSize(long value) {
        return WamEventSizes.intSize(WEBC_REVISION, value);
    }

    /**
     * Writes the web client revision global attribute.
     *
     * @param value   the client revision number
     * @param encoder the destination encoder
     */
    public static void writeWebcRevision(long value, WamEventEncoder encoder) {
        encoder.writeInt(WEBC_REVISION, GLOBAL, value);
    }

    // isInCohort (19129, bool as int)
    /**
     * Returns the number of bytes required to encode the is-in-cohort
     * global.
     *
     * @param value {@code true} if the user is in a cohort
     * @return the encoded size in bytes
     */
    public static int isInCohortSize(boolean value) {
        return WamEventSizes.intSize(IS_IN_COHORT, value ? 1 : 0);
    }

    /**
     * Writes the is-in-cohort global attribute.
     *
     * @param value   {@code true} if in cohort
     * @param encoder the destination encoder
     */
    public static void writeIsInCohort(boolean value, WamEventEncoder encoder) {
        encoder.writeInt(IS_IN_COHORT, GLOBAL, value ? 1 : 0);
    }

    // dynamic dispatch for dirty-tracking
    /**
     * Returns the number of bytes required to encode a global attribute
     * whose type is determined at runtime from the value's class.
     *
     * <p>Supported value types are {@link Long}, {@link Integer},
     * {@link String}, and {@link Boolean}. {@link Double} values that
     * are {@link Double#isNaN(double) NaN} are skipped (size 0).
     *
     * @param fieldId the numeric field identifier
     * @param value   the global value (must not be {@code null})
     * @return the encoded size in bytes, or {@code 0} if the value
     *         should be skipped
     */
    public static int dynamicGlobalSize(int fieldId, Object value) {
        return switch (value) {
            case Double d -> Double.isNaN(d) ? 0 : WamEventSizes.floatSize(fieldId);
            case Number n -> WamEventSizes.intSize(fieldId, n.longValue());
            case String s -> WamEventSizes.stringSize(fieldId, s);
            case Boolean b -> WamEventSizes.intSize(fieldId, b ? 1 : 0);
            default -> throw new IllegalArgumentException(
                    "Unsupported global value type: " + value.getClass());
        };
    }

    /**
     * Writes a global attribute whose type is determined at runtime from
     * the value's class.
     *
     * <p>Supported value types are {@link Long}, {@link Integer},
     * {@link String}, and {@link Boolean}. {@link Double} values that
     * are {@link Double#isNaN(double) NaN} are silently skipped.
     *
     * @param fieldId the numeric field identifier
     * @param value   the global value (must not be {@code null})
     * @param encoder the destination encoder
     */
    public static void writeDynamicGlobal(int fieldId, Object value, WamEventEncoder encoder) {
        switch (value) {
            case Double d -> {
                if (!Double.isNaN(d)) {
                    encoder.writeFloat(fieldId, GLOBAL, d);
                }
            }
            case Number n -> encoder.writeInt(fieldId, GLOBAL, n.longValue());
            case String s -> encoder.writeString(fieldId, GLOBAL, s);
            case Boolean b -> encoder.writeInt(fieldId, GLOBAL, b ? 1 : 0);
            default -> throw new IllegalArgumentException(
                    "Unsupported global value type: " + value.getClass());
        }
    }
}
