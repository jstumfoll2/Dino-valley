"""Storybook backgrounds for Dino Valley, drawn procedurally at 1920×1080.

Each scene is layered like a picture book: sky, far mountains, the volcano, hills, then a
detailed foreground (grass blades, flowers, ferns). A fixed seed per scene keeps the art stable
between renders. Backgrounds are rendered to compressed images; characters stay separate layers.
"""
import math
import random

W, H = 1920, 1080
INK = "#3d2a1c"


def _sky(top, bottom, gid="sky"):
    return (f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1">'
            f'<stop offset="0" stop-color="{top}"/><stop offset="1" stop-color="{bottom}"/></linearGradient></defs>'
            f'<rect width="{W}" height="{H}" fill="url(#{gid})"/>')


def _sun(x, y, r):
    return (f'<defs><radialGradient id="sunGlow"><stop offset="0" stop-color="#fff6c4" stop-opacity="0.9"/>'
            f'<stop offset="1" stop-color="#fff6c4" stop-opacity="0"/></radialGradient></defs>'
            f'<circle cx="{x}" cy="{y}" r="{r*3}" fill="url(#sunGlow)"/>'
            f'<circle cx="{x}" cy="{y}" r="{r}" fill="#ffe27a" stroke="#f5b942" stroke-width="6"/>')


def _cloud(x, y, s, fill="#ffffff", shade="#dbe9f5", opacity=1.0):
    puffs = [(-60, 10, 46), (-15, -18, 60), (40, -6, 52), (85, 14, 38), (10, 22, 50)]
    under = "".join(f'<circle cx="{x+dx*s:.0f}" cy="{y+dy*s+10*s:.0f}" r="{r*s:.0f}" fill="{shade}"/>' for dx, dy, r in puffs)
    over = "".join(f'<circle cx="{x+dx*s:.0f}" cy="{y+dy*s:.0f}" r="{r*s:.0f}" fill="{fill}"/>' for dx, dy, r in puffs)
    return f'<g opacity="{opacity}">{under}{over}</g>'


def _ridge(r, y0, amp, color, points=14, jag=0.0):
    pts = [(0, H)]
    for i in range(points + 1):
        x = W * i / points
        y = y0 - amp * (0.5 + 0.5 * math.sin(i * 1.3 + r.uniform(0, 1))) - r.uniform(0, jag)
        pts.append((x, y))
    pts.append((W, H))
    d = f"M{pts[0][0]} {pts[0][1]} " + " ".join(
        f"Q{(pts[i][0] + pts[i+1][0]) / 2:.0f} {pts[i][1]:.0f} {pts[i+1][0]:.0f} {pts[i+1][1]:.0f}" for i in range(len(pts) - 1)
    ) + " Z"
    return f'<path d="{d}" fill="{color}"/>'


