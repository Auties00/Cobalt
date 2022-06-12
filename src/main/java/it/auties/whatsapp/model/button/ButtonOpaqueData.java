package it.auties.whatsapp.model.button;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ButtonOpaqueData implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING)
    private String body;

    @ProtobufProperty(index = 3, type = STRING)
    private String caption;

    @ProtobufProperty(index = 4, type = STRING)
    private String clientUrl;

    @ProtobufProperty(index = 5, type = DOUBLE)
    private Double lng;

    @ProtobufProperty(index = 7, type = DOUBLE)
    private Double lat;

    @ProtobufProperty(index = 8, type = INT32)
    private int paymentAmount1000;

    @ProtobufProperty(index = 9, type = STRING)
    private String paymentNoteMsgBody;

    @ProtobufProperty(index = 10, type = STRING)
    private String canonicalUrl;

    @ProtobufProperty(index = 11, type = STRING)
    private String matchedText;

    @ProtobufProperty(index = 12, type = STRING)
    private String title;

    @ProtobufProperty(index = 13, type = STRING)
    private String description;
}
