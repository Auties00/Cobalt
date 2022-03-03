package it.auties.whatsapp.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.whatsapp.protobuf.chat.Chat;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.message.model.Message;
import it.auties.whatsapp.protobuf.sync.HistorySyncMessage;
import lombok.NonNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * This class is an implementation of ArrayList used to store the {@link Message} in a {@link Chat}.
 * The messages stored in this internal are guaranteed to be stored chronologically.
 * This contract can be technically broken using reflection to access the array associated with this internal, though, obviously, it's not advisable.
 *
 */
public final class SortedMessageList implements List<MessageInfo> {
    /**
     * The default comparator used to sort the entries in this internal
     */
    private static final Comparator<HistorySyncMessage> ENTRY_COMPARATOR
            = Comparator.comparingLong(entry -> entry.message().timestamp());

    private final List<HistorySyncMessage> internal;

    /**
     * @param internal An internal used to represent the messages
     */
    @JsonCreator
    public SortedMessageList(List<HistorySyncMessage> internal) {
        this.internal = internal;
    }

    public SortedMessageList() {
        this(new ArrayList<>());
    }

    /**
     * Adds {@code message} to this internal in the right position in order to respect the contract explained previously.
     * If this message already exists, it is removed automatically.
     *
     * @param message the non-null message to add to this internal
     * @return true if {@code message} was added successfully
     */
    public boolean add(@NonNull MessageInfo message) {
        internal.removeIf(entry -> message.id().equals(entry.message().id()));
        var initialSize = internal.size();
        var newEntry = new HistorySyncMessage(message, -1);
        var insertionPoint = Collections.binarySearch(internal, newEntry, ENTRY_COMPARATOR);
        internal.add(insertionPoint > -1 ? insertionPoint : -insertionPoint - 1, newEntry);
        return internal.size() != initialSize;
    }

    @Override
    public boolean remove(Object o) {
        return internal.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return new HashSet<>(internal).containsAll(c);
    }

    /**
     * Adds each entry of {@code internal} if no other entry in this internal has an jid that matches said entry's.
     * Otherwise, it removes said entry and adds said entry.
     *
     * @param collection the internal to add to this internal
     */
    public boolean addAll(@NonNull Collection<? extends MessageInfo> collection) {
        collection.forEach(this::add);
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends MessageInfo> c) {
        throw new UnsupportedOperationException("Put operations based on index are not supported");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return internal.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return internal.retainAll(c);
    }

    public boolean isEmpty() {
        return internal.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return internal.contains(o);
    }

    @Override
    public Iterator<MessageInfo> iterator() {
        return internal.stream()
                .map(HistorySyncMessage::message)
                .iterator();
    }

    @Override
    public Object[] toArray() {
        return internal.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Typed to array is not supported");
    }

    public MessageInfo get(int index) {
        return internal.get(index).message();
    }

    @Override
    public MessageInfo set(int index, MessageInfo element) {
        throw new UnsupportedOperationException("Put operations based on index are not supported");
    }

    @Override
    public void add(int index, MessageInfo element) {
        throw new UnsupportedOperationException("Put operations based on index are not supported");
    }

    @Override
    public MessageInfo remove(int index) {
        var result = internal.remove(index);
        return result != null ? result.message()
                : null;
    }

    @Override
    public int indexOf(Object o) {
        return internal.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return internal.lastIndexOf(o);
    }

    @Override
    public ListIterator<MessageInfo> listIterator() {
        throw new UnsupportedOperationException("List iterators are not supported");
    }

    @Override
    public ListIterator<MessageInfo> listIterator(int index) {
        throw new UnsupportedOperationException("List iterators are not supported");
    }

    @Override
    public List<MessageInfo> subList(int fromIndex, int toIndex) {
        return internal.subList(fromIndex, toIndex)
                .stream()
                .map(HistorySyncMessage::message)
                .toList();
    }

    public int size() {
        return internal.size();
    }

    public Stream<MessageInfo> stream() {
        return internal.stream().map(HistorySyncMessage::message);
    }

    public Stream<MessageInfo> parallelStream() {
        return internal.parallelStream().map(HistorySyncMessage::message);
    }

    public void forEach(Consumer<? super MessageInfo> consumer) {
        internal.forEach(entry -> consumer.accept(entry.message()));
    }

    public void clear() {
        internal.clear();
    }

    public void remove(MessageInfo message) {
        internal.removeIf(entry -> Objects.equals(message, entry.message()));
    }

    @JsonValue
    public List<HistorySyncMessage> toSync(){
        return internal;
    }
}