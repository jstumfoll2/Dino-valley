#!/usr/bin/env python3
"""Makes app/src/main/assets/sfx/growl.ogg: a dragon's low growl, used for "[growl]" in the story.

A speech model can only spell a growl out ("g rr rr"), so it is a sound effect (decision #47):
a low rattling buzz shaped like a big open mouth. Needs numpy and ffmpeg only.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

import letter_sounds as L

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app/src/main/assets/sfx/growl.ogg")


def growl(seconds: float = 1.1) -> np.ndarray:
    rng = np.random.default_rng(3)
    n = int(seconds * L.RATE)
    t = np.arange(n) / L.RATE
    # A low note that sags, with a fast rattle (the throat) and some breath.
    f0 = 78 - 18 * t / seconds + 4 * np.sin(2 * np.pi * 6 * t)
    phase = np.cumsum(f0) / L.RATE
    src = 2 * (phase % 1.0) - 1
    rattle = 0.55 + 0.45 * np.sign(np.sin(2 * np.pi * 27 * t)) * np.abs(np.sin(2 * np.pi * 27 * t)) ** 0.5
    src = src * rattle + 0.15 * rng.normal(0, 1, n)
    mouth = L._resonator(src, 420, 160) + 0.7 * L._resonator(src, 900, 250) + 0.3 * L._resonator(src, 1700, 400)
    env = np.clip(t / 0.12, 0, 1) * np.clip((seconds - t) / 0.35, 0, 1) * (0.85 + 0.15 * np.sin(2 * np.pi * 2.5 * t))
    out = mouth * env
    return (out / np.abs(out).max() * 0.8).astype(np.float32)


if __name__ == "__main__":
    x = growl()
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        path = tmp.name
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(L.RATE)
        w.writeframes((x * 32767).astype("<i2").tobytes())
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", path, "-c:a", "libvorbis", "-q:a", "4", OUT], check=True)
    os.remove(path)
    print("wrote", OUT)
