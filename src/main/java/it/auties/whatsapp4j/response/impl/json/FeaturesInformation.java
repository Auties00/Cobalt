package it.auties.whatsapp4j.response.impl.json;

/**
 * A json model that contains information about the features available to the user linked with this session
 * This record should only be used by {@link UserInformationResponse}
 *
 * @param url   a flag used to determine whether URLs can be shown???
 * @param flags unknown
 */
public record FeaturesInformation(boolean url, String flags) {
}
