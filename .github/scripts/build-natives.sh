#!/usr/bin/env bash
#
# Builds Cobalt's native dependency bundle.
#
# Each dependency is built as a PIC static archive, small C/C++ shims expose the
# FFM-facing symbols, and build_combined links everything into one platform
# library under modules/lib/natives/bin/<classifier>/.
#
# Exported symbols come from each dependency's generate.sh --include-function
# list, so the final library exposes only what the Java bindings call.
#
# Set <DEP>_SRC to reuse a local checkout; otherwise the pinned ref is cloned.
# Required tools: git, python3, autotools, pkg-config, nasm/yasm, cmake, meson,
# ninja, make, Rust/cargo for rav1e, and the host C/C++ toolchain. Optional: lld
# (used for the final link's identical-code-folding when on PATH).

set -Eeuo pipefail
trap 'echo "[build-natives] FAILED at ${BASH_SOURCE[0]}:${LINENO}: ${BASH_COMMAND}" >&2' ERR

case "$(uname -s)" in
    Linux)                OS=linux ;;
    Darwin)               OS=darwin ;;
    MINGW*|MSYS*|CYGWIN*) OS=windows ;;
    *) echo "unsupported OS: $(uname -s)" >&2; exit 1 ;;
esac
case "$(${CC:-cc} -dumpmachine 2>/dev/null)" in
    aarch64*|arm64*) ARCH=aarch64 ;;
    x86_64*|amd64*)  ARCH=x86_64 ;;
    *)
        case "$(uname -m)" in
            x86_64|amd64)  ARCH=x86_64 ;;
            aarch64|arm64) ARCH=aarch64 ;;
            *) echo "unsupported arch: $(uname -m)" >&2; exit 1 ;;
        esac
        ;;
esac
CLASSIFIER="$OS-$ARCH"

# Build for the oldest supported OS so the dylibs run on every macOS, not the runner's.
if [ "$OS" = darwin ]; then
    if [ "$ARCH" = aarch64 ]; then
        export MACOSX_DEPLOYMENT_TARGET=11.0
    else
        export MACOSX_DEPLOYMENT_TARGET=10.13
    fi
fi

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$DIR/../.." && pwd)"
DEPS="$ROOT/modules/lib/dependencies"
NATIVES="$ROOT/modules/lib/natives"
BUILD="${BUILD_CACHE:-/tmp/cobalt-natives-build}"
case "$BUILD" in
    *[[:space:]]*)
        echo "[build-natives] FAIL: BUILD path contains whitespace: '$BUILD' (autotools rejects this). Set BUILD_CACHE to a space-free path." >&2
        exit 1
        ;;
esac
mkdir -p "$BUILD"
JOBS="$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4)"

# All static archives are linked into a shared library, so they must be PIC.
# Split sections let the final linker drop unused code.
SECTIONS_CFLAGS="-O2 -DNDEBUG -ffunction-sections -fdata-sections -fPIC"

# Codec inner loops are hand-written asm, so -O2 matches -O3 throughput without
# its code-size inflation. Per-dependency LTO stays off because -Wl,-u and LTO
# archives do not mix reliably; FFmpeg performs its own LTO link.
CODEC_CFLAGS="-O2 -DNDEBUG -ffunction-sections -fdata-sections -fPIC"

# Pure-C deps never throw, so their async unwind tables are dead weight. Kept OFF
# the C++ libs and the FFI shims, which must stay unwindable.
C_ONLY_CFLAGS="-fno-asynchronous-unwind-tables"

# C++ that neither throws nor uses RTTI drops exception tables and type_info.
CXX_LEAN_CFLAGS="-fno-exceptions -fno-rtti"

# Hide webrtc-apm internals so its RTC_EXPORT API cannot leak into the export table.
CXX_HIDDEN_CFLAGS="-fvisibility=hidden -fvisibility-inlines-hidden"

# MinGW runtime libs are linked statically to avoid extra DLL dependencies.
MINGW_CFLAGS=""
if [ "$OS" = windows ]; then
    MINGW_CFLAGS="-static-libgcc"
fi

# Shims/neutral C: keep unwind (FFI boundary).
EXTRA_CFLAGS="$SECTIONS_CFLAGS $MINGW_CFLAGS"
# openh264 (C++ codec): unwind kept.
CODEC_EXTRA_CFLAGS="$CODEC_CFLAGS $MINGW_CFLAGS"
# opus, libvpx, dav1d, libwebp (pure-C codecs): no unwind tables.
C_CODEC_EXTRA_CFLAGS="$CODEC_CFLAGS $C_ONLY_CFLAGS $MINGW_CFLAGS"
# FFmpeg: -O2 is set via --optflags at configure (not here); no unwind tables.
FFMPEG_EXTRA_CFLAGS="-DNDEBUG -ffunction-sections -fdata-sections -fPIC $C_ONLY_CFLAGS $MINGW_CFLAGS"

