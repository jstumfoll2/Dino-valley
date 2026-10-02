#!/usr/bin/env python3
"""Builds app/src/main/assets/sfx/*.ogg from found CC0 sounds plus synthesized ones (decision #47).

    SFX_WORK=/some/dir scripts/make-sfx.py

The found sounds come from https://github.com/series-ai/jam-ready-assets at commit
782e3a09566b4bb3d98fe2ed07f5a8545e6fcfd4 (Kenney and Ninja Adventure packs, CC0); see
docs/SOUND_CREDITS.md for each file. The repo keeps audio in Git LFS, so fetch each file from
https://media.githubusercontent.com/media/series-ai/jam-ready-assets/<sha>/<path> into
$SFX_WORK/src/dl/<path>. Needs numpy, scipy, soundfile and ffmpeg.
"""
import os, subprocess, json
import numpy as np
import soundfile as sf
from scipy.signal import butter, sosfilt

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HERE = os.environ.get('SFX_WORK', os.path.join(ROOT, 'sfx-work'))
DL = os.path.join(HERE, 'src', 'dl')
OUT = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'sfx')
TMP = os.path.join(HERE, 'wav')
os.makedirs(OUT, exist_ok=True); os.makedirs(TMP, exist_ok=True)
rng = np.random.default_rng(7)

# ---------------------------------------------------------------- helpers
def load(rel):
    p = os.path.join(DL, rel)
    b = subprocess.run(['ffmpeg', '-v', 'error', '-i', p, '-ac', '1', '-ar', str(SR), '-f', 'f32le', '-'],
                       capture_output=True, check=True).stdout
    x = np.frombuffer(b, np.float32).astype(np.float64)
    assert len(x) > 0, rel
    return x

def t_(d): return np.arange(int(d * SR)) / SR

def env_adsr(n, a=0.01, r=0.05, shape=1.0):
    e = np.ones(n)
    na = max(1, int(a * SR)); nr = max(1, int(r * SR))
    e[:na] = np.linspace(0, 1, na)
    e[-nr:] *= np.linspace(1, 0, nr) ** shape
    return e

def bp(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, hi], 'bandpass', fs=SR, output='sos'), x)
def lp(x, f, order=2):
    return sosfilt(butter(order, f, 'lowpass', fs=SR, output='sos'), x)
def hp(x, f, order=2):
    return sosfilt(butter(order, f, 'highpass', fs=SR, output='sos'), x)

def place(total, parts):
    """parts: list of (start_seconds, signal)"""
    n = int(total * SR); y = np.zeros(n)
    for s, sig in parts:
        i = int(s * SR); m = min(len(sig), n - i)
        y[i:i + m] += sig[:m]
    return y

def formant_voice(f0, formants, tilt=1.0, jitter=0.0, fmax=6000):
    """Additive voiced source with gaussian formant envelope. f0: per-sample array."""
    n = len(f0)
    if jitter:
        f0 = f0 * (1 + jitter * lp(rng.standard_normal(n), 30) * 8)
    ph = 2 * np.pi * np.cumsum(f0) / SR
    y = np.zeros(n)
    for k in range(1, 60):
        fk = k * f0
        if fk.min() > fmax: break
        g = np.zeros(n)
        for F, bw, a in formants:
            g += a * np.exp(-((fk - F) / bw) ** 2)
        g += 0.05 / k ** tilt
        g *= (fk < fmax)
        y += g * np.sin(k * ph)
    return y

# ---------------------------------------------------------------- synths
def s_wrong():
    # gentle two-note "uh-oh": soft hummed vowels, falling minor third
    def note(f_start, f_end, d, formants):
        f0 = np.linspace(f_start, f_end, int(d * SR))
        f0 *= 1 + 0.006 * np.sin(2 * np.pi * 5.5 * t_(d))
        return formant_voice(f0, formants, tilt=1.5) * env_adsr(len(f0), 0.025, 0.08, 1.5)
    uh = note(392, 400, 0.18, [(600, 250, 1.0), (1050, 300, 0.45)])
    oh = note(330, 300, 0.36, [(450, 200, 1.0), (800, 250, 0.4)])
    return lp(place(0.62, [(0.0, uh), (0.24, oh)]), 3500)

