package com.github.auties00.cobalt.wire.push.fcm.mcs;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * Models an incoming FCM data-message stanza on the MCS stream.
 *
 * <p>A data-message stanza is the payload-bearing frame the server pushes to
 * the client; {@code FcmClient} scans the {@link #appData()} entries (and, on
 * the binary verification flow, {@link #rawData()}) for the WhatsApp
 * verification code and surfaces it via {@code FcmClient#getPushCode()}. The
 * {@link #persistentId()} is tracked so the message can be acknowledged on the
 * next MCS login and the server can stop redelivering it.
 *
 * @implNote This implementation carries the MCS frame tag {@code 8}; the tag is
 * written as the frame's length-prefixed type byte by the connection layer, not
 * by this message.
 */
@ProtobufMessage(name = "FcmMcsDataMessageStanza")
public final class FcmMcsDataMessageStanza {
    /**
     * Holds the application-level message id.
     *
     * <p>This is typically the same value the original sender supplied to the
     * FCM HTTP API and is otherwise uninterpreted by the client.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String id;

    /**
     * Holds the sender id, typically the project's GCM sender number.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String from;

    /**
     * Holds the push category.
     *
     * <p>This is typically the receiving app package name, or the topic name on
     * topic-style pushes.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String category;

    /**
     * Holds the FCM collapse key.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String token;

    /**
     * Holds the application-level key/value payload.
     *
     * <p>The FCM HTTP API's {@code data} JSON object lands here; the WhatsApp
     * registration server places the verification code under the
     * {@code "registration_code"} key.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    List<AppData> appData;

    /**
     * Holds the per-message persistent id used by the at-least-once delivery
     * mechanism.
     *
     * <p>{@code FcmClient} retains this id so it can be replayed on the next MCS
     * login, letting the server drop the message from its retry queue.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String persistentId;

    /**
     * Holds the time-to-live the sender attached, in seconds.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.INT64)
    long ttl;

    /**
     * Holds the server-side send timestamp, in milliseconds since the epoch.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.INT64)
    long sent;

    /**
     * Holds the optional binary payload.
     *
     * <p>This is used by FCM-driven SMS and silent verification flows, where the
     * verification code is shipped as raw bytes rather than as an
     * {@link AppData} entry.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
    byte[] rawData;

    /**
     * Constructs a new stanza with the given values.
     *
     * <p>This constructor is invoked by the protobuf decoder when reading a
     * frame off the MCS stream; the decoded stanza is consumed by
     * {@code FcmClient}.
     *
     * @param id           the application-level message id
     * @param from         the sender id
     * @param category     the push category
     * @param token        the FCM collapse key
     * @param appData      the application-level key/value payload
     * @param persistentId the per-message persistent id
     * @param ttl          the time-to-live in seconds
     * @param sent         the server-side send timestamp in milliseconds
     * @param rawData      the optional binary payload
     */
    FcmMcsDataMessageStanza(String id, String from, String category, String token,
                            List<AppData> appData, String persistentId, long ttl,
                            long sent, byte[] rawData) {
        this.id = id;
        this.from = from;
        this.category = category;
        this.token = token;
        this.appData = appData;
        this.persistentId = persistentId;
        this.ttl = ttl;
        this.sent = sent;
        this.rawData = rawData;
    }

    /**
     * Returns the application-level message id.
     *
     * @return the message id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the sender id.
     *
     * @return the sender id
     */
    public String from() {
        return from;
    }

    /**
     * Returns the push category.
     *
     * @return the category
     */
    public String category() {
        return category;
    }

    /**
     * Returns the FCM collapse key.
     *
     * @return the collapse key
     */
    public String token() {
        return token;
    }

    /**
     * Returns the application-level key/value payload.
     *
     * @return the {@link AppData} entries, possibly {@code null} when the stanza
     *         carried no payload
     */
    public List<AppData> appData() {
        return appData;
    }

    /**
     * Returns the per-message persistent id.
     *
     * @return the persistent id
     */
    public String persistentId() {
        return persistentId;
    }

    /**
     * Returns the time-to-live in seconds.
     *
     * @return the time-to-live
     */
    public long ttl() {
        return ttl;
    }

    /**
     * Returns the server-side send timestamp in milliseconds since the epoch.
     *
     * @return the send timestamp
     */
    public long sent() {
        return sent;
    }

    /**
     * Returns the optional binary payload.
     *
     * @return the raw payload, or {@code null} when absent
     */
    public byte[] rawData() {
        return rawData;
    }

    /**
     * Models one key/value entry in the {@link FcmMcsDataMessageStanza#appData()}
     * list.
     *
     * <p>The verification code lives in the entry whose {@link #key()} is
     * {@code "registration_code"}; every other entry is ignored by Cobalt.
     */
    @ProtobufMessage(name = "FcmMcsDataMessageStanza.AppData")
    public static final class AppData {
        /**
         * Holds the entry key.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String key;

        /**
         * Holds the entry value.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String value;

        /**
         * Constructs a new entry with the given key and value.
         *
         * @param key   the entry key
         * @param value the entry value
         */
        AppData(String key, String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Returns the entry key.
         *
         * @return the key
         */
        public String key() {
            return key;
        }

        /**
         * Returns the entry value.
         *
         * @return the value
         */
        public String value() {
            return value;
        }
    }
}
