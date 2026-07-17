package com.github.auties00.cobalt.calls.signaling.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents the {@code <group_info>} membership snapshot carried inside a group call's
 * {@code <group_update>}.
 *
 * <p>The group info element is the authoritative roster of a group call's participants at one point in
 * time. It pins two optional roster level attributes, the participant set hash and the server negotiated
 * connected limit, and carries the per participant identity entries as a list of either {@code <user>}
 * children or {@code <participant>} children. A receiver reconciles its local membership set against this
 * roster: each entry's identity, devices, capabilities, and state diff the corresponding participant
 * slot, and an entry missing from a fresh roster marks its slot removed.
 *
 * <p>The two child forms are mutually exclusive: a group info element carries only {@code <user>} entries
 * or only {@code <participant>} entries, never a mix. The {@code <participant>} form is the newer
 * membership representation; both forms share the same identity attribute vocabulary ({@code jid},
 * {@code user_pn}, {@code user_lid}, {@code username}, {@code push_name}, {@code guest_name},
 * {@code account_kind}, {@code platform}, {@code type}, {@code state}, {@code error}, {@code pid},
 * {@code country_code}, and the nested {@code <device>}, {@code <privacy>}, {@code <dec>}, and
 * {@code <rekey>} children). This record carries each entry as an opaque {@link Stanza} so the
 * participant subsystem owns the typed identity parse and this layer owns only the roster framing;
 * {@link #childForm()} reports which form the entries take.
 *
 * <p>On the wire the element takes the shape
 * {@snippet lang="xml" :
 * <group_info phash="..." connected-limit="N">
 *     <user .../>
 *     <!-- ... -->
 * </group_info>
 * }
 * or the equivalent with {@code <participant>} children. The roster may be empty (no entries), which is a
 * valid snapshot of a group call with no other connected participants.
 *
 * @implNote This implementation carries the entries as raw {@link Stanza} trees rather than a typed
 * participant record because the identity parse (the device list, capability decode, and the server user
 * state table) is owned by the participant subsystem, not the roster framing this record models. A single
 * roster carries at most {@value #MAX_PARTICIPANTS} entries.
 * @param phash          the participant set hash for a phash based link call, or {@code null} when absent
 * @param connectedLimit the server negotiated maximum simultaneously connected participant count, or
 *                       {@code -1} when absent
 * @param childForm      whether the entries are carried as {@code <user>} or {@code <participant>}
 *                       children; never {@code null}
 * @param entries        the per participant identity entries as opaque nodes; never {@code null},
 *                       possibly empty
 * @see GroupUpdateStanza
 * @see SignalingType#GROUP_UPDATE
 */
public record GroupInfoStanza(String phash, int connectedLimit, ChildForm childForm, List<Stanza> entries)
        implements CallMessage {
    /**
     * The wire element tag for a group info roster.
     */
    public static final String ELEMENT = "group_info";

    /**
     * The wire attribute naming the participant set hash on a {@code <group_info>} element.
     */
    private static final String PHASH_ATTRIBUTE = "phash";

    /**
     * The wire attribute naming the server negotiated connected limit on a {@code <group_info>}
     * element.
     */
    private static final String CONNECTED_LIMIT_ATTRIBUTE = "connected-limit";

    /**
     * The maximum number of participant entries a single group info roster may carry.
     */
    private static final int MAX_PARTICIPANTS = 0x7f;

    /**
     * Discriminates the child element form a {@link GroupInfoStanza} uses to carry its participant
     * entries.
     *
     * <p>A group info roster carries its entries as either {@code <user>} children or
     * {@code <participant>} children, never a mix; this enum records which form a given roster takes so
     * the builder emits the matching element tag and the decoder selects the matching children.
     */
    public enum ChildForm {
        /**
         * Marks a roster whose entries are carried as {@code <user>} children.
         */
        USER("user"),

        /**
         * Marks a roster whose entries are carried as {@code <participant>} children, the newer
         * membership representation.
         */
        PARTICIPANT("participant");

        /**
         * The wire element tag the entries of this form take.
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
         * Returns the wire element tag the entries of this form take.
         *
         * @return the entry element tag
         */
        public String element() {
            return element;
        }
    }

    /**
     * Canonicalizes the record components, defensively copying the entry list and enforcing the roster
     * invariants.
     *
     * <p>The entry list is copied into an unmodifiable list. The child form is required. Each entry's
     * description must match the {@link ChildForm#element() element tag} of the declared form so a
     * roster never mixes {@code <user>} and {@code <participant>} children. The roster may carry at most
     * {@value #MAX_PARTICIPANTS} entries.
     *
     * @throws NullPointerException     if {@code childForm} or {@code entries} is {@code null}, or any
     *                                  entry is {@code null}
     * @throws IllegalArgumentException if an entry's description does not match the declared child
     *                                  form, or the roster carries more than {@value #MAX_PARTICIPANTS}
     *                                  entries
     */
    public GroupInfoStanza {
        Objects.requireNonNull(childForm, "childForm cannot be null");
        Objects.requireNonNull(entries, "entries cannot be null");
        entries = List.copyOf(entries);
        if (entries.size() > MAX_PARTICIPANTS) {
            throw new IllegalArgumentException("group_info cannot carry more than " + MAX_PARTICIPANTS + " entries, got " + entries.size());
        }
        for (var entry : entries) {
            if (!entry.hasDescription(childForm.element())) {
                throw new IllegalArgumentException("group_info entry has description " + entry.description() + " but child form is " + childForm);
            }
        }
    }

    /**
     * Returns a group info roster carrying {@code <user>} entries.
     *
     * @param phash          the participant set hash, or {@code null} when absent
     * @param connectedLimit the server negotiated connected limit, or {@code -1} when absent
     * @param users          the {@code <user>} entry nodes
     * @return the user form group info roster
     * @throws NullPointerException     if {@code users} is {@code null} or any entry is {@code null}
     * @throws IllegalArgumentException if an entry is not a {@code <user>} element, or there are more
     *                                  than {@value #MAX_PARTICIPANTS} entries
     */
    public static GroupInfoStanza ofUsers(String phash, int connectedLimit, List<Stanza> users) {
        return new GroupInfoStanza(phash, connectedLimit, ChildForm.USER, users);
    }

    /**
     * Returns a group info roster carrying {@code <participant>} entries.
     *
     * @param phash          the participant set hash, or {@code null} when absent
     * @param connectedLimit the server negotiated connected limit, or {@code -1} when absent
     * @param participants   the {@code <participant>} entry nodes
     * @return the participant form group info roster
     * @throws NullPointerException     if {@code participants} is {@code null} or any entry is
     *                                  {@code null}
     * @throws IllegalArgumentException if an entry is not a {@code <participant>} element, or there are
     *                                  more than {@value #MAX_PARTICIPANTS} entries
     */
    public static GroupInfoStanza ofParticipants(String phash, int connectedLimit, List<Stanza> participants) {
        return new GroupInfoStanza(phash, connectedLimit, ChildForm.PARTICIPANT, participants);
    }

    /**
     * Returns the participant set hash, if present.
     *
     * @return an {@link Optional} holding the {@code phash} value, or empty when absent
     */
    public Optional<String> phashValue() {
        return Optional.ofNullable(phash);
    }

    /**
     * Returns the server negotiated connected limit, if present.
     *
     * @return an {@link OptionalInt} holding the connected limit, or empty when absent
     */
    public OptionalInt connectedLimitValue() {
        return connectedLimit < 0 ? OptionalInt.empty() : OptionalInt.of(connectedLimit);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@linkplain Optional#empty() empty}; a {@code <group_info>} descriptor carries no
     *         {@code call-id} header
     */
    @Override
    public Optional<String> callId() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@linkplain Optional#empty() empty}; a {@code <group_info>} descriptor carries no
     *         {@code call-creator} header
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * <p>A group info roster has no entry in the numeric {@code voip_signaling_message_type} table: it is
     * a structural sub element of {@code <group_update>}, not a standalone signaling action, so this
     * projection has no {@link SignalingType} and the method returns {@code null}. The element is
     * dispatched on its {@link #ELEMENT wire tag} when it appears directly under a {@code <call>}.
     *
     * @return {@code null}, since a group info roster carries no taxonomy ordinal
     */
    @Override
    public SignalingType type() {
        return null;
    }

    /**
     * Builds the {@code <group_info>} roster stanza.
     *
     * <p>The {@code phash} and {@code connected-limit} attributes are omitted when absent rather than
     * written as sentinels; the entries are emitted as children under their declared
     * {@link ChildForm#element() form tag}. A roster with no entries produces a {@code <group_info>}
     * element with only its attributes.
     *
     * @return the group info roster stanza
     */
    @Override
    public Stanza toStanza() {
        var builder = new StanzaBuilder()
                .description(ELEMENT)
                .attribute(PHASH_ATTRIBUTE, phash)
                .attribute(CONNECTED_LIMIT_ATTRIBUTE, connectedLimit, connectedLimit >= 0);
        if (!entries.isEmpty()) {
            builder.content(entries);
        }
        return builder.build();
    }

    /**
     * Decodes a {@code <group_info>} stanza into a {@link GroupInfoStanza}.
     *
     * <p>The child form is selected by the entries present: a roster with any {@code <participant>}
     * child decodes to the {@link ChildForm#PARTICIPANT participant} form and a roster with only
     * {@code <user>} children (or no entries) decodes to the {@link ChildForm#USER user} form. A stanza
     * that carries both forms violates the engine invariant and yields an empty result rather than
     * silently dropping one form. A stanza that is not a {@code <group_info>} element also yields an
     * empty result so callers iterating a mixed child list can skip it.
     *
     * @param stanza the {@code <group_info>} stanza
     * @return the decoded roster, or an empty result when the stanza is not a usable group info element
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    public static Optional<GroupInfoStanza> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription(ELEMENT)) {
            return Optional.empty();
        }
        var users = stanza.getChildren(ChildForm.USER.element());
        var participants = stanza.getChildren(ChildForm.PARTICIPANT.element());
        if (!users.isEmpty() && !participants.isEmpty()) {
            return Optional.empty();
        }
        var phash = stanza.getAttributeAsString(PHASH_ATTRIBUTE, null);
        var connectedLimit = stanza.getAttributeAsInt(CONNECTED_LIMIT_ATTRIBUTE, -1);
        if (!participants.isEmpty()) {
            return Optional.of(new GroupInfoStanza(phash, connectedLimit, ChildForm.PARTICIPANT, List.copyOf(participants)));
        }
        return Optional.of(new GroupInfoStanza(phash, connectedLimit, ChildForm.USER, List.copyOf(users)));
    }
}
