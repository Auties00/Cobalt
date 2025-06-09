package it.auties.whatsapp.model.media;

public record MediaDimensions(int width, int height) {
    private static final MediaDimensions DEFAULT = new MediaDimensions(128, 128);

    public static MediaDimensions defaultDimensions() {
        return DEFAULT;
    }
}
