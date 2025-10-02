package it.auties.whatsapp.stream.node;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.io.node.Node;

import java.util.Set;

public abstract class AbstractNodeHandler {
    final Whatsapp whatsapp;
    final Set<String> descriptions;

    AbstractNodeHandler(Whatsapp whatsapp, String... descriptions) {
        this.whatsapp = whatsapp;
        this.descriptions = Set.of(descriptions);
    }

    public abstract void handle(Node node);

    public Set<String> descriptions() {
        return descriptions;
    }

    public void dispose() {

    }
}