# openh264 is C++; advertise the matching C++ runtime in pkg-config shims.
case "$OS" in
    darwin) CXXLIB="-lc++" ;;
    *)      CXXLIB="-lstdc++" ;;
esac

OPUS_REPO=https://github.com/xiph/opus.git
OPUS_REF=v1.5.2

OPENH264_REPO=https://github.com/cisco/openh264.git
OPENH264_REF=v2.4.1

LIBVPX_REPO=https://chromium.googlesource.com/webm/libvpx
LIBVPX_REF=v1.15.1

DAV1D_REPO=https://code.videolan.org/videolan/dav1d.git
DAV1D_REF=1.4.3
RAV1E_REPO=https://github.com/xiph/rav1e.git
RAV1E_REF=v0.7.1

LIBWEBP_REPO=https://chromium.googlesource.com/webm/libwebp
LIBWEBP_REF=v1.5.0

FFMPEG_REPO=https://github.com/FFmpeg/FFmpeg.git
FFMPEG_REF=n7.1

WEBRTC_APM_REPO=https://gitlab.freedesktop.org/pulseaudio/webrtc-audio-processing.git
WEBRTC_APM_REF=v2.1

log()  { echo "[build-natives] $*"; }
fail() { echo "[build-natives] FAIL: $*" >&2; exit 1; }

ensure_src() {
    local var="$1" repo="$2" ref="$3" dirname="$4"
    local val="${!var-}"
    if [ -n "$val" ]; then
        [ -d "$val" ] || fail "$var=$val does not exist"
        log "$var override: $val"
        return 0
    fi
    local workdir="$BUILD/$dirname"
    if [ -d "$workdir/.git" ] && git -C "$workdir" rev-parse --verify "$ref^{commit}" >/dev/null 2>&1; then
        log "$dirname cached at $ref"
        git -C "$workdir" -c advice.detachedHead=false checkout --force "$ref"
        git -C "$workdir" clean -fdx
    else
        rm -rf "$workdir"
        if ! git clone --depth 1 --branch "$ref" "$repo" "$workdir"; then
            log "shallow clone of $repo @ $ref failed, retrying full clone"
            rm -rf "$workdir"
            git clone "$repo" "$workdir"
            git -C "$workdir" checkout "$ref"
        fi
    fi
    printf -v "$var" '%s' "$workdir"
    export "$var"
}

