package com.github.auties00.cobalt.wire.linked.device.pairing;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.*;

/**
 * Top level payload that a WhatsApp client sends to the server during the final step of
 * the Noise handshake to identify itself and negotiate session wide parameters.
 *
 * <p>The client payload is delivered inside {@link HandshakeMessage.ClientFinish} once
 * the Noise XX exchange has produced its final symmetric keys. It is by far the richest
 * message exchanged during connection set up: it names the account the client is
 * connecting as (or the device pairing material being registered), advertises the client
 * user agent and platform, declares connection metadata that is mostly useful for
 * telemetry (such as {@link #connectType} and {@link #connectReason}) and opts the client
 * into or out of a number of feature gates (such as {@link #trafficAnonymization},
 * {@link #accountType} or the various LID related flags).
 *
 * <p>The same payload shape is used for both the login flow, where an existing session is
 * resumed, and the registration flow, where a brand new device is being linked. Registration
 * populates {@link #devicePairingData} with the companion's Signal prekeys while login
 * leaves that field empty and sets {@link #username} instead. WhatsApp Web builds the
 * payload in {@code WAWebClientPayload}; Cobalt mirrors the same schema so that it can
 * masquerade as WhatsApp Web, Windows Desktop or macOS Desktop at the protocol level.
 *
 * <p>Almost every field is optional on the wire, and the majority are tri state booleans
 * or numeric counters. The accessor methods follow Cobalt's usual convention of returning
 * {@link Optional} for objects, {@link OptionalInt} or {@link OptionalLong} for boxed
 * numbers, and collapsing nullable booleans into plain {@code boolean}.
 */
@ProtobufMessage(name = "ClientPayload")
public final class ClientPayload {
    /**
     * Phone number of the account that is connecting, expressed as a numeric long (the
     * digits of the E.164 number without the leading plus). Populated for login flows
     * and left unset during the very first pairing attempt. Serialised as wire index
     * {@code 1}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
    Long username;

    /**
     * Whether the connection should start in passive mode, i.e. without subscribing the
     * client to new push notifications. Used by short lived background connections.
     * Serialised as wire index {@code 3}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    Boolean passive;

    /**
     * Description of the platform and build the client is running on, see
     * {@link UserAgent}. Serialised as wire index {@code 5}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    UserAgent userAgent;

    /**
     * Extra metadata specific to web and desktop clients, such as the browser name and
     * the enabled Web sub platform. See {@link WebInfo}. Serialised as wire index
     * {@code 6}.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    WebInfo webInfo;

    /**
     * Display name that the primary device last set for itself, echoed back to the
     * server so it can keep push notifications in sync. Serialised as wire index
     * {@code 7}.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String pushName;

    /**
     * Identifier of the application session that opened this connection, used by the
     * server for telemetry correlation. Serialised as wire index {@code 9}.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.SFIXED32)
    Integer sessionId;

    /**
     * Whether the client wants to keep the connection open only for the duration of a
     * single request (short connect). Serialised as wire index {@code 10}.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
    Boolean shortConnect;

    /**
     * Type of network the client believes it is running on, see {@link ConnectType}.
     * Used by the server purely for telemetry. Serialised as wire index {@code 12}.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.ENUM)
    ConnectType connectType;

    /**
     * Reason the client is opening the connection, see {@link ConnectReason}.
     * Serialised as wire index {@code 13}.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.ENUM)
    ConnectReason connectReason;

    /**
     * Shard identifiers the client wishes to subscribe to, used by the server to route
     * large scale broadcast traffic. Serialised as wire index {@code 14}.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.INT32)
    List<Integer> shards;

    /**
     * Description of how the client resolved the server hostname, see {@link DNSSource}.
     * Serialised as wire index {@code 15}.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    DNSSource dnsSource;

    /**
     * Number of connection attempts the client has made in the current session,
     * primarily for debugging retries. Serialised as wire index {@code 16}.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.UINT32)
    Integer connectAttemptCount;

    /**
     * Device index of the client within the account's device list. The primary device
     * uses {@code 0}, companions use strictly positive values. Serialised as wire index
     * {@code 18}.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.UINT32)
    Integer device;

    /**
     * Registration material transmitted when a new device is being paired for the first
     * time, see {@link DevicePairingRegistrationData}. Serialised as wire index
     * {@code 19}.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.MESSAGE)
    DevicePairingRegistrationData devicePairingData;

    /**
     * Product family that this client belongs to, see {@link Product}. The default of
     * {@link Product#WHATSAPP} covers the usual consumer case. Serialised as wire index
     * {@code 20}.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.ENUM)
    Product product;

    /**
     * Facebook connectivity authentication token, used when the session is being
     * authenticated via a Meta account. Serialised as wire index {@code 21}.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
    byte[] fbCat;

    /**
     * Facebook family user agent blob, supplied by Meta's cross app login flow.
     * Serialised as wire index {@code 22}.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.BYTES)
    byte[] fbUserAgent;

    /**
     * Legacy OC (origin client) flag, populated by Facebook family apps to indicate the
     * connection originates from one of them. Serialised as wire index {@code 23}.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.BOOL)
    Boolean oc;

    /**
     * Login counter, incremented by the client on each successful login so the server
     * can detect duplicate or replayed connections. Serialised as wire index {@code 24}.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.INT32)
    Integer lc;

    /**
     * Which iOS app extension, if any, initiated this connection, see
     * {@link IOSAppExtension}. Only relevant to iPhone builds. Serialised as wire
     * index {@code 30}.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.ENUM)
    IOSAppExtension iosAppExtension;

    /**
     * Facebook application identifier, used by non WhatsApp Meta clients that share this
     * protocol. Serialised as wire index {@code 31}.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.UINT64)
    Long fbAppId;

    /**
     * Stable device identifier generated by Meta's family login flow. Serialised as
     * wire index {@code 32}.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.BYTES)
    byte[] fbDeviceId;

    /**
     * Whether the client is reconnecting to pull missed data, mainly used for telemetry.
     * Serialised as wire index {@code 33}.
     */
    @ProtobufProperty(index = 33, type = ProtobufType.BOOL)
    Boolean pull;

    /**
     * Optional padding bytes that let the client hide the exact size of the payload, in
     * practice used as a mild anti fingerprinting measure. Serialised as wire index
     * {@code 34}.
     */
    @ProtobufProperty(index = 34, type = ProtobufType.BYTES)
    byte[] paddingBytes;

    /**
     * Device year class, a rough bucket (for example {@code 2018}) that classifies the
     * hardware generation running the client. Serialised as wire index {@code 36}.
     */
    @ProtobufProperty(index = 36, type = ProtobufType.INT32)
    Integer yearClass;

    /**
     * Memory class of the device in megabytes, giving the server a rough idea of how
     * much RAM the client has. Serialised as wire index {@code 37}.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.INT32)
    Integer memClass;

    /**
     * Interoperability data used when the session is being initiated from a non
     * WhatsApp Meta client, see {@link InteropData}. Serialised as wire index
     * {@code 38}.
     */
    @ProtobufProperty(index = 38, type = ProtobufType.MESSAGE)
    InteropData interopData;

    /**
     * Level of connection level traffic anonymization the client is opting into, see
     * {@link TrafficAnonymization}. Serialised as wire index {@code 40}.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.ENUM)
    TrafficAnonymization trafficAnonymization;

    /**
     * Whether the client's local database has already been migrated to the LID
     * (Linked Identity) scheme. Serialised as wire index {@code 41}.
     */
    @ProtobufProperty(index = 41, type = ProtobufType.BOOL)
    Boolean lidDbMigrated;

    /**
     * Kind of account that is connecting, see {@link AccountType}. Serialised as wire
     * index {@code 42}.
     */
    @ProtobufProperty(index = 42, type = ProtobufType.ENUM)
    AccountType accountType;

    /**
     * Encoded metadata about where the current connection sits in the client's own
     * connection sequence, used by the server for debugging reconnect storms.
     * Serialised as wire index {@code 43}.
     */
    @ProtobufProperty(index = 43, type = ProtobufType.SFIXED32)
    Integer connectionSequenceInfo;

    /**
     * Whether this is a Privacy Account Authentication (PAA) link session. Serialised
     * as wire index {@code 44}.
     */
    @ProtobufProperty(index = 44, type = ProtobufType.BOOL)
    Boolean paaLink;

    /**
     * Number of server messages the client is ready to pre acknowledge, used to tune
     * flow control on reconnect. Serialised as wire index {@code 45}.
     */
    @ProtobufProperty(index = 45, type = ProtobufType.INT32)
    Integer preacksCount;

    /**
     * Size of the client's inbound message processing queue, giving the server a hint
     * about how much data to stream on reconnect. Serialised as wire index {@code 46}.
     */
    @ProtobufProperty(index = 46, type = ProtobufType.INT32)
    Integer processingQueueSize;

