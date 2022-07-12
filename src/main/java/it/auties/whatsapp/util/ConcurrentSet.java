package it.auties.whatsapp.util;

import lombok.NonNull;
import lombok.experimental.Delegate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentSet<T> extends HashSet<T> {
    @Delegate
    @NonNull
    private final Set<T> internal;

    public ConcurrentSet() {
        this.internal = ConcurrentHashMap.newKeySet();
    }

    public ConcurrentSet(Collection<? extends T> c) {
        this();
        internal.addAll(c);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return Objects.equals(internal, o);
    }

    @Override
    public int hashCode() {
        return internal.hashCode();
    }
}
