package com.github.auties00.cobalt.wire.push.fcm.checkin;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Models the wire body for the gzipped POST against
 * {@code android.clients.google.com/checkin}, the first step of the Android device-registration handshake.
 *
 * <p>The encoded message carries the synthetic device profile and a single bootstrap event that Google's
 * checkin service requires before it hands back an Android id and security token. The paired response is
 * decoded by {@link FcmCheckinResponse}.
 *
 * @implNote This implementation models only the subset of fields Cobalt actually sends; the field indices
 * mirror Google's internal AndroidCheckin protobuf (the public reference is the leaked {@code checkin.proto}
 * schema), and unused fields are deliberately omitted from the schema so the encoder emits the smallest body
 * the server still accepts.
 */
@ProtobufMessage(name = "FcmCheckinRequest")
public final class FcmCheckinRequest {
    /**
     * Holds the existing Android id, or {@code 0} for a fresh registration.
     *
     * <p>A non-zero value re-confirms a previously assigned id; Cobalt sends {@code 0} on the first checkin.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT64)
    long id;

    /**
     * Holds the nested device-and-event description carried as field 4 of the request.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    Checkin checkin;

    /**
     * Holds the UI locale reported to the checkin service.
     *
     * @implNote This implementation sends {@code "en_US"} unconditionally rather than deriving the locale
     * from the host environment.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String locale;

    /**
     * Holds the random per-checkin logging id.
     *
     * @implNote This implementation generates the value as {@code SecureRandom.nextLong() & Long.MAX_VALUE}
     * so the server cannot fingerprint Cobalt across calls via a stable logging id; masking off the sign bit
     * keeps the value positive for the {@code INT64} wire type.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT64)
    long loggingId;

    /**
     * Holds the time zone reported to the checkin service.
     *
     * @implNote This implementation sends {@code "UTC"} unconditionally rather than deriving the zone from
     * the host environment.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    String timeZone;

    /**
     * Holds the checkin protocol version.
     *
     * @implNote This implementation sends {@code 3}, matching the value the native Android client emits.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.INT32)
    int version;

    /**
     * Holds a numeric flag the server expects in the wire body.
     *
     * <p>Carried so the encoded message matches what the checkin service accepts; non-zero values are not
     * modeled and Cobalt always sets it to {@code 0}.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.INT64)
    long fragment;

    /**
     * Holds the multi-user serial number flag the server expects in the wire body.
     *
     * <p>Shipped for the same reason as {@link #fragment}: to keep the encoded message intact. Cobalt always
     * sets it to {@code 0}.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.INT64)
    long userSerialNumber;

    /**
     * Constructs a new request with the given field values.
     *
     * @param id               the existing Android id, or {@code 0} for a fresh registration
     * @param checkin          the nested device-and-event description
     * @param locale           the UI locale
     * @param loggingId        the random per-checkin logging id
     * @param timeZone         the time zone
     * @param version          the checkin protocol version
     * @param fragment         the fragment flag
     * @param userSerialNumber the multi-user serial number flag
     */
    FcmCheckinRequest(long id, Checkin checkin, String locale, long loggingId,
                      String timeZone, int version, long fragment, long userSerialNumber) {
        this.id = id;
        this.checkin = checkin;
        this.locale = locale;
        this.loggingId = loggingId;
        this.timeZone = timeZone;
        this.version = version;
        this.fragment = fragment;
        this.userSerialNumber = userSerialNumber;
    }

    /**
     * Models the nested device-and-event payload carried as field 4 of the outer {@link FcmCheckinRequest}.
     *
     * <p>The block pairs a {@link Build} device description with a single synthetic {@link Event} and the
     * last-checkin bookkeeping the service uses to decide whether this is a fresh registration.
     */
    @ProtobufMessage(name = "FcmCheckinRequest.Checkin")
    public static final class Checkin {
        /**
         * Holds the build description of the device being impersonated.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        Build build;

        /**
         * Holds the last-checkin timestamp, in milliseconds since the epoch.
         *
         * <p>Cobalt sends {@code 0} for a fresh registration, signalling that the device has never checked
         * in before.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        long lastCheckinMs;

        /**
         * Holds one synthetic event entry.
         *
         * <p>The checkin service expects at least one event to be present; Cobalt always supplies a single
         * {@code event_log_start} entry.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        Event event;

        /**
         * Holds the multi-user serial number, {@code 0} on stock Android.
         */
        @ProtobufProperty(index = 9, type = ProtobufType.INT64)
        long userNumber;

