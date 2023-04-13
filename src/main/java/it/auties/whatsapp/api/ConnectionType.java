package it.auties.whatsapp.api;

/**
 * The constants of this enumerated type describe the various types of connections that can be initialized
 */
public enum ConnectionType {
    /**
     * Creates a new connection using a unique identifier
     * If no uuid is provided, a new connection will be created
     * If the connection doesn't exist, a new one will be created
     */
    NEW,

    /**
     * Creates a new connection from the first session that was serialized
     * If no connection is available, a new one will be created
     */
    FIRST,

    /**
     * Creates a new connection from the last session that was serialized
     * If no connection is available, a new one will be created
     */
    LAST
}