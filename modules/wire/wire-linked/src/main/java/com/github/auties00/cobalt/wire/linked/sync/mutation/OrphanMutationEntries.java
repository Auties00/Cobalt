package com.github.auties00.cobalt.wire.linked.sync.mutation;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collection;
import java.util.Collections;
import java.util.SequencedCollection;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A concurrent envelope around the per-collection list of {@link OrphanMutationEntry}
 * records that are awaiting replay.
 *
 * <p>Orphan mutations are retained as a single repeated field so that they can be
 * persisted together with the rest of the syncd state. This type wraps that list in a
 * protobuf message because the encoding requires a message wrapper for any repeated
 * field that needs its own deserialization seam.
 *
 * <p>The entries are held in a {@link ConcurrentLinkedDeque}, so a thread iterating
 * the entries exposed by {@link #entries()} traverses a weakly consistent snapshot and
 * never observes a {@link java.util.ConcurrentModificationException} while another
 * thread appends through {@link #add(OrphanMutationEntry)} or prunes through
 * {@link #removeAll(Collection)} or {@link #clear()}. The backing collection is
 * preserved across serialization round trips, so an instance reconstructed from its
 * encoded form is as thread-safe as a freshly constructed one.
 */
@ProtobufMessage
public final class OrphanMutationEntries {
    /**
     * The wrapped orphan mutation entries awaiting replay, in insertion order.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final SequencedCollection<OrphanMutationEntry> entries;

    /**
     * Constructs a container seeded with a thread-safe copy of the supplied entries.
     *
     * <p>The supplied entries are copied into a fresh {@link ConcurrentLinkedDeque}
     * rather than adopted by reference, so the resulting container is thread-safe
     * regardless of the concrete collection the caller passes. A {@code null} argument
     * yields an empty container.
     *
     * @param entries the entries to seed the container with, or {@code null} for none
     */
    OrphanMutationEntries(SequencedCollection<OrphanMutationEntry> entries) {
        this.entries = entries != null ? new ConcurrentLinkedDeque<>(entries) : new ConcurrentLinkedDeque<>();
    }

    /**
     * Constructs an empty container backed by a thread-safe collection.
     */
    public OrphanMutationEntries() {
        this.entries = new ConcurrentLinkedDeque<>();
    }

    /**
     * Returns an unmodifiable view of the orphan mutation entries in insertion order.
     *
     * <p>The returned view reflects later mutations to this container rather than a
     * point-in-time copy, and iterating it is safe while another thread appends or
     * removes entries. Callers that need a stable snapshot should copy the view.
     *
     * @return an unmodifiable, insertion-ordered view of the entries
     */
    public SequencedCollection<OrphanMutationEntry> entries() {
        return Collections.unmodifiableSequencedCollection(entries);
    }

    /**
     * Appends an orphan mutation entry to the end of this container.
     *
     * @param entry the entry to append
     */
    public void add(OrphanMutationEntry entry) {
        entries.add(entry);
    }

    /**
     * Removes every entry contained in the supplied collection from this container.
     *
     * <p>Entries absent from this container are ignored; entries present more than
     * once are all removed.
     *
     * @param toRemove the entries to remove
     */
    public void removeAll(Collection<OrphanMutationEntry> toRemove) {
        entries.removeAll(toRemove);
    }

    /**
     * Removes every entry from this container.
     */
    public void clear() {
        entries.clear();
    }

    /**
     * Returns whether this container holds no entries.
     *
     * @return {@code true} if this container is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
