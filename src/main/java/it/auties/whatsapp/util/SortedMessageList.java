package it.auties.whatsapp.util;

import com.fasterxml.jackson.annotation.JsonValue;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.sync.HistorySyncMessage;
import lombok.NonNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public final class SortedMessageList implements List<MessageInfo> {
    private final List<HistorySyncMessage> internal;

    public SortedMessageList(List<HistorySyncMessage> internal) {
        this.internal = internal;
    }

    public SortedMessageList() {
        this(new ArrayList<>());
    }

    public boolean add(@NonNull MessageInfo message) {
        return internal.add(new HistorySyncMessage(message, size()));
    }

    @Override
    public boolean remove(Object o) {
        return internal.remove(o);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return new HashSet<>(internal).containsAll(c);
    }

    public boolean addAll(@NonNull Collection<? extends MessageInfo> collection) {
        collection.forEach(this::add);
        return true;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends MessageInfo> c) {
        throw new UnsupportedOperationException("Put operations based on index are not supported");
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return internal.removeAll(c);
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
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
    public @NonNull Iterator<MessageInfo> iterator() {
        return internal.stream()
                .map(HistorySyncMessage::message)
                .iterator();
    }

    @Override
    public Object @NonNull [] toArray() {
        throw new UnsupportedOperationException("To array is not supported");
    }

    @Override
    public <T> T @NonNull [] toArray(T @NonNull [] a) {
        throw new UnsupportedOperationException("To array is not supported");
    }

    public MessageInfo get(int index) {
        return internal.get(index)
                .message();
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
        return result != null ?
                result.message() :
                null;
    }

    @Override
    public int indexOf(Object object) {
        return switch (object) {
            case HistorySyncMessage historySyncMessage -> internal.indexOf(historySyncMessage);
            case MessageInfo messageInfo -> internal.stream()
                    .map(HistorySyncMessage::message)
                    .filter(messageInfo::equals)
                    .findFirst()
                    .map(internal::indexOf)
                    .orElse(-1);
            default -> throw new IllegalArgumentException("Cannot find index of %s".formatted(object));
        };
    }

    @Override
    public int lastIndexOf(Object o) {
        return internal.lastIndexOf(o);
    }

    @Override
    public @NonNull ListIterator<MessageInfo> listIterator() {
        return internal.stream()
                .map(HistorySyncMessage::message)
                .toList()
                .listIterator();
    }

    @Override
    public @NonNull ListIterator<MessageInfo> listIterator(int index) {
        return internal.stream()
                .map(HistorySyncMessage::message)
                .toList()
                .listIterator(index);
    }

    @Override
    public List<MessageInfo> subList(int fromIndex, int toIndex) {
        return internal.subList(fromIndex, toIndex)
                .stream()
                .map(HistorySyncMessage::message)
                .toList();
    }

    @Override
    public int size() {
        return internal.size();
    }

    @Override
    public Stream<MessageInfo> stream() {
        return internal.stream()
                .map(HistorySyncMessage::message);
    }

    @Override
    public Stream<MessageInfo> parallelStream() {
        return internal.parallelStream()
                .map(HistorySyncMessage::message);
    }

    @Override
    public void forEach(Consumer<? super MessageInfo> consumer) {
        internal.forEach(entry -> consumer.accept(entry.message()));
    }

    @Override
    public void clear() {
        internal.clear();
    }

    public void remove(MessageInfo message) {
        internal.removeIf(entry -> Objects.equals(message, entry.message()));
    }

    @JsonValue
    public List<HistorySyncMessage> toSync() {
        return internal;
    }

    @Override
    public void replaceAll(UnaryOperator<MessageInfo> operator) {
        internal.replaceAll(historySyncMessage -> new HistorySyncMessage(operator.apply(historySyncMessage.message()),
                indexOf(historySyncMessage.message())));
    }

    @Override
    public void sort(Comparator<? super MessageInfo> comparator) {
        internal.sort((o1, o2) -> comparator.compare(o1 != null ?
                o1.message() :
                null, o2 != null ? o2.message() : null));
    }

    @Override
    public Spliterator<MessageInfo> spliterator() {
        return internal.stream()
                .map(HistorySyncMessage::message)
                .spliterator();
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        throw new UnsupportedOperationException("To array is not supported");
    }

    @Override
    public boolean removeIf(Predicate<? super MessageInfo> filter) {
        return internal.removeIf(entry -> filter.test(entry.message()));
    }
}