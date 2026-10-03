#!/usr/bin/env python3
"""Records every sentence the narrator can say, so the phone only plays sound files (decision #45).

    scripts/render-voice.py LINES MODEL_DIR CACHE_DIR OUT_DIR

LINES is the list from `./gradlew :engine:voiceLines`: one recording per line, as tab-separated
voice id, Kokoro speaker, speed, pitch change and the sentence. The narrator and each character
have their own voice (engine Who). Each sentence is spoken by the same Kokoro voice the app uses
(sherpa-onnx), pitch-shifted for the small and the big characters, brought to the app's loudness, and saved as a small Ogg Opus file named by the fingerprint the
app looks up (engine Voice.key). CACHE_DIR keeps recordings between builds, so only new
sentences are recorded; OUT_DIR (the app's assets/voice) gets exactly the ones in LINES.
"""
import hashlib
import multiprocessing as mp
import os
import shutil
import subprocess
import sys
import tempfile
import wave

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET_RMS = 0.11  # the app's Pcm.TARGET_RMS


def key(voice_id: str, sentence: str) -> str:
    """Matches Voice.key in the engine: SHA-1 of "voiceId|sentence", first 8 bytes in hex."""
    return hashlib.sha1(f"{voice_id}|{sentence}".encode("utf-8")).hexdigest()[:16]


def pitched(samples: np.ndarray, pitch: float) -> np.ndarray:
    """Higher (or lower) and a little faster (or slower), by straight-line resampling, like the app's."""
    if abs(pitch - 1.0) < 0.001 or len(samples) == 0:
        return samples
    n = int(len(samples) / pitch)
    return np.interp(np.arange(n) * pitch, np.arange(len(samples)), samples).astype(np.float32)


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
    (sid, speed, pitch, sentence), target = job
    audio = _tts.generate(sentence, sid=sid, speed=speed)
    samples = normalized(pitched(np.asarray(audio.samples, dtype=np.float32), pitch), audio.sample_rate)
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
    sentences = []  # (name of the file, (sid, speed, pitch, sentence))
    for line in open(lines_file, encoding="utf-8").read().split("\n"):
        if not line.strip():
            continue
        voice_id, sid, speed, pitch, text = line.split("\t", 4)
        sentences.append((key(voice_id, text) + ".ogg", (int(sid), float(speed), float(pitch), text)))
    os.makedirs(cache_dir, exist_ok=True)
    todo = []
    for name, spec in sentences:
        target = os.path.join(cache_dir, name)
        if not os.path.exists(target):
            todo.append((spec, target))
    print(f"{len(sentences)} sentences, {len(todo)} to record", flush=True)
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
    for name, _ in sentences:
        shutil.copyfile(os.path.join(cache_dir, name), os.path.join(out_dir, name))
        size += os.path.getsize(os.path.join(out_dir, name))
    print(f"packed {len(sentences)} recordings, {size / 1e6:.1f} MB", flush=True)


if __name__ == "__main__":
    main()
