package com.github.auties00.cobalt.calls.transport.warp;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.util.List;

/**
 * Builds the standalone WARP bandwidth estimation configuration message the client sends to the
 * selective forwarding unit when its downlink estimate drops.
 *
 * <p>When the downlink bandwidth estimate falls, the client asks the unit to lower the minimum rate it
 * allocates to remote senders by sending a standalone WARP message carrying only the
 * {@link WarpAttributeFlag#BANDWIDTH_REPORT} attribute. That attribute is the bandwidth estimation
 * configuration block: a version byte set to {@value #BWE_CONFIG_VERSION}, an index byte, and the
 * minimum remote bandwidth estimate as a {@code u16} in kilobits per second. The bandwidth report
 * attribute is legal only on its own, so the result is always a {@link WarpMessage.Standalone}, and the
 * {@link WarpMessage#encode()} call raises the combined {@code 0x180} attribute flags
 * ({@link WarpAttributeFlag#EXT_FLAG} {@code 0x80} together with {@link WarpAttributeFlag#BANDWIDTH_REPORT}
 * {@code 0x100}) from the single attribute.
 */
public final class BweConfigSender {
    /**
     * The logger for {@link BweConfigSender}.
     */
    private static final System.Logger LOGGER = Log.get(BweConfigSender.class);

    /**
     * The version byte written into the bandwidth estimation configuration block.
     */
    public static final int BWE_CONFIG_VERSION = 2;

    /**
     * Prevents instantiation of this stateless builder holder.
     *
     * @throws AssertionError always, since this holder exposes only static factory methods
     */
    private BweConfigSender() {
        throw new AssertionError("BweConfigSender is not instantiable");
    }

    /**
     * Builds the standalone WARP bandwidth estimation configuration message for a minimum remote bandwidth.
     *
     * <p>Wraps a single {@link WarpAttribute.BandwidthReport} carrying {@value #BWE_CONFIG_VERSION}, the
     * given index byte, and the given minimum remote bandwidth estimate into a
     * {@link WarpMessage.Standalone}.
     *
     * @param index            the report index byte, in {@code 0..255}
     * @param minRemoteBweKbps the minimum remote bandwidth estimate in kilobits per second, in {@code 0..65535}
     * @return a standalone WARP message carrying only the bandwidth report attribute
     * @throws IllegalArgumentException if {@code index} is outside {@code 0..255} or
     *                                  {@code minRemoteBweKbps} is outside {@code 0..65535}
     */
    public static WarpMessage.Standalone build(int index, int minRemoteBweKbps) {
        if (index < 0 || index > 0xff) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "bwe config rejected, index out of range: {0}", index);
            throw new IllegalArgumentException("index must be in [0, 255], got " + index);
        }
        if (minRemoteBweKbps < 0 || minRemoteBweKbps > 0xffff) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "bwe config rejected, minRemoteBweKbps out of range: {0}",
                        minRemoteBweKbps);
            }
            throw new IllegalArgumentException("minRemoteBweKbps must be in [0, 65535], got " + minRemoteBweKbps);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "bwe config built index={0} minRemoteBweKbps={1}", index, minRemoteBweKbps);
        }
        var attribute = new WarpAttribute.BandwidthReport(BWE_CONFIG_VERSION, index, minRemoteBweKbps);
        return new WarpMessage.Standalone(List.of(attribute));
    }

    /**
     * Builds and encodes the standalone WARP bandwidth estimation configuration message to its wire bytes.
     *
     * <p>Equivalent to calling {@link #build(int, int)} and then {@link WarpMessage.Standalone#encode()}.
     *
     * @param index            the report index byte, in {@code 0..255}
     * @param minRemoteBweKbps the minimum remote bandwidth estimate in kilobits per second, in {@code 0..65535}
     * @return the encoded WARP message bytes
     * @throws IllegalArgumentException if {@code index} is outside {@code 0..255} or
     *                                  {@code minRemoteBweKbps} is outside {@code 0..65535}
     */
    public static byte[] encode(int index, int minRemoteBweKbps) {
        return build(index, minRemoteBweKbps).encode();
    }
}