def s_boing():
    d = 0.75; t = t_(d)
    glide = 140 + 120 * (1 - np.exp(-t / 0.03))
    f0 = glide * (1 + 0.10 * np.exp(-t * 4) * np.sin(2 * np.pi * 16 * t))
    F = 900 + 600 * np.exp(-t * 3) * np.sin(2 * np.pi * 16 * t + 1.0)
    ph = 2 * np.pi * np.cumsum(f0) / SR
    y = np.zeros(len(t))
    for k in range(1, 30):
        g = np.exp(-((k * f0 - F) / 350) ** 2) + 0.25 / k
        y += g * np.sin(k * ph)
    return y * np.exp(-t * 4.5) * env_adsr(len(t), 0.004, 0.05)

def s_poof():
    d = 0.75; t = t_(d); n = len(t)
    noise = rng.standard_normal(n)
    # time-varying lowpass approximated by crossfading two filtered versions
    bright = lp(noise, 3500); dark = lp(noise, 500)
    w = np.exp(-t / 0.08)
    puff = (w * bright + (1 - w) * dark) * (1 - np.exp(-t / 0.006)) * np.exp(-t / 0.13)
    puff /= np.abs(puff).max()
    sp = np.zeros(n)
    for i in range(7):
        s = 0.05 + 0.06 * i + 0.02 * rng.random()
        f = rng.choice([2093, 2349, 2637, 3136, 3520, 4186])
        tt = t_(0.25)
        ping = np.sin(2 * np.pi * f * tt) * np.exp(-tt / 0.05) * (0.9 ** i)
        i0 = int(s * SR); m = min(len(ping), n - i0); sp[i0:i0 + m] += ping[:m]
    return 0.9 * puff + 0.28 * sp

def s_ribbit():
    def croak(d, f0s, f0e):
        t = t_(d)
        f0 = np.linspace(f0s, f0e, len(t))
        v = formant_voice(f0, [(500, 180, 1.0), (1400, 300, 0.6), (2400, 400, 0.2)], tilt=0.7)
        am = (0.5 + 0.5 * np.sin(2 * np.pi * 38 * t)) ** 2      # pulsed, frog-like
        return v * (0.35 + 0.65 * am) * env_adsr(len(t), 0.008, 0.03)
    return place(0.36, [(0.0, croak(0.11, 150, 140)), (0.16, croak(0.15, 175, 150))])

def brass(f, d, vib=True):
    t = t_(d); n = len(t)
    f0 = f * (1 - 0.05 * np.exp(-t / 0.03))           # small scoop up into the note
    if vib: f0 *= 1 + 0.01 * np.sin(2 * np.pi * 6 * t) * np.clip((t - 0.12) / 0.1, 0, 1)
    a = env_adsr(n, 0.03, 0.08, 1.5)
    ph = 2 * np.pi * np.cumsum(f0) / SR
    y = np.zeros(n)
    for k in range(1, 16):
        bright = a ** (1 + 0.35 * k)                   # brighter when louder (brassy)
        y += bright * np.sin(k * ph) / k ** 0.9
    return lp(y * a, 4500)

def s_toot():
    return place(0.62, [(0.0, brass(523.25, 0.14, False)), (0.18, brass(659.25, 0.42))])

def s_burp():
    d = 0.5; t = t_(d)
    f0 = 105 - 35 * t / d
    v = formant_voice(f0, [(550, 200, 1.0), (950, 250, 0.6), (2400, 400, 0.1)], tilt=1.2, jitter=0.02)
    flutter = 0.7 + 0.3 * lp(rng.standard_normal(len(t)), 25) * 6
    e = env_adsr(len(t), 0.02, 0.15, 2) * np.clip(flutter, 0.2, 1.2)
    return lp(v * e, 3000)

def s_raspberry():
    d = 0.65; t = t_(d); n = len(t)
    rate = 32 + 6 * np.exp(-t * 3)
    flap = np.sin(2 * np.pi * np.cumsum(rate) / SR)
    flap = np.clip(flap, 0, 1) ** 3
    src = 0.6 * formant_voice(130 * np.ones(n), [(700, 300, 1.0), (1200, 400, 0.5)], tilt=1.0) \
        + 0.6 * bp(rng.standard_normal(n), 300, 2500)
    return lp(src * flap * env_adsr(n, 0.03, 0.12, 1.5), 3000)

def s_munch():
    def chomp(seed):
        r = np.random.default_rng(seed); d = 0.12; n = int(d * SR)
        clicks = np.zeros(n)
        for _ in range(40):
            i = r.integers(0, int(0.07 * SR)); clicks[i] += r.uniform(-1, 1)
        crunch = bp(clicks + 0.2 * r.standard_normal(n), 900, 4500) * np.exp(-t_(d) / 0.035)
        crunch /= np.abs(crunch).max()
        tt = t_(d); thud = np.sin(2 * np.pi * 160 * tt) * np.exp(-tt / 0.03)
        return 0.75 * crunch + 0.5 * thud
    return place(0.62, [(0.0, chomp(1)), (0.22, chomp(2)), (0.44, chomp(3) * 0.85)])

