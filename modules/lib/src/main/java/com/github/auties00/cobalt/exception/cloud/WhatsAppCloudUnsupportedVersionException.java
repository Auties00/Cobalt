package com.github.auties00.cobalt.exception.cloud;

import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;

/**
 * Thrown when an operation requires a newer Cloud API version than the one the client is configured
 * to target.
 *
 * <p>Some Cloud API operations were introduced in a specific Graph API version and cannot be served
 * by an older versioned graph base. When the client targets a version older than an operation's
 * minimum, the call is rejected locally with this exception before any request is sent, carrying the
 * rejected operation's name together with the required and configured versions. The failure is scoped
 * to the single call; every operation available at the configured version stays usable.
 */
public final class WhatsAppCloudUnsupportedVersionException extends WhatsAppCloudException {
    /**
     * The name of the operation that was rejected.
     */
    private final String operation;

    /**
     * The minimum version the operation requires.
     */
    private final CloudApiVersion requiredVersion;

    /**
     * The version the client is configured to target.
     */
    private final CloudApiVersion configuredVersion;

    /**
     * Constructs a new unsupported-version exception.
     *
     * @param operation         the name of the rejected operation
     * @param requiredVersion   the minimum version the operation requires
     * @param configuredVersion the version the client is configured to target
     */
    public WhatsAppCloudUnsupportedVersionException(String operation, CloudApiVersion requiredVersion,
                                            CloudApiVersion configuredVersion) {
        super("operation '" + operation + "' requires Cloud API version " + requiredVersion.version()
                + " but the client is configured for " + configuredVersion.version());
        this.operation = operation;
        this.requiredVersion = requiredVersion;
        this.configuredVersion = configuredVersion;
    }

    /**
     * Returns the name of the operation that was rejected.
     *
     * @return the operation name
     */
    public String operation() {
        return operation;
    }

    /**
     * Returns the minimum version the operation requires.
     *
     * @return the required version
     */
    public CloudApiVersion requiredVersion() {
        return requiredVersion;
    }

    /**
     * Returns the version the client is configured to target.
     *
     * @return the configured version
     */
    public CloudApiVersion configuredVersion() {
        return configuredVersion;
    }
}
