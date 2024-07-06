package it.auties.whatsapp.util;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConcurrentLinkedSet<E> extends AbstractCollection<E> implements Set<E>, Deque<E> {
    private Node<E> head;
    private Node<E> tail;
    private final ReentrantLock lock;
    private final Set<Integer> hashes;

    public ConcurrentLinkedSet() {
        this.hashes = ConcurrentHashMap.newKeySet();
        this.lock = new ReentrantLock(true);
    }

    @Override
    public void push(E e) {
        add(e);
    }

    @Override
    public boolean offer(E e) {
        return add(e);
    }

    @Override
    public boolean offerLast(E e) {
        return add(e);
    }

    @Override
    public void addLast(E message) {
        add(message);
    }

    @Override
    public boolean add(E e) {
        try {
            lock.lock();
            var hash = Objects.hashCode(e);
            if (hashes.contains(hash)) {
                return false;
            }

            var newNode = new Node<>(e);
            if (tail == null) {
                head = newNode;
            } else {
                tail.next = newNode;
                newNode.prev = tail;
            }
            tail = newNode;
            hashes.add(hash);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    @Override
    public void addFirst(E message) {
        try {
            lock.lock();
            var hash = Objects.hashCode(message);
            if (hashes.contains(hash)) {
                return;
            }

            var newNode = new Node<>(message);
            if (head == null) {
                tail = newNode;
            } else {
                head.prev = newNode;
                newNode.next = head;
            }
            head = newNode;
            hashes.add(hash);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E removeLast() {
        return remove();
    }

    @Override
    public boolean remove(Object o) {
        try {
            lock.lock();
            var hash = Objects.hashCode(o);
            if (!hashes.contains(hash)) {
                return false;
            }

            var node = head;
            while (node != null) {
                if (node.item.equals(o)) {
                    removeNode(node, hash);
                    return true;
                }
                node = node.next;
            }

            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        try {
            if (collection.isEmpty()) {
                return true;
            }

            lock.lock();
            var hashCodes = collection.stream()
                    .map(Objects::hashCode)
                    .collect(Collectors.toSet());
            var node = head;
            while (node != null && !hashCodes.isEmpty()) {
                var hash = Objects.hashCode(node.item);
                if (hashCodes.remove(hash)) {
                    removeNode(node, hash);
                }
                node = node.next;
            }
            return hashCodes.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        try {
            lock.lock();
            var node = head;
            while (node != null) {
                if (node.item.equals(o)) {
                    var hash = Objects.hashCode(node.item);
                    removeNode(node, hash);
                    return true;
                }
                node = node.next;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        try {
            lock.lock();
            var node = tail;
            while (node != null) {
                if (filter.test(node.item)) {
                    var hash = Objects.hashCode(node.item);
                    removeNode(node, hash);
                    return true;
                }
                node = node.prev;
            }

            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        try {
            lock.lock();
            var node = tail;
            while (node != null) {
                if (node.item.equals(o)) {
                    var hash = Objects.hashCode(node.item);
                    removeNode(node, hash);
                    return true;
                }
                node = node.prev;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll() {
        return remove();
    }

    @Override
    public E pollLast() {
        return remove();
    }

    @Override
    public E remove() {
        try {
            lock.lock();
            var tailItem = tail;
            if (tailItem == null) {
                return null;
            }

            if (head == tail) {
                head = tailItem.prev;
            }

            var result = tail.item;
            hashes.remove(Objects.hashCode(result));
            tail = tailItem.prev;
            return result;
        } finally {
            lock.unlock();
        }
    }

    private void removeNode(Node<E> node, int hash) {
        try {
            lock.lock();
            if (node == head) {
                if (head == tail) {
                    tail = head.prev;
                }
                head = head.next;
            } else if (node == tail) {
                tail = tail.next;
            } else {
                node.prev.next = node.next;
                node.next.prev = node.prev;
            }
            hashes.remove(hash);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E pollFirst() {
        return removeFirst();
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public E removeFirst() {
        try {
            lock.lock();
            if (head == tail) {
                tail = head.prev;
            }

            var result = head.item;
            hashes.remove(Objects.hashCode(result));
            head = head.next;
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        return hashes.size();
    }

    @Override
    public boolean isEmpty() {
        return head == null;
    }

    @Override
    public boolean contains(Object o) {
        return hashes.contains(Objects.hashCode(o));
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private Node<E> nextNode = head;

            @Override
            public boolean hasNext() {
                return nextNode != null;
            }

            @Override
            public E next() {
                if (nextNode == null) {
                    throw new NoSuchElementException();
                }

                var item = nextNode.item;
                nextNode = nextNode.next;
                return item;
            }
        };
    }

    public Iterator<E> descendingIterator() {
        return new Iterator<>() {
            private Node<E> previousNode = tail;

            @Override
            public boolean hasNext() {
                return previousNode != null;
            }

            @Override
            public E next() {
                if (previousNode == null) {
                    throw new NoSuchElementException();
                }

                var item = previousNode.item;
                previousNode = previousNode.prev;
                return item;
            }
        };
    }


    @Override
    public E element() {
        return peek();
    }

    @Override
    public E peekFirst() {
        return peek();
    }

    @Override
    public E peek() {
        try {
            lock.lock();
            if (head == null) {
                return null;
            }

            return head.item;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E peekLast() {
        try {
            lock.lock();
            if (tail == null) {
                return null;
            }

            return tail.item;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E getFirst() {
        try {
            lock.lock();
            if (head == null) {
                throw new NoSuchElementException();
            }

            return head.item;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E getLast() {
        try {
            lock.lock();
            if (tail == null) {
                throw new NoSuchElementException();
            }

            return tail.item;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        try {
            lock.lock();
            head = tail = null;
            hashes.clear();
        }finally {
            lock.unlock();
        }
    }

    private static class Node<E> {
        final E item;
        Node<E> next;
        Node<E> prev;

        Node(E item) {
            this.item = item;
        }
    }
}
