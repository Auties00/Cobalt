package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.button.InteractiveMessageContent;

import java.util.Objects;

/**
 * A model class that represents a shop
 */
@ProtobufMessage(name = "Message.InteractiveMessage.ShopMessage")
public final class InteractiveShop implements InteractiveMessageContent {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final SurfaceType surfaceType;

    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final int version;

    InteractiveShop(String id, SurfaceType surfaceType, int version) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.surfaceType = Objects.requireNonNull(surfaceType, "surfaceType cannot be null");
        this.version = version;
    }

    public String id() {
        return id;
    }

    public SurfaceType surfaceType() {
        return surfaceType;
    }

    public int version() {
        return version;
    }

    @Override
    public Type contentType() {
        return Type.SHOP;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InteractiveShop that
                && Objects.equals(id, that.id)
                && Objects.equals(surfaceType, that.surfaceType)
                && version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, surfaceType, version);
    }

    @Override
    public String toString() {
        return "InteractiveShop[" +
                "id=" + id + ", " +
                "surfaceType=" + surfaceType + ", " +
                "version=" + version + ']';
    }

    /**
     * The constants of this enumerated type describe the various types of surfaces that a
     * {@link InteractiveShop} can have
     */
    @ProtobufEnum(name = "Message.InteractiveMessage.ShopMessage.Surface")
    public enum SurfaceType {
        /**
         * Unknown
         */
        UNKNOWN_SURFACE(0),
        /**
         * Facebook
         */
        FACEBOOK(1),
        /**
         * Instagram
         */
        INSTAGRAM(2),
        /**
         * Whatsapp
         */
        WHATSAPP(3);

        final int index;

        SurfaceType(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}