package it.auties.whatsapp.model.product;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents a product
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class Product implements ProtobufMessage {
    /**
     * The image of the product
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = ImageMessage.class)
    private ImageMessage image;

    /**
     * The id of the product
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String id;

    /**
     * The title of the product
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String title;

    /**
     * The description of the product
     */
    @ProtobufProperty(index = 4, type = STRING)
    private String description;

    /**
     * The currency used to buy this product
     */
    @ProtobufProperty(index = 5, type = STRING)
    private String currencyCode;

    /**
     * The price of this product
     */
    @ProtobufProperty(index = 6, type = INT64)
    private long priceAmount1000;

    /**
     * The id of the seller
     */
    @ProtobufProperty(index = 7, type = STRING)
    private String retailerId;

    /**
     * The url of this product
     */
    @ProtobufProperty(index = 8, type = STRING)
    private String url;

    /**
     * The number of images for this product
     */
    @ProtobufProperty(index = 9, type = UINT32)
    private int productImageCount;

    /**
     * The id of the first image for this product
     */
    @ProtobufProperty(index = 11, type = STRING)
    private String firstImageId;

    /**
     * The sale price for this product
     */
    @ProtobufProperty(index = 12, type = INT64)
    private long salePriceAmount1000;
}
