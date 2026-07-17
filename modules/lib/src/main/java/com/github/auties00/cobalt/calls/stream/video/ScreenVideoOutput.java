package com.github.auties00.cobalt.calls.stream.video;

import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.calls.stream.VideoPixelFormat;

/**
 * Captures the system's screen as the local video of a call for screen sharing.
 *
 * <p>This is the device backed {@link VideoOutput} returned by {@link VideoOutput#fromScreen()}. A screen
 * grab is a {@link CameraVideoOutput} pointed at a
 * libavdevice screen grab input format, so it produces {@link VideoPixelFormat#I420 I420} frames through
 * the same libavdevice, libavcodec, and libswscale pipeline that camera capture uses. {@link #primary()}
 * detects the running platform and captures its conventional default display. The named factories cover
 * the cases the platform default does not: {@link #x11(String)}, {@link #wayland(String)}, and
 * {@link #drm(String)} for the Linux display servers, {@link #macOs(int)} for a specific macOS screen,
 * and {@link #windowsDesktop()} or {@link #windowsWindow(String)} for the whole Windows desktop or a
 * single window. Each factory selects the screen grab input format conventional for its platform:
 *
 * <ul>
 *   <li>Linux X11: {@code xcbgrab}</li>
 *   <li>Linux Wayland: {@code pipewiregrab}</li>
 *   <li>Linux DRM: {@code kmsgrab} (requires root)</li>
 *   <li>macOS: {@code avfoundation}</li>
 *   <li>Windows: {@code gdigrab}</li>
 * </ul>
 *
 * <p>Each factory advertises the captured display's own native resolution, capped to {@code 1280} on the
 * longer side and rounded to even, detected when the screen grab device is opened rather than supplied by
 * the caller, at the default 30 frames per second and the recovered initial bitrate; the engine encodes
 * the share at that detected geometry.
 *
 * <p>Prefer {@link #primary()} for the simple "share my main display" case, and reach for a named
 * factory only when the platform default is wrong, for example on Wayland (where {@link #primary()}
 * assumes X11) or to pick one monitor of a setup with multiple displays.
 */
public final class ScreenVideoOutput extends CameraVideoOutput {
    /**
     * Opens a screen grab device for the given libavdevice input format and URL pair, detecting its native
     * geometry.
     *
     * @param indev the libavdevice screen grab input format name (for example {@code "xcbgrab"})
     * @param url   the screen grab device URL
     * @throws NullPointerException  if {@code indev} or {@code url} is {@code null}
     * @throws IllegalStateException if the named input format is unavailable in this build, the device
     *                               cannot be opened, or it has no video stream
     */
    private ScreenVideoOutput(String indev, String url) {
        super(indev, url);
    }

    /**
     * Returns a source capturing the platform's default screen, detecting its native geometry.
     *
     * <p>Selects the macOS screen at index {@code 0}, the whole Windows desktop, or the X11 display
     * {@code ":0.0"} according to the running platform. On Linux this assumes X11; a Wayland session
     * must instead use {@link #wayland(String)}.
     *
     * @return a source capturing the primary display
     * @throws IllegalStateException if screen capture is unavailable on the running platform
     */
    public static ScreenVideoOutput primary() {
        var os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return macOs(0);
        if (os.contains("win")) return windowsDesktop();
        return x11(":0.0");
    }

    /**
     * Returns a source capturing an X11 display through the {@code xcbgrab} input format, detecting its
     * native geometry.
     *
     * @param display the X11 display string (for example {@code ":0.0"})
     * @return a source capturing the given X11 display
     * @throws IllegalStateException if screen capture is unavailable on the running platform
     */
    public static ScreenVideoOutput x11(String display) {
        return new ScreenVideoOutput("xcbgrab", display);
    }

    /**
     * Returns a source capturing a Wayland source through the {@code pipewiregrab} input format, detecting
     * its native geometry.
     *
     * @param node the PipeWire stanza name to capture
     * @return a source capturing the given PipeWire stanza
     * @throws IllegalStateException if screen capture is unavailable on the running platform
     */
    public static ScreenVideoOutput wayland(String node) {
        return new ScreenVideoOutput("pipewiregrab", node);
    }

    /**
     * Returns a source capturing a Linux DRM card through the {@code kmsgrab} input format, detecting its
     * native geometry.
     *
     * <p>Capturing a DRM card requires elevated privileges (root or {@code CAP_SYS_ADMIN}).
     *
     * @param card the DRM card path (for example {@code "/dev/dri/card0"})
     * @return a source capturing the given DRM card
     * @throws IllegalStateException if screen capture is unavailable on the running platform
     */
    public static ScreenVideoOutput drm(String card) {
        return new ScreenVideoOutput("kmsgrab", card);
    }

    /**
     * Returns a source capturing a macOS screen through the {@code avfoundation} input format, detecting
     * its native geometry.
     *
     * <p>Passing only the screen index yields a video only capture with no audio.
     *
     * @param screenIndex the AVFoundation screen index
     * @return a source capturing the given macOS screen
     * @throws IllegalStateException if screen capture is unavailable on the running platform
     */
    public static ScreenVideoOutput macOs(int screenIndex) {
        return new ScreenVideoOutput("avfoundation", screenIndex + ":");
    }

    /**
     * Returns a source capturing the entire Windows desktop through the {@code gdigrab} input format,
     * detecting its native geometry.
     *
     * @return a source capturing the Windows desktop
     * @throws IllegalStateException if screen capture is unavailable on the running platform
     */
    public static ScreenVideoOutput windowsDesktop() {
        return new ScreenVideoOutput("gdigrab", "desktop");
    }

    /**
     * Returns a source capturing a single Windows window by title through the {@code gdigrab} input
     * format, detecting its native geometry.
     *
     * @param title the title of the window to capture
     * @return a source capturing the given Windows window
     * @throws IllegalStateException if screen capture is unavailable on the running platform
     */
    public static ScreenVideoOutput windowsWindow(String title) {
        return new ScreenVideoOutput("gdigrab", "title=" + title);
    }
}
