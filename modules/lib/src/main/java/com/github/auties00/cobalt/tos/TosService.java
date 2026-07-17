package com.github.auties00.cobalt.tos;

import com.github.auties00.cobalt.wire.linked.tos.TosNotice;

import java.util.Collection;
import java.util.List;

/**
 * Tracks the per-user acceptance state of WhatsApp {@link TosNotice}
 * Terms-of-Service notices and refreshes it from the relay.
 *
 * <p>This is the Cobalt counterpart of WhatsApp Web's {@code WAWebTos.TosManager}:
 * it pulls the acceptance state of a set of notices over the {@code w:tos}
 * acceptance IQ, persists the acknowledged ids, and exposes whether a given
 * notice has been acknowledged. The state is consulted by the inbound-message
 * gating (the interoperability Terms-of-Service gate) and by the acceptance
 * prompts that the various TOS-gated surfaces show.
 *
 * @apiNote The acknowledged-only persistence model means {@link #isAcknowledged(TosNotice)}
 *          distinguishes accepted from "not accepted or never pulled"; callers
 *          that must treat a never-pulled notice as unknown (rather than not
 *          accepted) gate their own logic on whether a {@link #refresh(Collection)}
 *          has run, mirroring WhatsApp Web's {@code UNKNOWN} state.
 */
public interface TosService {
    /**
     * Resolves a notice definition to its concrete notice id (or ids).
     *
     * <p>The id is taken from the active AB-prop ({@link TosNotice#smbProp()}
     * when the linked device is a WhatsApp Business client and it is set,
     * otherwise {@link TosNotice#webProp()}) when that value is non-blank, and
     * falls back to {@link TosNotice#defaultId()} otherwise. A
     * {@linkplain TosNotice#multiValued() multi-valued} notice splits the
     * resolved value on commas into a notice group.
     *
     * @implSpec Implementations return an empty list when neither the active
     *           AB-prop value nor the default id yields a non-blank id, and a
     *           list with a single element for a non-multi-valued notice.
     * @param notice the notice definition to resolve, must not be {@code null}
     * @return the resolved notice id(s), never {@code null} and possibly empty
     * @throws NullPointerException if {@code notice} is {@code null}
     */
    List<String> resolveIds(TosNotice notice);

    /**
     * Returns whether the local user has acknowledged (accepted) the given
     * notice.
     *
     * @implSpec Implementations return {@code true} only when the notice id is
     *           present in the persisted acknowledged set and {@code false}
     *           otherwise, including when the notice has never been pulled from
     *           the relay.
     * @param notice the notice to query, must not be {@code null}
     * @return {@code true} when the notice has been acknowledged, {@code false}
     *         otherwise
     * @throws NullPointerException if {@code notice} is {@code null}
     */
    boolean isAcknowledged(TosNotice notice);

    /**
     * Refreshes the acceptance state of the given notices from the relay and
     * updates the persisted acknowledged set accordingly.
     *
     * @implSpec Implementations issue the {@code w:tos} acceptance query for the
     *           given notice ids, then replace the persisted acknowledged set so
     *           that a notice the relay reports as not accepted is removed and
     *           one it reports as accepted is added. A failed query leaves the
     *           previously persisted set unchanged.
     * @param notices the notices to refresh, must not be {@code null}
     * @throws NullPointerException if {@code notices} is {@code null}
     */
    void refresh(Collection<TosNotice> notices);
}
