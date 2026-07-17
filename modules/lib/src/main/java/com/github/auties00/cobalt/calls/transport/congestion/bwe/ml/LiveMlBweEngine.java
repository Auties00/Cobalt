package com.github.auties00.cobalt.calls.transport.congestion.bwe.ml;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Maintains the machine learning bandwidth estimation history and builds the congestion inference round.
 *
 * <p>For each enabled {@link MlBweModelType} the engine holds one history ring with one column per signal.
 * Every {@link #infer(MlBweSignals)} pushes the round's signals into every ring with the fixed scaling:
 * packet loss times one hundred, and the nanosecond and bits per second values divided by one thousand
 * (nanoseconds to microseconds, bits per second to kilobits per second). The congestion classifier builds
 * the {@code (rounds, features)} input tensor in the fixed slot order, runs a forward pass, reads the second
 * output float, scales and quantizes it to a probability, and compares it against the per call threshold to
 * produce the verdict folded into {@link MlBweOutputs}.
 *
 * <p>The native ExecuTorch backend is not bundled in {@code cobalt-native}, so every native model operation
 * throws {@link UnsupportedOperationException} and no model can load or run. With no backend the engine is
 * inert: {@link #loadedModels()} is empty and {@link #infer(MlBweSignals)} returns
 * {@link MlBweOutputs#DISABLED}, so the media plane uses the {@link NoopMlBweEngine} and the delay based and
 * sender side path runs unchanged. Constructing the engine with a model whose {@code .pte} path resolves
 * makes the native load throw. The engine is single writer: a call session drives one from the single
 * transport thread.
 */
public final class LiveMlBweEngine implements MlBweEngine {
    /**
     * The logger for {@link LiveMlBweEngine}.
     */
    private static final System.Logger LOGGER = Log.get(LiveMlBweEngine.class);

    /**
     * The number of slide window columns the feature writer pushes each round.
     *
     * @implNote This implementation uses {@code 13}, the number of windows pushed per round (win0 through
     * win12). A further init only window and one spare ring slot are never read as features.
     */
    static final int FEATURE_COLUMNS = 13;

    /**
     * The feature slot ordering: the ring window index each tensor slot reads, skipping the uninitialised
     * placeholder window 8.
     *
     * @implNote This implementation maps {@code slot{0..7} = win{0..7}}, {@code slot8 = win9},
     * {@code slot9 = win10}, {@code slot10 = win11}, {@code slot11 = win12}: the feature reader takes 12 of
     * the 13 written windows, excluding win8.
     */
    static final int[] SLOT_TO_WINDOW = {0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12};

    /**
     * The scaling factor applied to the packet loss fraction before it is pushed into window 0.
     *
     * @implNote This implementation uses {@code 100.0}: window 0 stores {@code (int) (loss_fraction * 100.0)},
     * the loss as a percent.
     */
    private static final double LOSS_SCALE = 100.0;

    /**
     * The divisor converting nanoseconds to microseconds and bits per second to kilobits per second.
     *
     * @implNote This implementation uses {@code 1000}: the round trip, jitter, and bitrate windows are
     * divided by {@code 1000} before being pushed (ns to us, bps to kbps).
     */
    private static final int MICRO_KILO_DIVISOR = 1000;

    /**
     * The fixed output length of the congestion classification head.
     *
     * @implNote This implementation uses {@code 2}: the forward pass produces two floats and the verdict
     * reads {@code output[1]}.
     */
    private static final int CONGESTION_OUTPUT_LEN = 2;

    /**
     * The output tensor index of the congestion probability within the head's two floats.
     *
     * @implNote This implementation uses {@code 1}: the verdict reads {@code output[1]}; {@code output[0]}
     * is the no congestion class logit.
     */
    private static final int CONGESTION_OUTPUT_INDEX = 1;

    /**
     * The scaling factor applied to the congestion output before quantization.
     *
     * @implNote This implementation uses {@code 1000.0}: {@code output[1]} is multiplied by {@code 1000.0}
     * before being quantized to a {@code ushort}.
     */
    private static final double CONGESTION_PROBABILITY_SCALE = 1000.0;

    /**
     * The per model configuration and loaded state, keyed by model type.
     */
    private final Map<MlBweModelType, LoadedModel> models;

    /**
     * The arena owning the native input and output tensor buffers and the model path string segments.
     */
    private final Arena arena;

    /**
     * Whether {@link #close()} has run, so that a second close is a no op.
     */
    private boolean closed;

    /**
     * Constructs an engine over the given enabled models.
     *
     * <p>For each entry the engine allocates the model's history ring and resolves its {@code .pte} path
     * through {@code resolver}. Because the native ExecuTorch backend is not bundled, a model whose path
     * resolves cannot be loaded and construction throws; a model whose path is absent is held history only,
     * so an engine constructed with no provisioned paths builds successfully and stays inert.
     *
     * @param configs  the per model configuration for every model to enable; never {@code null}
     * @param resolver the resolver mapping a model type to its {@code .pte} filesystem path, or returning
     *                 {@code null} when no path is provisioned for that model; never {@code null}
     * @throws NullPointerException          if {@code configs} or {@code resolver} is {@code null}
     * @throws UnsupportedOperationException if a model's path resolves, since the ExecuTorch backend is not
     *                                       bundled
     */
    public LiveMlBweEngine(Map<MlBweModelType, ModelConfig> configs, Function<MlBweModelType, String> resolver) {
        Objects.requireNonNull(configs, "configs cannot be null");
        Objects.requireNonNull(resolver, "resolver cannot be null");
        this.arena = Arena.ofShared();
        this.models = new EnumMap<>(MlBweModelType.class);
        try {
            for (var entry : configs.entrySet()) {
                var type = entry.getKey();
                var config = entry.getValue();
                var ring = new HistoryRing(config.historyDepth());
                var handle = loadHandle(type, resolver);
                models.put(type, new LoadedModel(config, ring, handle));
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "ml bwe engine: constructed, configuredModels={0} loadedModels={1}",
                        configs.size(), loadedModels().size());
            }
        } catch (RuntimeException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "ml bwe engine: construction failed", e);
            freeHandles();
            arena.close();
            throw e;
        }
    }

    /**
     * Returns whether the native ExecuTorch shim is callable.
     *
     * @return always {@code false}, since the ExecuTorch backend is not bundled in {@code cobalt-native}
     */
    public static boolean nativeShimAvailable() {
        return false;
    }

    /**
     * Loads the model handle for one model, or returns {@link MemorySegment#NULL} when no path is
     * provisioned.
     *
     * <p>Returns {@link MemorySegment#NULL} when the path does not resolve; when a path is provisioned the
     * native create throws, since the ExecuTorch backend is not bundled, so a model with a {@code .pte} path
     * cannot be loaded.
     *
     * @param type     the model type to load
     * @param resolver the model path resolver
     * @return {@link MemorySegment#NULL} when no path is provisioned
     * @throws UnsupportedOperationException when a path is provisioned, since the ExecuTorch backend is not
     *                                       bundled
     */
    private MemorySegment loadHandle(MlBweModelType type, Function<MlBweModelType, String> resolver) {
        var path = resolver.apply(type);
        if (path == null || path.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ml bwe engine: no model path provisioned for {0}", type);
            return MemorySegment.NULL;
        }
        var pathSegment = arena.allocateFrom(path);
        var handle = ml_shim_create_executorch(pathSegment);
        if (handle.equals(MemorySegment.NULL) || ml_get_shim_create_status() != COBALT_ET_OK()) {
            if (!handle.equals(MemorySegment.NULL)) {
                ml_shim_free(handle);
            }
            if (Log.WARNING) LOGGER.log(Level.WARNING, "ml bwe engine: model create failed for {0}", type);
            return MemorySegment.NULL;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ml bwe engine: model loaded for {0}", type);
        return handle;
    }

    /**
     * Creates a model handle from a {@code .pte} model path.
     *
     * @param modelPath the native UTF-8 model path
     * @return never returns normally
     * @throws UnsupportedOperationException always, since the ExecuTorch native backend is not bundled into
     *                                       {@code cobalt-native}
     */
    private static MemorySegment ml_shim_create_executorch(MemorySegment modelPath) {
        throw nativeUnavailable();
    }

    /**
     * Runs one forward pass of a loaded model.
     *
     * @param model          the model handle
     * @param input          the input feature buffer
     * @param inputLen       the number of input floats
     * @param output         the output buffer
     * @param outputCapacity the output buffer capacity in floats
     * @return never returns normally
     * @throws UnsupportedOperationException always, since the ExecuTorch native backend is not bundled
     */
    private static int ml_shim_forward(MemorySegment model, MemorySegment input, int inputLen, MemorySegment output, int outputCapacity) {
        throw nativeUnavailable();
    }

    /**
     * Frees a loaded model handle.
     *
     * @param model the model handle
     * @throws UnsupportedOperationException always, since the ExecuTorch native backend is not bundled
     */
    private static void ml_shim_free(MemorySegment model) {
        throw nativeUnavailable();
    }

    /**
     * Returns the status of the most recent model create.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always, since the ExecuTorch native backend is not bundled
     */
    private static int ml_get_shim_create_status() {
        throw nativeUnavailable();
    }

    /**
     * Returns the native status code denoting a successful model create.
     *
     * @return the {@code COBALT_ET_OK} constant
     */
    private static int COBALT_ET_OK() {
        return 0;
    }

    /**
     * Returns the exception thrown at the absent native boundary.
     *
     * @return an {@link UnsupportedOperationException} describing the removed ExecuTorch backend
     */
    private static UnsupportedOperationException nativeUnavailable() {
        return new UnsupportedOperationException("ExecuTorch native backend is not bundled in cobalt-native");
    }

    /**
     * {@inheritDoc}
     *
     * @return the model types whose handle loaded successfully
     */
    @Override
    public Set<MlBweModelType> loadedModels() {
        var loaded = EnumSet.noneOf(MlBweModelType.class);
        for (var entry : models.entrySet()) {
            if (!entry.getValue().handle().equals(MemorySegment.NULL)) {
                loaded.add(entry.getKey());
            }
        }
        return loaded;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation pushes {@code signals} into every model's ring before running any
     * inference, then runs the loaded congestion model when its ring is full.
     * @param signals {@inheritDoc}
     * @return the fused steering outputs, or {@link MlBweOutputs#DISABLED} when no model ran
     */
    @Override
    public MlBweOutputs infer(MlBweSignals signals) {
        Objects.requireNonNull(signals, "signals cannot be null");
        for (var model : models.values()) {
            pushRound(model.ring(), signals);
        }
        // TODO: derive the undershoot and high definition targeting heads; only the congestion verdict is
        //  wired into the output today.
        var cong = models.get(MlBweModelType.CONG);
        if (cong == null || cong.handle().equals(MemorySegment.NULL)) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "ml bwe engine: infer skipped, congestion model not loaded");
            return MlBweOutputs.DISABLED;
        }
        var outputs = runCongestion(cong, signals);
        if (Log.DEBUG && outputs.congestionDetected()) {
            LOGGER.log(Level.DEBUG, "ml bwe engine: congestion detected, probability={0}", outputs.congestionProbability());
        }
        return outputs;
    }

    /**
     * Pushes one round's signals into a model's history ring, in the fixed column order and scaling.
     *
     * <p>Windows 0 through 7 take the loss percent, the round trip estimate, the remote and sender bandwidth
     * estimates, the audio round trip, the video and audio jitter, and the packet pair estimate; window 12
     * takes the max target bitrate. Window 8 is a placeholder and windows 9 through 11 are categorical
     * knobs. A slot whose source signal is {@link MlBweSignals#UNAVAILABLE} is pushed as {@code 0}, and the
     * model is gated separately on whether its selected slots are all available.
     *
     * @param ring    the model's history ring
     * @param signals the round's signals
     */
    private void pushRound(HistoryRing ring, MlBweSignals signals) {
        ring.push(0, scaleLoss(signals.packetLossFraction()));
        ring.push(1, divideOrZero(signals.rttNs()));
        ring.push(2, divideOrZero(signals.remoteBweBps()));
        ring.push(3, divideOrZero(signals.senderBweBps()));
        ring.push(4, divideOrZero(signals.audioRttNs()));
        ring.push(5, divideOrZero(signals.videoJitterNs()));
        ring.push(6, divideOrZero(signals.audioJitterNs()));
        ring.push(7, divideOrZero(signals.packetPairBps()));
        // Window 8 is the placeholder; it is excluded from every model's feature tensor, so it is pushed
        // as 0 and never read.
        ring.push(8, 0);
        // TODO: thread the real values for windows 9 through 11, the categorical voip param knobs (per model
        //  platform pair feature values); their runtime values are voip param constants not yet decoded, so
        //  they are pushed as the sentinel default 0 for now.
        ring.push(9, 0);
        ring.push(10, 0);
        ring.push(11, 0);
        ring.push(12, divideOrZero(signals.maxTargetBitrateBps()));
    }

    /**
     * Runs the congestion model and returns its verdict and probability.
     *
     * <p>Requires the model's history ring to be full and its selected feature slots all available; when
     * either is not met it returns {@link MlBweOutputs#DISABLED}. Otherwise it builds the
     * {@code (rounds, features)} input tensor in the fixed slot order, runs the forward pass, reads the
     * second output float, scales and quantizes it, and compares it against the per call threshold.
     *
     * @param model   the loaded congestion model
     * @param signals the round's signals, for the per slot availability check
     * @return the congestion outputs, or {@link MlBweOutputs#DISABLED} when the model could not run
     */
    private MlBweOutputs runCongestion(LoadedModel model, MlBweSignals signals) {
        var config = model.config();
        var ring = model.ring();
        if (!ring.isFull() || !config.isResolved()) {
            return MlBweOutputs.DISABLED;
        }
        var selectedSlots = selectedSlots(config.featureBitmap());
        if (!slotsAvailable(selectedSlots, signals)) {
            return MlBweOutputs.DISABLED;
        }
        var rounds = config.historyDepth();
        var features = selectedSlots.length;
        if (features != config.featureCount()) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "ml bwe engine: congestion model feature count mismatch, expected={0} actual={1}",
                        config.featureCount(), features);
            }
            return MlBweOutputs.DISABLED;
        }

        var input = arena.allocate((long) rounds * features * Float.BYTES);
        fillInput(input, ring, selectedSlots, rounds);
        var output = arena.allocate((long) CONGESTION_OUTPUT_LEN * Float.BYTES);
        var written = ml_shim_forward(model.handle(), input, rounds * features, output, CONGESTION_OUTPUT_LEN);
        if (written != CONGESTION_OUTPUT_LEN) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "ml bwe engine: congestion model forward pass incomplete, written={0} expected={1}",
                        written, CONGESTION_OUTPUT_LEN);
            }
            return MlBweOutputs.DISABLED;
        }
        var raw = output.getAtIndex(ValueLayout.JAVA_FLOAT, CONGESTION_OUTPUT_INDEX);
        var probability = quantizeProbability(raw);
        var detected = probability > config.probabilityThreshold();
        return MlBweOutputs.ofCongestion(detected, probability);
    }

    /**
     * Fills the native input tensor row major from the history ring in the fixed slot order.
     *
     * <p>The tensor is {@code (rounds, features)} row major: row {@code r} holds the {@code r}-th sample,
     * oldest to newest, of each selected feature column. Each element is written at
     * {@code (r * features + c)}.
     *
     * @param input         the native input buffer of {@code rounds * features} floats
     * @param ring          the model's history ring
     * @param selectedSlots the tensor slots, in order, each mapping to a ring window
     * @param rounds        the history depth (number of rows)
     */
    // TODO: confirm the tensor row direction (oldest to newest, as written here, versus newest to oldest).
    //  The column set and the per feature scaling are known but the time axis ordering is not; it is only
    //  observable from a paused forward pass on a connected congested call. This is moot until a model's
    //  bitmap, rounds, and feature count are decoded (the model is otherwise skipped via
    //  ModelConfig.isResolved), but the row order must be verified before a provisioned model is trusted.
    private void fillInput(MemorySegment input, HistoryRing ring, int[] selectedSlots, int rounds) {
        var features = selectedSlots.length;
        for (var row = 0; row < rounds; row++) {
            for (var col = 0; col < features; col++) {
                var window = SLOT_TO_WINDOW[selectedSlots[col]];
                var value = ring.sampleAt(window, row);
                input.setAtIndex(ValueLayout.JAVA_FLOAT, (long) row * features + col, value);
            }
        }
    }

    /**
     * Returns the tensor slot indices the feature bitmap selects, in ascending slot order.
     *
     * <p>The bitmap is a 12 bit mask over the {@link #SLOT_TO_WINDOW} feature slots; a set bit {@code i}
     * selects slot {@code i}.
     *
     * @param bitmap the 12 bit feature selection mask
     * @return the selected slot indices in ascending order
     */
    private static int[] selectedSlots(int bitmap) {
        var count = Integer.bitCount(bitmap & 0xFFF);
        var slots = new int[count];
        var index = 0;
        for (var slot = 0; slot < SLOT_TO_WINDOW.length; slot++) {
            if ((bitmap & (1 << slot)) != 0) {
                slots[index++] = slot;
            }
        }
        return slots;
    }

    /**
     * Returns whether every selected slot maps to a signal the round actually carries.
     *
     * <p>Slots 0 to 3 (loss, round trip, remote estimate, sender estimate) and slot 11 (the configured max
     * bitrate) are always available; slots 4 to 7 (audio round trip, video and audio jitter, packet pair)
     * are available only when the corresponding {@link MlBweSignals} field is not
     * {@link MlBweSignals#UNAVAILABLE}; slots 8 to 10 (the categorical knobs) are voip param constants
     * treated as available. A model selecting an unavailable slot is skipped rather than fed a fabricated
     * value.
     *
     * @param selectedSlots the slots the model selects
     * @param signals       the round's signals
     * @return {@code true} when every selected slot is fillable
     */
    private static boolean slotsAvailable(int[] selectedSlots, MlBweSignals signals) {
        for (var slot : selectedSlots) {
            var available = switch (slot) {
                case 4 -> MlBweSignals.isAvailable(signals.audioRttNs());
                case 5 -> MlBweSignals.isAvailable(signals.videoJitterNs());
                case 6 -> MlBweSignals.isAvailable(signals.audioJitterNs());
                case 7 -> MlBweSignals.isAvailable(signals.packetPairBps());
                default -> true;
            };
            if (!available) {
                return false;
            }
        }
        return true;
    }

    /**
     * Scales a packet loss fraction to the percent value the loss window stores.
     *
     * <p>Saturates to {@link Integer#MIN_VALUE} when the scaled value would overflow an {@code int}.
     *
     * @param lossFraction the packet loss fraction, in {@code [0, 1]}
     * @return the loss as an integer percent ({@code fraction * 100})
     */
    private static int scaleLoss(double lossFraction) {
        var scaled = lossFraction * LOSS_SCALE;
        if (Math.abs(scaled) >= 2.1474836e9) {
            return Integer.MIN_VALUE;
        }
        return (int) scaled;
    }

    /**
     * Divides a nanosecond or bits per second signal by one thousand, or returns {@code 0} for an
     * unavailable signal.
     *
     * @param value the raw signal, or {@link MlBweSignals#UNAVAILABLE}
     * @return the signal divided by {@value #MICRO_KILO_DIVISOR}, or {@code 0} when unavailable
     */
    private static int divideOrZero(long value) {
        if (!MlBweSignals.isAvailable(value)) {
            return 0;
        }
        return (int) (value / MICRO_KILO_DIVISOR);
    }

    /**
     * Scales and quantizes the raw congestion output float to a probability in {@code [0, 65535]}.
     *
     * <p>Multiplies the output by {@value #CONGESTION_PROBABILITY_SCALE}, takes the value as an unsigned
     * 32 bit integer only when it falls in {@code [0, 2^32)} (otherwise {@code 0}), and masks it to a
     * {@code ushort}, so the result lies in
     * {@code [0, }{@value MlBweOutputs#MAX_CONGESTION_PROBABILITY}{@code ]}.
     *
     * @param rawOutput the raw congestion output float ({@code output[1]})
     * @return the quantized probability, in {@code [0, 65535]}
     */
    private static int quantizeProbability(float rawOutput) {
        var scaled = rawOutput * CONGESTION_PROBABILITY_SCALE;
        long asUint;
        if (scaled >= 0.0 && scaled < 4.2949673e9) {
            asUint = (long) scaled;
        } else {
            asUint = 0;
        }
        return (int) (asUint & 0xFFFF);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation frees every loaded model handle and closes the native arena holding the
     * tensor buffers; closing an already closed engine has no effect.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        freeHandles();
        arena.close();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ml bwe engine: closed");
    }

    /**
     * Frees every loaded model handle, swallowing any error.
     */
    private void freeHandles() {
        for (var model : models.values()) {
            if (!model.handle().equals(MemorySegment.NULL)) {
                try {
                    ml_shim_free(model.handle());
                } catch (Throwable t) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "ml bwe engine: model free failed", t);
                }
            }
        }
    }

    /**
     * The per model runtime configuration read from the {@code voip_settings} blob: the feature selection
     * bitmap, the feature count, the history depth, and the congestion probability threshold.
     *
     * <p>These are not compiled constants; each is a runtime voip param the engine reads per model. A
     * configuration whose values are unknown ({@link #unresolved()}) marks a model the engine holds history
     * only and never runs, so a missing voip param never causes a fabricated model run.
     *
     * @param featureBitmap        the 12 bit feature selection mask the tensor builder reads
     * @param featureCount         the number of selected features
     * @param historyDepth         the time series depth (the number of rounds) and the slide window capacity
     * @param probabilityThreshold the per call congestion probability threshold, in {@code [0, 65535]}
     */
    public record ModelConfig(int featureBitmap, int featureCount, int historyDepth, int probabilityThreshold) {
        /**
         * Validates that the history depth is not negative.
         *
         * @throws IllegalArgumentException if {@code historyDepth} is negative
         */
        public ModelConfig {
            if (historyDepth < 0) {
                throw new IllegalArgumentException("historyDepth cannot be negative, got " + historyDepth);
            }
        }

        /**
         * Returns an unresolved configuration that keeps its model dormant.
         *
         * <p>An unresolved configuration has a zero bitmap and feature count and a history depth of one; a
         * model with it is never run, only its history accumulated, until the voip param values are decoded.
         *
         * @return the unresolved configuration
         */
        public static ModelConfig unresolved() {
            return new ModelConfig(0, 0, 1, 0);
        }

        /**
         * Returns whether this configuration carries decoded voip param values.
         *
         * @return {@code true} when the feature count and history depth are positive and the feature bitmap
         * is nonzero
         */
        public boolean isResolved() {
            return featureCount > 0 && historyDepth > 0 && featureBitmap != 0;
        }
    }

    /**
     * Holds one model's configuration, history ring, and loaded model handle.
     *
     * @param config the model's runtime configuration
     * @param ring   the model's slide window history ring
     * @param handle the loaded model handle, or {@link MemorySegment#NULL} when not loaded
     */
    private record LoadedModel(ModelConfig config, HistoryRing ring, MemorySegment handle) {
    }

    /**
     * The multi column slide window history ring: {@value #FEATURE_COLUMNS} fixed capacity circular
     * buffers, one per feature column.
     *
     * <p>Each column is a circular {@code int} buffer of {@code capacity} samples with a head index and a
     * sample count that saturates at the capacity. A push writes at the head, advances the head modulo the
     * capacity, and grows the count until the column is full, after which each new sample overwrites the
     * oldest.
     */
    static final class HistoryRing {
        /**
         * The per column capacity, equal to the model's history depth.
         */
        private final int capacity;

        /**
         * The per column circular sample buffers, indexed {@code [column][slot]}.
         */
        private final int[][] data;

        /**
         * The per column head indices, where the next sample is written.
         */
        private final int[] head;

        /**
         * The per column live sample counts, each saturating at the capacity.
         */
        private final int[] count;

        /**
         * Constructs a ring of {@value #FEATURE_COLUMNS} columns, each holding {@code capacity} samples.
         *
         * @param capacity the per column capacity (the model's history depth), clamped to at least one
         */
        HistoryRing(int capacity) {
            this.capacity = Math.max(1, capacity);
            this.data = new int[FEATURE_COLUMNS][this.capacity];
            this.head = new int[FEATURE_COLUMNS];
            this.count = new int[FEATURE_COLUMNS];
        }

        /**
         * Pushes one sample into a column, overwriting the oldest when the column is full.
         *
         * @param column the column index, in {@code [0, }{@value #FEATURE_COLUMNS}{@code )}
         * @param value  the sample value
         */
        void push(int column, int value) {
            data[column][head[column]] = value;
            head[column] = (head[column] + 1) % capacity;
            if (count[column] < capacity) {
                count[column]++;
            }
        }

        /**
         * Returns the sample at an oldest to newest row position within a column.
         *
         * <p>Row {@code 0} is the oldest live sample; row {@code capacity - 1} is the newest. Before the
         * column fills, the live samples are the first {@code count} positions.
         *
         * @param column the column index
         * @param row    the oldest to newest row position, in {@code [0, capacity)}
         * @return the sample value at that position, as a float for the tensor
         */
        float sampleAt(int column, int row) {
            var oldest = (head[column] - count[column] + capacity) % capacity;
            var index = (oldest + row) % capacity;
            return data[column][index];
        }

        /**
         * Returns whether every column has filled to its capacity.
         *
         * @return {@code true} when each column holds {@code capacity} samples
         */
        boolean isFull() {
            for (var c = 0; c < FEATURE_COLUMNS; c++) {
                if (count[c] < capacity) {
                    return false;
                }
            }
            return true;
        }
    }
}
