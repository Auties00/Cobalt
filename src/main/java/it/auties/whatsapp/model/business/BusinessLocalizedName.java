package it.auties.whatsapp.model.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a time a localizable name
 */
@ProtobufMessage(name = "LocalizedName")
public final class BusinessLocalizedName {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String lg;
    
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String lc;
    
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String name;

    BusinessLocalizedName(String lg, String lc, String name) {
        this.lg = lg;
        this.lc = lc;
        this.name = name;
    }

    public Optional<String> lg() {
        return Optional.ofNullable(lg);
    }

    public Optional<String> lc() {
        return Optional.ofNullable(lc);
    }

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessLocalizedName that
                && Objects.equals(lg, that.lg)
                && Objects.equals(lc, that.lc)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lg, lc, name);
    }

    @Override
    public String toString() {
        return "BusinessLocalizedName[" +
                "lg=" + lg +
                ", lc=" + lc +
                ", name=" + name + ']';
    }
}