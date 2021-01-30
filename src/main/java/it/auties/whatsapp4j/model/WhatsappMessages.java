package it.auties.whatsapp4j.model;

import lombok.NoArgsConstructor;

import java.util.*;

import java.util.Comparator;

@NoArgsConstructor
public class WhatsappMessages extends ArrayList<WhatsappMessage> {
    private static final Comparator<WhatsappMessage> ENTRY_COMPARATOR = Comparator.comparingLong(message -> message.info().getMessageTimestamp());
    public WhatsappMessages(WhatsappMessage message) {
        add(message);
    }

    @Override
    public boolean add(WhatsappMessage message) {
        var initialSize = size();
        var insertionPoint = Collections.binarySearch(this, message, ENTRY_COMPARATOR);
        super.add(insertionPoint > -1 ? insertionPoint : -insertionPoint - 1, message);
        return size() != initialSize;
    }

    @Override
    public boolean addAll(Collection<? extends WhatsappMessage> c) {
        throw new UnsupportedOperationException();
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