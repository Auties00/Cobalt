package it.auties.whatsapp.util;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConcurrentLinkedHashedDequeue<E> extends AbstractQueue<E> implements Deque<E> {
    private final AtomicReference<Node<E>> head;
    private final AtomicReference<Node<E>> tail;
    private final Set<Integer> hashes;

    public ConcurrentLinkedHashedDequeue() {
        this.head = new AtomicReference<>(null);
        this.tail = new AtomicReference<>(null);
        this.hashes = ConcurrentHashMap.newKeySet();
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
        var hash = Objects.hashCode(e);
        if (hashes.contains(hash)) {
            return false;
        }

        var newNode = new Node<>(e);
        var oldTail = tail.getAndSet(newNode);
        if (oldTail == null) {
            head.set(newNode);
        } else {
            oldTail.next = newNode;
            newNode.prev = oldTail;
        }

        hashes.add(hash);
        return true;
    }

    @Override
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    @Override
    public void addFirst(E message) {
        var hash = Objects.hashCode(message);
        if (hashes.contains(hash)) {
            return;
        }

        var newNode = new Node<>(message);
        var oldHead = head.getAndSet(newNode);
        if (oldHead == null) {
            tail.set(newNode);
        } else {
            oldHead.prev = newNode;
            newNode.next = oldHead;
        }

        hashes.add(hash);
    }

    @Override
    public E removeLast() {
        return remove();
    }

    @Override
    public boolean remove(Object o) {
        var hash = Objects.hashCode(o);
        if (!hashes.contains(hash)) {
            return false;
        }

        var node = head.get();
        while (node != null) {
            if (node.item.equals(o)) {
                removeNode(node, hash);
                return true;
            }
            node = node.next;
        }

        return false;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        var hashCodes = collection.stream()
                .map(Objects::hashCode)
                .collect(Collectors.toUnmodifiableSet());
        var node = head.get();
        while (node != null) {
            var hash = Objects.hashCode(node.item);
            if (hashCodes.contains(hash)) {
                removeNode(node, hash);
                return true;
            }
            node = node.next;
        }

        return false;
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
        var tailItem = tail.get();
        if (tailItem == null) {
            return null;
        }

        var node = tail.getAndSet(tailItem.prev);
        if (node == head.get()) {
            head.compareAndSet(node, tailItem.prev);
        }

        hashes.remove(Objects.hashCode(node.item));
        return node.item;
    }

    private void removeNode(Node<E> node, int hash) {
        if (node == head.get()) {
            var removed = head.getAndSet(head.get().next);
            if (removed == tail.get()) {
                tail.compareAndSet(removed, removed.prev);
            }
        } else if (node == tail.get()) {
            var removed = tail.getAndSet(tail.get().prev);
            if (removed == head.get()) {
                head.compareAndSet(removed, tail.get().prev);
            }
        } else {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }
        hashes.remove(hash);
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
        var node = head.getAndSet(head.get().next);
        if (node == tail.get()) {
            tail.compareAndSet(node, node.prev);
        }
        return node.item;
    }

    @Override
    public int size() {
        return hashes.size();
    }

    @Override
    public boolean isEmpty() {
        return head.get() == null;
    }

    @Override
    public boolean contains(Object o) {
        return hashes.contains(Objects.hashCode(o));
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private Node<E> nextNode = head.get();

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
            private Node<E> previousNode = tail.get();

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
        var headItem = head.get();
        if (headItem == null) {
            return null;
        }

        return headItem.item;
    }

    @Override
    public E peekLast() {
        var tailItem = tail.get();
        if (tailItem == null) {
            return null;
        }

        return tailItem.item;
    }

    @Override
    public E getFirst() {
        var headItem = head.get();
        if (headItem == null) {
           throw new NoSuchElementException();
        }

        return headItem.item;
    }

    @Override
    public E getLast() {
        var tailItem = tail.get();
        if (tailItem == null) {
            throw new NoSuchElementException();
        }

        return tailItem.item;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        var node = head.get();
        while (node != null) {
            if (node.item.equals(o)) {
                var hash = Objects.hashCode(node.item);
                removeNode(node, hash);
                return true;
            }
            node = node.next;
        }
        return false;
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        var node = tail.get();
        while (node != null) {
            if (filter.test(node.item)) {
                var hash = Objects.hashCode(node.item);
                removeNode(node, hash);
                return true;
            }
            node = node.prev;
        }

        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        var node = tail.get();
        while (node != null) {
            if (node.item.equals(o)) {
                var hash = Objects.hashCode(node.item);
                removeNode(node, hash);
                return true;
            }
            node = node.prev;
        }
        return false;
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
