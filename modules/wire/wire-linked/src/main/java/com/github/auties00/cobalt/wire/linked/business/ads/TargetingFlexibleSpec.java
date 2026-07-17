package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * One detailed-targeting group of a Click-to-WhatsApp ad targeting spec.
 *
 * <p>Detailed targeting narrows an ad's audience by interests, behaviours, and demographics grouped
 * into flexible-spec entries. This model carries the writable category lists WhatsApp Web populates,
 * each a list of {@link DetailedTargetingItem} entries: {@link #interests() interests},
 * {@link #behaviors() behaviours}, {@link #lifeEvents() life events}, {@link #educationStatuses()
 * education statuses}, {@link #educationSchools() schools}, {@link #educationMajors() majors},
 * {@link #workPositions() job titles}, {@link #workEmployers() employers}, {@link #relationshipStatuses()
 * relationship statuses}, and {@link #interestedIn() interested-in}.
 */
@ProtobufMessage(name = "TargetingFlexibleSpec")
public final class TargetingFlexibleSpec {
    /**
     * Targeted interests, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<DetailedTargetingItem> interests;

    /**
     * Targeted behaviours, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<DetailedTargetingItem> behaviors;

    /**
     * Targeted life events, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<DetailedTargetingItem> lifeEvents;

    /**
     * Targeted education statuses, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final List<DetailedTargetingItem> educationStatuses;

    /**
     * Targeted education schools, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final List<DetailedTargetingItem> educationSchools;

    /**
     * Targeted education majors, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final List<DetailedTargetingItem> educationMajors;

    /**
     * Targeted job titles, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final List<DetailedTargetingItem> workPositions;

    /**
     * Targeted employers, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final List<DetailedTargetingItem> workEmployers;

    /**
     * Targeted relationship statuses, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    final List<DetailedTargetingItem> relationshipStatuses;

    /**
     * Targeted interested-in values, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    final List<DetailedTargetingItem> interestedIn;

    /**
     * Constructs a new {@code TargetingFlexibleSpec}. Every {@code null} list argument is coerced to an
     * empty list.
     *
     * @param interests            the targeted interests; {@code null} treated as empty
     * @param behaviors            the targeted behaviours; {@code null} treated as empty
     * @param lifeEvents           the targeted life events; {@code null} treated as empty
     * @param educationStatuses    the targeted education statuses; {@code null} treated as empty
     * @param educationSchools     the targeted education schools; {@code null} treated as empty
     * @param educationMajors      the targeted education majors; {@code null} treated as empty
     * @param workPositions        the targeted job titles; {@code null} treated as empty
     * @param workEmployers        the targeted employers; {@code null} treated as empty
     * @param relationshipStatuses the targeted relationship statuses; {@code null} treated as empty
     * @param interestedIn         the targeted interested-in values; {@code null} treated as empty
     */
    TargetingFlexibleSpec(List<DetailedTargetingItem> interests, List<DetailedTargetingItem> behaviors,
                          List<DetailedTargetingItem> lifeEvents, List<DetailedTargetingItem> educationStatuses,
                          List<DetailedTargetingItem> educationSchools, List<DetailedTargetingItem> educationMajors,
                          List<DetailedTargetingItem> workPositions, List<DetailedTargetingItem> workEmployers,
                          List<DetailedTargetingItem> relationshipStatuses, List<DetailedTargetingItem> interestedIn) {
        this.interests = interests == null ? List.of() : List.copyOf(interests);
        this.behaviors = behaviors == null ? List.of() : List.copyOf(behaviors);
        this.lifeEvents = lifeEvents == null ? List.of() : List.copyOf(lifeEvents);
        this.educationStatuses = educationStatuses == null ? List.of() : List.copyOf(educationStatuses);
        this.educationSchools = educationSchools == null ? List.of() : List.copyOf(educationSchools);
        this.educationMajors = educationMajors == null ? List.of() : List.copyOf(educationMajors);
        this.workPositions = workPositions == null ? List.of() : List.copyOf(workPositions);
        this.workEmployers = workEmployers == null ? List.of() : List.copyOf(workEmployers);
        this.relationshipStatuses = relationshipStatuses == null ? List.of() : List.copyOf(relationshipStatuses);
        this.interestedIn = interestedIn == null ? List.of() : List.copyOf(interestedIn);
    }

    /**
     * Returns the targeted interests.
     *
     * @return an unmodifiable view of the targeted interests; never {@code null}, possibly empty
     */
    public List<DetailedTargetingItem> interests() {
        return interests;
    }

    /**
     * Returns the targeted behaviours.
     *
     * @return an unmodifiable view of the targeted behaviours; never {@code null}, possibly empty
     */
    public List<DetailedTargetingItem> behaviors() {
        return behaviors;
    }

    /**
     * Returns the targeted life events.
     *
     * @return an unmodifiable view of the targeted life events; never {@code null}, possibly empty
     */
    public List<DetailedTargetingItem> lifeEvents() {
        return lifeEvents;
    }

    /**
     * Returns the targeted education statuses.
     *
     * @return an unmodifiable view of the targeted education statuses; never {@code null}, possibly empty
     */
    public List<DetailedTargetingItem> educationStatuses() {
        return educationStatuses;
    }

    /**
     * Returns the targeted education schools.
     *
     * @return an unmodifiable view of the targeted education schools; never {@code null}, possibly empty
     */
    public List<DetailedTargetingItem> educationSchools() {
        return educationSchools;
    }

    /**
     * Returns the targeted education majors.
     *
     * @return an unmodifiable view of the targeted education majors; never {@code null}, possibly empty
     */
    public List<DetailedTargetingItem> educationMajors() {
        return educationMajors;
    }

    /**
     * Returns the targeted job titles.
     *
     * @return an unmodifiable view of the targeted job titles; never {@code null}, possibly empty
     */
    public List<DetailedTargetingItem> workPositions() {
        return workPositions;
    }

    /**
     * Returns the targeted employers.
     *
     * @return an unmodifiable view of the targeted employers; never {@code null}, possibly empty
     */
    public List<DetailedTargetingItem> workEmployers() {
        return workEmployers;
    }

    /**
     * Returns the targeted relationship statuses.
     *
     * @return an unmodifiable view of the targeted relationship statuses; never {@code null}, possibly empty
     */
    public List<DetailedTargetingItem> relationshipStatuses() {
        return relationshipStatuses;
    }

    /**
     * Returns the targeted interested-in values.
     *
     * @return an unmodifiable view of the targeted interested-in values; never {@code null}, possibly empty
     */
    public List<DetailedTargetingItem> interestedIn() {
        return interestedIn;
    }
}
