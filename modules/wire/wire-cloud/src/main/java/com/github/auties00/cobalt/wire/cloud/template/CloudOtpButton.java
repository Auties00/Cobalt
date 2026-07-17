package com.github.auties00.cobalt.wire.cloud.template;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A one-time-password button of a WhatsApp Cloud API authentication template.
 *
 * <p>Authentication templates carry a single buttons component whose button delivers the verification
 * code to the recipient. The delivery mode is a closed three-variant union, and the type itself is the
 * discriminator rather than a separate token: a {@link CopyCode} shows a button the recipient taps to
 * copy the code, a {@link OneTap} autofills the code into a registered app and falls back to copy-code
 * on devices that do not support autofill, and a {@link ZeroTap} delivers the code to a registered app
 * with no recipient interaction.
 *
 * <p>The required fields differ per variant: a {@link CopyCode} carries only its optional fallback
 * {@linkplain CopyCode#text() text}; a {@link OneTap} adds the optional
 * {@linkplain OneTap#autofillText() autofill label} and the {@linkplain OneTap#supportedApps() supported
 * apps} the autofill targets; a {@link ZeroTap} carries the
 * {@linkplain ZeroTap#zeroTapTermsAccepted() zero-tap terms acknowledgement} and the
 * {@linkplain ZeroTap#supportedApps() supported apps} the handoff targets. Both {@link OneTap} and
 * {@link ZeroTap} require at least one {@linkplain App supported app}; a {@link CopyCode} takes none.
 */
public sealed interface CloudOtpButton permits CloudOtpButton.CopyCode, CloudOtpButton.OneTap,
        CloudOtpButton.ZeroTap {
    /**
     * A copy-code OTP button.
     *
     * <p>Shows a button the recipient taps to copy the verification code to the clipboard.
     */
    final class CopyCode implements CloudOtpButton {
        /**
         * The copy-code button text, or {@code null} to use the server default.
         */
        private final String text;

        /**
         * Constructs a new copy-code button.
         *
         * @param text the copy-code button text, or {@code null} for the server default
         */
        public CopyCode(String text) {
            this.text = text;
        }

        /**
         * Returns the copy-code button text.
         *
         * @return an {@link Optional} carrying the text, or empty to use the server default
         */
        public Optional<String> text() {
            return Optional.ofNullable(text);
        }
    }

    /**
     * A one-tap OTP button.
     *
     * <p>Autofills the verification code into a registered app with a single tap and falls back to
     * copy-code on devices that do not support autofill.
     */
    final class OneTap implements CloudOtpButton {
        /**
         * The copy-code fallback button text, or {@code null} to use the server default.
         */
        private final String text;

        /**
         * The autofill button label, or {@code null} to use the server default.
         */
        private final String autofillText;

        /**
         * The apps the autofill targets.
         */
        private final List<App> supportedApps;

        /**
         * Constructs a new one-tap button.
         *
         * @param text          the copy-code fallback button text, or {@code null} for the default
         * @param autofillText  the autofill button label, or {@code null} for the default
         * @param supportedApps the apps the autofill targets, or {@code null} for none
         */
        public OneTap(String text, String autofillText, List<App> supportedApps) {
            this.text = text;
            this.autofillText = autofillText;
            this.supportedApps = supportedApps == null ? List.of() : List.copyOf(supportedApps);
        }

        /**
         * Returns the copy-code fallback button text.
         *
         * @return an {@link Optional} carrying the text, or empty to use the server default
         */
        public Optional<String> text() {
            return Optional.ofNullable(text);
        }

        /**
         * Returns the autofill button label.
         *
         * @return an {@link Optional} carrying the label, or empty to use the server default
         */
        public Optional<String> autofillText() {
            return Optional.ofNullable(autofillText);
        }

        /**
         * Returns the apps the autofill targets.
         *
         * @return an unmodifiable list of supported apps, empty when none were declared
         */
        public List<App> supportedApps() {
            return supportedApps;
        }
    }

    /**
     * A zero-tap OTP button.
     *
     * <p>Delivers the verification code to a registered app with no recipient interaction. The business
     * must acknowledge the zero-tap terms for this delivery mode to be accepted.
     */
    final class ZeroTap implements CloudOtpButton {
        /**
         * Whether the business has accepted the zero-tap terms, required to be {@code true}.
         */
        private final boolean zeroTapTermsAccepted;

        /**
         * The apps the zero-tap handoff targets.
         */
        private final List<App> supportedApps;

        /**
         * Constructs a new zero-tap button.
         *
         * @param zeroTapTermsAccepted whether the zero-tap terms have been accepted
         * @param supportedApps        the apps the handoff targets, or {@code null} for none
         */
        public ZeroTap(boolean zeroTapTermsAccepted, List<App> supportedApps) {
            this.zeroTapTermsAccepted = zeroTapTermsAccepted;
            this.supportedApps = supportedApps == null ? List.of() : List.copyOf(supportedApps);
        }

        /**
         * Returns whether the zero-tap terms have been accepted.
         *
         * @return {@code true} if the zero-tap terms have been accepted
         */
        public boolean zeroTapTermsAccepted() {
            return zeroTapTermsAccepted;
        }

        /**
         * Returns the apps the zero-tap handoff targets.
         *
         * @return an unmodifiable list of supported apps, empty when none were declared
         */
        public List<App> supportedApps() {
            return supportedApps;
        }
    }

    /**
     * An app a {@link OneTap} or {@link ZeroTap} button hands the verification code to.
     *
     * <p>The pair identifies an Android app eligible to receive the autofilled or zero-tap code: the
     * package name and the app's signing-key signature hash.
     */
    final class App {
        /**
         * The Android package name, for example {@code com.example.app}.
         */
        private final String packageName;

        /**
         * The app signing-key signature hash.
         */
        private final String signatureHash;

        /**
         * Constructs a new supported app.
         *
         * @param packageName   the Android package name
         * @param signatureHash the app signing-key signature hash
         * @throws NullPointerException if {@code packageName} or {@code signatureHash} is {@code null}
         */
        public App(String packageName, String signatureHash) {
            this.packageName = Objects.requireNonNull(packageName, "packageName must not be null");
            this.signatureHash = Objects.requireNonNull(signatureHash, "signatureHash must not be null");
        }

        /**
         * Returns the Android package name.
         *
         * @return the package name
         */
        public String packageName() {
            return packageName;
        }

        /**
         * Returns the app signing-key signature hash.
         *
         * @return the signature hash
         */
        public String signatureHash() {
            return signatureHash;
        }
    }
}
