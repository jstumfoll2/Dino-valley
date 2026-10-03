"""Pure letter sounds ("mmm", "sss") made from scratch, so they are one steady sound and never
the letter's name. A speech model reads "mmmm" as "em em em", and given a lone sound it adds a
vowel ("ess"); these are built the way phonics teachers say them: a held buzz shaped by the
mouth (m, n, l, r, v, z) or hissing air (s, f). Used by render-voice.py for sentences like "[[s:]]."
"""
import numpy as np

RATE = 24000


def _resonator(x: np.ndarray, freq: float, bw: float) -> np.ndarray:
    """A two-pole band-pass: the mouth's shape lets one band of the buzz through."""
    r = np.exp(-np.pi * bw / RATE)
    a1 = 2 * r * np.cos(2 * np.pi * freq / RATE)
    a2 = -r * r
    gain = 1 - r
    y = np.zeros(len(x))
    y1 = y2 = 0.0
    for i, v in enumerate(x):
        y0 = gain * v + a1 * y1 + a2 * y2
        y[i] = y0
        y2 = y1
        y1 = y0
    return y


def _buzz(n: int, f0: float, rng: np.random.Generator) -> np.ndarray:
    """The voice box: a pulse train with a little wobble, like a held note."""
    t = np.arange(n) / RATE
    f = f0 * (1 + 0.012 * np.sin(2 * np.pi * 5 * t)) + rng.normal(0, 0.4, n).cumsum() * 0.002
    phase = np.cumsum(f) / RATE
    saw = 2 * (phase % 1.0) - 1
    return saw


def _hiss(n: int, low: float, high: float, rng: np.random.Generator) -> np.ndarray:
    """Noise limited to a band of frequencies."""
    x = rng.normal(0, 1, n)
    spectrum = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(n, 1 / RATE)
    spectrum[(freqs < low) | (freqs > high)] = 0
    return np.fft.irfft(spectrum, n)


# sound: (buzz formants as (freq, bandwidth, level), buzz level, hiss band or None, hiss level)
SOUNDS = {
    "m": ([(250, 90, 1.0), (1100, 250, 0.10)], 1.0, None, 0.0),
    "n": ([(250, 90, 1.0), (1600, 250, 0.22), (2500, 300, 0.08)], 1.0, None, 0.0),
    "l": ([(360, 110, 1.0), (1050, 150, 0.55), (2700, 250, 0.25)], 1.0, None, 0.0),
    "r": ([(420, 110, 1.0), (1250, 160, 0.6), (1650, 160, 0.55)], 1.0, None, 0.0),
    "v": ([(250, 100, 1.0), (1500, 400, 0.12)], 0.8, (1500, 7000), 0.12),
    "z": ([(250, 100, 1.0)], 0.6, (4500, 10500), 0.55),
    "s": ([], 0.0, (4800, 10500), 1.0),
    "f": ([], 0.0, (1800, 10500), 0.55),
}


def synth(letter: str, seconds: float = 0.85, seed: int = 7) -> np.ndarray:
    """One held sound as float samples at 24 kHz, with a soft start and end."""
    formants, buzz_level, band, hiss_level = SOUNDS[letter]
    n = int(seconds * RATE)
    rng = np.random.default_rng(seed)
    out = np.zeros(n)
    if formants:
        src = _buzz(n, 165.0, rng)
        voiced = sum(level * _resonator(src, f, bw) for f, bw, level in formants)
        out += buzz_level * voiced / (np.abs(voiced).max() or 1)
    if band:
        h = _hiss(n, band[0], band[1], rng)
        out += hiss_level * h / (np.abs(h).max() or 1)
    t = np.arange(n) / RATE
    attack = np.clip(t / 0.06, 0, 1)
    release = np.clip((seconds - t) / 0.12, 0, 1)
    swell = 0.9 + 0.1 * np.sin(2 * np.pi * 3 * t)
    out = out * attack * release * swell
    return (out / (np.abs(out).max() or 1) * 0.8).astype(np.float32)
