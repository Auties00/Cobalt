package com.github.auties00.cobalt.model.info;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unchecked")
public final class MessageInfoContainer<V extends MessageInfo> extends AbstractMap<String, V> implements SequencedMap<String, V> {
    // TODO Assess impact of cache sharing
    //      https://www.baeldung.com/java-false-sharing-contended
    //      Not sure if the added memory usage is worth it for Cobalt + users would need to run with --add-opens,--add-exports,-XX:-RestrictContended
    private static final class Node<V extends MessageInfo> {
        private static final VarHandle NEXT;
        private static final VarHandle PREV;
        private static final VarHandle MARKED;
        private static final VarHandle VALUE;

        static {
            try {
                var lookup = MethodHandles.lookup();
                NEXT = lookup.findVarHandle(Node.class, "next", Node.class);
                PREV = lookup.findVarHandle(Node.class, "prev", Node.class);
                MARKED = lookup.findVarHandle(Node.class, "marked", boolean.class);
                VALUE = lookup.findVarHandle(Node.class, "value", MessageInfo.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        final String key;
        volatile V value;
        volatile Node<V> next;
        volatile Node<V> prev;
        volatile boolean marked;

        Node(String key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final VarHandle HEAD_HANDLE;
    private static final VarHandle TAIL_HANDLE;

    static {
        try {
            var lookup = MethodHandles.lookup();
            HEAD_HANDLE = lookup.findVarHandle(MessageInfoContainer.class, "head", Node.class);
            TAIL_HANDLE = lookup.findVarHandle(MessageInfoContainer.class, "tail", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("FieldMayBeFinal")
    private volatile Node<V> head;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile Node<V> tail;
    private final ConcurrentMap<String, Node<V>> keyToNode;

    public MessageInfoContainer() {
        this.keyToNode = new ConcurrentHashMap<>();
        this.head = null;
        this.tail = null;
    }

    @Override
    public V put(String key, V value) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        var existingNode = keyToNode.get(key);
        if (existingNode != null) {
            // Update existing value
            return (V) Node.VALUE.getAndSet(existingNode, value);
        }

        var newNode = new Node<>(key, value);
        var existing = keyToNode.putIfAbsent(key, newNode);
        if (existing != null) {
            // Race: another thread added it
            return (V) Node.VALUE.getAndSet(existing, value);
        }

        addLastNode(newNode);
        return null;
    }

    @Override
    public V putIfAbsent(String key, V value) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        var existingNode = keyToNode.get(key);
        if (existingNode != null) {
            return existingNode.value;
        }

        var newNode = new Node<>(key, value);
        var existing = keyToNode.putIfAbsent(key, newNode);
        if (existing != null) {
            return existing.value;
        }

        addLastNode(newNode);
        return null;
    }

    @Override
    public V putLast(String key, V value) {
        return put(key, value);
    }

    private void addLastNode(Node<V> newNode) {
        var currentTail = (Node<V>) TAIL_HANDLE.getAcquire(this);

        if (currentTail == null) {
            if (HEAD_HANDLE.compareAndSet(this, null, newNode)) {
                TAIL_HANDLE.compareAndSet(this, null, newNode);
                return;
            }
        } else {
            var tailNext = (Node<V>) Node.NEXT.getAcquire(currentTail);
            var tailMarked = (boolean) Node.MARKED.getAcquire(currentTail);

            if (tailNext == null && !tailMarked) {
                Node.PREV.setRelease(newNode, currentTail);

                if (Node.NEXT.compareAndSet(currentTail, null, newNode)) {
                    TAIL_HANDLE.compareAndSet(this, currentTail, newNode);
                    return;
                }
            }
        }

        while (true) {
            currentTail = (Node<V>) TAIL_HANDLE.getAcquire(this);

            if (currentTail == null) {
                if (HEAD_HANDLE.compareAndSet(this, null, newNode)) {
                    TAIL_HANDLE.compareAndSet(this, null, newNode);
                    return;
                }
                continue;
            }

            var tailNext = (Node<V>) Node.NEXT.getVolatile(currentTail);
            var tailMarked = (boolean) Node.MARKED.getVolatile(currentTail);

            if (tailMarked) {
                helpRemoveTail(currentTail);
                continue;
            }

            Node.PREV.setRelease(newNode, currentTail);

            if (Node.NEXT.compareAndSet(currentTail, tailNext, newNode)) {
                TAIL_HANDLE.compareAndSet(this, currentTail, newNode);
                return;
            }

            if (tailNext != null) {
                TAIL_HANDLE.compareAndSet(this, currentTail, tailNext);
            }
        }
    }

    @Override
    public V putFirst(String key, V value) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }

        var existingNode = keyToNode.get(key);
        if (existingNode != null) {
            return (V) Node.VALUE.getAndSet(existingNode, value);
        }

        var newNode = new Node<>(key, value);
        var existing = keyToNode.putIfAbsent(key, newNode);
        if (existing != null) {
            return (V) Node.VALUE.getAndSet(existing, value);
        }

        var currentHead = (Node<V>) HEAD_HANDLE.getAcquire(this);

        if (currentHead == null) {
            if (HEAD_HANDLE.compareAndSet(this, null, newNode)) {
                TAIL_HANDLE.compareAndSet(this, null, newNode);
                return null;
            }
        } else {
            var headMarked = (boolean) Node.MARKED.getAcquire(currentHead);

            if (!headMarked) {
                Node.NEXT.setRelease(newNode, currentHead);

                if (HEAD_HANDLE.compareAndSet(this, currentHead, newNode)) {
                    Node.PREV.compareAndSet(currentHead, null, newNode);
                    return null;
                }
            }
        }

        while (true) {
            currentHead = (Node<V>) HEAD_HANDLE.getAcquire(this);

            if (currentHead == null) {
                if (HEAD_HANDLE.compareAndSet(this, null, newNode)) {
                    TAIL_HANDLE.compareAndSet(this, null, newNode);
                    return null;
                }
                continue;
            }

            var headMarked = (boolean) Node.MARKED.getVolatile(currentHead);

            if (headMarked) {
                helpRemoveHead(currentHead);
                continue;
            }

            Node.NEXT.setRelease(newNode, currentHead);

            if (HEAD_HANDLE.compareAndSet(this, currentHead, newNode)) {
                Node.PREV.compareAndSet(currentHead, null, newNode);
                return null;
            }
        }
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            return null;
        }

        var node = keyToNode.get(key);
        if (node == null) {
            return null;
        }

        var marked = (boolean) Node.MARKED.getVolatile(node);
        return marked ? null : node.value;
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public V remove(Object key) {
        if (key == null) {
            return null;
        }

        var node = keyToNode.get(key);
        if (node == null) {
            return null;
        }

        if (removeNode(node)) {
            return node.value;
        }
        return null;
    }

    private boolean removeNode(Node<V> node) {
        var marked = (boolean) Node.MARKED.compareAndSet(node, false, true);
        if (!marked) {
            return false;
        }

        keyToNode.remove(node.key, node);

        var prev = (Node<V>) Node.PREV.getVolatile(node);
        var next = (Node<V>) Node.NEXT.getVolatile(node);

        if (prev == null) {
            if (next != null) {
                Node.PREV.compareAndSet(next, node, null);
                HEAD_HANDLE.compareAndSet(this, node, next);
            } else {
                HEAD_HANDLE.compareAndSet(this, node, null);
                TAIL_HANDLE.compareAndSet(this, node, null);
            }
        } else if (next == null) {
            Node.NEXT.compareAndSet(prev, node, null);
            TAIL_HANDLE.compareAndSet(this, node, prev);
        } else {
            Node.NEXT.compareAndSet(prev, node, next);
            Node.PREV.compareAndSet(next, node, prev);
        }

        return true;
    }

    private void helpRemoveHead(Node<V> markedHead) {
        var next = (Node<V>) Node.NEXT.getVolatile(markedHead);

        if (next != null) {
            Node.PREV.compareAndSet(next, markedHead, null);
            HEAD_HANDLE.compareAndSet(this, markedHead, next);
        } else {
            HEAD_HANDLE.compareAndSet(this, markedHead, null);
            TAIL_HANDLE.compareAndSet(this, markedHead, null);
        }
    }

    private void helpRemoveTail(Node<V> markedTail) {
        var prev = (Node<V>) Node.PREV.getVolatile(markedTail);

        if (prev != null) {
            Node.NEXT.compareAndSet(prev, markedTail, null);
            TAIL_HANDLE.compareAndSet(this, markedTail, prev);
        } else {
            HEAD_HANDLE.compareAndSet(this, markedTail, null);
            TAIL_HANDLE.compareAndSet(this, markedTail, null);
        }
    }

    @Override
    public Entry<String, V> pollFirstEntry() {
        while (true) {
            var head = (Node<V>) HEAD_HANDLE.getAcquire(this);
            if (head == null) {
                return null;
            }

            var marked = (boolean) Node.MARKED.getVolatile(head);
            if (marked) {
                helpRemoveHead(head);
                continue;
            }

            if (removeNode(head)) {
                return new AbstractMap.SimpleImmutableEntry<>(head.key, (V) head.value);
            }
        }
    }

    @Override
    public Entry<String, V> pollLastEntry() {
        while (true) {
            var tail = (Node<V>) TAIL_HANDLE.getAcquire(this);
            if (tail == null) {
                return null;
            }

            var marked = (boolean) Node.MARKED.getVolatile(tail);
            if (marked) {
                helpRemoveTail(tail);
                continue;
            }

            if (removeNode(tail)) {
                return new AbstractMap.SimpleImmutableEntry<>(tail.key, (V) tail.value);
            }
        }
    }

    @Override
    public Entry<String, V> firstEntry() {
        var current = (Node<V>) HEAD_HANDLE.getAcquire(this);
        while (current != null) {
            var marked = (boolean) Node.MARKED.getVolatile(current);
            if (!marked) {
                return new AbstractMap.SimpleImmutableEntry<>(current.key, (V) current.value);
            }
            current = (Node<V>) Node.NEXT.getVolatile(current);
        }
        return null;
    }

    @Override
    public Entry<String, V> lastEntry() {
        var current = (Node<V>) TAIL_HANDLE.getAcquire(this);
        while (current != null) {
            var marked = (boolean) Node.MARKED.getVolatile(current);
            if (!marked) {
                return new AbstractMap.SimpleImmutableEntry<>(current.key, (V) current.value);
            }
            current = (Node<V>) Node.PREV.getVolatile(current);
        }
        return null;
    }

    @Override
    public int size() {
        return keyToNode.size();
    }

    @Override
    public boolean isEmpty() {
        return keyToNode.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return keyToNode.containsKey(key);
    }

    @Override
    public void clear() {
        keyToNode.clear();
        HEAD_HANDLE.setRelease(this, null);
        TAIL_HANDLE.setRelease(this, null);
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        return new EntrySetView();
    }

    private final class EntrySetView extends AbstractSet<Entry<String, V>> {
        @Override
        public Iterator<Entry<String, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return MessageInfoContainer.this.size();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry<?, ?> entry)) {
                return false;
            }
            var value = MessageInfoContainer.this.get(entry.getKey());
            return value != null && value.equals(entry.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry<?, ?> entry)) {
                return false;
            }
            return MessageInfoContainer.this.remove(entry.getKey()) != null;
        }