vendor_headers() {
    local src="$1" dest="$2"
    [ -d "$src" ] || fail "vendor_headers: source dir $src does not exist"
    mkdir -p "$dest"
    find "$dest" -maxdepth 1 -name '*.h' -type f -delete 2>/dev/null || true
    cp "$src"/*.h "$dest/"
}

build_opus() {
    log "opus (static, -O2)"
    ensure_src OPUS_SRC "$OPUS_REPO" "$OPUS_REF" opus
    [ -x "$OPUS_SRC/configure" ] || ( cd "$OPUS_SRC" && autoreconf -isf )
    local b="$BUILD/build-opus"
    rm -rf "$b" && mkdir -p "$b"
    # Windows-on-ARM has no opus RTCD backend; NEON is baseline on ARMv8.
    local opus_extra=""
    if [ "$OS" = windows ] && [ "$ARCH" = aarch64 ]; then
        opus_extra="--disable-rtcd"
    fi
    # shellcheck disable=SC2086
    ( cd "$b" && CFLAGS="${CFLAGS:-} $C_CODEC_EXTRA_CFLAGS" \
        "$OPUS_SRC/configure" --prefix="$b/inst" \
        --disable-shared --enable-static --with-pic \
        --disable-doc --disable-extra-programs \
        --disable-deep-plc --disable-dred \
        $opus_extra )
    make -C "$b" -j "$JOBS"
    make -C "$b" install
    # Vendor headers for compiling the shim; Java binds the shim header.
    vendor_headers "$b/inst/include/opus" "$DEPS/libopus/headers/opus"
    # Build the extern-C wrapper that exports cobalt_opus_* symbols.
    local cc="${CC:-cc}"
    local shim_src="$DEPS/libopus/cobalt_opus_shim.c"
    [ -f "$shim_src" ] || fail "libopus shim source not found: $shim_src"
    "$cc" -std=c11 -O2 -DNDEBUG $EXTRA_CFLAGS \
        -I "$DEPS/libopus" -I "$b/inst/include" \
        -c "$shim_src" -o "$b/cobalt_opus_shim.o"
    ar rcs "$b/libcobalt_opus_shim.a" "$b/cobalt_opus_shim.o"
}

# Keep libopenh264.a intact; the final shared-library link strips unused code.
build_openh264() {
    log "openh264 (static, Release -O2)"
    ensure_src OPENH264_SRC "$OPENH264_REPO" "$OPENH264_REF" openh264
    local make_os make_arch
    case "$OS" in
        linux)   make_os=linux ;;
        darwin)  make_os=darwin ;;
        windows) make_os=mingw_nt ;;
    esac
    case "$ARCH" in
        x86_64)  make_arch=x86_64 ;;
        aarch64) make_arch=arm64 ;;
    esac
    make -C "$OPENH264_SRC" OS="$make_os" ARCH="$make_arch" clean 2>/dev/null || true
    make -C "$OPENH264_SRC" OS="$make_os" ARCH="$make_arch" BUILDTYPE=Release \
        CC="${CC:-cc}" CXX="${CXX:-c++}" \
        CFLAGS="${CFLAGS:-} $CODEC_EXTRA_CFLAGS $CXX_LEAN_CFLAGS" \
        -j "$JOBS" libopenh264.a
    [ -f "$OPENH264_SRC/libopenh264.a" ] || fail "openh264 static archive not produced"
    # Vendor public API headers used by the shim.
    vendor_headers "$OPENH264_SRC/codec/api/wels" "$DEPS/openh264/headers"
    # Build the C++ wrapper that exports cobalt_h264_* symbols.
    local b="$BUILD/build-openh264"
    rm -rf "$b" && mkdir -p "$b"
    local cxx="${CXX:-c++}"
    local shim_src="$DEPS/openh264/cobalt_h264_shim.cpp"
    [ -f "$shim_src" ] || fail "openh264 shim source not found: $shim_src"
    "$cxx" -std=c++17 -O2 -DNDEBUG $EXTRA_CFLAGS \
        -I "$DEPS/openh264" -I "$DEPS/openh264/headers" \
        -c "$shim_src" -o "$b/cobalt_h264_shim.o"
    ar rcs "$b/libcobalt_h264_shim.a" "$b/cobalt_h264_shim.o"
}

build_libvpx() {
    log "libvpx (static)"
    ensure_src LIBVPX_SRC "$LIBVPX_REPO" "$LIBVPX_REF" libvpx
    local target
    case "$OS-$ARCH" in
        linux-x86_64)   target=x86_64-linux-gcc ;;
        linux-aarch64)  target=arm64-linux-gcc ;;
        windows-x86_64) target=x86_64-win64-gcc ;;
        windows-aarch64) target=arm64-win64-gcc ;;
        darwin-x86_64)  target=x86_64-darwin17-gcc ;;
        darwin-aarch64) target=arm64-darwin20-gcc ;;
    esac
    local b="$BUILD/build-libvpx"
    rm -rf "$b" && mkdir -p "$b"
    # Bindings cover VP8 and VP9 encode/decode, so enable both families.
    ( cd "$b" && CC="${CC:-cc}" CXX="${CXX:-c++}" CFLAGS="${CFLAGS:-} $C_CODEC_EXTRA_CFLAGS" \
        "$LIBVPX_SRC/configure" \
        --target="$target" --prefix="$b/inst" \
        --disable-shared --enable-static --enable-pic \
        --enable-vp8 --enable-vp8-encoder --enable-vp8-decoder \
        --enable-vp9 --enable-vp9-encoder --enable-vp9-decoder \
        --enable-runtime-cpu-detect --enable-small \
        --disable-examples --disable-tools --disable-docs --disable-unit-tests )
    make -C "$b" -j "$JOBS"
    make -C "$b" install
    # Vendor headers for compiling the shim; Java binds the shim header.
    vendor_headers "$b/inst/include/vpx" "$DEPS/libvpx/headers/vpx"
    # Build the extern-C wrapper that exports cobalt_vpx_* symbols.
    local cc="${CC:-cc}"
    local shim_src="$DEPS/libvpx/cobalt_vpx_shim.c"
    [ -f "$shim_src" ] || fail "libvpx shim source not found: $shim_src"
    "$cc" -std=c11 -O2 -DNDEBUG $EXTRA_CFLAGS \
        -I "$DEPS/libvpx" -I "$b/inst/include" \
        -c "$shim_src" -o "$b/cobalt_vpx_shim.o"
    ar rcs "$b/libcobalt_vpx_shim.a" "$b/cobalt_vpx_shim.o"
}

# dav1d provides AV1 decode only.
build_av1() {
    log "dav1d / AV1 decode (static, -O2, 8-bit only)"
    command -v meson >/dev/null 2>&1 || fail "dav1d needs meson on PATH (pip install meson ninja)"
    command -v ninja >/dev/null 2>&1 || fail "dav1d needs ninja on PATH (pip install meson ninja)"
    ensure_src DAV1D_SRC "$DAV1D_REPO" "$DAV1D_REF" dav1d
    local b="$BUILD/build-dav1d"
    rm -rf "$b"
    # The call video path is 8-bit, so omit 16-bit DSP objects.
    meson setup "$b" "$DAV1D_SRC" \
        --prefix="$b/inst" \
        --libdir=lib \
        --default-library=static \
        --buildtype=release \
        -Dbitdepths=8 \
        -Denable_tools=false \
        -Denable_tests=false \
        -Db_staticpic=true \
        -Dc_args="${CFLAGS:-} $CODEC_CFLAGS $C_ONLY_CFLAGS"
    ninja -C "$b"
    ninja -C "$b" install
    # Vendor public headers for compiling the shim.
    rm -f "$DEPS/dav1d/headers"/*.h
    cp "$b/inst/include/dav1d"/*.h "$DEPS/dav1d/headers/"
    # Build the extern-C wrapper that exports cobalt_dav1d_* symbols.
    local cc="${CC:-cc}"
    local shim_src="$DEPS/dav1d/cobalt_dav1d_shim.c"
    [ -f "$shim_src" ] || fail "dav1d shim source not found: $shim_src"
    "$cc" -std=c11 -O2 -DNDEBUG $EXTRA_CFLAGS \
        -I "$DEPS/dav1d" -I "$b/inst/include/dav1d" \
        -c "$shim_src" -o "$b/cobalt_dav1d_shim.o"
    ar rcs "$b/libcobalt_dav1d_shim.a" "$b/cobalt_dav1d_shim.o"
}

# rav1e provides AV1 encode through cargo-c's C API.
build_rav1e() {
    log "rav1e / AV1 encode (static, release, cargo-c)"
    command -v cargo >/dev/null 2>&1 || fail "rav1e needs cargo (Rust toolchain) on PATH"
    # cargo-c provides cargo cinstall and the static C API.
    cargo cinstall --help >/dev/null 2>&1 || cargo install cargo-c
    ensure_src RAV1E_SRC "$RAV1E_REPO" "$RAV1E_REF" rav1e
    local b="$BUILD/build-rav1e"
    rm -rf "$b"
    # rav1e's aarch64 asm compiles through cc-rs; versions before the LLVM 19 fix
    # pass a gnullvm triple clang rejects, so refresh cc-rs on Windows-on-ARM. Keep
    # asm for SIMD; drop only git_version so libgit2 (unused) is not built.
    local rav1e_features=""
    if [ "$OS" = windows ] && [ "$ARCH" = aarch64 ]; then
        ( cd "$RAV1E_SRC" && cargo update -p cc )
        rav1e_features="--no-default-features --features=asm,threading,capi"
    fi
    # Install librav1e.a, rav1e.h, and rav1e.pc under $b/inst. Override rav1e's
    # release profile: fat LTO + one codegen unit and no debug info shrink the
    # archive; panic=abort drops landing pads (panics never cross the FFI). opt-level
    # stays at the default 3 so the encoder is not slowed.
    # shellcheck disable=SC2086
    ( cd "$RAV1E_SRC" && \
      CARGO_TARGET_DIR="$b/target" \
      CARGO_PROFILE_RELEASE_LTO=fat \
      CARGO_PROFILE_RELEASE_CODEGEN_UNITS=1 \
      CARGO_PROFILE_RELEASE_PANIC=abort \
      CARGO_PROFILE_RELEASE_DEBUG=false \
      CARGO_PROFILE_RELEASE_INCREMENTAL=false \
      cargo cinstall --release \
        $rav1e_features \
        --library-type=staticlib \
        --prefix="$b/inst" --libdir=lib --includedir=include )
    # Vendor the generated C API header for compiling the shim.
    rm -f "$DEPS/rav1e/headers"/*.h
    mkdir -p "$DEPS/rav1e/headers"
    cp "$b/inst/include/rav1e"/*.h "$DEPS/rav1e/headers/"
    # Build the extern-C wrapper that exports cobalt_rav1e_* symbols.
    local cc="${CC:-cc}"
    local shim_src="$DEPS/rav1e/cobalt_rav1e_shim.c"
    [ -f "$shim_src" ] || fail "rav1e shim source not found: $shim_src"
    "$cc" -std=c11 -O2 -DNDEBUG $EXTRA_CFLAGS \
        -I "$DEPS/rav1e" -I "$b/inst/include/rav1e" \
        -c "$shim_src" -o "$b/cobalt_rav1e_shim.o"
    ar rcs "$b/libcobalt_rav1e_shim.a" "$b/cobalt_rav1e_shim.o"
}

# FFmpeg consumes libwebp; Cobalt does not bind libwebp directly.
build_libwebp() {
    log "libwebp (static)"
    ensure_src LIBWEBP_SRC "$LIBWEBP_REPO" "$LIBWEBP_REF" libwebp
    [ -x "$LIBWEBP_SRC/configure" ] || ( cd "$LIBWEBP_SRC" && ./autogen.sh )
    local b="$BUILD/build-libwebp"
    rm -rf "$b" && mkdir -p "$b"
    ( cd "$b" && CFLAGS="${CFLAGS:-} $C_CODEC_EXTRA_CFLAGS" \
        "$LIBWEBP_SRC/configure" --prefix="$b/inst" \
        --disable-shared --enable-static --with-pic \
        --disable-libwebpmux --disable-libwebpdemux \
        --disable-cwebp --disable-dwebp \
        --disable-png --disable-jpeg --disable-tiff --disable-gif --disable-wic )
    make -C "$b" -j "$JOBS"
    make -C "$b" install
    vendor_headers "$b/inst/include/webp" "$DEPS/libwebp/headers/webp"
}

vendor_ffmpeg_headers() {
    local inst_include="$1"
    local dest="$DEPS/ffmpeg/headers"
    for lib in libavformat libavcodec libavdevice libavfilter libavutil libswscale libswresample; do
        local src="$inst_include/$lib"
        local out="$dest/$lib"
        [ -d "$src" ] || continue
        rm -rf "$out" && mkdir -p "$out"
        cp "$src"/*.h "$out/"
    done
    if [ -f "$FFMPEG_SRC/VERSION" ]; then
        cp "$FFMPEG_SRC/VERSION" "$dest/UPSTREAM_VERSION"
    elif [ -f "$FFMPEG_SRC/RELEASE" ]; then
        cp "$FFMPEG_SRC/RELEASE" "$dest/UPSTREAM_VERSION"
    fi
}

# FFmpeg install prefix consumed by build_combined through pkg-config.
FFMPEG_INST="$BUILD/build-ffmpeg/build/inst"
# Synthetic pkg-config files for static codec dependencies.
FFMPEG_PC_DIR="$BUILD/build-ffmpeg/pc"

build_ffmpeg() {
    log "ffmpeg (static)"
    ensure_src FFMPEG_SRC "$FFMPEG_REPO" "$FFMPEG_REF" ffmpeg
    local indevs=()
    case "$OS" in
        linux)   indevs+=(--enable-indev=v4l2 --enable-indev=kmsgrab --enable-indev=xcbgrab) ;;
        darwin)  indevs+=(--enable-indev=avfoundation) ;;
        windows) indevs+=(--enable-indev=dshow --enable-indev=gdigrab) ;;
    esac
    rm -rf "$FFMPEG_PC_DIR" && mkdir -p "$FFMPEG_PC_DIR"
    local opus_inst="$BUILD/build-opus/inst"
    local vpx_inst="$BUILD/build-libvpx/inst"
    local webp_inst="$BUILD/build-libwebp/inst"
    local h264_stage="$BUILD/build-openh264/stage"
    rm -rf "$h264_stage" && mkdir -p "$h264_stage/lib" "$h264_stage/include/wels"
    cp "$OPENH264_SRC/libopenh264.a" "$h264_stage/lib/"
    cp "$OPENH264_SRC"/codec/api/wels/*.h "$h264_stage/include/wels/"
    # $6 is Libs.private: dependencies required only for static linking.
    emit_pc() {
        local n="$1" lib="$2" ver="$3" libdir="$4" inc="$5" priv="${6:-}"
        cat > "$FFMPEG_PC_DIR/${n}.pc" <<EOF
libdir=$libdir
includedir=$inc

Name: $n
Description: $n
Version: $ver
Libs: -L\${libdir} -l$lib
Libs.private: $priv
Cflags: -I\${includedir}
EOF
    }
    emit_pc opus     opus     1.5.2  "$opus_inst/lib"  "$opus_inst/include/opus" "-lm"
    emit_pc vpx      vpx      1.15.1 "$vpx_inst/lib"   "$vpx_inst/include"       "-lm -lpthread"
    emit_pc openh264 openh264 2.4.1  "$h264_stage/lib" "$h264_stage/include"     "$CXXLIB -lm"
    emit_pc libwebp  webp     1.5.0  "$webp_inst/lib"  "$webp_inst/include"      "-lsharpyuv -lm -lpthread"

    local b="$BUILD/build-ffmpeg/build"
    rm -rf "$b" && mkdir -p "$b"
    # MSYS2 base is x86_64 but the toolchain targets aarch64; pin ffmpeg's arch.
    local ffmpeg_extra=""
    if [ "$OS" = windows ] && [ "$ARCH" = aarch64 ]; then
        ffmpeg_extra="--arch=aarch64 --target-os=mingw32"
    fi
    # shellcheck disable=SC2086
    ( cd "$b" && PKG_CONFIG_LIBDIR="$FFMPEG_PC_DIR" "$FFMPEG_SRC/configure" \
        --prefix="$b/inst" \
        --cc="${CC:-cc}" --cxx="${CXX:-c++}" \
        --extra-cflags="${CFLAGS:-} $FFMPEG_EXTRA_CFLAGS" \
        --disable-everything --disable-programs \
        --disable-doc --disable-htmlpages --disable-manpages --disable-podpages --disable-txtpages \
        --disable-network --enable-static --disable-shared \
        --enable-pic --enable-lto --optflags=-O2 \
        --disable-iconv \
        --pkg-config-flags=--static \
        --enable-demuxer=mov,matroska,ogg,mp3,wav,flac,mp4,aac,image2,webp_pipe,jpeg_pipe \
        --enable-muxer=matroska,mov,wav,mp4,ipod,webp,image2,ogg,opus \
        --enable-parser=h264,aac,mpegaudio,vp8,opus \
        --enable-protocol=file \
        --enable-filter=scale,format,fps,crop,transpose,thumbnail \
        --enable-swscale --enable-swresample \
        --enable-avdevice \
        "${indevs[@]}" \
        --enable-encoder=mjpeg,aac \
        --enable-decoder=mjpeg,aac,flac,mp3,pcm_s16le,pcm_s16be,pcm_u8,vorbis \
        --enable-libopenh264 --enable-encoder=libopenh264 --enable-decoder=libopenh264 \
        --enable-libopus    --enable-encoder=libopus    --enable-decoder=libopus    \
        --enable-libvpx     --enable-encoder=libvpx_vp8 --enable-decoder=libvpx_vp8 \
        --enable-libwebp    --enable-encoder=libwebp    --enable-decoder=libwebp    \
        --disable-decoder=h264,opus,vp8 \
        --disable-encoder=vp9 \
        $ffmpeg_extra )
    make -C "$b" -j "$JOBS"
    make -C "$b" install
    vendor_ffmpeg_headers "$b/inst/include"
}

# WebRTC APM provides capture-side audio processing.
build_webrtc_apm() {
    local shim_src="$DEPS/webrtc-apm/cobalt_webrtc_apm_shim.cpp"
    [ -f "$shim_src" ] || fail "webrtc-apm shim source not found: $shim_src"
    log "webrtc-audio-processing (static, release)"
    command -v meson >/dev/null 2>&1 || fail "webrtc-apm needs meson on PATH (pip install meson ninja)"
    command -v ninja >/dev/null 2>&1 || fail "webrtc-apm needs ninja on PATH (pip install meson ninja)"
    ensure_src WEBRTC_APM_SRC "$WEBRTC_APM_REPO" "$WEBRTC_APM_REF" webrtc-apm
    # RTC_EXPORT -> __declspec(dllexport) leaks the webrtc/rtc API into the export
    # table (dllexport overrides -fvisibility=hidden), and each export roots dead
    # code against --gc-sections. Neutralize it so only the .def controls exports.
    local export_hdr
    export_hdr=$(find "$WEBRTC_APM_SRC" -name rtc_export.h -type f | head -1)
    if [ -n "$export_hdr" ]; then
        sed 's/__declspec(dllexport)//g; s/__declspec(dllimport)//g' "$export_hdr" > "$export_hdr.tmp" \
            && mv "$export_hdr.tmp" "$export_hdr"
        log "neutralized dllexport in $export_hdr"
    else
        log "WARN: rtc_export.h not found; relying on -fvisibility=hidden + --exclude-all-symbols"
    fi
    local b="$BUILD/build-webrtc-apm"
    rm -rf "$b"
    meson setup "$b" "$WEBRTC_APM_SRC" \
        --prefix="$b/inst" \
        --libdir=lib \
        --default-library=static \
        --buildtype=release \
        -Db_staticpic=true \
        -Dcpp_args="${CXXFLAGS:-} -O3 -DNDEBUG -ffunction-sections -fdata-sections -fPIC $CXX_HIDDEN_CFLAGS $CXX_LEAN_CFLAGS -include cstdint -include cstddef"
    if [ "$OS" = windows ]; then
        # abseil's NTDDI_WIN10_NI branch enables WinRT calls undeclared under MinGW.
        local tzsrc
        tzsrc=$(find "$WEBRTC_APM_SRC/subprojects" -path '*/absl/time/internal/cctz/src/time_zone_lookup.cc' -type f | head -1)
        if [ -n "$tzsrc" ]; then
            sed 's/defined(NTDDI_WIN10_NI) && NTDDI_VERSION >= NTDDI_WIN10_NI/& \&\& !defined(__MINGW32__)/' \
                "$tzsrc" > "$tzsrc.tmp" && mv "$tzsrc.tmp" "$tzsrc"
            log "patched abseil time_zone_lookup.cc for MinGW"
        fi
    fi
    ninja -C "$b"
    ninja -C "$b" install
    # Build the C++ wrapper that exports cobalt_webrtc_apm_* symbols.
    local apm_inc; apm_inc=$(find "$b/inst/include" -maxdepth 1 -type d -name 'webrtc-audio-processing*' | head -1)
    [ -n "$apm_inc" ] || fail "webrtc-audio-processing include dir not produced"
    local cxx="${CXX:-c++}"
    local absl_inc; absl_inc=$(find "$WEBRTC_APM_SRC/subprojects" -maxdepth 1 -type d -name 'abseil-cpp*' | head -1)
    "$cxx" -std=c++17 -O2 -DNDEBUG $EXTRA_CFLAGS \
        -I "$DEPS/webrtc-apm/headers" -I "$apm_inc" -I "$absl_inc" \
        -c "$shim_src" -o "$b/cobalt_webrtc_apm_shim.o"
    ar rcs "$b/libcobalt_webrtc_apm_shim.a" "$b/cobalt_webrtc_apm_shim.o"
}

