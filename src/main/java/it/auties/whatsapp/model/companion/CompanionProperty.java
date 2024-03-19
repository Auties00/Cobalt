package it.auties.whatsapp.model.companion;

/**
 * A model that represents an immutable property associated with the linked device
 *
 * @param name         the name of the property
 * @param code         an id that represents the property
 * @param value        the value associated with this property
 * @param defaultValue the default value for this property
 */
public record CompanionProperty(String name, double code, Object value, Object defaultValue) {

}