        @Override
        public void clear() {
            MessageInfoContainer.this.clear();
        }
    }

    private final class EntryIterator implements Iterator<Entry<String, V>> {
        private Node<V> current;
        private Node<V> lastReturned;
        private Entry<String, V> nextEntry;

        EntryIterator() {
            this.current = (Node<V>) HEAD_HANDLE.getAcquire(MessageInfoContainer.this);
            advance();
        }

        private void advance() {
            while (current != null) {
                var marked = (boolean) Node.MARKED.getAcquire(current);
                if (!marked) {
                    nextEntry = new AbstractMap.SimpleImmutableEntry<>(current.key, (V) current.value);
                    return;
                }
                current = (Node<V>) Node.NEXT.getAcquire(current);
            }
            nextEntry = null;
        }

        @Override
        public boolean hasNext() {
            return nextEntry != null;
        }

        @Override
        public Entry<String, V> next() {
            if (nextEntry == null) {
                throw new NoSuchElementException();
            }

            var entry = nextEntry;
            lastReturned = current;
            current = (Node<V>) Node.NEXT.getAcquire(current);
            advance();
            return entry;
        }

        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            removeNode(lastReturned);
            lastReturned = null;
        }
    }

    private final class DescendingEntryIterator implements Iterator<Entry<String, V>> {
        private Node<V> current;
        private Node<V> lastReturned;
        private Entry<String, V> nextEntry;

        DescendingEntryIterator() {
            this.current = (Node<V>) TAIL_HANDLE.getAcquire(MessageInfoContainer.this);
            advance();
        }

        private void advance() {
            while (current != null) {
                var marked = (boolean) Node.MARKED.getAcquire(current);
                if (!marked) {
                    nextEntry = new AbstractMap.SimpleImmutableEntry<>(current.key, (V) current.value);
                    return;
                }
                current = (Node<V>) Node.PREV.getAcquire(current);
            }
            nextEntry = null;
        }

        @Override
        public boolean hasNext() {
            return nextEntry != null;
        }

        @Override
        public Entry<String, V> next() {
            if (nextEntry == null) {
                throw new NoSuchElementException();
            }

            var entry = nextEntry;
            lastReturned = current;
            current = (Node<V>) Node.PREV.getAcquire(current);
            advance();
            return entry;
        }

        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            removeNode(lastReturned);
            lastReturned = null;
        }
    }

    @Override
    public SequencedMap<String, V> reversed() {
        return new ReversedView();
    }

    private final class ReversedView extends AbstractMap<String, V> implements SequencedMap<String, V> {
        @Override
        public V putFirst(String key, V value) {
            return MessageInfoContainer.this.putLast(key, value);
        }

        @Override
        public V putLast(String key, V value) {
            return MessageInfoContainer.this.putFirst(key, value);
        }

        @Override
        public Entry<String, V> firstEntry() {
            return MessageInfoContainer.this.lastEntry();
        }

        @Override
        public Entry<String, V> lastEntry() {
            return MessageInfoContainer.this.firstEntry();
        }

        @Override
        public Entry<String, V> pollFirstEntry() {
            return MessageInfoContainer.this.pollLastEntry();
        }

        @Override
        public Entry<String, V> pollLastEntry() {
            return MessageInfoContainer.this.pollFirstEntry();
        }

        @Override
        public V put(String key, V value) {
            return MessageInfoContainer.this.putFirst(key, value);
        }

        @Override
        public V get(Object key) {
            return MessageInfoContainer.this.get(key);
        }

        @Override
        public V remove(Object key) {
            return MessageInfoContainer.this.remove(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return MessageInfoContainer.this.containsKey(key);
        }

        @Override
        public int size() {
            return MessageInfoContainer.this.size();
        }

        @Override
        public boolean isEmpty() {
            return MessageInfoContainer.this.isEmpty();
        }

        @Override
        public void clear() {
            MessageInfoContainer.this.clear();
        }

        @Override
        public Set<Entry<String, V>> entrySet() {
            return new ReversedEntrySetView();
        }

        @Override
        public SequencedMap<String, V> reversed() {
            return MessageInfoContainer.this;
        }

        private final class ReversedEntrySetView extends AbstractSet<Entry<String, V>> {
            @Override
            public Iterator<Entry<String, V>> iterator() {
                return new DescendingEntryIterator();
            }

            @Override
            public int size() {
                return MessageInfoContainer.this.size();
            }
        }
    }
}