#!/usr/bin/env python3
"""Regenerate the MLow 60 ms SID/DTX C-oracle golden consumed by MlowSidGoldenTest.

This script drives the reference C encoder (the raw ``smpl_Encode`` layer, i.e. Cobalt's
``MlowEncoder`` counterpart, NOT the Opus wrapper) over a fixed deterministic input and writes the
resulting packet stream to ``encode/mlow-sid-60ms.json`` next to this script. A silence-suppressed
frame is a genuine zero-length packet at this layer, matching ``MlowEncoder.encode`` returning an
empty array.

Prerequisites (all outside this repo; the golden is committed so tests do not need them):
  * The ``smpl_opus`` oracle tree checked out, with ``build-oracle/libopus.a`` already built
    (the same tree the MLow port was validated against). Point at it with --oracle or the
    MLOW_ORACLE_ROOT env var; the default is ~/Downloads/smpl_opus/smpl_opus.
  * A C toolchain. On Windows the MLow port used msys64 ucrt64 gcc; pass --gcc or set CC.

What it does:
  1. Compiles ``smpl/test/smpl_codec.c`` against the prebuilt ``libopus.a`` into
     ``build-oracle/smpl_codec.exe`` (skipped if already present unless --rebuild).
  2. Synthesizes the input: ACTIVE_PACKETS of a two-sine + integer-LCG-dither active lead-in
     followed by SILENCE_PACKETS of zeros, PACKET_SAMPLES per 60 ms packet.
  3. Runs ``smpl_codec -e 16000 9600 -packetsize 60 -complexity 9 -dtx`` to encode it.
  4. Parses the harness output ([4-byte big-endian length][TOC + payload] per packet, length 0 for a
     non-transmitted frame) and writes the compact golden JSON.

Keep the input parameters below IN SYNC with any change to the golden; the config (16 kHz / 9600 bps
/ 60 ms / complexity 9 / DTX on) must match the encoder MlowSidGoldenTest constructs.
"""

import argparse
import base64
import json
import math
import os
import shutil
import struct
import subprocess
import sys
import tempfile
from pathlib import Path

# --- Golden configuration (must match MlowSidGoldenTest and Cobalt's MLow encode scope) -----------
SAMPLE_RATE = 16000
BITRATE = 9600
PACKET_MS = 60
COMPLEXITY = 9
PACKET_SAMPLES = SAMPLE_RATE * PACKET_MS // 1000  # 960
ACTIVE_PACKETS = 15   # two-sine + dither active lead-in (establishes encoder state + candidates)
SILENCE_PACKETS = 25  # zeros: exercises hangover -> SID interval -> suppressed-frame drops

# Two-sine active fixture (mirrors the MlowBitIdentityTest tone) plus a deterministic 64-bit LCG dither.
TONE_A_HZ, TONE_A_AMP = 440.0, 8000.0
TONE_B_HZ, TONE_B_AMP = 1000.0, 6000.0
LCG_SEED = 0x9E3779B97F4A7C15
LCG_MULT = 6364136223846793005
LCG_INC = 1442695040888963407
U64 = (1 << 64) - 1

# Include dirs relative to the oracle root, and the compile flags used by its own build.
INCLUDE_DIRS = ["include/opus", "smpl", "build-oracle", ".", "celt", "silk", "smpl/pffft",
                "silk/float", "src"]


def synthesize_input():
    """Builds the deterministic int16 PCM: active two-sine+dither packets then silence packets."""
    lcg = LCG_SEED
    samples = []
    sample_index = 0
    for _ in range(ACTIVE_PACKETS):
        for _ in range(PACKET_SAMPLES):
            t = sample_index / SAMPLE_RATE
            mix = TONE_A_AMP * math.sin(2 * math.pi * TONE_A_HZ * t) \
                + TONE_B_AMP * math.sin(2 * math.pi * TONE_B_HZ * t)
            lcg = (lcg * LCG_MULT + LCG_INC) & U64
            dither = ((lcg >> 40) & 0x7F) - 64
            value = max(-32768, min(32767, int(round(mix)) + dither))
            samples.append(value)
            sample_index += 1
    samples.extend([0] * (SILENCE_PACKETS * PACKET_SAMPLES))
    return struct.pack("<%dh" % len(samples), *samples)


def build_harness(oracle_root: Path, gcc: str, rebuild: bool) -> Path:
    """Compiles smpl_codec.c against the prebuilt libopus.a; returns the harness path."""
    harness = oracle_root / "build-oracle" / "smpl_codec.exe"
    if harness.exists() and not rebuild:
        return harness
    libopus = oracle_root / "build-oracle" / "libopus.a"
    if not libopus.exists():
        sys.exit(f"libopus.a not found at {libopus}; build the oracle tree first (its build-oracle "
                 f"ninja target).")
    src = oracle_root / "smpl" / "test" / "smpl_codec.c"
    cmd = [gcc, str(src)]
    for inc in INCLUDE_DIRS:
        cmd += ["-I" + str(oracle_root / inc)]
    cmd += ["-DHAVE_CONFIG_H", "-O2", "-o", str(harness), str(libopus), "-lm"]
    subprocess.run(cmd, check=True)
    return harness


