package com.github.auties00.cobalt.calls.engine.participant;

import com.github.auties00.cobalt.calls.signaling.session.CallCapability;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents one typed participant entry inside a group call's {@code <group_info>} roster.
 *
 * <p>A group info roster carries its members as a list of either {@code <user>} children or
 * {@code <participant>} children (never a mix); this record is the typed projection of one such entry.
 * It pins a member by its user {@link #jid() JID} and decorates it with the full identity vocabulary the
 * engine fills from the stanza: the phone number and LID forms of the address, the display username, the
 * push and guest names, the {@link #accountKind() account kind}, the country code, the
 * {@link #userType() user type}, the server projected {@link #state() membership state literal}, an
 * optional NACK {@link #error() error code} and {@link #reason() reason}, the server assigned
 * {@link #pid() participant id}, an opaque {@link #metadata() metadata} blob, and the
 * {@link #termsOfServiceNotAccepted() terms of service not accepted} flag. Below the entry sit its
 * {@link #devices() devices}, each carried as a {@link Device} with its own device JID, platform,
 * participant id, and inline {@link CallCapability capability} mask, plus the entry's optional
 * {@link #decoderCapability() video decoder capability} token and {@link #rekey() rekey} flag.
 *
 * <p>The JID is the only required component; every decoration is absent unless the relay supplied it,
 * and a NACKed entry carries no devices. The entry's {@link #childForm() child form} records whether it
 * travelled as a {@code <user>} or {@code <participant>} element so an entry that is emitted again uses
 * the same tag; both forms share this identity vocabulary.
 *
 * <p>On the wire a member is
 * {@snippet lang = xml:
 * <user jid="83116928594056@lid" state="connected" user_pn="19255550101@s.whatsapp.net">
 *   <device jid="83116928594056:2@lid" platform="web" pid="0">
 *     <capability ver="1">0105F709E4BB13</capability>
 *   </device>
 * </user>
 *}
 * with the {@code <participant>} form differing only in the element tag.
 *
 * @param childForm                  whether the entry travels as a {@code <user>} or
 *                                    {@code <participant>} element; never {@code null}
 * @param jid                        the member's user JID; never {@code null}
 * @param userPn                     the member's phone number JID, if present
 * @param userLid                    the member's LID JID, if present
 * @param username                   the member's display username, if present
 * @param pushName                   the member's push (profile) name, if present
 * @param guestName                  the member's guest display name, if present
 * @param accountKind                the member's account kind, present only when the relay supplied an
 *                                   {@code account_kind} token
 * @param countryCode                the member's country code, if present
 * @param userType                   the member's user type, present only when the relay supplied a
 *                                   classifiable {@code type} token
 * @param state                      the server projected membership state literal, if present
 * @param error                      the NACK error code, or {@code -1} when absent
 * @param reason                     the NACK reason literal, if present
 * @param pid                        the server assigned participant id, or {@code -1} when absent
 * @param metadata                   the opaque per member metadata blob, if present
 * @param termsOfServiceNotAccepted  whether the member has not accepted the group call terms of service
 * @param decoderCapability          the member's video decoder capability token, if present
 * @param rekey                      whether the member is flagged for a key rotation
 * @param devices                    the member's devices; never {@code null}, possibly empty
 * @see CallParticipantUserType
 * @see CallParticipantAccountKind
 * @see CallParticipantPlatform
 * @see CallMembership
 */
public record CallParticipantUserNode(ChildForm childForm,
                                      Jid jid,
                                      Optional<Jid> userPn,
                                      Optional<Jid> userLid,
                                      Optional<String> username,
                                      Optional<String> pushName,
                                      Optional<String> guestName,
                                      Optional<CallParticipantAccountKind> accountKind,
                                      Optional<String> countryCode,
                                      Optional<CallParticipantUserType> userType,
                                      Optional<String> state,
                                      int error,
                                      Optional<String> reason,
                                      int pid,
                                      Optional<byte[]> metadata,
                                      boolean termsOfServiceNotAccepted,
                                      Optional<String> decoderCapability,
                                      boolean rekey,
                                      List<Device> devices) {
    /**
     * The wire attribute naming the member's user JID.
     */
    private static final String JID_ATTRIBUTE = "jid";

    /**
     * The wire attribute naming the member's phone number JID.
     */
    private static final String USER_PN_ATTRIBUTE = "user_pn";

    /**
     * The wire attribute naming the member's LID JID.
     */
    private static final String USER_LID_ATTRIBUTE = "user_lid";

    /**
     * The wire attribute naming the member's display username.
     */
    private static final String USERNAME_ATTRIBUTE = "username";

    /**
     * The wire attribute naming the member's push (profile) name.
     */
    private static final String PUSH_NAME_ATTRIBUTE = "push_name";

    /**
     * The wire attribute naming the member's guest display name.
     */
    private static final String GUEST_NAME_ATTRIBUTE = "guest_name";

    /**
     * The wire attribute naming the member's account kind.
     */
    private static final String ACCOUNT_KIND_ATTRIBUTE = "account_kind";

    /**
     * The wire attribute naming the member's country code.
     */
    private static final String COUNTRY_CODE_ATTRIBUTE = "country_code";

    /**
     * The wire attribute naming the member's user type.
     */
    private static final String TYPE_ATTRIBUTE = "type";

    /**
     * The wire attribute naming the server projected membership state literal.
     */
    private static final String STATE_ATTRIBUTE = "state";

    /**
     * The wire attribute naming the NACK error code.
     */
    private static final String ERROR_ATTRIBUTE = "error";

    /**
     * The wire attribute naming the NACK reason literal.
     */
    private static final String REASON_ATTRIBUTE = "reason";

    /**
     * The wire attribute naming the server assigned participant id.
     */
    private static final String PID_ATTRIBUTE = "pid";

    /**
     * The wire attribute naming the opaque per member metadata blob.
     */
    private static final String METADATA_ATTRIBUTE = "metadata";

    /**
     * The wire attribute naming the terms of service not accepted flag.
     */
    private static final String TOS_NOT_ACCEPTED_ATTRIBUTE = "tos_not_accepted";

    /**
     * The wire attribute naming the key rotation flag.
     */
    private static final String REKEY_ATTRIBUTE = "rekey";

    /**
     * The wire element tag carrying the member's video decoder capability token.
     */
    private static final String DECODER_CAPABILITY_ELEMENT = "dec";

    /**
     * The sentinel an absent integer attribute decodes to.
     */
    private static final int ABSENT = -1;

    /**
     * Discriminates the element tag a {@link CallParticipantUserNode} travels under.
     *
     * <p>A group info roster carries its members as either {@code <user>} children or
     * {@code <participant>} children, never a mix; this enum records which tag a given entry uses so an
     * entry that is emitted again reproduces it and a decoder can select the matching children.
     */
    public enum ChildForm {
        /**
         * Marks an entry carried as a {@code <user>} element.
         */
        USER("user"),

        /**
         * Marks an entry carried as a {@code <participant>} element, the newer membership
         * representation.
         */
        PARTICIPANT("participant");

        /**
         * The wire element tag an entry of this form takes.
         */
        private final String element;

        /**
         * Constructs a child form constant bound to its wire element tag.
         *
         * @param element the wire element tag of an entry in this form
         */
        ChildForm(String element) {
            this.element = element;
        }

        /**
         * Returns the wire element tag an entry of this form takes.
         *
         * @return the entry element tag
         */
        public String element() {
            return element;
        }
    }

    /**
     * Canonicalizes the record components, defensively copying the device list and metadata blob and
     * rejecting {@code null} required components.
     *
     * <p>Each optional component is required to be a present {@link Optional} holder rather than
     * {@code null} (an absent value is {@link Optional#empty()}, never a {@code null} holder); the
     * metadata blob is cloned on the way in so the stored array cannot be mutated through the supplied
     * reference.
     *
     * @throws NullPointerException if {@code childForm}, {@code jid}, {@code devices}, any device, or any
     *                              optional holder is {@code null}
     */
    public CallParticipantUserNode {
        Objects.requireNonNull(childForm, "childForm cannot be null");
        Objects.requireNonNull(jid, "jid cannot be null");
        Objects.requireNonNull(userPn, "userPn cannot be null");
        Objects.requireNonNull(userLid, "userLid cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(pushName, "pushName cannot be null");
        Objects.requireNonNull(guestName, "guestName cannot be null");
        Objects.requireNonNull(accountKind, "accountKind cannot be null");
        Objects.requireNonNull(countryCode, "countryCode cannot be null");
        Objects.requireNonNull(userType, "userType cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        Objects.requireNonNull(metadata, "metadata cannot be null");
        Objects.requireNonNull(decoderCapability, "decoderCapability cannot be null");
        Objects.requireNonNull(devices, "devices cannot be null");
        metadata = metadata.map(byte[]::clone);
        devices = List.copyOf(devices);
    }

    /**
     * Returns a roster entry that pins a member by its user JID alone, under the {@code <user>} form.
     *
     * <p>Every decoration is left absent and the device list is empty; this is the minimal entry a
     * remove or query roster uses when it only needs to identify the member to act on.
     *
     * @param jid the member's user JID
     * @return the user form entry
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    public static CallParticipantUserNode ofUser(Jid jid) {
        return new CallParticipantUserNode(ChildForm.USER, jid, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), ABSENT, Optional.empty(), ABSENT, Optional.empty(),
                false, Optional.empty(), false, List.of());
    }

    /**
     * Returns a roster entry that pins a member by its user JID and enumerates its devices, under the
     * {@code <user>} form.
     *
     * <p>Every identity decoration other than the device list is left absent; this is the entry an outbound
     * group {@code <offer>} rosters, where each member is identified by its user JID and its known device
     * JIDs, and the local (self) member's device additionally carries the inline capability advertisement.
     *
     * @param jid     the member's user JID
     * @param devices the member's devices
     * @return the user form entry carrying the given devices
     * @throws NullPointerException if {@code jid} or {@code devices} is {@code null}, or any device is
     *                              {@code null}
     */
    public static CallParticipantUserNode ofUserDevices(Jid jid, List<Device> devices) {
        return new CallParticipantUserNode(ChildForm.USER, jid, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), ABSENT, Optional.empty(), ABSENT, Optional.empty(),
                false, Optional.empty(), false, devices);
    }

    /**
     * Returns the opaque per member metadata blob backing this entry, if present.
     *
     * <p>This accessor overrides the implicit record accessor to defensively copy the array so the
     * stored blob cannot be mutated through the returned reference.
     *
     * @return an {@link Optional} holding a copy of the metadata bytes, or empty when absent
     */
    @Override
    public Optional<byte[]> metadata() {
        return metadata.map(byte[]::clone);
    }

    /**
     * Returns the NACK error code, if present.
     *
     * @return an {@link OptionalInt} holding the {@code error} value, or empty when absent
     */
    public OptionalInt errorValue() {
        return error == ABSENT ? OptionalInt.empty() : OptionalInt.of(error);
    }

    /**
     * Returns the server assigned participant id, if present.
     *
     * @return an {@link OptionalInt} holding the {@code pid} value, or empty when absent
     */
    public OptionalInt pidValue() {
        return pid == ABSENT ? OptionalInt.empty() : OptionalInt.of(pid);
    }

    /**
     * Builds the {@code <user>} or {@code <participant>} roster entry stanza.
     *
     * <p>The element tag is taken from the {@link #childForm() child form}; each optional attribute is
     * omitted when its backing component is absent rather than written as a sentinel; the
     * {@code account_kind} and {@code type} tokens are written from their enum tokens; the boolean
     * {@code tos_not_accepted} and {@code rekey} flags are written only when {@code true}; and the
     * devices and the optional {@code <dec>} video decoder token are emitted as children. An entry with
     * no children produces an element with only its attributes.
     *
     * @return the roster entry stanza
     */
    public Stanza toNode() {
        var children = new ArrayList<Stanza>();
        for (var device : devices) {
            children.add(device.toNode());
        }
        decoderCapability.ifPresent(token -> children.add(new StanzaBuilder()
                .description(DECODER_CAPABILITY_ELEMENT)
                .content(token)
                .build()));
        var builder = new StanzaBuilder()
                .description(childForm.element())
                .attribute(JID_ATTRIBUTE, jid)
                .attribute(USER_PN_ATTRIBUTE, userPn.orElse(null), userPn.isPresent())
                .attribute(USER_LID_ATTRIBUTE, userLid.orElse(null), userLid.isPresent())
                .attribute(USERNAME_ATTRIBUTE, username.orElse(null), username.isPresent())
                .attribute(PUSH_NAME_ATTRIBUTE, pushName.orElse(null), pushName.isPresent())
                .attribute(GUEST_NAME_ATTRIBUTE, guestName.orElse(null), guestName.isPresent())
                .attribute(ACCOUNT_KIND_ATTRIBUTE, accountKind.map(CallParticipantAccountKind::token).orElse(null), accountKind.isPresent())
                .attribute(COUNTRY_CODE_ATTRIBUTE, countryCode.orElse(null), countryCode.isPresent())
                .attribute(TYPE_ATTRIBUTE, userType.map(CallParticipantUserType::token).orElse(null), userType.isPresent())
                .attribute(STATE_ATTRIBUTE, state.orElse(null), state.isPresent())
                .attribute(ERROR_ATTRIBUTE, error, error != ABSENT)
                .attribute(REASON_ATTRIBUTE, reason.orElse(null), reason.isPresent())
                .attribute(PID_ATTRIBUTE, pid, pid != ABSENT)
                .attribute(METADATA_ATTRIBUTE, metadata.orElse(null), metadata.isPresent())
                .attribute(TOS_NOT_ACCEPTED_ATTRIBUTE, "1", termsOfServiceNotAccepted)
                .attribute(REKEY_ATTRIBUTE, "1", rekey);
        if (!children.isEmpty()) {
            builder.content(children);
        }
        return builder.build();
    }

    /**
     * Decodes a {@code <user>} or {@code <participant>} roster entry stanza into a
     * {@link CallParticipantUserNode}.
     *
     * <p>The {@link #childForm() child form} is taken from the stanza's description. The integer
     * {@code error} and {@code pid} attributes decode to {@code -1} when absent; the boolean
     * {@code tos_not_accepted} and {@code rekey} flags decode to {@code false} when absent; the
     * {@code account_kind} and {@code type} tokens are resolved through their enum mappings, with an
     * unclassifiable {@code type} token yielding an empty {@link #userType()} rather than a failure; and
     * each {@code <device>} child is decoded through {@link Device#of(Stanza)}. A stanza that is neither a
     * {@code <user>} nor a {@code <participant>} element yields an empty result so callers iterating a
     * mixed child list can skip it.
     *
     * @param stanza the roster entry stanza
     * @return the decoded entry, or an empty result when the stanza is not a usable roster entry
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code jid} attribute is absent
     */
    public static Optional<CallParticipantUserNode> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        ChildForm childForm = null;
        for (var form : ChildForm.values()) {
            if (stanza.hasDescription(form.element())) {
                childForm = form;
                break;
            }
        }
        if (childForm == null) {
            return Optional.empty();
        }
        var jid = stanza.getRequiredAttributeAsJid(JID_ATTRIBUTE);
        var userPn = stanza.getAttributeAsJid(USER_PN_ATTRIBUTE);
        var userLid = stanza.getAttributeAsJid(USER_LID_ATTRIBUTE);
        var username = stanza.getAttributeAsString(USERNAME_ATTRIBUTE);
        var pushName = stanza.getAttributeAsString(PUSH_NAME_ATTRIBUTE);
        var guestName = stanza.getAttributeAsString(GUEST_NAME_ATTRIBUTE);
        var accountKind = stanza.getAttributeAsString(ACCOUNT_KIND_ATTRIBUTE)
                .map(CallParticipantAccountKind::ofToken);
        var countryCode = stanza.getAttributeAsString(COUNTRY_CODE_ATTRIBUTE);
        var userType = stanza.getAttributeAsString(TYPE_ATTRIBUTE)
                .flatMap(CallParticipantUserType::ofToken);
        var state = stanza.getAttributeAsString(STATE_ATTRIBUTE);
        var error = stanza.getAttributeAsInt(ERROR_ATTRIBUTE, ABSENT);
        var reason = stanza.getAttributeAsString(REASON_ATTRIBUTE);
        var pid = stanza.getAttributeAsInt(PID_ATTRIBUTE, ABSENT);
        var metadata = stanza.getAttributeAsBytes(METADATA_ATTRIBUTE);
        var tosNotAccepted = stanza.getAttributeAsBool(TOS_NOT_ACCEPTED_ATTRIBUTE, false);
        var rekey = stanza.getAttributeAsBool(REKEY_ATTRIBUTE, false);
        var decoderCapability = stanza.getChild(DECODER_CAPABILITY_ELEMENT)
                .flatMap(Stanza::toContentString);
        var devices = stanza.streamChildren(Device.ELEMENT)
                .map(Device::of)
                .flatMap(Optional::stream)
                .toList();
        return Optional.of(new CallParticipantUserNode(childForm, jid, userPn, userLid, username, pushName,
                guestName, accountKind, countryCode, userType, state, error, reason, pid, metadata,
                tosNotAccepted, decoderCapability, rekey, devices));
    }

    /**
     * Represents one {@code <device>} child of a {@link CallParticipantUserNode}.
     *
     * <p>Each device of a member is listed as a {@code <device>} child carrying the device's
     * {@link #jid() device JID}, its {@link #platform() platform}, its server assigned
     * {@link #pid() participant id}, and, on the local device, an inline {@link #capability() capability}
     * mask. The device JID is the only required component; the platform, participant id, and capability
     * are absent unless the relay supplied them.
     *
     * <p>On the wire the element is
     * {@snippet lang = xml:
     * <device jid="83116928594056:2@lid" platform="web" pid="0">
     *   <capability ver="1">0105F709E4BB13</capability>
     * </device>
     *}.
     *
     * @param jid        the device JID; never {@code null}
     * @param platform   the device's client platform, present only when the relay supplied a
     *                   {@code platform} token
     * @param pid        the server assigned participant id of this device, or {@code -1} when absent
     * @param capability the inline capability mask, present only on a device that advertised one
     */
    public record Device(Jid jid,
                         Optional<CallParticipantPlatform> platform,
                         int pid,
                         Optional<CallCapability> capability) {
        /**
         * The wire element tag for a member device.
         */
        static final String ELEMENT = "device";

        /**
         * The wire attribute naming the device JID.
         */
        private static final String JID_ATTRIBUTE = "jid";

        /**
         * The wire attribute naming the device's client platform.
         */
        private static final String PLATFORM_ATTRIBUTE = "platform";

        /**
         * The wire attribute naming the device's participant id.
         */
        private static final String PID_ATTRIBUTE = "pid";

        /**
         * Canonicalizes the record components, rejecting {@code null} required components.
         *
         * @throws NullPointerException if {@code jid}, {@code platform}, or {@code capability} is
         *                              {@code null}
         */
        public Device {
            Objects.requireNonNull(jid, "jid cannot be null");
            Objects.requireNonNull(platform, "platform cannot be null");
            Objects.requireNonNull(capability, "capability cannot be null");
        }

        /**
         * Returns the server assigned participant id of this device, if present.
         *
         * @return an {@link OptionalInt} holding the {@code pid} value, or empty when absent
         */
        public OptionalInt pidValue() {
            return pid == ABSENT ? OptionalInt.empty() : OptionalInt.of(pid);
        }

        /**
         * Builds the {@code <device jid platform pid>[<capability/>]</device>} stanza.
         *
         * <p>The {@code platform} token, the {@code pid} attribute, and the inline {@code <capability>}
         * child are each omitted when absent rather than written as a sentinel.
         *
         * @return the device stanza
         */
        public Stanza toNode() {
            var builder = new StanzaBuilder()
                    .description(ELEMENT)
                    .attribute(JID_ATTRIBUTE, jid)
                    .attribute(PLATFORM_ATTRIBUTE, platform.map(CallParticipantPlatform::token).orElse(null), platform.isPresent())
                    .attribute(PID_ATTRIBUTE, pid, pid != ABSENT);
            capability.ifPresent(value -> builder.content(value.toStanza()));
            return builder.build();
        }

        /**
         * Decodes a {@code <device>} stanza into a {@link Device}.
         *
         * <p>The {@code platform} token resolves through {@link CallParticipantPlatform#ofToken(String)};
         * the {@code pid} attribute decodes to {@code -1} when absent; the inline {@code <capability>}
         * child decodes through {@link CallCapability#of(Stanza)}. A stanza that is not a {@code <device>}
         * element yields an empty result so callers iterating a mixed child list can skip it.
         *
         * @param stanza the {@code <device>} stanza
         * @return the decoded device, or an empty result when the stanza is not a usable device element
         * @throws NullPointerException   if {@code stanza} is {@code null}
         * @throws NoSuchElementException if the required {@code jid} attribute is absent
         */
        public static Optional<Device> of(Stanza stanza) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            if (!stanza.hasDescription(ELEMENT)) {
                return Optional.empty();
            }
            var jid = stanza.getRequiredAttributeAsJid(JID_ATTRIBUTE);
            var platform = stanza.getAttributeAsString(PLATFORM_ATTRIBUTE)
                    .map(CallParticipantPlatform::ofToken);
            var pid = stanza.getAttributeAsInt(PID_ATTRIBUTE, ABSENT);
            var capability = stanza.getChild(CallCapability.ELEMENT)
                    .flatMap(CallCapability::of);
            return Optional.of(new Device(jid, platform, pid, capability));
        }
    }
}