    /**
     * Full protobuf constructor invoked by the generated builder and the deserializer.
     * All arguments mirror the fields of the message and may be {@code null}.
     *
     * @param username               the numeric account username
     * @param passive                the passive connect flag
     * @param userAgent              the client user agent
     * @param webInfo                the web or desktop specific info
     * @param pushName               the account's push name
     * @param sessionId              the client session identifier
     * @param shortConnect           the short connect flag
     * @param connectType            the network type
     * @param connectReason          the connect reason
     * @param shards                 the subscribed shard identifiers
     * @param dnsSource              the DNS resolution source
     * @param connectAttemptCount    the number of connect attempts
     * @param device                 the device index within the account
     * @param devicePairingData      the registration payload for new pairings
     * @param product                the product family
     * @param fbCat                  the Facebook connectivity auth token
     * @param fbUserAgent            the Facebook family user agent blob
     * @param oc                     the origin client flag
     * @param lc                     the login counter
     * @param iosAppExtension        the triggering iOS app extension
     * @param fbAppId                the Facebook app identifier
     * @param fbDeviceId             the Facebook device identifier
     * @param pull                   the pull reconnect flag
     * @param paddingBytes           anti fingerprinting padding
     * @param yearClass              the device year class
     * @param memClass               the device memory class in MB
     * @param interopData            the interoperability data
     * @param trafficAnonymization   the traffic anonymization level
     * @param lidDbMigrated          the LID database migration flag
     * @param accountType            the account type
     * @param connectionSequenceInfo the connection sequence debug info
     * @param paaLink                the PAA link flag
     * @param preacksCount           the pre acknowledgement count
     * @param processingQueueSize    the inbound queue size
     */
    ClientPayload(Long username, Boolean passive, UserAgent userAgent, WebInfo webInfo, String pushName, Integer sessionId, Boolean shortConnect, ConnectType connectType, ConnectReason connectReason, List<Integer> shards, DNSSource dnsSource, Integer connectAttemptCount, Integer device, DevicePairingRegistrationData devicePairingData, Product product, byte[] fbCat, byte[] fbUserAgent, Boolean oc, Integer lc, IOSAppExtension iosAppExtension, Long fbAppId, byte[] fbDeviceId, Boolean pull, byte[] paddingBytes, Integer yearClass, Integer memClass, InteropData interopData, TrafficAnonymization trafficAnonymization, Boolean lidDbMigrated, AccountType accountType, Integer connectionSequenceInfo, Boolean paaLink, Integer preacksCount, Integer processingQueueSize) {
        this.username = username;
        this.passive = passive;
        this.userAgent = userAgent;
        this.webInfo = webInfo;
        this.pushName = pushName;
        this.sessionId = sessionId;
        this.shortConnect = shortConnect;
        this.connectType = connectType;
        this.connectReason = connectReason;
        this.shards = shards;
        this.dnsSource = dnsSource;
        this.connectAttemptCount = connectAttemptCount;
        this.device = device;
        this.devicePairingData = devicePairingData;
        this.product = product;
        this.fbCat = fbCat;
        this.fbUserAgent = fbUserAgent;
        this.oc = oc;
        this.lc = lc;
        this.iosAppExtension = iosAppExtension;
        this.fbAppId = fbAppId;
        this.fbDeviceId = fbDeviceId;
        this.pull = pull;
        this.paddingBytes = paddingBytes;
        this.yearClass = yearClass;
        this.memClass = memClass;
        this.interopData = interopData;
        this.trafficAnonymization = trafficAnonymization;
        this.lidDbMigrated = lidDbMigrated;
        this.accountType = accountType;
        this.connectionSequenceInfo = connectionSequenceInfo;
        this.paaLink = paaLink;
        this.preacksCount = preacksCount;
        this.processingQueueSize = processingQueueSize;
    }

    /**
     * Returns the numeric account username.
     *
     * @return the username, or {@link OptionalLong#empty()} when absent
     */
    public OptionalLong username() {
        return username == null ? OptionalLong.empty() : OptionalLong.of(username);
    }

    /**
     * Returns whether the client is requesting a passive session.
     *
     * @return {@code true} when passive mode was requested, otherwise {@code false}
     */
    public boolean passive() {
        return passive != null && passive;
    }

    /**
     * Returns the client user agent description.
     *
     * @return the user agent, or {@link Optional#empty()} when absent
     */
    public Optional<UserAgent> userAgent() {
        return Optional.ofNullable(userAgent);
    }

    /**
     * Returns the web or desktop specific metadata.
     *
     * @return the web info, or {@link Optional#empty()} when absent
     */
    public Optional<WebInfo> webInfo() {
        return Optional.ofNullable(webInfo);
    }

    /**
     * Returns the account's push notification display name.
     *
     * @return the push name, or {@link Optional#empty()} when absent
     */
    public Optional<String> pushName() {
        return Optional.ofNullable(pushName);
    }

    /**
     * Returns the client session identifier used for telemetry correlation.
     *
     * @return the session id, or {@link OptionalInt#empty()} when absent
     */
    public OptionalInt sessionId() {
        return sessionId == null ? OptionalInt.empty() : OptionalInt.of(sessionId);
    }

    /**
     * Returns whether the client requested a short connect session.
     *
     * @return {@code true} when short connect was requested, otherwise {@code false}
     */
    public boolean shortConnect() {
        return shortConnect != null && shortConnect;
    }

    /**
     * Returns the declared network type.
     *
     * @return the connect type, or {@link Optional#empty()} when absent
     */
    public Optional<ConnectType> connectType() {
        return Optional.ofNullable(connectType);
    }

    /**
     * Returns the reason the client opened this connection.
     *
     * @return the connect reason, or {@link Optional#empty()} when absent
     */
    public Optional<ConnectReason> connectReason() {
        return Optional.ofNullable(connectReason);
    }

    /**
     * Returns the list of shard identifiers the client subscribed to, wrapped in an
     * unmodifiable view.
     *
     * @return the shards, or an empty list when unset
     */
    public List<Integer> shards() {
        return shards == null ? List.of() : Collections.unmodifiableList(shards);
    }

    /**
     * Returns the DNS resolution information describing how the client reached the
     * server hostname.
     *
     * @return the DNS source, or {@link Optional#empty()} when absent
     */
    public Optional<DNSSource> dnsSource() {
        return Optional.ofNullable(dnsSource);
    }

    /**
     * Returns the number of connection attempts the client has made.
     *
     * @return the attempt count, or {@link OptionalInt#empty()} when absent
     */
    public OptionalInt connectAttemptCount() {
        return connectAttemptCount == null ? OptionalInt.empty() : OptionalInt.of(connectAttemptCount);
    }

    /**
     * Returns the device index of the client within the account, where {@code 0}
     * indicates the primary.
     *
     * @return the device index, or {@link OptionalInt#empty()} when absent
     */
    public OptionalInt device() {
        return device == null ? OptionalInt.empty() : OptionalInt.of(device);
    }

    /**
     * Returns the registration data sent on first time pairing.
     *
     * @return the pairing registration data, or {@link Optional#empty()} on login flows
     */
    public Optional<DevicePairingRegistrationData> devicePairingData() {
        return Optional.ofNullable(devicePairingData);
    }

    /**
     * Returns the product family the client belongs to.
     *
     * @return the product, or {@link Optional#empty()} when absent
     */
    public Optional<Product> product() {
        return Optional.ofNullable(product);
    }

    /**
     * Returns the Facebook connectivity authentication token.
     *
     * @return the token bytes, or {@link Optional#empty()} when absent
     */
    public Optional<byte[]> fbCat() {
        return Optional.ofNullable(fbCat);
    }

    /**
     * Returns the Facebook family user agent blob.
     *
     * @return the user agent bytes, or {@link Optional#empty()} when absent
     */
    public Optional<byte[]> fbUserAgent() {
        return Optional.ofNullable(fbUserAgent);
    }

    /**
     * Returns the origin client flag.
     *
     * @return {@code true} when the flag was set, otherwise {@code false}
     */
    public boolean oc() {
        return oc != null && oc;
    }

    /**
     * Returns the login counter.
     *
     * @return the counter, or {@link OptionalInt#empty()} when absent
     */
    public OptionalInt lc() {
        return lc == null ? OptionalInt.empty() : OptionalInt.of(lc);
    }

    /**
     * Returns the iOS app extension that initiated the connection, if any.
     *
     * @return the extension, or {@link Optional#empty()} when absent
     */
    public Optional<IOSAppExtension> iosAppExtension() {
        return Optional.ofNullable(iosAppExtension);
    }

    /**
     * Returns the Facebook application identifier.
     *
     * @return the app id, or {@link OptionalLong#empty()} when absent
     */
    public OptionalLong fbAppId() {
        return fbAppId == null ? OptionalLong.empty() : OptionalLong.of(fbAppId);
    }

    /**
     * Returns the Facebook device identifier.
     *
     * @return the device id bytes, or {@link Optional#empty()} when absent
     */
    public Optional<byte[]> fbDeviceId() {
        return Optional.ofNullable(fbDeviceId);
    }

    /**
     * Returns whether this connection is a pull reconnect.
     *
     * @return {@code true} when pull mode was requested, otherwise {@code false}
     */
    public boolean pull() {
        return pull != null && pull;
    }

    /**
     * Returns the optional padding bytes.
     *
     * @return the padding bytes, or {@link Optional#empty()} when absent
     */
    public Optional<byte[]> paddingBytes() {
        return Optional.ofNullable(paddingBytes);
    }

    /**
     * Returns the device year class bucket.
     *
     * @return the year class, or {@link OptionalInt#empty()} when absent
     */
    public OptionalInt yearClass() {
        return yearClass == null ? OptionalInt.empty() : OptionalInt.of(yearClass);
    }

    /**
     * Returns the declared device memory class.
     *
     * @return the memory class in MB, or {@link OptionalInt#empty()} when absent
     */
    public OptionalInt memClass() {
        return memClass == null ? OptionalInt.empty() : OptionalInt.of(memClass);
    }

    /**
     * Returns the interoperability data for cross product sessions.
     *
     * @return the interop data, or {@link Optional#empty()} when absent
     */
    public Optional<InteropData> interopData() {
        return Optional.ofNullable(interopData);
    }

    /**
     * Returns the traffic anonymization level requested by the client.
     *
     * @return the anonymization level, or {@link Optional#empty()} when absent
     */
    public Optional<TrafficAnonymization> trafficAnonymization() {
        return Optional.ofNullable(trafficAnonymization);
    }

