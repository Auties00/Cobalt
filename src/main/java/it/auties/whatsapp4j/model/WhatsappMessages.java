package it.auties.whatsapp4j.model;

import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

@NoArgsConstructor
public class WhatsappMessages extends ArrayList<WhatsappMessage> {
    private static final Comparator<WhatsappMessage> ENTRY_COMPARATOR = Comparator.comparingLong(message -> message.info().getMessageTimestamp());
    public WhatsappMessages(@NotNull WhatsappMessage message) {
        add(message);
    }

    @Override
    public boolean add(@NotNull WhatsappMessage message) {
        var initialSize = size();
        var insertionPoint = Collections.binarySearch(this, message, ENTRY_COMPARATOR);
        super.add(insertionPoint > -1 ? insertionPoint : -insertionPoint - 1, message);
        return size() != initialSize;
    }

    public boolean addOrReplace(@NotNull WhatsappMessage message){
        var result = contains(message);
        remove(message);
        add(message);
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends WhatsappMessage> c) {
        return c.stream().map(this::add).reduce(true, (a, b) -> a && b);
    }

    @Override
    public void add(int index, WhatsappMessage element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends WhatsappMessage> c) {
        throw new UnsupportedOperationException();
    }
}