def _hill(cx, cy, rx, ry, top, bottom, gid):
    return (f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{top}"/>'
            f'<stop offset="1" stop-color="{bottom}"/></linearGradient></defs>'
            f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="url(#{gid})"/>')


def _volcano(x, base, h, w, night=False):
    body = "#8d6a7a" if night else "#b98a6e"
    dark = "#5d4458" if night else "#9a6f57"
    smoke = "".join(
        f'<circle cx="{x + i*22 + 10*math.sin(i):.0f}" cy="{base - h - 40 - i*55:.0f}" r="{28 + i*12}" fill="#f4f1ee" opacity="{0.85 - i*0.15:.2f}"/>'
        for i in range(5)
    )
    return (f'<path d="M{x-w} {base} L{x-46} {base-h} Q{x} {base-h-14} {x+46} {base-h} L{x+w} {base} Z" fill="{body}" stroke="{INK}" stroke-width="5" stroke-opacity="0.4"/>'
            f'<path d="M{x-46} {base-h} L{x-w*0.4:.0f} {base} L{x-w} {base} Z" fill="{dark}" opacity="0.6"/>'
            f'<path d="M{x-40} {base-h+4} Q{x-20} {base-h+60} {x-30} {base-h+110} Q{x} {base-h+70} {x+8} {base-h+30} Q{x+20} {base-h+80} {x+38} {base-h+4} Z" fill="#ff8a3d"/>'
            f'{smoke}')


def _palm(x, y, s, r):
    trunk = (f'<path d="M{x} {y} C{x+10*s} {y-120*s} {x-20*s} {y-240*s} {x+20*s} {y-330*s}" stroke="#8a5a2b" '
             f'stroke-width="{26*s:.0f}" fill="none" stroke-linecap="round"/>')
    rings = "".join(f'<path d="M{x-12*s+ (i%2)*6*s:.0f} {y-i*36*s:.0f} q{14*s:.0f} {-8*s:.0f} {26*s:.0f} 0" stroke="#6f4520" stroke-width="{5*s:.0f}" fill="none"/>' for i in range(1, 9))
    tx, ty = x + 20 * s, y - 330 * s
    fronds = ""
    for a in range(0, 360, 45):
        rad = math.radians(a + r.uniform(-10, 10))
        ex, ey = tx + math.cos(rad) * 200 * s, ty + math.sin(rad) * 120 * s + 60 * s
        cx, cy = tx + math.cos(rad) * 110 * s, ty + math.sin(rad) * 60 * s - 60 * s
        fronds += (f'<path d="M{tx:.0f} {ty:.0f} Q{cx:.0f} {cy:.0f} {ex:.0f} {ey:.0f} Q{cx+10*s:.0f} {cy+40*s:.0f} {tx:.0f} {ty:.0f} Z" '
                   f'fill="{r.choice(["#3f9a3a", "#4fae45", "#5fc152"])}" stroke="#1f5a22" stroke-width="{4*s:.0f}"/>')
    return trunk + rings + fronds


def _fern(x, y, s, r, color="#4fae45", stroke="#1f5a22"):
    out = ""
    for k in range(5):
        ang = math.radians(-150 + k * 30 + r.uniform(-8, 8))
        L = (160 + r.uniform(-30, 40)) * s
        ex, ey = x + math.cos(ang) * L, y + math.sin(ang) * L
        out += f'<path d="M{x:.0f} {y:.0f} Q{(x+ex)/2:.0f} {min(y,ey)-40*s:.0f} {ex:.0f} {ey:.0f}" stroke="{stroke}" stroke-width="{5*s:.0f}" fill="none"/>'
        for j in range(1, 9):
            t = j / 9
            px, py = x + (ex - x) * t, y + (ey - y) * t - 40 * s * math.sin(math.pi * t)
            ln = (34 - j * 2.6) * s
            for side in (-1, 1):
                a2 = ang + side * 1.1
                out += (f'<ellipse cx="{px + math.cos(a2)*ln/2:.0f}" cy="{py + math.sin(a2)*ln/2:.0f}" rx="{ln/2:.0f}" ry="{ln/5:.0f}" '
                        f'fill="{color}" transform="rotate({math.degrees(a2):.0f} {px + math.cos(a2)*ln/2:.0f} {py + math.sin(a2)*ln/2:.0f})"/>')
    return out


def _grass_blades(r, y0, y1, n, colors):
    out = []
    for _ in range(n):
        x = r.uniform(0, W)
        y = r.uniform(y0, y1)
        h = r.uniform(18, 46) * (0.6 + (y - y0) / (y1 - y0 + 1))
        lean = r.uniform(-12, 12)
        out.append(f'<path d="M{x:.0f} {y:.0f} q{lean/2:.0f} {-h/2:.0f} {lean:.0f} {-h:.0f} q{-lean/3:.0f} {h/2:.0f} {6:.0f} {h:.0f} Z" fill="{r.choice(colors)}"/>')
    return "".join(out)


def _flower(x, y, s, color, r):
    petals = "".join(
        f'<ellipse cx="{x + math.cos(math.radians(a))*9*s:.0f}" cy="{y + math.sin(math.radians(a))*9*s:.0f}" rx="{8*s:.0f}" ry="{5*s:.0f}" '
        f'fill="{color}" transform="rotate({a} {x + math.cos(math.radians(a))*9*s:.0f} {y + math.sin(math.radians(a))*9*s:.0f})"/>'
        for a in range(0, 360, 72)
    )
    return (f'<path d="M{x:.0f} {y+6*s:.0f} q{r.uniform(-6,6):.0f} {20*s:.0f} 0 {40*s:.0f}" stroke="#3f8a35" stroke-width="{3*s:.0f}" fill="none"/>'
            f'{petals}<circle cx="{x:.0f}" cy="{y:.0f}" r="{5*s:.0f}" fill="#ffd23f" stroke="#c98a12" stroke-width="{1.5*s:.1f}"/>')


def _ground(top_y, top, bottom, gid):
    return (f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{top}"/>'
            f'<stop offset="1" stop-color="{bottom}"/></linearGradient></defs>'
            f'<path d="M0 {top_y} Q{W*0.25} {top_y-30} {W*0.5} {top_y} T{W} {top_y} L{W} {H} L0 {H} Z" fill="url(#{gid})"/>')


def _paper_texture(opacity=0.08):
    """A soft printed-paper grain so scenes read like book pages rather than flat vector."""
    return (f'<defs><filter id="grain" x="0" y="0" width="100%" height="100%">'
            f'<feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" seed="3"/>'
            f'<feColorMatrix values="0 0 0 0 0.3  0 0 0 0 0.2  0 0 0 0 0.1  0 0 0 1.2 -0.45"/></filter></defs>'
            f'<rect width="{W}" height="{H}" filter="url(#grain)" opacity="{opacity}"/>')


def _vignette(color="#000", opacity=0.25):
    return (f'<defs><radialGradient id="vig" cx="0.5" cy="0.5" r="0.75"><stop offset="0.6" stop-color="{color}" stop-opacity="0"/>'
            f'<stop offset="1" stop-color="{color}" stop-opacity="{opacity}"/></radialGradient></defs>'
            f'<rect width="{W}" height="{H}" fill="url(#vig)"/>')


def _svg(body):
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">{body}</svg>'


def valley(night=False):
    r = random.Random(1)
    parts = []
    if night:
        parts.append(_sky("#1b2346", "#4a4d7a"))
        parts += [_cloud(r.uniform(100, 1800), r.uniform(80, 330), r.uniform(1.6, 2.6), "#5d6390", "#3c4170") for _ in range(9)]
        parts.append('<path d="M1180 120 L1120 300 L1180 300 L1110 480 L1260 260 L1195 260 L1260 120 Z" fill="#fff6a8" stroke="#ffd23f" stroke-width="6"/>')
    else:
        parts.append(_sky("#9fdcff", "#fff3d6"))
        parts.append(_sun(230, 170, 70))
        parts += [_cloud(x, y, s) for x, y, s in [(620, 160, 1.3), (1080, 110, 1.0), (1560, 200, 1.5)]]
    parts.append(_ridge(r, 600, 140, "#b9a6d9" if not night else "#4c4a7a", jag=40))
    parts.append(_volcano(1460, 640, 300, 300, night))
    parts.append(_hill(420, 760, 700, 240, "#a8dc8a" if not night else "#3f6a55", "#86c66b" if not night else "#2f5546", "h1"))
    parts.append(_hill(1500, 790, 760, 230, "#9bd57e" if not night else "#3a6450", "#7cbf62" if not night else "#2b4f41", "h2"))
    parts.append(_palm(180, 860, 1.25, r))
    parts.append(_palm(1790, 840, 1.0, r))
    parts.append(_ground(780, "#8fd16a" if not night else "#355a44", "#5fae45" if not night else "#24402f", "g1"))
    parts.append(_grass_blades(r, 790, 1080, 900, ["#5fae45", "#4f9e3a", "#7cc85a"] if not night else ["#2f5546", "#3a6450"]))
    parts += [_fern(x, 1080, 1.1, r, "#4fae45" if not night else "#2f5546", "#1f5a22" if not night else "#173326") for x in (60, 1860)]
    if not night:
        parts += [_flower(r.uniform(40, 1880), r.uniform(840, 1060), r.uniform(1.0, 1.6), r.choice(["#ff8fb1", "#ffffff", "#b58cff", "#ff9f43"]), r) for _ in range(40)]
    else:
        parts.append("".join(f'<path d="M{r.uniform(0, W):.0f} {r.uniform(0, H):.0f} l-14 46" stroke="#cfe3ff" stroke-width="4" opacity="0.6" stroke-linecap="round"/>' for _ in range(220)))
    parts.append(_paper_texture())
    parts.append(_vignette(opacity=0.18 if not night else 0.4))
    return _svg("".join(parts))


def meadow():
    r = random.Random(2)
    parts = [_sky("#a7e2ff", "#fff7d6"), _sun(1720, 150, 60)]
    parts += [_cloud(x, y, s) for x, y, s in [(360, 150, 1.2), (980, 120, 0.9)]]
    parts.append(_ridge(r, 520, 90, "#c7b8e3", jag=20))
    parts.append(_hill(960, 700, 1300, 260, "#b6e494", "#8ccf6c", "m1"))
    parts.append(_ground(600, "#9fdc78", "#5fae45", "mg"))
    parts.append(_grass_blades(r, 610, 1080, 1400, ["#5fae45", "#4f9e3a", "#7cc85a", "#8bd66a"]))
    colors = ["#ff8fb1", "#ffffff", "#b58cff", "#ff9f43", "#ffd23f", "#7fc8ff"]
    flowers = [(r.uniform(0, W), r.uniform(640, 1070)) for _ in range(160)]
    flowers.sort(key=lambda p: p[1])
    parts += [_flower(x, y, 0.8 + (y - 600) / 300, r.choice(colors), r) for x, y in flowers]
    parts.append(_paper_texture())
    parts.append(_vignette(opacity=0.15))
    return _svg("".join(parts))


def river():
    r = random.Random(3)
    parts = [_sky("#9fdcff", "#fff3d6"), _sun(300, 140, 60)]
    parts += [_cloud(x, y, s) for x, y, s in [(900, 140, 1.2), (1500, 100, 1.0)]]
    parts.append(_ridge(r, 540, 120, "#b9a6d9", jag=30))
    parts.append(_hill(960, 640, 1200, 200, "#a8dc8a", "#86c66b", "r1"))
    parts.append(_ground(600, "#8fd16a", "#5fae45", "rg"))
    parts.append('<defs><linearGradient id="water" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#7fd0ff"/><stop offset="1" stop-color="#2f8fc9"/></linearGradient></defs>')
    parts.append(f'<path d="M0 700 C400 660 700 740 1000 700 C1300 660 1600 720 {W} 690 L{W} 930 C1600 960 1300 900 1000 940 C700 980 400 900 0 940 Z" fill="url(#water)" stroke="{INK}" stroke-width="5" stroke-opacity="0.35"/>')
    parts.append("".join(
        f'<path d="M{x:.0f} {y:.0f} q30 -8 60 0" stroke="#e6f7ff" stroke-width="5" fill="none" opacity="0.7" stroke-linecap="round"/>'
        for x, y in [(r.uniform(0, W - 60), r.uniform(720, 900)) for _ in range(60)]
    ))
    for x in [r.uniform(0, W) for _ in range(30)]:
        y = r.choice([r.uniform(680, 710), r.uniform(930, 960)])
        parts.append(f'<path d="M{x:.0f} {y:.0f} q-6 -60 4 -110" stroke="#4f8a3a" stroke-width="7" fill="none"/>'
                     f'<ellipse cx="{x+4:.0f}" cy="{y-110:.0f}" rx="9" ry="24" fill="#8a5a2b"/>')
    parts.append(_grass_blades(r, 940, 1080, 400, ["#5fae45", "#4f9e3a", "#7cc85a"]))
    parts.append(_grass_blades(r, 600, 690, 300, ["#5fae45", "#4f9e3a", "#7cc85a"]))
    parts.append(_paper_texture())
    parts.append(_vignette(opacity=0.15))
    return _svg("".join(parts))


def forest():
    r = random.Random(4)
    parts = [_sky("#bfe9d6", "#f3f7d8")]
    parts.append('<defs><radialGradient id="beam" cx="0.5" cy="0" r="1"><stop offset="0" stop-color="#fffbe0" stop-opacity="0.7"/><stop offset="1" stop-color="#fffbe0" stop-opacity="0"/></radialGradient></defs>')
    for i in range(7):
        x = 120 + i * 280 + r.uniform(-40, 40)
        w = r.uniform(50, 90)
        parts.append(f'<rect x="{x:.0f}" y="0" width="{w:.0f}" height="900" fill="#7a6a58" opacity="0.35"/>')
    parts.append(_hill(960, 720, 1300, 200, "#9fd38c", "#6cb35c", "f1"))
    for i in range(5):
        x = 60 + i * 450 + r.uniform(-30, 30)
        parts.append(f'<path d="M{x:.0f} 1000 C{x+10:.0f} 600 {x-20:.0f} 300 {x+10:.0f} 0 L{x+120:.0f} 0 C{x+90:.0f} 300 {x+110:.0f} 600 {x+130:.0f} 1000 Z" fill="#8a5a3b" stroke="{INK}" stroke-width="6" stroke-opacity="0.5"/>')
        parts.append(f'<path d="M{x+40:.0f} 900 C{x+50:.0f} 600 {x+30:.0f} 300 {x+50:.0f} 0" stroke="#6f4528" stroke-width="10" fill="none" opacity="0.5"/>')
    parts.append(_ground(800, "#7fc263", "#4f9a3e", "fg"))
    parts.append(f'<path d="M500 0 L900 0 L1300 1080 L700 1080 Z" fill="url(#beam)" opacity="0.6"/>')
    parts.append(_grass_blades(r, 800, 1080, 900, ["#4f9e3a", "#5fae45", "#3f8a35"]))
    parts += [_fern(x, 1090, r.uniform(1.0, 1.5), r) for x in (0, 300, 1620, 1920)]
    for _ in range(25):
        x, y = r.uniform(0, W), r.uniform(0, 700)
        parts.append(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{r.uniform(6, 14):.0f}" fill="#fffbe0" opacity="0.5"/>')
    parts.append(_paper_texture())
    parts.append(_vignette(opacity=0.25))
    return _svg("".join(parts))


def cave():
    r = random.Random(5)
    parts = ['<rect width="1920" height="1080" fill="#1c1a2e"/>']
    parts.append('<defs><radialGradient id="mouth" cx="0.5" cy="0.4" r="0.6"><stop offset="0" stop-color="#ffe6a8" stop-opacity="0.9"/><stop offset="0.5" stop-color="#ffb46a" stop-opacity="0.35"/><stop offset="1" stop-color="#ffb46a" stop-opacity="0"/></radialGradient></defs>')
    parts.append('<ellipse cx="1650" cy="420" rx="380" ry="420" fill="url(#mouth)"/>')
    rock = ""
    for i in range(18):
        x = i * 115 + r.uniform(-20, 20)
        h = r.uniform(80, 220)
        rock += f'<path d="M{x:.0f} 0 L{x+70:.0f} 0 L{x+35:.0f} {h:.0f} Z" fill="#2b2842" stroke="#151324" stroke-width="5"/>'
    parts.append(rock)
    parts.append('<path d="M0 760 C300 720 600 780 960 750 C1300 720 1600 780 1920 740 L1920 1080 L0 1080 Z" fill="#2e2a46" stroke="#151324" stroke-width="6"/>')
    for _ in range(40):
        x, y = r.uniform(0, W), r.uniform(780, 1060)
        parts.append(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{r.uniform(20, 60):.0f}" ry="{r.uniform(10, 24):.0f}" fill="#3b3658"/>')
    for x, y, s, c in [(260, 760, 1.0, "#9a7cff"), (420, 790, 0.7, "#7fe0ff"), (1500, 770, 0.9, "#ff8fd1"), (1120, 740, 0.6, "#9a7cff")]:
        parts.append(f'<g opacity="0.95"><path d="M{x} {y} l{20*s:.0f} {-110*s:.0f} l{20*s:.0f} {110*s:.0f} Z M{x+30*s:.0f} {y} l{30*s:.0f} {-80*s:.0f} l{14*s:.0f} {80*s:.0f} Z" fill="{c}" stroke="#151324" stroke-width="4"/></g>')
    for _ in range(60):
        parts.append(f'<circle cx="{r.uniform(0, W):.0f}" cy="{r.uniform(0, 700):.0f}" r="{r.uniform(1.5, 3.5):.1f}" fill="#d8ffef" opacity="{r.uniform(0.3, 0.8):.2f}"/>')
    parts.append(_paper_texture(0.1))
    parts.append(_vignette(opacity=0.5))
    return _svg("".join(parts))


def scenes() -> dict:
    return {
        "scene_valley": valley(),
        "scene_storm": valley(night=True),
        "scene_meadow": meadow(),
        "scene_river": river(),
        "scene_forest": forest(),
        "scene_cave": cave(),
    }
