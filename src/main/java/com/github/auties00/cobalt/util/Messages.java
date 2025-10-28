package com.github.auties00.cobalt.util;

import com.github.auties00.cobalt.model.info.MessageInfo;
import com.github.auties00.collections.ConcurrentLinkedHashMap;

import java.util.*;

public final class Messages<E extends MessageInfo> implements SequencedCollection<E> {
    private final ConcurrentLinkedHashMap<String, E> backing;

    public Messages() {
        this.backing = new ConcurrentLinkedHashMap<>();
    }

    public Optional<E> getById(String id) {
        return Optional.ofNullable(backing.get(id));
    }

    public boolean removeById(String id) {
        return backing.remove(id) != null;
    }

    @Override
    public SequencedCollection<E> reversed() {
        return backing.sequencedValues()
                .reversed();
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof MessageInfo messageInfo
               && backing.containsKey(messageInfo.id());
    }

    @Override
    public Iterator<E> iterator() {
        return backing.sequencedValues()
                .iterator();
    }

    @Override
    public Object[] toArray() {
        return backing.sequencedValues()
                .toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return backing.sequencedValues()
                .toArray(a);
    }

    @Override
    public boolean add(E messageInfo) {
        Objects.requireNonNull(messageInfo);
        backing.put(messageInfo.id(), messageInfo);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        return o instanceof MessageInfo messageInfo
               && backing.remove(messageInfo.id()) != null;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        Objects.requireNonNull(collection);
        for(var entry : collection) {
            if (!(entry instanceof MessageInfo messageInfo)) {
                return false;
            }

            if (!backing.containsKey(messageInfo.id())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        Objects.requireNonNull(collection);
        for(var entry : collection) {
            backing.put(entry.id(), entry);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        Objects.requireNonNull(collection);
        var result = true;
        for(var entry : collection) {
            if (!(entry instanceof MessageInfo messageInfo) || backing.remove(messageInfo.id()) == null) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return false;
    }

    @Override
    public void clear() {
        backing.clear();
    }
}