def s_sneeze():
    def ah(d, fs, fe, amp):
        f0 = np.linspace(fs, fe, int(d * SR))
        v = formant_voice(f0, [(800, 250, 1.0), (1250, 300, 0.6), (2600, 400, 0.15)], tilt=1.2)
        breath = bp(rng.standard_normal(len(f0)), 600, 3000) * 0.15
        return (v + breath) * env_adsr(len(f0), 0.04, 0.06) * amp
    # "choo": noisy 'ch' burst into a short 'oo'
    ch_d = 0.09; ch = bp(rng.standard_normal(int(ch_d * SR)), 1800, 6500) * env_adsr(int(ch_d * SR), 0.004, 0.04)
    oo_d = 0.24; f0 = np.linspace(420, 300, int(oo_d * SR))
    oo = formant_voice(f0, [(330, 150, 1.0), (850, 250, 0.4)], tilt=1.5) * env_adsr(len(f0), 0.01, 0.15, 2)
    oo += bp(rng.standard_normal(len(f0)), 1500, 5000) * 0.25 * np.exp(-t_(oo_d) / 0.05)
    return place(1.05, [(0.0, ah(0.2, 300, 360, 0.45)), (0.3, ah(0.2, 340, 420, 0.6)),
                        (0.66, ch * 1.1), (0.72, oo)])

def s_giggle():
    parts = []
    pitches = [560, 530, 500, 470]
    for i, p in enumerate(pitches):
        d = 0.085; n = int(d * SR)
        f0 = np.linspace(p * 1.04, p * 0.96, n)
        v = formant_voice(f0, [(380, 150, 1.0), (2300, 350, 0.55), (3000, 400, 0.25)], tilt=1.3)
        v *= env_adsr(n, 0.012, 0.035)
        h = bp(rng.standard_normal(int(0.03 * SR)), 1500, 5000) * 0.12 * env_adsr(int(0.03 * SR), 0.005, 0.02)
        s = i * 0.13
        parts += [(s, h), (s + 0.022, v)]
    return place(0.6, parts)

def s_fizz():
    d = 1.3; t = t_(d); n = len(t)
    dens = 0.004 * np.exp(-t / 0.5) + 0.0006
    imp = (rng.random(n) < dens) * rng.uniform(-1, 1, n)
    crackle = hp(imp, 3000, 2)
    hiss = hp(rng.standard_normal(n), 5000) * 0.05 * np.exp(-t / 0.45)
    blips = np.zeros(n)
    for _ in range(9):
        s = rng.uniform(0.02, 0.9); bd = 0.025; bt = t_(bd)
        f = np.linspace(rng.uniform(900, 1300), rng.uniform(1800, 2600), len(bt))
        b = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * bt / bd) * 0.35 * np.exp(-s / 0.6)
        i0 = int(s * SR); blips[i0:i0 + len(b)] += b
    y = crackle * 1.2 + hiss + blips
    return y * env_adsr(n, 0.01, 0.35, 1.5)

# ---------------------------------------------------------------- found-sound recipes
K = 'kenney-'
J = lambda pack, f: f'{K}{pack}/audio/Audio/{f}'
NIN = lambda f: f'ninja-adventure/2D/top-down-rpg/Audio/Sounds/{f}'

def f_dice():
    shake = load(J('casino-audio', 'dice-shake-1.ogg'))
    shake = trim_lead(shake)[:int(0.6 * SR)] * env_adsr(int(0.6 * SR), 0.005, 0.15)
    throw = trim_lead(load(J('casino-audio', 'dice-throw-3.ogg')))
    # shake is quieter than throw in the source; balance them
    shake *= rms_active(throw) / max(rms_active(shake), 1e-9) * 0.8
    return place(0.62 + len(throw) / SR, [(0, shake), (0.62, throw)])

def f_stomp():
    a = trim_lead(load(J('impact-sounds', 'footstep_wood_000.ogg')))
    b = trim_lead(load(J('impact-sounds', 'footstep_wood_002.ogg')))
    return place(0.38 + len(b) / SR, [(0, a), (0.38, b)])

def f_tweet():
    b = trim_lead(load(NIN('Creature/Bird.wav')))
    return place(0.2 + len(b) / SR, [(0, b), (0.2, b * 0.9)])

