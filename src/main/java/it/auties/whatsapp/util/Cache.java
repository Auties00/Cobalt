package it.auties.whatsapp.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.delayedExecutor;

@Value
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class Cache<V> extends LinkedList<V> {
    public static final int DEFAULT_TIMEOUT = 300;

    Executor executor;

    public Cache(){
        this(DEFAULT_TIMEOUT);
    }

    public Cache(long timeout){
        this.executor = delayedExecutor(timeout, TimeUnit.SECONDS);
    }

    @Override
    public boolean add(V element) {
        var result = super.add(element);
        if(result){
            scheduleRemoval(element);
        }

        return result;
    }

    @Override
    public void add(int index, V element) {
        super.add(index, element);
        scheduleRemoval(element);
    }

    @Override
    public boolean addAll(Collection<? extends V> collection) {
        var result = super.addAll(collection);
        if(result){
            collection.forEach(this::scheduleRemoval);
        }

        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> collection) {
        var result = super.addAll(index, collection);
        if(result){
            collection.forEach(this::scheduleRemoval);
        }

        return result;
    }

    @Override
    public void addFirst(V element) {
        super.addFirst(element);
        scheduleRemoval(element);
    }

    @Override
    public void addLast(V element) {
        super.addLast(element);
        scheduleRemoval(element);
    }

    public void scheduleRemoval(V key){
        executor.execute(() -> remove(key));
    }
}
