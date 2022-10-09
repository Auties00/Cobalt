package it.auties.whatsapp.model.business;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents a verified name
 */
@AllArgsConstructor
@Data
@Builder(builderMethodName = "newVerifiedNameDetailsBuilder")
@Jacksonized
@Accessors(fluent = true)
public class BusinessVerifiedNameDetails implements ProtobufMessage {
    /**
     * The verified serial
     */
    @ProtobufProperty(index = 1, type = UINT64)
    private long serial;

    /**
     * The issuer of this certificate
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String issuer;

    /**
     * The verified name
     */
    @ProtobufProperty(index = 4, type = STRING)
    private String name;

    /**
     * The localizable names
     */
    @ProtobufProperty(index = 8, type = MESSAGE, implementation = BusinessLocalizedName.class, repeated = true)
    private List<BusinessLocalizedName> localizedNames;

    /**
     * The timestamp when this certificate was issued
     */
    @ProtobufProperty(index = 10, type = UINT64)
    private long issueTime;

    public static class BusinessVerifiedNameDetailsBuilder {
        public BusinessVerifiedNameDetailsBuilder localizedNames(List<BusinessLocalizedName> localizedNames) {
            if (this.localizedNames == null)
                this.localizedNames = new ArrayList<>();
            this.localizedNames.addAll(localizedNames);
            return this;
        }
    }
}
