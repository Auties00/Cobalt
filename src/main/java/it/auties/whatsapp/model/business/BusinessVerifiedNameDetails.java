package it.auties.whatsapp.model.business;

import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class BusinessVerifiedNameDetails {
    @ProtobufProperty(index = 1, type = UINT64)
    private long serial;

    @ProtobufProperty(index = 2, type = STRING)
    private String issuer;

    @ProtobufProperty(index = 4, type = STRING)
    private String verifiedName;

    @ProtobufProperty(index = 8, type = MESSAGE, concreteType = BusinessLocalizedName.class, repeated = true)
    private List<BusinessLocalizedName> localizedNames;

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