        /**
         * Constructs a new checkin block.
         *
         * @param build         the device build description
         * @param lastCheckinMs the last-checkin timestamp, in milliseconds since the epoch
         * @param event         the synthetic event entry
         * @param userNumber    the multi-user serial number
         */
        Checkin(Build build, long lastCheckinMs, Event event, long userNumber) {
            this.build = build;
            this.lastCheckinMs = lastCheckinMs;
            this.event = event;
            this.userNumber = userNumber;
        }
    }

    /**
     * Models the device build description carried as field 1 of {@link Checkin}.
     *
     * <p>The fields together describe the device the registration impersonates: its fingerprint, hardware
     * codes, marketing names, and OS level.
     *
     * @implNote This implementation impersonates a Nexus 7 ({@code "google/razor/flo:5.0.1/..."}) running
     * SDK 30; the synthetic profile is held stable across embedders so the server fingerprint stays
     * consistent.
     */
    @ProtobufMessage(name = "FcmCheckinRequest.Build")
    public static final class Build {
        /**
         * Holds the build fingerprint, for example
         * {@code "google/razor/flo:5.0.1/LRX22C/1602158:user/release-keys"}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String fingerprint;

        /**
         * Holds the hardware id, for example {@code "flo"}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String hardware;

        /**
         * Holds the brand, for example {@code "google"}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String brand;

        /**
         * Holds the client identifier.
         *
         * @implNote This implementation always reports {@code "android-google"} as part of its synthetic
         * device profile.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        String clientId;

        /**
         * Holds the build timestamp, in epoch seconds.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.INT64)
        long timeMs;

        /**
         * Holds the SDK version (Android API level).
         *
         * @implNote This implementation sends {@code 30} to match the rest of the synthetic device profile.
         */
        @ProtobufProperty(index = 10, type = ProtobufType.INT32)
        int sdkVersion;

        /**
         * Holds the marketing model, for example {@code "Nexus 7"}.
         */
        @ProtobufProperty(index = 11, type = ProtobufType.STRING)
        String model;

        /**
         * Holds the OEM, for example {@code "asus"}.
         */
        @ProtobufProperty(index = 12, type = ProtobufType.STRING)
        String manufacturer;

        /**
         * Holds the internal product code, for example {@code "razor"}.
         */
        @ProtobufProperty(index = 13, type = ProtobufType.STRING)
        String product;

        /**
         * Holds whether an over-the-air update is installed.
         *
         * <p>Cobalt reports {@code false} for its fresh synthetic device.
         */
        @ProtobufProperty(index = 14, type = ProtobufType.BOOL)
        boolean otaInstalled;

        /**
         * Constructs a new build block.
         *
         * @param fingerprint  the build fingerprint
         * @param hardware     the hardware id
         * @param brand        the brand
         * @param clientId     the client identifier
         * @param timeMs       the build timestamp, in epoch seconds
         * @param sdkVersion   the SDK version
         * @param model        the marketing model
         * @param manufacturer the OEM
         * @param product      the internal product code
         * @param otaInstalled whether an over-the-air update is installed
         */
        Build(String fingerprint, String hardware, String brand, String clientId,
              long timeMs, int sdkVersion, String model, String manufacturer,
              String product, boolean otaInstalled) {
            this.fingerprint = fingerprint;
            this.hardware = hardware;
            this.brand = brand;
            this.clientId = clientId;
            this.timeMs = timeMs;
            this.sdkVersion = sdkVersion;
            this.model = model;
            this.manufacturer = manufacturer;
            this.product = product;
            this.otaInstalled = otaInstalled;
        }
    }

    /**
     * Models a single event entry carried as field 3 of {@link Checkin}.
     *
     * <p>The entry pairs a textual tag with the time the event occurred and satisfies the checkin service's
     * requirement that every checkin carry at least one event.
     */
    @ProtobufMessage(name = "FcmCheckinRequest.Event")
    public static final class Event {
        /**
         * Holds the event tag.
         *
         * @implNote This implementation always sends {@code "event_log_start"} for a synthetic checkin.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String tag;

        /**
         * Holds the event timestamp, in epoch milliseconds.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.INT64)
        long timeMs;

        /**
         * Constructs a new event entry.
         *
         * @param tag    the event tag
         * @param timeMs the event timestamp, in epoch milliseconds
         */
        Event(String tag, long timeMs) {
            this.tag = tag;
            this.timeMs = timeMs;
        }
    }
}