RECIPES = {
    # name: (kind, source/callable)
    'tap':      ('found', J('interface-sounds', 'drop_002.ogg')),
    'right':    ('found', J('interface-sounds', 'confirmation_001.ogg')),
    'wrong':    ('synth', s_wrong),
    'cheer':    ('found', f'{K}music-jingles/audio/Audio (Pizzicato)/jingles-pizzicato_08.ogg'),
    'levelup':  ('found', f'{K}music-jingles/audio/Audio (Steeldrum)/jingles-steel_00.ogg'),
    'dice':     ('found', f_dice),
    'coins':    ('found', J('rpg-audio', 'handleCoins.ogg')),
    'creak':    ('found', J('rpg-audio', 'doorOpen_1.ogg')),
    'unlock':   ('found', J('rpg-audio', 'metalLatch.ogg')),
    'clunk':    ('found', J('impact-sounds', 'impactWood_heavy_002.ogg')),
    'bubble':   ('found', NIN('Elemental/Bubble.wav')),
    'splash':   ('found', f'{K}foley-sounds/audio/Audio/Water/drip2.ogg'),
    'boing':    ('synth', s_boing),
    'poof':     ('synth', s_poof),
    'zap':      ('found', NIN('Magic & Skill/Magic2.wav')),
    'whoosh':   ('found', NIN('Creature/Wings.wav')),
    'ribbit':   ('synth', s_ribbit),
    'toot':     ('synth', s_toot),
    'sneeze':   ('synth', s_sneeze),
    'burp':     ('synth', s_burp),
    'raspberry':('synth', s_raspberry),
    'munch':    ('synth', s_munch),
    'giggle':   ('synth', s_giggle),
    'stomp':    ('found', f_stomp),
    'heart':    ('found', J('interface-sounds', 'drop_004.ogg')),
    'tweet':    ('found', f_tweet),
    'rumble':   ('found', f'{K}foley-sounds/audio/Audio/Rocks/stoneDrag2.ogg'),
    'fizz':     ('synth', s_fizz),
}

# ---------------------------------------------------------------- processing
def trim_lead(x, thr_db=-45):
    x = x - np.mean(x[:min(len(x), 2048)]) * 0  # keep as-is (no DC hack)
    thr = 10 ** (thr_db / 20) * max(np.abs(x).max(), 1e-9)
    idx = np.where(np.abs(x) > thr)[0]
    if len(idx) == 0: return x
    s = max(0, idx[0] - int(0.002 * SR)); e = min(len(x), idx[-1] + int(0.01 * SR))
    return x[s:e]

def rms_active(x):
    env = np.convolve(x ** 2, np.ones(441) / 441, 'same')
    thr = env.max() * 10 ** (-30 / 10)
    a = env[env > thr]
    return np.sqrt(a.mean()) if len(a) else 1e-9

TARGET_RMS_DB = -18.0   # loudness of the active part
PEAK_DB = -3.0

def finish(x):
    x = hp(x, 40, 2)                      # remove DC / sub rumble
    x = trim_lead(x)
    fo = min(int(0.02 * SR), len(x) // 4)
    x[-fo:] *= np.linspace(1, 0, fo)
    fi = int(0.001 * SR); x[:fi] *= np.linspace(0, 1, fi)
    g = min(10 ** (TARGET_RMS_DB / 20) / rms_active(x), 10 ** (PEAK_DB / 20) / np.abs(x).max())
    x = x * g
    if 20 * np.log10(rms_active(x)) < -23:
        # very peaky clicks (dice, coins, fizz): bring body up and soft-limit transients
        x = x * (10 ** (-22 / 20) / rms_active(x))
        t = 0.45; a = np.abs(x)
        lim = np.where(a > t, t + (1 - t) * np.tanh((a - t) / (1 - t)), a)
        x = np.sign(x) * lim
        x *= 10 ** (PEAK_DB / 20) / max(np.abs(x).max(), 10 ** (PEAK_DB / 20))
    return x

report = {}
for name, (kind, src) in RECIPES.items():
    x = src() if callable(src) else load(src)
    y = finish(np.asarray(x, dtype=np.float64))
    w = os.path.join(TMP, name + '.wav'); sf.write(w, y.astype(np.float32), SR, subtype='FLOAT')
    o = os.path.join(OUT, name + '.ogg')
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', w, '-ac', '1', '-ar', str(SR), '-c:a', 'libvorbis',
                    '-q:a', '4', '-map_metadata', '-1', o], check=True)
    report[name] = dict(kind=kind, dur=round(len(y) / SR, 3),
                        peak_db=round(20 * np.log10(np.abs(y).max()), 1),
                        rms_db=round(20 * np.log10(rms_active(y)), 1))
    print(name, report[name])
