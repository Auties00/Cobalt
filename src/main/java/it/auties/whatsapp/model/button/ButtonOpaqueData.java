package it.auties.whatsapp.model.button;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents data about a button
 */
@AllArgsConstructor
@Data
@Builder(builderMethodName = "newButtonOpaqueDataBuilder")
@Jacksonized
@Accessors(fluent = true)
public class ButtonOpaqueData implements ProtobufMessage {
    /**
     * The body of the button
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String body;

    /**
     * The caption of the button
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String caption;

    /**
     * The client url of the button
     */
    @ProtobufProperty(index = 4, type = STRING)
    private String clientUrl;

    /**
     * The longitude of the button
     */
    @ProtobufProperty(index = 5, type = DOUBLE)
    private double longitude;

    /**
     * The latitude of the button
     */
    @ProtobufProperty(index = 7, type = DOUBLE)
    private double latitude;

    /**
     * The payment amount of the button
     */
    @ProtobufProperty(index = 8, type = INT32)
    private int paymentAmount1000;

    /**
     * The note attached to the payment of the button
     */
    @ProtobufProperty(index = 9, type = STRING)
    private String paymentNote;

    /**
     * The canonical url of the button
     */
    @ProtobufProperty(index = 10, type = STRING)
    private String canonicalUrl;

    /**
     * The matched text of the button
     */
    @ProtobufProperty(index = 11, type = STRING)
    private String matchedText;

    /**
     * The title of the button
     */
    @ProtobufProperty(index = 12, type = STRING)
    private String title;

    /**
     * The description of the button
     */
    @ProtobufProperty(index = 13, type = STRING)
    private String description;
}