# Export exactly the functions listed in dependency generate.sh files.
gen_exports() {
    local out="$1"
    local gens=()
    local g
    while IFS= read -r g; do
        gens+=("$g")
    done < <(find "$DEPS" -maxdepth 2 -name generate.sh -type f | sort)
    [ "${#gens[@]}" -gt 0 ] || fail "no generate.sh under $DEPS/*/ to derive exports from"
    for g in "${gens[@]}"; do
        # Ignore comments so prose cannot add fake symbols.
        awk '/^[[:space:]]*#/ { next } { for (i = 1; i <= NF; i++) if ($i == "--include-function") print $(i + 1) }' "$g"
    done | sort -u > "$out"
    [ -s "$out" ] || fail "gen_exports produced no symbols"
    log "exports: $(wc -l < "$out" | tr -d ' ') symbols -> $out"
}

# Emit the linker export-control file for the current platform.
write_export_file() {
    local exports="$1" b="$2"
    case "$OS" in
        linux)
            local vs="$b/exports.map"
            { echo '{'; echo '  global:'; sed 's/^/    /; s/$/;/' "$exports"; \
              echo '  local:'; echo '    *;'; echo '};'; } > "$vs"
            echo "$vs"
            ;;
        darwin)
            local sl="$b/exports.syms"
            sed 's/^/_/' "$exports" > "$sl"
            echo "$sl"
            ;;
        windows)
            local def="$b/exports.def"
            { echo 'EXPORTS'; cat "$exports"; } > "$def"
            echo "$def"
            ;;
    esac
}