# The reference smpl_codec harness's CELP analysis-by-synthesis reads ASLR-dependent uninitialized
# scratch inside libopus, so the ACTIVE-voice packet bytes are not byte-deterministic across runs (only a
# few active packets vary, but which ones is machine-dependent). The transmit/drop schedule and the
# silence-driven SID comfort-noise packets ARE input-deterministic. The golden therefore byte-asserts only
# the SID packets and the drop pattern; every other transmitted (active/hangover) packet is stored as the
# ACTIVE_MARKER, which MlowSidGoldenTest checks as "transmitted, bytes not asserted". Active-voice
# byte-exactness is covered separately by the self-referential MlowBitIdentityTest.
ACTIVE_MARKER = "*"
DETERMINISM_RUNS = 6


def _run_once(harness: Path, pcm: bytes) -> list[str]:
    with tempfile.TemporaryDirectory() as tmp:
        in_pcm = Path(tmp) / "in.pcm"
        out_smpl = Path(tmp) / "out.smpl"
        in_pcm.write_bytes(pcm)
        subprocess.run([str(harness), "-e", str(SAMPLE_RATE), str(BITRATE),
                        "-packetsize", str(PACKET_MS), "-complexity", str(COMPLEXITY), "-dtx",
                        str(in_pcm), str(out_smpl)], check=True,
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        data = out_smpl.read_bytes()
    packets = []
    offset = 0
    while offset + 4 <= len(data):
        length = struct.unpack(">I", data[offset:offset + 4])[0]  # big-endian, int_to_char
        offset += 4
        packets.append(data[offset:offset + length].hex())
        offset += length
    return packets


def encode(harness: Path, pcm: bytes) -> list[str]:
    """Encodes the PCM several times and classifies each packet into the golden representation.

    Drops become the empty string, SID packets (TOC bit 7 set) their exact hex, and every other
    transmitted packet the {@code ACTIVE_MARKER}. Raises if the drop schedule or any SID packet is not
    identical across runs, since those are what the golden asserts.
    """
    runs = [_run_once(harness, pcm) for _ in range(DETERMINISM_RUNS)]
    count = len(runs[0])
    if any(len(r) != count for r in runs):
        sys.exit("packet count varied across runs")
    result = []
    for i in range(count):
        values = {r[i] for r in runs}
        first = runs[0][i]
        is_drop = {r[i] == "" for r in runs}
        if len(is_drop) != 1:
            sys.exit(f"drop schedule not stable at packet {i}: {values}")
        if first == "":
            result.append("")
        elif int(first[:2], 16) & 0x80:
            if len(values) != 1:
                sys.exit(f"SID packet {i} not byte-stable across runs: {values}")
            result.append(first)
        else:
            result.append(ACTIVE_MARKER)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    default_oracle = os.environ.get("MLOW_ORACLE_ROOT",
                                    str(Path.home() / "Downloads" / "smpl_opus" / "smpl_opus"))
    parser.add_argument("--oracle", default=default_oracle,
                        help="smpl_opus oracle root (with build-oracle/libopus.a)")
    parser.add_argument("--gcc", default=os.environ.get("CC", shutil.which("gcc") or "gcc"),
                        help="C compiler (msys64 ucrt64 gcc on Windows)")
    parser.add_argument("--rebuild", action="store_true", help="force recompiling the harness")
    args = parser.parse_args()

    oracle_root = Path(args.oracle)
    if not oracle_root.exists():
        sys.exit(f"oracle root not found: {oracle_root} (set --oracle or MLOW_ORACLE_ROOT)")

    harness = build_harness(oracle_root, args.gcc, args.rebuild)
    pcm = synthesize_input()
    packets = encode(harness, pcm)

    golden = {
        "description": ("MLow 60ms SID/DTX golden from the C oracle smpl_codec (raw smpl_Encode), "
                        "16kHz mono 9600bps complexity9 -dtx. Input: 15 packets two-sine+LCG-dither "
                        "active, then 25 packets zeros (silence). packets[i]: empty string = a 0-length "
                        "(non-transmitted) silence frame; '*' = a transmitted active/hangover packet whose "
                        "bytes are NOT asserted (the C harness CELP is ASLR-non-deterministic; active "
                        "byte-exactness is covered by MlowBitIdentityTest); a hex string = a SID "
                        "comfort-noise packet asserted byte-for-byte. Regenerate with "
                        "generate_mlow_sid_golden.py."),
        "sampleRate": SAMPLE_RATE,
        "bitrate": BITRATE,
        "packetMs": PACKET_MS,
        "complexity": COMPLEXITY,
        "dtx": True,
        "framesPerPacketSamples": PACKET_SAMPLES,
        "inputPcmBase64": base64.b64encode(pcm).decode(),
        "packets": packets,
    }
    out = Path(__file__).resolve().parent / "encode" / "mlow-sid-60ms.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(golden, separators=(",", ":")))

    drop = packets.count("")
    active = packets.count(ACTIVE_MARKER)
    sid = len(packets) - drop - active
    print(f"wrote {out} ({out.stat().st_size} bytes): {len(packets)} packets, "
          f"{active} active(not asserted), {sid} SID(asserted), {drop} dropped")


if __name__ == "__main__":
    main()
