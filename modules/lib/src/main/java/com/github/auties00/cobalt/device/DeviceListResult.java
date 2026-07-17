package com.github.auties00.cobalt.device;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.DeviceConstants;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceInfo;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceList;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceListBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed outcome of parsing one user's slot inside a USync device-list response.
 *
 * <p>This drives the multi-device fanout path. After a USync round-trip resolves a chat's
 * recipients, the parser walks each {@code <user>} child and emits one of the three variants:
 * {@link Full} for a fresh device list with a signed key-index list, {@link Omitted} when the
 * server confirms the local device-hash already matches, and {@link Error} when the server refused
 * that user's slot or the entire batch. Downstream code switches on the variant before persisting,
 * fanning out, or surfacing the failure.
 *
 * @implNote
 * This implementation flattens the three internal branches of
 * {@code WAWebAdvHandlerApi.handleADVDeviceSyncResult} into a single sealed hierarchy so callers
 * can pattern-match instead of inspecting object shape.
 */
@WhatsAppWebModule(moduleName = "WAWebAdvHandlerApi")
public sealed interface DeviceListResult {

    /**
     * Returns the user JID this result refers to.
     *
     * <p>The device-list applier uses this to address each variant's downstream effect (cache
     * replacement for {@link Full}, expected-timestamp refresh for {@link Omitted}, error reporting
     * for a per-user {@link Error}). The value is empty only for a global batch-fatal {@link Error}
     * that is not bound to any user.
     *
     * @implSpec
     * Implementations must return a present value for {@link Full} and {@link Omitted}, and an
     * empty value only for a batch-fatal {@link Error}.
     *
     * @return the user JID, or empty for global errors not tied to a user
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvHandlerApi",
            exports = "handleADVDeviceSyncResult",
            adaptation = WhatsAppAdaptation.ADAPTED)
    Optional<Jid> userJid();

    /**
     * Returns a copy of this result with hosted business-coexistence devices removed.
     *
     * <p>The receive path calls this when the {@code adv_accept_hosted_devices} AB prop is off so
     * the local cache stays free of HOSTED entries (id {@link DeviceConstants#HOSTED_DEVICE_ID}).
     * Only {@link Full} actually filters; {@link Omitted} and {@link Error} return themselves
     * unchanged because they carry no device list.
     *
     * @implSpec
     * Implementations that carry no device list must return {@code this}; those that do must return
     * a copy with hosted-device entries removed, or {@code this} when none are present.
     *
     * @return a result without hosted devices
     */
    @WhatsAppWebExport(moduleName = "WAWebAdvHandlerApi",
            exports = "handleADVDeviceSyncResult",
            adaptation = WhatsAppAdaptation.ADAPTED)
    DeviceListResult withoutHostedDevices();

    /**
     * Models the full device list response emitted when the server returned a complete device list
     * for a user.
     *
     * <p>This is materialised from the {@code <devices>} child of a USync {@code <user>} slot once
     * {@code WAWebHandleAdvKeyIndexResultApi.handleKeyIndexResultSync} has verified the signed
     * key-index list against the primary identity. It carries the decoded device list, the account
     * signature key needed to verify subsequent key rotations, and the username co-fetched via the
     * username USync protocol when that protocol was included in the query.
     */
    @WhatsAppWebModule(moduleName = "WAWebHandleAdvForUsyncApi")
    @WhatsAppWebModule(moduleName = "WAWebHandleAdvKeyIndexResultApi")
    final class Full implements DeviceListResult {
        /**
         * The decoded device list.
         */
        private final DeviceList deviceList;

        /**
         * The account signature key embedded in the signed key-index list, or {@code null} when the
         * server omitted it.
         */
        private final byte[] accountSignatureKey;

        /**
         * The username returned by the {@code username} USync protocol on the same round-trip, or
         * {@code null} when the protocol was not requested or returned nothing.
         */
        private final String username;

        /**
         * Constructs a new {@link Full} result.
         *
         * <p>The USync response parser invokes this once both the device list and the signed
         * key-index list have been validated; callers do not normally construct one directly.
         *
         * @param deviceList          the device list
         * @param accountSignatureKey the account signature key, or {@code null}
         * @param username            the username, or {@code null}
         * @throws NullPointerException if {@code deviceList} is {@code null}
         */
        public Full(DeviceList deviceList, byte[] accountSignatureKey, String username) {
            this.deviceList = Objects.requireNonNull(deviceList, "deviceList cannot be null");
            this.accountSignatureKey = accountSignatureKey;
            this.username = username;
        }