# Log an archive's .text total (from size -t) and on-disk size. Pre-gc-sections,
# so a proxy for each library's contribution, not its exact slice of the DLL.
log_footprint() {
    local label="$1" path="$2" text=""
    [ -f "$path" ] || return 0
    text=$(size "$path" 2>/dev/null | awk '$1 ~ /^[0-9]+$/ { sum += $1 } END { print sum + 0 }') || text=""
    case "$text" in ''|*[!0-9]*) text=0 ;; esac
    printf '[build-natives] footprint %-18s text=%7d KiB  archive=%7d KiB\n' \
        "$label" "$(( text / 1024 ))" "$(( $(wc -c < "$path") / 1024 ))"
}

build_combined() {
    log "combined libcobalt-native"
    local b="$BUILD/build-combined"
    rm -rf "$b" && mkdir -p "$b"

    local exports="$b/exports.txt"
    gen_exports "$exports"

    # Link archives that are outside or not reliably rooted by FFmpeg's closure.
    local apm_archive; apm_archive=$(find "$BUILD/build-webrtc-apm/inst/lib" -maxdepth 1 -name 'libwebrtc-audio-processing*.a' | head -1)
    [ -n "$apm_archive" ] || fail "webrtc-audio-processing static archive not found next to its shim"
    local extra_archives=(
        "$BUILD/build-dav1d/libcobalt_dav1d_shim.a"
        "$BUILD/build-dav1d/inst/lib/libdav1d.a"
        "$BUILD/build-rav1e/libcobalt_rav1e_shim.a"
        "$BUILD/build-rav1e/inst/lib/librav1e.a"
        "$BUILD/build-libvpx/libcobalt_vpx_shim.a"
        "$BUILD/build-opus/libcobalt_opus_shim.a"
        "$BUILD/build-openh264/libcobalt_h264_shim.a"
        "$BUILD/build-webrtc-apm/libcobalt_webrtc_apm_shim.a"
        "$apm_archive"
    )

    local a
    for a in "${extra_archives[@]}"; do
        [ -f "$a" ] || fail "missing static archive: $a"
    done

    # Ask pkg-config for FFmpeg's complete static link closure.
    local ff_libs
    ff_libs=$(PKG_CONFIG_PATH="$FFMPEG_INST/lib/pkgconfig:$FFMPEG_PC_DIR" \
        pkg-config --static --libs \
        libavdevice libavfilter libavformat libavcodec libswscale libswresample libavutil) \
        || fail "pkg-config failed to resolve ffmpeg static closure"

    local footprints=(
        "ffmpeg-avcodec|$FFMPEG_INST/lib/libavcodec.a"
        "ffmpeg-avformat|$FFMPEG_INST/lib/libavformat.a"
        "ffmpeg-avfilter|$FFMPEG_INST/lib/libavfilter.a"
        "ffmpeg-avutil|$FFMPEG_INST/lib/libavutil.a"
        "ffmpeg-swscale|$FFMPEG_INST/lib/libswscale.a"
        "ffmpeg-swresample|$FFMPEG_INST/lib/libswresample.a"
        "ffmpeg-avdevice|$FFMPEG_INST/lib/libavdevice.a"
        "opus|$BUILD/build-opus/inst/lib/libopus.a"
        "libvpx|$BUILD/build-libvpx/inst/lib/libvpx.a"
        "openh264|${OPENH264_SRC:-}/libopenh264.a"
        "dav1d|$BUILD/build-dav1d/inst/lib/libdav1d.a"
        "rav1e|$BUILD/build-rav1e/inst/lib/librav1e.a"
        "libwebp|$BUILD/build-libwebp/inst/lib/libwebp.a"
        "webrtc-apm|$apm_archive"
    )
    log "per-library footprint (pre-link archive .text):"
    local fp
    for fp in "${footprints[@]}"; do
        log_footprint "${fp%%|*}" "${fp#*|}"
    done

    # Force exported symbols as link roots, then let dead-code stripping prune.
    local uflags
    case "$OS" in
        darwin) uflags=$(sed 's/^/-Wl,-u,_/' "$exports" | tr '\n' ' ') ;;
        *)      uflags=$(sed 's/^/-Wl,-u,/'  "$exports" | tr '\n' ' ') ;;
    esac

    local expfile; expfile=$(write_export_file "$exports" "$b")
    local cxx="${CXX:-c++}"
    local out

    # LLD's ICF would fold the identical abseil/webrtc template instances that BFD
    # cannot, but lld 21.x crashes on this link (reproduced with --icf=all/safe/none),
    # so BFD is the default. Set COBALT_NATIVE_LINKER=lld to opt in once lld can link
    # this tree; the build then adds ICF.
    local ld_flags=""
    if [ "${COBALT_NATIVE_LINKER:-}" = lld ] && command -v ld.lld >/dev/null 2>&1; then
        ld_flags="-fuse-ld=lld -Wl,--icf=all"
        log "final link: lld + ICF (opt-in)"
    else
        log "final link: bfd"
    fi

    case "$OS" in
        linux)
            out="$b/libcobalt-native.so"
            # shellcheck disable=SC2086
            "$cxx" -shared -fPIC $uflags $ld_flags \
                -Wl,--version-script="$expfile" \
                -Wl,--gc-sections -Wl,--no-undefined \
                -Wl,-soname,libcobalt-native.so \
                -static-libgcc -static-libstdc++ \
                -Wl,--start-group \
                    "${extra_archives[@]}" $ff_libs \
                -Wl,--end-group \
                -lpthread -lm -ldl \
                -o "$out"
            # Keep dynamic symbols so FFM can resolve exports.
            strip --strip-unneeded "$out"
            ;;
        darwin)
            out="$b/libcobalt-native.dylib"
            # shellcheck disable=SC2086
            "$cxx" -dynamiclib -fPIC $uflags \
                -Wl,-exported_symbols_list,"$expfile" \
                -Wl,-dead_strip \
                -Wl,-install_name,@rpath/libcobalt-native.dylib \
                "${extra_archives[@]}" $ff_libs \
                -framework CoreFoundation -framework CoreMedia \
                -framework CoreVideo -framework AVFoundation \
                -framework AudioToolbox -framework VideoToolbox \
                -framework CoreServices -framework Security \
                -lc++ -lm \
                -o "$out"
            # Remove local symbols while preserving the exported symbol list.
            strip -x "$out"
            ;;
        windows)
            out="$b/cobalt-native.dll"
            # Bundle dependency archives statically; use import libs for Windows APIs.
            # --exclude-all-symbols keeps exports to the .def, suppressing leaks.
            # shellcheck disable=SC2086
            "$cxx" -shared $uflags $ld_flags \
                "$expfile" \
                -Wl,--gc-sections -Wl,--no-undefined -Wl,--exclude-all-symbols \
                -static-libgcc -static-libstdc++ \
                -Wl,-Bstatic \
                -Wl,--start-group \
                    "${extra_archives[@]}" $ff_libs \
                -Wl,--end-group \
                -lstdc++ -lpthread \
                -Wl,-Bdynamic \
                -lws2_32 -liphlpapi -lntdll -lwinmm -lbcrypt -lsecur32 -lole32 -loleaut32 -lstrmiids -luuid -lgdi32 \
                -o "$out"
            # Strip non-exported symbols; the .def file preserves FFM exports.
            strip -s "$out"
            ;;
    esac

    [ -f "$out" ] || fail "combined library not produced: $out"
    log "final library sections:"
    size "$out" 2>/dev/null | sed 's/^/[build-natives] /' || true
    local dest="$NATIVES/bin/$CLASSIFIER"
    mkdir -p "$dest"
    find "$dest" -maxdepth 1 \( -type f -o -type l \) ! -name '.gitkeep' -delete 2>/dev/null || true
    cp "$out" "$dest/$(basename "$out")"
    log "wrote $dest/$(basename "$out") ($(wc -c < "$out" | tr -d ' ') bytes)"
}

build_opus
build_openh264
build_libvpx
build_av1
build_rav1e
build_libwebp
build_ffmpeg
build_webrtc_apm
build_combined
log "done $CLASSIFIER"
