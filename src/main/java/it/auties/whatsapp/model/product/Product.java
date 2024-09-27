package it.auties.whatsapp.model.product;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.standard.ImageMessage;


/**
 * A model class that represents a product
 */
@ProtobufMessage(name = "Message.ListMessage.Product")
public record Product(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        ImageMessage image,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String id,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String title,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String description,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String currencyCode,
        @ProtobufProperty(index = 6, type = ProtobufType.INT64)
        long priceAmount1000,
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        String retailerId,
        @ProtobufProperty(index = 8, type = ProtobufType.STRING)
        String url,
        @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
        int productImageCount,
        @ProtobufProperty(index = 11, type = ProtobufType.STRING)
        String firstImageId,
        @ProtobufProperty(index = 12, type = ProtobufType.INT64)
        long salePriceAmount1000
) {

}