        /**
         * {@inheritDoc}
         */
        @WhatsAppWebExport(moduleName = "WAWebAdvHandlerApi",
                exports = "handleADVDeviceSyncResult",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public Optional<Jid> userJid() {
            return Optional.of(deviceList.userJid());
        }

        /**
         * Returns the decoded device list.
         *
         * <p>The device service uses this to replace the cached entry for {@link #userJid()} and to
         * drive identity-update side effects on the Signal store.
         *
         * @return the device list
         */
        public DeviceList deviceList() {
            return deviceList;
        }

        /**
         * Returns the account signature key embedded in the signed key-index list.
         *
         * <p>The hosted-business coexistence path forwards this key; the standard E2EE path
         * verifies against the locally-stored primary identity and returns empty here. Callers that
         * persist the key only do so for hosted accounts.
         *
         * @return the account signature key, or empty when none was provided
         */
        @WhatsAppWebExport(moduleName = "WAWebHandleAdvDeviceNotificationUtils",
                exports = "verifySKeyIndexWithAccSigKey",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Optional<byte[]> accountSignatureKey() {
            return Optional.ofNullable(accountSignatureKey);
        }

        /**
         * Returns the username co-fetched via the {@code username} USync protocol.
         *
         * <p>The value is present only when the USync query bundled the username protocol alongside
         * the devices protocol; otherwise it is empty. The username feeds the
         * {@code WAWebUsyncUsername.usernameParser} consumer that maps it onto the contact's
         * display name.
         *
         * @return the username, or empty when none was returned
         */
        @WhatsAppWebExport(moduleName = "WAWebUsyncUsername",
                exports = "usernameParser",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Optional<String> username() {
            return Optional.ofNullable(username);
        }

        /**
         * Returns whether the embedded device list contains at least one hosted business-coexistence
         * device (id {@link DeviceConstants#HOSTED_DEVICE_ID}).
         *
         * <p>The receive path inspects this before routing to {@link #withoutHostedDevices()}, so
         * the caller can short-circuit when there is nothing to strip.
         *
         * @implNote
         * This implementation centralises the hosted-device check as a {@code boolean} helper where
         * WA Web inlines the equivalent {@code devices.some(d => d.id === HOSTED_DEVICE_ID)} test at
         * every call site.
         *
         * @return {@code true} when a hosted device is present
         */
        @WhatsAppWebExport(moduleName = "WAWebBizCoexUtils",
                exports = "hasHostedDevice",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public boolean hasHostedDevice() {
            return deviceList.devices().stream().anyMatch(DeviceInfo::isHosted);
        }

        /**
         * {@inheritDoc}
         */
        @WhatsAppWebExport(moduleName = "WAWebAdvHandlerApi",
                exports = "handleADVDeviceSyncResult",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public DeviceListResult withoutHostedDevices() {
            var filteredDevices = deviceList.devices()
                    .stream()
                    .filter(device -> device.id() != DeviceConstants.HOSTED_DEVICE_ID)
                    .toList();
            if (filteredDevices.size() == deviceList.devices().size()) {
                return this;
            }

            var filteredList = new DeviceListBuilder()
                    .userJid(deviceList.userJid())
                    .devices(filteredDevices)
                    .timestamp(deviceList.timestamp())
                    .rawId(deviceList.rawId())
                    .deleted(deviceList.deleted())
                    .deletedChangedToHost(deviceList.deletedChangedToHost())
                    .advAccountType(deviceList.advAccountType())
                    .expectedTimestamp(deviceList.expectedTimestamp())
                    .expectedTimestampLastDeviceJobTimestamp(deviceList.expectedTimestampLastDeviceJobTimestamp())
                    .expectedTimestampUpdateTimestamp(deviceList.expectedTimestampUpdateTimestamp())
                    .currentIndex(deviceList.currentIndex())
                    .validIndexes(deviceList.validIndexes())
                    .build();
            return new DeviceListResult.Full(filteredList, accountSignatureKey, username);
        }
    }

    /**
     * Models the omitted device list emitted when the server confirms the local device hash is
     * up-to-date and declines to retransmit the full list.
     *
     * <p>This is materialised by {@code WAWebHandleAdvOmittedResultApi.handleOmittedResult} when the
     * USync {@code <user>} slot carries timestamps but no {@code signedKeyIndexBytes}. Callers
     * refresh the cached expected-timestamp tracking fields without touching the device set. The
     * {@link #fromHandleOmittedResult()} flag separates real omitted responses from the synthetic
     * ones the HOSTED-to-E2EE coexistence transition emits to piggy-back on the same applier.
     */
    @WhatsAppWebModule(moduleName = "WAWebHandleAdvOmittedResultApi")
    final class Omitted implements DeviceListResult {
        /**
         * The user JID this result refers to.
         */
        private final Jid userJid;

        /**
         * The server-reported snapshot timestamp, or {@code null} when the slot carried none.
         */
        private final Instant timestamp;

        /**
         * The server-reported expected timestamp, or {@code null} when the slot carried none.
         */
        private final Instant expectedTimestamp;

        /**
         * Whether this result was produced by the genuine omitted-result code path rather than
         * synthesised as part of a HOSTED-to-E2EE coexistence transition.
         */
        private final boolean fromHandleOmittedResult;

        /**
         * Constructs a new {@link Omitted} result.
         *
         * <p>The USync response parser invokes this; the synthetic HOSTED-to-E2EE transition path
         * sets {@code fromHandleOmittedResult} to {@code true} so the downstream applier can
         * recognise the {@code advAccountType} flip.
         *
         * @param userJid                 the user JID
         * @param timestamp               the server's snapshot timestamp, or {@code null}
         * @param expectedTimestamp       the server's expected timestamp, or {@code null}
         * @param fromHandleOmittedResult whether this result came from the omitted code path
         * @throws NullPointerException if {@code userJid} is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WAWebHandleAdvOmittedResultApi",
                exports = "handleOmittedResult",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public Omitted(Jid userJid, Instant timestamp, Instant expectedTimestamp, boolean fromHandleOmittedResult) {
            this.userJid = Objects.requireNonNull(userJid, "userJid cannot be null");
            this.timestamp = timestamp;
            this.expectedTimestamp = expectedTimestamp;
            this.fromHandleOmittedResult = fromHandleOmittedResult;
        }

        /**
         * {@inheritDoc}
         */
        @WhatsAppWebExport(moduleName = "WAWebHandleAdvOmittedResultApi",
                exports = "handleOmittedResult",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public Optional<Jid> userJid() {
            return Optional.of(userJid);
        }

        /**
         * Returns the server-reported snapshot timestamp from the USync slot.
         *
         * <p>This seeds the cached device list's {@code timestamp} on the synthetic
         * HOSTED-to-E2EE transition; the genuine omitted path leaves the cached snapshot timestamp
         * alone and only refreshes the expected-timestamp tracking fields.
         *
         * @return the timestamp, or empty when not present
         */
        @WhatsAppWebExport(moduleName = "WAWebUsyncDevice",
                exports = "deviceParser",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Optional<Instant> timestamp() {
            return Optional.ofNullable(timestamp);
        }

        /**
         * Returns the server-reported expected timestamp from the USync slot.
         *
         * <p>This feeds {@code WAWebAdvExpectedTsApi.computeExpectedTsForDeviceRecord} so the cached
         * device list can recompute when the next ADV check job needs to fetch the user's devices
         * again.
         *
         * @return the expected timestamp, or empty when not present
         */
        @WhatsAppWebExport(moduleName = "WAWebUsyncDevice",
                exports = "deviceParser",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Optional<Instant> expectedTimestamp() {
            return Optional.ofNullable(expectedTimestamp);
        }

        /**
         * Returns whether this result came from the omitted-result code path rather than a synthetic
         * HOSTED-to-E2EE coexistence transition.
         *
         * <p>This mirrors WA Web's {@code fromHandleOmittedResult} discriminator that the
         * device-list applier inspects to decide whether to flip the cached {@code advAccountType}
         * from HOSTED back to E2EE.
         *
         * @return {@code true} when the result is a genuine omitted response
         */
        @WhatsAppWebExport(moduleName = "WAWebHandleAdvOmittedResultApi",
                exports = "handleOmittedResult",
                adaptation = WhatsAppAdaptation.DIRECT)
        public boolean fromHandleOmittedResult() {
            return fromHandleOmittedResult;
        }

        /**
         * {@inheritDoc}
         */
        @WhatsAppWebExport(moduleName = "WAWebAdvHandlerApi",
                exports = "handleADVDeviceSyncResult",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @Override
        public DeviceListResult withoutHostedDevices() {
            return this;
        }
    }

    /**
     * Models the error emitted when the server refused to answer a USync slot.
     *
     * <p>A per-user error (the {@code <error>} child of a single {@code <user>}) carries the
     * addressed user JID and lets the batch continue with the remaining users. A batch-fatal error
     * (the {@code error} attribute on the outer USync result) carries a {@code null} user JID and
     * {@link #fatal()} {@code true}; downstream code aborts the whole batch and surfaces the failure
     * to the caller of the USync round-trip.
     */
    @WhatsAppWebModule(moduleName = "WAWebUsyncDevice")
    final class Error implements DeviceListResult {
        /**
         * The user JID this error is associated with, or {@code null} for a batch-fatal global error
         * that has no addressee.
         */
        private final Jid userJid;

        /**
         * The error code reported by the server's {@code <error code="...">} attribute.
         */
        private final int errorCode;

        /**
         * The error text reported by the server's {@code <error text="...">} attribute.
         */
        private final String errorText;

        /**
         * Whether this error is batch-fatal; a non-fatal error only suppresses the affected user.
         */
        private final boolean fatal;

        /**
         * Constructs a new {@link Error} result.
         *
         * <p>The USync response parser invokes this for each {@code <error>} child it encounters;
         * tests construct one directly when stubbing failure paths.
         *
         * @param userJid   the user JID, or {@code null} for batch-fatal errors
         * @param errorCode the error code
         * @param errorText the error text
         * @param fatal     {@code true} when the error aborts the entire batch
         * @throws NullPointerException if {@code errorText} is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WAWebUsyncDevice",
                exports = "deviceParser",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public Error(Jid userJid, int errorCode, String errorText, boolean fatal) {
            this.userJid = userJid;
            this.errorCode = errorCode;
            this.errorText = Objects.requireNonNull(errorText, "errorText cannot be null");
            this.fatal = fatal;
        }

        /**
         * {@inheritDoc}
         */
        @WhatsAppWebExport(moduleName = "WAWebAdvHandlerApi",
                exports = "handleADVDeviceSyncResult",
                adaptation = WhatsAppAdaptation.DIRECT)
        @Override
        public Optional<Jid> userJid() {
            return Optional.ofNullable(userJid);
        }

        /**
         * Returns the error code lifted from the server's {@code <error>} attribute.
         *
         * <p>Callers that map specific server codes onto retry or fail-loud policies use this; the
         * device-list applier itself only inspects {@link #fatal()}.
         *
         * @return the error code
         */
        @WhatsAppWebExport(moduleName = "WAWebUsyncDevice",
                exports = "deviceParser",
                adaptation = WhatsAppAdaptation.DIRECT)
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text lifted from the server's {@code <error>} attribute.
         *
         * <p>This is surfaced in diagnostic logs and in exceptions thrown for fatal results.
         *
         * @return the error text
         */
        @WhatsAppWebExport(moduleName = "WAWebUsyncDevice",
                exports = "deviceParser",
                adaptation = WhatsAppAdaptation.DIRECT)
        public String errorText() {
            return errorText;
        }

        /**
         * Returns whether this error aborts the entire USync batch.
         *
         * <p>The parser sets this to {@code true} for the outer USync {@code error} attribute
         * (batch-fatal); a per-user {@code <error>} child stays non-fatal so the rest of the slots
         * can still apply.
         *
         * @return {@code true} for fatal errors that abort the batch
         */
        @WhatsAppWebExport(moduleName = "WAWebAdvHandlerApi",
                exports = "handleADVDeviceSyncResult",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public boolean fatal() {
            return fatal;
        }

        /**
         * {@inheritDoc}
         */
        @WhatsAppWebExport(moduleName = "WAWebAdvHandlerApi",
                exports = "handleADVDeviceSyncResult",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @Override
        public DeviceListResult withoutHostedDevices() {
            return this;
        }
    }
}
