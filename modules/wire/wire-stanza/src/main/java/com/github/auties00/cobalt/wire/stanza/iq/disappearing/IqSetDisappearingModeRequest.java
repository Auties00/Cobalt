package com.github.auties00.cobalt.wire.stanza.iq.disappearing;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.time.Duration;
import java.util.Objects;

/**
 * Builds the outbound {@code <iq xmlns="disappearing_mode" type="set">} stanza that updates the
 * account's default disappearing-mode duration.
 *
 * <p>The duration becomes the per-chat default applied to newly-created chats; per-chat overrides
 * are left untouched. {@link Duration#ZERO} disables the feature. The stanza is addressed to
 * {@link #target()}, which defaults to {@link JidServer#user()} for the account-wide default and is
 * set to a chat when the caller updates that chat's timer; the matching reply is parsed by
 * {@link IqSetDisappearingModeResponse}.
 */
@WhatsAppWebModule(moduleName = "WAWebSetDisappearingModeJob")
public final class IqSetDisappearingModeRequest implements IqStanza.Request {
    /**
     * Holds the new default disappearing-mode duration to install.
     *
     * <p>{@link Duration#ZERO} encodes the off state; the value is never {@code null} and never
     * negative because the constructor rejects both.
     */
    private final Duration duration;

    /**
     * Holds the target the {@code <iq>} envelope is addressed to.
     *
     * <p>Defaults to {@link JidServer#user()} for the account-wide default; a per-chat caller supplies the chat
     * {@link JidProvider} so the relay applies the timer to that chat instead of the global default. Never
     * {@code null}.
     */
    private final JidProvider target;

    /**
     * Constructs a set-disappearing-mode request bound to the given duration and addressed to the
     * account-wide default target.
     *
     * <p>Delegates to {@link #IqSetDisappearingModeRequest(Duration, JidProvider)} with
     * {@link JidServer#user()} as the target.
     *
     * @param duration the new default duration; never {@code null}
     * @throws NullPointerException     if {@code duration} is {@code null}
     * @throws IllegalArgumentException if {@code duration} is negative
     */
    public IqSetDisappearingModeRequest(Duration duration) {
        this(duration, JidServer.user());
    }

    /**
     * Constructs a set-disappearing-mode request bound to the given duration and target.
     *
     * <p>The duration is rounded down to whole seconds when emitted, so sub-second precision is
     * lost. Pass {@link Duration#ZERO} to disable the feature. Pass {@link JidServer#user()} as the
     * target for the account-wide default, or a chat {@link JidProvider} to update that chat's timer.
     *
     * @param duration the new default duration; never {@code null}
     * @param target   the target the IQ is addressed to; never {@code null}
     * @throws NullPointerException     if {@code duration} or {@code target} is {@code null}
     * @throws IllegalArgumentException if {@code duration} is negative
     */
    public IqSetDisappearingModeRequest(Duration duration, JidProvider target) {
        Objects.requireNonNull(duration, "duration cannot be null");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration cannot be negative");
        }
        this.duration = duration;
        this.target = Objects.requireNonNull(target, "target cannot be null");
    }

    /**
     * Returns the bound default disappearing-mode duration.
     *
     * @return the duration; never {@code null}
     */
    public Duration duration() {
        return duration;
    }

    /**
     * Returns the target the {@code <iq>} envelope is addressed to.
     *
     * @return the target; never {@code null}
     */
    public JidProvider target() {
        return target;
    }

    /**
     * Builds the outbound {@code <iq>} stanza wrapping the
     * {@code <disappearing_mode duration=SECONDS/>} payload.
     *
     * <p>The returned {@link StanzaBuilder} is wire-ready except for the IQ {@code id} attribute,
     * which the dispatch layer assigns. The {@code duration} attribute carries the configured
     * duration as a seconds-valued string via {@link Duration#toSeconds()}.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <disappearing_mode>}
     *         payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSetDisappearingModeJob",
            exports = "setDisappearingMode",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var dmNode = new StanzaBuilder()
                .description("disappearing_mode")
                .attribute("duration", String.valueOf(duration.toSeconds()))
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "disappearing_mode")
                .attribute("to", target)
                .attribute("type", "set")
                .content(dmNode);
    }

    /**
     * Compares this request to another object for equality.
     *
     * <p>Two set-disappearing-mode requests are equal when they share the same runtime class and
     * the same bound {@link #duration()} and {@link #target()}.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is an equal set-disappearing-mode request
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSetDisappearingModeRequest) obj;
        return Objects.equals(this.duration, that.duration)
                && Objects.equals(this.target, that.target);
    }

    /**
     * Returns a hash code for this request derived from the bound {@link #duration()} and
     * {@link #target()}.
     *
     * @return the derived hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(duration, target);
    }

    /**
     * Returns a debug string carrying the bound {@link #duration()} and {@link #target()}.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "IqSetDisappearingModeRequest[duration=" + duration + ", target=" + target + ']';
    }
}