    /**
     * Returns whether the local database has been migrated to the LID scheme.
     *
     * @return {@code true} when migrated, otherwise {@code false}
     */
    public boolean lidDbMigrated() {
        return lidDbMigrated != null && lidDbMigrated;
    }

    /**
     * Returns the kind of account that is connecting.
     *
     * @return the account type, or {@link Optional#empty()} when absent
     */
    public Optional<AccountType> accountType() {
        return Optional.ofNullable(accountType);
    }

    /**
     * Returns the encoded connection sequence debug info.
     *
     * @return the sequence info, or {@link OptionalInt#empty()} when absent
     */
    public OptionalInt connectionSequenceInfo() {
        return connectionSequenceInfo == null ? OptionalInt.empty() : OptionalInt.of(connectionSequenceInfo);
    }

    /**
     * Returns whether this is a PAA link session.
     *
     * @return {@code true} when the PAA link flag was set, otherwise {@code false}
     */
    public boolean paaLink() {
        return paaLink != null && paaLink;
    }

    /**
     * Returns the number of messages the client is willing to pre acknowledge.
     *
     * @return the preacks count, or {@link OptionalInt#empty()} when absent
     */
    public OptionalInt preacksCount() {
        return preacksCount == null ? OptionalInt.empty() : OptionalInt.of(preacksCount);
    }

    /**
     * Returns the size of the client's inbound processing queue.
     *
     * @return the queue size, or {@link OptionalInt#empty()} when absent
     */
    public OptionalInt processingQueueSize() {
        return processingQueueSize == null ? OptionalInt.empty() : OptionalInt.of(processingQueueSize);
    }

    /**
     * Replaces the numeric account username.
     *
     * @param username the new username, or {@code null} to clear it
     */
    public void setUsername(Long username) {
        this.username = username;
    }

    /**
     * Replaces the passive connect flag.
     *
     * @param passive the new flag value, or {@code null} to clear it
     */
    public void setPassive(Boolean passive) {
        this.passive = passive;
    }

