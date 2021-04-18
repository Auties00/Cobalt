package it.auties.whatsapp4j.response.impl;


import java.util.Objects;

/**
 * A json model that contains information about the features available to the user linked with this session
 * This record should only be used by {@link UserInformationResponse}
 *
 */
public final class FeaturesInformation {
    private final boolean url;
    private final String flags;

    /**
     * @param url a flag used to determine whether URLs can be shown???
     * @param flags unknown
     */
    public FeaturesInformation(boolean url, String flags) {
        this.url = url;
        this.flags = flags;
    }

    public boolean url() {
        return url;
    }

    public String flags() {
        return flags;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FeaturesInformation) obj;
        return this.url == that.url &&
                Objects.equals(this.flags, that.flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, flags);
    }

    @Override
    public String toString() {
        return "FeaturesInformation[" +
                "url=" + url + ", " +
                "flags=" + flags + ']';
    }


}
