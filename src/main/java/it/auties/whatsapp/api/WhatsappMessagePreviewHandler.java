package it.auties.whatsapp.api;

import it.auties.linkpreview.LinkPreview;
import it.auties.whatsapp.model.message.standard.TextMessage;

import java.util.Objects;

public interface WhatsappMessagePreviewHandler {
    static WhatsappMessagePreviewHandler enabled(boolean allowInference) {
        return textMessage -> LinkPreview.createPreview(textMessage.text()).ifPresent(match -> {
            var uri = match.result().toString();
            if (allowInference && !Objects.equals(match.text(), uri)) {
                textMessage.setText(textMessage.text().replace(match.text(), uri));
            }
            textMessage.setMatchedText(uri);

            textMessage.setTitle(match.result().title());
            textMessage.setDescription(match.result().siteDescription());

            match.result()
                    .images()
                    .stream()
                    .reduce((first, second) -> first.width() * first.height() > second.width() * second.height() ? first : second)
                    .ifPresent(media -> {
                        try (var stream = media.uri().toURL().openStream()) {
                            textMessage.setThumbnail(stream.readAllBytes());
                        } catch (Throwable ignored) {

                        }
                        textMessage.setThumbnailWidth(media.width());
                        textMessage.setThumbnailHeight(media.height());
                    });

            match.result()
                    .videos()
                    .stream()
                    .reduce((first, second) -> first.width() * first.height() > second.width() * second.height() ? first : second)
                    .ifPresentOrElse(
                            media -> {
                                textMessage.setCanonicalUrl(media.uri().toString());
                                textMessage.setThumbnailWidth(media.width());
                                textMessage.setThumbnailHeight(media.height());
                                textMessage.setPreviewType(TextMessage.PreviewType.VIDEO);
                            },
                            () -> {
                                textMessage.setCanonicalUrl(match.result().uri().toString());
                                textMessage.setPreviewType(TextMessage.PreviewType.NONE);
                            }
                    );
        });
    }

    static WhatsappMessagePreviewHandler disable() {
        return _ -> {};
    }

    void attribute(TextMessage message);
}
