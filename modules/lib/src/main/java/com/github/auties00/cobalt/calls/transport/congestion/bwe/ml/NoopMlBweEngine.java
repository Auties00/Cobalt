package com.github.auties00.cobalt.calls.transport.congestion.bwe.ml;

import java.util.Set;

/**
 * The disabled machine learning bandwidth estimation engine that loads no model and reports no machine
 * learning signal, so the pure delay based sender side estimator runs standalone.
 *
 * <p>This is the default {@link MlBweEngine}. {@link #loadedModels()} is always empty,
 * {@link #infer(MlBweSignals)} always returns {@link MlBweOutputs#DISABLED}, and {@link #close()} does
 * nothing, so the combiner and rate control behave as if no model exists.
 *
 * <p>The engine is stateless and therefore safe to share across threads, though the call session drives one
 * from the single transport thread by convention.
 *
 * @implNote This implementation realizes the path where every model is gated off by its per model voip
 * parameter, running the estimator without inference.
 */
public final class NoopMlBweEngine implements MlBweEngine {
    /**
     * Constructs the disabled engine.
     */
    public NoopMlBweEngine() {
    }

    /**
     * {@inheritDoc}
     *
     * @return an empty set, since this engine loads no model
     */
    @Override
    public Set<MlBweModelType> loadedModels() {
        return Set.of();
    }

    /**
     * {@inheritDoc}
     *
     * @param signals ignored, since the disabled engine runs no model
     * @return {@link MlBweOutputs#DISABLED}
     */
    @Override
    public MlBweOutputs infer(MlBweSignals signals) {
        return MlBweOutputs.DISABLED;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Does nothing, since the disabled engine holds no resources.
     */
    @Override
    public void close() {
    }
}
