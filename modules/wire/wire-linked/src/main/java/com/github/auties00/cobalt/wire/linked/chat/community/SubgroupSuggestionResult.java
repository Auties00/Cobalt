package com.github.auties00.cobalt.wire.linked.chat.community;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the outcome of a sub-group suggestion proposed to a community.
 *
 * <p>Community administrators can suggest that the relay either spin up a
 * fresh sub-group with a chosen subject and description, or fold one or
 * more existing groups into the community as sub-groups. The relay's reply
 * carries one of two shapes: a {@code newGroup} variant projecting the
 * provisional metadata of the freshly reserved sub-group JID, or an
 * {@code existingGroups} variant projecting the per-candidate result rows
 * surfaced when the relay validated each pre-existing group against the
 * community's policies.
 *
 * <p>Use {@link #kind()} to discriminate between the two shapes:
 * {@link Kind#NEW_GROUP} populates {@link #newGroupJid()},
 * {@link #newGroupCreator()} and {@link #newGroupCreationTimestamp()};
 * {@link Kind#EXISTING_GROUPS} populates {@link #candidates()}.
 */
@ProtobufMessage(name = "SubgroupSuggestionResult")
public final class SubgroupSuggestionResult {
    /**
     * The variant discriminator. Required, never {@code null} after
     * construction.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    Kind kind;

    /**
     * The provisional sub-group JID reserved by the relay; populated only
     * on the {@link Kind#NEW_GROUP} variant.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    Jid newGroupJid;

    /**
     * The user that created the suggestion; populated only on the
     * {@link Kind#NEW_GROUP} variant.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    Jid newGroupCreator;

    /**
     * The instant at which the suggestion was created; populated only on
     * the {@link Kind#NEW_GROUP} variant.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant newGroupCreationTimestamp;

    /**
     * The optional resolved phone-number JID for the suggestion creator;
     * populated only on the {@link Kind#NEW_GROUP} variant when the relay
     * surfaced LID-to-PN resolution.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    Jid newGroupCreatorPhoneNumber;

    /**
     * The optional description-error string echoed by the relay when it
     * could not accept the proposed description verbatim. Populated only
     * on the {@link Kind#NEW_GROUP} variant.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String newGroupDescriptionError;

    /**
     * The per-candidate result rows projected on the
     * {@link Kind#EXISTING_GROUPS} variant.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    List<Candidate> candidates;

    /**
     * Constructs a new suggestion result.
     *
     * @param kind                       the variant discriminator; must
     *                                   not be {@code null}
     * @param newGroupJid                optional provisional sub-group
     *                                   JID; must be set on the
     *                                   {@link Kind#NEW_GROUP} variant
     * @param newGroupCreator            optional creator JID; must be
     *                                   set on the {@link Kind#NEW_GROUP}
     *                                   variant
     * @param newGroupCreationTimestamp  optional creation instant; must
     *                                   be set on the
     *                                   {@link Kind#NEW_GROUP} variant
     * @param newGroupCreatorPhoneNumber optional resolved creator phone
     *                                   number JID
     * @param newGroupDescriptionError   optional description-error
     *                                   string
     * @param candidates                 the per-candidate rows; must be
     *                                   set on the
     *                                   {@link Kind#EXISTING_GROUPS}
     *                                   variant
     * @throws NullPointerException if {@code kind} is {@code null}
     */
    SubgroupSuggestionResult(Kind kind, Jid newGroupJid, Jid newGroupCreator,
                             Instant newGroupCreationTimestamp, Jid newGroupCreatorPhoneNumber,
                             String newGroupDescriptionError, List<Candidate> candidates) {
        this.kind = Objects.requireNonNull(kind, "kind cannot be null");
        this.newGroupJid = newGroupJid;
        this.newGroupCreator = newGroupCreator;
        this.newGroupCreationTimestamp = newGroupCreationTimestamp;
        this.newGroupCreatorPhoneNumber = newGroupCreatorPhoneNumber;
        this.newGroupDescriptionError = newGroupDescriptionError;
        this.candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    /**
     * Returns the variant discriminator.
     *
     * @return the non-{@code null} discriminator
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Returns the provisional sub-group JID reserved by the relay.
     *
     * @return an {@link Optional} carrying the JID, or empty when the
     *         result is not the {@link Kind#NEW_GROUP} variant
     */
    public Optional<Jid> newGroupJid() {
        return Optional.ofNullable(newGroupJid);
    }

    /**
     * Returns the user that created the suggestion.
     *
     * @return an {@link Optional} carrying the creator JID, or empty
     *         when the result is not the {@link Kind#NEW_GROUP} variant
     */
    public Optional<Jid> newGroupCreator() {
        return Optional.ofNullable(newGroupCreator);
    }

    /**
     * Returns the instant the suggestion was created at.
     *
     * @return an {@link Optional} carrying the timestamp, or empty when
     *         the result is not the {@link Kind#NEW_GROUP} variant
     */
    public Optional<Instant> newGroupCreationTimestamp() {
        return Optional.ofNullable(newGroupCreationTimestamp);
    }

    /**
     * Returns the resolved phone-number JID for the creator.
     *
     * @return an {@link Optional} carrying the phone-number JID, or empty
     *         when the relay omitted it
     */
    public Optional<Jid> newGroupCreatorPhoneNumber() {
        return Optional.ofNullable(newGroupCreatorPhoneNumber);
    }

    /**
     * Returns the optional description-error string.
     *
     * @return an {@link Optional} carrying the description-error string,
     *         or empty when the relay accepted the description verbatim
     */
    public Optional<String> newGroupDescriptionError() {
        return Optional.ofNullable(newGroupDescriptionError);
    }

    /**
     * Returns the per-candidate result rows for the
     * {@link Kind#EXISTING_GROUPS} variant.
     *
     * @return an unmodifiable list of candidate rows; empty for the
     *         {@link Kind#NEW_GROUP} variant
     */
    public List<Candidate> candidates() {
        return candidates == null ? List.of() : Collections.unmodifiableList(candidates);
    }

    /**
     * Identifies which projection of the suggestion-result wire shape
     * the result corresponds to.
     */
    @ProtobufEnum
    public enum Kind {
        /**
         * The relay reserved a new sub-group JID for the proposed
         * suggestion.
         */
        NEW_GROUP(0),

        /**
         * The relay validated the proposed pre-existing groups and
         * returned per-candidate outcomes.
         */
        EXISTING_GROUPS(1);

        /**
         * The protobuf wire-format index associated with this kind.
         */
        final int index;

        /**
         * Constructs a new {@code Kind} with the supplied protobuf
         * index.
         *
         * @param index the protobuf wire-format index
         */
        Kind(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf wire-format index associated with this
         * value.
         *
         * @return the protobuf wire-format index
         */
        public int index() {
            return index;
        }
    }

    /**
     * Per-candidate projection captured on the
     * {@link Kind#EXISTING_GROUPS} variant.
     */
    @ProtobufMessage(name = "SubgroupSuggestionResult.Candidate")
    public static final class Candidate {
        /**
         * The candidate group JID echoed by the relay. Required, never
         * {@code null} after construction.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid jid;

        /**
         * The optional per-candidate failure discriminator. {@code null}
         * when the candidate was admitted cleanly.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
        Reason reason;

        /**
         * Constructs a new candidate row.
         *
         * @param jid    the candidate JID; must not be {@code null}
         * @param reason the optional failure discriminator; {@code null}
         *               when the candidate was admitted cleanly
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        Candidate(Jid jid, Reason reason) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            this.reason = reason;
        }

        /**
         * Returns the candidate JID.
         *
         * @return the non-{@code null} JID
         */
        public Jid jid() {
            return jid;
        }

        /**
         * Returns the optional per-candidate failure discriminator.
         *
         * @return an {@link Optional} carrying the {@link Reason}, or
         *         empty when the candidate was admitted cleanly
         */
        public Optional<Reason> reason() {
            return Optional.ofNullable(reason);
        }

        /**
         * Returns whether the candidate was admitted cleanly.
         *
         * @return {@code true} when the relay reported no failure for
         *         this candidate
         */
        public boolean succeeded() {
            return reason == null;
        }

        /**
         * Identifies why a single candidate sub-group was rejected.
         */
        @ProtobufEnum
        public enum Reason {
            /**
             * The caller does not own the candidate or lacks the admin
             * rights required to attach it as a sub-group.
             */
            NOT_AUTHORIZED(0),

            /**
             * The candidate group does not exist on the relay.
             */
            NOT_EXIST(1),

            /**
             * The candidate is already linked to another community or
             * conflicts with an existing community-level invariant.
             */
            CONFLICT(2),

            /**
             * The community's policy forbids accepting suggestions for
             * this candidate.
             */
            SUGGESTION_NOT_ALLOWED(3),

            /**
             * The community has reached its sub-group resource limit
             * and cannot accept additional candidates.
             */
            RESOURCE_LIMIT(4),

            /**
             * The request was rejected as malformed.
             */
            BAD_REQUEST(5),

            /**
             * The relay refused the candidate for policy reasons.
             */
            NOT_ACCEPTABLE(6),

            /**
             * The relay encountered a transient internal failure while
             * validating the candidate.
             */
            SERVER_ERROR(7);

            /**
             * The protobuf wire-format index associated with this reason.
             */
            final int index;

            /**
             * Constructs a new {@code Reason} with the supplied protobuf
             * index.
             *
             * @param index the protobuf wire-format index
             */
            Reason(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * Returns the protobuf wire-format index associated with
             * this value.
             *
             * @return the protobuf wire-format index
             */
            public int index() {
                return index;
            }

            /**
             * Returns the {@link Reason} matching the given on-the-wire
             * discriminator tag, or {@code null} when the tag is absent
             * or unrecognised.
             *
             * @param tag the discriminator tag emitted by the relay;
             *            may be {@code null}
             * @return the matching {@link Reason}, or {@code null} when
             *         the tag does not match any known reason
             */
            public static Reason of(String tag) {
                if (tag == null) {
                    return null;
                }
                return switch (tag) {
                    case "not_authorized" -> NOT_AUTHORIZED;
                    case "not_exist" -> NOT_EXIST;
                    case "conflict" -> CONFLICT;
                    case "suggestion_not_allowed" -> SUGGESTION_NOT_ALLOWED;
                    case "resource_limit" -> RESOURCE_LIMIT;
                    case "bad_request" -> BAD_REQUEST;
                    case "not_acceptable" -> NOT_ACCEPTABLE;
                    case "server_error" -> SERVER_ERROR;
                    default -> null;
                };
            }
        }
    }
}
