package com.github.auties00.cobalt.wire.stanza.smax.pushconfig;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the sealed disjunction of payload variants for a {@link SmaxPushConfigSetRequest}.
 *
 * <p>The push-config SET stanza carries exactly one of two shapes: a {@link Config} registration
 * that nests a platform-specific {@link SmaxPushConfigSetConfigVariant}, or a {@link Clear}
 * de-registration optionally scoped to a single platform. Each variant renders itself into the
 * {@code <config>} or {@code <clear>} child stanza that {@link SmaxPushConfigSetRequest#toStanza()}
 * nests under the outbound IQ.
 */
public sealed interface SmaxPushConfigSetSetVariant
        permits SmaxPushConfigSetSetVariant.Config, SmaxPushConfigSetSetVariant.Clear {

    /**
     * Builds the {@code <config>} or {@code <clear>} child stanza for this variant.
     *
     * <p>{@link SmaxPushConfigSetRequest#toStanza()} calls this method to materialise the variant
     * into the outbound stanza.
     *
     * @implSpec Implementations must return a single {@link Stanza} whose description is either
     * {@code config} or {@code clear}, with no surrounding envelope.
     * @return the {@link Stanza} for this variant
     */
    Stanza toStanza();

    /**
     * Represents the {@code <config>} variant that registers a push channel for a specific client
     * family.
     *
     * <p>The instance carries exactly one of the platform-specific config mixins exposed through
     * {@link SmaxPushConfigSetConfigVariant} (FB, Android, Apple, WNS, Enterprise, or Web Push);
     * the carried variant determines which platform-specific {@code <config>} shape the relay
     * receives.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutPushConfigSetSetConfigMixin")
    @WhatsAppWebModule(moduleName = "WASmaxOutPushConfigConfigMixins")
    final class Config implements SmaxPushConfigSetSetVariant {
        /**
         * Holds the platform-specific config payload that this variant renders.
         */
        private final SmaxPushConfigSetConfigVariant config;

        /**
         * Constructs a {@code <config>} variant wrapping the given platform-specific config.
         *
         * @param config the platform-specific {@link SmaxPushConfigSetConfigVariant}
         * @throws NullPointerException if {@code config} is {@code null}
         */
        public Config(SmaxPushConfigSetConfigVariant config) {
            this.config = Objects.requireNonNull(config, "config cannot be null");
        }

        /**
         * Returns the platform-specific config payload carried by this variant.
         *
         * @return the {@link SmaxPushConfigSetConfigVariant}
         */
        public SmaxPushConfigSetConfigVariant config() {
            return config;
        }

        /**
         * Builds the {@code <config>} child stanza by delegating to the carried platform-specific
         * variant.
         *
         * @return the {@link Stanza} produced by {@link SmaxPushConfigSetConfigVariant#toStanza()}
         */
        @Override
        @WhatsAppWebExport(moduleName = "WASmaxOutPushConfigSetSetConfigMixin",
                exports = "mergeSetSetConfigMixin",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Stanza toStanza() {
            return config.toStanza();
        }

        /**
         * Compares this variant to another object for equality on the carried config.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link Config} with an equal carried config
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Config) obj;
            return Objects.equals(this.config, that.config);
        }

        /**
         * Returns a hash code derived from the carried config.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(config);
        }

        /**
         * Returns a debug rendering of this variant.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxPushConfigSetSetVariant.Config[config=" + config + ']';
        }
    }

    /**
     * Represents the {@code <clear>} variant that drops a push registration.
     *
     * <p>A non-null platform scope confines the clear to a single platform (for example,
     * {@code "web"} to drop only the web subscription while leaving any mobile registration
     * intact); a {@code null} scope drops every registration tied to the account.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutPushConfigSetClearMixin")
    final class Clear implements SmaxPushConfigSetSetVariant {
        /**
         * Holds the optional platform scope, one of {@code "fb"}, {@code "apple"},
         * {@code "android"}, {@code "wns"}, {@code "ent"}, or {@code "web"}; {@code null} drops
         * every registration.
         */
        private final String clearPlatform;

        /**
         * Constructs a {@code <clear>} variant with the given optional platform scope.
         *
         * <p>A non-null {@code clearPlatform} scopes the clear to that platform; {@code null}
         * drops every registration tied to the account.
         *
         * @param clearPlatform the optional platform scope
         */
        public Clear(String clearPlatform) {
            this.clearPlatform = clearPlatform;
        }

        /**
         * Returns the optional platform scope.
         *
         * <p>When present, the relay clears only the registration matching that platform name;
         * when absent every registration is dropped.
         *
         * @return an {@link Optional} carrying the scope
         */
        public Optional<String> clearPlatform() {
            return Optional.ofNullable(clearPlatform);
        }

        /**
         * Builds the {@code <clear>} child stanza, adding the {@code platform} attribute only when
         * a scope is present.
         *
         * @return the {@link Stanza}
         */
        @Override
        @WhatsAppWebExport(moduleName = "WASmaxOutPushConfigSetClearMixin",
                exports = "mergeSetClearMixin",
                adaptation = WhatsAppAdaptation.DIRECT)
        public Stanza toStanza() {
            var builder = new StanzaBuilder()
                    .description("clear");
            if (clearPlatform != null) {
                builder.attribute("platform", clearPlatform);
            }
            return builder.build();
        }

        /**
         * Compares this variant to another object for equality on the platform scope.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link Clear} with an equal platform scope
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Clear) obj;
            return Objects.equals(this.clearPlatform, that.clearPlatform);
        }

        /**
         * Returns a hash code derived from the platform scope.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(clearPlatform);
        }

        /**
         * Returns a debug rendering of this variant.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxPushConfigSetSetVariant.Clear[clearPlatform=" + clearPlatform + ']';
        }
    }
}
