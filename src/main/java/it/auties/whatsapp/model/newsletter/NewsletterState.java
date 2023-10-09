package it.auties.whatsapp.model.newsletter;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public final class NewsletterState {
    private String type;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)

    public NewsletterState(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public NewsletterState setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
