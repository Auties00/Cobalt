package com.github.auties00.cobalt.wire.cloud.flow;

import java.util.List;

/**
 * The outcome of uploading the Flow JSON document as the {@code FLOW_JSON} asset of a Flow.
 *
 * <p>The asset upload edge returns whether the upload succeeded together with any validation errors the
 * server found in the document. A successful upload with an empty {@link #validationErrors()} list means
 * the document validated cleanly; a successful upload with non-empty errors means the document was
 * stored but is not yet valid.
 */
public final class CloudFlowJsonUploadResult {
    /**
     * Whether the upload succeeded.
     */
    private final boolean success;

    /**
     * The validation errors the server found in the document.
     */
    private final List<CloudFlowValidationError> validationErrors;

    /**
     * Constructs a new upload result.
     *
     * @param success          whether the upload succeeded
     * @param validationErrors the validation errors, or {@code null} for none
     */
    public CloudFlowJsonUploadResult(boolean success, List<CloudFlowValidationError> validationErrors) {
        this.success = success;
        this.validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
    }

    /**
     * Returns whether the upload succeeded.
     *
     * @return {@code true} if the upload succeeded
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns the validation errors the server found in the document.
     *
     * @return an unmodifiable list of validation errors, empty when the document validated cleanly
     */
    public List<CloudFlowValidationError> validationErrors() {
        return validationErrors;
    }
}
