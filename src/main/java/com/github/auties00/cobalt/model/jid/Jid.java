package com.github.auties00.cobalt.model.jid;

import com.github.auties00.cobalt.exception.MalformedJidException;
import com.github.auties00.libsignal.SignalProtocolAddress;
import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;
import it.auties.protobuf.model.ProtobufString;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.github.auties00.cobalt.model.jid.JidConstants.*;

/**
 * A sealed interface representing a WhatsApp JID (Jabber ID).
 * Each JID type is represented by a specific record implementation,
 * enabling exhaustive pattern matching.
 * <p>
 * Example usage with pattern matching:
 * <pre>{@code
 * switch (jid) {
 *     case Jid.PhoneUser pu -> handlePhoneUser(pu.user());
 *     case Jid.LidUser lu -> handleLidUser(lu.user());
 *     case Jid.Group g -> handleGroup(g.user());
 *     default -> handleOther(jid);
 * }
 * }</pre>
 */
public sealed interface Jid extends JidProvider permits
        Jid.PhoneUser, Jid.PhoneDevice, Jid.LidUser, Jid.LidDevice,
        Jid.MsgrUser, Jid.MsgrDevice, Jid.InteropUser, Jid.InteropDevice,
        Jid.Group, Jid.Broadcast, Jid.Status, Jid.Call, Jid.Newsletter,
        Jid.Bot, Jid.Hosted, Jid.HostedLid, Jid.Server, Jid.Unknown {
    String user();
    JidServer server();
    int device();
    int agent();

    default boolean hasUser() {
        return user() != null;
    }

    default boolean hasDevice() {
        return device() != 0;
    }

    default boolean hasAgent() {
        return agent() != 0;
    }

    default boolean hasServer(JidServer server) {
        return this.server().equals(server);
    }

    default boolean isServerJid(JidServer server) {
        return user() == null && this.server().equals(server);
    }

    default boolean isLid() {
        return isLidUser() || isLidDevice();
    }

    default boolean isLidUser() {
        return this instanceof LidUser;
    }

    default boolean isLidDevice() {
        return this instanceof LidDevice;
    }

    default boolean isPhone() {
        return isPhoneUser() || isPhoneDevice();
    }

    private boolean isPhoneUser() {
        return this instanceof PhoneUser;
    }

    private boolean isPhoneDevice() {
        return this instanceof PhoneDevice;
    }

    default boolean isRegularUser() {
        return isPhone() || isLid();
    }

    default boolean isGroup() {
        return this instanceof Group;
    }

    default boolean isBroadcast() {
        return this instanceof Broadcast || this instanceof Status;
    }

    default boolean isStatus() {
        return this instanceof Status;
    }

    default boolean isNewsletter() {
        return this instanceof Newsletter;
    }

    default boolean isBot() {
        return this instanceof Bot;
    }

    default boolean isCall() {
        return this instanceof Call;
    }

    default boolean isMsgr() {
        return isMsgrUser() || isMsgrDevice();
    }

    private boolean isMsgrUser() {
        return this instanceof MsgrUser;
    }

    private boolean isMsgrDevice() {
        return this instanceof MsgrDevice;
    }

    default boolean isInterop() {
        return isInteropUser() || isInteropDevice();
    }

    private boolean isInteropUser() {
        return this instanceof InteropUser;
    }

    private boolean isInteropDevice() {
        return this instanceof InteropDevice;
    }

    default boolean isHosted() {
        return this instanceof Hosted;
    }

    default boolean isHostedLid() {
        return this instanceof HostedLid;
    }

    default boolean isChat() {
        return isRegularUser() || isMsgr() || isInterop() || isGroup();
    }

    default boolean isUserType() {
        return isPhoneUser()
               || isLidUser()
               || isMsgr()
               || isInteropUser();
    }

    default boolean isDeviceType() {
        return isPhoneDevice()
               || isLidDevice()
               || isMsgrDevice()
               || isInteropDevice()
               || isHosted()
               || isHostedLid();
    }

    // ==================== Transformation Methods ====================

    default Jid withServer(JidServer server) {
        if (Objects.equals(this.server(), server)) {
            return this;
        }
        return createJid(user(), server, device(), agent());
    }

    default Jid withAgent(int agent) {
        if (this.agent() == agent) {
            return this;
        }
        return createJid(user(), server(), device(), agent);
    }

    default Jid withDevice(int device) {
        if (this.device() == device) {
            return this;
        }
        return createJid(user(), server(), device, agent());
    }

    default Jid withoutData() {
        if (!hasDevice() && !hasAgent()) {
            return this;
        }
        return createJid(user(), server(), 0, 0);
    }

    /**
     * Converts this JID to a user JID, normalizing hosted servers to their regular counterparts
     * and removing device/agent information.
     */
    default Jid toUserJid() {
        var server = server();
        if (server.equals(JidServer.hosted())) {
            return Jid.of(user(), JidServer.user());
        }
        if (server.equals(JidServer.hostedLid())) {
            return Jid.of(user(), JidServer.lid());
        }
        return withoutData();
    }

    default Optional<String> toPhoneNumber() {
        var user = user();
        if (user == null) {
            return Optional.empty();
        }
        for (var i = 0; i < user.length(); i++) {
            if (!Character.isDigit(user.charAt(i))) {
                return Optional.empty();
            }
        }
        return Optional.of('+' + user);
    }

    default SignalProtocolAddress toSignalAddress() {
        return new SignalProtocolAddress(user(), device());
    }

    @Override
    default Jid toJid() {
        return this;
    }

    // ==================== Record Implementations ====================

    /**
     * Phone number-based user JID (e.g., 1234567890@s.whatsapp.net)
     * User must be "0" or 5-20 digits not starting with "10"
     */
    record PhoneUser(String user, JidServer server, int agent) implements Jid {
        private static final PhoneUser OFFICIAL_SURVEYS_ACCOUNT = new PhoneUser("16505361212", JidServer.user());
        private static final PhoneUser OFFICIAL_BUSINESS_ACCOUNT = new PhoneUser("16505361212", JidServer.legacyUser());
        private static final PhoneUser ANNOUNCEMENTS = new PhoneUser("0", JidServer.user());

        public PhoneUser {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid phone user: " + user);
            }
            checkUnsignedByte(agent);
        }

        public PhoneUser(String user, JidServer server) {
            this(user, server, 0);
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length == 0) return false;
            if (length == 1) return user.charAt(0) == '0';
            if (length < 5 || length > 20) return false;
            if (user.charAt(0) == '1' && user.charAt(1) == '0') return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public int device() {
            return 0;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * Phone number-based device JID (e.g., 1234567890:1@s.whatsapp.net)
     * User must be "0" or 5-20 digits not starting with "10", device must be > 0
     */
    record PhoneDevice(String user, JidServer server, int device, int agent) implements Jid {
        public PhoneDevice {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid phone user: " + user);
            }
            if (device <= 0) {
                throw new MalformedJidException("PhoneDevice requires device > 0");
            }
            checkUnsignedByte(device);
            checkUnsignedByte(agent);
        }

        public PhoneDevice(String user, JidServer server, int device) {
            this(user, server, device, 0);
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length == 0) return false;
            if (length == 1) return user.charAt(0) == '0';
            if (length < 5 || length > 20) return false;
            if (user.charAt(0) == '1' && user.charAt(1) == '0') return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * LID-based user JID (e.g., 123456789012345@lid)
     * User must be 1-15 digits starting with 1-9
     */
    record LidUser(String user, JidServer server, int agent) implements Jid {
        public LidUser {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid LID user: " + user);
            }
            checkUnsignedByte(agent);
        }

        public LidUser(String user, JidServer server) {
            this(user, server, 0);
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length == 0 || length > 15) return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public int device() {
            return 0;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * LID-based device JID (e.g., 123456789012345:1@lid)
     * User must be 1-15 digits starting with 1-9, device must be > 0
     */
    record LidDevice(String user, JidServer server, int device, int agent) implements Jid {
        public LidDevice {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid LID user: " + user);
            }
            if (device <= 0) {
                throw new MalformedJidException("LidDevice requires device > 0");
            }
            checkUnsignedByte(device);
            checkUnsignedByte(agent);
        }

        public LidDevice(String user, JidServer server, int device) {
            this(user, server, device, 0);
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length == 0 || length > 15) return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * Messenger user JID (e.g., 1234567890@msgr)
     * User must be 1-20 digits starting with 1-9
     */
    record MsgrUser(String user, JidServer server, int agent) implements Jid {
        public MsgrUser {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid MSGR user: " + user);
            }
            checkUnsignedByte(agent);
        }

        public MsgrUser(String user, JidServer server) {
            this(user, server, 0);
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length == 0 || length > 20) return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public int device() {
            return 0;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * Messenger device JID (e.g., 1234567890:1@msgr)
     * User must be 1-20 digits starting with 1-9, device must be > 0
     */
    record MsgrDevice(String user, JidServer server, int device, int agent) implements Jid {
        public MsgrDevice {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid MSGR user: " + user);
            }
            if (device <= 0) {
                throw new MalformedJidException("MsgrDevice requires device > 0");
            }
            checkUnsignedByte(device);
            checkUnsignedByte(agent);
        }

        public MsgrDevice(String user, JidServer server, int device) {
            this(user, server, device, 0);
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length == 0 || length > 20) return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * Interop user JID (e.g., 1-234567890@interop)
     * User must be countryCode-userId format (1-3 digits, dash, 1-15 digits)
     */
    record InteropUser(String user, JidServer server, int agent) implements Jid {
        public InteropUser {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid interop user: " + user);
            }
            checkUnsignedByte(agent);
        }

        public InteropUser(String user, JidServer server) {
            this(user, server, 0);
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length < 3) return false;
            var dashIndex = -1;
            for (var i = 0; i < length; i++) {
                if (user.charAt(i) == '-') {
                    dashIndex = i;
                    break;
                }
            }
            if (dashIndex == -1 || dashIndex == 0 || dashIndex > 3) return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < dashIndex; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            var userIdLength = length - dashIndex - 1;
            if (userIdLength == 0 || userIdLength > 15) return false;
            var userIdFirst = user.charAt(dashIndex + 1);
            if (userIdFirst < '1' || userIdFirst > '9') return false;
            for (var i = dashIndex + 2; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public int device() {
            return 0;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * Interop device JID (e.g., 1-234567890:0@interop)
     * User must be countryCode-userId format, device >= 0
     */
    record InteropDevice(String user, JidServer server, int device, int agent) implements Jid {
        public InteropDevice {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid interop user: " + user);
            }
            checkUnsignedByte(device);
            checkUnsignedByte(agent);
        }

        public InteropDevice(String user, JidServer server, int device) {
            this(user, server, device, 0);
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length < 3) return false;
            var dashIndex = -1;
            for (var i = 0; i < length; i++) {
                if (user.charAt(i) == '-') {
                    dashIndex = i;
                    break;
                }
            }
            if (dashIndex == -1 || dashIndex == 0 || dashIndex > 3) return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < dashIndex; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            var userIdLength = length - dashIndex - 1;
            if (userIdLength == 0 || userIdLength > 15) return false;
            var userIdFirst = user.charAt(dashIndex + 1);
            if (userIdFirst < '1' || userIdFirst > '9') return false;
            for (var i = dashIndex + 2; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * Group JID (e.g., 123456789-1234567890@g.us)
     * User must be numeric (1-20 digits) or phone-timestamp format
     */
    record Group(String user, JidServer server) implements Jid {
        public Group {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid group ID: " + user);
            }
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length == 0) return false;
            var dashIndex = -1;
            for (var i = 0; i < length; i++) {
                if (user.charAt(i) == '-') {
                    dashIndex = i;
                    break;
                }
            }
            if (dashIndex != -1) {
                var timestampLength = length - dashIndex - 1;
                if (dashIndex < 5 || dashIndex > 20 || timestampLength != 10) return false;
                if (user.charAt(0) == '1' && user.charAt(1) == '0') return false;
                var first = user.charAt(0);
                if (first < '1' || first > '9') return false;
                for (var i = 1; i < dashIndex; i++) {
                    var c = user.charAt(i);
                    if (c < '0' || c > '9') return false;
                }
                var tsFirst = user.charAt(dashIndex + 1);
                if (tsFirst < '1' || tsFirst > '9') return false;
                for (var i = dashIndex + 2; i < length; i++) {
                    var c = user.charAt(i);
                    if (c < '0' || c > '9') return false;
                }
                return true;
            }
            if (length > 20) return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public int device() {
            return 0;
        }

        @Override
        public int agent() {
            return 0;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * Broadcast JID (e.g., location@broadcast or 1234567890@broadcast)
     * User must be "location" or 1-20 digits
     */
    record Broadcast(String user, JidServer server) implements Jid {
        private static final Broadcast LOCATION_BROADCAST = new Broadcast("location", JidServer.broadcast());

        public Broadcast {
            Objects.requireNonNull(server, "Server cannot be null");
            if (user != null && !isValidUser(user)) {
                throw new MalformedJidException("Invalid broadcast ID: " + user);
            }
        }

        private static boolean isValidUser(String user) {
            if ("location".equals(user)) return true;
            var length = user.length();
            if (length == 0 || length > 20) return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public int device() {
            return 0;
        }

        @Override
        public int agent() {
            return 0;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * Status JID (status@broadcast)
     * User is always "status"
     */
    record Status(JidServer server) implements Jid {
        private static final Status STATUS_BROADCAST = new Status(JidServer.broadcast());

        private static final String STATUS_USER = "status";

        public Status {
            Objects.requireNonNull(server, "Server cannot be null");
        }

        @Override
        public String user() {
            return STATUS_USER;
        }

        @Override
        public int device() {
            return 0;
        }

        @Override
        public int agent() {
            return 0;
        }

        @Override
        public String toString() {
            return STATUS_USER + "@" + server.toString();
        }
    }

    /**
     * Call JID (e.g., 1234ABCD5678EFGH@call)
     * User must be 18-32 hex characters
     */
    record Call(String user, JidServer server) implements Jid {
        public Call {
            Objects.requireNonNull(server, "Server cannot be null");
            if (user != null && !isValidUser(user)) {
                throw new MalformedJidException("Invalid call ID: " + user);
            }
        }

        private static boolean isValidUser(String user) {
            var length = user.length();
            if (length < 18 || length > 32) return false;
            for (var i = 0; i < length; i++) {
                var c = user.charAt(i);
                if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int device() {
            return 0;
        }

        @Override
        public int agent() {
            return 0;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * Newsletter JID (e.g., 1234567890@newsletter)
     * User must be 1-20 digits starting with 1-9
     */
    record Newsletter(String user, JidServer server) implements Jid {
        public Newsletter {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid newsletter ID: " + user);
            }
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length == 0 || length > 20) return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public int device() {
            return 0;
        }

        @Override
        public int agent() {
            return 0;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * Bot JID (e.g., 1234567890@bot)
     * User must be 1-20 digits starting with 1-9
     */
    record Bot(String user, JidServer server) implements Jid {
        public Bot {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid bot ID: " + user);
            }
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length == 0 || length > 20) return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public int device() {
            return 0;
        }

        @Override
        public int agent() {
            return 0;
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    /**
     * Hosted device JID (e.g., 1234567890:99@hosted)
     * User must be a valid phone number, device is always 99
     */
    record Hosted(String user, JidServer server) implements Jid {
        private static final int HOSTED_DEVICE = 99;

        public Hosted {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid hosted user: " + user);
            }
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length == 0) return false;
            if (length == 1) return user.charAt(0) == '0';
            if (length < 5 || length > 20) return false;
            if (user.charAt(0) == '1' && user.charAt(1) == '0') return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public int device() {
            return HOSTED_DEVICE;
        }

        @Override
        public int agent() {
            return 0;
        }

        @Override
        public String toString() {
            return user + ":" + HOSTED_DEVICE + "@" + server.toString();
        }
    }

    /**
     * Hosted LID device JID (e.g., 123456789012345:99@hosted.lid)
     * User must be a valid LID, device is always 99
     */
    record HostedLid(String user, JidServer server) implements Jid {
        private static final int HOSTED_DEVICE = 99;

        public HostedLid {
            Objects.requireNonNull(server, "Server cannot be null");
            if (!isValidUser(user)) {
                throw new MalformedJidException("Invalid hosted LID user: " + user);
            }
        }

        private static boolean isValidUser(String user) {
            if (user == null) return false;
            var length = user.length();
            if (length == 0 || length > 15) return false;
            var first = user.charAt(0);
            if (first < '1' || first > '9') return false;
            for (var i = 1; i < length; i++) {
                var c = user.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public int device() {
            return HOSTED_DEVICE;
        }

        @Override
        public int agent() {
            return 0;
        }

        @Override
        public String toString() {
            return user + ":" + HOSTED_DEVICE + "@" + server.toString();
        }
    }

    /**
     * Server-only JID (e.g., @s.whatsapp.net)
     */
    record Server(JidServer server) implements Jid {
        private static final ConcurrentMap<JidServer, Server> CACHE = new ConcurrentHashMap<>();

        private static final Server LEGACY_USER_SERVER = new Server(JidServer.legacyUser());
        private static final Server GROUP_OR_COMMUNITY_SERVER = new Server(JidServer.groupOrCommunity());
        private static final Server BROADCAST_SERVER = new Server(JidServer.broadcast());
        private static final Server CALL_SERVER = new Server(JidServer.call());
        private static final Server USER_SERVER = new Server(JidServer.user());
        private static final Server LID_SERVER = new Server(JidServer.lid());
        private static final Server NEWSLETTER_SERVER = new Server(JidServer.newsletter());
        private static final Server BOT_SERVER = new Server(JidServer.bot());
        private static final Server HOSTED_SERVER = new Server(JidServer.hosted());
        private static final Server HOSTED_LID_SERVER = new Server(JidServer.hostedLid());
        private static final Server MSGR_SERVER = new Server(JidServer.msgr());
        private static final Server INTEROP_SERVER = new Server(JidServer.interop());
        
        public Server {
            Objects.requireNonNull(server, "Server cannot be null");
        }

        public static Server of(JidServer server) {
            return CACHE.computeIfAbsent(server, Server::new);
        }

        @Override
        public String user() {
            return null;
        }

        @Override
        public int device() {
            return 0;
        }

        @Override
        public int agent() {
            return 0;
        }

        @Override
        public String toString() {
            return server.toString();
        }
    }

    /**
     * Unknown or unrecognized JID type - accepts any values
     */
    record Unknown(String user, JidServer server, int device, int agent) implements Jid {
        public Unknown {
            Objects.requireNonNull(server, "Server cannot be null");
            checkUnsignedByte(device);
            checkUnsignedByte(agent);
        }

        @Override
        public String toString() {
            return Jid.toString(this);
        }
    }

    static Jid legacyUserServer() {
        return Server.LEGACY_USER_SERVER;
    }

    static Jid groupOrCommunityServer() {
        return Server.GROUP_OR_COMMUNITY_SERVER;
    }

    static Jid broadcastServer() {
        return Server.BROADCAST_SERVER;
    }

    static Jid callServer() {
        return Server.CALL_SERVER;
    }

    static Jid userServer() {
        return Server.USER_SERVER;
    }

    static Jid lidServer() {
        return Server.LID_SERVER;
    }

    static Jid newsletterServer() {
        return Server.NEWSLETTER_SERVER;
    }

    static Jid botServer() {
        return Server.BOT_SERVER;
    }

    static Jid hostedServer() {
        return Server.HOSTED_SERVER;
    }

    static Jid hostedLidServer() {
        return Server.HOSTED_LID_SERVER;
    }

    static Jid msgrServer() {
        return Server.MSGR_SERVER;
    }

    static Jid interopServer() {
        return Server.INTEROP_SERVER;
    }

    static Jid officialSurveysAccount() {
        return PhoneUser.OFFICIAL_SURVEYS_ACCOUNT;
    }

    static Jid officialBusinessAccount() {
        return PhoneUser.OFFICIAL_BUSINESS_ACCOUNT;
    }

    static Jid statusBroadcastAccount() {
        return Status.STATUS_BROADCAST;
    }

    static Jid announcementsAccount() {
        return PhoneUser.ANNOUNCEMENTS;
    }

    static Jid locationBroadcast() {
        return Broadcast.LOCATION_BROADCAST;
    }

    /**
     * Creates a phone user JID from a phone number string
     */
    static Jid ofPhone(String phoneNumber) {
        Objects.requireNonNull(phoneNumber, "Phone number cannot be null");
        return Jid.of(phoneNumber, JidServer.user());
    }

    /**
     * Creates a phone user JID from a numeric phone number
     */
    static Jid ofPhone(long phoneNumber) {
        return Jid.of(phoneNumber);
    }

    /**
     * Creates a LID user JID from a LID string
     */
    static Jid ofLid(String lid) {
        Objects.requireNonNull(lid, "LID cannot be null");
        return Jid.of(lid, JidServer.lid());
    }

    /**
     * Creates a messenger user JID from a Facebook user ID
     */
    static Jid ofMsgr(String userId) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        return Jid.of(userId, JidServer.msgr());
    }

    /**
     * Creates an interop user JID from an interop user ID
     */
    static Jid ofInterop(String userId) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        return Jid.of(userId, JidServer.interop());
    }

    /**
     * Creates a group JID from a group ID
     */
    static Jid ofGroup(String groupId) {
        Objects.requireNonNull(groupId, "Group ID cannot be null");
        if (groupId.endsWith("@g.us")) {
            return Jid.of(groupId);
        }
        return Jid.of(groupId, JidServer.groupOrCommunity());
    }

    /**
     * Creates a newsletter JID from a newsletter ID
     */
    static Jid ofNewsletter(String id) {
        Objects.requireNonNull(id, "Newsletter ID cannot be null");
        if (id.endsWith("@newsletter")) {
            return Jid.of(id);
        }
        return Jid.of(id, JidServer.newsletter());
    }

    /**
     * Creates a bot JID from a bot ID
     */
    static Jid ofBot(String id) {
        Objects.requireNonNull(id, "Bot ID cannot be null");
        return Jid.of(id, JidServer.bot());
    }

    @ProtobufSerializer
    default String toProtobufString() {
        return toString();
    }

    static Jid of(String user, JidServer server, int device, int agent) {
        Objects.requireNonNull(server, "Server cannot be null");
        if (user == null) {
            return of(server);
        }
        var cleanUser = cleanUser(user);
        return createJid(cleanUser, server, device, agent);
    }

    static Jid of(JidServer server) {
        Objects.requireNonNull(server, "Server cannot be null");
        return switch (server.type()) {
            case UNKNOWN -> Server.of(server);
            case LEGACY_USER -> legacyUserServer();
            case GROUP_OR_COMMUNITY -> groupOrCommunityServer();
            case BROADCAST -> broadcastServer();
            case CALL -> callServer();
            case USER -> userServer();
            case LID -> lidServer();
            case NEWSLETTER -> newsletterServer();
            case BOT -> botServer();
            case HOSTED -> hostedServer();
            case HOSTED_LID -> hostedLidServer();
            case MSGR -> msgrServer();
            case INTEROP -> interopServer();
        };
    }

    static Jid of(long jid) {
        if (jid < 0) {
            throw new MalformedJidException("value cannot be negative");
        }
        return new PhoneUser(String.valueOf(jid), JidServer.user());
    }

    static Jid of(String jid) {
        if (jid == null) {
            return null;
        }
        var knownServer = JidServer.of(jid, false);
        if (knownServer != null) {
            return of(knownServer);
        }
        var serverSeparatorIndex = jid.indexOf(SERVER_CHAR);
        var server = serverSeparatorIndex == -1
                ? JidServer.user()
                : JidServer.of(jid, serverSeparatorIndex + 1, jid.length() - serverSeparatorIndex - 1);
        return parseJid(jid, serverSeparatorIndex, server);
    }

    static Jid of(String user, JidServer server) {
        Objects.requireNonNull(server, "Server cannot be null");
        return parseJid(user, user.indexOf(SERVER_CHAR), server);
    }

    @ProtobufDeserializer
    static Jid of(ProtobufString jid) {
        return switch (jid) {
            case ProtobufString.Lazy lazy -> Jid.of(lazy);
            case ProtobufString.Value value -> Jid.of(value.toString());
            case null -> null;
        };
    }

    static Jid of(ProtobufString.Lazy jid) {
        if (jid == null) {
            return null;
        }
        var source = jid.encodedBytes();
        var offset = jid.encodedOffset();
        var length = jid.encodedLength();
        var knownServer = JidServer.of(source, offset, length, false);
        if (knownServer != null) {
            return of(knownServer);
        }

        enum ParserState { USER, DEVICE, AGENT }

        var state = ParserState.USER;
        var userLength = length;
        var agent = 0;
        var device = 0;
        var server = JidServer.user();
        for (var parserPosition = 0; parserPosition < length; parserPosition++) {
            var token = (char) (source[offset + parserPosition] & 0x7F);
            if (token == SERVER_CHAR) {
                if (state == ParserState.USER) {
                    userLength = parserPosition;
                }
                server = JidServer.of(source, offset + parserPosition + 1, length - parserPosition - 1, true);
                break;
            }
            switch (state) {
                case USER -> {
                    if (token == DEVICE_CHAR) {
                        userLength = parserPosition;
                        state = ParserState.DEVICE;
                    } else if (token == AGENT_CHAR) {
                        userLength = parserPosition;
                        state = ParserState.AGENT;
                    }
                }
                case DEVICE -> {
                    if (token == AGENT_CHAR) {
                        if (agent != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                        }
                        state = ParserState.AGENT;
                    } else if (Character.isDigit(token)) {
                        device = device * 10 + (token - '0');
                    } else {
                        var value = new String(source, offset, length, StandardCharsets.US_ASCII);
                        throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + value + "'");
                    }
                }
                case AGENT -> {
                    if (token == DEVICE_CHAR) {
                        if (device != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                        }
                        state = ParserState.DEVICE;
                    } else if (Character.isDigit(token)) {
                        agent = agent * 10 + (token - '0');
                    } else {
                        var value = new String(source, offset, length, StandardCharsets.US_ASCII);
                        throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + value + "'");
                    }
                }
            }
        }
        var user = new String(source, offset, userLength, StandardCharsets.UTF_8);
        assert server != null;
        return createJid(user, server, device, agent);
    }

    private static void checkUnsignedByte(int i) {
        if (i < 0 || i > 255) {
            throw new MalformedJidException(i + " is not an unsigned byte");
        }
    }

    private static String cleanUser(String user) {
        if (user == null || user.isEmpty()) {
            return user;
        }
        var length = user.length();
        var offset = user.charAt(0) == PHONE_CHAR ? 1 : 0;
        for (var i = offset; i < length; i++) {
            var token = user.charAt(i);
            if (token == SERVER_CHAR || token == DEVICE_CHAR || token == AGENT_CHAR) {
                return user.substring(offset, i);
            }
        }
        return offset == 0 ? user : user.substring(offset);
    }

    private static Jid parseJid(String jid, int jidLength, JidServer server) {
        var length = jidLength == -1 ? jid.length() : jidLength;
        if (length == 0) {
            return of(server);
        }
        var offset = jid.charAt(0) == PHONE_CHAR ? 1 : 0;
        if (offset >= length) {
            throw new MalformedJidException("Malformed value '" + jid + "'");
        }

        enum ParserState { USER, DEVICE, AGENT }

        var state = ParserState.USER;
        var userLength = length;
        var agent = 0;
        var device = 0;
        for (var parserPosition = 0; parserPosition < length; parserPosition++) {
            var token = jid.charAt(offset + parserPosition);
            if (token == SERVER_CHAR) {
                if (state == ParserState.USER) {
                    userLength = parserPosition;
                }
                server = JidServer.of(jid, offset + parserPosition + 1, length - parserPosition - 1);
                break;
            }
            switch (state) {
                case USER -> {
                    if (token == DEVICE_CHAR) {
                        userLength = parserPosition;
                        state = ParserState.DEVICE;
                    } else if (token == AGENT_CHAR) {
                        userLength = parserPosition;
                        state = ParserState.AGENT;
                    }
                }
                case DEVICE -> {
                    if (token == AGENT_CHAR) {
                        if (agent != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                        }
                        state = ParserState.AGENT;
                    } else if (Character.isDigit(token)) {
                        device = device * 10 + (token - '0');
                    } else {
                        throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                    }
                }
                case AGENT -> {
                    if (token == DEVICE_CHAR) {
                        if (device != 0) {
                            throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                        }
                        state = ParserState.DEVICE;
                    } else if (Character.isDigit(token)) {
                        agent = agent * 10 + (token - '0');
                    } else {
                        throw new MalformedJidException("Encountered unexpected token '" + token + "' while parsing value '" + jid + "'");
                    }
                }
            }
        }
        var user = jid.substring(offset, offset + userLength);
        return createJid(user, server, device, agent);
    }

    private static Jid createJid(String user, JidServer server, int device, int agent) {
        var serverType = server.type();
        var hasDevice = device != 0;
        return switch (serverType) {
            case USER, LEGACY_USER -> {
                if (user == null || user.isEmpty()) {
                    yield new Server(server);
                }
                yield hasDevice
                        ? new PhoneDevice(user, server, device, agent)
                        : new PhoneUser(user, server, agent);
            }
            case LID -> {
                if (user == null || user.isEmpty()) {
                    yield new Server(server);
                }
                yield hasDevice
                        ? new LidDevice(user, server, device, agent)
                        : new LidUser(user, server, agent);
            }
            case MSGR -> {
                if (user == null || user.isEmpty()) {
                    yield new Server(server);
                }
                yield hasDevice
                        ? new MsgrDevice(user, server, device, agent)
                        : new MsgrUser(user, server, agent);
            }
            case INTEROP -> {
                if (user == null || user.isEmpty()) {
                    yield new Server(server);
                }
                yield hasDevice
                        ? new InteropDevice(user, server, device, agent)
                        : new InteropUser(user, server, agent);
            }
            case GROUP_OR_COMMUNITY -> {
                if (user == null || user.isEmpty()) {
                    yield new Server(server);
                }
                yield new Group(user, server);
            }
            case BROADCAST -> {
                if ("status".equals(user)) {
                    yield new Status(server);
                }
                yield new Broadcast(user, server);
            }
            case CALL -> new Call(user, server);
            case NEWSLETTER -> {
                if (user == null || user.isEmpty()) {
                    yield new Server(server);
                }
                yield new Newsletter(user, server);
            }
            case BOT -> {
                if (user == null || user.isEmpty()) {
                    yield new Server(server);
                }
                yield new Bot(user, server);
            }
            case HOSTED -> new Hosted(user, server);
            case HOSTED_LID -> new HostedLid(user, server);
            case UNKNOWN -> new Unknown(user, server, device, agent);
        };
    }

    private static String toString(Jid jid) {
        var hasUser = jid.hasUser();
        var hasAgent = jid.hasAgent();
        var hasDevice = jid.hasDevice();
        if (!hasUser && !hasAgent && !hasDevice) {
            return jid.server().toString();
        }
        var user = hasUser ? jid.user() : "";
        var agentStr = hasAgent ? "" + AGENT_CHAR + jid.agent() : "";
        var deviceStr = hasDevice ? "" + DEVICE_CHAR + jid.device() : "";
        return user + agentStr + deviceStr + SERVER_CHAR + jid.server().toString();
    }
}
