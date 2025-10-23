package com.github.auties00.cobalt.exception;

import java.util.Optional;

/**
 * Exception thrown when errors occur during Web App State Sync operations.
 * <p>
 * This exception provides detailed error information including the error type,
 * the collection involved, and the underlying cause. It supports both fatal
 * and retryable errors based on the error type.
 */
public final class WebAppStateSyncException extends RuntimeException {
    private final ErrorType errorType;
    private final String collectionName;
    
    /**
     * Constructs a new AppStateSyncException with the specified detail message and error type.
     * 
     * @param message the detail message
     * @param errorType the type of error
     * @param collectionName the collection name associated with the error (can be null)
     */
    public WebAppStateSyncException(String message, ErrorType errorType, String collectionName) {
        super(message);
        this.errorType = errorType;
        this.collectionName = collectionName;
    }
    
    /**
     * Constructs a new AppStateSyncException with the specified detail message, error type, and cause.
     * 
     * @param message the detail message
     * @param errorType the type of error
     * @param collectionName the collection name associated with the error (can be null)
     * @param cause the cause of the exception
     */
    public WebAppStateSyncException(String message, ErrorType errorType, String collectionName, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.collectionName = collectionName;
    }
    
    /**
     * Returns the error type of this exception.
     * 
     * @return the error type
     */
    public ErrorType errorType() {
        return errorType;
    }
    
    /**
     * Returns the collection name associated with this error, if any.
     * 
     * @return an Optional containing the collection name, or empty if not applicable
     */
    public Optional<String> collectionName() {
        return Optional.ofNullable(collectionName);
    }
    
    /**
     * Returns whether this exception represents a fatal error.
     * 
     * @return true if this is a fatal error, false otherwise
     */
    public boolean isFatal() {
        return errorType.isFatal();
    }

    @Override
    public String toString() {
        return "AppStateSyncException{" +
               "errorType=" + errorType +
               ", collectionName='" + collectionName + '\'' +
               ", message='" + getMessage() + '\'' +
               ", fatal=" + isFatal() +
               '}';
    }

    /**
     * Enumeration of error types that can occur during App State Sync operations.
     * These types correspond to the error types identified in the WAWebSync modules.
     */
    public enum ErrorType {
        /** Failed to deserialize external blob reference protobuf */
        EXTERNAL_BLOB_REFERENCE_DESERIALIZATION_FAILED(true),
        /** Failed to deserialize snapshot protobuf */
        SNAPSHOT_DESERIALIZATION_FAILED(true),
        /** Failed to deserialize patch protobuf */
        PATCH_DESERIALIZATION_FAILED(true),
        /** Failed to deserialize mutations protobuf */
        MUTATIONS_DESERIALIZATION_FAILED(true),
        /** Failed to deserialize action data protobuf */
        ACTION_DATA_DESERIALIZATION_FAILED(true),
        /** Failed to serialize patch protobuf */
        PATCH_SERIALIZATION_FAILED(true),
        /** Failed to serialize mutations protobuf */
        MUTATIONS_SERIALIZATION_FAILED(true),
        /** Failed to serialize action data protobuf */
        ACTION_DATA_SERIALIZATION_FAILED(true),
        /** Missing required action index field */
        MISSING_ACTION_INDEX(true),
        /** Missing required action version field */
        MISSING_ACTION_VERSION(true),
        /** Encryption operation failed */
        ENCRYPTION_FAILED(true),
        /** Decryption operation failed */
        DECRYPTION_FAILED(true),
        /** Invalid node structure received */
        INVALID_NODE_STRUCTURE(false),
        /** Network error occurred (retryable) */
        NETWORK_ERROR(false);

        private final boolean fatal;

        ErrorType(boolean fatal) {
            this.fatal = fatal;
        }

        /**
         * Returns whether this error type represents a fatal error.
         * Fatal errors should stop sync operations, while non-fatal errors can be retried.
         *
         * @return true if this is a fatal error, false otherwise
         */
        public boolean isFatal() {
            return fatal;
        }
    }
}