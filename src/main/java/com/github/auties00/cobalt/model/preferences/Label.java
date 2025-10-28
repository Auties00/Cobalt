package com.github.auties00.cobalt.model.preferences;

import com.github.auties00.cobalt.model.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.SequencedCollection;
import java.util.SequencedSet;

@ProtobufMessage
public final class Label {
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final int id;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    int color;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    SequencedSet<Jid> assignments;

    Label(int id, String name, int color, SequencedSet<Jid> assignments) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.assignments = assignments;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int color() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public SequencedCollection<Jid> assignments() {
        return Collections.unmodifiableSequencedCollection(assignments);
    }

    public void addAssignment(Jid jid) {
        assignments.add(jid);
    }

    public boolean removeAssignment(Jid jid) {
        return assignments.remove(jid);
    }
}