    /**
     * Replaces the user agent description.
     *
     * @param userAgent the new user agent, or {@code null} to clear it
     */
    public void setUserAgent(UserAgent userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Replaces the web or desktop specific metadata.
     *
     * @param webInfo the new web info, or {@code null} to clear it
     */
    public void setWebInfo(WebInfo webInfo) {
        this.webInfo = webInfo;
    }

    /**
     * Replaces the account's push notification display name.
     *
     * @param pushName the new push name, or {@code null} to clear it
     */
    public void setPushName(String pushName) {
        this.pushName = pushName;
    }

    /**
     * Replaces the client session identifier.
     *
     * @param sessionId the new session id, or {@code null} to clear it
     */
    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Replaces the short connect flag.
     *
     * @param shortConnect the new flag value, or {@code null} to clear it
     */
    public void setShortConnect(Boolean shortConnect) {
        this.shortConnect = shortConnect;
    }

    /**
     * Replaces the declared network type.
     *
     * @param connectType the new connect type, or {@code null} to clear it
     */
    public void setConnectType(ConnectType connectType) {
        this.connectType = connectType;
    }

    /**
     * Replaces the connect reason.
     *
     * @param connectReason the new connect reason, or {@code null} to clear it
     */
    public void setConnectReason(ConnectReason connectReason) {
        this.connectReason = connectReason;
    }

    /**
     * Replaces the list of shard identifiers.
     *
     * @param shards the new shard list, or {@code null} to clear it
     */
    public void setShards(List<Integer> shards) {
        this.shards = shards;
    }

    /**
     * Replaces the DNS resolution information.
     *
     * @param dnsSource the new DNS source, or {@code null} to clear it
     */
    public void setDnsSource(DNSSource dnsSource) {
        this.dnsSource = dnsSource;
    }

    /**
     * Replaces the number of connection attempts.
     *
     * @param connectAttemptCount the new attempt count, or {@code null} to clear it
     */
    public void setConnectAttemptCount(Integer connectAttemptCount) {
        this.connectAttemptCount = connectAttemptCount;
    }

    /**
     * Replaces the device index.
     *
     * @param device the new device index, or {@code null} to clear it
     */
    public void setDevice(Integer device) {
        this.device = device;
    }

    /**
     * Replaces the registration payload used during first time pairing.
     *
     * @param devicePairingData the new pairing registration data, or {@code null} to
     *                          clear it
     */
    public void setDevicePairingData(DevicePairingRegistrationData devicePairingData) {
        this.devicePairingData = devicePairingData;
    }

    /**
     * Replaces the product family.
     *
     * @param product the new product, or {@code null} to clear it
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    /**
     * Replaces the Facebook connectivity auth token.
     *
     * @param fbCat the new token bytes, or {@code null} to clear it
     */
    public void setFbCat(byte[] fbCat) {
        this.fbCat = fbCat;
    }

    /**
     * Replaces the Facebook family user agent blob.
     *
     * @param fbUserAgent the new user agent bytes, or {@code null} to clear it
     */
    public void setFbUserAgent(byte[] fbUserAgent) {
        this.fbUserAgent = fbUserAgent;
    }

    /**
     * Replaces the origin client flag.
     *
     * @param oc the new flag value, or {@code null} to clear it
     */
    public void setOc(Boolean oc) {
        this.oc = oc;
    }

    /**
     * Replaces the login counter.
     *
     * @param lc the new counter, or {@code null} to clear it
     */
    public void setLc(Integer lc) {
        this.lc = lc;
    }

    /**
     * Replaces the iOS app extension identifier.
     *
     * @param iosAppExtension the new extension, or {@code null} to clear it
     */
    public void setIosAppExtension(IOSAppExtension iosAppExtension) {
        this.iosAppExtension = iosAppExtension;
    }

    /**
     * Replaces the Facebook application identifier.
     *
     * @param fbAppId the new app id, or {@code null} to clear it
     */
    public void setFbAppId(Long fbAppId) {
        this.fbAppId = fbAppId;
    }

    /**
     * Replaces the Facebook device identifier.
     *
     * @param fbDeviceId the new device id bytes, or {@code null} to clear it
     */
    public void setFbDeviceId(byte[] fbDeviceId) {
        this.fbDeviceId = fbDeviceId;
    }

    /**
     * Replaces the pull reconnect flag.
     *
     * @param pull the new flag value, or {@code null} to clear it
     */
    public void setPull(Boolean pull) {
        this.pull = pull;
    }

    /**
     * Replaces the padding bytes.
     *
     * @param paddingBytes the new padding bytes, or {@code null} to clear it
     */
    public void setPaddingBytes(byte[] paddingBytes) {
        this.paddingBytes = paddingBytes;
    }

    /**
     * Replaces the device year class.
     *
     * @param yearClass the new year class, or {@code null} to clear it
     */
    public void setYearClass(Integer yearClass) {
        this.yearClass = yearClass;
    }

    /**
     * Replaces the device memory class.
     *
     * @param memClass the new memory class in MB, or {@code null} to clear it
     */
    public void setMemClass(Integer memClass) {
        this.memClass = memClass;
    }

    /**
     * Replaces the interoperability data.
     *
     * @param interopData the new interop data, or {@code null} to clear it
     */
    public void setInteropData(InteropData interopData) {
        this.interopData = interopData;
    }

    /**
     * Replaces the traffic anonymization level.
     *
     * @param trafficAnonymization the new anonymization level, or {@code null} to clear
     *                             it
     */
    public void setTrafficAnonymization(TrafficAnonymization trafficAnonymization) {
        this.trafficAnonymization = trafficAnonymization;
    }

    /**
     * Replaces the LID database migration flag.
     *
     * @param lidDbMigrated the new flag value, or {@code null} to clear it
     */
    public void setLidDbMigrated(Boolean lidDbMigrated) {
        this.lidDbMigrated = lidDbMigrated;
    }

    /**
     * Replaces the account type.
     *
     * @param accountType the new account type, or {@code null} to clear it
     */
    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    /**
     * Replaces the connection sequence debug info.
     *
     * @param connectionSequenceInfo the new sequence info, or {@code null} to clear it
     */
    public void setConnectionSequenceInfo(Integer connectionSequenceInfo) {
        this.connectionSequenceInfo = connectionSequenceInfo;
    }

    /**
     * Replaces the PAA link flag.
     *
     * @param paaLink the new flag value, or {@code null} to clear it
     */
    public void setPaaLink(Boolean paaLink) {
        this.paaLink = paaLink;
    }

    /**
     * Replaces the pre acknowledgement count.
     *
     * @param preacksCount the new count, or {@code null} to clear it
     */
    public void setPreacksCount(Integer preacksCount) {
        this.preacksCount = preacksCount;
    }

    /**
     * Replaces the inbound processing queue size.
     *
     * @param processingQueueSize the new queue size, or {@code null} to clear it
     */
    public void setProcessingQueueSize(Integer processingQueueSize) {
        this.processingQueueSize = processingQueueSize;
    }

    /**
     * Kind of account that is connecting to WhatsApp.
     *
     * <p>Populated by flows that distinguish between ordinary accounts and ephemeral
     * guest accounts used for temporary or restricted sessions.
     */
    @ProtobufEnum(name = "ClientPayload.AccountType")
    public static enum AccountType {
        /** Regular long lived account. */
        DEFAULT(0),
        /** Ephemeral guest account with restricted capabilities. */
        GUEST(1);

        /**
         * Protobuf constructor that records the numeric wire value assigned to each
         * entry.
         *
         * @param index the stable wire index
         */
        AccountType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /** Stable wire value of this entry. */
        final int index;

        /**
         * Returns the wire value of this entry.
         *
         * @return the numeric index used on the wire
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Reason the client is opening a connection.
     *
     * <p>WhatsApp's servers use this enum purely for telemetry. It lets operations
     * engineers understand how connect storms are triggered and which reconnection path
     * the client hit.
     */
    @ProtobufEnum(name = "ClientPayload.ConnectReason")
    public static enum ConnectReason {
        /** Connection was triggered by a server push. */
        PUSH(0),
        /** User opened the app explicitly. */
        USER_ACTIVATED(1),
        /** Periodic scheduled connection. */
        SCHEDULED(2),
        /** Reconnection after a previous failure. */
        ERROR_RECONNECT(3),
        /** Reconnection after a network switch (for example Wi Fi to cellular). */
        NETWORK_SWITCH(4),
        /** Reconnection after a ping timeout. */
        PING_RECONNECT(5),
        /** Reason is unknown or not reported. */
        UNKNOWN(6);

        /**
         * Protobuf constructor that records the numeric wire value assigned to each
         * entry.
         *
         * @param index the stable wire index
         */
        ConnectReason(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /** Stable wire value of this entry. */
        final int index;

        /**
         * Returns the wire value of this entry.
         *
         * @return the numeric index used on the wire
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Type of network the client believes it is connected to.
     *
     * <p>The values mirror the constants Android exposes for cellular data types. Only
     * {@link #CELLULAR_UNKNOWN} and {@link #WIFI_UNKNOWN} are common for non Android
     * clients; the granular cellular generations are provided for telemetry parity.
     */
    @ProtobufEnum(name = "ClientPayload.ConnectType")
    public static enum ConnectType {
        /** Generic cellular data without further detail. */
        CELLULAR_UNKNOWN(0),
        /** Generic Wi Fi data without further detail. */
        WIFI_UNKNOWN(1),
        /** 2G EDGE. */
        CELLULAR_EDGE(100),
        /** iDEN (Integrated Digital Enhanced Network). */
        CELLULAR_IDEN(101),
        /** 3G UMTS. */
        CELLULAR_UMTS(102),
        /** 3G EVDO. */
        CELLULAR_EVDO(103),
        /** 2G GPRS. */
        CELLULAR_GPRS(104),
        /** 3G HSDPA. */
        CELLULAR_HSDPA(105),
        /** 3G HSUPA. */
        CELLULAR_HSUPA(106),
        /** 3G HSPA. */
        CELLULAR_HSPA(107),
        /** 2G/3G CDMA. */
        CELLULAR_CDMA(108),
        /** CDMA 1xRTT. */
        CELLULAR_1XRTT(109),
        /** eHRPD. */
        CELLULAR_EHRPD(110),
        /** 4G LTE. */
        CELLULAR_LTE(111),
        /** HSPA+. */
        CELLULAR_HSPAP(112);

        /**
         * Protobuf constructor that records the numeric wire value assigned to each
         * entry.
         *
         * @param index the stable wire index
         */
        ConnectType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /** Stable wire value of this entry. */
        final int index;

        /**
         * Returns the wire value of this entry.
         *
         * @return the numeric index used on the wire
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Identifies which iOS app extension triggered a connection.
     *
     * <p>iOS apps can run short lived extensions (for example the share sheet or the
     * push notification service) that open their own WhatsApp session. This enum tells
     * the server which extension opened the socket so it can scope the work accordingly.
     */
    @ProtobufEnum(name = "ClientPayload.IOSAppExtension")
    public static enum IOSAppExtension {
        /** Share sheet extension. */
        SHARE_EXTENSION(0),
        /** Notification service extension. */
        SERVICE_EXTENSION(1),
        /** Siri Intents extension. */
        INTENTS_EXTENSION(2);

        /**
         * Protobuf constructor that records the numeric wire value assigned to each
         * entry.
         *
         * @param index the stable wire index
         */
        IOSAppExtension(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /** Stable wire value of this entry. */
        final int index;

        /**
         * Returns the wire value of this entry.
         *
         * @return the numeric index used on the wire
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Identifies the Meta product family that a client belongs to.
     *
     * <p>Most Cobalt sessions advertise {@link #WHATSAPP}. Other values are sent by
     * Messenger, the cross product Interop surface and internal LID specific builds.
     */
    @ProtobufEnum(name = "ClientPayload.Product")
    public static enum Product {
        /** Consumer WhatsApp product. */
        WHATSAPP(0),
        /** Messenger product. */
        MESSENGER(1),
        /** Interoperability surface used by third party apps. */
        INTEROP(2),
        /** Messenger side of the Interoperability surface. */
        INTEROP_MSGR(3),
        /** LID specific WhatsApp internal build. */
        WHATSAPP_LID(4);

        /**
         * Protobuf constructor that records the numeric wire value assigned to each
         * entry.
         *
         * @param index the stable wire index
         */
        Product(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /** Stable wire value of this entry. */
        final int index;

        /**
         * Returns the wire value of this entry.
         *
         * @return the numeric index used on the wire
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Level of connection level traffic anonymization the client wishes to use.
     *
     * <p>When {@link #STANDARD} is selected the server avoids logging identifiers such
     * as the source IP address at the connection layer. When {@link #OFF} is selected
     * the traffic is handled without any extra anonymization.
     */
    @ProtobufEnum(name = "ClientPayload.TrafficAnonymization")
    public static enum TrafficAnonymization {
        /** No anonymization applied. */
        OFF(0),
        /** Standard server side anonymization. */
        STANDARD(1);

        /**
         * Protobuf constructor that records the numeric wire value assigned to each
         * entry.
         *
         * @param index the stable wire index
         */
        TrafficAnonymization(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /** Stable wire value of this entry. */
        final int index;

        /**
         * Returns the wire value of this entry.
         *
         * @return the numeric index used on the wire
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Release channel of the client build.
     *
     * <p>The server uses the release channel to decide which experimental features it
     * is willing to hand out to the client.
     */
    @ProtobufEnum(name = "ClientPayload.UserAgent.ReleaseChannel")
    public static enum ClientReleaseChannel {
        /** Public release build. */
        RELEASE(0),
        /** Public beta build. */
        BETA(1),
        /** Internal alpha build. */
        ALPHA(2),
        /** Developer debug build. */
        DEBUG(3);

        /**
         * Protobuf constructor that records the numeric wire value assigned to each
         * entry.
         *
         * @param index the stable wire index
         */
        ClientReleaseChannel(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /** Stable wire value of this entry. */
        final int index;

        /**
         * Returns the wire value of this entry.
         *
         * @return the numeric index used on the wire
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Form factor of the device hosting the client.
     *
     * <p>Used by the server to tune behaviour, for example by assuming a phone is the
     * primary device and rejecting attempts to link another phone as a companion.
     */
    @ProtobufEnum(name = "ClientPayload.UserAgent.DeviceType")
    public static enum ClientType {
        /** Smartphone form factor. */
        PHONE(0),
        /** Tablet form factor. */
        TABLET(1),
        /** Desktop or laptop. */
        DESKTOP(2),
        /** Wearable device such as a smartwatch. */
        WEARABLE(3),
        /** Virtual reality headset. */
        VR(4);

        /**
         * Protobuf constructor that records the numeric wire value assigned to each
         * entry.
         *
         * @param index the stable wire index
         */
        ClientType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /** Stable wire value of this entry. */
        final int index;

        /**
         * Returns the wire value of this entry.
         *
         * @return the numeric index used on the wire
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Describes how the client resolved the server hostname when opening the socket.
     *
     * <p>The server uses this information for network health telemetry, to detect DNS
     * hijacks and to correlate regional outages with specific resolution paths.
     */
    @ProtobufMessage(name = "ClientPayload.DNSSource")
    public static final class DNSSource {
        /**
         * Method used to turn the hostname into an address, see
         * {@link DNSResolutionMethod}. Serialised as wire index {@code 15}.
         */
        @ProtobufProperty(index = 15, type = ProtobufType.ENUM)
        DNSSource.DNSResolutionMethod dnsMethod;

        /**
         * Whether the resolved record came out of a client side DNS cache rather than a
         * fresh network lookup. Serialised as wire index {@code 16}.
         */
        @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
        Boolean appCached;

        /**
         * Full protobuf constructor invoked by the generated builder and the
         * deserializer.
         *
         * @param dnsMethod the DNS resolution method
         * @param appCached the app cache flag
         */
        DNSSource(DNSResolutionMethod dnsMethod, Boolean appCached) {
            this.dnsMethod = dnsMethod;
            this.appCached = appCached;
        }

        /**
         * Returns the DNS resolution method.
         *
         * @return the resolution method, or {@link Optional#empty()} when absent
         */
        public Optional<DNSResolutionMethod> dnsMethod() {
            return Optional.ofNullable(dnsMethod);
        }

        /**
         * Returns whether the DNS lookup was served from the app cache.
         *
         * @return {@code true} when the record came from the cache, otherwise
         *         {@code false}
         */
        public boolean appCached() {
            return appCached != null && appCached;
        }

        /**
         * Replaces the DNS resolution method.
         *
         * @param dnsMethod the new method, or {@code null} to clear it
         */
        public void setDnsMethod(DNSResolutionMethod dnsMethod) {
            this.dnsMethod = dnsMethod;
    }

        /**
         * Replaces the app cache flag.
         *
         * @param appCached the new flag value, or {@code null} to clear it
         */
        public void setAppCached(Boolean appCached) {
            this.appCached = appCached;
    }

        /**
         * Concrete mechanism used by the client to resolve the server hostname.
         *
         * <p>Values range from the operating system's resolver, through hard coded
         * fallbacks baked into the app binary, to connections going via a SOCKS proxy.
         */
        @ProtobufEnum(name = "ClientPayload.DNSSource.DNSResolutionMethod")
        public static enum DNSResolutionMethod {
            /** System provided resolver (for example the platform DNS APIs). */
            SYSTEM(0),
            /** Google public DNS. */
            GOOGLE(1),
            /** Hard coded IP address shipped with the client. */
            HARDCODED(2),
            /** Administrator or user supplied override. */
            OVERRIDE(3),
            /** Fallback resolver used when the primary method fails. */
            FALLBACK(4),
            /** Meta Name Service primary. */
            MNS(5),
            /** Meta Name Service secondary. */
            MNS_SECONDARY(6),
            /** Resolution performed through a SOCKS proxy. */
            SOCKS_PROXY(7);

            /**
             * Protobuf constructor that records the numeric wire value assigned to
             * each entry.
             *
             * @param index the stable wire index
             */
            DNSResolutionMethod(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /** Stable wire value of this entry. */
            final int index;

            /**
             * Returns the wire value of this entry.
             *
             * @return the numeric index used on the wire
             */
            public int index() {
                return this.index;
            }
        }
    }

    /**
     * Cryptographic material sent by a companion device when it registers itself with
     * the server for the first time.
     *
     * <p>The fields mirror the companion's Signal prekey bundle: the registration id,
     * identity key, signed prekey and signature. The server stores this data against
     * the newly created device entry so that future sessions with the companion can
     * derive shared secrets without asking for it again. The {@link #buildHash} field
     * carries the MD5 of the client's app version (see {@link ClientAppVersion}) and
     * {@link #deviceProps} carries an opaque serialised {@code DeviceProps} describing
     * the companion's feature flags.
     */
    @ProtobufMessage(name = "ClientPayload.DevicePairingRegistrationData")
    public static final class DevicePairingRegistrationData {
        /**
         * Big endian four byte encoding of the companion's Signal registration id.
         * Serialised as wire index {@code 1}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] eRegid;

        /**
         * Single byte key type identifier, always the Curve25519 indicator in practice.
         * Serialised as wire index {@code 2}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] eKeytype;

        /**
         * Companion's long lived identity public key. Serialised as wire index
         * {@code 3}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] eIdent;

        /**
         * Three byte big endian encoding of the id of the signed prekey being
         * registered. Serialised as wire index {@code 4}.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
        byte[] eSkeyId;

        /**
         * Public half of the signed prekey. Serialised as wire index {@code 5}.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
        byte[] eSkeyVal;

        /**
         * Signature of {@link #eSkeyVal} made with the companion's identity key.
         * Serialised as wire index {@code 6}.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
        byte[] eSkeySig;

        /**
         * MD5 digest of the client app version, matching {@link ClientAppVersion#toHash()}.
         * Serialised as wire index {@code 7}.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
        byte[] buildHash;

        /**
         * Opaque serialised {@code DeviceProps} protobuf that advertises companion
         * feature flags to the server. Serialised as wire index {@code 8}.
         */
        @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
        byte[] deviceProps;

        /**
         * Full protobuf constructor invoked by the generated builder and the
         * deserializer.
         *
         * @param eRegid      the encoded registration id
         * @param eKeytype    the encoded key type byte
         * @param eIdent      the identity public key
         * @param eSkeyId     the encoded signed prekey id
         * @param eSkeyVal    the signed prekey public value
         * @param eSkeySig    the signed prekey signature
         * @param buildHash   the MD5 of the client app version
         * @param deviceProps the serialised DeviceProps blob
         */
        DevicePairingRegistrationData(byte[] eRegid, byte[] eKeytype, byte[] eIdent, byte[] eSkeyId, byte[] eSkeyVal, byte[] eSkeySig, byte[] buildHash, byte[] deviceProps) {
            this.eRegid = eRegid;
            this.eKeytype = eKeytype;
            this.eIdent = eIdent;
            this.eSkeyId = eSkeyId;
            this.eSkeyVal = eSkeyVal;
            this.eSkeySig = eSkeySig;
            this.buildHash = buildHash;
            this.deviceProps = deviceProps;
        }

        /**
         * Returns the encoded registration id.
         *
         * @return the registration id bytes, or {@link Optional#empty()} when absent
         */
        public Optional<byte[]> eRegid() {
            return Optional.ofNullable(eRegid);
        }

        /**
         * Returns the encoded key type byte.
         *
         * @return the key type bytes, or {@link Optional#empty()} when absent
         */
        public Optional<byte[]> eKeytype() {
            return Optional.ofNullable(eKeytype);
        }

        /**
         * Returns the identity public key.
         *
         * @return the identity key bytes, or {@link Optional#empty()} when absent
         */
        public Optional<byte[]> eIdent() {
            return Optional.ofNullable(eIdent);
        }

        /**
         * Returns the encoded signed prekey id.
         *
         * @return the signed prekey id bytes, or {@link Optional#empty()} when absent
         */
        public Optional<byte[]> eSkeyId() {
            return Optional.ofNullable(eSkeyId);
        }

        /**
         * Returns the signed prekey public value.
         *
         * @return the signed prekey bytes, or {@link Optional#empty()} when absent
         */
        public Optional<byte[]> eSkeyVal() {
            return Optional.ofNullable(eSkeyVal);
        }

        /**
         * Returns the signature over the signed prekey.
         *
         * @return the signature bytes, or {@link Optional#empty()} when absent
         */
        public Optional<byte[]> eSkeySig() {
            return Optional.ofNullable(eSkeySig);
        }

        /**
         * Returns the MD5 digest of the client app version.
         *
         * @return the build hash bytes, or {@link Optional#empty()} when absent
         */
        public Optional<byte[]> buildHash() {
            return Optional.ofNullable(buildHash);
        }

        /**
         * Returns the serialised DeviceProps blob.
         *
         * @return the device props bytes, or {@link Optional#empty()} when absent
         */
        public Optional<byte[]> deviceProps() {
            return Optional.ofNullable(deviceProps);
        }

        /**
         * Replaces the encoded registration id.
         *
         * @param eRegid the new registration id bytes, or {@code null} to clear it
         */
        public void setERegid(byte[] eRegid) {
            this.eRegid = eRegid;
    }

        /**
         * Replaces the encoded key type byte.
         *
         * @param eKeytype the new key type bytes, or {@code null} to clear it
         */
        public void setEKeytype(byte[] eKeytype) {
            this.eKeytype = eKeytype;
    }

        /**
         * Replaces the identity public key.
         *
         * @param eIdent the new identity key bytes, or {@code null} to clear it
         */
        public void setEIdent(byte[] eIdent) {
            this.eIdent = eIdent;
    }

        /**
         * Replaces the encoded signed prekey id.
         *
         * @param eSkeyId the new signed prekey id bytes, or {@code null} to clear it
         */
        public void setESkeyId(byte[] eSkeyId) {
            this.eSkeyId = eSkeyId;
    }

        /**
         * Replaces the signed prekey public value.
         *
         * @param eSkeyVal the new signed prekey bytes, or {@code null} to clear it
         */
        public void setESkeyVal(byte[] eSkeyVal) {
            this.eSkeyVal = eSkeyVal;
    }

        /**
         * Replaces the signature over the signed prekey.
         *
         * @param eSkeySig the new signature bytes, or {@code null} to clear it
         */
        public void setESkeySig(byte[] eSkeySig) {
            this.eSkeySig = eSkeySig;
    }

        /**
         * Replaces the MD5 digest of the client app version.
         *
         * @param buildHash the new build hash bytes, or {@code null} to clear it
         */
        public void setBuildHash(byte[] buildHash) {
            this.buildHash = buildHash;
    }

        /**
         * Replaces the serialised DeviceProps blob.
         *
         * @param deviceProps the new device props bytes, or {@code null} to clear it
         */
        public void setDeviceProps(byte[] deviceProps) {
            this.deviceProps = deviceProps;
    }
    }

    /**
     * Extra data used when the session is being established from a non WhatsApp Meta
     * product that talks to WhatsApp through the Interoperability surface.
     *
     * <p>Only populated when {@link ClientPayload#product} is
     * {@link Product#INTEROP} or {@link Product#INTEROP_MSGR}.
     */
    @ProtobufMessage(name = "ClientPayload.InteropData")
    public static final class InteropData {
        /**
         * Numeric identifier of the Meta account the interop session is tied to.
         * Serialised as wire index {@code 1}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.UINT64)
        Long accountId;

        /**
         * Opaque authentication token issued by the interop flow. Serialised as wire
         * index {@code 2}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] token;

        /**
         * Whether the user has consented to sending read receipts over the interop
         * bridge. Serialised as wire index {@code 3}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
        Boolean enableReadReceipts;

        /**
         * Full protobuf constructor invoked by the generated builder and the
         * deserializer.
         *
         * @param accountId          the Meta account id
         * @param token              the interop auth token
         * @param enableReadReceipts the read receipt consent flag
         */
        InteropData(Long accountId, byte[] token, Boolean enableReadReceipts) {
            this.accountId = accountId;
            this.token = token;
            this.enableReadReceipts = enableReadReceipts;
        }

        /**
         * Returns the Meta account identifier.
         *
         * @return the account id, or {@link OptionalLong#empty()} when absent
         */
        public OptionalLong accountId() {
            return accountId == null ? OptionalLong.empty() : OptionalLong.of(accountId);
        }

        /**
         * Returns the interop authentication token.
         *
         * @return the token bytes, or {@link Optional#empty()} when absent
         */
        public Optional<byte[]> token() {
            return Optional.ofNullable(token);
        }

        /**
         * Returns whether read receipts are enabled for this interop session.
         *
         * @return {@code true} when read receipts are allowed, otherwise {@code false}
         */
        public boolean enableReadReceipts() {
            return enableReadReceipts != null && enableReadReceipts;
        }

        /**
         * Replaces the Meta account identifier.
         *
         * @param accountId the new account id, or {@code null} to clear it
         */
        public void setAccountId(Long accountId) {
            this.accountId = accountId;
    }

        /**
         * Replaces the interop authentication token.
         *
         * @param token the new token bytes, or {@code null} to clear it
         */
        public void setToken(byte[] token) {
            this.token = token;
    }

        /**
         * Replaces the read receipt consent flag.
         *
         * @param enableReadReceipts the new flag value, or {@code null} to clear it
         */
        public void setEnableReadReceipts(Boolean enableReadReceipts) {
            this.enableReadReceipts = enableReadReceipts;
    }
    }

    /**
     * Detailed description of the platform and build that is connecting to WhatsApp.
     *
     * <p>The user agent bundles the client platform (see {@link ClientPlatformType}),
     * the app version (see {@link ClientAppVersion}), locale and carrier information
     * plus hardware metadata such as the manufacturer and model. The server uses it to
     * decide which features to enable and to key many of its telemetry pipelines.
     */
    @ProtobufMessage(name = "ClientPayload.UserAgent")
    public static final class UserAgent {
        /**
         * Platform this client is running on. Serialised as wire index {@code 1}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        ClientPlatformType platform;

        /**
         * Application version. Serialised as wire index {@code 2}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        ClientAppVersion appVersion;

        /**
         * Mobile Country Code of the device's cellular carrier, if any. Serialised as
         * wire index {@code 3}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String mcc;

        /**
         * Mobile Network Code of the device's cellular carrier, if any. Serialised as
         * wire index {@code 4}.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String mnc;

        /**
         * Version of the operating system hosting the client. Serialised as wire index
         * {@code 5}.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String osVersion;

        /**
         * Device manufacturer name (for example {@code "Apple"} or {@code "Samsung"}).
         * Serialised as wire index {@code 6}.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        String manufacturer;

        /**
         * Device model marketing name. Serialised as wire index {@code 7}.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        String device;

        /**
         * Operating system build number. Serialised as wire index {@code 8}.
         */
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        String osBuildNumber;

        /**
         * Stable random identifier assigned to the device by WhatsApp. Serialised as
         * wire index {@code 9}.
         */
        @ProtobufProperty(index = 9, type = ProtobufType.STRING)
        String phoneId;

        /**
         * Release channel of the build, see {@link ClientReleaseChannel}. Serialised as
         * wire index {@code 10}.
         */
        @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
        ClientReleaseChannel releaseChannel;

        /**
         * Locale language as an ISO 639 1 code (for example {@code "en"}). Serialised
         * as wire index {@code 11}.
         */
        @ProtobufProperty(index = 11, type = ProtobufType.STRING)
        String localeLanguageIso6391;

        /**
         * Locale country as an ISO 3166 1 alpha 2 code (for example {@code "US"}).
         * Serialised as wire index {@code 12}.
         */
        @ProtobufProperty(index = 12, type = ProtobufType.STRING)
        String localeCountryIso31661Alpha2;

        /**
         * Hardware board name reported by the OS. Serialised as wire index {@code 13}.
         */
        @ProtobufProperty(index = 13, type = ProtobufType.STRING)
        String deviceBoard;

        /**
         * Device experiment id used by Meta's internal A/B testing infrastructure.
         * Serialised as wire index {@code 14}.
         */
        @ProtobufProperty(index = 14, type = ProtobufType.STRING)
        String deviceExpId;

        /**
         * Form factor of the device, see {@link ClientType}. Serialised as wire index
         * {@code 15}.
         */
        @ProtobufProperty(index = 15, type = ProtobufType.ENUM)
        ClientType deviceType;

        /**
         * Marketing name of the specific device model variant. Serialised as wire
         * index {@code 16}.
         */
        @ProtobufProperty(index = 16, type = ProtobufType.STRING)
        String deviceModelType;

        /**
         * Full protobuf constructor invoked by the generated builder and the
         * deserializer.
         *
         * @param platform                    the platform
         * @param appVersion                  the app version
         * @param mcc                         the mobile country code
         * @param mnc                         the mobile network code
         * @param osVersion                   the OS version string
         * @param manufacturer                the device manufacturer
         * @param device                      the device marketing name
         * @param osBuildNumber               the OS build number
         * @param phoneId                     the stable device identifier
         * @param releaseChannel              the release channel
         * @param localeLanguageIso6391       the ISO 639 1 language code
         * @param localeCountryIso31661Alpha2 the ISO 3166 1 alpha 2 country code
         * @param deviceBoard                 the hardware board name
         * @param deviceExpId                 the Meta experiment identifier
         * @param deviceType                  the device form factor
         * @param deviceModelType             the device model variant
         */
        UserAgent(ClientPlatformType platform, ClientAppVersion appVersion, String mcc, String mnc, String osVersion, String manufacturer, String device, String osBuildNumber, String phoneId, ClientReleaseChannel releaseChannel, String localeLanguageIso6391, String localeCountryIso31661Alpha2, String deviceBoard, String deviceExpId, ClientType deviceType, String deviceModelType) {
            this.platform = platform;
            this.appVersion = appVersion;
            this.mcc = mcc;
            this.mnc = mnc;
            this.osVersion = osVersion;
            this.manufacturer = manufacturer;
            this.device = device;
            this.osBuildNumber = osBuildNumber;
            this.phoneId = phoneId;
            this.releaseChannel = releaseChannel;
            this.localeLanguageIso6391 = localeLanguageIso6391;
            this.localeCountryIso31661Alpha2 = localeCountryIso31661Alpha2;
            this.deviceBoard = deviceBoard;
            this.deviceExpId = deviceExpId;
            this.deviceType = deviceType;
            this.deviceModelType = deviceModelType;
        }

        /**
         * Returns the declared platform.
         *
         * @return the platform, or {@link Optional#empty()} when absent
         */
        public Optional<ClientPlatformType> platform() {
            return Optional.ofNullable(platform);
        }

        /**
         * Returns the application version.
         *
         * @return the version, or {@link Optional#empty()} when absent
         */
        public Optional<ClientAppVersion> appVersion() {
            return Optional.ofNullable(appVersion);
        }

        /**
         * Returns the mobile country code.
         *
         * @return the MCC, or {@link Optional#empty()} when absent
         */
        public Optional<String> mcc() {
            return Optional.ofNullable(mcc);
        }

        /**
         * Returns the mobile network code.
         *
         * @return the MNC, or {@link Optional#empty()} when absent
         */
        public Optional<String> mnc() {
            return Optional.ofNullable(mnc);
        }

        /**
         * Returns the OS version string.
         *
         * @return the OS version, or {@link Optional#empty()} when absent
         */
        public Optional<String> osVersion() {
            return Optional.ofNullable(osVersion);
        }

        /**
         * Returns the device manufacturer.
         *
         * @return the manufacturer, or {@link Optional#empty()} when absent
         */
        public Optional<String> manufacturer() {
            return Optional.ofNullable(manufacturer);
        }

        /**
         * Returns the device marketing name.
         *
         * @return the device name, or {@link Optional#empty()} when absent
         */
        public Optional<String> device() {
            return Optional.ofNullable(device);
        }

        /**
         * Returns the OS build number.
         *
         * @return the build number, or {@link Optional#empty()} when absent
         */
        public Optional<String> osBuildNumber() {
            return Optional.ofNullable(osBuildNumber);
        }

        /**
         * Returns the stable phone identifier.
         *
         * @return the phone id, or {@link Optional#empty()} when absent
         */
        public Optional<String> phoneId() {
            return Optional.ofNullable(phoneId);
        }

        /**
         * Returns the release channel of this build.
         *
         * @return the release channel, or {@link Optional#empty()} when absent
         */
        public Optional<ClientReleaseChannel> releaseChannel() {
            return Optional.ofNullable(releaseChannel);
        }

        /**
         * Returns the ISO 639 1 language code.
         *
         * @return the language code, or {@link Optional#empty()} when absent
         */
        public Optional<String> localeLanguageIso6391() {
            return Optional.ofNullable(localeLanguageIso6391);
        }

        /**
         * Returns the ISO 3166 1 alpha 2 country code.
         *
         * @return the country code, or {@link Optional#empty()} when absent
         */
        public Optional<String> localeCountryIso31661Alpha2() {
            return Optional.ofNullable(localeCountryIso31661Alpha2);
        }

        /**
         * Returns the hardware board name.
         *
         * @return the board name, or {@link Optional#empty()} when absent
         */
        public Optional<String> deviceBoard() {
            return Optional.ofNullable(deviceBoard);
        }

        /**
         * Returns the Meta experiment identifier.
         *
         * @return the experiment id, or {@link Optional#empty()} when absent
         */
        public Optional<String> deviceExpId() {
            return Optional.ofNullable(deviceExpId);
        }

        /**
         * Returns the device form factor.
         *
         * @return the device type, or {@link Optional#empty()} when absent
         */
        public Optional<ClientType> deviceType() {
            return Optional.ofNullable(deviceType);
        }

        /**
         * Returns the device model variant name.
         *
         * @return the model variant, or {@link Optional#empty()} when absent
         */
        public Optional<String> deviceModelType() {
            return Optional.ofNullable(deviceModelType);
        }

        /**
         * Replaces the declared platform.
         *
         * @param platform the new platform, or {@code null} to clear it
         */
        public void setPlatform(ClientPlatformType platform) {
            this.platform = platform;
    }

        /**
         * Replaces the application version.
         *
         * @param appVersion the new version, or {@code null} to clear it
         */
        public void setAppVersion(ClientAppVersion appVersion) {
            this.appVersion = appVersion;
    }

        /**
         * Replaces the mobile country code.
         *
         * @param mcc the new MCC, or {@code null} to clear it
         */
        public void setMcc(String mcc) {
            this.mcc = mcc;
    }

        /**
         * Replaces the mobile network code.
         *
         * @param mnc the new MNC, or {@code null} to clear it
         */
        public void setMnc(String mnc) {
            this.mnc = mnc;
    }

        /**
         * Replaces the OS version string.
         *
         * @param osVersion the new OS version, or {@code null} to clear it
         */
        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
    }

        /**
         * Replaces the device manufacturer.
         *
         * @param manufacturer the new manufacturer, or {@code null} to clear it
         */
        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
    }

        /**
         * Replaces the device marketing name.
         *
         * @param device the new device name, or {@code null} to clear it
         */
        public void setDevice(String device) {
            this.device = device;
    }

        /**
         * Replaces the OS build number.
         *
         * @param osBuildNumber the new build number, or {@code null} to clear it
         */
        public void setOsBuildNumber(String osBuildNumber) {
            this.osBuildNumber = osBuildNumber;
    }

        /**
         * Replaces the stable phone identifier.
         *
         * @param phoneId the new phone id, or {@code null} to clear it
         */
        public void setPhoneId(String phoneId) {
            this.phoneId = phoneId;
    }

        /**
         * Replaces the release channel.
         *
         * @param releaseChannel the new release channel, or {@code null} to clear it
         */
        public void setReleaseChannel(ClientReleaseChannel releaseChannel) {
            this.releaseChannel = releaseChannel;
    }

        /**
         * Replaces the ISO 639 1 language code.
         *
         * @param localeLanguageIso6391 the new language code, or {@code null} to clear
         *                              it
         */
        public void setLocaleLanguageIso6391(String localeLanguageIso6391) {
            this.localeLanguageIso6391 = localeLanguageIso6391;
    }

        /**
         * Replaces the ISO 3166 1 alpha 2 country code.
         *
         * @param localeCountryIso31661Alpha2 the new country code, or {@code null} to
         *                                    clear it
         */
        public void setLocaleCountryIso31661Alpha2(String localeCountryIso31661Alpha2) {
            this.localeCountryIso31661Alpha2 = localeCountryIso31661Alpha2;
    }

        /**
         * Replaces the hardware board name.
         *
         * @param deviceBoard the new board name, or {@code null} to clear it
         */
        public void setDeviceBoard(String deviceBoard) {
            this.deviceBoard = deviceBoard;
    }

        /**
         * Replaces the Meta experiment identifier.
         *
         * @param deviceExpId the new experiment id, or {@code null} to clear it
         */
        public void setDeviceExpId(String deviceExpId) {
            this.deviceExpId = deviceExpId;
    }

        /**
         * Replaces the device form factor.
         *
         * @param deviceType the new device type, or {@code null} to clear it
         */
        public void setDeviceType(ClientType deviceType) {
            this.deviceType = deviceType;
    }

        /**
         * Replaces the device model variant name.
         *
         * @param deviceModelType the new model variant, or {@code null} to clear it
         */
        public void setDeviceModelType(String deviceModelType) {
            this.deviceModelType = deviceModelType;
    }
    }

    /**
     * Web and desktop specific extension to the user agent, set only for clients running
     * inside a browser or an Electron style desktop shell.
     *
     * <p>Mobile clients leave the entire field unset. Web clients populate at least
     * {@link #webSubPlatform}, {@link #browser} and {@link #browserVersion}; the legacy
     * {@link #refToken} and {@link #version} fields are mostly used by older pairing
     * flows. {@link #webdPayload} advertises the fine grained feature flags the web
     * layer supports.
     */
    @ProtobufMessage(name = "ClientPayload.WebInfo")
    public static final class WebInfo {
        /**
         * Reference token issued by a previous pairing step, used to tie the web
         * session back to its QR scan. Serialised as wire index {@code 1}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String refToken;

        /**
         * Version string of the web client bundle. Serialised as wire index {@code 2}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String version;

        /**
         * Web specific feature capability payload, see {@link WebdPayload}. Serialised
         * as wire index {@code 3}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        WebInfo.WebdPayload webdPayload;

        /**
         * Web sub platform (browser, App Store, Windows Store, native wrapper etc.),
         * see {@link WebSubPlatform}. Serialised as wire index {@code 4}.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
        WebInfo.WebSubPlatform webSubPlatform;

        /**
         * Name of the host browser (for example {@code "Chrome"} or {@code "Firefox"}).
         * Serialised as wire index {@code 5}.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String browser;

        /**
         * Version of the host browser. Serialised as wire index {@code 6}.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        String browserVersion;

        /**
         * Full protobuf constructor invoked by the generated builder and the
         * deserializer.
         *
         * @param refToken       the pairing reference token
         * @param version        the web bundle version
         * @param webdPayload    the web feature payload
         * @param webSubPlatform the web sub platform
         * @param browser        the browser name
         * @param browserVersion the browser version
         */
        WebInfo(String refToken, String version, WebdPayload webdPayload, WebSubPlatform webSubPlatform, String browser, String browserVersion) {
            this.refToken = refToken;
            this.version = version;
            this.webdPayload = webdPayload;
            this.webSubPlatform = webSubPlatform;
            this.browser = browser;
            this.browserVersion = browserVersion;
        }

        /**
         * Returns the pairing reference token.
         *
         * @return the token, or {@link Optional#empty()} when absent
         */
        public Optional<String> refToken() {
            return Optional.ofNullable(refToken);
        }

        /**
         * Returns the web bundle version string.
         *
         * @return the version, or {@link Optional#empty()} when absent
         */
        public Optional<String> version() {
            return Optional.ofNullable(version);
        }

        /**
         * Returns the web feature capability payload.
         *
         * @return the payload, or {@link Optional#empty()} when absent
         */
        public Optional<WebdPayload> webdPayload() {
            return Optional.ofNullable(webdPayload);
        }

        /**
         * Returns the web sub platform.
         *
         * @return the sub platform, or {@link Optional#empty()} when absent
         */
        public Optional<WebSubPlatform> webSubPlatform() {
            return Optional.ofNullable(webSubPlatform);
        }

        /**
         * Returns the host browser name.
         *
         * @return the browser name, or {@link Optional#empty()} when absent
         */
        public Optional<String> browser() {
            return Optional.ofNullable(browser);
        }

        /**
         * Returns the host browser version.
         *
         * @return the browser version, or {@link Optional#empty()} when absent
         */
        public Optional<String> browserVersion() {
            return Optional.ofNullable(browserVersion);
        }

        /**
         * Replaces the pairing reference token.
         *
         * @param refToken the new token, or {@code null} to clear it
         */
        public void setRefToken(String refToken) {
            this.refToken = refToken;
    }

        /**
         * Replaces the web bundle version.
         *
         * @param version the new version, or {@code null} to clear it
         */
        public void setVersion(String version) {
            this.version = version;
    }

        /**
         * Replaces the web feature capability payload.
         *
         * @param webdPayload the new payload, or {@code null} to clear it
         */
        public void setWebdPayload(WebdPayload webdPayload) {
            this.webdPayload = webdPayload;
    }

        /**
         * Replaces the web sub platform.
         *
         * @param webSubPlatform the new sub platform, or {@code null} to clear it
         */
        public void setWebSubPlatform(WebSubPlatform webSubPlatform) {
            this.webSubPlatform = webSubPlatform;
    }

        /**
         * Replaces the host browser name.
         *
         * @param browser the new browser name, or {@code null} to clear it
         */
        public void setBrowser(String browser) {
            this.browser = browser;
    }

        /**
         * Replaces the host browser version.
         *
         * @param browserVersion the new browser version, or {@code null} to clear it
         */
        public void setBrowserVersion(String browserVersion) {
            this.browserVersion = browserVersion;
    }

        /**
         * Sub platform that a web or desktop client is running in.
         *
         * <p>Distinguishes between classic browser based WhatsApp Web and the various
         * native wrappers that embed the same bundle (App Store build, Microsoft Store
         * build, Electron, Win32 and the hybrid Windows shell).
         */
        @ProtobufEnum(name = "ClientPayload.WebInfo.WebSubPlatform")
        public static enum WebSubPlatform {
            /** Running inside a standard web browser. */
            WEB_BROWSER(0),
            /** macOS App Store build. */
            APP_STORE(1),
            /** Microsoft Store build. */
            WIN_STORE(2),
            /** Darwin based native wrapper (Electron or equivalent on macOS). */
            DARWIN(3),
            /** Classic Win32 native wrapper. */
            WIN32(4),
            /** Hybrid native/web Windows shell. */
            WIN_HYBRID(5);

            /**
             * Protobuf constructor that records the numeric wire value assigned to
             * each entry.
             *
             * @param index the stable wire index
             */
            WebSubPlatform(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /** Stable wire value of this entry. */
            final int index;

            /**
             * Returns the wire value of this entry.
             *
             * @return the numeric index used on the wire
             */
            public int index() {
                return this.index;
            }
        }

        /**
         * Feature capability payload advertised by web and desktop clients during
         * connection setup.
         *
         * <p>Each boolean describes a feature the web bundle already supports. The
         * server uses these flags to decide whether it can deliver certain message
         * types, reactions, polls or document types without any further negotiation.
         * The {@link #features} field carries an opaque binary blob of additional
         * capabilities that are more fluid than the fixed booleans.
         */
        @ProtobufMessage(name = "ClientPayload.WebInfo.WebdPayload")
        public static final class WebdPayload {
            /**
             * Whether the client uses the new participant scoped message keys.
             * Serialised as wire index {@code 1}.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
            Boolean usesParticipantInKey;

            /**
             * Whether the client supports starred messages. Serialised as wire index
             * {@code 2}.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
            Boolean supportsStarredMessages;

            /**
             * Whether the client supports displaying document messages. Serialised as
             * wire index {@code 3}.
             */
            @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
            Boolean supportsDocumentMessages;

            /**
             * Whether the client supports rendering URL preview messages. Serialised
             * as wire index {@code 4}.
             */
            @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
            Boolean supportsUrlMessages;

            /**
             * Whether the client supports retrying failed media downloads via the
             * media retry channel. Serialised as wire index {@code 5}.
             */
            @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
            Boolean supportsMediaRetry;

            /**
             * Whether the client supports end to end encrypted images. Serialised as
             * wire index {@code 6}.
             */
            @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
            Boolean supportsE2EImage;

            /**
             * Whether the client supports end to end encrypted video. Serialised as
             * wire index {@code 7}.
             */
            @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
            Boolean supportsE2EVideo;

            /**
             * Whether the client supports end to end encrypted audio. Serialised as
             * wire index {@code 8}.
             */
            @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
            Boolean supportsE2EAudio;

            /**
             * Whether the client supports end to end encrypted documents. Serialised
             * as wire index {@code 9}.
             */
            @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
            Boolean supportsE2EDocument;

            /**
             * Comma separated list of document MIME types the client is willing to
             * display. Serialised as wire index {@code 10}.
             */
            @ProtobufProperty(index = 10, type = ProtobufType.STRING)
            String documentTypes;

            /**
             * Opaque binary bitfield of additional negotiated features. Serialised as
             * wire index {@code 11}.
             */
            @ProtobufProperty(index = 11, type = ProtobufType.BYTES)
            byte[] features;

            /**
             * Full protobuf constructor invoked by the generated builder and the
             * deserializer.
             *
             * @param usesParticipantInKey     the participant scoped keys flag
             * @param supportsStarredMessages  the starred messages flag
             * @param supportsDocumentMessages the document message support flag
             * @param supportsUrlMessages      the URL preview support flag
             * @param supportsMediaRetry       the media retry support flag
             * @param supportsE2EImage         the E2E image support flag
             * @param supportsE2EVideo         the E2E video support flag
             * @param supportsE2EAudio         the E2E audio support flag
             * @param supportsE2EDocument      the E2E document support flag
             * @param documentTypes            the accepted document MIME types
             * @param features                 the additional feature bitfield
             */
            WebdPayload(Boolean usesParticipantInKey, Boolean supportsStarredMessages, Boolean supportsDocumentMessages, Boolean supportsUrlMessages, Boolean supportsMediaRetry, Boolean supportsE2EImage, Boolean supportsE2EVideo, Boolean supportsE2EAudio, Boolean supportsE2EDocument, String documentTypes, byte[] features) {
                this.usesParticipantInKey = usesParticipantInKey;
                this.supportsStarredMessages = supportsStarredMessages;
                this.supportsDocumentMessages = supportsDocumentMessages;
                this.supportsUrlMessages = supportsUrlMessages;
                this.supportsMediaRetry = supportsMediaRetry;
                this.supportsE2EImage = supportsE2EImage;
                this.supportsE2EVideo = supportsE2EVideo;
                this.supportsE2EAudio = supportsE2EAudio;
                this.supportsE2EDocument = supportsE2EDocument;
                this.documentTypes = documentTypes;
                this.features = features;
            }

            /**
             * Returns whether participant scoped keys are in use.
             *
             * @return {@code true} when the flag is set, otherwise {@code false}
             */
            public boolean usesParticipantInKey() {
                return usesParticipantInKey != null && usesParticipantInKey;
            }

            /**
             * Returns whether starred messages are supported.
             *
             * @return {@code true} when the flag is set, otherwise {@code false}
             */
            public boolean supportsStarredMessages() {
                return supportsStarredMessages != null && supportsStarredMessages;
            }

            /**
             * Returns whether document messages are supported.
             *
             * @return {@code true} when the flag is set, otherwise {@code false}
             */
            public boolean supportsDocumentMessages() {
                return supportsDocumentMessages != null && supportsDocumentMessages;
            }

            /**
             * Returns whether URL preview messages are supported.
             *
             * @return {@code true} when the flag is set, otherwise {@code false}
             */
            public boolean supportsUrlMessages() {
                return supportsUrlMessages != null && supportsUrlMessages;
            }

            /**
             * Returns whether media retry is supported.
             *
             * @return {@code true} when the flag is set, otherwise {@code false}
             */
            public boolean supportsMediaRetry() {
                return supportsMediaRetry != null && supportsMediaRetry;
            }

            /**
             * Returns whether E2E encrypted images are supported.
             *
             * @return {@code true} when the flag is set, otherwise {@code false}
             */
            public boolean supportsE2EImage() {
                return supportsE2EImage != null && supportsE2EImage;
            }

            /**
             * Returns whether E2E encrypted video is supported.
             *
             * @return {@code true} when the flag is set, otherwise {@code false}
             */
            public boolean supportsE2EVideo() {
                return supportsE2EVideo != null && supportsE2EVideo;
            }

            /**
             * Returns whether E2E encrypted audio is supported.
             *
             * @return {@code true} when the flag is set, otherwise {@code false}
             */
            public boolean supportsE2EAudio() {
                return supportsE2EAudio != null && supportsE2EAudio;
            }

            /**
             * Returns whether E2E encrypted documents are supported.
             *
             * @return {@code true} when the flag is set, otherwise {@code false}
             */
            public boolean supportsE2EDocument() {
                return supportsE2EDocument != null && supportsE2EDocument;
            }

            /**
             * Returns the accepted document MIME types.
             *
             * @return the document types string, or {@link Optional#empty()} when
             *         absent
             */
            public Optional<String> documentTypes() {
                return Optional.ofNullable(documentTypes);
            }

            /**
             * Returns the opaque binary feature bitfield.
             *
             * @return the features bytes, or {@link Optional#empty()} when absent
             */
            public Optional<byte[]> features() {
                return Optional.ofNullable(features);
            }

            /**
             * Replaces the participant scoped keys flag.
             *
             * @param usesParticipantInKey the new flag value, or {@code null} to
             *                             clear it
             */
            public void setUsesParticipantInKey(Boolean usesParticipantInKey) {
                this.usesParticipantInKey = usesParticipantInKey;
    }

            /**
             * Replaces the starred messages flag.
             *
             * @param supportsStarredMessages the new flag value, or {@code null} to
             *                                clear it
             */
            public void setSupportsStarredMessages(Boolean supportsStarredMessages) {
                this.supportsStarredMessages = supportsStarredMessages;
    }

            /**
             * Replaces the document message support flag.
             *
             * @param supportsDocumentMessages the new flag value, or {@code null} to
             *                                 clear it
             */
            public void setSupportsDocumentMessages(Boolean supportsDocumentMessages) {
                this.supportsDocumentMessages = supportsDocumentMessages;
    }

            /**
             * Replaces the URL preview support flag.
             *
             * @param supportsUrlMessages the new flag value, or {@code null} to clear
             *                            it
             */
            public void setSupportsUrlMessages(Boolean supportsUrlMessages) {
                this.supportsUrlMessages = supportsUrlMessages;
    }

            /**
             * Replaces the media retry support flag.
             *
             * @param supportsMediaRetry the new flag value, or {@code null} to clear
             *                           it
             */
            public void setSupportsMediaRetry(Boolean supportsMediaRetry) {
                this.supportsMediaRetry = supportsMediaRetry;
    }

            /**
             * Replaces the E2E image support flag.
             *
             * @param supportsE2EImage the new flag value, or {@code null} to clear it
             */
            public void setSupportsE2EImage(Boolean supportsE2EImage) {
                this.supportsE2EImage = supportsE2EImage;
    }

            /**
             * Replaces the E2E video support flag.
             *
             * @param supportsE2EVideo the new flag value, or {@code null} to clear it
             */
            public void setSupportsE2EVideo(Boolean supportsE2EVideo) {
                this.supportsE2EVideo = supportsE2EVideo;
    }

            /**
             * Replaces the E2E audio support flag.
             *
             * @param supportsE2EAudio the new flag value, or {@code null} to clear it
             */
            public void setSupportsE2EAudio(Boolean supportsE2EAudio) {
                this.supportsE2EAudio = supportsE2EAudio;
    }

            /**
             * Replaces the E2E document support flag.
             *
             * @param supportsE2EDocument the new flag value, or {@code null} to clear
             *                            it
             */
            public void setSupportsE2EDocument(Boolean supportsE2EDocument) {
                this.supportsE2EDocument = supportsE2EDocument;
    }

            /**
             * Replaces the accepted document MIME types string.
             *
             * @param documentTypes the new document types string, or {@code null} to
             *                      clear it
             */
            public void setDocumentTypes(String documentTypes) {
                this.documentTypes = documentTypes;
    }

            /**
             * Replaces the opaque binary feature bitfield.
             *
             * @param features the new features bytes, or {@code null} to clear it
             */
            public void setFeatures(byte[] features) {
                this.features = features;
    }
        }
    }
}
