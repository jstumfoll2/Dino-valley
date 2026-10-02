#!/usr/bin/env python3
"""Records every sentence the narrator can say, so the phone only plays sound files (decision #45).

    scripts/render-voice.py LINES MODEL_DIR CACHE_DIR OUT_DIR

LINES is the list from `./gradlew :engine:voiceLines` (one spoken sentence per line). Each
sentence is spoken by the same Kokoro voice the app uses (sherpa-onnx, speaker 1, speed 0.9),
brought to the app's loudness, and saved as a small Ogg Opus file named by the fingerprint the
app looks up (engine Voice.key). CACHE_DIR keeps recordings between builds, so only new
sentences are recorded; OUT_DIR (the app's assets/voice) gets exactly the ones in LINES.
"""
import hashlib
import multiprocessing as mp
import os
import re
import shutil
import subprocess
import sys
import tempfile
import wave

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VOICE_KT = os.path.join(ROOT, "engine/src/main/kotlin/com/dinovalley/engine/model/Voice.kt")
VOICE_ID = re.search(r'const val VOICE_ID = "([^"]+)"', open(VOICE_KT).read()).group(1)
SPEAKER = 1
SPEED = 0.9
TARGET_RMS = 0.11  # the app's Pcm.TARGET_RMS


def key(sentence: str) -> str:
    """Matches Voice.key in the engine: SHA-1 of "VOICE_ID|sentence", first 8 bytes in hex."""
    return hashlib.sha1(f"{VOICE_ID}|{sentence}".encode("utf-8")).hexdigest()[:16]


def normalized(samples: np.ndarray, rate: int) -> np.ndarray:
    """The app's Pcm.normalized: speaking parts to TARGET_RMS, never clipping the loudest peak."""
    window = rate // 50
    n = len(samples) // window * window
    if n == 0:
        return samples
    frames = samples[:n].reshape(-1, window)
    rms = np.sqrt((frames ** 2).mean(axis=1))
    loud = frames[rms > 0.02]
    peak = float(np.abs(samples).max())
    if loud.size == 0 or peak == 0:
        return samples
    gain = min(TARGET_RMS / float(np.sqrt((loud ** 2).mean())), 0.98 / peak)
    return samples * gain


_tts = None


def _init(model_dir: str):
    global _tts
    import sherpa_onnx

    config = sherpa_onnx.OfflineTtsConfig(
        model=sherpa_onnx.OfflineTtsModelConfig(
            kokoro=sherpa_onnx.OfflineTtsKokoroModelConfig(
                model=os.path.join(model_dir, "model.int8.onnx"),
                voices=os.path.join(model_dir, "voices.bin"),
                tokens=os.path.join(model_dir, "tokens.txt"),
                data_dir=os.path.join(model_dir, "espeak-ng-data"),
            ),
            num_threads=1,
            provider="cpu",
        ),
        max_num_sentences=1,
    )
    _tts = sherpa_onnx.OfflineTts(config)


def _render(job):
    sentence, target = job
    audio = _tts.generate(sentence, sid=SPEAKER, speed=SPEED)
    samples = normalized(np.asarray(audio.samples, dtype=np.float32), audio.sample_rate)
    pcm = (np.clip(samples, -1, 1) * 32767).astype("<i2")
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav = tmp.name
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(audio.sample_rate)
        w.writeframes(pcm.tobytes())
    part = target + ".part.ogg"
    subprocess.run(
        ["ffmpeg", "-loglevel", "error", "-y", "-i", wav, "-c:a", "libopus", "-b:a", "24k", "-application", "voip", part],
        check=True,
    )
    os.replace(part, target)
    os.remove(wav)
    return len(samples) / audio.sample_rate


def main():
    lines_file, model_dir, cache_dir, out_dir = sys.argv[1:5]
    sentences = [l for l in open(lines_file, encoding="utf-8").read().split("\n") if l.strip()]
    os.makedirs(cache_dir, exist_ok=True)
    todo = []
    for s in sentences:
        target = os.path.join(cache_dir, key(s) + ".ogg")
        if not os.path.exists(target):
            todo.append((s, target))
    print(f"{len(sentences)} sentences, {len(todo)} to record (voice {VOICE_ID})", flush=True)
    if todo:
        workers = max(1, os.cpu_count() or 1)
        with mp.Pool(workers, initializer=_init, initargs=(model_dir,)) as pool:
            seconds = 0.0
            for i, d in enumerate(pool.imap_unordered(_render, todo, chunksize=4), 1):
                seconds += d
                if i % 100 == 0 or i == len(todo):
                    print(f"  recorded {i}/{len(todo)} ({seconds / 60:.1f} min of speech)", flush=True)
    shutil.rmtree(out_dir, ignore_errors=True)
    os.makedirs(out_dir)
    size = 0
    for s in sentences:
        name = key(s) + ".ogg"
        shutil.copyfile(os.path.join(cache_dir, name), os.path.join(out_dir, name))
        size += os.path.getsize(os.path.join(out_dir, name))
    print(f"packed {len(sentences)} recordings, {size / 1e6:.1f} MB", flush=True)


if __name__ == "__main__":
    main()
