package com.github.auties00.cobalt.calls.platform;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

/**
 * Owns the off heap memory lifetime of a single call subsystem behind a named {@link Arena}.
 *
 * <p>The native voip engine allocates almost everything a call needs (transport state, parser scratch,
 * ICE candidates) out of named memory pools, and wraps pool creation in a helper that releases a pool
 * when its owner goes out of scope. Cobalt has no such allocator: the native libraries it binds through
 * the foreign function and memory API expect raw, correctly scoped {@link MemorySegment} arguments, and
 * the natural lifetime container for those segments is an {@link Arena}. This class is that container
 * with a diagnostic name attached. It hands its backing arena to native bindings as a
 * {@link java.lang.foreign.SegmentAllocator}, allocates off heap segments whose validity is bounded by
 * the arena's lifetime, and frees every one of them at once when {@link #close()} runs.
 *
 * <p>An instance is created through {@link #create(String)} and is an {@link AutoCloseable} resource;
 * the recommended use binds it in a resource block whose scope matches the lifetime of the call
 * subsystem that owns the memory:
 *
 * {@snippet :
 *   try (var pool = WaCallMemoryArena.create("ice transport")) {
 *       MemorySegment buffer = pool.allocate(1500);
 *       // hand buffer to a native binding; valid until the block exits
 *   }
 *   // every segment allocated from pool is now released
 * }
 *
 * <p>The backing arena is a confined arena: it may be accessed and closed only by the thread that
 * created it. That matches how a call subsystem is driven by a single owning thread and lets the arena
 * detect misuse from another thread. A subsystem shared across threads must hold its own arena per
 * thread or serialize access.
 *
 * @implNote This implementation backs the pool with a confined {@link Arena} rather than a native memory
 * pool. {@link Arena#ofConfined()} carries no status code and never yields a {@code null} handle, so the
 * only allocation failure surface is address space exhaustion, which the foreign memory API raises
 * directly at allocation time. The name is retained purely for diagnostics.
 */
public final class WaCallMemoryArena implements AutoCloseable {
    /**
     * The logger for {@link WaCallMemoryArena}.
     */
    private static final System.Logger LOGGER = Log.get(WaCallMemoryArena.class);

    /**
     * Holds the diagnostic name of this pool.
     */
    private final String name;

    /**
     * Holds the confined arena that backs every allocation and bounds their lifetime.
     */
    private final Arena arena;

    /**
     * Constructs a pool over a name and its backing arena.
     *
     * @param name  the diagnostic pool name
     * @param arena the confined arena that owns the off heap memory
     */
    private WaCallMemoryArena(String name, Arena arena) {
        this.name = name;
        this.arena = arena;
    }

    /**
     * Creates a memory pool with the given diagnostic name.
     *
     * <p>The returned pool owns a fresh confined {@link Arena}; close it, ideally within a resource
     * block, to release every segment it allocated.
     *
     * @param name the diagnostic pool name
     * @return a new open memory pool
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static WaCallMemoryArena create(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "creating memory pool {0}", name);
        return new WaCallMemoryArena(name, Arena.ofConfined());
    }

    /**
     * Returns the diagnostic name of this pool.
     *
     * @return the pool name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the backing arena, suitable to pass to a native binding as its segment allocator.
     *
     * <p>The arena is a {@link java.lang.foreign.SegmentAllocator}, so a binding that accepts an
     * allocator can allocate its own arguments from this pool and inherit the pool's lifetime. Do not
     * close the returned arena directly; close the owning pool through {@link #close()} instead.
     *
     * @return the confined arena backing this pool
     */
    public Arena arena() {
        return arena;
    }

    /**
     * Allocates an off heap segment of the given size from this pool.
     *
     * <p>The returned segment is valid until this pool is closed. Its contents are zero initialized, as
     * guaranteed by the foreign memory API.
     *
     * @param byteSize the size of the segment in bytes
     * @return a new zero initialized native segment of {@code byteSize} bytes
     * @throws IllegalArgumentException if {@code byteSize} is negative
     * @throws IllegalStateException    if this pool has been closed or is accessed from another thread
     */
    public MemorySegment allocate(long byteSize) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "pool {0} allocating {1} bytes", name, byteSize);
        return arena.allocate(byteSize);
    }

    /**
     * Allocates an off heap segment laid out for the given memory layout from this pool.
     *
     * <p>The segment is sized and aligned for {@code layout} and is valid until this pool is closed.
     *
     * @param layout the layout that sizes and aligns the segment
     * @return a new zero initialized native segment for {@code layout}
     * @throws NullPointerException  if {@code layout} is {@code null}
     * @throws IllegalStateException if this pool has been closed or is accessed from another thread
     */
    public MemorySegment allocate(MemoryLayout layout) {
        Objects.requireNonNull(layout, "layout cannot be null");
        if (Log.TRACE) LOGGER.log(Level.TRACE, "pool {0} allocating {1} bytes for layout", name, layout.byteSize());
        return arena.allocate(layout);
    }

    /**
     * Releases every segment this pool allocated.
     *
     * <p>After this call the backing arena is closed and all segments it handed out are invalidated;
     * touching any of them afterward raises an {@link IllegalStateException}. Closing a pool more than
     * once is rejected by the backing arena. Every segment the pool handed out is released at once.
     *
     * @throws IllegalStateException if this pool has already been closed or is closed from a thread other
     *                               than the one that created it
     */
    @Override
    public void close() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing memory pool {0}", name);
        arena.close();
    }
}
