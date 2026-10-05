"""Storybook backgrounds for The Little Dungeon, drawn procedurally at 1920x1080.

Same house style as scenes.py (layered gradients, soft shapes, warm ink outlines, paper grain and
a vignette), but every scene is laid out around the app's overlay zones:

* bottom-left (x 0-700, y 450-1080): hero + baby dragon stand here, so it stays an open floor;
* top centre (x 380-1540, y 0-260): the parchment caption bar, so it stays low-detail;
* centre-right (x 700-1880, y 300-1000): challenge pictures, so it stays a calm mid-tone.

Floors read as floors around y 800-1000. Each scene uses a fixed seed so renders are stable.
"""
import math
import random

W, H = 1920, 1080
INK = "#3d2a1c"


# --------------------------------------------------------------------------------------------
# Document builder: unique ids per SVG, gradient helpers, and the finishing paper + vignette.
# --------------------------------------------------------------------------------------------

class Doc:
    def __init__(self, seed):
        self.r = random.Random(seed)
        self.defs = []
        self.parts = []
        self.n = 0
        self._blur = {}

    def uid(self, prefix):
        self.n += 1
        return f"{prefix}{self.n}"

    def add(self, *items):
        self.parts.extend(items)

    def _stops(self, stops):
        out = ""
        for s in stops:
            off, col = s[0], s[1]
            op = s[2] if len(s) > 2 else 1
            out += f'<stop offset="{off}" stop-color="{col}" stop-opacity="{op}"/>'
        return out

    def lin(self, stops, x1=0, y1=0, x2=0, y2=1, user=False):
        gid = self.uid("lg")
        units = ' gradientUnits="userSpaceOnUse"' if user else ""
        self.defs.append(f'<linearGradient id="{gid}" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}"{units}>'
                         f'{self._stops(stops)}</linearGradient>')
        return f"url(#{gid})"

    def rad(self, stops, cx=0.5, cy=0.5, r=0.5, fx=None, fy=None, user=False):
        gid = self.uid("rg")
        units = ' gradientUnits="userSpaceOnUse"' if user else ""
        f = ""
        if fx is not None:
            f = f' fx="{fx}" fy="{fy}"'
        self.defs.append(f'<radialGradient id="{gid}" cx="{cx}" cy="{cy}" r="{r}"{f}{units}>'
                         f'{self._stops(stops)}</radialGradient>')
        return f"url(#{gid})"

    def blur(self, sd):
        if sd not in self._blur:
            fid = self.uid("bl")
            self.defs.append(f'<filter id="{fid}" x="-50%" y="-50%" width="200%" height="200%">'
                             f'<feGaussianBlur stdDeviation="{sd}"/></filter>')
            self._blur[sd] = f"url(#{fid})"
        return self._blur[sd]

    def fade_mask(self, cx, cy, rx, ry, inner=0.0):
        """A mask that hides content inside an ellipse (soft edge) and keeps it outside.
        `inner` is how much shows at the very centre (0 = nothing)."""
        mid = self.uid("mk")
        g = self.rad([(0, "#fff", 1), (0.55, "#fff", 1), (1, "#fff", 0)], user=True, cx=cx, cy=cy, r=1)
        gid = g[5:-1]
        # rebuild with an ellipse-shaped user-space gradient via transform
        self.defs[-1] = (f'<radialGradient id="{gid}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" '
                         f'gradientTransform="translate({cx} {cy}) scale({rx} {ry})">'
                         f'<stop offset="0" stop-color="#000"/><stop offset="0.55" stop-color="#000"/>'
                         f'<stop offset="1" stop-color="#fff"/></radialGradient>')
        base = f'<rect width="{W}" height="{H}" fill="#fff"/>'
        self.defs.append(f'<mask id="{mid}" maskUnits="userSpaceOnUse" x="0" y="0" width="{W}" height="{H}">'
                         f'{base}<rect width="{W}" height="{H}" fill="url(#{gid})"/>'
                         f'<rect width="{W}" height="{H}" fill="#fff" opacity="{inner}"/></mask>')
        return f"url(#{mid})"

    def clip(self, d_path):
        cid = self.uid("cp")
        self.defs.append(f'<clipPath id="{cid}"><path d="{d_path}"/></clipPath>')
        return f"url(#{cid})"

    def svg(self, grain=0.08, vignette=0.25, vig_color="#1a0f08"):
        # paper grain (same recipe as scenes._paper_texture) + soft vignette
        gid = self.uid("grain")
        self.defs.append(f'<filter id="{gid}" x="0" y="0" width="100%" height="100%">'
                         f'<feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" seed="3"/>'
                         f'<feColorMatrix values="0 0 0 0 0.3  0 0 0 0 0.2  0 0 0 0 0.1  0 0 0 1.2 -0.45"/></filter>')
        vig = self.rad([(0.55, vig_color, 0), (1, vig_color, vignette)], r=0.78)
        tail = (f'<rect width="{W}" height="{H}" filter="url(#{gid})" opacity="{grain}"/>'
                f'<rect width="{W}" height="{H}" fill="{vig}"/>')
        return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">'
                f'<defs>{"".join(self.defs)}</defs>{"".join(self.parts)}{tail}</svg>')


# --------------------------------------------------------------------------------------------
# Shape helpers
# --------------------------------------------------------------------------------------------

def smooth(pts, closed=True):
    """Catmull-Rom spline through points -> cubic bezier path."""
    n = len(pts)
    if n < 3:
        return "M" + " L".join(f"{x:.0f} {y:.0f}" for x, y in pts)
    d = f"M{pts[0][0]:.1f} {pts[0][1]:.1f}"
    rng = range(n) if closed else range(n - 1)
    for i in rng:
        p0 = pts[(i - 1) % n] if closed or i > 0 else pts[0]
        p1 = pts[i]
        p2 = pts[(i + 1) % n]
        p3 = pts[(i + 2) % n] if closed or i + 2 < n else pts[-1]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        d += f" C{c1[0]:.1f} {c1[1]:.1f} {c2[0]:.1f} {c2[1]:.1f} {p2[0]:.1f} {p2[1]:.1f}"
    return d + (" Z" if closed else "")


def blob_pts(r, cx, cy, rx, ry, n=12, jit=0.12, rot=0.0):
    pts = []
    for i in range(n):
        a = rot + 2 * math.pi * i / n
        k = 1 + r.uniform(-jit, jit)
        pts.append((cx + math.cos(a) * rx * k, cy + math.sin(a) * ry * k))
    return pts


def glow(d, x, y, rx, color, op=0.6, ry=None):
    ry = ry or rx
    g = d.rad([(0, color, op), (0.4, color, op * 0.45), (1, color, 0)])
    return f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{rx:.0f}" ry="{ry:.0f}" fill="{g}"/>'


def sparkle(x, y, s, color="#fffbe6", op=0.9):
    return (f'<path d="M{x:.1f} {y - s:.1f} Q{x + s * 0.18:.1f} {y - s * 0.18:.1f} {x + s:.1f} {y:.1f} '
            f'Q{x + s * 0.18:.1f} {y + s * 0.18:.1f} {x:.1f} {y + s:.1f} Q{x - s * 0.18:.1f} {y + s * 0.18:.1f} '
            f'{x - s:.1f} {y:.1f} Q{x - s * 0.18:.1f} {y - s * 0.18:.1f} {x:.1f} {y - s:.1f} Z" fill="{color}" opacity="{op:.2f}"/>')


def flame(x, y, s, outer="#ff9a3c", inner="#ffe27a"):
    return (f'<path d="M{x} {y} C{x - 20 * s:.1f} {y - 6 * s:.1f} {x - 16 * s:.1f} {y - 40 * s:.1f} {x + 2 * s:.1f} {y - 66 * s:.1f} '
            f'C{x + 4 * s:.1f} {y - 44 * s:.1f} {x + 20 * s:.1f} {y - 34 * s:.1f} {x + 16 * s:.1f} {y - 12 * s:.1f} '
            f'C{x + 14 * s:.1f} {y - 2 * s:.1f} {x + 6 * s:.1f} {y + 2 * s:.1f} {x} {y} Z" fill="{outer}"/>'
            f'<path d="M{x} {y - 2 * s:.1f} C{x - 10 * s:.1f} {y - 6 * s:.1f} {x - 8 * s:.1f} {y - 26 * s:.1f} {x + 1 * s:.1f} {y - 40 * s:.1f} '
            f'C{x + 3 * s:.1f} {y - 26 * s:.1f} {x + 10 * s:.1f} {y - 20 * s:.1f} {x + 8 * s:.1f} {y - 8 * s:.1f} '
            f'C{x + 7 * s:.1f} {y - 3 * s:.1f} {x + 3 * s:.1f} {y - 1 * s:.1f} {x} {y - 2 * s:.1f} Z" fill="{inner}"/>')


def torch(d, x, y, s=1.0, glow_col="#ffb45a"):
    """Wall torch: bracket, wooden handle, flame and a warm glow (glow drawn first)."""
    return (glow(d, x, y - 70 * s, 260 * s, glow_col, 0.55) +
            f'<path d="M{x - 26 * s:.0f} {y + 40 * s:.0f} L{x + 26 * s:.0f} {y + 40 * s:.0f}" stroke="#4a3a34" stroke-width="{10 * s:.0f}" stroke-linecap="round"/>'
            f'<path d="M{x - 12 * s:.0f} {y - 10 * s:.0f} L{x - 7 * s:.0f} {y + 70 * s:.0f} L{x + 7 * s:.0f} {y + 70 * s:.0f} L{x + 12 * s:.0f} {y - 10 * s:.0f} Z" '
            f'fill="#8a5a33" stroke="{INK}" stroke-width="{4 * s:.0f}" stroke-linejoin="round"/>'
            f'<path d="M{x - 20 * s:.0f} {y - 14 * s:.0f} L{x + 20 * s:.0f} {y - 14 * s:.0f} L{x + 14 * s:.0f} {y + 6 * s:.0f} L{x - 14 * s:.0f} {y + 6 * s:.0f} Z" '
            f'fill="#5d4a44" stroke="{INK}" stroke-width="{4 * s:.0f}" stroke-linejoin="round"/>'
            + flame(x, y - 12 * s, 1.15 * s))


def candle(d, x, y, s=1.0, wax="#fff1d0", glow_on=True):
    out = glow(d, x, y - 40 * s, 90 * s, "#ffd27a", 0.5) if glow_on else ""
    out += (f'<rect x="{x - 9 * s:.1f}" y="{y - 36 * s:.1f}" width="{18 * s:.1f}" height="{36 * s:.1f}" rx="{4 * s:.1f}" '
            f'fill="{wax}" stroke="{INK}" stroke-width="{2.5 * s:.1f}" stroke-opacity="0.5"/>'
            f'<path d="M{x - 9 * s:.1f} {y - 30 * s:.1f} q{4 * s:.1f} {6 * s:.1f} {6 * s:.1f} 0" stroke="#f3d9a8" stroke-width="{3 * s:.1f}" fill="none"/>')
    out += flame(x, y - 38 * s, 0.42 * s)
    return out


def tree(d, r, x, base, s, leaf=("#5c8f45", "#77a957", "#9cc26a"), trunk="#6b4a32"):
    out = (f'<path d="M{x - 14 * s:.0f} {base} Q{x - 6 * s:.0f} {base - 120 * s:.0f} {x - 10 * s:.0f} {base - 200 * s:.0f} '
           f'L{x + 12 * s:.0f} {base - 200 * s:.0f} Q{x + 8 * s:.0f} {base - 120 * s:.0f} {x + 16 * s:.0f} {base} Z" fill="{trunk}"/>')
    for k, col in enumerate(leaf):
        cy = base - 260 * s + k * 18 * s
        pts = blob_pts(r, x - k * 6 * s, cy - k * 10 * s, (120 - k * 28) * s, (120 - k * 30) * s, 10, 0.16)
        out += f'<path d="{smooth(pts)}" fill="{col}"/>'
    return out


def pine(x, base, s, col="#3f6e4a", dark="#2f5a3d"):
    out = f'<rect x="{x - 8 * s:.0f}" y="{base - 40 * s:.0f}" width="{16 * s:.0f}" height="{40 * s:.0f}" fill="#5a3f2c"/>'
    for k in range(3):
        top = base - 40 * s - (k + 1) * 70 * s
        w = (100 - k * 22) * s
        out += (f'<path d="M{x - w:.0f} {top + 110 * s:.0f} Q{x:.0f} {top + 85 * s:.0f} {x + w:.0f} {top + 110 * s:.0f} '
                f'L{x:.0f} {top - 10 * s:.0f} Z" fill="{col if k % 2 == 0 else dark}"/>')
    return out


def crystal(x, y, h, w, ang, col, light, dark, stroke="#2a1e48"):
    """A single crystal shard pointing along `ang` degrees (0 = up) from base (x, y)."""
    pts = [(-w / 2, 0), (-w / 2, -h * 0.72), (0, -h), (w / 2, -h * 0.72), (w / 2, 0)]
    body = " ".join(f"{px:.1f},{py:.1f}" for px, py in pts)
    facet = f"0,{-h:.1f} {w / 2:.1f},{-h * 0.72:.1f} {w / 2:.1f},0 {w * 0.08:.1f},0 {w * 0.08:.1f},{-h * 0.72:.1f}"
    shine = f'M{-w * 0.28:.1f} {-h * 0.12:.1f} L{-w * 0.28:.1f} {-h * 0.62:.1f}'
    return (f'<g transform="translate({x:.1f} {y:.1f}) rotate({ang:.1f})">'
            f'<polygon points="{body}" fill="{col}" stroke="{stroke}" stroke-width="4" stroke-linejoin="round"/>'
            f'<polygon points="{facet}" fill="{dark}" opacity="0.55"/>'
            f'<polygon points="{-w / 2:.1f},{-h * 0.72:.1f} 0,{-h:.1f} {w * 0.08:.1f},{-h * 0.72:.1f}" fill="{light}" opacity="0.7"/>'
            f'<path d="{shine}" stroke="#ffffff" stroke-width="{max(2, w * 0.1):.1f}" stroke-linecap="round" opacity="0.6"/></g>')


CRYSTAL_COLORS = {
    "pink": ("#ff8fcf", "#ffd0ec", "#c4508f"),
    "cyan": ("#6fe3ff", "#d2f7ff", "#2f8fbf"),
    "violet": ("#a98aff", "#e0d4ff", "#6a4fc4"),
    "amber": ("#ffc65a", "#fff0c2", "#c98a22"),
    "mint": ("#7ff0b8", "#dcffe9", "#2fa878"),
}


def crystal_cluster(d, r, x, y, s, palette, base_ang=0, spread=70, n=5, glow_op=0.45):
    col, light, dark = CRYSTAL_COLORS[palette]
    out = glow(d, x, y - 60 * s * math.cos(math.radians(base_ang)), 220 * s, col, glow_op)
    a0 = math.radians(base_ang)
    px, py = math.cos(a0), math.sin(a0)
    rock = smooth(blob_pts(r, x, y, 70 * s * abs(px) + 26 * s, 70 * s * abs(py) + 26 * s, 9, 0.18))
    out += f'<path d="{rock}" fill="#3a3168" stroke="#1a1534" stroke-width="4"/>'
    shards = []
    for i in range(n):
        t = (i / (n - 1) - 0.5) if n > 1 else 0
        ang = base_ang + t * spread + r.uniform(-8, 8)
        h = (210 - abs(t) * 170 + r.uniform(-20, 20)) * s
        w = (h * 0.34) + r.uniform(-4, 4) * s
        off = t * 110 * s
        shards.append((abs(t), x + px * off, y + py * off, h, w, ang))
    shards.sort(key=lambda q: -q[0])
    for _, sx, sy, h, w, ang in shards:
        out += crystal(sx, sy, h, w, ang, col, light, dark)
    return out


def bottle(x, y, s, liquid, kind=0, glass="#e8f4ff"):
    """Potion bottle standing on y. kind 0 round flask, 1 tall, 2 squat jar."""
    if kind == 0:
        body = (f'<circle cx="{x}" cy="{y - 26 * s:.1f}" r="{26 * s:.1f}" fill="{glass}" fill-opacity="0.55" stroke="{INK}" stroke-width="{3 * s:.1f}"/>'
                f'<path d="M{x - 24 * s:.1f} {y - 22 * s:.1f} A{26 * s:.1f} {26 * s:.1f} 0 0 0 {x + 24 * s:.1f} {y - 22 * s:.1f} Z" fill="{liquid}"/>'
                f'<rect x="{x - 8 * s:.1f}" y="{y - 70 * s:.1f}" width="{16 * s:.1f}" height="{22 * s:.1f}" fill="{glass}" fill-opacity="0.6" stroke="{INK}" stroke-width="{3 * s:.1f}"/>'
                f'<rect x="{x - 10 * s:.1f}" y="{y - 80 * s:.1f}" width="{20 * s:.1f}" height="{12 * s:.1f}" rx="{3 * s:.1f}" fill="#b07a4a" stroke="{INK}" stroke-width="{2.5 * s:.1f}"/>'
                f'<ellipse cx="{x - 10 * s:.1f}" cy="{y - 36 * s:.1f}" rx="{5 * s:.1f}" ry="{9 * s:.1f}" fill="#fff" opacity="0.7"/>')
    elif kind == 1:
        body = (f'<path d="M{x - 16 * s:.1f} {y} L{x - 16 * s:.1f} {y - 60 * s:.1f} Q{x - 16 * s:.1f} {y - 72 * s:.1f} {x - 6 * s:.1f} {y - 78 * s:.1f} L{x - 6 * s:.1f} {y - 96 * s:.1f} '
                f'L{x + 6 * s:.1f} {y - 96 * s:.1f} L{x + 6 * s:.1f} {y - 78 * s:.1f} Q{x + 16 * s:.1f} {y - 72 * s:.1f} {x + 16 * s:.1f} {y - 60 * s:.1f} L{x + 16 * s:.1f} {y} Z" '
                f'fill="{glass}" fill-opacity="0.55" stroke="{INK}" stroke-width="{3 * s:.1f}" stroke-linejoin="round"/>'
                f'<rect x="{x - 14 * s:.1f}" y="{y - 44 * s:.1f}" width="{28 * s:.1f}" height="{42 * s:.1f}" fill="{liquid}"/>'
                f'<rect x="{x - 8 * s:.1f}" y="{y - 106 * s:.1f}" width="{16 * s:.1f}" height="{12 * s:.1f}" rx="{3 * s:.1f}" fill="#b07a4a" stroke="{INK}" stroke-width="{2.5 * s:.1f}"/>'
                f'<rect x="{x - 10 * s:.1f}" y="{y - 58 * s:.1f}" width="{5 * s:.1f}" height="{40 * s:.1f}" rx="{2 * s:.1f}" fill="#fff" opacity="0.6"/>')
    else:
        body = (f'<rect x="{x - 24 * s:.1f}" y="{y - 44 * s:.1f}" width="{48 * s:.1f}" height="{44 * s:.1f}" rx="{10 * s:.1f}" fill="{glass}" fill-opacity="0.55" stroke="{INK}" stroke-width="{3 * s:.1f}"/>'
                f'<rect x="{x - 21 * s:.1f}" y="{y - 26 * s:.1f}" width="{42 * s:.1f}" height="{23 * s:.1f}" rx="{7 * s:.1f}" fill="{liquid}"/>'
                f'<rect x="{x - 26 * s:.1f}" y="{y - 54 * s:.1f}" width="{52 * s:.1f}" height="{12 * s:.1f}" rx="{4 * s:.1f}" fill="#c98f5a" stroke="{INK}" stroke-width="{2.5 * s:.1f}"/>'
                f'<rect x="{x - 16 * s:.1f}" y="{y - 38 * s:.1f}" width="{5 * s:.1f}" height="{24 * s:.1f}" rx="{2 * s:.1f}" fill="#fff" opacity="0.6"/>')
    return body


def shelf_board(x0, x1, y, col="#7a4e2e", dark="#4f321e", depth=16):
    return (f'<rect x="{x0}" y="{y}" width="{x1 - x0}" height="{depth}" rx="3" fill="{col}" stroke="{INK}" stroke-width="3" stroke-opacity="0.6"/>'
            f'<rect x="{x0}" y="{y + depth}" width="{x1 - x0}" height="8" fill="{dark}" opacity="0.5"/>')


def books_row(r, x0, x1, y, hmin=60, hmax=110, palette=None):
    palette = palette or ["#c4513f", "#3f78b5", "#e0a83a", "#4f9a5a", "#8a5ab0", "#d97a4a", "#2f8f8f", "#b5476d"]
    out = ""
    x = x0
    while x < x1 - 14:
        w = r.uniform(16, 30)
        if x + w > x1:
            break
        h = r.uniform(hmin, hmax)
        col = r.choice(palette)
        lean = 0
        if r.random() < 0.08 and x + w + 30 < x1:
            lean = r.choice([-1, 1]) * 12
        tr = f' transform="rotate({lean} {x + w / 2:.0f} {y})"' if lean else ""
        out += (f'<g{tr}><rect x="{x:.1f}" y="{y - h:.1f}" width="{w:.1f}" height="{h:.1f}" rx="2" fill="{col}" stroke="{INK}" stroke-width="2.5" stroke-opacity="0.7"/>'
                f'<rect x="{x + 3:.1f}" y="{y - h + 10:.1f}" width="{w - 6:.1f}" height="5" fill="#f6dfa0" opacity="0.7"/>'
                f'<rect x="{x + 3:.1f}" y="{y - 18:.1f}" width="{w - 6:.1f}" height="4" fill="#f6dfa0" opacity="0.5"/></g>')
        x += w + (r.uniform(8, 18) if lean else r.uniform(0, 3))
    return out


def flying_book(x, y, s, col, ang=0, flap=1.0):
    """An open book flapping like a bird: two covers as wings with pages between."""
    a = 34 * flap
    return (f'<g transform="translate({x} {y}) rotate({ang}) scale({s})">'
            f'<path d="M0 0 L-70 {-a} L-74 {-a + 44} L0 40 Z" fill="{col}" stroke="{INK}" stroke-width="4" stroke-linejoin="round"/>'
            f'<path d="M0 0 L70 {-a} L74 {-a + 44} L0 40 Z" fill="{col}" stroke="{INK}" stroke-width="4" stroke-linejoin="round"/>'
            f'<path d="M0 4 Q-30 {-a * 0.4 - 10} -62 {-a + 4} L-64 {-a + 38} Q-30 {-a * 0.4 + 26} 0 36 Z" fill="#fff8e6" stroke="{INK}" stroke-width="2.5"/>'
            f'<path d="M0 4 Q30 {-a * 0.4 - 10} 62 {-a + 4} L64 {-a + 38} Q30 {-a * 0.4 + 26} 0 36 Z" fill="#fff2d6" stroke="{INK}" stroke-width="2.5"/>'
            f'<path d="M-14 {14} L-50 {-a * 0.7 + 10} M-14 22 L-50 {-a * 0.7 + 20} M14 14 L50 {-a * 0.7 + 10} M14 22 L50 {-a * 0.7 + 20}" stroke="#c9b48a" stroke-width="2"/>'
            f'<path d="M-30 46 q-10 10 -24 8 M30 46 q10 10 24 8" stroke="#fff6dc" stroke-width="3" fill="none" opacity="0.6" stroke-linecap="round"/>'
            f'</g>')


