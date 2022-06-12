package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class HydratedFourRowTemplate implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = DocumentMessage.class)
    private DocumentMessage documentMessage;

    @ProtobufProperty(index = 2, type = STRING)
    private String hydratedTitleText;

    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ImageMessage.class)
    private ImageMessage imageMessage;

    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = VideoMessage.class)
    private VideoMessage videoMessage;

    @ProtobufProperty(index = 5, type = MESSAGE, concreteType = LocationMessage.class)
    private LocationMessage locationMessage;

    @ProtobufProperty(index = 6, type = STRING)
    private String hydratedContentText;

    @ProtobufProperty(index = 7, type = STRING)
    private String hydratedFooterText;

    @ProtobufProperty(index = 8, type = MESSAGE, concreteType = HydratedTemplateButton.class, repeated = true)
    private List<HydratedTemplateButton> hydratedButtons;

    @ProtobufProperty(index = 9, type = STRING)
    private String templateId;

    public Title titleType() {
        if (documentMessage != null)
            return Title.DOCUMENT_MESSAGE;
        if (hydratedTitleText != null)
            return Title.HYDRATED_TITLE_TEXT;
        if (imageMessage != null)
            return Title.IMAGE_MESSAGE;
        if (videoMessage != null)
            return Title.VIDEO_MESSAGE;
        if (locationMessage != null)
            return Title.LOCATION_MESSAGE;
        return Title.UNKNOWN;
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum Title implements ProtobufMessage {
        UNKNOWN(0),
        DOCUMENT_MESSAGE(1),
        HYDRATED_TITLE_TEXT(2),
        IMAGE_MESSAGE(3),
        VIDEO_MESSAGE(4),
        LOCATION_MESSAGE(5);

        @Getter
        private final int index;

        @JsonCreator
        public static Title forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(Title.UNKNOWN);
        }
    }

    public static class HydratedFourRowTemplateBuilder {
        public HydratedFourRowTemplateBuilder hydratedButtons(List<HydratedTemplateButton> hydratedButtons) {
            if (this.hydratedButtons == null)
                this.hydratedButtons = new ArrayList<>();
            this.hydratedButtons.addAll(hydratedButtons);
            return this;
        }
    }
}
