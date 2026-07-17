package com.github.auties00.cobalt.meta.annotation;

import com.github.auties00.cobalt.meta.model.WhatsAppCloudApiVersion;

import java.lang.annotation.*;

/**
 * Declares that the annotated method implements a Meta WhatsApp Cloud API operation and records the
 * minimum Graph API version that operation requires.
 *
 * <p>Unlike the WhatsApp Web and Mobile provenance annotations, the Cloud API surface has no
 * reverse-engineered source counterpart; its contract source is Meta's public Cloud API documentation.
 * This marker therefore carries only the version floor: the operation is unavailable on a Graph API
 * version older than {@link #since()}.
 *
 * <p>The marker is advisory documentation and is not retained at runtime; the authoritative gate is the
 * runtime version check performed by the Cloud API client implementation, which rejects the call when
 * the configured version is older than the declared minimum.
 *
 * <p>Example:
 * <pre>{@code
 * @WhatsAppCloudMethod(since = CloudApiVersion.V23_0)
 * CloudCallPermission getUserCallPermissions(JidProvider user);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface WhatsAppCloudMethod {
    /**
     * Returns the minimum Graph API version this operation requires.
     *
     * @return the minimum version
     */
    WhatsAppCloudApiVersion since();
}