def barrel(x, y, w, h, wood="#a8693c", dark="#7a4626", band="#5c5450"):
    """Barrel standing with its bottom centre at (x, y)."""
    l, rr, t = x - w / 2, x + w / 2, y - h
    bulge = w * 0.08
    body = (f'M{l + bulge:.1f} {t:.1f} Q{l - bulge:.1f} {t + h / 2:.1f} {l + bulge:.1f} {y:.1f} L{rr - bulge:.1f} {y:.1f} '
            f'Q{rr + bulge:.1f} {t + h / 2:.1f} {rr - bulge:.1f} {t:.1f} Z')
    staves = "".join(
        f'<path d="M{x + k * w * 0.2:.1f} {t + 4:.1f} Q{x + k * w * 0.24:.1f} {t + h / 2:.1f} {x + k * w * 0.2:.1f} {y - 4:.1f}" stroke="{dark}" stroke-width="3" fill="none" opacity="0.6"/>'
        for k in (-2, -1, 0, 1, 2))
    bands = "".join(
        f'<path d="M{l + bulge * (1 - 2 * abs(0.5 - f)) * -1 + bulge:.1f} {t + h * f:.1f} L{rr - bulge + bulge * (1 - 2 * abs(0.5 - f)):.1f} {t + h * f:.1f}" '
        f'stroke="{band}" stroke-width="{h * 0.06:.1f}" stroke-linecap="round"/>' for f in (0.16, 0.84))
    return (f'<path d="{body}" fill="{wood}" stroke="{INK}" stroke-width="5" stroke-linejoin="round"/>{staves}{bands}'
            f'<path d="M{l + bulge + 10:.1f} {t + 16:.1f} Q{l - bulge + 18:.1f} {t + h / 2:.1f} {l + bulge + 10:.1f} {y - 16:.1f}" stroke="#e7b07a" stroke-width="6" fill="none" opacity="0.55" stroke-linecap="round"/>'
            f'<ellipse cx="{x:.1f}" cy="{t:.1f}" rx="{w / 2 - bulge:.1f}" ry="{w * 0.1:.1f}" fill="{dark}" stroke="{INK}" stroke-width="4"/>')


def lantern(d, x, y_top, y, s=1.0, light="#ffd77a", frame="#4a3a30"):
    out = f'<path d="M{x} {y_top} L{x} {y - 50 * s:.0f}" stroke="#3d2f28" stroke-width="{4 * s:.1f}"/>'
    out += glow(d, x, y, 200 * s, light, 0.55)
    out += (f'<path d="M{x - 26 * s:.0f} {y - 30 * s:.0f} L{x + 26 * s:.0f} {y - 30 * s:.0f} L{x + 20 * s:.0f} {y + 34 * s:.0f} L{x - 20 * s:.0f} {y + 34 * s:.0f} Z" '
            f'fill="{light}" stroke="{frame}" stroke-width="{5 * s:.1f}" stroke-linejoin="round"/>'
            f'<path d="M{x} {y - 30 * s:.0f} L{x} {y + 34 * s:.0f}" stroke="{frame}" stroke-width="{3 * s:.1f}"/>'
            f'<path d="M{x - 34 * s:.0f} {y - 30 * s:.0f} L{x} {y - 56 * s:.0f} L{x + 34 * s:.0f} {y - 30 * s:.0f} Z" fill="{frame}" stroke="{INK}" stroke-width="{3 * s:.1f}" stroke-linejoin="round"/>'
            f'<rect x="{x - 24 * s:.0f}" y="{y + 32 * s:.0f}" width="{48 * s:.0f}" height="{10 * s:.0f}" rx="{3 * s:.0f}" fill="{frame}"/>'
            f'<circle cx="{x}" cy="{y - 58 * s:.0f}" r="{7 * s:.0f}" fill="none" stroke="{frame}" stroke-width="{3 * s:.1f}"/>'
            f'<ellipse cx="{x}" cy="{y + 6 * s:.0f}" rx="{9 * s:.0f}" ry="{14 * s:.0f}" fill="#fff6d0"/>')
    return out


def mushroom(d, x, y, s, cap="#7ff0e0", stem="#d8f0ee", glow_op=0.35):
    return (glow(d, x, y - 20 * s, 70 * s, cap, glow_op) +
            f'<path d="M{x - 6 * s:.1f} {y} Q{x - 4 * s:.1f} {y - 18 * s:.1f} {x - 5 * s:.1f} {y - 26 * s:.1f} L{x + 5 * s:.1f} {y - 26 * s:.1f} Q{x + 4 * s:.1f} {y - 18 * s:.1f} {x + 6 * s:.1f} {y} Z" fill="{stem}"/>'
            f'<path d="M{x - 22 * s:.1f} {y - 24 * s:.1f} Q{x - 20 * s:.1f} {y - 48 * s:.1f} {x} {y - 48 * s:.1f} Q{x + 20 * s:.1f} {y - 48 * s:.1f} {x + 22 * s:.1f} {y - 24 * s:.1f} Z" fill="{cap}"/>'
            f'<circle cx="{x - 7 * s:.1f}" cy="{y - 36 * s:.1f}" r="{3 * s:.1f}" fill="#ffffff" opacity="0.7"/>'
            f'<circle cx="{x + 8 * s:.1f}" cy="{y - 32 * s:.1f}" r="{2.2 * s:.1f}" fill="#ffffff" opacity="0.6"/>')


def coin_pile(d, r, cx, base, rx, ry, n=160, gems=8, light_side=-1):
    """A mound of gold coins. cx/base: centre of the mound's base, rx/ry: half-width/height."""
    body = d.lin([(0, "#ffe27a"), (0.5, "#f2b636"), (1, "#c98a22")])
    pts = [(cx - rx, base)]
    for i in range(1, 12):
        t = i / 12
        x = cx - rx + 2 * rx * t
        y = base - ry * math.sin(math.pi * t) ** 0.8 + r.uniform(-ry * 0.05, ry * 0.05)
        pts.append((x, y))
    pts.append((cx + rx, base))
    path = smooth(pts, closed=False) + f" Q{cx} {base + ry * 0.22:.0f} {cx - rx} {base} Z"
    out = f'<path d="{path}" fill="{body}" stroke="#9a6416" stroke-width="5"/>'
    coins = []
    for _ in range(n):
        t = r.uniform(0.04, 0.96)
        x = cx - rx + 2 * rx * t
        top = base - ry * math.sin(math.pi * t) ** 0.8
        y = r.uniform(top + 8, base + ry * 0.12 * math.sin(math.pi * t))
        c = r.uniform(12, 20)
        coins.append((y, x, c))
    coins.sort()
    for y, x, c in coins:
        tilt = r.uniform(0.3, 0.6)
        out += (f'<ellipse cx="{x:.1f}" cy="{y:.1f}" rx="{c:.1f}" ry="{c * tilt:.1f}" fill="#ffd34d" stroke="#b07a1a" stroke-width="2.5"/>'
                f'<ellipse cx="{x - c * 0.15:.1f}" cy="{y - c * tilt * 0.2:.1f}" rx="{c * 0.6:.1f}" ry="{c * tilt * 0.5:.1f}" fill="#fff0a6" opacity="0.7"/>')
    gem_cols = [("#ff5a7a", "#ffc2cf"), ("#4fb0ff", "#c8e8ff"), ("#5fd68a", "#d0ffe0"), ("#b07aff", "#e6d4ff")]
    for _ in range(gems):
        t = r.uniform(0.15, 0.85)
        x = cx - rx + 2 * rx * t
        top = base - ry * math.sin(math.pi * t) ** 0.8
        y = r.uniform(top + 14, top + ry * 0.4 + 14)
        g, gl = r.choice(gem_cols)
        sz = r.uniform(14, 22)
        out += gem(x, y, sz, g, gl)
    # rim light on the lit side
    out += f'<path d="{smooth(pts, closed=False)}" stroke="#fff6c4" stroke-width="5" fill="none" opacity="0.55"/>'
    for _ in range(int(n / 25)):
        t = r.uniform(0.1, 0.9)
        x = cx - rx + 2 * rx * t
        top = base - ry * math.sin(math.pi * t) ** 0.8
        out += sparkle(x, top + r.uniform(0, 40), r.uniform(8, 16), "#fffbe6", 0.9)
    return out


def gem(x, y, s, col, light):
    return (f'<path d="M{x - s:.1f} {y - s * 0.3:.1f} L{x - s * 0.5:.1f} {y - s * 0.8:.1f} L{x + s * 0.5:.1f} {y - s * 0.8:.1f} '
            f'L{x + s:.1f} {y - s * 0.3:.1f} L{x:.1f} {y + s * 0.8:.1f} Z" fill="{col}" stroke="{INK}" stroke-width="3" stroke-linejoin="round"/>'
            f'<path d="M{x - s * 0.5:.1f} {y - s * 0.8:.1f} L{x - s * 0.2:.1f} {y - s * 0.3:.1f} L{x - s:.1f} {y - s * 0.3:.1f} Z" fill="{light}" opacity="0.8"/>'
            f'<path d="M{x - s * 0.2:.1f} {y - s * 0.3:.1f} L{x:.1f} {y + s * 0.8:.1f}" stroke="{light}" stroke-width="2" opacity="0.6"/>')


def stone_blocks(r, x0, y0, x1, y1, bw, bh, base, light, dark, op=1.0):
    """Soft rounded masonry blocks; colours jitter between light and dark."""
    out = []
    row = 0
    y = y0
    cols = [base, light, dark]
    while y < y1:
        x = x0 - (bw / 2 if row % 2 else 0) - r.uniform(0, bw * 0.3)
        while x < x1:
            w = bw * r.uniform(0.8, 1.2)
            out.append(f'<rect x="{x + 3:.0f}" y="{y + 3:.0f}" width="{w - 6:.0f}" height="{bh - 6:.0f}" rx="{bh * 0.22:.0f}" '
                       f'fill="{r.choice(cols)}" opacity="{op * r.uniform(0.55, 1):.2f}"/>')
            x += w
        y += bh
        row += 1
    return "".join(out)


def grass_blades(r, x0, x1, y0, y1, n, colors, hmin=14, hmax=36):
    out = []
    for _ in range(n):
        x = r.uniform(x0, x1)
        y = r.uniform(y0, y1)
        h = r.uniform(hmin, hmax) * (0.6 + 0.8 * (y - y0) / (y1 - y0 + 1))
        lean = r.uniform(-10, 10)
        out.append(f'<path d="M{x:.0f} {y:.0f} q{lean / 2:.0f} {-h / 2:.0f} {lean:.0f} {-h:.0f} q{-lean / 3:.0f} {h / 2:.0f} 6 {h:.0f} Z" fill="{r.choice(colors)}"/>')
    return "".join(out)


def motes(r, n, x0, x1, y0, y1, color, rmin=2, rmax=5, avoid=None):
    out = []
    for _ in range(n):
        x, y = r.uniform(x0, x1), r.uniform(y0, y1)
        if avoid and avoid[0] < x < avoid[2] and avoid[1] < y < avoid[3]:
            continue
        rr = r.uniform(rmin, rmax)
        out.append(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{rr * 3:.1f}" fill="{color}" opacity="0.15"/>'
                   f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{rr:.1f}" fill="{color}" opacity="{r.uniform(0.5, 0.9):.2f}"/>')
    return "".join(out)


def stalactites(r, x0, x1, y_base, hmin, hmax, fill, stroke, step=(50, 110)):
    out = []
    x = x0
    while x < x1:
        w = r.uniform(*step)
        h = r.uniform(hmin, hmax)
        tip = x + w * r.uniform(0.4, 0.6)
        out.append(f'<path d="M{x:.0f} {y_base - 30:.0f} L{x:.0f} {y_base:.0f} C{x + w * 0.2:.0f} {y_base + h * 0.35:.0f} {tip - 3:.0f} {y_base + h * 0.8:.0f} {tip:.0f} {y_base + h:.0f} '
                   f'C{tip + 3:.0f} {y_base + h * 0.8:.0f} {x + w * 0.8:.0f} {y_base + h * 0.35:.0f} {x + w:.0f} {y_base:.0f} L{x + w:.0f} {y_base - 30:.0f} Z" fill="{fill}"/>'
                   f'<path d="M{x:.0f} {y_base:.0f} C{x + w * 0.2:.0f} {y_base + h * 0.35:.0f} {tip - 3:.0f} {y_base + h * 0.8:.0f} {tip:.0f} {y_base + h:.0f} '
                   f'C{tip + 3:.0f} {y_base + h * 0.8:.0f} {x + w * 0.8:.0f} {y_base + h * 0.35:.0f} {x + w:.0f} {y_base:.0f}" '
                   f'fill="none" stroke="{stroke}" stroke-width="4" stroke-linejoin="round" stroke-linecap="round"/>'
                   f'<path d="M{x + w * 0.22:.0f} {y_base + 6:.0f} C{x + w * 0.28:.0f} {y_base + h * 0.45:.0f} {tip - 10:.0f} {y_base + h * 0.75:.0f} {tip - 4:.0f} {y_base + h * 0.85:.0f}" '
                   f'stroke="#ffffff" stroke-width="{max(2, w * 0.06):.0f}" fill="none" opacity="0.12" stroke-linecap="round"/>')
        x += w + r.uniform(-w * 0.25, w * 0.6)
    return "".join(out)


def banner(x, top, w, h, col, dark, emblem="star"):
    l, rr = x - w / 2, x + w / 2
    body = (f'M{l:.0f} {top} L{rr:.0f} {top} L{rr:.0f} {top + h:.0f} L{x:.0f} {top + h - w * 0.35:.0f} L{l:.0f} {top + h:.0f} Z')
    em = ""
    cy = top + h * 0.42
    if emblem == "star":
        pts = []
        for i in range(10):
            a = -math.pi / 2 + i * math.pi / 5
            rad = w * 0.26 if i % 2 == 0 else w * 0.11
            pts.append(f"{x + math.cos(a) * rad:.1f},{cy + math.sin(a) * rad:.1f}")
        em = f'<polygon points="{" ".join(pts)}" fill="#ffd96a" stroke="{INK}" stroke-width="3" stroke-linejoin="round"/>'
    elif emblem == "moon":
        em = (f'<circle cx="{x:.0f}" cy="{cy:.0f}" r="{w * 0.24:.0f}" fill="#ffe9a8"/>'
              f'<circle cx="{x + w * 0.1:.0f}" cy="{cy - w * 0.06:.0f}" r="{w * 0.2:.0f}" fill="{col}"/>')
    elif emblem == "sun":
        em = (f'<circle cx="{x:.0f}" cy="{cy:.0f}" r="{w * 0.15:.0f}" fill="#ffd96a"/>' +
              "".join(f'<path d="M{x + math.cos(a) * w * 0.2:.1f} {cy + math.sin(a) * w * 0.2:.1f} L{x + math.cos(a) * w * 0.3:.1f} {cy + math.sin(a) * w * 0.3:.1f}" '
                      f'stroke="#ffd96a" stroke-width="5" stroke-linecap="round"/>' for a in [i * math.pi / 4 for i in range(8)]))
    return (f'<rect x="{l - 14:.0f}" y="{top - 10}" width="{w + 28:.0f}" height="12" rx="6" fill="#8a6a3a" stroke="{INK}" stroke-width="3"/>'
            f'<path d="{body}" fill="{col}" stroke="{INK}" stroke-width="4" stroke-linejoin="round"/>'
            f'<path d="M{rr - 12:.0f} {top + 4} L{rr - 12:.0f} {top + h - 10:.0f}" stroke="{dark}" stroke-width="10" opacity="0.5"/>'
            f'<path d="M{l + 8:.0f} {top + 4} L{l + 8:.0f} {top + h - 6:.0f}" stroke="#ffffff" stroke-width="5" opacity="0.18"/>'
            f'<path d="M{l + 10:.0f} {top + 22} L{rr - 10:.0f} {top + 22}" stroke="#ffd96a" stroke-width="5" opacity="0.8"/>'
            f'{em}')


def arch_path(x0, x1, y_spring, y_bottom):
    rx = (x1 - x0) / 2
    return f'M{x0:.0f} {y_bottom:.0f} L{x0:.0f} {y_spring:.0f} A{rx:.0f} {rx:.0f} 0 0 1 {x1:.0f} {y_spring:.0f} L{x1:.0f} {y_bottom:.0f} Z'


def pillar(d, x, w, top, bottom, base_col="#8a8fa8", light="#b4b9cf", dark="#5d627a", stroke=INK):
    g = d.lin([(0, light), (0.35, base_col), (1, dark)], 0, 0, 1, 0)
    return (f'<rect x="{x - w / 2:.0f}" y="{top:.0f}" width="{w:.0f}" height="{bottom - top:.0f}" fill="{g}" stroke="{stroke}" stroke-width="5" stroke-opacity="0.5"/>'
            f'<rect x="{x - w / 2 - 18:.0f}" y="{top:.0f}" width="{w + 36:.0f}" height="34" rx="6" fill="{g}" stroke="{stroke}" stroke-width="5" stroke-opacity="0.5"/>'
            f'<rect x="{x - w / 2 - 22:.0f}" y="{bottom - 40:.0f}" width="{w + 44:.0f}" height="40" rx="6" fill="{g}" stroke="{stroke}" stroke-width="5" stroke-opacity="0.5"/>'
            + "".join(f'<path d="M{x - w / 2 + w * f:.0f} {top + 40:.0f} L{x - w / 2 + w * f:.0f} {bottom - 46:.0f}" stroke="{dark}" stroke-width="4" opacity="0.35"/>' for f in (0.3, 0.55, 0.8)))


def floor_planks(r, y0, y1, col_a, col_b, line, n_rows=7):
    """Perspective-ish wooden floor: rows get taller towards the viewer."""
    out = []
    ys = [y0]
    for i in range(n_rows):
        ys.append(ys[-1] + (y1 - y0) * (0.06 + 0.035 * i) / (0.06 * n_rows + 0.035 * n_rows * (n_rows - 1) / 2))
    for i in range(n_rows):
        ya, yb = ys[i], ys[i + 1]
        out.append(f'<rect x="0" y="{ya:.1f}" width="{W}" height="{yb - ya + 1:.1f}" fill="{col_a if i % 2 else col_b}" opacity="0.5"/>')
        out.append(f'<path d="M0 {ya:.1f} L{W} {ya:.1f}" stroke="{line}" stroke-width="{2 + i * 0.6:.1f}" opacity="0.45"/>')
        x = r.uniform(-200, 0)
        while x < W:
            x += r.uniform(260, 520)
            out.append(f'<path d="M{x:.0f} {ya + 2:.1f} L{x + (x - 960) * 0.04:.0f} {yb - 1:.1f}" stroke="{line}" stroke-width="{2 + i * 0.5:.1f}" opacity="0.35"/>')
    return "".join(out)


def rug(d, cx, cy, rx, ry, main="#b5473f", border="#e8b44a", inner="#7a2e3a"):
    g = d.rad([(0, main), (1, inner)])
    return (f'<ellipse cx="{cx}" cy="{cy + 8}" rx="{rx + 6}" ry="{ry + 6}" fill="#000" opacity="0.18"/>'
            f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="{g}" stroke="{INK}" stroke-width="5" stroke-opacity="0.6"/>'
            f'<ellipse cx="{cx}" cy="{cy}" rx="{rx - 26}" ry="{ry - 12}" fill="none" stroke="{border}" stroke-width="10" opacity="0.8"/>'
            f'<ellipse cx="{cx}" cy="{cy}" rx="{rx - 52}" ry="{ry - 24}" fill="none" stroke="{border}" stroke-width="3" stroke-dasharray="14 12" opacity="0.6"/>'
            f'<ellipse cx="{cx}" cy="{cy}" rx="{rx * 0.42:.0f}" ry="{ry * 0.4:.0f}" fill="none" stroke="{border}" stroke-width="5" opacity="0.5"/>')


def vine(r, x, y, length, sway=1, leaf="#5fae45", stem="#3f7a35"):
    pts = []
    for i in range(7):
        t = i / 6
        pts.append((x + sway * 22 * math.sin(t * 5 + r.uniform(-0.4, 0.4)), y + length * t))
    out = f'<path d="{smooth(pts, closed=False)}" stroke="{stem}" stroke-width="5" fill="none" stroke-linecap="round"/>'
    for i in range(1, 12):
        t = i / 12
        k = t * 6
        a = int(k)
        f = k - a
        px = pts[a][0] + (pts[min(a + 1, 6)][0] - pts[a][0]) * f
        py = pts[a][1] + (pts[min(a + 1, 6)][1] - pts[a][1]) * f
        side = 1 if i % 2 else -1
        ang = side * r.uniform(30, 60)
        out += (f'<ellipse cx="{px + side * 11:.0f}" cy="{py:.0f}" rx="13" ry="7" fill="{leaf}" stroke="{stem}" stroke-width="2" '
                f'transform="rotate({ang:.0f} {px + side * 11:.0f} {py:.0f})"/>')
    return out


# --------------------------------------------------------------------------------------------
# Scenes
# --------------------------------------------------------------------------------------------

def scene_camp():
    d = Doc(101)
    r = d.r
    sky = d.lin([(0, "#9c8fd0"), (0.38, "#f2b6a0"), (0.68, "#ffd796"), (1, "#ffe9b6")])
    d.add(f'<rect width="{W}" height="{H}" fill="{sky}"/>')
    # low golden sun, left of the caption, half-sunk behind the far ridge
    d.add(glow(d, 250, 470, 520, "#fff1b8", 0.75))
    d.add('<circle cx="250" cy="470" r="78" fill="#fff0a8" stroke="#ffd36a" stroke-width="6"/>')
    # soft streaky clouds
    for cx, cy, s in [(140, 150, 1.0), (1730, 120, 1.2), (980, 300, 0.8), (1540, 330, 0.7)]:
        for k in range(4):
            d.add(f'<ellipse cx="{cx + k * 60 * s - 90 * s:.0f}" cy="{cy - (k % 2) * 16 * s:.0f}" rx="{(90 - k * 6) * s:.0f}" ry="{22 * s:.0f}" fill="#ffe2cf" opacity="0.7"/>')
        d.add(f'<ellipse cx="{cx:.0f}" cy="{cy + 14 * s:.0f}" rx="{200 * s:.0f}" ry="{10 * s:.0f}" fill="#f3a98f" opacity="0.4"/>')
    # far ridge
    ridge = [(0, 600), (200, 560), (420, 590), (700, 540), (980, 575), (1240, 540), (1450, 560), (1920, 590)]
    d.add(f'<path d="{smooth(ridge, closed=False)} L1920 1080 L0 1080 Z" fill="#c5a2c4"/>')
    # the mountain with a cave mouth (right)
    mg = d.lin([(0, "#b49acb"), (1, "#9d84b5")])
    d.add(f'<path d="M1430 640 Q1560 420 1700 330 Q1730 312 1760 334 Q1880 430 1990 600 L1990 700 L1430 700 Z" fill="{mg}" stroke="{INK}" stroke-width="4" stroke-opacity="0.25"/>')
    d.add('<path d="M1660 362 Q1700 322 1760 334 Q1790 360 1812 392 Q1780 380 1760 400 Q1735 375 1712 398 Q1690 375 1660 362 Z" fill="#f6eef8"/>')
    d.add('<path d="M1700 330 Q1640 500 1580 640 L1700 640 Q1690 480 1730 320 Z" fill="#8a72a6" opacity="0.5"/>')
    d.add(glow(d, 1760, 590, 90, "#ffc777", 0.5))
    d.add('<path d="M1712 640 L1712 600 Q1760 538 1808 600 L1808 640 Z" fill="#5a456e" stroke="#4a3a5e" stroke-width="4"/>')
    d.add('<path d="M1726 640 L1726 606 Q1760 562 1794 606 L1794 640 Z" fill="#3f3052"/>')
    # castle hill
    hg = d.lin([(0, "#c3c47e"), (1, "#a0ab62")])
    d.add(f'<path d="M180 680 Q380 520 560 510 Q760 520 940 680 Z" fill="{hg}"/>')
    cx, cy = 560, 520
    wall, roof, dark = "#efd9b4", "#d06f5c", "#c4a984"
    d.add(f'<rect x="{cx - 70}" y="{cy - 50}" width="140" height="50" fill="{wall}" stroke="{INK}" stroke-width="3" stroke-opacity="0.5"/>')
    for k in range(7):
        d.add(f'<rect x="{cx - 70 + k * 20}" y="{cy - 60}" width="12" height="12" fill="{wall}" stroke="{INK}" stroke-width="2" stroke-opacity="0.4"/>')
    for tx, tw, th, flag in [(cx - 78, 34, 110, "#e2574c"), (cx + 78, 34, 100, "#4fa3d9"), (cx, 42, 150, "#ffcf4a")]:
        d.add(f'<rect x="{tx - tw / 2}" y="{cy - th}" width="{tw}" height="{th}" fill="{wall}" stroke="{INK}" stroke-width="3" stroke-opacity="0.5"/>'
              f'<rect x="{tx - tw / 2}" y="{cy - th}" width="{tw * 0.35:.0f}" height="{th}" fill="{dark}" opacity="0.5"/>'
              f'<path d="M{tx - tw / 2 - 6} {cy - th} L{tx} {cy - th - tw * 1.2:.0f} L{tx + tw / 2 + 6} {cy - th} Z" fill="{roof}" stroke="{INK}" stroke-width="3" stroke-opacity="0.5" stroke-linejoin="round"/>'
              f'<path d="M{tx} {cy - th - tw * 1.2:.0f} L{tx} {cy - th - tw * 1.2 - 30:.0f}" stroke="{INK}" stroke-width="3" stroke-opacity="0.6"/>'
              f'<path d="M{tx} {cy - th - tw * 1.2 - 30:.0f} q14 -2 24 6 q-12 4 -24 6 Z" fill="{flag}"/>'
              f'<path d="M{tx - 5} {cy - th + 26} a5 7 0 0 1 10 0 L{tx + 5} {cy - th + 40} L{tx - 5} {cy - th + 40} Z" fill="#6b4f6a"/>')
    d.add(f'<path d="M{cx - 14} {cy} L{cx - 14} {cy - 24} a14 14 0 0 1 28 0 L{cx + 14} {cy} Z" fill="#7a5a50"/>')
    # rolling mid hills
    h1 = d.lin([(0, "#b8c870"), (1, "#8fae55")])
    d.add(f'<path d="M-50 720 Q300 610 760 660 Q1200 700 1500 640 Q1750 600 1990 650 L1990 1080 L-50 1080 Z" fill="{h1}"/>')
    # distant tree lines at the far edges
    for x, s in [(1490, 0.55), (1560, 0.65), (1630, 0.5), (1880, 0.8), (1820, 0.6)]:
        d.add(pine(x, 680 - (0 if x < 1700 else 10), s, "#6f8f4f", "#5d7d45"))
    # grass floor
    gg = d.lin([(0, "#a9cf6c"), (0.5, "#88b955"), (1, "#5e9440")])
    d.add(f'<path d="M0 730 Q480 690 960 720 Q1440 750 1920 710 L1920 1080 L0 1080 Z" fill="{gg}"/>')
    # golden light wash across the floor
    d.add(glow(d, 300, 760, 900, "#ffd27a", 0.35, ry=300))
    # the tent (far left, back)
    tx, tb = 210, 740
    d.add(f'<ellipse cx="{tx + 10}" cy="{tb + 4}" rx="150" ry="16" fill="#3d5a2a" opacity="0.3"/>')
    stripes = ""
    for k in range(6):
        x0 = tx - 130 + k * 260 / 6
        x1 = x0 + 260 / 6
        col = "#e05a4a" if k % 2 == 0 else "#fbe9cf"
        # stripe = slice of the triangle between x0 and x1
        def ty(x):
            return tb - 170 * (1 - abs(x - tx) / 130)
        stripes += f'<path d="M{x0:.1f} {tb} L{x0:.1f} {ty(x0):.1f} L{min(max(tx, x0), x1):.1f} {ty(min(max(tx, x0), x1)):.1f} L{x1:.1f} {ty(x1):.1f} L{x1:.1f} {tb} Z" fill="{col}"/>'
    d.add(stripes)
    d.add(f'<path d="M{tx - 130} {tb} L{tx} {tb - 170} L{tx + 130} {tb} Z" fill="none" stroke="{INK}" stroke-width="5" stroke-linejoin="round"/>')
    d.add(f'<path d="M{tx} {tb - 170} L{tx + 130} {tb} L{tx + 40} {tb} Z" fill="#7a3a2e" opacity="0.22"/>')
    d.add(f'<path d="M{tx - 32} {tb} Q{tx - 6} {tb - 70} {tx} {tb - 120} Q{tx + 6} {tb - 70} {tx + 32} {tb} Z" fill="#4a2e26" stroke="{INK}" stroke-width="4"/>')
    d.add(f'<path d="M{tx} {tb - 120} Q{tx + 10} {tb - 60} {tx + 44} {tb} L{tx + 22} {tb} Q{tx + 6} {tb - 50} {tx} {tb - 120} Z" fill="#fbe9cf" stroke="{INK}" stroke-width="3"/>')
    d.add(f'<path d="M{tx} {tb - 170} L{tx} {tb - 215}" stroke="{INK}" stroke-width="5"/><path d="M{tx} {tb - 215} q24 -2 38 10 q-18 6 -38 8 Z" fill="#ffcf4a" stroke="{INK}" stroke-width="2"/>')
    d.add(f'<path d="M{tx - 130} {tb} L{tx - 170} {tb + 12} M{tx + 130} {tb} L{tx + 168} {tb + 10}" stroke="#8a6a4a" stroke-width="3"/>')
    # campfire: small, sitting at the back of the hero area
    fx, fy = 470, 770
    d.add(glow(d, fx, fy - 20, 280, "#ffb050", 0.55, ry=200))
    for k in range(8):
        a = math.pi * k / 7
        d.add(f'<ellipse cx="{fx + math.cos(a) * 52:.0f}" cy="{fy + 6 - math.sin(a) * -10:.0f}" rx="14" ry="10" fill="#9a8a7e" stroke="{INK}" stroke-width="3" stroke-opacity="0.6"/>')
    d.add(f'<path d="M{fx - 40} {fy + 4} L{fx + 34} {fy - 14}" stroke="#7a4a2a" stroke-width="14" stroke-linecap="round"/>'
          f'<path d="M{fx + 40} {fy + 4} L{fx - 34} {fy - 14}" stroke="#8a5a32" stroke-width="14" stroke-linecap="round"/>')
    d.add(flame(fx, fy - 4, 1.2))
    for k in range(7):
        d.add(f'<circle cx="{fx + r.uniform(-30, 30):.0f}" cy="{fy - 90 - k * 22:.0f}" r="{r.uniform(2, 4):.1f}" fill="#ffd27a" opacity="{0.9 - k * 0.1:.2f}"/>')
    # grass and flowers, sparse and soft
    d.add(grass_blades(r, 0, W, 740, 1080, 520, ["#7fb04c", "#6ea443", "#98c75e"]))
    for _ in range(36):
        x, y = r.uniform(0, W), r.uniform(800, 1070)
        if 720 < x < 1880 and y < 1000:
            continue
        col = r.choice(["#ffffff", "#ffd36a", "#ff9fb8", "#c7a6ff"])
        d.add(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="7" fill="{col}" stroke="{INK}" stroke-width="1.5" stroke-opacity="0.4"/><circle cx="{x:.0f}" cy="{y:.0f}" r="2.5" fill="#e09a2a"/>')
    # framing trees at the edges
    d.add(tree(d, r, -20, 1000, 1.8, ("#4f7d3e", "#66974b", "#86b25c")))
    d.add(tree(d, r, 1960, 980, 1.7, ("#4f7d3e", "#66974b", "#86b25c")))
    for x, y, s in [(1880, 1060, 1.0), (60, 1070, 0.8)]:
        pts = blob_pts(r, x, y, 140 * s, 70 * s, 9, 0.15)
        d.add(f'<path d="{smooth(pts)}" fill="#5f8f45"/>')
    # fireflies
    d.add(motes(r, 26, 0, W, 300, 900, "#fff2a0", 2, 4, avoid=(700, 300, 1880, 1000)))
    return d.svg(vignette=0.22)


def scene_gate():
    d = Doc(102)
    r = d.r
    sky = d.lin([(0, "#8fcaf0"), (0.6, "#cfe8f0"), (1, "#fde6b8")])
    d.add(f'<rect width="{W}" height="{H}" fill="{sky}"/>')
    for x, y, s in [(170, 120, 1.0), (1720, 90, 1.1)]:
        for dx, dy, rr in [(-60, 10, 46), (-15, -18, 60), (40, -6, 52), (85, 14, 38), (10, 22, 50)]:
            d.add(f'<circle cx="{x + dx * s:.0f}" cy="{y + dy * s + 10:.0f}" r="{rr * s:.0f}" fill="#e2eef6"/>')
        for dx, dy, rr in [(-60, 10, 46), (-15, -18, 60), (40, -6, 52), (85, 14, 38), (10, 22, 50)]:
            d.add(f'<circle cx="{x + dx * s:.0f}" cy="{y + dy * s:.0f}" r="{rr * s:.0f}" fill="#ffffff"/>')
    # far hills on the left
    d.add('<path d="M-20 560 Q200 440 420 500 Q560 540 700 520 L700 1080 L-20 1080 Z" fill="#a9cfa0"/>')
    # the mossy hillside the dungeon is dug into
    hill = d.lin([(0, "#8fbf62"), (0.5, "#6f9f4c"), (1, "#557f3c")])
    crest = [(-60, 820), (200, 760), (420, 640), (620, 470), (900, 290), (1200, 220), (1500, 230), (1760, 300), (1990, 360)]
    d.add(f'<path d="{smooth(crest, closed=False)} L1990 1080 L-60 1080 Z" fill="{hill}" stroke="{INK}" stroke-width="4" stroke-opacity="0.25"/>')
    d.add(f'<path d="{smooth([(560, 520), (900, 300), (1200, 232), (1500, 240)], closed=False)}" stroke="#c4e48a" stroke-width="10" fill="none" opacity="0.55" stroke-linecap="round"/>')
    # little bushes along the crest
    for x in (760, 1640, 1840):
        y = 380 if x == 760 else (270 if x == 1640 else 330)
        d.add(f'<path d="{smooth(blob_pts(r, x, y, 60, 34, 9, 0.18))}" fill="#5f9442"/>')
    # rocky face set into the hill around the gate (soft, mid-tone)
    rock = d.lin([(0, "#9a9384"), (1, "#7a7266")])
    face = blob_pts(r, 1300, 640, 470, 400, 16, 0.06)
    face = [(x, min(y, 900)) for x, y in face]
    d.add(f'<path d="{smooth(face)}" fill="{rock}" opacity="0.9"/>')
    d.add(f'<g opacity="0.55" clip-path="{d.clip(smooth(face))}">{stone_blocks(r, 820, 220, 1800, 900, 120, 70, "#8d8676", "#a39c8b", "#7a7366")}</g>')
    # archway
    acx, spring, outer, inner, bottom = 1300, 600, 330, 245, 900
    d.add(f'<path d="{arch_path(acx - inner, acx + inner, spring, bottom)}" fill="#3a2e2a"/>')
    # wooden double gate
    wood = d.lin([(0, "#a8713f"), (1, "#7a4a28")])
    gx0, gx1 = acx - inner + 14, acx + inner - 14
    d.add(f'<path d="{arch_path(gx0, gx1, spring + 6, bottom)}" fill="{wood}" stroke="{INK}" stroke-width="5"/>')
    rx = (gx1 - gx0) / 2
    planks = ""
    for k in range(1, 10):
        x = gx0 + k * (gx1 - gx0) / 10
        dy = math.sqrt(max(0, rx ** 2 - (x - acx) ** 2))
        planks += f'<path d="M{x:.0f} {spring + 6 - dy + 6:.0f} L{x:.0f} {bottom - 4}" stroke="#6a3f22" stroke-width="4" opacity="0.65"/>'
    d.add(planks)
    d.add(f'<path d="M{acx} {spring + 6 - rx + 2:.0f} L{acx} {bottom}" stroke="{INK}" stroke-width="7"/>')
    for yb in (spring + 20, 820):
        dy = 0
        d.add(f'<rect x="{gx0 + 6}" y="{yb}" width="{gx1 - gx0 - 12}" height="26" rx="5" fill="#5d5653" stroke="{INK}" stroke-width="4"/>')
        for k in range(8):
            d.add(f'<circle cx="{gx0 + 30 + k * (gx1 - gx0 - 60) / 7:.0f}" cy="{yb + 13}" r="5" fill="#8a837e"/>')
    for sx in (-1, 1):
        d.add(f'<circle cx="{acx + sx * 40}" cy="730" r="22" fill="none" stroke="#5d5653" stroke-width="8"/>'
              f'<circle cx="{acx + sx * 40}" cy="706" r="9" fill="#5d5653"/>')
    d.add(f'<path d="{arch_path(gx0 + 18, gx0 + 50, spring + 40, bottom - 40)}" fill="#ffffff" opacity="0.08"/>')
    # stone ring (voussoirs)
    n = 13
    for k in range(n):
        a0 = math.pi + k * math.pi / n
        a1 = a0 + math.pi / n
        p = [(acx + math.cos(a0) * inner, spring + math.sin(a0) * inner), (acx + math.cos(a0) * outer, spring + math.sin(a0) * outer),
             (acx + math.cos(a1) * outer, spring + math.sin(a1) * outer), (acx + math.cos(a1) * inner, spring + math.sin(a1) * inner)]
        col = r.choice(["#b7ae9c", "#a9a08e", "#c2b9a6"])
        if k == n // 2:
            col = "#cfc4ac"
        d.add(f'<path d="M{p[0][0]:.1f} {p[0][1]:.1f} L{p[1][0]:.1f} {p[1][1]:.1f} A{outer} {outer} 0 0 1 {p[2][0]:.1f} {p[2][1]:.1f} '
              f'L{p[3][0]:.1f} {p[3][1]:.1f} A{inner} {inner} 0 0 0 {p[0][0]:.1f} {p[0][1]:.1f} Z" fill="{col}" stroke="{INK}" stroke-width="4" stroke-opacity="0.7" stroke-linejoin="round"/>')
    # keystone star
    ks = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        rad = 26 if i % 2 == 0 else 11
        ks.append(f"{acx + math.cos(a) * rad:.1f},{spring - (inner + outer) / 2 + 4 + math.sin(a) * rad:.1f}")
    d.add(f'<polygon points="{" ".join(ks)}" fill="#e9d48a" stroke="{INK}" stroke-width="3" opacity="0.9"/>')
    # side legs of the arch
    for sx in (-1, 1):
        x0 = acx + sx * inner if sx > 0 else acx - outer
        for k in range(4):
            col = r.choice(["#b7ae9c", "#a9a08e", "#c2b9a6"])
            d.add(f'<rect x="{x0:.0f}" y="{spring + k * 75:.0f}" width="{outer - inner}" height="75" rx="6" fill="{col}" stroke="{INK}" stroke-width="4" stroke-opacity="0.7"/>')
    d.add(f'<path d="M{acx - outer} {spring - 4} A{outer} {outer} 0 0 1 {acx - outer * 0.2:.0f} {spring - outer + 8}" stroke="#fff6dc" stroke-width="6" fill="none" opacity="0.45"/>')
    # moss caps on the arch
    for k in range(9):
        a = math.pi + (k + 0.5) * math.pi / 9
        x, y = acx + math.cos(a) * outer, spring + math.sin(a) * outer
        d.add(f'<path d="{smooth(blob_pts(r, x, y + 4, 34, 16, 8, 0.25))}" fill="{r.choice(["#7fb04c", "#6ea443"])}"/>')
    # vines hanging from the arch
    for x, y, L in [(acx - 250, 470, 210), (acx - 180, 360, 170), (acx + 200, 380, 200), (acx + 290, 520, 150)]:
        d.add(vine(r, x, y, L, 1 if x < acx else -1))
    # torches either side
    d.add(torch(d, acx - outer - 90, 620, 1.1))
    d.add(torch(d, acx + outer + 90, 620, 1.1))
    # ground
    gg = d.lin([(0, "#8fbf5c"), (1, "#5e8f40")])
    d.add(f'<path d="M0 860 Q500 820 1000 880 Q1500 920 1920 880 L1920 1080 L0 1080 Z" fill="{gg}"/>')
    d.add(f'<path d="M{acx - inner - 30} 890 L{acx + inner + 30} 890 L{acx + inner + 60} 910 L{acx - inner - 60} 910 Z" fill="#a39c8b" stroke="{INK}" stroke-width="4" stroke-opacity="0.5"/>')
    # flagstone path from the gate towards the bottom left
    stones = []
    for i in range(9):
        t = i / 8
        px = acx - 60 + (300 - (acx - 60)) * t
        py = 935 + 125 * t
        sw = 90 + 80 * t
        for j in (-1, 0, 1):
            if r.random() < 0.15:
                continue
            x = px + j * sw * 0.95 + r.uniform(-10, 10)
            pts = blob_pts(r, x, py, sw * 0.42, 20 + 18 * t, 7, 0.15)
            stones.append((py, f'<path d="{smooth(pts)}" fill="{r.choice(["#c4bba8", "#b5ac98", "#cfc6b2"])}" stroke="{INK}" stroke-width="3" stroke-opacity="0.45"/>'))
    for _, s in sorted(stones):
        d.add(s)
    d.add(grass_blades(r, 0, W, 870, 1080, 380, ["#6ea443", "#7fb04c", "#5e9440"]))
    # mossy rocks + ferns at the far left edge
    for x, y, s in [(40, 900, 1.0), (1880, 980, 0.9)]:
        d.add(f'<path d="{smooth(blob_pts(r, x, y, 90 * s, 60 * s, 9, 0.12))}" fill="#9a9384" stroke="{INK}" stroke-width="4" stroke-opacity="0.5"/>'
              f'<path d="{smooth(blob_pts(r, x - 10, y - 40 * s, 70 * s, 22 * s, 8, 0.2))}" fill="#7fb04c"/>')
    for _ in range(20):
        x, y = r.uniform(0, W), r.uniform(880, 1070)
        if 700 < x < 1880 and y < 1000:
            continue
        d.add(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="6" fill="{r.choice(["#ffffff", "#ffd36a", "#ff9fb8"])}"/>')
    return d.svg(vignette=0.22)


def scene_rune_hall():
    d = Doc(103)
    r = d.r
    wall = d.lin([(0, "#3b4468"), (0.3, "#5a6690"), (0.75, "#6f7aa0"), (1, "#5d6890")])
    d.add(f'<rect width="{W}" height="{H}" fill="{wall}"/>')
    # faint masonry, faded out over the centre-right wall where the rune door goes
    mask = d.fade_mask(1290, 600, 760, 520, inner=0.0)
    d.add(f'<g mask="{mask}">{stone_blocks(r, 0, 120, W, 820, 150, 80, "#66729a", "#7581a8", "#56628a", 0.6)}</g>')
    # soft blue magic light on that plain wall
    d.add(glow(d, 1290, 620, 700, "#9fdcff", 0.35, ry=520))
    # vaulted ceiling
    ceil = d.lin([(0, "#1e2440"), (1, "#2e3658", 0)])
    d.add(f'<rect width="{W}" height="320" fill="{ceil}"/>')
    for x in (-200, 410, 1590, 2120):
        d.add(f'<path d="M{x - 300} 0 Q{x} 260 {x + 300} 0" stroke="#2a3152" stroke-width="40" fill="none" opacity="0.5"/>')
    # arched recesses at the far left and far right
    for ax0, ax1 in [(70, 330), (1660, 1920)]:
        rec = d.lin([(0, "#2a3254"), (1, "#3f4a72")])
        d.add(f'<path d="{arch_path(ax0, ax1, 300, 820)}" fill="{rec}" stroke="{INK}" stroke-width="6" stroke-opacity="0.5"/>')
        d.add(f'<path d="{arch_path(ax0 + 22, ax1 - 22, 312, 820)}" fill="none" stroke="#8792ba" stroke-width="6" opacity="0.5"/>')
    d.add(banner(200, 230, 140, 420, "#3f6fb5", "#2a4a85", "moon"))
    d.add(banner(1790, 230, 140, 420, "#b5473f", "#7a2e2a", "star"))
    # tall pillars
    d.add(pillar(d, 400, 90, 60, 840, "#7a84a8", "#a9b2d0", "#4f587c"))
    d.add(pillar(d, 1590, 90, 60, 840, "#7a84a8", "#a9b2d0", "#4f587c"))
    # blue crystal sconces on the pillars
    for x in (400, 1590):
        d.add(glow(d, x, 470, 220, "#7fd8ff", 0.5))
        d.add(f'<path d="M{x - 30} 500 L{x + 30} 500 L{x + 18} 520 L{x - 18} 520 Z" fill="#4a5070" stroke="{INK}" stroke-width="3"/>')
        d.add(crystal(x, 500, 70, 24, 0, "#8fe6ff", "#e0faff", "#3f9fd0"))
    # floor
    fl = d.lin([(0, "#59628a"), (0.25, "#4b547a"), (1, "#353c5e")])
    d.add(f'<rect x="0" y="820" width="{W}" height="260" fill="{fl}"/>')
    d.add(f'<rect x="0" y="812" width="{W}" height="16" fill="#7c86ac"/>')
    for k in range(1, 5):
        y = 820 + 260 * (k / 5) ** 1.4
        d.add(f'<path d="M0 {y:.0f} L{W} {y:.0f}" stroke="#2e3454" stroke-width="3" opacity="0.4"/>')
    for k in range(-8, 9):
        x0 = 960 + k * 150
        d.add(f'<path d="M{x0} 828 L{960 + k * 420} 1080" stroke="#2e3454" stroke-width="3" opacity="0.35"/>')
    d.add(glow(d, 1290, 830, 600, "#9fdcff", 0.2, ry=90))
    # floating magic motes
    d.add(motes(r, 40, 0, W, 280, 1000, "#c8f0ff", 2, 4.5, avoid=(820, 330, 1760, 900)))
    for _ in range(10):
        x, y = r.uniform(820, 1760), r.uniform(330, 900)
        d.add(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="2.5" fill="#d8f6ff" opacity="0.5"/>')
    return d.svg(vignette=0.32, vig_color="#0d1022")


def scene_bridge():
    d = Doc(104)
    r = d.r
    bg = d.lin([(0, "#2e2a4e"), (0.45, "#4b5482"), (0.8, "#6f8aa8"), (1, "#8fb0c0")])
    d.add(f'<rect width="{W}" height="{H}" fill="{bg}"/>')
    # receding chasm walls (layers lighten with distance / mist)
    layers = [("#4a4f7c", 0), ("#3e4370", 1), ("#33375f", 2)]
    for col, k in layers:
        lx = 520 + k * -110
        rx = 1440 + k * 120
        left = [(-50, 120), (lx - 120, 260 + k * 30), (lx, 460), (lx - 60, 700), (lx + 40, 900), (lx - 30, 1130)]
        right = [(1970, 120), (rx + 140, 270 + k * 20), (rx, 470), (rx + 70, 690), (rx - 30, 900), (rx + 40, 1130)]
        d.add(f'<path d="{smooth(left, closed=False)} L-50 1130 Z" fill="{col}" opacity="0.9"/>')
        d.add(f'<path d="{smooth(right, closed=False)} L1970 1130 Z" fill="{col}" opacity="0.9"/>')
    # misty glow rising from far below
    d.add(glow(d, 1080, 1080, 900, "#c8f0ec", 0.55, ry=360))
    for _ in range(9):
        x, y = r.uniform(400, 1700), r.uniform(880, 1060)
        d.add(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{r.uniform(160, 300):.0f}" ry="{r.uniform(20, 40):.0f}" fill="#e8fbf6" opacity="0.18"/>')
    # cave ceiling
    ceil = d.lin([(0, "#1d1a33"), (1, "#2c2848")])
    cpts = [(-40, 200), (200, 170), (420, 120), (700, 110), (960, 95), (1240, 110), (1520, 120), (1720, 170), (1960, 210)]
    d.add(f'<path d="M-40 -10 L1960 -10 L1960 210 {smooth(cpts[::-1], closed=False).replace("M", "L", 1)} Z" fill="{ceil}"/>')
    d.add(stalactites(r, -20, 380, 180, 30, 120, "#2c2848", "#1a1730"))
    d.add(stalactites(r, 1540, 1940, 170, 30, 120, "#2c2848", "#1a1730"))
    d.add(stalactites(r, 380, 1540, 105, 30, 70, "#2c2848", "#1a1730", (40, 80)))
    # hanging moss strands
    for _ in range(26):
        x = r.uniform(0, W)
        if 380 < x < 1540:
            L = r.uniform(30, 90)
            top = 105
        else:
            L = r.uniform(60, 180)
            top = 175
        d.add(f'<path d="M{x:.0f} {top} q{r.uniform(-14, 14):.0f} {L / 2:.0f} {r.uniform(-6, 6):.0f} {L:.0f}" stroke="{r.choice(["#5f9a5a", "#4f8a50", "#7fb06a"])}" stroke-width="{r.uniform(5, 9):.0f}" fill="none" stroke-linecap="round" opacity="0.9"/>')
    # lanterns hanging at the edges
    d.add(lantern(d, 170, 180, 330, 1.0))
    d.add(lantern(d, 1700, 170, 290, 0.9))
    d.add(lantern(d, 1840, 180, 240, 0.7))
    # near cliff (bottom-left), where the hero stands
    near = d.lin([(0, "#8a7a6a"), (0.15, "#6e6070"), (1, "#3f3a52")])
    npts = f'M-20 790 Q300 772 540 792 Q600 800 612 850 Q630 960 650 1100 L-20 1100 Z'
    d.add(f'<path d="{npts}" fill="{near}" stroke="{INK}" stroke-width="5" stroke-opacity="0.6"/>')
    top = d.lin([(0, "#9c8f7a"), (1, "#7d7262")])
    d.add(f'<path d="M-20 790 Q300 772 540 792 Q596 800 606 830 Q300 820 -20 840 Z" fill="{top}"/>')
    d.add(f'<path d="M-20 790 Q300 772 540 792 Q596 800 606 830" stroke="#d8cdb0" stroke-width="6" fill="none" opacity="0.6"/>')
    d.add(f'<path d="M-20 845 Q300 830 600 840 L620 1100 L-20 1100 Z" fill="#7a6c5e" opacity="0.6"/>')
    d.add(grass_blades(r, 0, 560, 784, 800, 60, ["#6ea443", "#7fb04c"], 10, 22))
    # far cliff (right)
    far = d.lin([(0, "#8a7a6a"), (0.2, "#6a6074"), (1, "#3f3a52")])
    d.add(f'<path d="M1950 770 Q1760 762 1640 776 Q1590 784 1582 840 Q1566 960 1540 1100 L1950 1100 Z" fill="{far}" stroke="{INK}" stroke-width="5" stroke-opacity="0.6"/>')
    d.add(f'<path d="M1950 770 Q1760 762 1640 776 Q1594 784 1586 812 Q1760 800 1950 820 Z" fill="{top}"/>')
    d.add(f'<path d="M1950 770 Q1760 762 1640 776 Q1594 784 1586 812" stroke="#d8cdb0" stroke-width="6" fill="none" opacity="0.6"/>')
    # bridge ropes and posts (no planks: the app draws stepping stones)
    rope = "#8a6238"
    def post(x, y, h):
        return (f'<rect x="{x - 9}" y="{y - h}" width="18" height="{h + 16}" rx="6" fill="#8a5a33" stroke="{INK}" stroke-width="4"/>'
                f'<rect x="{x - 12}" y="{y - h - 8}" width="24" height="14" rx="5" fill="#6f4528" stroke="{INK}" stroke-width="3"/>')
    # back pair (slightly higher, thinner) then front pair
    for off, wdt, op in [(-26, 5, 0.6), (0, 8, 1.0)]:
        x0, x1 = 568 + (20 if off else 0), 1622 - (20 if off else 0)
        low0, low1 = 796 + off, 788 + off
        hi0, hi1 = 700 + off, 694 + off
        d.add(f'<path d="M{x0} {low0} Q1095 {846 + off} {x1} {low1}" stroke="{rope}" stroke-width="{wdt}" fill="none" opacity="{op}"/>')
        d.add(f'<path d="M{x0} {hi0} Q1095 {760 + off} {x1} {hi1}" stroke="{rope}" stroke-width="{wdt}" fill="none" opacity="{op}"/>')
        # vertical suspender cords
        for k in range(1, 14):
            t = k / 14
            xl = (1 - t) ** 2 * x0 + 2 * (1 - t) * t * 1095 + t * t * x1
            yl = (1 - t) ** 2 * low0 + 2 * (1 - t) * t * (846 + off) + t * t * low1
            yh = (1 - t) ** 2 * hi0 + 2 * (1 - t) * t * (760 + off) + t * t * hi1
            d.add(f'<path d="M{xl:.0f} {yh:.0f} L{xl:.0f} {yl:.0f}" stroke="{rope}" stroke-width="{wdt * 0.45:.1f}" opacity="{op * 0.8:.2f}"/>')
        if off:
            d.add(post(x0, low0 + 4, 110))
            d.add(post(x1, low1 + 4, 110))
    d.add(post(568, 804, 120))
    d.add(post(1622, 796, 120))
    d.add(f'<path d="M568 704 Q1095 764 1622 698" stroke="#d9b07a" stroke-width="2.5" fill="none" opacity="0.6"/>')
    # fireflies in the chasm
    d.add(motes(r, 30, 0, W, 300, 1050, "#d8fff0", 2, 4, avoid=(700, 300, 1880, 760)))
    return d.svg(vignette=0.32, vig_color="#0e0c1e")


def scene_crystal_cave():
    d = Doc(105)
    r = d.r
    bg = d.rad([(0, "#7a6bb5"), (0.55, "#54498e"), (1, "#2c2656")], cx=0.62, cy=0.55, r=0.7)
    d.add(f'<rect width="{W}" height="{H}" fill="{bg}"/>')
    # soft rock lumps on the back wall, faded in the centre-right
    mask = d.fade_mask(1280, 620, 720, 460, 0.15)
    lumps = ""
    for _ in range(30):
        x, y = r.uniform(0, W), r.uniform(100, 820)
        lumps += f'<path d="{smooth(blob_pts(r, x, y, r.uniform(80, 200), r.uniform(50, 120), 9, 0.2))}" fill="{r.choice(["#5d5296", "#4a4182", "#6a5fa4"])}" opacity="0.6"/>'
    d.add(f'<g mask="{mask}">{lumps}</g>')
    # cave mouth frame: dark rock border around the edges
    frame = d.lin([(0, "#2a234e"), (1, "#3a3168")])
    d.add(f'<path d="M0 0 L{W} 0 L{W} {H} L0 {H} Z M60 860 Q20 500 120 260 Q300 80 700 60 Q1200 40 1600 90 Q1860 180 1880 480 Q1900 700 1860 880 Z" '
          f'fill="{frame}" fill-rule="evenodd"/>')
    d.add(stalactites(r, 0, 420, 50, 60, 200, "#2a234e", "#1a1534"))
    d.add(stalactites(r, 1500, 1920, 70, 60, 200, "#2a234e", "#1a1534"))
    d.add(stalactites(r, 420, 1500, 40, 30, 70, "#2a234e", "#1a1534", (40, 80)))
    # floor
    fl = d.lin([(0, "#6b5ea8"), (0.3, "#54488e"), (1, "#352c66")])
    d.add(f'<path d="M-20 820 Q500 790 960 810 Q1400 830 1940 800 L1940 1100 L-20 1100 Z" fill="{fl}"/>')
    d.add(f'<path d="M-20 820 Q500 790 960 810 Q1400 830 1940 800" stroke="#9a8ed6" stroke-width="6" fill="none" opacity="0.5"/>')
    for _ in range(20):
        x, y = r.uniform(0, W), r.uniform(860, 1060)
        d.add(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{r.uniform(30, 80):.0f}" ry="{r.uniform(8, 16):.0f}" fill="#4a3f82" opacity="0.5"/>')
    # crystal clusters around the edges
    d.add(crystal_cluster(d, r, 90, 40, 1.3, "cyan", 160, 70, 5))
    d.add(crystal_cluster(d, r, 300, 40, 0.9, "pink", 190, 50, 4))
    d.add(crystal_cluster(d, r, 10, 380, 1.1, "violet", 90, 50, 4))
    d.add(crystal_cluster(d, r, 1780, 50, 1.2, "pink", 175, 70, 5))
    d.add(crystal_cluster(d, r, 1600, 40, 0.8, "amber", 200, 40, 3))
    d.add(crystal_cluster(d, r, 1920, 420, 1.2, "cyan", -95, 60, 4))
    d.add(crystal_cluster(d, r, 1910, 760, 1.0, "mint", -70, 60, 4))
    d.add(crystal_cluster(d, r, 1850, 1080, 1.3, "violet", -15, 80, 5))
    d.add(crystal_cluster(d, r, 30, 1080, 0.8, "amber", 20, 60, 4))
    d.add(crystal_cluster(d, r, 30, 680, 0.75, "pink", 70, 40, 3))
    # sparkles, mostly near the crystals
    for _ in range(70):
        x, y = r.uniform(0, W), r.uniform(0, H)
        if 650 < x < 1860 and 280 < y < 1000:
            if r.random() < 0.85:
                continue
            d.add(sparkle(x, y, r.uniform(4, 8), "#ffffff", 0.5))
            continue
        d.add(sparkle(x, y, r.uniform(6, 16), r.choice(["#ffffff", "#d8f6ff", "#ffe0f4"]), r.uniform(0.6, 0.95)))
    return d.svg(vignette=0.3, vig_color="#120c2a")


def scene_library():
    d = Doc(106)
    r = d.r
    wall = d.lin([(0, "#5a3a2e"), (0.3, "#8a5e44"), (0.8, "#a47452"), (1, "#8a5e44")])
    d.add(f'<rect width="{W}" height="{H}" fill="{wall}"/>')
    # warm light pooled on the back wall
    d.add(glow(d, 1150, 560, 900, "#ffd59a", 0.35, ry=520))
    # gentle wood panelling low on the back wall
    d.add(f'<rect x="0" y="600" width="{W}" height="220" fill="#6e4630" opacity="0.45"/>')
    d.add(f'<rect x="0" y="596" width="{W}" height="12" fill="#c08a5a" opacity="0.7"/>')
    for x in range(380, 1620, 205):
        d.add(f'<rect x="{x + 16}" y="630" width="170" height="160" rx="10" fill="none" stroke="#5a3a28" stroke-width="4" opacity="0.4"/>')
    # curved tall bookcases, left and right
    def bookcase(x0, x1, side):
        out = ""
        case = d.lin([(0, "#7a4a2a"), (1, "#5a3420")], 0, 0, 1, 0)
        top = 40
        inner_w = x1 - x0
        out += (f'<path d="M{x0} 860 L{x0} {top + 120} Q{(x0 + x1) / 2:.0f} {top - 40} {x1} {top + 120} L{x1} 860 Z" '
                f'fill="{case}" stroke="{INK}" stroke-width="6"/>')
        out += (f'<path d="M{x0 + 26} 840 L{x0 + 26} {top + 140} Q{(x0 + x1) / 2:.0f} {top - 4} {x1 - 26} {top + 140} L{x1 - 26} 840 Z" fill="#3e2418"/>')
        ys = [260, 400, 540, 680, 820]
        for i, y in enumerate(ys):
            curve = 10 * side
            out += books_row(r, x0 + 30, x1 - 30, y, 70, 115)
            out += (f'<path d="M{x0 + 20} {y} Q{(x0 + x1) / 2:.0f} {y + 16} {x1 - 20} {y} L{x1 - 20} {y + 16} Q{(x0 + x1) / 2:.0f} {y + 32} {x0 + 20} {y + 16} Z" '
                    f'fill="#8a5a33" stroke="{INK}" stroke-width="3"/>')
        out += books_row(r, x0 + 60, x1 - 60, 150, 50, 80)
        out += (f'<path d="M{x0 + 20} 150 Q{(x0 + x1) / 2:.0f} 166 {x1 - 20} 150 L{x1 - 20} 166 Q{(x0 + x1) / 2:.0f} 182 {x0 + 20} 166 Z" '
                f'fill="#8a5a33" stroke="{INK}" stroke-width="3"/>')
        # rim light on the inner edge
        lx = x1 - 6 if side < 0 else x0 + 6
        out += f'<path d="M{lx} 850 L{lx} {top + 120}" stroke="#ffcf8a" stroke-width="6" opacity="0.5"/>'
        return out
    d.add(bookcase(-30, 330, -1))
    d.add(bookcase(1600, 1960, 1))
    # candles on the bookcases + a few floating candles high at the edges
    for x, y in [(80, 400), (250, 680), (1680, 540), (1860, 260)]:
        d.add(candle(d, x, y - 2, 1.0))
    for x, y in [(450, 320), (530, 290), (1490, 320), (1560, 300)]:
        d.add(candle(d, x, y, 0.9))
    # flying books near the top corners
    for x, y, s, c, a, f in [(170, 90, 1.0, "#3f78b5", -10, 1.0), (470, 150, 0.75, "#c4513f", 8, 0.6),
                             (1730, 100, 1.05, "#4f9a5a", 12, 0.8), (1500, 210, 0.7, "#e0a83a", -6, 1.1),
                             (1830, 300, 0.7, "#8a5ab0", -14, 0.7)]:
        d.add(flying_book(x, y, s, c, a, f))
    # floor + rug
    fl = d.lin([(0, "#8a5a3a"), (1, "#5a3624")])
    d.add(f'<rect x="0" y="820" width="{W}" height="260" fill="{fl}"/>')
    d.add(floor_planks(r, 820, 1080, "#7a4e32", "#946446", "#4a2e1e", 6))
    d.add(f'<rect x="0" y="814" width="{W}" height="12" fill="#5a3624"/>')
    d.add(rug(d, 860, 960, 700, 92, "#a8473f", "#e8b44a", "#7a2e3a"))
    d.add(glow(d, 1100, 840, 700, "#ffd59a", 0.18, ry=80))
    d.add(motes(r, 30, 0, W, 250, 900, "#ffe8b0", 1.5, 3, avoid=(700, 300, 1880, 1000)))
    return d.svg(vignette=0.3)


def scene_tunnel():
    d = Doc(107)
    r = d.r
    d.add(f'<rect width="{W}" height="{H}" fill="#121830"/>')
    vx, vy = 1650, 600
    # twisty tube: jagged rings from far (light, small) to near (dark, huge), centres along a curve
    N = 7
    rings = []
    for i in range(N):
        t = i / (N - 1)  # 0 = far, 1 = near
        cx = vx + (900 - vx) * t + 260 * math.sin(t * math.pi) * -1
        cy = vy + (560 - vy) * t - 70 * math.sin(t * math.pi * 1.3)
        rx = 70 + 1250 * t ** 1.6
        ry = 60 + 760 * t ** 1.6
        rings.append((t, cx, cy, rx, ry))
    def lerp(a, b, t):
        return tuple(int(a[k] + (b[k] - a[k]) * t) for k in range(3))
    far_c, near_c = (0x4a, 0x60, 0x9a), (0x16, 0x1c, 0x38)
    for t, cx, cy, rx, ry in rings:
        col = "#%02x%02x%02x" % lerp(far_c, near_c, t ** 0.6)
        dark = "#%02x%02x%02x" % lerp(far_c, near_c, min(1, t ** 0.6 + 0.25))
        hole = smooth(blob_pts(r, cx, cy, rx, ry, 11, 0.1, r.uniform(0, 1)))
        # each ring is a rock band shaded darker away from the light
        g = d.lin([(0, col), (1, dark)], 1, 0.3, 0, 0.7)
        d.add(f'<path d="M-50 -50 L1970 -50 L1970 1130 L-50 1130 Z {hole}" fill="{g}" fill-rule="evenodd"/>')
        # cast shadow just inside the lip, then a soft rim light facing the far glow
        d.add(f'<path d="{hole}" fill="none" stroke="#0a0e20" stroke-width="{10 + 30 * t:.0f}" opacity="0.25" transform="translate({6 + 14 * t:.0f} {8 + 14 * t:.0f})"/>')
        d.add(f'<path d="{hole}" fill="none" stroke="#8fb0f0" stroke-width="{3 + 6 * (1 - t):.0f}" opacity="{0.08 + 0.1 * (1 - t):.2f}"/>')
        # a few boulders on each band
        for _ in range(3):
            a = r.uniform(0, 2 * math.pi)
            bx, by = cx + math.cos(a) * rx * 1.12, cy + math.sin(a) * ry * 1.12
            br = 18 + 90 * t
            d.add(f'<path d="{smooth(blob_pts(r, bx, by, br, br * 0.7, 8, 0.2))}" fill="{dark}" opacity="0.7"/>')
    # far glow behind the crystal
    d.add(glow(d, vx, vy, 360, "#7fe6ff", 0.6, ry=280))
    d.add(glow(d, vx, vy, 120, "#e0fbff", 0.7))
    # tunnel floor winding toward the light
    fl = d.lin([(0, "#3a4a7a"), (0.4, "#26305a"), (1, "#161c36")])
    floor = (f'M-50 800 C300 760 700 760 1000 720 C1300 680 1500 650 {vx - 40} 640 L{vx + 30} 640 '
             f'C1760 660 1900 760 1970 860 L1970 1130 L-50 1130 Z')
    d.add(f'<path d="{floor}" fill="{fl}"/>')
    d.add(f'<path d="M-50 800 C300 760 700 760 1000 720 C1300 680 1500 650 {vx - 40} 640" stroke="#5a72b0" stroke-width="5" fill="none" opacity="0.5"/>')
    d.add(f'<path d="M300 900 C700 850 1100 760 {vx - 20} 650" stroke="#4a5e98" stroke-width="40" fill="none" opacity="0.18" stroke-linecap="round"/>')
    # foreground rock overhangs in the corners
    for pts in ([(-60, -60), (700, -60), (520, 60), (300, 120), (120, 260), (-60, 380)],
                [(1980, -60), (1300, -60), (1500, 50), (1720, 120), (1860, 260), (1980, 300)],
                [(-60, 1140), (-60, 940), (120, 1000), (260, 1140)]):
        d.add(f'<path d="{smooth(pts)}" fill="#0d1226" stroke="#24305a" stroke-width="5"/>')
    # the little glowing crystal far away
    for ang, h, w in [(-25, 50, 18), (0, 74, 24), (22, 56, 18)]:
        d.add(crystal(vx + ang * 0.6, vy + 30, h, w, ang, "#8fe6ff", "#e8fcff", "#3f9fd0", "#1d3a66"))
    d.add(sparkle(vx + 30, vy - 50, 14, "#ffffff", 0.9))
    d.add(sparkle(vx - 40, vy - 20, 8, "#ffffff", 0.8))
    # faint glowing mushrooms along the walls
    for x, y, s, c in [(120, 820, 0.9, "#7ff0e0"), (180, 840, 0.6, "#7ff0e0"), (60, 560, 0.7, "#c79bff"),
                       (1820, 950, 1.0, "#7ff0e0"), (1880, 920, 0.7, "#c79bff"), (1560, 700, 0.5, "#7ff0e0"),
                       (1480, 690, 0.4, "#c79bff"), (420, 160, 0.6, "#c79bff"), (1700, 240, 0.6, "#7ff0e0")]:
        d.add(mushroom(d, x, y, s, c, "#cfe8f0", 0.4))
    d.add(motes(r, 30, 0, W, 200, 1000, "#9fe8ff", 1.5, 3, avoid=(700, 300, 1550, 1000)))
    return d.svg(vignette=0.35, vig_color="#05070f")


def scene_mirror_hall():
    d = Doc(108)
    r = d.r
    wall = d.lin([(0, "#6e5470"), (0.35, "#a58098"), (0.8, "#b8949f"), (1, "#a8848f")])
    d.add(f'<rect width="{W}" height="{H}" fill="{wall}"/>')
    d.add(glow(d, 1250, 520, 800, "#ffe6d6", 0.3, ry=500))
    # damask-free soft pilasters + a picture rail
    for x in (360, 1560):
        g = d.lin([(0, "#c8a6b2"), (1, "#a08090")], 0, 0, 1, 0)
        d.add(f'<rect x="{x - 26}" y="120" width="52" height="700" fill="{g}" opacity="0.7"/>')
    d.add(f'<rect x="0" y="110" width="{W}" height="14" fill="#d8b46a" opacity="0.7"/>')
    d.add(f'<rect x="0" y="640" width="{W}" height="180" fill="#8a6878" opacity="0.35"/>')
    d.add(f'<rect x="0" y="636" width="{W}" height="10" fill="#d8b46a" opacity="0.6"/>')
    # ceiling cornice
    d.add(f'<rect x="0" y="0" width="{W}" height="110" fill="#5a4060" opacity="0.6"/>')
    def mirror(cx, top, w, h):
        frame = d.lin([(0, "#ffe58a"), (0.4, "#e0a83a"), (1, "#a8701e")], 0, 0, 1, 1)
        glass = d.lin([(0, "#e6f0ff"), (0.5, "#c7d0ea"), (1, "#b6a8d0")], 0, 0, 0.4, 1)
        x0, x1 = cx - w / 2, cx + w / 2
        out = glow(d, cx, top + h / 2, w * 1.1, "#fff0c0", 0.3, ry=h * 0.65)
        out += f'<path d="{arch_path(x0 - 24, x1 + 24, top + w / 2, top + h + 24)}" fill="{frame}" stroke="{INK}" stroke-width="5"/>'
        out += f'<path d="{arch_path(x0, x1, top + w / 2 + 6, top + h)}" fill="{glass}" stroke="#a8701e" stroke-width="4"/>'
        # soft reflection of the room: floor line and a far mirror glow
        out += f'<path d="M{x0 + 4} {top + h * 0.78:.0f} L{x1 - 4} {top + h * 0.78:.0f} L{x1 - 4} {top + h - 4} L{x0 + 4} {top + h - 4} Z" fill="#c9b2c8" opacity="0.6"/>'
        out += f'<path d="{arch_path(cx - w * 0.18, cx + w * 0.18, top + h * 0.36, top + h * 0.78)}" fill="#f2d48a" opacity="0.35"/>'
        out += f'<path d="M{x0 + w * 0.2:.0f} {top + h * 0.15:.0f} L{x0 + w * 0.45:.0f} {top + h * 0.15:.0f} L{x0 + w * 0.15:.0f} {top + h * 0.7:.0f} L{x0 + 4:.0f} {top + h * 0.7:.0f} Z" fill="#ffffff" opacity="0.35"/>'
        out += f'<path d="M{x0 + w * 0.52:.0f} {top + h * 0.12:.0f} L{x0 + w * 0.62:.0f} {top + h * 0.12:.0f} L{x0 + w * 0.32:.0f} {top + h * 0.72:.0f} L{x0 + w * 0.22:.0f} {top + h * 0.72:.0f} Z" fill="#ffffff" opacity="0.25"/>'
        # ornate crest and scrolls
        ty = top - 6
        out += (f'<path d="M{cx - 60} {ty + 10} Q{cx - 40} {ty - 40} {cx} {ty - 56} Q{cx + 40} {ty - 40} {cx + 60} {ty + 10} Z" fill="{frame}" stroke="{INK}" stroke-width="4"/>'
                f'<circle cx="{cx}" cy="{ty - 22}" r="16" fill="#ff8fb1" stroke="{INK}" stroke-width="3"/>'
                f'<circle cx="{cx - 5}" cy="{ty - 27}" r="5" fill="#fff" opacity="0.7"/>')
        for sx in (-1, 1):
            out += (f'<circle cx="{cx + sx * (w / 2 + 24)}" cy="{top + w / 2}" r="18" fill="{frame}" stroke="{INK}" stroke-width="4"/>'
                    f'<circle cx="{cx + sx * (w / 2 + 24)}" cy="{top + h + 10}" r="16" fill="{frame}" stroke="{INK}" stroke-width="4"/>')
        out += f'<rect x="{x0 - 40}" y="{top + h + 20}" width="{w + 80}" height="18" rx="8" fill="{frame}" stroke="{INK}" stroke-width="4"/>'
        for _ in range(5):
            out += sparkle(r.uniform(x0 + 10, x1 - 10), r.uniform(top + 40, top + h - 40), r.uniform(8, 16), "#ffffff", 0.9)
        return out
    d.add(mirror(170, 190, 220, 560))
    d.add(mirror(1750, 190, 220, 560))
    # wall sconces with candles
    for x in (360, 1560):
        d.add(f'<path d="M{x - 34} 420 Q{x} 450 {x + 34} 420 L{x + 20} 436 Q{x} 452 {x - 20} 436 Z" fill="#e0a83a" stroke="{INK}" stroke-width="3"/>')
        d.add(candle(d, x, 420, 1.1))
    # checkered marble floor in perspective (soft, low contrast)
    floor_top = 820
    d.add(f'<rect x="0" y="{floor_top}" width="{W}" height="{H - floor_top}" fill="#d6c2cc"/>')
    hz = 380.0
    vxp = 1000.0
    tiles = []
    rows = [floor_top]
    k = 1.0
    z = 1.0
    while rows[-1] < H:
        z *= 0.78
        rows.append(hz + (floor_top - hz) / z)
    for j in range(len(rows) - 1):
        ya, yb = rows[j], rows[j + 1]
        fa, fb = (ya - hz) / (floor_top - hz), (yb - hz) / (floor_top - hz)
        for i in range(-14, 15):
            if (i + j) % 2:
                continue
            xa0, xa1 = vxp + (i * 140 - 0) * fa, vxp + ((i + 1) * 140) * fa
            xb0, xb1 = vxp + (i * 140) * fb, vxp + ((i + 1) * 140) * fb
            tiles.append(f'M{xa0:.0f} {ya:.0f} L{xa1:.0f} {ya:.0f} L{xb1:.0f} {yb:.0f} L{xb0:.0f} {yb:.0f} Z')
    d.add(f'<path d="{" ".join(tiles)}" fill="#bfa8b8"/>')
    fsheen = d.lin([(0, "#ffffff", 0.35), (1, "#ffffff", 0)])
    d.add(f'<rect x="0" y="{floor_top}" width="{W}" height="140" fill="{fsheen}"/>')
    d.add(f'<rect x="0" y="{floor_top - 8}" width="{W}" height="12" fill="#8a6878"/>')
    # mirror reflections on the floor
    for x in (170, 1750):
        d.add(f'<ellipse cx="{x}" cy="{floor_top + 50}" rx="150" ry="26" fill="#fff4e0" opacity="0.35"/>')
    for _ in range(18):
        x, y = r.uniform(0, W), r.uniform(130, 1050)
        if 700 < x < 1880 and 300 < y < 1000:
            continue
        d.add(sparkle(x, y, r.uniform(6, 12), "#fff6e0", 0.8))
    return d.svg(vignette=0.25, vig_color="#2a1424")


def scene_vault():
    d = Doc(109)
    r = d.r
    wall = d.lin([(0, "#3e2a1e"), (0.35, "#7a5638"), (0.8, "#94693f"), (1, "#7a5638")])
    d.add(f'<rect width="{W}" height="{H}" fill="{wall}"/>')
    mask = d.fade_mask(1290, 620, 700, 460, 0.1)
    d.add(f'<g mask="{mask}">{stone_blocks(r, 0, 140, W, 820, 170, 90, "#86603c", "#9a7248", "#74522f", 0.6)}</g>')
    # warm golden light from above
    d.add(glow(d, 1150, 400, 900, "#ffd07a", 0.35, ry=600))
    beam = d.lin([(0, "#fff0b8", 0.35), (1, "#fff0b8", 0)])
    d.add(f'<path d="M1500 0 L1700 0 L1500 820 L1050 820 Z" fill="{beam}"/>')
    # ceiling vault
    ceil = d.lin([(0, "#24170f"), (1, "#3e2a1e", 0)])
    d.add(f'<rect width="{W}" height="260" fill="{ceil}"/>')
    # wall niches with treasure (upper corners only)
    for x0, x1 in [(170, 370)]:
        d.add(f'<path d="{arch_path(x0, x1, 340, 470)}" fill="#4a3220" stroke="{INK}" stroke-width="5"/>')
        d.add(f'<g clip-path="{d.clip(arch_path(x0, x1, 340, 470))}">{coin_pile(d, r, (x0 + x1) / 2, 470, 90, 50, 26, 2)}</g>')
        d.add(f'<rect x="{x0 - 10}" y="466" width="{x1 - x0 + 20}" height="20" rx="4" fill="#9a7248" stroke="{INK}" stroke-width="4"/>')
    # floor
    fl = d.lin([(0, "#a07848"), (0.3, "#8a6440"), (1, "#5a3e26")])
    d.add(f'<rect x="0" y="820" width="{W}" height="260" fill="{fl}"/>')
    d.add(f'<rect x="0" y="812" width="{W}" height="14" fill="#b88a58"/>')
    for k in range(1, 5):
        y = 820 + 260 * (k / 5) ** 1.4
        d.add(f'<path d="M0 {y:.0f} L{W} {y:.0f}" stroke="#4a3220" stroke-width="3" opacity="0.35"/>')
    d.add(glow(d, 1280, 880, 560, "#ffe08a", 0.3, ry=110))
    # stone pillars at the sides
    d.add(pillar(d, 60, 100, 40, 840, "#a07c58", "#d0aa7c", "#6e4e30"))
    d.add(pillar(d, 1860, 120, 40, 840, "#a07c58", "#d0aa7c", "#6e4e30"))
    # coin piles at the sides
    d.add(coin_pile(d, r, 1840, 1000, 210, 220, 150, 8))
    d.add(coin_pile(d, r, 1900, 1080, 200, 140, 80, 4))
    d.add(coin_pile(d, r, -30, 1090, 150, 110, 40, 2))
    # a little crown and goblet on the big pile
    cx, cy = 1830, 800
    d.add(f'<path d="M{cx - 50} {cy} L{cx - 56} {cy - 50} L{cx - 26} {cy - 24} L{cx} {cy - 62} L{cx + 26} {cy - 24} L{cx + 56} {cy - 50} L{cx + 50} {cy} Z" '
          f'fill="#ffd34d" stroke="#9a6416" stroke-width="5" stroke-linejoin="round"/>')
    d.add(gem(cx, cy - 16, 12, "#ff5a7a", "#ffc2cf"))
    for gx in (cx - 56, cx, cx + 56):
        d.add(f'<circle cx="{gx}" cy="{cy - 54 if gx != cx else cy - 66}" r="7" fill="#fff6c4" stroke="#9a6416" stroke-width="3"/>')
    gx, gy = 1730, 910
    d.add(f'<path d="M{gx - 34} {gy - 80} Q{gx - 30} {gy - 30} {gx} {gy - 26} Q{gx + 30} {gy - 30} {gx + 34} {gy - 80} Z" fill="#ffd34d" stroke="#9a6416" stroke-width="5"/>'
          f'<path d="M{gx} {gy - 26} L{gx} {gy - 4} M{gx - 22} {gy} L{gx + 22} {gy}" stroke="#d8a030" stroke-width="10" stroke-linecap="round"/>')
    d.add(gem(gx, gy - 56, 10, "#4fb0ff", "#c8e8ff"))
    # loose jewels near the sides
    for x, y, c, l in [(1540, 1030, "#b07aff", "#e6d4ff"), (1600, 980, "#ff5a7a", "#ffc2cf")]:
        d.add(gem(x, y, 16, c, l))
    d.add(motes(r, 30, 0, W, 200, 1000, "#fff0b0", 2, 4, avoid=(700, 300, 1600, 1000)))
    for _ in range(8):
        d.add(sparkle(r.uniform(1500, 1900), r.uniform(700, 1050), r.uniform(10, 18), "#fffbe6", 0.9))
    return d.svg(vignette=0.32)


def scene_goblin_den():
    d = Doc(110)
    r = d.r
    bg = d.rad([(0, "#a8805e"), (0.6, "#7e5a40"), (1, "#4a3222")], cx=0.6, cy=0.55, r=0.75)
    d.add(f'<rect width="{W}" height="{H}" fill="{bg}"/>')
    # earthy wall texture: pebbles and soil strata, faded in the centre-right
    mask = d.fade_mask(1250, 620, 640, 420, 0.15)
    tex = ""
    for _ in range(80):
        x, y = r.uniform(0, W), r.uniform(60, 820)
        tex += f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{r.uniform(8, 22):.0f}" ry="{r.uniform(5, 12):.0f}" fill="{r.choice(["#6e4e36", "#94704f", "#5e4230"])}" opacity="0.6"/>'
    for k in range(5):
        y = 180 + k * 130
        tex += f'<path d="M0 {y} Q480 {y + r.uniform(-30, 30):.0f} 960 {y} T1920 {y}" stroke="#6a4a34" stroke-width="10" fill="none" opacity="0.25"/>'
    d.add(f'<g mask="{mask}">{tex}</g>')
    # burrow ceiling arch (dark earth) with roots
    d.add(f'<path d="M0 0 L{W} 0 L{W} 420 Q1880 120 1500 70 Q960 20 420 70 Q60 120 0 420 Z" fill="#4a3222"/>')
    d.add(f'<path d="M0 420 Q60 120 420 70 Q960 20 1500 70 Q1880 120 1920 420" stroke="#2e1e14" stroke-width="8" fill="none" opacity="0.6"/>')
    for _ in range(22):
        x = r.uniform(0, W)
        inside = 380 < x < 1540
        y0 = 50 + 40 * (abs(x - 960) / 960) ** 2 * 3
        L = r.uniform(30, 80) if inside else r.uniform(80, 220)
        w = r.uniform(4, 10)
        d.add(f'<path d="M{x:.0f} {y0:.0f} q{r.uniform(-30, 30):.0f} {L * 0.5:.0f} {r.uniform(-20, 20):.0f} {L:.0f} q{r.uniform(-10, 10):.0f} {L * 0.2:.0f} {r.uniform(-14, 14):.0f} {L * 0.35:.0f}" '
              f'stroke="{r.choice(["#8a6040", "#7a5236", "#9a6e4a"])}" stroke-width="{w:.0f}" fill="none" stroke-linecap="round"/>')
    # hanging lantern (left)
    d.add(lantern(d, 250, 60, 250, 1.0, "#ffcf6a"))
    # patched blanket slung like a hammock on the left wall
    patches = ["#c4513f", "#e0a83a", "#4f8ab5", "#6aa05a", "#b5778f", "#d9c38a"]
    hx0, hx1, hy = 10, 370, 330
    d.add(f'<path d="M{hx0} {hy} L{hx0 - 30} {hy - 60} M{hx1} {hy} L{hx1 + 20} {hy - 70}" stroke="#7a5a3a" stroke-width="6"/>')
    quilt = ""
    for i in range(6):
        for j in range(2):
            xa = hx0 + i * (hx1 - hx0) / 6
            xb = xa + (hx1 - hx0) / 6
            sag = lambda x: 70 * math.sin(math.pi * (x - hx0) / (hx1 - hx0))
            ya0 = hy + sag(xa) + j * 50
            yb0 = hy + sag(xb) + j * 50
            quilt += (f'<path d="M{xa:.0f} {ya0:.0f} L{xb:.0f} {yb0:.0f} L{xb:.0f} {yb0 + 50:.0f} L{xa:.0f} {ya0 + 50:.0f} Z" fill="{patches[(i * 2 + j * 3) % 6]}" '
                      f'stroke="{INK}" stroke-width="3" stroke-dasharray="7 5"/>')
    d.add(quilt)
    d.add(f'<path d="M{hx0} {hy} Q{(hx0 + hx1) / 2} {hy + 140} {hx1} {hy}" stroke="{INK}" stroke-width="4" fill="none" opacity="0.6"/>')
    # shelf with jars (top right) and the round wooden door below it
    d.add(shelf_board(1580, 1910, 330))
    for x, col, k in [(1620, "#ffcf4a", 2), (1690, "#7fd0a0", 0), (1760, "#e07a5a", 1), (1830, "#b58cff", 2), (1880, "#7fc8ff", 0)]:
        d.add(bottle(x, 330, 0.9, col, k, "#f4e8d0"))
    dcx, dcy, dr = 1810, 640, 170
    d.add(f'<circle cx="{dcx}" cy="{dcy}" r="{dr + 26}" fill="#8a7a6a" stroke="{INK}" stroke-width="5"/>')
    for k in range(14):
        a = 2 * math.pi * k / 14
        d.add(f'<circle cx="{dcx + math.cos(a) * (dr + 14):.0f}" cy="{dcy + math.sin(a) * (dr + 14):.0f}" r="18" fill="{r.choice(["#9a8a78", "#a89884", "#8a7a6a"])}" stroke="{INK}" stroke-width="3" stroke-opacity="0.6"/>')
    dg = d.lin([(0, "#b07a44"), (1, "#7a4a26")])
    d.add(f'<circle cx="{dcx}" cy="{dcy}" r="{dr}" fill="{dg}" stroke="{INK}" stroke-width="5"/>')
    for k in range(-3, 4):
        x = dcx + k * dr / 3.6
        dy = math.sqrt(max(0, dr ** 2 - (x - dcx) ** 2))
        d.add(f'<path d="M{x:.0f} {dcy - dy + 4:.0f} L{x:.0f} {dcy + dy - 4:.0f}" stroke="#6a3f22" stroke-width="4" opacity="0.6"/>')
    d.add(f'<circle cx="{dcx}" cy="{dcy - 70}" r="44" fill="#ffd87a" stroke="{INK}" stroke-width="5"/>'
          f'<path d="M{dcx - 44} {dcy - 70} L{dcx + 44} {dcy - 70} M{dcx} {dcy - 114} L{dcx} {dcy - 26}" stroke="{INK}" stroke-width="5"/>'
          f'<circle cx="{dcx - 120}" cy="{dcy + 10}" r="14" fill="#e0b040" stroke="{INK}" stroke-width="4"/>')
    # floor
    fl = d.lin([(0, "#8a6646"), (0.4, "#74543a"), (1, "#4e3626")])
    d.add(f'<path d="M-20 830 Q600 800 1200 820 Q1600 830 1940 812 L1940 1100 L-20 1100 Z" fill="{fl}"/>')
    d.add(f'<path d="M-20 830 Q600 800 1200 820 Q1600 830 1940 812" stroke="#b08a64" stroke-width="6" fill="none" opacity="0.5"/>')
    for _ in range(30):
        x, y = r.uniform(0, W), r.uniform(860, 1070)
        d.add(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{r.uniform(6, 16):.0f}" ry="{r.uniform(3, 7):.0f}" fill="#5e4230" opacity="0.6"/>')
    d.add(glow(d, 250, 300, 400, "#ffcf6a", 0.18))
    # braided rag rug on the floor (flat, so the hero can stand on it)
    for k, col in enumerate(["#b5674a", "#d9a45a", "#7a9a6a", "#c47a8a", "#d9b98a"]):
        d.add(f'<ellipse cx="330" cy="960" rx="{300 - k * 50}" ry="{58 - k * 10}" fill="{col}" stroke="{INK}" stroke-width="3" stroke-opacity="0.35"/>')
    # garlic and a little pot hanging from the roots (top-left corner)
    for x, y in [(70, 150), (110, 175), (150, 140)]:
        d.add(f'<path d="M{x} 60 L{x} {y - 20}" stroke="#8a6040" stroke-width="3"/>'
              f'<path d="M{x - 14} {y} Q{x - 16} {y - 22} {x} {y - 26} Q{x + 16} {y - 22} {x + 14} {y} Q{x} {y + 12} {x - 14} {y} Z" fill="#f4ead6" stroke="{INK}" stroke-width="3" stroke-opacity="0.6"/>')
    d.add(f'<path d="M340 60 L340 120" stroke="#8a6040" stroke-width="3"/>'
          f'<path d="M312 122 L368 122 L360 170 Q340 182 320 170 Z" fill="#c4703f" stroke="{INK}" stroke-width="4" stroke-linejoin="round"/>'
          f'<path d="M318 132 L362 132" stroke="#e8a070" stroke-width="4" opacity="0.6"/>')
    # a candle on the jar shelf and a broom by the door
    d.add(candle(d, 1585, 326, 0.8))
    d.add(f'<path d="M1560 600 L1600 990" stroke="{INK}" stroke-width="16" stroke-linecap="round"/>'
          f'<path d="M1560 600 L1600 990" stroke="#b07a4a" stroke-width="9" stroke-linecap="round"/>'
          f'<path d="M1580 900 Q1560 980 1570 1010 L1640 1004 Q1626 960 1608 896 Z" fill="#d9b06a" stroke="{INK}" stroke-width="4" stroke-linejoin="round"/>'
          f'<path d="M1586 924 L1612 920" stroke="#8a6a3a" stroke-width="4"/>')
    # the hiding barrel (centre-right)
    d.add(f'<ellipse cx="1350" cy="1002" rx="110" ry="18" fill="#2e1e14" opacity="0.35"/>')
    d.add(barrel(1350, 1000, 190, 180))
    # clutter at the right edge, clear of the zone
    d.add(f'<path d="{smooth([(1830, 1080), (1810, 960), (1850, 900), (1900, 880), (1950, 920), (1960, 1080)])}" fill="#c8a878" stroke="{INK}" stroke-width="5"/>'
          f'<path d="M1850 905 Q1880 890 1910 900" stroke="#8a6a44" stroke-width="5" fill="none"/>')
    d.add(barrel(1735, 1100, 140, 130, "#9a5f36"))
    # little mushrooms in the bottom-left corner
    for x, y, s in [(40, 1060, 0.8), (80, 1070, 0.6)]:
        d.add(mushroom(d, x, y, s, "#e07a5a", "#f4e0c8", 0.0))
    d.add(motes(r, 16, 0, W, 200, 800, "#ffe0a0", 1.5, 3, avoid=(700, 300, 1880, 1000)))
    return d.svg(vignette=0.3)


def scene_workshop():
    d = Doc(111)
    r = d.r
    wall = d.lin([(0, "#4e3830"), (0.3, "#8a634a"), (0.8, "#9e7556"), (1, "#86604a")])
    d.add(f'<rect width="{W}" height="{H}" fill="{wall}"/>')
    d.add(glow(d, 1250, 600, 760, "#ffd8a0", 0.25, ry=460))
    # timber framing on the wall (edges only)
    for x in (430, 1580):
        d.add(f'<rect x="{x - 22}" y="0" width="44" height="820" fill="#5a3e2c" stroke="{INK}" stroke-width="4" stroke-opacity="0.5"/>')
    d.add(f'<rect x="0" y="22" width="{W}" height="36" fill="#5a3e2c" stroke="{INK}" stroke-width="4" stroke-opacity="0.5"/>')
    # big arched window with stars (top left)
    wx0, wx1, wtop, wbot = 70, 370, 100, 520
    sky = d.lin([(0, "#1e2350"), (1, "#3e3f86")])
    d.add(f'<path d="{arch_path(wx0 - 20, wx1 + 20, wtop + (wx1 - wx0) / 2, wbot + 20)}" fill="#6a4a34" stroke="{INK}" stroke-width="6"/>')
    d.add(f'<path d="{arch_path(wx0, wx1, wtop + (wx1 - wx0) / 2, wbot)}" fill="{sky}"/>')
    d.add(f'<circle cx="{wx0 + 210}" cy="{wtop + 90}" r="40" fill="#fff4c4"/><circle cx="{wx0 + 228}" cy="{wtop + 78}" r="36" fill="#2a2e62"/>')
    for _ in range(26):
        x, y = r.uniform(wx0 + 10, wx1 - 10), r.uniform(wtop + 30, wbot - 10)
        if (x - (wx0 + wx1) / 2) ** 2 + (y - (wtop + (wx1 - wx0) / 2)) ** 2 > ((wx1 - wx0) / 2 - 10) ** 2 and y < wtop + (wx1 - wx0) / 2:
            continue
        d.add(sparkle(x, y, r.uniform(4, 10), "#fff6c4", r.uniform(0.6, 1)))
    d.add(f'<path d="M{(wx0 + wx1) / 2} {wtop} L{(wx0 + wx1) / 2} {wbot} M{wx0} 360 L{wx1} 360" stroke="#6a4a34" stroke-width="12"/>')
    d.add(f'<rect x="{wx0 - 40}" y="{wbot + 10}" width="{wx1 - wx0 + 80}" height="24" rx="6" fill="#8a5a33" stroke="{INK}" stroke-width="4"/>')
    d.add(bottle(wx0 + 30, wbot + 10, 0.8, "#7fd0a0", 0))
    d.add(f'<path d="M{wx0 + 250} {wbot + 10} L{wx0 + 250} {wbot - 50}" stroke="#4f8a3a" stroke-width="5"/><circle cx="{wx0 + 250}" cy="{wbot - 50}" r="16" fill="#ff8fb1"/>'
          f'<rect x="{wx0 + 228}" y="{wbot - 20}" width="44" height="30" rx="6" fill="#c46a3a" stroke="{INK}" stroke-width="3"/>')
    # moonlight spill
    spill = d.lin([(0, "#bcd0ff", 0.18), (1, "#bcd0ff", 0)])
    d.add(f'<path d="M{wx0} {wbot} L{wx1} {wbot} L{wx1 + 420} 1080 L{wx0 + 120} 1080 Z" fill="{spill}"/>')
    # tall shelf unit with bottles (right edge)
    d.add(f'<rect x="1660" y="130" width="300" height="700" fill="#4e3424" stroke="{INK}" stroke-width="6"/>')
    cols = ["#ff7a8a", "#7fd0ff", "#7fe0a0", "#ffcf4a", "#b58cff", "#ff9f43", "#5fd6c8"]
    for y in (300, 470, 640, 810):
        d.add(shelf_board(1650, 1950, y, "#8a5a33", "#5a3a22", 18))
        x = 1690
        while x < 1920:
            k = r.choice([0, 1, 2])
            d.add(bottle(x, y, r.uniform(0.8, 1.05), r.choice(cols), k))
            x += r.uniform(56, 80)
    d.add(f'<path d="M1666 830 L1666 140" stroke="#ffcf8a" stroke-width="5" opacity="0.5"/>')
    # hanging herbs from the beam
    for x in [480, 560, 640, 1310, 1400, 1490, 1630, 1730, 1830, 30, 150]:
        L = r.uniform(60, 120) if 380 < x < 1540 else r.uniform(90, 150)
        col = r.choice(["#6a9a4a", "#8aaa5a", "#a07ab0", "#c9a24a"])
        d.add(f'<path d="M{x} 58 L{x} {58 + L * 0.4:.0f}" stroke="#7a5a3a" stroke-width="3"/>')
        for k in range(6):
            a = -30 + k * 12
            d.add(f'<ellipse cx="{x + a * 0.6:.0f}" cy="{58 + L * 0.4 + L * 0.35:.0f}" rx="7" ry="{L * 0.36:.0f}" fill="{col}" '
                  f'transform="rotate({a * 0.5:.0f} {x} {58 + L * 0.4:.0f})" stroke="#3e5a2a" stroke-width="1.5" stroke-opacity="0.5"/>')
        d.add(f'<rect x="{x - 9}" y="{58 + L * 0.4 - 4:.0f}" width="18" height="10" rx="3" fill="#c4513f"/>')
    # wooden floor
    fl = d.lin([(0, "#9a6a44"), (1, "#5e3c26")])
    d.add(f'<rect x="0" y="820" width="{W}" height="260" fill="{fl}"/>')
    d.add(floor_planks(r, 820, 1080, "#8a5e3c", "#a87650", "#4a2e1e", 6))
    d.add(f'<rect x="0" y="812" width="{W}" height="14" fill="#4e3424"/>')
    d.add(glow(d, 1300, 860, 520, "#ffc87a", 0.25, ry=110))
    d.add(motes(r, 30, 0, W, 150, 1000, "#c8f0ff", 1.5, 3, avoid=(700, 300, 1650, 1000)))
    for _ in range(6):
        d.add(sparkle(r.uniform(1690, 1900), r.uniform(150, 800), r.uniform(6, 12), "#ffffff", 0.8))
    return d.svg(vignette=0.3)


def big_bell(d, x, y, s=1.0):
    """A great bronze bell on a wooden yoke, hanging from its top at (x, y)."""
    bronze = d.lin([(0, "#f0c26a"), (0.45, "#c98f3a"), (1, "#8a5a22")], 0, 0, 1, 0)
    rim = d.lin([(0, "#ffe09a"), (1, "#9a6a28")], 0, 0, 0, 1)
    out = (f'<rect x="{x - 150 * s:.0f}" y="{y - 14 * s:.0f}" width="{300 * s:.0f}" height="{34 * s:.0f}" rx="{10 * s:.0f}" fill="#6a4a34" stroke="{INK}" stroke-width="5"/>'
           f'<path d="M{x - 40 * s:.0f} {y + 20 * s:.0f} L{x - 46 * s:.0f} {y + 70 * s:.0f} L{x + 46 * s:.0f} {y + 70 * s:.0f} L{x + 40 * s:.0f} {y + 20 * s:.0f} Z" fill="#8a5a33" stroke="{INK}" stroke-width="5" stroke-linejoin="round"/>'
           f'<path d="M{x - 92 * s:.0f} {y + 70 * s:.0f} C{x - 92 * s:.0f} {y + 70 * s:.0f} {x - 88 * s:.0f} {y + 40 * s:.0f} {x:.0f} {y + 40 * s:.0f} C{x + 88 * s:.0f} {y + 40 * s:.0f} {x + 92 * s:.0f} {y + 70 * s:.0f} {x + 92 * s:.0f} {y + 70 * s:.0f} '
           f'C{x + 100 * s:.0f} {y + 190 * s:.0f} {x + 120 * s:.0f} {y + 250 * s:.0f} {x + 160 * s:.0f} {y + 300 * s:.0f} L{x - 160 * s:.0f} {y + 300 * s:.0f} C{x - 120 * s:.0f} {y + 250 * s:.0f} {x - 100 * s:.0f} {y + 190 * s:.0f} {x - 92 * s:.0f} {y + 70 * s:.0f} Z" '
           f'fill="{bronze}" stroke="{INK}" stroke-width="6" stroke-linejoin="round"/>'
           f'<path d="M{x - 160 * s:.0f} {y + 300 * s:.0f} L{x + 160 * s:.0f} {y + 300 * s:.0f} C{x + 160 * s:.0f} {y + 330 * s:.0f} {x + 120 * s:.0f} {y + 340 * s:.0f} {x:.0f} {y + 340 * s:.0f} '
           f'C{x - 120 * s:.0f} {y + 340 * s:.0f} {x - 160 * s:.0f} {y + 330 * s:.0f} {x - 160 * s:.0f} {y + 300 * s:.0f} Z" fill="{rim}" stroke="{INK}" stroke-width="6" stroke-linejoin="round"/>'
           f'<path d="M{x - 80 * s:.0f} {y + 90 * s:.0f} C{x - 90 * s:.0f} {y + 170 * s:.0f} {x - 108 * s:.0f} {y + 230 * s:.0f} {x - 130 * s:.0f} {y + 276 * s:.0f}" fill="none" stroke="#fff3c8" stroke-width="{12 * s:.0f}" stroke-linecap="round" opacity="0.55"/>'
           f'<path d="M{x - 108 * s:.0f} {y + 190 * s:.0f} L{x + 108 * s:.0f} {y + 190 * s:.0f} M{x - 126 * s:.0f} {y + 240 * s:.0f} L{x + 126 * s:.0f} {y + 240 * s:.0f}" stroke="#8a5a22" stroke-width="{6 * s:.0f}" opacity="0.7"/>'
           f'<circle cx="{x:.0f}" cy="{y + 340 * s:.0f}" r="{22 * s:.0f}" fill="#7a5a30" stroke="{INK}" stroke-width="5"/>')
    return out


def scene_belfry():
    d = Doc(113)
    r = d.r
    wall = d.lin([(0, "#4a3d4e"), (0.4, "#7a6a66"), (1, "#8f7b6c")])
    d.add(f'<rect width="{W}" height="{H}" fill="{wall}"/>')
    d.add(f'<g opacity="0.45">{stone_blocks(r, 0, 60, W, 830, 150, 74, "#7d6c68", "#948079", "#625560", 0.7)}</g>')
    d.add(glow(d, 1180, 560, 760, "#ffd8a0", 0.22, ry=420))
    # two tall arched openings onto the dusk: the big one on the left holds the great bell
    def opening(x0, x1, top, bottom, moon=None):
        sky = d.lin([(0, "#2e2a5e"), (0.55, "#a8627a"), (1, "#f2b27a")])
        out = (f'<path d="{arch_path(x0 - 26, x1 + 26, top + (x1 - x0) / 2, bottom + 26)}" fill="#5a4640" stroke="{INK}" stroke-width="6"/>'
               f'<path d="{arch_path(x0, x1, top + (x1 - x0) / 2, bottom)}" fill="{sky}"/>')
        for _ in range(16):
            sx, sy = r.uniform(x0 + 14, x1 - 14), r.uniform(top + 24, top + (bottom - top) * 0.45)
            if (sx - (x0 + x1) / 2) ** 2 + (sy - (top + (x1 - x0) / 2)) ** 2 > ((x1 - x0) / 2 - 14) ** 2 and sy < top + (x1 - x0) / 2:
                continue
            out += sparkle(sx, sy, r.uniform(3, 7), "#fff6d0", r.uniform(0.6, 1))
        if moon:
            mid = d.uid("mk")
            d.defs.append(f'<mask id="{mid}" maskUnits="userSpaceOnUse" x="{moon[0] - 50}" y="{moon[1] - 50}" width="100" height="100">'
                          f'<rect x="{moon[0] - 50}" y="{moon[1] - 50}" width="100" height="100" fill="#fff"/>'
                          f'<circle cx="{moon[0] + 14}" cy="{moon[1] - 10}" r="30" fill="#000"/></mask>')
            out += f'<circle cx="{moon[0]}" cy="{moon[1]}" r="34" fill="#fff4c4" mask="url(#{mid})"/>'
        # far hills and the roofs of a little town, so the tower is clearly high up
        hills = d.lin([(0, "#6a4a78"), (1, "#3e3262")])
        hy = bottom - (bottom - top) * 0.28
        out += (f'<path d="M{x0} {hy:.0f} Q{x0 + (x1 - x0) * 0.25:.0f} {hy - 40:.0f} {x0 + (x1 - x0) * 0.5:.0f} {hy - 12:.0f} T{x1} {hy - 24:.0f} L{x1} {bottom} L{x0} {bottom} Z" fill="{hills}"/>')
        tx = x0 + 18
        while tx < x1 - 30:
            w, h = r.uniform(26, 44), r.uniform(26, 42)
            by = bottom - 14
            out += (f'<rect x="{tx:.0f}" y="{by - h:.0f}" width="{w:.0f}" height="{h:.0f}" fill="#2e2650"/>'
                    f'<path d="M{tx - 4:.0f} {by - h:.0f} L{tx + w / 2:.0f} {by - h - 20:.0f} L{tx + w + 4:.0f} {by - h:.0f} Z" fill="#3a2e5e"/>'
                    f'<rect x="{tx + w * 0.3:.0f}" y="{by - h * 0.6:.0f}" width="7" height="9" fill="#ffd77a"/>')
            tx += w + r.uniform(8, 22)
        return out
    d.add(opening(80, 400, 90, 560))
    d.add(opening(1730, 1890, 170, 640, moon=(1812, 300)))
    # the great bell hangs in the big opening
    d.add(big_bell(d, 240, 170, 0.78))
    # moonlight spill from the big opening
    spill = d.lin([(0, "#f0c0a0", 0.2), (1, "#f0c0a0", 0)])
    d.add(f'<path d="M80 560 L400 560 L860 1080 L260 1080 Z" fill="{spill}" filter="{d.blur(16)}"/>')
    # heavy timber beams across the top and down the sides, with braces
    for x in (470, 1640):
        d.add(f'<rect x="{x - 24}" y="0" width="48" height="830" fill="#5a3e2c" stroke="{INK}" stroke-width="4" stroke-opacity="0.5"/>')
    d.add(f'<rect x="0" y="20" width="{W}" height="40" fill="#5a3e2c" stroke="{INK}" stroke-width="4" stroke-opacity="0.5"/>')
    for bx, dx in ((470, 150), (1640, -150)):
        d.add(f'<path d="M{bx} 400 L{bx + dx} 60 L{bx + dx * 0.78:.0f} 60 L{bx} 330 Z" fill="#6a4a34" stroke="{INK}" stroke-width="4" stroke-opacity="0.5"/>')
    # a sleepy pigeon pair on the small window's sill, and two hanging lanterns
    for px in (1790, 1850):
        d.add(f'<ellipse cx="{px}" cy="618" rx="26" ry="20" fill="#a7a6b4" stroke="{INK}" stroke-width="3"/><circle cx="{px + 18}" cy="604" r="11" fill="#b8b7c4" stroke="{INK}" stroke-width="3"/>'
              f'<path d="M{px + 28} 604 l10 4 l-10 3 Z" fill="#e8a45a"/><circle cx="{px + 21}" cy="602" r="2.2" fill="{INK}"/>')
    d.add(lantern(d, 590, 60, 190, 0.9))
    d.add(lantern(d, 1530, 60, 220, 0.9))
    # a stout rope coil and a little bell-ringer's stool on the floor, out of the stand-up zone
    # wooden floor
    fl = d.lin([(0, "#8a6a4e"), (1, "#54382a")])
    d.add(f'<rect x="0" y="830" width="{W}" height="250" fill="{fl}"/>')
    d.add(floor_planks(r, 830, 1080, "#7a5a40", "#9a7652", "#3e2a1e", 6))
    d.add(f'<rect x="0" y="822" width="{W}" height="14" fill="#4e3424"/>')
    d.add(f'<path d="M1810 940 q40 -40 80 -4 q-6 40 -46 40 q-44 0 -34 -36 Z" fill="none" stroke="#b08a52" stroke-width="12" stroke-linecap="round"/>'
          f'<path d="M1822 940 q30 -26 56 -4 q-8 28 -38 28" fill="none" stroke="#d1ad72" stroke-width="7" stroke-linecap="round"/>')
    d.add(glow(d, 1100, 880, 560, "#ffc87a", 0.22, ry=110))
    d.add(motes(r, 26, 0, W, 150, 1000, "#ffe9c8", 1.5, 3, avoid=(700, 300, 1650, 1000)))
    return d.svg(vignette=0.3)


def scene_lair():
    d = Doc(112)
    r = d.r
    bg = d.rad([(0, "#c08a5e"), (0.55, "#94603e"), (1, "#5a3424")], cx=0.65, cy=0.45, r=0.75)
    d.add(f'<rect width="{W}" height="{H}" fill="{bg}"/>')
    mask = d.fade_mask(1300, 620, 700, 440, 0.15)
    lumps = ""
    for _ in range(26):
        x, y = r.uniform(0, W), r.uniform(80, 800)
        lumps += f'<path d="{smooth(blob_pts(r, x, y, r.uniform(90, 220), r.uniform(50, 120), 9, 0.2))}" fill="{r.choice(["#8a5638", "#a06a46", "#7a4a30"])}" opacity="0.55"/>'
    d.add(f'<g mask="{mask}">{lumps}</g>')
    # light from a hole in the cavern roof
    d.add(glow(d, 1300, 700, 760, "#fff0c0", 0.3, ry=420))
    shaft = d.lin([(0, "#fff6d0", 0.5), (1, "#fff6d0", 0)])
    d.add(f'<path d="M1580 0 L1800 0 L1560 900 L1040 900 Z" fill="{shaft}"/>')
    # cavern roof frame
    d.add(f'<path d="M0 0 L{W} 0 L{W} 500 Q1900 160 1820 80 L1800 0 L1580 0 Q1400 70 960 60 Q420 60 120 160 Q20 260 0 560 Z" fill="#5a3424"/>')
    d.add(f'<path d="M1580 0 Q1690 26 1800 0" stroke="#fff0c0" stroke-width="10" fill="none" opacity="0.7"/>')
    d.add(stalactites(r, 0, 380, 140, 40, 160, "#5a3424", "#3a2016"))
    d.add(stalactites(r, 1820, 1920, 100, 60, 180, "#5a3424", "#3a2016"))
    d.add(stalactites(r, 380, 1540, 55, 30, 70, "#5a3424", "#3a2016", (40, 80)))
    # floor
    fl = d.lin([(0, "#b07a50"), (0.3, "#94603e"), (1, "#5e3826")])
    d.add(f'<path d="M-20 810 Q500 780 1000 800 Q1500 820 1940 790 L1940 1100 L-20 1100 Z" fill="{fl}"/>')
    d.add(f'<path d="M-20 810 Q500 780 1000 800 Q1500 820 1940 790" stroke="#e0b07a" stroke-width="6" fill="none" opacity="0.5"/>')
    # gold heaps and trinkets at the edges
    d.add(coin_pile(d, r, 1880, 900, 230, 240, 140, 8))
    d.add(coin_pile(d, r, 20, 850, 210, 140, 70, 4))
    d.add(coin_pile(d, r, 1920, 1080, 180, 120, 50, 3))
    # round shield + sword leaning on the right heap
    sx, sy = 1800, 730
    d.add(f'<path d="M{sx + 90} {sy - 140} L{sx + 120} {sy + 80}" stroke="#c8d0e0" stroke-width="16" stroke-linecap="round"/>'
          f'<path d="M{sx + 70} {sy - 100} L{sx + 120} {sy - 108}" stroke="#8a5a33" stroke-width="12" stroke-linecap="round"/>'
          f'<circle cx="{sx + 86}" cy="{sy - 150}" r="10" fill="#ffd34d" stroke="{INK}" stroke-width="3"/>')
    d.add(f'<circle cx="{sx}" cy="{sy}" r="70" fill="#4f7ab5" stroke="{INK}" stroke-width="6"/>'
          f'<circle cx="{sx}" cy="{sy}" r="50" fill="none" stroke="#ffd34d" stroke-width="8"/>'
          f'<circle cx="{sx}" cy="{sy}" r="14" fill="#ffd34d" stroke="{INK}" stroke-width="3"/>'
          f'<path d="M{sx - 46} {sy - 30} A56 56 0 0 1 {sx - 10} {sy - 54}" stroke="#ffffff" stroke-width="6" fill="none" opacity="0.5" stroke-linecap="round"/>')
    # goblet on the left heap
    gx, gy = 70, 735
    d.add(f'<path d="M{gx - 30} {gy - 70} Q{gx - 26} {gy - 26} {gx} {gy - 22} Q{gx + 26} {gy - 26} {gx + 30} {gy - 70} Z" fill="#ffd34d" stroke="#9a6416" stroke-width="5"/>'
          f'<path d="M{gx} {gy - 22} L{gx} {gy - 2} M{gx - 20} {gy} L{gx + 20} {gy}" stroke="#d8a030" stroke-width="9" stroke-linecap="round"/>')
    d.add(gem(gx, gy - 50, 9, "#ff5a7a", "#ffc2cf"))
    # the big round nest of cushions (centre-right floor, where the dragon sits)
    ncx, ncy, nrx, nry = 1260, 930, 420, 95
    d.add(f'<ellipse cx="{ncx}" cy="{ncy + 20}" rx="{nrx + 30}" ry="{nry + 20}" fill="#3a2016" opacity="0.3"/>')
    inner = d.rad([(0, "#f4d6a8"), (1, "#c99a6a")])
    d.add(f'<ellipse cx="{ncx}" cy="{ncy}" rx="{nrx}" ry="{nry}" fill="#8a5a33" stroke="{INK}" stroke-width="5"/>')
    d.add(f'<ellipse cx="{ncx}" cy="{ncy - 6}" rx="{nrx - 50}" ry="{nry - 26}" fill="{inner}"/>')
    cushions = ["#d9675a", "#e8a84a", "#6a9ac4", "#9a6ab4", "#e88aa0", "#6ab08a"]
    back, front = [], []
    for k in range(16):
        a = 2 * math.pi * k / 16
        x = ncx + math.cos(a) * (nrx - 30)
        y = ncy + math.sin(a) * (nry - 6)
        col = cushions[k % len(cushions)]
        c = (f'<path d="{smooth(blob_pts(r, x, y - 18, 80, 40, 8, 0.1))}" fill="{col}" stroke="{INK}" stroke-width="4"/>'
             f'<ellipse cx="{x - 18:.0f}" cy="{y - 34:.0f}" rx="30" ry="10" fill="#ffffff" opacity="0.3"/>'
             f'<circle cx="{x:.0f}" cy="{y - 18:.0f}" r="4" fill="{INK}" opacity="0.4"/>')
        (back if math.sin(a) < 0 else front).append((y, c))
    for _, c in sorted(back):
        d.add(c)
    d.add(f'<ellipse cx="{ncx}" cy="{ncy - 10}" rx="{nrx - 110}" ry="{nry - 50}" fill="#fff1d0" opacity="0.4"/>')
    for _, c in sorted(front):
        d.add(c)
    # dust motes in the light shaft + sparkles on the gold
    d.add(motes(r, 30, 1000, 1800, 50, 900, "#fff6d0", 1.5, 3))
    for _ in range(10):
        d.add(sparkle(r.uniform(1600, 1920), r.uniform(650, 1050), r.uniform(10, 18), "#fffbe6", 0.9))
    for _ in range(5):
        d.add(sparkle(r.uniform(0, 420), r.uniform(680, 820), r.uniform(10, 16), "#fffbe6", 0.9))
    return d.svg(vignette=0.32)


def scene_map():
    d = Doc(113)
    r = d.r
    table = d.rad([(0, "#7a5236"), (1, "#3e2618")], r=0.8)
    d.add(f'<rect width="{W}" height="{H}" fill="{table}"/>')
    for k in range(14):
        y = k * 80 + r.uniform(-10, 10)
        d.add(f'<path d="M0 {y:.0f} Q960 {y + r.uniform(-20, 20):.0f} 1920 {y:.0f}" stroke="#2e1c10" stroke-width="3" opacity="0.25" fill="none"/>')
    # torn-edged parchment sheet
    pts = []
    m = 36
    def edge(x0, y0, x1, y1, n):
        out = []
        for i in range(n):
            t = i / n
            x = x0 + (x1 - x0) * t
            y = y0 + (y1 - y0) * t
            j = r.uniform(-12, 12)
            if r.random() < 0.12:
                j += r.choice([-1, 1]) * r.uniform(10, 22)
            if x0 == x1:
                out.append((x + j, y))
            else:
                out.append((x, y + j))
        return out
    pts += edge(m, m, W - m, m, 60)
    pts += edge(W - m, m, W - m, H - m, 34)
    pts += edge(W - m, H - m, m, H - m, 60)
    pts += edge(m, H - m, m, m, 34)
    poly = " ".join(f"{x:.0f},{y:.0f}" for x, y in pts)
    d.add(f'<polygon points="{" ".join(f"{x + 10:.0f},{y + 14:.0f}" for x, y in pts)}" fill="#1e120a" opacity="0.45"/>')
    paper = d.rad([(0, "#f6e8c4"), (0.6, "#eedaa8"), (1, "#d8b77c")], r=0.72)
    d.add(f'<polygon points="{poly}" fill="{paper}" stroke="#9a7444" stroke-width="4" stroke-linejoin="round"/>')
    # burnt / darkened edge band
    d.add(f'<polygon points="{poly}" fill="none" stroke="#b08a54" stroke-width="40" stroke-linejoin="round" opacity="0.35"/>')
    # stains and blotches
    for _ in range(9):
        x, y = r.uniform(150, 1770), r.uniform(150, 930)
        d.add(f'<path d="{smooth(blob_pts(r, x, y, r.uniform(40, 140), r.uniform(30, 100), 10, 0.25))}" fill="#c9a46a" opacity="{r.uniform(0.08, 0.18):.2f}"/>')
    for _ in range(3):
        x, y = r.uniform(200, 1700), r.uniform(200, 900)
        rr = r.uniform(50, 80)
        d.add(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{rr:.0f}" fill="none" stroke="#b08a54" stroke-width="5" opacity="0.15"/>')
    # fold creases
    d.add(f'<path d="M960 40 L960 1040 M40 540 L1880 540" stroke="#c4a06a" stroke-width="6" opacity="0.35"/>'
          f'<path d="M966 40 L966 1040 M40 546 L1880 546" stroke="#fff6dc" stroke-width="3" opacity="0.35"/>')
    # faint dotted grid
    grid = []
    for x in range(160, 1800, 120):
        grid.append(f'M{x} 90 L{x} 990')
    for y in range(120, 1000, 120):
        grid.append(f'M90 {y} L1830 {y}')
    d.add(f'<path d="{" ".join(grid)}" stroke="#8a6a44" stroke-width="3" stroke-dasharray="2 14" stroke-linecap="round" opacity="0.35"/>')
    # faint compass rose (bottom right)
    cx, cy = 1700, 860
    rose = f'<circle cx="{cx}" cy="{cy}" r="110" fill="none" stroke="#7a5a36" stroke-width="4"/>'
    rose += f'<circle cx="{cx}" cy="{cy}" r="96" fill="none" stroke="#7a5a36" stroke-width="2" stroke-dasharray="4 8"/>'
    for k in range(8):
        a = -math.pi / 2 + k * math.pi / 4
        L = 100 if k % 2 == 0 else 60
        w = 18 if k % 2 == 0 else 12
        tip = (cx + math.cos(a) * L, cy + math.sin(a) * L)
        lft = (cx + math.cos(a - math.pi / 2) * w, cy + math.sin(a - math.pi / 2) * w)
        rgt = (cx + math.cos(a + math.pi / 2) * w, cy + math.sin(a + math.pi / 2) * w)
        rose += (f'<path d="M{cx} {cy} L{lft[0]:.1f} {lft[1]:.1f} L{tip[0]:.1f} {tip[1]:.1f} Z" fill="#7a5a36"/>'
                 f'<path d="M{cx} {cy} L{rgt[0]:.1f} {rgt[1]:.1f} L{tip[0]:.1f} {tip[1]:.1f} Z" fill="#c9a46a"/>')
    rose += f'<circle cx="{cx}" cy="{cy}" r="10" fill="#c4513f"/>'
    rose += f'<path d="M{cx - 12} {cy - 136} L{cx - 12} {cy - 160} L{cx + 12} {cy - 136} L{cx + 12} {cy - 160}" stroke="#7a5a36" stroke-width="5" fill="none" stroke-linejoin="round"/>'
    d.add(f'<g opacity="0.4">{rose}</g>')
    # tiny decorative corner flourishes
    for (x, y, sx, sy) in [(90, 90, 1, 1), (1830, 90, -1, 1), (90, 990, 1, -1)]:
        d.add(f'<path d="M{x} {y + sy * 70} Q{x} {y} {x + sx * 70} {y} M{x + sx * 16} {y + sy * 50} Q{x + sx * 16} {y + sy * 16} {x + sx * 50} {y + sy * 16}" '
              f'stroke="#8a6a44" stroke-width="4" fill="none" opacity="0.35"/>')
    return d.svg(grain=0.1, vignette=0.3)


# --------------------------------------------------------------------------------------------
# Storeroom, cave pond and mosaic hall (sorting, skip-counting and jigsaw games)
# --------------------------------------------------------------------------------------------

def crate(x, y, w, h, wood="#b5804a", dark="#7a4e2a", face=False):
    """Wooden crate standing with its bottom-left corner at (x, y)."""
    t = y - h
    out = (f'<rect x="{x}" y="{t}" width="{w}" height="{h}" rx="5" fill="{wood}" stroke="{INK}" stroke-width="5" stroke-linejoin="round"/>'
           f'<rect x="{x + 8}" y="{t + 8}" width="{w - 16}" height="{h - 16}" rx="3" fill="none" stroke="{dark}" stroke-width="4" opacity="0.7"/>'
           f'<path d="M{x + 12} {t + 12} L{x + w - 12} {y - 12}" stroke="{dark}" stroke-width="12" stroke-linecap="round" opacity="0.8"/>'
           f'<path d="M{x + 12} {t + 12} L{x + w - 12} {y - 12}" stroke="{wood}" stroke-width="6" stroke-linecap="round"/>')
    for f in (0.36, 0.64):
        out += f'<path d="M{x + 6} {t + h * f:.0f} L{x + w - 6} {t + h * f:.0f}" stroke="{dark}" stroke-width="3" opacity="0.45"/>'
    for cx, cy in ((x + 14, t + 14), (x + w - 14, t + 14), (x + 14, y - 14), (x + w - 14, y - 14)):
        out += f'<circle cx="{cx}" cy="{cy}" r="3.5" fill="{INK}" opacity="0.55"/>'
    out += f'<path d="M{x + 8} {t + 6} L{x + w * 0.5:.0f} {t + 6}" stroke="#f0c88e" stroke-width="5" opacity="0.6" stroke-linecap="round"/>'
    if face:  # a goblin doodle chalked on the crate
        cx, cy = x + w * 0.5, t + h * 0.5
        out += (f'<g stroke="#f6ecd6" stroke-width="5" fill="none" stroke-linecap="round" opacity="0.85">'
                f'<circle cx="{cx:.0f}" cy="{cy:.0f}" r="{w * 0.17:.0f}"/>'
                f'<path d="M{cx - w * 0.17:.0f} {cy - 6:.0f} L{cx - w * 0.3:.0f} {cy - 18:.0f} M{cx + w * 0.17:.0f} {cy - 6:.0f} L{cx + w * 0.3:.0f} {cy - 18:.0f}"/>'
                f'<path d="M{cx - 10:.0f} {cy + 6:.0f} Q{cx:.0f} {cy + 16:.0f} {cx + 10:.0f} {cy + 6:.0f}"/></g>'
                f'<circle cx="{cx - 8:.0f}" cy="{cy - 5:.0f}" r="3.5" fill="#f6ecd6"/><circle cx="{cx + 8:.0f}" cy="{cy - 5:.0f}" r="3.5" fill="#f6ecd6"/>')
    return out


def sack(d, x, y, w, h, col="#d9bf8a", dark="#a8885a", tie="#8a5a33", lean=0, open_top=False):
    """A plump burlap sack sitting on y, centred on x."""
    l, rr, t = x - w / 2, x + w / 2, y - h
    g = d.lin([(0, col), (1, dark)], 0, 0, 1, 1)
    neck = w * 0.16
    body = (f'M{x - neck:.0f} {t + h * 0.18:.0f} C{l - w * 0.08:.0f} {t + h * 0.3:.0f} {l - w * 0.1:.0f} {y - 4:.0f} {l + w * 0.12:.0f} {y:.0f} '
            f'L{rr - w * 0.12:.0f} {y:.0f} C{rr + w * 0.1:.0f} {y - 4:.0f} {rr + w * 0.08:.0f} {t + h * 0.3:.0f} {x + neck:.0f} {t + h * 0.18:.0f} Z')
    out = f'<g transform="rotate({lean} {x} {y})"><path d="{body}" fill="{g}" stroke="{INK}" stroke-width="5" stroke-linejoin="round"/>'
    if open_top:
        out += (f'<ellipse cx="{x:.0f}" cy="{t + h * 0.16:.0f}" rx="{neck * 1.9:.0f}" ry="{neck * 0.6:.0f}" fill="{dark}" stroke="{INK}" stroke-width="4"/>'
                f'<ellipse cx="{x:.0f}" cy="{t + h * 0.17:.0f}" rx="{neck * 1.4:.0f}" ry="{neck * 0.38:.0f}" fill="#f2dca0"/>')
    else:
        out += (f'<path d="M{x - neck:.0f} {t + h * 0.18:.0f} L{x - neck * 1.6:.0f} {t:.0f} L{x + neck * 1.6:.0f} {t:.0f} L{x + neck:.0f} {t + h * 0.18:.0f} Z" fill="{col}" stroke="{INK}" stroke-width="4" stroke-linejoin="round"/>'
                f'<path d="M{x - neck * 1.2:.0f} {t + h * 0.17:.0f} L{x + neck * 1.2:.0f} {t + h * 0.17:.0f}" stroke="{tie}" stroke-width="8" stroke-linecap="round"/>')
    out += (f'<path d="M{l + w * 0.18:.0f} {t + h * 0.4:.0f} Q{l + w * 0.1:.0f} {t + h * 0.7:.0f} {l + w * 0.2:.0f} {y - 12:.0f}" stroke="#fff6dc" stroke-width="6" fill="none" opacity="0.35" stroke-linecap="round"/>'
            f'<path d="M{x - w * 0.05:.0f} {t + h * 0.45:.0f} L{x + w * 0.22:.0f} {t + h * 0.45:.0f} L{x + w * 0.22:.0f} {t + h * 0.68:.0f} L{x - w * 0.05:.0f} {t + h * 0.68:.0f} Z" '
            f'fill="{dark}" stroke="{INK}" stroke-width="3" stroke-dasharray="6 5" opacity="0.8"/></g>')
    return out


def apple(x, y, s=1.0, col="#e04a3a"):
    return (f'<path d="M{x:.0f} {y - 18 * s:.0f} C{x - 26 * s:.0f} {y - 30 * s:.0f} {x - 26 * s:.0f} {y + 12 * s:.0f} {x:.0f} {y + 12 * s:.0f} '
            f'C{x + 26 * s:.0f} {y + 12 * s:.0f} {x + 26 * s:.0f} {y - 30 * s:.0f} {x:.0f} {y - 18 * s:.0f} Z" fill="{col}" stroke="{INK}" stroke-width="{3.5 * s:.1f}"/>'
            f'<path d="M{x:.0f} {y - 18 * s:.0f} L{x + 3 * s:.0f} {y - 28 * s:.0f}" stroke="#6a4226" stroke-width="{3.5 * s:.1f}" stroke-linecap="round"/>'
            f'<path d="M{x + 3 * s:.0f} {y - 24 * s:.0f} q{10 * s:.0f} {-8 * s:.0f} {16 * s:.0f} {-2 * s:.0f} q{-8 * s:.0f} {6 * s:.0f} {-16 * s:.0f} {2 * s:.0f} Z" fill="#6aa84a"/>'
            f'<ellipse cx="{x - 8 * s:.0f}" cy="{y - 8 * s:.0f}" rx="{4 * s:.1f}" ry="{6 * s:.1f}" fill="#fff" opacity="0.5"/>')


def onion_string(r, x, y0, n, s=1.0):
    out = f'<path d="M{x} {y0} L{x} {y0 + n * 30 * s:.0f}" stroke="#c9a46a" stroke-width="4"/>'
    for k in range(n):
        y = y0 + 26 * s + k * 30 * s
        ox = x + (12 if k % 2 else -12) * s
        col = r.choice(["#e0a85a", "#c97a4a", "#f2d3a0"])
        out += (f'<path d="M{ox:.0f} {y - 22 * s:.0f} Q{ox - 20 * s:.0f} {y - 4 * s:.0f} {ox:.0f} {y + 12 * s:.0f} Q{ox + 20 * s:.0f} {y - 4 * s:.0f} {ox:.0f} {y - 22 * s:.0f} Z" '
                f'fill="{col}" stroke="{INK}" stroke-width="2.5" stroke-opacity="0.7"/>'
                f'<path d="M{ox - 4 * s:.0f} {y - 10 * s:.0f} Q{ox - 6 * s:.0f} {y:.0f} {ox - 2 * s:.0f} {y + 6 * s:.0f}" stroke="#fff6dc" stroke-width="2.5" fill="none" opacity="0.5"/>')
    return out


def scene_storeroom():
    d = Doc(114)
    r = d.r
    wall = d.lin([(0, "#5a3e2c"), (0.25, "#8a6244"), (0.75, "#9c7350"), (1, "#7e5a3e")])
    d.add(f'<rect width="{W}" height="{H}" fill="{wall}"/>')
    # plank wall, faded over the centre-right play area
    mask = d.fade_mask(1300, 620, 700, 470, 0.25)
    planks = ""
    x = 0
    while x < W:
        w = r.uniform(90, 140)
        planks += (f'<rect x="{x:.0f}" y="60" width="{w:.0f}" height="770" fill="{r.choice(["#8e6546", "#9a7050", "#83603f"])}"/>'
                   f'<path d="M{x:.0f} 60 L{x:.0f} 830" stroke="#5a3e2a" stroke-width="5"/>')
        for _ in range(2):
            ky = r.uniform(120, 780)
            planks += f'<ellipse cx="{x + w * r.uniform(0.3, 0.7):.0f}" cy="{ky:.0f}" rx="6" ry="10" fill="none" stroke="#5a3e2a" stroke-width="3" opacity="0.6"/>'
        x += w
    d.add(f'<g mask="{mask}" opacity="0.9">{planks}</g>')
    d.add(glow(d, 1300, 600, 760, "#ffe0b0", 0.22, ry=440))
    # ceiling beam with hanging onions and sausages (edges only)
    d.add(f'<rect x="0" y="0" width="{W}" height="64" fill="#4a3222" stroke="{INK}" stroke-width="5"/>'
          f'<path d="M0 56 L{W} 56" stroke="#7a5236" stroke-width="5" opacity="0.6"/>')
    for x, n in [(60, 6), (140, 4), (1610, 4), (1860, 6)]:
        d.add(onion_string(r, x, 64, n, 1.0))
    sx0, sx1 = 1660, 1800
    d.add(f'<path d="M{sx0} 64 Q{(sx0 + sx1) / 2} 120 {sx1} 64" stroke="#c9a46a" stroke-width="4" fill="none"/>')
    for k in range(5):
        t = (k + 0.5) / 5
        x = sx0 + (sx1 - sx0) * t
        y = 64 + 56 * 4 * t * (1 - t)
        d.add(f'<path d="M{x:.0f} {y:.0f} q-8 40 6 70" stroke="#a8432f" stroke-width="18" fill="none" stroke-linecap="round"/>'
              f'<path d="M{x:.0f} {y:.0f} q-8 40 6 70" stroke="#d8644a" stroke-width="9" fill="none" stroke-linecap="round" opacity="0.8"/>')
    # a hanging lantern on the left
    d.add(lantern(d, 300, 64, 240, 0.9, "#ffcf6a"))
    # wall shelves on the left, crammed with jars, pots and a cheese
    for y in (320, 470):
        d.add(f'<path d="M30 {y + 16} L50 {y + 60} M330 {y + 16} L310 {y + 60}" stroke="#5a3a22" stroke-width="10" stroke-linecap="round"/>')
        d.add(shelf_board(0, 380, y))
    for x, col, k, s in [(40, "#ffcf4a", 2, 0.9), (100, "#e07a5a", 1, 0.85), (160, "#7fd0a0", 0, 0.8)]:
        d.add(bottle(x, 320, s, col, k, "#f4e8d0"))
    d.add(f'<path d="M210 320 L210 268 Q250 250 290 268 L290 320 Z" fill="#f2c94c" stroke="{INK}" stroke-width="4" stroke-linejoin="round"/>'
          f'<path d="M210 268 Q250 284 290 268" stroke="{INK}" stroke-width="3" fill="none" opacity="0.6"/>'
          f'<circle cx="236" cy="296" r="7" fill="#d9a830"/><circle cx="268" cy="304" r="5" fill="#d9a830"/>')
    d.add(f'<path d="M300 320 L310 280 L360 280 L370 320 Z" fill="#c4703f" stroke="{INK}" stroke-width="4" stroke-linejoin="round"/>'
          f'<rect x="304" y="272" width="62" height="12" rx="5" fill="#a85a32" stroke="{INK}" stroke-width="3"/>')
    d.add(crate(24, 470, 110, 86))
    d.add(sack(d, 210, 470, 100, 110, open_top=True))
    for k, x in enumerate((196, 214, 230)):
        d.add(apple(x, 360 + (k % 2) * 6, 0.55, ["#e04a3a", "#7cc04a", "#f2b23a"][k]))
    d.add(bottle(310, 470, 0.9, "#b58cff", 1, "#f4e8d0"))
    # floor
    fl = d.lin([(0, "#9a6a44"), (1, "#5e3c26")])
    d.add(f'<rect x="0" y="830" width="{W}" height="250" fill="{fl}"/>')
    d.add(floor_planks(r, 830, 1080, "#8a5e3c", "#a87650", "#4a2e1e", 6))
    d.add(f'<rect x="0" y="822" width="{W}" height="14" fill="#4e3424"/>')
    d.add(glow(d, 1300, 880, 560, "#ffc87a", 0.22, ry=110))
    # the messy pile at the right edge: stacked crates, sacks and spilled apples
    d.add(f'<ellipse cx="1800" cy="1010" rx="220" ry="26" fill="#2e1e14" opacity="0.35"/>')
    d.add(crate(1700, 1010, 200, 190, face=True))
    d.add(crate(1740, 820, 180, 150, "#c08a52"))
    d.add(crate(1780, 670, 150, 120, "#a8743e"))
    d.add(barrel(1890, 1030, 120, 150, "#9a5f36"))
    d.add(f'<path d="M1650 860 L1690 640" stroke="{INK}" stroke-width="14" stroke-linecap="round"/><path d="M1650 860 L1690 640" stroke="#b07a4a" stroke-width="7" stroke-linecap="round"/>')
    d.add(sack(d, 1660, 1012, 130, 150, "#e0c890", "#b0905c", lean=-6))
    # spilled things along the bottom edge
    d.add(sack(d, 1520, 1100, 140, 120, lean=-70, open_top=True))
    for x, y in [(1400, 1062), (1372, 1048), (1340, 1066), (1300, 1056), (1426, 1040)]:
        d.add(f'<ellipse cx="{x}" cy="{y}" rx="9" ry="6" fill="#f2dca0" stroke="{INK}" stroke-width="2" stroke-opacity="0.5"/>')
    for x, y, col in [(1180, 1058, "#e04a3a"), (1050, 1070, "#7cc04a"), (1880, 1050, "#e04a3a"), (60, 1060, "#f2b23a")]:
        d.add(apple(x, y, 0.9, col))
    # a dropped spoon and a cork near the left corner
    d.add(f'<path d="M120 1050 L230 1032" stroke="{INK}" stroke-width="12" stroke-linecap="round"/>'
          f'<path d="M120 1050 L230 1032" stroke="#c9a46a" stroke-width="6" stroke-linecap="round"/>'
          f'<ellipse cx="246" cy="1029" rx="22" ry="13" fill="#c9a46a" stroke="{INK}" stroke-width="4" transform="rotate(-10 246 1029)"/>')
    d.add(motes(r, 22, 0, W, 120, 820, "#ffe6b0", 1.5, 3, avoid=(740, 230, 1880, 1030)))
    return d.svg(vignette=0.3)


def lily_pad(d, x, y, rx, flower=False, rot=0):
    ry = rx * 0.42
    g = d.rad([(0, "#9ad86a"), (0.7, "#5fae45"), (1, "#3f8a3a")], cx=0.45, cy=0.4)
    a0, a1 = math.radians(rot - 18), math.radians(rot + 18)
    p0 = (x + math.cos(a0) * rx, y + math.sin(a0) * ry)
    p1 = (x + math.cos(a1) * rx, y + math.sin(a1) * ry)
    out = (f'<ellipse cx="{x}" cy="{y + 6}" rx="{rx + 4}" ry="{ry + 3}" fill="#0e3a3a" opacity="0.3"/>'
           f'<path d="M{x} {y} L{p0[0]:.0f} {p0[1]:.0f} A{rx} {ry:.0f} 0 1 0 {p1[0]:.0f} {p1[1]:.0f} Z" fill="{g}" stroke="#24502a" stroke-width="4" stroke-linejoin="round"/>')
    for k in range(5):
        a = math.radians(rot + 50 + k * 52)
        out += f'<path d="M{x} {y} L{x + math.cos(a) * rx * 0.75:.0f} {y + math.sin(a) * ry * 0.75:.0f}" stroke="#3f8a3a" stroke-width="3" opacity="0.6"/>'
    if flower:
        fx, fy = x - rx * 0.3, y - ry * 0.2
        for k in range(7):
            a = -math.pi / 2 + (k - 3) * 0.42
            out += (f'<ellipse cx="{fx + math.cos(a) * 14:.0f}" cy="{fy + math.sin(a) * 14:.0f}" rx="8" ry="17" fill="#ffb0cf" stroke="#c4508f" stroke-width="2.5" '
                    f'transform="rotate({math.degrees(a) + 90:.0f} {fx + math.cos(a) * 14:.0f} {fy + math.sin(a) * 14:.0f})"/>')
        out += f'<circle cx="{fx:.0f}" cy="{fy:.0f}" r="7" fill="#ffd34d" stroke="#c98a22" stroke-width="2"/>'
    return out


def frog(x, y, s=1.0, flip=False):
    """A friendly round frog sitting with its bottom centre at (x, y)."""
    tf = f'translate({x} {y}) scale({-s if flip else s} {s})'
    o = "#24451c"
    return (f'<g transform="{tf}">'
            f'<ellipse cx="0" cy="2" rx="62" ry="12" fill="#0e2a2a" opacity="0.3"/>'
            f'<path d="M-58 -4 C-70 -14 -60 -34 -40 -30 L-30 -4 Z M58 -4 C70 -14 60 -34 40 -30 L30 -4 Z" fill="#5fae45" stroke="{o}" stroke-width="4" stroke-linejoin="round"/>'
            f'<path d="M-48 0 C-56 -50 -36 -78 0 -78 C36 -78 56 -50 48 0 Z" fill="#7cc75a" stroke="{o}" stroke-width="5" stroke-linejoin="round"/>'
            f'<path d="M-26 -2 C-30 -30 -16 -44 0 -44 C16 -44 30 -30 26 -2 Z" fill="#e8f6b0"/>'
            f'<circle cx="-24" cy="-80" r="18" fill="#7cc75a" stroke="{o}" stroke-width="5"/><circle cx="24" cy="-80" r="18" fill="#7cc75a" stroke="{o}" stroke-width="5"/>'
            f'<circle cx="-24" cy="-80" r="11" fill="#fff"/><circle cx="24" cy="-80" r="11" fill="#fff"/>'
            f'<circle cx="-21" cy="-79" r="6.5" fill="#1a1a10"/><circle cx="27" cy="-79" r="6.5" fill="#1a1a10"/>'
            f'<circle cx="-19" cy="-82" r="2.4" fill="#fff"/><circle cx="29" cy="-82" r="2.4" fill="#fff"/>'
            f'<path d="M-30 -54 Q0 -34 30 -54" stroke="{o}" stroke-width="4.5" fill="none" stroke-linecap="round"/>'
            f'<ellipse cx="-34" cy="-50" rx="8" ry="5" fill="#ff9aa8" opacity="0.7"/><ellipse cx="34" cy="-50" rx="8" ry="5" fill="#ff9aa8" opacity="0.7"/>'
            f'<path d="M-30 0 C-30 -14 -20 -18 -14 -10 L-10 0 Z M30 0 C30 -14 20 -18 14 -10 L10 0 Z" fill="#5fae45" stroke="{o}" stroke-width="3.5" stroke-linejoin="round"/>'
            f'<ellipse cx="-30" cy="-64" rx="8" ry="5" fill="#fff" opacity="0.35"/></g>')


def cattails(r, x, y, n, spread=60, h=200):
    out = ""
    for k in range(n):
        bx = x + r.uniform(-spread, spread)
        hh = h * r.uniform(0.7, 1.1)
        lean = r.uniform(-30, 30)
        out += f'<path d="M{bx:.0f} {y} Q{bx + lean * 0.3:.0f} {y - hh * 0.5:.0f} {bx + lean:.0f} {y - hh:.0f}" stroke="#4f8a3a" stroke-width="6" fill="none" stroke-linecap="round"/>'
        if k % 2 == 0:
            tx, ty = bx + lean * 0.9, y - hh * 0.9
            out += f'<rect x="{tx - 9:.0f}" y="{ty - 10:.0f}" width="18" height="52" rx="9" fill="#8a5a33" stroke="{INK}" stroke-width="3" transform="rotate({lean * 0.4:.0f} {tx:.0f} {ty:.0f})"/>'
        else:
            out += (f'<path d="M{bx:.0f} {y} Q{bx + lean:.0f} {y - hh * 0.6:.0f} {bx + lean * 1.6 + 20:.0f} {y - hh * 0.75:.0f} Q{bx + lean * 0.5 + 6:.0f} {y - hh * 0.4:.0f} {bx + 10:.0f} {y} Z" '
                    f'fill="#6aa84a" stroke="#3f7a35" stroke-width="2"/>')
    return out


def scene_pond():
    d = Doc(115)
    r = d.r
    bg = d.rad([(0, "#5aa59c"), (0.55, "#3a7a80"), (1, "#1e4552")], cx=0.62, cy=0.5, r=0.72)
    d.add(f'<rect width="{W}" height="{H}" fill="{bg}"/>')
    mask = d.fade_mask(1300, 560, 700, 420, 0.15)
    lumps = ""
    for _ in range(30):
        x, y = r.uniform(0, W), r.uniform(80, 760)
        lumps += f'<path d="{smooth(blob_pts(r, x, y, r.uniform(80, 200), r.uniform(50, 120), 9, 0.2))}" fill="{r.choice(["#3a7276", "#46848a", "#326468"])}" opacity="0.6"/>'
    d.add(f'<g mask="{mask}">{lumps}</g>')
    d.add(glow(d, 1280, 560, 720, "#c8fff0", 0.22, ry=420))
    # cave frame and stalactites
    frame = d.lin([(0, "#1a3a44"), (1, "#24505a")])
    d.add(f'<path d="M0 0 L{W} 0 L{W} {H} L0 {H} Z M40 900 Q10 520 110 270 Q300 80 700 60 Q1200 40 1620 90 Q1880 180 1890 470 Q1900 700 1890 900 Z" '
          f'fill="{frame}" fill-rule="evenodd"/>')
    d.add(stalactites(r, 0, 420, 50, 60, 190, "#1a3a44", "#0e2228"))
    d.add(stalactites(r, 1540, 1920, 70, 60, 190, "#1a3a44", "#0e2228"))
    d.add(stalactites(r, 420, 1540, 40, 30, 66, "#1a3a44", "#0e2228", (40, 80)))
    # a thin waterfall at the right edge
    fall = d.lin([(0, "#bff4ff", 0.9), (1, "#7fd8e8", 0.8)])
    d.add(f'<path d="M1880 150 Q1870 400 1872 760 L1920 760 L1920 150 Z" fill="{fall}" stroke="#2a6a78" stroke-width="4"/>')
    for k in range(4):
        x = 1884 + k * 9
        d.add(f'<path d="M{x} 170 Q{x - 6} 450 {x - 2} 750" stroke="#ffffff" stroke-width="3" fill="none" opacity="0.5"/>')
    # glowing mushrooms on the left wall ledge, with a frog sitting there
    ledge = smooth([(-20, 520), (120, 486), (300, 478), (380, 500), (360, 560), (200, 580), (-20, 590)])
    d.add(f'<path d="{ledge}" fill="#2e5a5a" stroke="#0e2228" stroke-width="5"/>')
    d.add(f'<path d="M20 500 Q160 470 330 486" stroke="#6fbf7a" stroke-width="12" fill="none" stroke-linecap="round" opacity="0.8"/>')
    for x, y, s in [(40, 500, 0.9), (90, 492, 0.6), (330, 490, 0.75)]:
        d.add(mushroom(d, x, y, s, "#7ff0e0", "#d8f0ee", 0.4))
    d.add(frog(210, 484, 0.75))
    for x, y, s in [(1700, 140, 0.8), (1760, 160, 0.6)]:
        d.add(mushroom(d, x, y, s, "#b8ffcf", "#e0fff0", 0.35))
    # the shore the hero stands on (left) and the pond (centre-right)
    shore = d.lin([(0, "#6a8a6a"), (0.3, "#4f6e56"), (1, "#2e4438")])
    d.add(f'<path d="M-20 800 Q500 780 760 800 Q1300 820 1940 790 L1940 1100 L-20 1100 Z" fill="{shore}"/>')
    water = d.lin([(0, "#3f9aa8"), (0.5, "#2a7a8e"), (1, "#1a5470")])
    pond = smooth([(700, 1100), (720, 960), (800, 860), (980, 822), (1400, 818), (1800, 812), (1960, 808), (1960, 1100)], closed=False)
    d.add(f'<path d="{pond} Z" fill="{water}" stroke="#163a40" stroke-width="6"/>')
    d.add(f'<path d="M790 870 Q900 832 1100 826 Q1500 822 1920 816" stroke="#a8f0f0" stroke-width="5" fill="none" opacity="0.5"/>')
    d.add(f'<path d="M-20 800 Q500 780 760 800" stroke="#9ac89a" stroke-width="6" fill="none" opacity="0.5"/>')
    d.add(grass_blades(r, 0, 700, 790, 840, 70, ["#5f9a5a", "#78b06a", "#4a7a4a"], 10, 26))
    # soft reflections and ripples on the water
    d.add(glow(d, 1280, 900, 520, "#c8fff0", 0.25, ry=70))
    for _ in range(14):
        x, y = r.uniform(820, 1900), r.uniform(860, 1060)
        w = r.uniform(40, 110)
        d.add(f'<path d="M{x - w:.0f} {y:.0f} Q{x:.0f} {y - 8:.0f} {x + w:.0f} {y:.0f}" stroke="#bff4ff" stroke-width="3" fill="none" opacity="0.35" stroke-linecap="round"/>')
    # lily pads and stepping stones round the pond's edges
    for x, y, rx in [(760, 1050, 50), (860, 1070, 44)]:
        d.add(f'<ellipse cx="{x}" cy="{y + 6}" rx="{rx + 4}" ry="{rx * 0.36 + 3:.0f}" fill="#0e3a3a" opacity="0.3"/>'
              f'<ellipse cx="{x}" cy="{y}" rx="{rx}" ry="{rx * 0.36:.0f}" fill="#9a9a8a" stroke="#3a3a30" stroke-width="4"/>'
              f'<ellipse cx="{x - 8}" cy="{y - 4}" rx="{rx * 0.6:.0f}" ry="{rx * 0.16:.0f}" fill="#c8c8b8" opacity="0.7"/>')
    d.add(lily_pad(d, 1040, 1066, 70, rot=-30))
    d.add(lily_pad(d, 1880, 880, 60, True, rot=200))
    d.add(lily_pad(d, 1820, 1056, 96, rot=-60))
    d.add(frog(1820, 1056, 0.85, flip=True))
    d.add(lily_pad(d, 1560, 1070, 54, True, rot=30))
    # cattails at both edges
    d.add(cattails(r, 30, 860, 6, 40, 220))
    d.add(cattails(r, 1900, 830, 4, 20, 200))
    # little stones and pebbles on the shore edge
    for _ in range(10):
        x, y = r.uniform(0, 680), r.uniform(1010, 1075)
        d.add(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{r.uniform(10, 22):.0f}" ry="{r.uniform(6, 10):.0f}" fill="{r.choice(["#7a8a7a", "#8a9a88", "#5e6e60"])}" stroke="#2e3a30" stroke-width="2" stroke-opacity="0.5"/>')
    # fireflies
    for _ in range(26):
        x, y = r.uniform(0, W), r.uniform(120, 1000)
        if 740 < x < 1880 and 240 < y < 1020:
            continue
        d.add(glow(d, x, y, 22, "#f8ff9a", 0.6) + f'<circle cx="{x:.0f}" cy="{y:.0f}" r="3.5" fill="#fbffc8"/>')
    d.add(motes(r, 14, 760, 1860, 260, 780, "#e0fff0", 1.5, 2.5))
    return d.svg(vignette=0.32, vig_color="#06161c")


MOSAIC_GROUT = "#4a3a30"


def mosaic_panel(d, r, x0, y0, w, h, tile=24, missing=0.1, hole=None):
    """An arched picture mosaic (sun, sky, hills and a little tree) with some tiles missing.
    hole = (cx, cy, radius) in panel fractions knocks out a cluster of tiles."""
    out = ""
    rx = w / 2
    spring = y0 + rx
    cx = x0 + rx
    frame = d.lin([(0, "#e8d2a0"), (1, "#a8865a")], 0, 0, 1, 1)
    out += f'<path d="{arch_path(x0 - 26, x0 + w + 26, spring, y0 + h + 26)}" fill="{frame}" stroke="{INK}" stroke-width="6"/>'
    out += f'<path d="{arch_path(x0, x0 + w, spring, y0 + h)}" fill="{MOSAIC_GROUT}"/>'
    fallen = []
    ny, nx = int(h / tile) + 1, int(w / tile) + 1
    for j in range(ny):
        for i in range(nx):
            tx, ty = x0 + i * tile, y0 + j * tile
            mx, my = tx + tile / 2, ty + tile / 2
            if mx > x0 + w - 2 or my > y0 + h - 2:
                continue
            if my < spring and (mx - cx) ** 2 + (my - spring) ** 2 > (rx - tile * 0.45) ** 2:
                continue
            u, v = (mx - x0) / w, (my - y0) / h
            gone = r.random() < missing
            if hole and (u - hole[0]) ** 2 + ((v - hole[1]) * h / w) ** 2 < hole[2] ** 2:
                gone = r.random() < 0.85
            if gone:
                fallen.append(u)
                continue
            # the picture
            hill1 = 0.72 + 0.06 * math.sin(u * 5.0 + 0.6)
            hill2 = 0.8 + 0.05 * math.sin(u * 7.0 + 2.0)
            su, sv = 0.66, 0.3
            dsun = math.hypot(u - su, (v - sv) * h / w)
            if v > hill2:
                col = r.choice(["#4f9a4a", "#5aa852", "#468a40"])
            elif v > hill1:
                col = r.choice(["#78c060", "#86cc6a", "#6ab456"])
            elif 0.24 < u < 0.3 and 0.52 < v <= hill1:
                col = r.choice(["#8a5a33", "#7a4e2a"])
            elif math.hypot(u - 0.27, (v - 0.48) * h / w) < 0.12:
                col = r.choice(["#3f8a3a", "#4f9a44", "#367a34"])
            elif dsun < 0.13:
                col = r.choice(["#ffd34d", "#ffdf6a", "#f8c838"])
            elif dsun < 0.19:
                col = r.choice(["#ffb04a", "#ffa03a"])
            else:
                col = r.choice(["#7fc8f0", "#8fd2f4", "#72bce8"]) if v > 0.3 else r.choice(["#5aa8e0", "#66b2e6", "#4f9ad6"])
            jx, jy = r.uniform(-1.5, 1.5), r.uniform(-1.5, 1.5)
            out += f'<rect x="{tx + 2 + jx:.1f}" y="{ty + 2 + jy:.1f}" width="{tile - 4}" height="{tile - 4}" rx="3" fill="{col}"/>'
    # a soft shine across the panel
    out += f'<path d="{arch_path(x0, x0 + w, spring, y0 + h)}" fill="#ffffff" opacity="0.08"/>'
    out += f'<path d="M{x0 + w * 0.12:.0f} {y0 + h * 0.2:.0f} L{x0 + w * 0.3:.0f} {y0 + h * 0.2:.0f} L{x0 + w * 0.12:.0f} {y0 + h * 0.6:.0f} Z" fill="#ffffff" opacity="0.1"/>'
    # ledge under the panel
    out += f'<rect x="{x0 - 44}" y="{y0 + h + 20}" width="{w + 88}" height="22" rx="8" fill="{frame}" stroke="{INK}" stroke-width="5"/>'
    return out, fallen


def scene_mosaic_hall():
    d = Doc(116)
    r = d.r
    wall = d.lin([(0, "#7a5e48"), (0.3, "#b0906c"), (0.75, "#c4a47c"), (1, "#a88a66")])
    d.add(f'<rect width="{W}" height="{H}" fill="{wall}"/>')
    mask = d.fade_mask(1290, 600, 760, 500, 0.1)
    d.add(f'<g mask="{mask}">{stone_blocks(r, 0, 120, W, 820, 160, 84, "#b8976e", "#c8a87e", "#a4855e", 0.6)}</g>')
    d.add(glow(d, 1290, 600, 720, "#fff0d0", 0.3, ry=500))
    # coffered ceiling band with a mosaic frieze
    d.add(f'<rect x="0" y="0" width="{W}" height="96" fill="#6a4e3a"/>')
    frieze = ""
    cols = ["#c4513f", "#e0a83a", "#3f8ab5", "#f6e8c4"]
    for k in range(0, W // 32 + 1):
        x = k * 32
        frieze += f'<rect x="{x + 3}" y="64" width="26" height="26" rx="3" fill="{cols[k % 4]}" opacity="{0.55 if 380 < x < 1540 else 0.95}"/>'
    d.add(frieze)
    d.add(f'<rect x="0" y="92" width="{W}" height="12" fill="#e8d2a0" stroke="{INK}" stroke-width="3" stroke-opacity="0.5"/>')
    # the big broken mosaic on the left wall, and a smaller one on the right
    panel, fallen = mosaic_panel(d, r, 50, 170, 300, 420, 25, 0.06, hole=(0.62, 0.55, 0.22))
    d.add(panel)
    panel2, fallen2 = mosaic_panel(d, r, 1700, 190, 200, 380, 25, 0.05, hole=(0.35, 0.3, 0.2))
    d.add(panel2)
    # pillars framing the hall
    d.add(pillar(d, 450, 80, 104, 840, "#c4a47c", "#e8d2a8", "#8a6a4a"))
    d.add(pillar(d, 1600, 80, 104, 840, "#c4a47c", "#e8d2a8", "#8a6a4a"))
    for x in (450, 1600):
        d.add(torch(d, x, 400, 0.9))
    # floor: big calm stone slabs with a mosaic border strip
    fl = d.lin([(0, "#b89a74"), (0.3, "#a4855e"), (1, "#7a5e44")])
    d.add(f'<rect x="0" y="830" width="{W}" height="250" fill="{fl}"/>')
    d.add(f'<rect x="0" y="822" width="{W}" height="16" fill="#e8d2a0"/>')
    for k in range(1, 5):
        y = 830 + 250 * (k / 5) ** 1.4
        d.add(f'<path d="M0 {y:.0f} L{W} {y:.0f}" stroke="#6a4e3a" stroke-width="3" opacity="0.35"/>')
    for k in range(-8, 9):
        d.add(f'<path d="M{960 + k * 160} 838 L{960 + k * 440} 1080" stroke="#6a4e3a" stroke-width="3" opacity="0.3"/>')
    strip = ""
    for k in range(W // 36 + 1):
        strip += f'<rect x="{k * 36 + 3}" y="1044" width="30" height="30" rx="4" fill="{cols[(k * 3) % 4]}" opacity="0.9"/>'
    d.add(f'<rect x="0" y="1038" width="{W}" height="42" fill="{MOSAIC_GROUT}"/>{strip}')
    d.add(glow(d, 1290, 860, 600, "#fff0d0", 0.22, ry=100))
    # fallen tiles on the floor below each panel
    tile_cols = ["#ffd34d", "#7fc8f0", "#78c060", "#5aa8e0", "#ffb04a"]
    for _ in range(9):
        x, y = r.uniform(30, 380), r.uniform(860, 920)
        a = r.uniform(-40, 40)
        d.add(f'<rect x="{x - 12:.0f}" y="{y - 8:.0f}" width="24" height="16" rx="3" fill="{r.choice(tile_cols)}" stroke="{INK}" stroke-width="2.5" transform="rotate({a:.0f} {x:.0f} {y:.0f})"/>')
    for _ in range(5):
        x, y = r.uniform(1700, 1910), r.uniform(1000, 1030)
        a = r.uniform(-40, 40)
        d.add(f'<rect x="{x - 12:.0f}" y="{y - 8:.0f}" width="24" height="16" rx="3" fill="{r.choice(tile_cols)}" stroke="{INK}" stroke-width="2.5" transform="rotate({a:.0f} {x:.0f} {y:.0f})"/>')
    # a little bucket of spare tiles in the right corner
    d.add(f'<path d="M1800 1030 L1790 950 L1890 950 L1880 1030 Z" fill="#8a6a4a" stroke="{INK}" stroke-width="5" stroke-linejoin="round"/>'
          f'<ellipse cx="1840" cy="950" rx="50" ry="12" fill="#5a3e2a" stroke="{INK}" stroke-width="4"/>'
          f'<path d="M1792 950 Q1840 880 1888 950" stroke="{INK}" stroke-width="5" fill="none"/>')
    for k, col in enumerate(tile_cols):
        d.add(f'<rect x="{1806 + k * 14}" y="{936 + (k % 2) * 4}" width="16" height="12" rx="2" fill="{col}" stroke="{INK}" stroke-width="2"/>')
    for _ in range(14):
        x, y = r.uniform(0, W), r.uniform(120, 1030)
        if 700 < x < 1880 and 240 < y < 1030:
            continue
        d.add(sparkle(x, y, r.uniform(6, 12), "#fff6e0", 0.7))
    d.add(motes(r, 20, 0, W, 120, 820, "#fff0c8", 1.5, 3, avoid=(740, 230, 1880, 1030)))
    return d.svg(vignette=0.28)


def scenes() -> dict:
    return {
        "scene_camp": scene_camp(),
        "scene_gate": scene_gate(),
        "scene_rune_hall": scene_rune_hall(),
        "scene_bridge": scene_bridge(),
        "scene_crystal_cave": scene_crystal_cave(),
        "scene_library": scene_library(),
        "scene_tunnel": scene_tunnel(),
        "scene_mirror_hall": scene_mirror_hall(),
        "scene_vault": scene_vault(),
        "scene_goblin_den": scene_goblin_den(),
        "scene_workshop": scene_workshop(),
        "scene_belfry": scene_belfry(),
        "scene_lair": scene_lair(),
        "scene_map": scene_map(),
        "scene_storeroom": scene_storeroom(),
        "scene_pond": scene_pond(),
        "scene_mosaic_hall": scene_mosaic_hall(),
    }
